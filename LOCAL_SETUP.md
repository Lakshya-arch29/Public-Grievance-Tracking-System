# Local Setup Guide

*Install, configure and run JanSunwai on your own laptop*

> JanSunwai Public Grievance Tracking System | Version 1.1 (split edition) | 21 September 2026

> This guide applies once the project code exists in the repository. Follow it top to bottom on every team member's laptop at the start of the project (checkpoint T+0 to T+1).

## 1. What you need

| Tool | Version | Check with |
|---|---|---|
| JDK | 21 (all three members must use the same major version) | java -version |
| PostgreSQL | 14 or newer | psql --version |
| Git | any recent version | git --version |
| Maven | Not needed, the wrapper is included | ./mvnw -v |
| IDE | IntelliJ IDEA, or VS Code with Java extensions | - |
| DB viewer (optional) | pgAdmin or DBeaver | - |
| API tester (optional) | Postman or curl | curl --version |

## 2. First-time setup

### Step 1: get the code

```bash
git clone <repository-url>
cd jansunwai
```

Open the `jansunwai` folder in your IDE and let it import the Maven project.

### Step 2: check Java

```bash
java -version
```

The output must show version 21. If it shows an older version, install JDK 21 and point `JAVA_HOME` at it (see Troubleshooting).

### Step 3: create the database (once)

```bash
psql -U postgres -c "CREATE DATABASE jansunwai;"
```

You only create the empty database. The tables and demo data are created by the application at startup.

### Step 4: set the database password

The password is read from an environment variable so it is never committed to Git. Set it in the same terminal you will use to start the app.

| System | Command |
|---|---|
| Windows PowerShell | `$env:DB_PASSWORD="your_password"` |
| macOS or Linux | `export DB_PASSWORD=your_password` |

Optional variables: `DB_USER` (default `postgres`) and `DB_URL` (default `jdbc:postgresql://localhost:5432/jansunwai`).

### Step 5: start the application

```bash
./mvnw spring-boot:run          (macOS or Linux)
mvnw.cmd spring-boot:run        (Windows)
```

The first start downloads dependencies, so it needs internet and may take a few minutes. Wait for the line saying Spring Boot has started on port 8080.

### Step 6: open the app

Go to `http://localhost:8080`. You should see the three role cards. Log in with a demo account from the README, for example Admin with `admin` and `Admin@123`.

## 3. Quick verification (5 minutes)

Do this on every laptop before anyone writes more code.

1. The role picker page opens at localhost:8080.
2. Log in as `admin` on the Admin card. The dashboard shows non-zero numbers (proves the seed data loaded).
3. Stop the app and start it again. Log in again and confirm the numbers did not double (proves the seed is safe to repeat).
4. In pgAdmin or psql, run `\dt` in the jansunwai database. You should see six tables: city, category, users, grievance, grievance_history, auth_token.
5. Run `./mvnw test` and confirm the build passes.

Optional command-line check that the API answers (no login needed):

```bash
curl http://localhost:8080/api/lookup/cities
```

It should return a JSON list of the eight seeded cities.

## 4. Everyday commands

| Task | Command |
|---|---|
| Run the app | ./mvnw spring-boot:run |
| Run the tests | ./mvnw test |
| Build a runnable jar | ./mvnw clean package |
| Run the built jar | java -jar target/jansunwai-0.0.1-SNAPSHOT.jar |
| Open a psql shell | psql -U postgres -d jansunwai |
| List tables | \dt  (inside psql) |
| Use another port | add server.port=8081 in application.properties, or start with --server.port=8081 |

### Reset all data

Stop the app first, then recreate the database and restart:

```bash
psql -U postgres -c "DROP DATABASE jansunwai;"
psql -U postgres -c "CREATE DATABASE jansunwai;"
./mvnw spring-boot:run
```

### Working on the frontend only

Static files are served straight from `src/main/resources/static`. After changing a page, refresh the browser with Ctrl+Shift+R because the browser caches static files. With IntelliJ you may need to rebuild the resources, or restart the app, for the change to appear.

## 5. Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| Password authentication failed | DB_PASSWORD is not set in the same terminal that runs the app, or the password is wrong. Set it again and start the app from that terminal. |
| Connection refused on 5432 | PostgreSQL is not running or uses another port. Start the service, or set DB_URL to the right port. |
| database "jansunwai" does not exist | Run the CREATE DATABASE command from step 3. |
| Port 8080 already in use | Stop the other program or set server.port=8081. |
| Unsupported class file major version | A JDK older than 21 is active. Check java -version and JAVA_HOME, then restart the terminal or IDE. |
| Cannot resolve dependencies | No internet, or a proxy is blocking Maven Central. Connect and retry. |
| mvnw: permission denied (macOS, Linux) | Run chmod +x mvnw once. |
| Changed a page but nothing changed | Hard refresh with Ctrl+Shift+R. Static files are cached by the browser. |
| Everyone sees different data | Each laptop has its own local database. That is expected. Agree on seed data, not on live data. |
| Duplicate key error at startup | The seed script is not idempotent. Fix data.sql so it inserts only when rows are missing, then reset the database. |
| Login always says 'Invalid username or password' | Seed password hashes do not match the demo passwords. Regenerate the BCrypt hashes in data.sql. |

## 6. Installing the tools (if missing)

### JDK 21

- Windows or macOS: download an OpenJDK 21 build (for example Temurin) and run the installer.
- Linux: install through your package manager, for example openjdk-21-jdk.
- Afterwards open a new terminal and run java -version.

### PostgreSQL

- Install PostgreSQL 14 or newer and remember the password you set for the postgres user. That is your DB_PASSWORD.
- Make sure the psql command is on your PATH. On Windows, add the PostgreSQL bin folder to PATH.
- Confirm the service is running before starting the app.
