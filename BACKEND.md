# Backend Guide

*Architecture, security, business rules and the REST API contract*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

## 1. Architecture and request flow

```text
Browser (static HTML + JS)
  | fetch("/api/...", Authorization: Bearer <token>)
  v
AuthInterceptor   checks token in auth_token table, expiry, then the required role
  v
Controller        receives JSON into a DTO, runs validation, calls one service method
  v
Service           business rules, @Transactional, throws ApiException on rule violations
  v
Repository        SQL through NamedParameterJdbcTemplate, maps rows to objects
  v
PostgreSQL
```

- Controllers never contain SQL. Repositories never contain business rules.
- Every state-changing operation that touches more than one table runs inside one service-level transaction (for example: update grievance and insert history row).
- Every SQL statement uses named or positional parameters. Concatenating user input into SQL is forbidden.
- All errors leave the API in one JSON shape (section 5), produced by a single GlobalExceptionHandler.

### Maven dependencies

| Artifact | Scope | Purpose |
|---|---|---|
| spring-boot-starter-web | compile | REST controllers, embedded Tomcat, static file serving |
| spring-boot-starter-jdbc | compile | JdbcTemplate, NamedParameterJdbcTemplate, connection pool, transactions |
| spring-boot-starter-validation | compile | @NotBlank, @Size, @Pattern on request objects |
| postgresql | runtime | JDBC driver |
| spring-security-crypto | compile | BCryptPasswordEncoder only |
| spring-boot-starter-test | test | JUnit 5 and Spring test support |

> The earlier practice project used spring-boot-starter-data-jdbc. This project uses spring-boot-starter-jdbc, because JdbcTemplate is all that is needed and the extra repository layer would only add confusion.

## 2. Package layout

| Package | Contents |
|---|---|
| config | PasswordConfig (BCrypt bean), WebConfig (registers the interceptor) |
| security | AuthInterceptor, TokenService, RoleRequired annotation, CurrentUser |
| model | User, Grievance, HistoryEntry and the enums Role, Status, Priority |
| dto | LoginRequest, RegisterRequest, GrievanceRequest, StatusUpdateRequest, AssignRequest, OfficerRequest, OfficerStats, GrievanceDetail and similar |
| repository | UserRepository, GrievanceRepository, HistoryRepository, TokenRepository, LookupRepository, StatsRepository |
| service | AuthService, GrievanceService, AssignmentService, OfficerService, StatsService |
| controller | AuthController, LookupController, CitizenController, OfficerController, AdminController |
| exception | ApiException, GlobalExceptionHandler |

### Naming conventions

- Java classes PascalCase, methods and variables camelCase, packages lower case. Use the correct spelling Officer and Grievance (the practice project had typos in these names).
- JSON fields camelCase. URLs lower case, with hyphens if needed.
- One class per file. A repository has no business logic, a controller has no SQL.

### application.properties

```text
spring.application.name=jansunwai
server.port=8080
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/jansunwai}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD}
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/schema.sql
spring.sql.init.data-locations=classpath:db/data.sql
```

Spring runs schema.sql and then data.sql on every start, which is why both files must be safe to repeat. The database password comes only from the `DB_PASSWORD` environment variable, so no secret is committed.

## 3. Authentication and security

### Login flow

1. The browser sends username, password and the role of the card the user clicked to `POST /api/auth/login`.
2. The server loads the user by lower-cased username. If the user is not found or the BCrypt check fails: HTTP 401 'Invalid username or password' (same message for both cases, so usernames cannot be discovered).
3. If the password is correct but the stored role differs from the requested role: HTTP 403 'This account is not an Admin account' (using the role name the user picked).
4. If the account is disabled: HTTP 403 'Account is disabled'.
5. Otherwise generate a 32-byte random token (SecureRandom), store it in auth_token with an 8 hour expiry, and return it with the user's basic profile.
6. The browser stores the token in sessionStorage and redirects by role.

### Protecting endpoints

- AuthInterceptor runs before every `/api/**` request except `/api/auth/login`, `/api/auth/register` and `/api/lookup/**`.
- It reads the `Authorization: Bearer` header, finds the token row, rejects missing, unknown or expired tokens with 401, and puts the user id and role on the request.
- A @RoleRequired annotation on each controller class lists the allowed roles. A wrong role gets 403.
- Ownership is checked in the service layer, not only by role: a citizen reads only their own grievances and an officer only their assigned ones.

### Security checklist

| Risk | Rule in this project |
|---|---|
| SQL injection | Parameters only. No string concatenation of request data into SQL. |
| Password leaks | BCrypt hashes only. Never log a password. Never return password_hash in any response. |
| Privilege escalation | Sign-up always creates a CITIZEN and ignores any role sent by the client. |
| Cross-site scripting | Frontend uses textContent for user-supplied text. |
| Secrets in Git | Database password only through DB_PASSWORD. application.properties holds no secret. |
| Broken access control | Every endpoint has a role check plus an ownership check where relevant. |
| Demo accounts | Seed passwords are for demo only and must be changed before any real deployment. |

## 4. Business rules

### Grievance status flow

| From | Allowed next status | Who | Remark |
|---|---|---|---|
| PENDING | IN_PROGRESS, RESOLVED, REJECTED | Assigned officer | Required |
| IN_PROGRESS | RESOLVED, REJECTED | Assigned officer | Required |
| RESOLVED | none (final in v1) | - | - |
| REJECTED | none (final in v1) | - | - |

- A new grievance is always created as PENDING and gets a first history row with the remark 'Grievance submitted'.
- When status becomes RESOLVED, resolved_at is set to now. Average resolution time is resolved_at minus created_at.
- An officer can only update grievances assigned to them. Attempts on others return 404 (not 403), so the system does not reveal that the ID exists.

### Automatic assignment

Runs inside the same transaction as the grievance insert. The auto-assignment query (see the Database Guide) returns the active officer with the fewest open grievances, ties broken by the lowest user id. Open means status PENDING or IN_PROGRESS.

1. Look for an active officer with the same city and category. Pick the one with the fewest open grievances.
2. If none, look for an active officer in the same city (any category), fewest open grievances.
3. If still none, leave assigned_officer_id empty. The grievance shows as Unassigned in the admin list and the citizen sees 'awaiting assignment'.

An assignment is recorded as a history row, for example 'Assigned to Mr. Sharma (automatic)', with changed_by empty because the system did it.

### Reassignment and officer deactivation

- Admin reassigns a grievance to any active officer. A history row is written with the current status and the remark 'Reassigned to X by admin' (or the admin's own remark).
- Disabling an officer blocks their login and unassigns their open grievances, so they appear as Unassigned and the admin can reassign them. Resolved and rejected grievances keep their officer for reporting.
- Enabling an officer again does not restore old assignments.

### Definitions

| Term | Definition |
|---|---|
| Overdue | Status is PENDING or IN_PROGRESS and created_at is more than 7 days ago. |
| Tracking ID | Text GRV- followed by the grievance id padded to 5 digits, for example GRV-00042. Generated by the database, never by the application. |
| Average resolution days | Average of (resolved_at - created_at) in days over RESOLVED grievances, rounded to 1 decimal. |
| City ranking | Cities ordered by number of PENDING grievances, highest first. |

### Validation rules

| Field | Rule |
|---|---|
| username | 4 to 30 characters: letters, digits and underscore. Unique. Stored and compared in lower case. |
| password | Minimum 8 characters. |
| email | Valid email format. Unique. |
| phone | Exactly 10 digits. |
| title | 5 to 150 characters. |
| description | 10 to 2000 characters. |
| area | 2 to 120 characters. |
| priority | LOW, MEDIUM or HIGH (default MEDIUM). |
| remark | 1 to 500 characters. Required on every status update. |

## 5. API contract

This is the agreement between the API developer and the UI developer. Change it only by editing this document and telling the team.

### Conventions

- Base path `/api`. JSON request and response bodies, UTF-8. Field names in camelCase.
- Authentication: header `Authorization: Bearer <token>` on every endpoint except those marked Public.
- Timestamps: ISO-8601 local time without zone, for example `2026-09-21T10:15:30`.
- Lists that are not paginated return a plain JSON array. Only `GET /api/admin/grievances` is paginated (page starting at 1, size default 10, maximum 50).
- Enum values are upper case: PENDING, IN_PROGRESS, RESOLVED, REJECTED; LOW, MEDIUM, HIGH; CITIZEN, OFFICER, ADMIN.
- Grievances are addressed by trackingId (for example GRV-00042), not by the numeric id.

### Error format and status codes

```json
{ "status": 400, "error": "Bad Request", "message": "Validation failed",
  "fieldErrors": { "phone": "must be exactly 10 digits" } }
```

| Code | Meaning | Example |
|---|---|---|
| 200 / 201 / 204 | Success / created / success with no body | Login, file grievance, logout |
| 400 | Validation error or illegal status change | Short title, RESOLVED to PENDING |
| 401 | Missing, invalid or expired token; wrong username or password | Bad login |
| 403 | Authenticated but not allowed | Citizen calls an admin endpoint, role mismatch at login |
| 404 | Not found, or not yours | Unknown tracking ID, another citizen's grievance |
| 409 | Conflict | Username or email already exists |
| 500 | Unexpected error. Message is generic; details only in the server log. | - |

### Endpoints: public and shared

| Method and path | Role | Purpose |
|---|---|---|
| POST /api/auth/register | Public | Citizen sign-up |
| POST /api/auth/login | Public | Login for all roles |
| POST /api/auth/logout | Any logged in | Delete the current token (204) |
| GET /api/me | Any logged in | Current user profile |
| GET /api/lookup/categories | Public | List of 12 categories |
| GET /api/lookup/cities | Public | List of cities |

### Endpoints: citizen

| Method and path | Purpose |
|---|---|
| POST /api/citizen/grievances | File a grievance (auto-assigned). Returns tracking ID. |
| GET /api/citizen/summary | Counts by status for the dashboard |
| GET /api/citizen/grievances?status= | Own grievances, newest first, optional status filter |
| GET /api/citizen/grievances/{trackingId} | Detail with history. 404 if not the citizen's own. |

### Endpoints: officer

| Method and path | Purpose |
|---|---|
| GET /api/officer/summary | assigned, pending, inProgress, resolved, overdue |
| GET /api/officer/grievances?status=&priority=&q= | Assigned grievances with filters. q searches title and area. |
| GET /api/officer/grievances/{trackingId} | Detail with citizen contact and history |
| PUT /api/officer/grievances/{trackingId}/status | Change status with remark. Returns updated detail. |

### Endpoints: admin

| Method and path | Purpose |
|---|---|
| GET /api/admin/summary | Totals, overdue, unassigned, grievances by category |
| GET /api/admin/stats/cities | Cities ranked by pending (city ranking query) |
| GET /api/admin/grievances?status=&categoryId=&cityId=&unassigned=&q=&page=&size= | Paginated list of all grievances |
| GET /api/admin/grievances/{trackingId} | Full detail with citizen, officer and history |
| PUT /api/admin/grievances/{trackingId}/assign | Reassign to an active officer |
| GET /api/admin/officers?cityId=&categoryId=&active=&q=&sort= | Officer directory with stats (officer statistics query). sort is pending (default), overdue, resolved or name. |
| GET /api/admin/officers/{userId} | Officer contact card and their open grievances |
| POST /api/admin/officers | Create an officer |
| PATCH /api/admin/officers/{userId}/active | Enable or disable. Disabling unassigns open grievances. |

### Request and response examples

### POST /api/auth/register (Public)

```text
Request : { "fullName": "Rahul Verma", "username": "rahul", "email": "rahul@example.com",
            "phone": "9876543210", "password": "Citizen@123" }
Response: 201 { "userId": 7, "username": "rahul", "role": "CITIZEN" }
Errors  : 400 validation, 409 { "message": "Username already taken" }
```

### POST /api/auth/login (Public)

```text
Request : { "username": "rahul", "password": "Citizen@123", "role": "CITIZEN" }
Response: 200 { "token": "9f2c...e1", "expiresAt": "2026-09-21T18:30:00",
                "user": { "userId": 7, "fullName": "Rahul Verma", "username": "rahul",
                          "role": "CITIZEN" } }
Errors  : 401 { "message": "Invalid username or password" }
          403 { "message": "This account is not an Admin account" }
```

### POST /api/citizen/grievances (Citizen)

```text
Request : { "title": "Pothole on MG Road", "categoryId": 1, "cityId": 2, "area": "Ward 4",
            "priority": "HIGH",
            "description": "Large pothole near the bus stop causing accidents." }
Response: 201 { "trackingId": "GRV-00043", "status": "PENDING", "priority": "HIGH",
                "assignedOfficer": { "userId": 2, "fullName": "Mr. Sharma" } }
          (assignedOfficer is null when no officer matched)
```

### GET /api/citizen/grievances/GRV-00037 (Citizen)

```json
{ "trackingId": "GRV-00037", "title": "No water for 3 days",
  "description": "No supply since Monday ...",
  "category": "Water Supply", "city": "Thane", "area": "Ward 12", "priority": "HIGH",
  "status": "IN_PROGRESS", "createdAt": "2026-09-15T10:02:00", "updatedAt": "2026-09-15T14:30:00",
  "assignedOfficer": { "fullName": "Ms. Patil", "phone": "9800000003" },
  "history": [
    { "status": "PENDING", "remark": "Grievance submitted", "changedBy": "Rahul Verma",
      "changedAt": "2026-09-15T10:02:00" },
    { "status": "PENDING", "remark": "Assigned to Ms. Patil (automatic)", "changedBy": "System",
      "changedAt": "2026-09-15T10:02:00" },
    { "status": "IN_PROGRESS", "remark": "Team sent to check pipeline", "changedBy": "Ms. Patil",
      "changedAt": "2026-09-15T14:30:00" } ] }
```

The officer version of this response adds `citizen: { fullName, phone, email }`. The admin version adds both the citizen and the full officer object.

### PUT /api/officer/grievances/GRV-00037/status (Officer)

```text
Request : { "status": "RESOLVED", "remark": "Pipeline repaired, supply restored" }
Response: 200 (same body as the officer detail response above)
Errors  : 400 { "message": "Cannot change status from RESOLVED to PENDING" }
          404 (grievance not assigned to this officer)
```

### PUT /api/admin/grievances/GRV-00044/assign (Admin)

```text
Request : { "officerId": 4, "remark": "Moved to Thane water team" }   (remark optional)
Response: 200 (admin detail response)
Errors  : 400 { "message": "Officer is not active" }
```

### GET /api/admin/officers?cityId=1&sort=pending (Admin)

```json
[ { "userId": 2, "fullName": "Mr. Sharma", "username": "sharma", "email": "sharma@example.com",
    "phone": "9800000001", "city": "Pune", "category": "Roads & Potholes", "active": true,
    "pending": 12, "inProgress": 5, "resolved": 40, "overdue": 3, "avgResolutionDays": 2.8 },
  ... ]
```

### GET /api/admin/stats/cities (Admin)

```json
[ { "cityId": 1, "city": "Pune",  "pending": 42, "inProgress": 12, "resolved": 130 },
  { "cityId": 4, "city": "Thane", "pending": 18, "inProgress": 7,  "resolved": 95 } ]
```

### GET /api/admin/summary (Admin)

```json
{ "total": 1240, "pending": 120, "inProgress": 140, "resolved": 960, "rejected": 20,
  "overdue": 18, "unassigned": 6,
  "byCategory": [ { "name": "Roads & Potholes", "count": 310 },
                  { "name": "Garbage & Cleanliness", "count": 220 } ] }
```

### POST /api/admin/officers (Admin)

```text
Request : { "fullName": "Mr. Kulkarni", "username": "kulkarni", "email": "k@example.com",
            "phone": "9800000009", "tempPassword": "Officer@123", "cityId": 1, "categoryId": 2 }
Response: 201 { "userId": 12, "username": "kulkarni", "role": "OFFICER" }
Errors  : 409 duplicate username or email
```
