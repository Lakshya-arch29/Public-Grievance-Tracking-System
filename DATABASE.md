# Database Guide

*PostgreSQL design, schema, seed plan and key queries*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

## 1. Overview

Database name `jansunwai`, PostgreSQL 14 or newer (the design was written and checked against PostgreSQL 16). Six tables. Officers must have a city and a category, enforced by a CHECK constraint.

PostgreSQL features the design relies on: FILTER aggregates (used in the statistics queries) and generated columns (used for the tracking ID).

### Relationships

```text
city 1 ------< users (officers: city_id)         category 1 ------< users (officers: category_id)
city 1 ------< grievance                         category 1 ------< grievance
users 1 ------< grievance (citizen who filed it: user_id)
users 1 ------< grievance (assigned officer: assigned_officer_id, optional)
grievance 1 ------< grievance_history            users 1 ------< grievance_history (changed_by)
users 1 ------< auth_token
```

## 2. Tables

### city and category (lookup tables)

Seeded once. Grievances and officers reference them, so counts by city or category are exact. Free text would allow 'Pune' and 'pune' to be counted separately.

### users

| Column | Type | Notes |
|---|---|---|
| user_id | SERIAL, PK |  |
| full_name, username, email, phone | VARCHAR | username and email are UNIQUE |
| password_hash | VARCHAR(100) | BCrypt hash. Plain passwords are never stored or logged. |
| role | VARCHAR(10) | CHECK in CITIZEN, OFFICER, ADMIN |
| city_id, category_id | INT, FK, nullable | Required for OFFICER (CHECK constraint), empty for others |
| active | BOOLEAN | Default true. Disabled users cannot log in. |
| created_at | TIMESTAMP | Default now |

### grievance

| Column | Type | Notes |
|---|---|---|
| grievance_id | SERIAL, PK |  |
| tracking_id | VARCHAR(20), generated | 'GRV-' plus id padded to 5 digits. UNIQUE. Generated always, stored. |
| user_id | INT, FK users | The citizen who filed it |
| category_id, city_id | INT, FK | Both required |
| area, title, description | VARCHAR, TEXT | area is free text within the city (ward, street) |
| priority | VARCHAR(10) | CHECK LOW, MEDIUM, HIGH |
| status | VARCHAR(12) | CHECK PENDING, IN_PROGRESS, RESOLVED, REJECTED |
| assigned_officer_id | INT, FK users, nullable | Empty means unassigned |
| created_at, updated_at, resolved_at | TIMESTAMP | resolved_at is set only when RESOLVED |

### grievance_history

One row per event: submission, automatic assignment, status change, reassignment. This table is the timeline shown to the citizen. Columns: history_id, grievance_id (FK, cascade delete), status, remark, changed_by (FK users, NULL when the system did it, for example automatic assignment), changed_at.

### auth_token

One row per active login. `token` is a 64-character hex string generated from 32 random bytes. `expires_at` is created_at plus 8 hours. Logout deletes the row. Expired rows are ignored and can be deleted lazily.

## 3. schema.sql

Safe to run repeatedly (IF NOT EXISTS everywhere). Location: `src/main/resources/db/schema.sql`. These statements were run against PostgreSQL 16 when the planning document was written and are carried over unchanged. Re-running the file a second time should produce no errors. Confirm this on your own machine as the first check.

```sql
CREATE TABLE IF NOT EXISTS city (
    city_id  SERIAL PRIMARY KEY,
    name     VARCHAR(80) NOT NULL UNIQUE,
    state    VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    category_id SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    user_id       SERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    phone         VARCHAR(15)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL
                  CHECK (role IN ('CITIZEN', 'OFFICER', 'ADMIN')),
    city_id       INT REFERENCES city(city_id),
    category_id   INT REFERENCES category(category_id),
    active        BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CHECK (role <> 'OFFICER' OR (city_id IS NOT NULL AND category_id IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS grievance (
    grievance_id        SERIAL PRIMARY KEY,
    tracking_id         VARCHAR(20) GENERATED ALWAYS AS
                        ('GRV-' || LPAD(grievance_id::text, 5, '0')) STORED UNIQUE,
    user_id             INT NOT NULL REFERENCES users(user_id),
    category_id         INT NOT NULL REFERENCES category(category_id),
    city_id             INT NOT NULL REFERENCES city(city_id),
    area                VARCHAR(120) NOT NULL,
    title               VARCHAR(150) NOT NULL,
    description         TEXT NOT NULL,
    priority            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status              VARCHAR(12) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    assigned_officer_id INT REFERENCES users(user_id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grievance_history (
    history_id   SERIAL PRIMARY KEY,
    grievance_id INT NOT NULL REFERENCES grievance(grievance_id) ON DELETE CASCADE,
    status       VARCHAR(12) NOT NULL
                 CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    remark       VARCHAR(500) NOT NULL,
    changed_by   INT REFERENCES users(user_id),   -- NULL = done by the system
    changed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth_token (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_grievance_user        ON grievance(user_id);
CREATE INDEX IF NOT EXISTS idx_grievance_officer     ON grievance(assigned_officer_id);
CREATE INDEX IF NOT EXISTS idx_grievance_status      ON grievance(status);
CREATE INDEX IF NOT EXISTS idx_grievance_city_status ON grievance(city_id, status);
CREATE INDEX IF NOT EXISTS idx_history_grievance     ON grievance_history(grievance_id);
```

## 4. Seed data (data.sql)

Written at build time. Location: `src/main/resources/db/data.sql`.

- 12 categories: Roads & Potholes, Garbage & Cleanliness, Water Supply, Electricity & Street Lights, Drainage & Sewage, Pollution, Public Transport, Parks & Public Spaces, Stray Animals, Health & Hospitals, Illegal Construction & Encroachment, Other.
- 8 cities: Mumbai, Pune, Nagpur, Thane, Nashik, Delhi, Bengaluru, Hyderabad.
- 1 admin, 6 officers (spread across cities and categories), 4 citizens. Passwords are pre-computed BCrypt hashes.
- About 25 sample grievances across all statuses with history rows, so dashboards look real on the first run.
- The seed is idempotent: lookup rows and users are inserted only if missing, and sample grievances only when the grievance table is empty. Restarting the app never creates duplicates.

> Spring runs schema.sql and then data.sql on every start, so idempotent seeding is required, not optional. Test it by starting the app twice.

## 5. Key queries

Four queries carry the interesting logic. In the officer statistics and auto-assignment queries, the `:name` values are NamedParameterJdbcTemplate parameters.

### Officer statistics (admin Officers table)

Returns one row per officer with pending, in-progress and resolved counts, the overdue count and average resolution days. `:cityId` may be NULL, meaning all cities. The CAST is needed because PostgreSQL cannot infer the type of a NULL parameter used in an IS NULL test.

```sql
SELECT u.user_id, u.full_name, u.username, u.email, u.phone,
       c.name AS city, cat.name AS category, u.active,
       COUNT(*) FILTER (WHERE g.status = 'PENDING')      AS pending,
       COUNT(*) FILTER (WHERE g.status = 'IN_PROGRESS')  AS in_progress,
       COUNT(*) FILTER (WHERE g.status = 'RESOLVED')     AS resolved,
       COUNT(*) FILTER (WHERE g.status IN ('PENDING', 'IN_PROGRESS')
                          AND g.created_at < NOW() - INTERVAL '7 days') AS overdue,
       ROUND((AVG(EXTRACT(EPOCH FROM (g.resolved_at - g.created_at)) / 86400)
              FILTER (WHERE g.status = 'RESOLVED'))::numeric, 1) AS avg_days
FROM users u
JOIN city c        ON c.city_id = u.city_id
JOIN category cat  ON cat.category_id = u.category_id
LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
WHERE u.role = 'OFFICER'
  AND (CAST(:cityId AS INTEGER) IS NULL OR u.city_id = CAST(:cityId AS INTEGER))
GROUP BY u.user_id, c.name, cat.name
ORDER BY pending DESC, u.full_name;
```

The category, active and name-search filters and the alternative sort orders are added by the repository. Sort choices must come from a fixed whitelist mapped to column names in Java, never from the raw request value, because ORDER BY cannot use bound parameters.

### Cities ranked by pending grievances

Feeds the 'cities with most pending cases' strip and the admin dashboard.

```sql
SELECT c.city_id, c.name AS city,
       COUNT(*) FILTER (WHERE g.status = 'PENDING')     AS pending,
       COUNT(*) FILTER (WHERE g.status = 'IN_PROGRESS') AS in_progress,
       COUNT(*) FILTER (WHERE g.status = 'RESOLVED')    AS resolved
FROM city c
LEFT JOIN grievance g ON g.city_id = c.city_id
GROUP BY c.city_id, c.name
ORDER BY pending DESC, c.name;
```

### Auto-assignment: least-loaded officer for city and category

Picks the active officer with the fewest open grievances for a city and category, ties broken by the lowest user id. Returns no row when nobody matches.

```sql
SELECT u.user_id
FROM users u
LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
                     AND g.status IN ('PENDING', 'IN_PROGRESS')
WHERE u.role = 'OFFICER' AND u.active
  AND u.city_id = :cityId AND u.category_id = :categoryId
GROUP BY u.user_id
ORDER BY COUNT(g.grievance_id), u.user_id
LIMIT 1;
```

### Auto-assignment fallback: same city, any category

Business rule step 2 needs this second lookup, which is the same query without the category condition. The service runs the first query, and only if it returns no row runs this one.

```sql
SELECT u.user_id
FROM users u
LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
                     AND g.status IN ('PENDING', 'IN_PROGRESS')
WHERE u.role = 'OFFICER' AND u.active
  AND u.city_id = :cityId
GROUP BY u.user_id
ORDER BY COUNT(g.grievance_id), u.user_id
LIMIT 1;
```

> **Verification status:** the four queries above the fallback come from the tested planning document. The fallback query is new in this split edition and has not been executed yet. Run it against the seed data at the first database checkpoint and check that it returns the least-loaded officer of the city.

### Category breakdown (admin dashboard)

```sql
SELECT cat.name, COUNT(g.grievance_id) AS total
FROM category cat
LEFT JOIN grievance g ON g.category_id = cat.category_id
GROUP BY cat.category_id, cat.name
ORDER BY total DESC, cat.name;
```

### What to check when you run them

- Officer statistics: the counts match the grievances you inserted for that officer, and passing a city id returns only that city's officers.
- City ranking: cities appear ordered by pending count, and a city with no grievances still appears with zeros.
- Auto-assignment: an officer with no open cases is chosen over one with two, and no row comes back when no officer matches.
- Re-running schema.sql a second time produces no errors.

## 6. Repository rules

- Repositories contain SQL and row mapping only. No business rules.
- Use NamedParameterJdbcTemplate with RowMapper. Never build SQL by joining request strings.
- Multi-table changes (update grievance plus insert history) are called from a @Transactional service method, not committed separately.
- The grievance insert must read back the generated `tracking_id` (use RETURNING or KeyHolder), because the application never builds it.
- Never select password_hash into any object that is returned to the client.
