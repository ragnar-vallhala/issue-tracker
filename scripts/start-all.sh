#!/usr/bin/env bash
#
# Starts the whole system in dependency order and waits for each tier to come up.
#
# Order matters at two points only: Eureka must be up before the services register, and
# the gateway must be up before the web UI is useful. The services themselves can start
# in any order - they retry registration - but the gateway cannot route to a service that
# has not yet appeared in the registry, so it is worth letting them settle first.
#
#   ./scripts/start-all.sh          start everything
#   ./scripts/start-all.sh -v       and show every command, health probe and setting
#   ./scripts/stop-all.sh           stop everything
#
#   SKIP_DB_CHECK=1 ./scripts/start-all.sh    start anyway if the database probe is wrong
#
# A service that fails to start used to leave nothing behind but "TIMED OUT - see the
# log". Nearly every such failure is one of four things - MySQL not running, sql/setup.sql
# never run, a port already taken, or a stale JVM from a previous run - and each one has a
# recognisable line in the log. This script checks for those before starting anything,
# gives up the moment a JVM dies rather than waiting out the timeout, and reads the log
# back to you when a wait fails.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS="$ROOT/logs"
PIDS="$ROOT/logs/pids"

VERBOSE=0
for arg in "$@"; do
    case "$arg" in
        -v|--verbose) VERBOSE=1 ;;
        -h|--help)
            sed -n '3,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *) echo "Unknown option: $arg (try --help)" >&2; exit 2 ;;
    esac
done

# Colour only when stdout is a terminal, so redirected output stays readable.
if [[ -t 1 ]]; then
    BOLD=$'\033[1m'; RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'
    DIM=$'\033[2m'; RESET=$'\033[0m'
else
    BOLD=""; RED=""; GREEN=""; YELLOW=""; DIM=""; RESET=""
fi

say()    { echo "$*"; }
detail() { [[ $VERBOSE -eq 1 ]] && echo "${DIM}  . $*${RESET}" || true; }
warn()   { echo "${YELLOW}  ! $*${RESET}" >&2; }
fail()   { echo "${RED}  x $*${RESET}" >&2; }
ok()     { echo "${GREEN}  + $*${RESET}"; }

mkdir -p "$LOGS"

# ---------------------------------------------------------------------------------
# Environment
# ---------------------------------------------------------------------------------

if [[ -f "$ROOT/.env" ]]; then
    # shellcheck disable=SC1091
    set -a && source "$ROOT/.env" && set +a
    say "Loaded environment from .env"
else
    detail "No .env file; application.yml defaults apply"
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-its}"

detail "ROOT       $ROOT"
detail "DB         $DB_USER@$DB_HOST:$DB_PORT"
detail "EUREKA_URL ${EUREKA_URL:-http://localhost:8761/eureka/ (default)}"
detail "SEED       ${SEED_ENABLED:-true (default)}"

# ---------------------------------------------------------------------------------
# Pre-flight
#
# Every check here corresponds to a failure that is otherwise indistinguishable from
# any other: the service starts, writes a stack trace to its own log, exits, and the
# script waits two minutes for a health endpoint that will never answer.
# ---------------------------------------------------------------------------------

PREFLIGHT_FAILED=0

say ""
say "${BOLD}=== Pre-flight ===${RESET}"

# --- Java ---
if ! command -v java >/dev/null 2>&1; then
    fail "java is not on PATH"
    PREFLIGHT_FAILED=1
else
    JAVA_VERSION="$(java -version 2>&1 | head -1)"
    detail "java: $JAVA_VERSION"
    ok "java found"
fi

# --- curl, used by every readiness probe below ---
if ! command -v curl >/dev/null 2>&1; then
    fail "curl is not on PATH - the readiness probes need it"
    PREFLIGHT_FAILED=1
fi

# --- Build artifacts ---
missing_artifacts=0
for artifact in \
    "eureka-server/target/eureka-server-1.0.0.jar" \
    "user-service/target/user-service-1.0.0.jar" \
    "project-service/target/project-service-1.0.0.jar" \
    "issue-service/target/issue-service-1.0.0.jar" \
    "comment-service/target/comment-service-1.0.0.jar" \
    "api-gateway/target/api-gateway-1.0.0.jar" \
    "web-ui/target/web-ui-1.0.0.war"
do
    if [[ -f "$ROOT/$artifact" ]]; then
        detail "$(basename "$artifact") present"
    else
        fail "missing $artifact"
        missing_artifacts=1
    fi
done

if [[ $missing_artifacts -eq 1 ]]; then
    fail "run 'mvn clean package -DskipTests' first"
    PREFLIGHT_FAILED=1
else
    ok "build artifacts present"
fi

# --- MySQL ---
#
# Eureka has no database, so it starts happily while all four data services fail. That
# asymmetry is the single most confusing failure this script can produce, and it is
# what a bare TCP probe here prevents.
mysql_reachable() {
    # bash's /dev/tcp works under MSYS (Git Bash) as well as Linux, but it is a shell
    # build option rather than a guarantee. Where the client is installed it answers the
    # question authoritatively, so it is tried first - a shell without /dev/tcp would
    # otherwise report a perfectly healthy database as down and refuse to start anything.
    if command -v mysql >/dev/null 2>&1; then
        # --protocol=TCP matters: given -h localhost the client connects over a unix
        # socket and ignores -P entirely, so a wrong DB_PORT would pass this check and
        # then fail in every service, which connects over TCP through the JDBC driver.
        mysql --protocol=TCP -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
              -p"${DB_PASSWORD:-Its#Tracker2026!}" -e "SELECT 1;" >/dev/null 2>&1 && return 0
    fi
    (exec 3<>"/dev/tcp/$DB_HOST/$DB_PORT") 2>/dev/null
}

if [[ "${SKIP_DB_CHECK:-0}" == "1" ]]; then
    warn "SKIP_DB_CHECK=1 - not checking the database; services will fail on their own"
elif mysql_reachable; then
    ok "MySQL is accepting connections on $DB_HOST:$DB_PORT"

    # Reachable is not the same as usable: the login and the four schemas come from
    # sql/setup.sql, and without them every service fails with "Access denied" or
    # "Unknown database" well after this script has stopped watching.
    if command -v mysql >/dev/null 2>&1; then
        if mysql --protocol=TCP -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
                 -p"${DB_PASSWORD:-Its#Tracker2026!}" \
                 -N -e "SELECT COUNT(*) FROM information_schema.schemata
                        WHERE schema_name IN
                        ('user_db','project_db','issue_db','comment_db');" \
                 2>/dev/null | grep -q '^4$'; then
            ok "the four schemas exist and '$DB_USER' can see them"
        else
            fail "connected to MySQL, but '$DB_USER' cannot see all four schemas"
            fail "run:  mysql -u root -p < sql/setup.sql"
            PREFLIGHT_FAILED=1
        fi
    else
        detail "mysql client not on PATH - cannot verify the schemas from here"
        warn "if the services fail with 'Unknown database', run sql/setup.sql"
    fi
else
    fail "nothing is listening on $DB_HOST:$DB_PORT - MySQL is not running"
    fail "start MySQL, then run:  mysql -u root -p < sql/setup.sql"
    say ""
    say "  ${DIM}Eureka would still start without it, which is why this used to look"
    say "  like 'user-service timed out' rather than 'the database is down'.${RESET}"
    PREFLIGHT_FAILED=1
fi

# --- Ports ---
#
# A JVM left over from a previous run holds its port, and the replacement dies on
# "Port 8085 was already in use" - but the old one keeps answering /actuator/health,
# so a naive probe reports success and the new code never actually runs.
port_in_use() {
    (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null
}

busy_ports=""
for port in 8761 8085 8082 8083 8084 8080 8090; do
    if port_in_use "$port"; then
        busy_ports="$busy_ports $port"
    fi
done

if [[ -n "$busy_ports" ]]; then
    fail "already in use:$busy_ports"
    fail "run ./scripts/stop-all.sh, or kill whatever holds those ports"
    PREFLIGHT_FAILED=1
else
    ok "all seven ports are free"
fi

if [[ $PREFLIGHT_FAILED -eq 1 ]]; then
    say ""
    fail "pre-flight failed - nothing was started"
    exit 1
fi

: > "$PIDS"

# ---------------------------------------------------------------------------------
# Starting and waiting
# ---------------------------------------------------------------------------------

start() {
    local name="$1" artifact="$2" port="$3"

    detail "java -jar $artifact"
    echo -n "Starting $name on :$port ... "

    nohup java -jar "$artifact" > "$LOGS/$name.log" 2>&1 &
    local pid=$!

    echo "$pid" >> "$PIDS"
    echo "$pid" > "$LOGS/$name.pid"
    echo "pid $pid"
}

# Reads the log back and names the cause where the log makes it obvious. The patterns
# are the four failures that account for nearly every one of these in practice.
diagnose() {
    local name="$1" logfile="$LOGS/$name.log"

    say ""
    fail "$name did not come up"

    if [[ ! -s "$logfile" ]]; then
        fail "its log is empty: $logfile"
        fail "the JVM produced no output at all - check that java runs at all"
        return
    fi

    local hint=""
    if grep -qi "Communications link failure\|Connection refused.*3306\|CommunicationsException" "$logfile"; then
        hint="MySQL is not reachable. Start it, then retry."
    elif grep -qi "Access denied for user" "$logfile"; then
        hint="MySQL rejected the credentials. Run: mysql -u root -p < sql/setup.sql"
    elif grep -qi "Unknown database" "$logfile"; then
        hint="A schema is missing. Run: mysql -u root -p < sql/setup.sql"
    elif grep -qi "Port .* was already in use\|Address already in use" "$logfile"; then
        hint="Its port is held by another process. Run ./scripts/stop-all.sh first."
    elif grep -qi "Cannot assign requested address" "$logfile"; then
        # Distinct from a port clash, and easy to confuse with one: the port is free,
        # the address is not this machine's. SERVER_ADDRESS in .env is the usual cause.
        hint="SERVER_ADDRESS (${SERVER_ADDRESS:-unset}) is not an address this host owns."
    elif grep -qi "OutOfMemoryError" "$logfile"; then
        hint="The JVM ran out of memory."
    elif grep -qi "UnsatisfiedDependencyException\|BeanCreationException" "$logfile"; then
        hint="A bean failed to build - the cause is in the 'Caused by' line below."
    fi

    if [[ -n "$hint" ]]; then
        say ""
        fail "likely cause: $hint"
    fi

    say ""
    say "${DIM}--- last 40 lines of $logfile ---${RESET}"
    tail -40 "$logfile"
    say "${DIM}--- end of log ---${RESET}"
    say ""
    fail "full log: $logfile"
}

wait_for() {
    local name="$1" url="$2" attempts="${3:-60}"
    local pid
    pid="$(cat "$LOGS/$name.pid" 2>/dev/null || echo "")"

    echo -n "  waiting for $name "
    local started=$SECONDS

    for ((i = 0; i < attempts; i++)); do
        # A dead JVM will never answer, so stop waiting the moment it exits rather
        # than burning the full two minutes on a process that is already gone.
        if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then
            echo " ${RED}process exited${RESET}"
            diagnose "$name"
            return 1
        fi

        if curl -sf -o /dev/null "$url"; then
            echo " ${GREEN}up${RESET} ${DIM}($((SECONDS - started))s)${RESET}"
            if [[ $VERBOSE -eq 1 ]]; then
                detail "health: $(curl -s "$url" 2>/dev/null | head -c 200)"
            fi
            return 0
        fi

        echo -n "."
        sleep 2
    done

    echo " ${RED}TIMED OUT${RESET} after $((SECONDS - started))s"
    diagnose "$name"
    return 1
}

# Any failed wait should stop the run and explain itself, not leave half a system up
# with the reason scrolled off the screen.
launch() {
    local name="$1" artifact="$2" port="$3" url="$4"

    start "$name" "$ROOT/$artifact" "$port"
    if ! wait_for "$name" "$url"; then
        say ""
        fail "startup aborted at $name. Stopping what did start."
        "$ROOT/scripts/stop-all.sh" >/dev/null 2>&1 || true
        exit 1
    fi
}

say ""
say "${BOLD}=== Eureka ===${RESET}"
launch eureka-server "eureka-server/target/eureka-server-1.0.0.jar" 8761 \
    http://localhost:8761/actuator/health

say ""
say "${BOLD}=== Services ===${RESET}"
start user-service    "$ROOT/user-service/target/user-service-1.0.0.jar"       8085
start project-service "$ROOT/project-service/target/project-service-1.0.0.jar" 8082
start issue-service   "$ROOT/issue-service/target/issue-service-1.0.0.jar"     8083
start comment-service "$ROOT/comment-service/target/comment-service-1.0.0.jar" 8084

for svc in "user-service:8085" "project-service:8082" \
           "issue-service:8083" "comment-service:8084"; do
    name="${svc%%:*}"
    port="${svc##*:}"
    if ! wait_for "$name" "http://localhost:$port/actuator/health"; then
        say ""
        fail "startup aborted at $name. Stopping what did start."
        "$ROOT/scripts/stop-all.sh" >/dev/null 2>&1 || true
        exit 1
    fi
done

say ""
say "${BOLD}=== Edge ===${RESET}"
launch api-gateway "api-gateway/target/api-gateway-1.0.0.jar" 8080 \
    http://localhost:8080/actuator/health

# Give the gateway a moment to pull the registry, or the first request through it can
# 503 while its instance list is still empty.
detail "waiting 5s for the gateway to pull the registry"
sleep 5

launch web-ui "web-ui/target/web-ui-1.0.0.war" 8090 \
    http://localhost:8090/actuator/health

if [[ $VERBOSE -eq 1 ]]; then
    say ""
    say "${BOLD}=== Registry ===${RESET}"
    # Eureka answers XML by default. Application names are upper-case; the datacenter
    # element is also a <name>, which is why this filters rather than pairing blindly.
    registry_xml="$(curl -s http://localhost:8761/eureka/apps 2>/dev/null || true)"
    registered="$(printf '%s' "$registry_xml" \
        | grep -o '<name>[^<]*</name>' | sed 's/<[^>]*>//g' \
        | grep -E '^[A-Z][A-Z0-9-]*$' | sort -u || true)"

    if [[ -n "$registered" ]]; then
        while IFS= read -r app; do detail "$app"; done <<< "$registered"
        detail "$(printf '%s' "$registry_xml" | grep -c '<status>UP</status>' || true) instance(s) UP"
    else
        detail "(registry returned nothing - services register a few seconds after start)"
    fi
fi

cat <<EOF

${GREEN}All services are up.${RESET}

  Application    http://localhost:8090
  API gateway    http://localhost:8080
  Swagger        http://localhost:8080/swagger-ui.html
  Eureka         http://localhost:8761

Sign in with a seeded account (see README):
  emily.sinha@example.com / EmilySecure!2025   (Project Owner)
  carlos.singh@example.com / CarlosStrong\$2025 (Assignee)

Logs are in $LOGS. Stop everything with ./scripts/stop-all.sh
EOF
