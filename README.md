# Issue Tracking System

Spring Boot microservices issue tracker with MySQL, Eureka service discovery, a Spring
Cloud Gateway and a JSP web front end.

Built from a case-study specification and its reference workbook. The requirements, and
every decision taken where those two sources were silent or contradicted each other, are
in **[docs/SRS.md](docs/SRS.md)**; the design is in **[docs/DESIGN.md](docs/DESIGN.md)**.

---

## Quick start

**1. Create the databases** — once, and the only step that needs administrator rights:

```bash
sudo mysql < sql/setup.sql
```

This creates the `its` MySQL login and the four schemas. The services create their own
tables at start-up and load the workbook's sample rows into empty ones.

> The password in that script is `Its#Tracker2026!`, chosen to satisfy MySQL's default
> `validate_password` policy — a simpler one is rejected with `ERROR 1819`. It matches the
> development default compiled into each service, so nothing else needs configuring. If
> your MySQL enforces a stricter policy, change it in `sql/setup.sql` and set
> `DB_PASSWORD` in `.env` to match.

**2. Build:**

```bash
mvn clean package
```

**3. Run:**

```bash
./scripts/start-all.sh
```

Then open **http://localhost:8090**.

Stop everything with `./scripts/stop-all.sh`. Logs are written to `logs/`.

If a service does not come up, `start-all.sh` reads its log back and names the likely
cause. `./scripts/start-all.sh -v` adds the exact command, health responses and timings
for every step.

### Running on Windows

Run it under **Git Bash**, which ships with Git for Windows. Nothing in the application
itself is platform-specific — no shell-outs, no unix paths, no native dependencies — but
four things differ from the instructions above.

**Use Git Bash, not PowerShell or `cmd`.** Both scripts are bash. Git Bash also supplies
the `curl` the readiness probes need.

**Step 1 has no `sudo`.** Run the setup script as the MySQL root user instead:

```bash
mysql -u root -p < sql/setup.sql
```

**Check your PATH.** Git Bash inherits the Windows PATH, and the MySQL installer does not
add the client to it by default. Maven has to be installed separately — there is no
wrapper in this repository. All three should answer, with Java on **17 or newer**:

```bash
java -version && mvn -v && mysql --version
```

`start-all.sh` verifies Java and the build artifacts before it starts anything, so a
missing JDK is reported rather than discovered seven JVMs later. It also probes MySQL,
preferring the `mysql` client when it is on PATH and falling back to a TCP check. If that
probe is ever wrong on your machine, `SKIP_DB_CHECK=1 ./scripts/start-all.sh` bypasses it.

**Line endings are handled, but only for fresh clones.** `.gitattributes` pins `*.sh` to
LF, because Git for Windows otherwise checks them out with CRLF and bash then fails with
`/usr/bin/env: 'bash\r': No such file or directory`. A clone taken *before* that file
existed still has CRLF on disk; re-check out the two scripts once:

```bash
rm scripts/*.sh && git checkout scripts/
```

`git checkout` restores **every** file under the path from the last commit, not only the
one you deleted, so commit or stash any local edits to the scripts first — otherwise this
silently discards them. Nothing outside `scripts/` is touched. To confirm the fix,
`file scripts/start-all.sh` should no longer mention `CRLF`.

Two behavioural differences worth knowing. `stop-all.sh` sends SIGTERM, which does not map
cleanly onto a native Windows JVM, so a service may not run its shutdown hook and can
linger on the Eureka dashboard as `UP` for a minute after stopping. And closing the Git
Bash window can take the background JVMs with it — use `stop-all.sh` rather than the
window's close button, or the next run finds its ports held.

### Seeded data

Starting the services on empty tables loads a development dataset: **70 users, 34
projects, 524 issues and 551 comments**. It is built in three layers, and knowing which
layer a row came from saves time when a screen looks wrong:

- **Hand-written** — the reference workbook's rows first, then a readable cast and
  backlog. Every status, priority and type is represented. Issues 1–40 are the ones worth
  reading.
- **Edge cases** — values at the 255-character column limit, non-Latin scripts, an
  apostrophe that breaks naive escaping, a row with every nullable field null, an issue
  from 2019, critical-and-unassigned, zero story points. These exist to be awkward, and
  they are how the tidy-data assumptions get caught.
- **Generated** — a few hundred rows of deterministic filler so lists, filters and status
  meters are exercised at volume. Index arithmetic, no randomness: two clean checkouts
  seed identical databases, so a screenshot stays reproducible.

Fixtures worth preserving: projects **1013** and **1022** have no issues (empty states,
cascade delete against a childless project), every issue on **1021** is `DONE` (a
completed project reads differently from an empty one), user **118** owns nothing and user
**119** is assigned nothing.

The bulk sits on two archive projects owned by one account, **`arun.balakrishnan@`**. That
is deliberate — the owner dashboard fetches every issue of every project it owns, so his
is the slow one and everybody else's stays quick. Sign in as him to see the system under
volume.

Passwords are stored as BCrypt hashes; these are the plaintexts to type in. The four
accounts from the reference workbook keep their original passwords:

| Email | Password | Role |
|---|---|---|
| `emily.sinha@example.com` | `EmilySecure!2025` | Project Owner |
| `priya.jackson@example.com` | `PriyaSafe@2025` | Project Owner |
| `carlos.singh@example.com` | `CarlosStrong$2025` | Assignee |
| `michael.patel@example.com` | `MichaelPass#2025` | Assignee |

**Every other account uses `Password!2026`**, including the generated ones. The names
worth knowing: `lena.fischer@` and `arun.balakrishnan@` are Project Owners,
`aisha.rahman@`, `tom.okafor@`, `diego.morales@`, `sofia.bergman@`, `raj.mehta@`,
`chloe.dubois@` and `noah.adeyemi@` are Assignees (all `@example.com`).

Sign in as **Emily** for the owner side (issue creation, cascade delete), **Carlos** for
the assignee side — a board of his own work, where he can change status and nothing else,
**Arun** for the same screens under a few hundred issues, or **Grace**
(`grace.nakamura@`) for an owner dashboard with nothing on it at all.

To reload the dataset after changing it, clear the tables and restart:

```bash
mysql -u its -p'Its#Tracker2026!' -e "TRUNCATE user_db.user; TRUNCATE project_db.project; \
  TRUNCATE issue_db.issue; TRUNCATE comment_db.comment;"
```

Seeders only populate an empty table, so nothing is overwritten while data is present.

---

## What runs where

| Component | Port | Purpose |
|---|---|---|
| Web UI | 8090 | JSP application — the only page a person opens |
| API Gateway | 8080 | Single entry point, JWT verification, load balancing |
| Eureka | 8761 | Service registry — dashboard shows all four services `UP` |
| User Service | 8085 | Identity, credentials, roles; issues the JWT |
| Project Service | 8082 | Projects and ownership; orchestrates the delete cascade |
| Issue Service | 8083 | Issues, assignment, workflow — owns every issue query |
| Comment Service | 8084 | Comment threads |

- **Swagger (all services, one page):** http://localhost:8080/swagger-ui.html
- **Eureka dashboard:** http://localhost:8761

---

## Testing

```bash
mvn test
```

106 tests. The ones worth knowing about:

| What | Where |
|---|---|
| The `role` 0/1 encoding, pinned to the workbook | `user-service` · `RoleConverterTest` |
| Login failures are indistinguishable to a caller | `user-service` · `UserServiceImplTest` |
| Cascade deletes children first, and a failure leaves the parent | `project-service` · `ProjectCascadeDeleteTest` |
| Status transitions and Assignee restrictions | `issue-service` · `IssueWorkflowTest` |
| A partial cascade failure deletes no issue rows | `issue-service` · `IssueCascadeDeleteTest` |
| A spoofed `X-User-Role` header is stripped at the gateway | `api-gateway` · `JwtAuthenticationFilterTest` |
| The OpenAPI document carries `bearerAuth`, so Swagger can Authorize | `user-service` · `OpenApiDocumentTest` |
| Seed rows fit their columns, and cross-service ids still resolve | all four · `DataSeederTest` |

Tests run against in-memory H2 and need neither MySQL nor a running registry.

**Exercising the API by hand:** open http://localhost:8080/swagger-ui.html. The dropdown
top right switches between the four services; everything is reachable through the gateway,
so the port never changes.

1. Pick *user-service*, run `POST /api/users/login` with a seeded account, and copy the
   `token` from the response.
2. Click **Authorize**, paste the raw token — no `Bearer` prefix — and confirm.
3. Every other endpoint now sends that token. The authorization stays put while you switch
   services in the dropdown, so a project owner's token carries over to the Issue Service.

Endpoints are grouped by controller and documented against the seeded data, so reading a
service top to bottom walks its lifecycle. `POST /api/users` and `POST /api/users/login`
are public and ignore the Authorize state; everything else answers 401 without it.

---

## Repository layout

```
├── docs/            SRS and design document
├── eureka-server/   Service registry
├── api-gateway/     Routing, JWT verification, load balancing
├── user-service/    :8085  user_db
├── project-service/ :8082  project_db
├── issue-service/   :8083  issue_db
├── comment-service/ :8084  comment_db
├── web-ui/          :8090  JSP front end (WAR)
├── scripts/         start-all.sh, stop-all.sh
└── sql/setup.sql    One-time database setup
```

---

## Things that will look odd unless you know

Each of these is a decision, not an accident, and each is recorded in the SRS assumptions
register.

- **`role = 0` is Project Owner, `1` is Assignee.** The reverse of what most people
  guess. It comes from the workbook's sample data, where both owners are `0` and both
  engineers are `1`, corroborated by every `project_owner_id` and `assignee_id` in the
  other two tables. Getting it backwards inverts the whole permission model while still
  passing a smoke test, so the integer appears in exactly two files — `Role` and
  `RoleConverter` — and nothing above the persistence layer ever sees it. ([A-04](docs/SRS.md#a-04))

- **`profile` is a text bio, not an image.** The case study prose says "profile image"
  throughout; the workbook's actual values are *"Front-end developer"*, *"Seasoned
  project owner"*. The workbook wins. ([A-16](docs/SRS.md#a-16))

- **Statuses are `TO_DO / IN_PROGRESS / IN_REVIEW / DONE`,** not `OPEN`/`CLOSED`. Only
  `TO_DO`, `HIGH` and `BUG` are attested by the workbook; the rest complete each set
  conventionally, which is why enums are persisted as strings rather than ordinals.
  ([A-11](docs/SRS.md#a-11))

- **Deleting a project deletes its issues and their comments,** and does so
  children-first across three databases. It is not atomic — no distributed transaction
  exists here — but the ordering means any failure leaves the project still listed and
  the operation safely repeatable. Deleting the parent first would be simpler and would
  strand unreachable rows. ([A-07](docs/SRS.md#a-07), [DESIGN §6.4](docs/DESIGN.md))

- **A username is the local part of an email address.** Neither source defines a username
  column, but an endpoint requires one and the workbook's `sam.lee` matches the shape.
  ([A-18](docs/SRS.md#a-18))

- **The JWT never reaches the browser.** JSP renders server-side, so the token lives in
  the web tier's `HttpSession`; the browser holds only a `JSESSIONID`.
  ([A-14](docs/SRS.md#a-14))

- **`web-ui` is a WAR, every other module a JAR.** Not a preference: Spring Boot cannot
  serve JSPs from an executable JAR. ([A-09](docs/SRS.md#a-09))

---

## Configuration

Everything has a working development default, so a clean checkout runs with no
configuration at all. To override, copy `.env.example` to `.env` — `start-all.sh` sources
it.

The one setting that must match across two modules is `JWT_SECRET`: `user-service` signs
with it and `api-gateway` verifies with it. A mismatch shows up as every authenticated
request returning 401 with a token that looks perfectly valid.

---

## Milestones

| # | Requirement | Where |
|---|---|---|
| 1 | User microservice + endpoints, exercised in Swagger | `user-service`, *user-service* in the dropdown |
| 2 | Project microservice + endpoints, exercised in Swagger | `project-service`, *project-service* |
| 3 | Issue microservice + endpoints, exercised in Swagger | `issue-service`, *issue-service* |
| 4 | Eureka; all services on the dashboard | `eureka-server`, :8761 |
| 5 | All inter-service communication endpoints | 8 endpoints marked **ISC** in Swagger |
| 6 | `ResponseEntity` throughout; Swagger | Every controller; :8080/swagger-ui.html |
| 7 | Gateway with client-side load balancing | `api-gateway`, `lb://` routes |
| 8 | Push to GitHub | — |

The web UI is not a source milestone; it was added on top of the case study's scope.

---

## Network access

Every service binds `0.0.0.0` by default, so the system is reachable from other machines
on the network — open `http://<host-ip>:8090` instead of `localhost`.

To restrict it to the local machine, set `SERVER_ADDRESS=127.0.0.1` in `.env`.

Two things to be aware of before exposing this beyond a trusted network: it is served
over plain HTTP, so tokens and session cookies travel in clear text; and the seeded
accounts and their passwords are in this README. Neither matters on a development
machine — both matter on a shared network.
