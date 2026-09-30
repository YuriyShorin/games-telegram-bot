# Games Telegram Bot

Telegram multiplayer game project. Product rules live in [docs/product](docs/product/PRODUCT_VISION.md); the selected technologies and version sources are in [TECH_STACK.md](docs/technical/TECH_STACK.md).

The current scaffold configures Kotlin on Java 25, stable Spring Boot, PostgreSQL, Hibernate, Liquibase, ktlint, and the Telegram SDK. Game commands and the Telegram polling lifecycle are not implemented yet; no token is needed to check this scaffold.

## Prerequisites

- JDK 25, with JAVA_HOME pointing to that JDK.
- Docker Engine running with Linux containers for local PostgreSQL and integration tests.
- Use the committed Gradle wrapper; no separate Gradle installation is needed.

## Local application

```powershell
docker compose up -d --wait postgres
.\gradlew.bat bootRun
```

The initial Liquibase migration creates the application schema. Hibernate validates schema mappings and never creates or updates tables itself. The scaffold has no Telegram worker yet and may exit after startup; this step verifies configuration, not a playable bot.

Defaults are a local-only database named games_bot on port 5432 with user games_bot and password games_bot_local. Override DB_URL, DB_USERNAME, and DB_PASSWORD through environment variables. Compose accepts DB_PASSWORD and DB_PORT; when changing DB_PORT, also set DB_URL for the application. These defaults are for local development.

## Verification

```powershell
# Formatting, compilation, and packaging without Docker:
.\gradlew.bat ktlintCheck test bootJar

# Full verification, including PostgreSQL integration tests (Docker required):
.\gradlew.bat check

# Integration tests alone:
.\gradlew.bat integrationTest

# Format Kotlin sources and Gradle scripts:
.\gradlew.bat ktlintFormat
```

There are no game unit tests yet. The integration tests validate Liquibase migration execution and a Hibernate transaction against real PostgreSQL. They fail when Docker is unavailable; they are not silently skipped. Testcontainers starts an isolated database, so Compose does not need to be running for tests.

On Linux/macOS use ./gradlew in place of .\gradlew.bat. Stop the local database with docker compose stop postgres; its named volume preserves data.
