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
#   ./scripts/stop-all.sh           stop everything

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS="$ROOT/logs"
PIDS="$ROOT/logs/pids"

mkdir -p "$LOGS"
: > "$PIDS"

if [[ -f "$ROOT/.env" ]]; then
    # shellcheck disable=SC1091
    set -a && source "$ROOT/.env" && set +a
    echo "Loaded environment from .env"
fi

start() {
    local name="$1" artifact="$2" port="$3"

    if [[ ! -f "$artifact" ]]; then
        echo "Missing $artifact - run 'mvn clean package -DskipTests' first." >&2
        exit 1
    fi

    echo -n "Starting $name on :$port ... "
    nohup java -jar "$artifact" > "$LOGS/$name.log" 2>&1 &
    echo "$!" >> "$PIDS"
    echo "pid $!"
}

wait_for() {
    local name="$1" url="$2" attempts="${3:-60}"

    echo -n "  waiting for $name "
    for ((i = 0; i < attempts; i++)); do
        if curl -sf -o /dev/null "$url"; then
            echo " up"
            return 0
        fi
        echo -n "."
        sleep 2
    done

    echo " TIMED OUT - see $LOGS/$name.log" >&2
    return 1
}

echo "=== Eureka ==="
start eureka-server "$ROOT/eureka-server/target/eureka-server-1.0.0.jar" 8761
wait_for eureka-server http://localhost:8761/actuator/health

echo
echo "=== Services ==="
start user-service    "$ROOT/user-service/target/user-service-1.0.0.jar"       8085
start project-service "$ROOT/project-service/target/project-service-1.0.0.jar" 8082
start issue-service   "$ROOT/issue-service/target/issue-service-1.0.0.jar"     8083
start comment-service "$ROOT/comment-service/target/comment-service-1.0.0.jar" 8084

wait_for user-service    http://localhost:8085/actuator/health
wait_for project-service http://localhost:8082/actuator/health
wait_for issue-service   http://localhost:8083/actuator/health
wait_for comment-service http://localhost:8084/actuator/health

echo
echo "=== Edge ==="
start api-gateway "$ROOT/api-gateway/target/api-gateway-1.0.0.jar" 8080
wait_for api-gateway http://localhost:8080/actuator/health

# Give the gateway a moment to pull the registry, or the first request through it can
# 503 while its instance list is still empty.
sleep 5

start web-ui "$ROOT/web-ui/target/web-ui-1.0.0.war" 8090
wait_for web-ui http://localhost:8090/actuator/health

cat <<EOF

All services are up.

  Application    http://localhost:8090
  API gateway    http://localhost:8080
  Swagger        http://localhost:8080/swagger-ui.html
  Eureka         http://localhost:8761

Sign in with a seeded account (see README):
  emily.sinha@example.com / EmilySecure!2025   (Project Owner)
  carlos.singh@example.com / CarlosStrong\$2025 (Assignee)

Logs are in $LOGS. Stop everything with ./scripts/stop-all.sh
EOF
