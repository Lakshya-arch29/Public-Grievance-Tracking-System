# Public-Grievance-Tracking-System
# Public-Grievance-Tracking-System
# JanSunwai

**Public Grievance Tracking System — report civic problems, get them routed to the right officer, and follow every step until resolved.**

JanSunwai lets citizens file grievances (potholes, water shortage, garbage, street lights and more), automatically assigns each one to a responsible officer, and gives everyone a status timeline until the case is closed. Administrators oversee all grievances, reassign them, and manage officers with live statistics.

> **Citizens report. The system routes. Officers act, and every step is on record.**

## Quickstart (local run)

Full step-by-step guide: **(LOCAL_SETUP.md)** (includes troubleshooting).

Short version:

```bash
git clone <repository-url>
cd jansunwai
# create the empty database once
psql -U postgres -c "CREATE DATABASE jansunwai;"
# set the DB password in the same terminal (never commit it)
export DB_PASSWORD=your_password          # Windows PowerShell: $env:DB_PASSWORD="your_password"
# start the app (tables and demo data are created automatically)
./mvnw spring-boot:run                    # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080 (role picker) and log in with a demo account below.

Prerequisites: **JDK 21**, **PostgreSQL 14+**, Git. Maven is not needed, the wrapper is included.

## Roles

| Role | How the account is created | What they can do |
|---|---|---|
| Citizen | Self sign-up | File grievances, see only their own, follow the status timeline |
| Officer | Created by an Admin | See assigned grievances, update status with a remark, see citizen contact details |
| Admin | Seeded in the database | See everything, reassign, dashboard statistics, officer directory with stats and filters, add or disable officers |

## Demo accounts

Seed data for demonstration only. Change every password before any real deployment.

| Role | Username | Password | Notes |
|---|---|---|---|
| Admin | `admin` | `Admin@123` | Sees everything |
| Officer | `sharma` | `Officer@123` | Pune, Roads & Potholes |
| Officer | `patil` | `Officer@123` | Thane, Water Supply |
| Officer | `kulkarni`, `deshmukh`, `iyer`, `khan` | `Officer@123` | Other cities and categories (finalised in the seed file) |
| Citizen | `rahul`, `priya`, `amit`, `sneha` | `Citizen@123` | Each has a few sample grievances |

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.x, Spring Web MVC |
| Data access | Spring JDBC (`NamedParameterJdbcTemplate`, `RowMapper`) |
| Database | PostgreSQL 14+ |
| Validation | Jakarta Bean Validation |
| Passwords | BCrypt via `spring-security-crypto` (full Spring Security is not used) |
| Authentication | Opaque random token in the `auth_token` table, sent as a Bearer header |
| Build | Maven with wrapper |
| Frontend | HTML5, CSS3, vanilla JavaScript (fetch API), served by Spring Boot |

## How a grievance flows

1. A citizen files a grievance and gets a tracking ID such as `GRV-00042`.
2. **Auto-assignment** picks the active officer with the fewest open grievances: same city and category first, then same city (any category), otherwise the grievance stays **Unassigned** for the admin.
3. The officer moves it to `IN_PROGRESS`, `RESOLVED` or `REJECTED`, always with a remark. `RESOLVED` and `REJECTED` are final in v1.
4. Every event (submission, assignment, status change, reassignment) is written to the history table and shown to the citizen as a timeline.
5. **Overdue** means still open and created more than 7 days ago.

## API surface

Base path `/api`, JSON, camelCase, `Authorization: Bearer <token>` on everything except public endpoints.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/me` |
| Lookup | `GET /api/lookup/categories` · `GET /api/lookup/cities` |
| Citizen | `POST /api/citizen/grievances` · `GET /api/citizen/summary` · `GET /api/citizen/grievances` · `GET /api/citizen/grievances/{trackingId}` |
| Officer | `GET /api/officer/summary` · `GET /api/officer/grievances` · `GET /api/officer/grievances/{trackingId}` · `PUT /api/officer/grievances/{trackingId}/status` |
| Admin | `GET /api/admin/summary` · `GET /api/admin/stats/cities` · `GET /api/admin/grievances` · `GET /api/admin/grievances/{trackingId}` · `PUT /api/admin/grievances/{trackingId}/assign` |
| Admin: officers | `GET /api/admin/officers` · `GET /api/admin/officers/{userId}` · `POST /api/admin/officers` · `PATCH /api/admin/officers/{userId}/active` |

Errors always use one JSON shape: `status`, `error`, `message` and optional `fieldErrors`. Full request and response examples are in the Backend Guide.

## Persistence (PostgreSQL)

All runtime data lives in PostgreSQL, so uploads, grievances, history and tokens survive restarts.

- Schema: `src/main/resources/db/schema.sql` (6 tables: `city`, `category`, `users`, `grievance`, `grievance_history`, `auth_token`)
- Seed: `src/main/resources/db/data.sql` (12 categories, 8 cities, 1 admin, 6 officers, 4 citizens, about 25 sample grievances)
- Both files run on every start, so both are **idempotent**: restarting never creates duplicates
- Tracking IDs are a **generated column** in the database, never built by the application
- Officers must have a city and a category (enforced by a CHECK constraint)

## Security basics

- BCrypt hashed passwords; a password or `password_hash` is never logged or returned
- Every SQL statement uses bound parameters
- Server-side role check on every endpoint, plus ownership checks (a citizen sees only their own grievances, an officer only their assigned ones)
- Sign-up always creates a `CITIZEN` and ignores any role sent by the client
- The role card on the landing page is only a convenience: the server compares it with the stored role
- User-supplied text is inserted with `textContent`, never `innerHTML`
- The database password comes only from the `DB_PASSWORD` environment variable

## Repository layout

```text
jansunwai/
├── README.md                     # this file
├── pom.xml                       # Spring Boot 3.x, Java 21
├── mvnw, mvnw.cmd, .mvn/         # Maven wrapper
├── .gitignore                    # target/, .idea/, .vscode/, *.iml, *.log
├── docs/                         # detailed guides (see index below)
└── src/
    ├── main/
    │   ├── java/com/jansunwai/
    │   │   ├── JanSunWaiApplication.java
    │   │   ├── config/           # PasswordConfig (BCrypt), WebConfig
    │   │   ├── security/         # AuthInterceptor, TokenService, RoleRequired, CurrentUser
    │   │   ├── model/            # User, Grievance, HistoryEntry + enums
    │   │   ├── dto/              # request and response classes
    │   │   ├── repository/       # JDBC repositories
    │   │   ├── service/          # AuthService, GrievanceService, AssignmentService, ...
    │   │   ├── controller/       # Auth, Lookup, Citizen, Officer, Admin
    │   │   └── exception/        # ApiException, GlobalExceptionHandler
    │   └── resources/
    │       ├── application.properties
    │       ├── db/               # schema.sql, data.sql
    │       └── static/           # index, login, citizen/, officer/, admin/, css/, js/
    └── test/java/com/jansunwai/  # unit and repository tests
```

Layering rule: controllers contain no SQL, repositories contain no business rules.

## Commands

| Task | Command |
|---|---|
| Run the app | `./mvnw spring-boot:run` |
| Run the tests | `./mvnw test` |
| Build a runnable jar | `./mvnw clean package` |
| Open a psql shell | `psql -U postgres -d jansunwai` |
| Reset all data | stop the app, `DROP DATABASE jansunwai;`, create it again, restart |

Environment variables: `DB_PASSWORD` (required), `DB_USER` (default `postgres`), `DB_URL` (default `jdbc:postgresql://localhost:5432/jansunwai`).

## Documentation index

- **[docs/BACKEND.md](docs/BACKEND.md)**: architecture, security, business rules, full API contract
- **[docs/FRONTEND.md](docs/FRONTEND.md)**: screens, navigation, JavaScript files, UI conventions
- **[docs/DATABASE.md](docs/DATABASE.md)**: tables, schema, key queries
- **[docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md)**: install, run, reset, troubleshooting
- **[docs/TESTING.md](docs/TESTING.md)**: test checklist, curl checks, demo script
- **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)**: how citizens, officers and admins use the portal
- **[docs/TEAM_GUIDE.md](docs/TEAM_GUIDE.md)**: ownership, git rules, checkpoints, risks

## Team and workflow

| Person | Owns |
|---|---|
| 1: Data | `db/`, `model/`, `repository/` |
| 2: API | `service/`, `controller/`, `security/`, `dto/`, `exception/`, `config/` |
| 3: UI | everything under `static/` |

- Never commit directly to `main`. Branches: `feature/db`, `feature/api`, `feature/ui`.
- Commit messages: `feat:`, `fix:`, `docs:` or `chore:` followed by what changed.
- Merge to `main` through a pull request that one other person has looked at.
- Shared files (`pom.xml`, README, docs) are edited by the team lead only, or through a reviewed pull request.
- Keep the API contract in the Backend Guide as the single agreement between API and UI. If code and docs disagree, fix one of them the same day.
- Never commit passwords, `.env` files, `target/` or IDE folders.

## Status

- Planning baseline (version 1.0): design, database, API contract, setup and team guide are written
- Code generation has not started yet, and the Java has not been compiled, so the first run on each laptop may need a small fix. Run the skeleton on all three machines at the start
- Out of scope for v1: real email or SMS, attachments, maps, forgot-password, citizen re-open, multi-language UI (see the Team Guide for future scope)
