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

### Seeded accounts

From the reference workbook. Passwords are stored as BCrypt hashes; these are the
plaintexts to type in.

| Email | Password | Role |
|---|---|---|
| `emily.sinha@example.com` | `EmilySecure!2025` | Project Owner |
| `priya.jackson@example.com` | `PriyaSafe@2025` | Project Owner |
| `carlos.singh@example.com` | `CarlosStrong$2025` | Assignee |
| `michael.patel@example.com` | `MichaelPass#2025` | Assignee |

Sign in as Emily to see the owner side (projects, issue creation, cascade delete), and as
Carlos to see the assignee side (status-only updates on his own issues).

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

70 tests. The ones worth knowing about:

| What | Where |
|---|---|
| The `role` 0/1 encoding, pinned to the workbook | `user-service` · `RoleConverterTest` |
| Login failures are indistinguishable to a caller | `user-service` · `UserServiceImplTest` |
| Cascade deletes children first, and a failure leaves the parent | `project-service` · `ProjectCascadeDeleteTest` |
| Status transitions and Assignee restrictions | `issue-service` · `IssueWorkflowTest` |
| A partial cascade failure deletes no issue rows | `issue-service` · `IssueCascadeDeleteTest` |
| A spoofed `X-User-Role` header is stripped at the gateway | `api-gateway` · `JwtAuthenticationFilterTest` |

Tests run against in-memory H2 and need neither MySQL nor a running registry.

**Postman:** import `postman/ITS.postman_collection.json`. Run *0 - Authentication →
Log in* first; it stores the token and every other request picks it up. The folders are
ordered to run top to bottom against the seeded data.

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
├── postman/         API collection
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
| 1 | User microservice + endpoints, Postman | `user-service`, collection folder 1 |
| 2 | Project microservice + endpoints, Postman | `project-service`, folder 2 |
| 3 | Issue microservice + endpoints, Postman | `issue-service`, folder 3 |
| 4 | Eureka; all services on the dashboard | `eureka-server`, :8761 |
| 5 | All inter-service communication endpoints | 8 endpoints marked **ISC** in Swagger and the collection |
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
