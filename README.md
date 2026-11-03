# FinEdge Backend

Spring Boot 3 backend for the FinEdge Enterprise Finance Management Portal.
Provides REST APIs under `/api/etfdb/...` with stateless JWT authentication
and Flyway-managed database migrations.

## Prerequisites

- Java 17 (JDK)
- Maven 3.9.6 or compatible (installed independently; the Maven wrapper
  `mvnw` / `mvnw.cmd` is present but its `distributionUrl` is invalid and
  should not be relied upon until repaired)
- A MySQL instance for the `local` and `etfdb` profiles (the `default` profile
  uses an in-memory H2 database and requires no external database)

## Build

```bash
mvn clean install
```

On Windows with Maven 3.9.6:

```cmd
"C:\Users\mohan\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" clean install
```

Run tests only:

```bash
mvn test
```

On Windows:

```cmd
"C:\Users\mohan\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" test
```

`target/` is build output and is gitignored; it must not be committed.

## Local startup

Zero-setup mode (default profile, H2 in-memory database):

```bash
mvn spring-boot:run
```

On Windows:

```cmd
"C:\Users\mohan\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
```

The default profile (`application.yml`) activates automatically and starts on
`http://localhost:8080`. No database password is required; data is lost when
the process stops.

Local MySQL mode (`local` profile):

```bash
mvn spring-boot:run --spring.profiles.active=local
```

On Windows:

```cmd
"C:\Users\mohan\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run --spring.profiles.active=local
```

Requires a MySQL database named `finedgedb` and the environment variables below.

ETFDB local MySQL mode (`etfdb` profile, dedicated `etfdb_local` database):

```bash
mvn spring-boot:run --spring.profiles.active=etfdb
```

On Windows:

```cmd
"C:\Users\mohan\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run --spring.profiles.active=etfdb
```

Requires a MySQL database named `etfdb_local` and `DB_PASSWORD` (no default;
startup fails fast if missing).

## Spring profiles and verified purposes

| Profile | Config file | Purpose | Database |
|---|---|---|---|
| `default` | `application.yml` | Zero-setup / dev mode | H2 in-memory |
| `local` | `application-local.yml` | Local MySQL development | `finedgedb` |
| `etfdb` | `application-etfdb.yml` | Dedicated local ETFDB instance | `etfdb_local` |
| `lambda` | `application-lambda.yml` | AWS Lambda + API Gateway | MySQL via env vars |

Only profiles listed above exist in the project. Do not activate any other
profile name.

## Required environment variables

Configure secrets through environment variables, never in source files.

| Variable | Profile(s) | Notes |
|---|---|---|
| `DB_PASSWORD` | `local`, `etfdb`, `lambda` | Required; no default. Missing value fails startup. |
| `DB_USERNAME` | `local`, `etfdb`, `lambda` | Defaults: `root` (local), `etfdb_app` (etfdb), required (lambda) |
| `DB_HOST` | `local`, `etfdb`, `lambda` | Default `localhost` |
| `DB_PORT` | `local`, `etfdb`, `lambda` | Default `3306` |
| `DB_NAME` | `local`, `etfdb`, `lambda` | Defaults: `finedgedb` (local/lambda), `etfdb_local` (etfdb) |
| `FINEDGE_JWT_SECRET` | All | Base64-encoded signing key of at least 256 bits. **Required.** No development fallback is provided; startup fails if unset. |
| `FINEDGE_JWT_EXPIRATION_MS` | All | Default `900000` (15 minutes) |
| `FINEDGE_JWT_ISSUER` | All | Default `finedge` |
| `FINEDGE_AUTH_DEFAULT_ORG_ID` | All | Default `ORG-T001` |
| `FINEDGE_AUTH_DEFAULT_BORROWER_ROLE_ID` | All | Default `ROLE-T04` |

## Database and Flyway migration setup

Flyway is enabled in all profiles and runs migrations from
`classpath:db/migration`. Migrations are versioned V1 through V11 and create
the schema plus seed data. `hibernate.ddl-auto` is set to `validate`, so the
application starts only if the schema matches the migrations.

On first startup against an empty database, Flyway applies the full migration
chain automatically. No manual SQL execution is required.

## Local API base URL

All API endpoints are served at:

```
http://localhost:8080/api/etfdb/...
```

Public auth endpoints (`/api/etfdb/auth/**`) do not require authentication.
All other endpoints require a valid JWT.

## Configuring without committing credentials

- All database passwords and the JWT signing key are read from environment
  variables (`${DB_PASSWORD}`, `${FINEDGE_JWT_SECRET}`, etc.).
- `application-local.yml` and `application-etfdb.yml` use `${DB_PASSWORD}`
  with no default, so startup fails fast if the variable is unset.
- Never commit `.env` files or hardcoded credentials. `.env*` and `*.env` are
  gitignored.
