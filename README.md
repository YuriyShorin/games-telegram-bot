# Games Telegram Bot

Telegram multiplayer game project. Product rules live in [docs/product](docs/product/PRODUCT_VISION.md); the selected technologies and version sources are in [TECH_STACK.md](docs/technical/TECH_STACK.md).

The application configures Kotlin on Java 25, stable Spring Boot, PostgreSQL, Hibernate, Liquibase, ktlint, and the Telegram SDK. It includes the Dice match core and a persistent PvP wallet service. Game commands and the Telegram polling lifecycle are not implemented yet; no token is needed to run checks.

## Prerequisites

- JDK 25, with JAVA_HOME pointing to that JDK.
- Docker Engine running with Linux containers for local PostgreSQL and integration tests.
- Use the committed Gradle wrapper; no separate Gradle installation is needed.

## Local application

```powershell
docker compose up -d --wait postgres
.\gradlew.bat bootRun
```

Liquibase creates the application schema, PvP wallets, duel banks, and the transaction ledger. Hibernate validates schema mappings and never creates or updates tables itself. The application has no Telegram worker yet and may exit after startup; this step verifies configuration, not a playable bot.

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

The Dice match core has unit tests for paired results, five/seven-round series, and until-victory matches with tiebreaks. It uses the documented trial rules and is not connected to Telegram or stakes yet. Branch conventions and implementation scope are in [BRANCHING.md](docs/technical/BRANCHING.md).

The integration tests validate migrations, Hibernate mappings, and PvP wallet transactions against real PostgreSQL, including concurrent acceptance/settlement retries, overspending prevention, refunds, and daily recovery. They fail when Docker is unavailable; they are not silently skipped. Testcontainers starts an isolated database, so Compose does not need to be running for tests.

The internal `PvpWalletService` registers a group-local PvP balance, commits equal duel stakes, settles wins/forfeits or interruption refunds, and claims daily recovery. Callers must verify consent and trustworthy match results. Scope and unresolved rules are documented in [PVP_WALLET.md](docs/technical/PVP_WALLET.md). Solo wallets and tournament accounting are not implemented yet.

On Linux/macOS use ./gradlew in place of .\gradlew.bat. Stop the local database with docker compose stop postgres; its named volume preserves data.
