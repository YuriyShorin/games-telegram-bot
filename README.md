# Games Telegram Bot

Telegram multiplayer game project. Product rules live in [docs/product](docs/product/PRODUCT_VISION.md); the selected technologies and version sources are in [TECH_STACK.md](docs/technical/TECH_STACK.md).

Code conventions are recorded in [NAMING.md](docs/technical/NAMING.md): entity and model names, packages, mappers, persistence properties, and constants. Contributors and coding agents must follow these rules; [AGENTS.md](AGENTS.md) links to them.

The development sequence, completed work, next tasks, and product questions that block individual steps are tracked in [IMPLEMENTATION_PLAN.md](docs/technical/IMPLEMENTATION_PLAN.md). Update it after each completed task so new sessions can continue from the recorded state.

The application configures Kotlin on Java 25, stable Spring Boot, PostgreSQL, Hibernate, Liquibase, ktlint, and the Telegram SDK. It includes the Dice match core, a persistent PvP wallet service, Dice duel invitations with authorized atomic acceptance, and persisted initial Dice duel state. Game commands and the Telegram polling lifecycle are not implemented yet; no token is needed to run checks.

## Prerequisites

- JDK 25, with JAVA_HOME pointing to that JDK.
- Docker Engine running with Linux containers for local PostgreSQL and integration tests.
- Use the committed Gradle wrapper; no separate Gradle installation is needed.

## Local application

```powershell
docker compose up -d --wait postgres
.\gradlew.bat bootRun
```

Liquibase creates the application schema, PvP wallets, duel invitations, Dice duels, duel banks, and the transaction ledger. Hibernate validates schema mappings and never creates or updates tables itself. The application has no Telegram worker yet and may exit after startup; this step verifies configuration, not a playable bot.

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

The Dice match core has unit tests for paired results, five/seven-round series, and until-victory matches with tiebreaks. It uses the documented trial rules; persistent duel services connect its results to PvP stakes. Telegram integration remains pending. Branch conventions and implementation scope are in [BRANCHING.md](docs/technical/BRANCHING.md).

The integration tests validate migrations, Hibernate mappings, and PvP wallet transactions against real PostgreSQL, including concurrent acceptance/settlement retries, overspending prevention, refunds, and daily recovery. They fail when Docker is unavailable; they are not silently skipped. Testcontainers starts an isolated database, so Compose does not need to be running for tests.

The internal `PvpWalletService` registers a group-local PvP balance, commits equal duel stakes, settles wins/forfeits or interruption refunds, and claims daily recovery. Callers must verify consent and trustworthy match results. Scope and unresolved rules are documented in [PVP_WALLET.md](docs/technical/PVP_WALLET.md). Solo wallets and tournament accounting are not implemented yet.

The internal `DuelInvitationService` persists Dice challenges and accepts them only from the invited opponent in the originating chat. Acceptance commits both stakes in the same transaction; failed or repeated acceptance cannot debit funds twice. Scope and lifecycle limits are documented in [DUEL_INVITATIONS.md](docs/technical/DUEL_INVITATIONS.md).

Acceptance also persists the initial Dice duel in that transaction. `DiceDuelService` reads its immutable state in the originating chat. `DiceAttemptService` prepares sequential requests with a saved random first player, starts a one-minute deadline after confirmed prompt delivery, and records native results idempotently. Complete pairs update the match through the game core. A decisive pair atomically records the winner and settles the PvP bank; retries cannot pay twice. Timeout execution and Telegram delivery remain subsequent tasks. Persistence and legacy-data handling are documented in [DICE_DUELS.md](docs/technical/DICE_DUELS.md).

On Linux/macOS use ./gradlew in place of .\gradlew.bat. Stop the local database with docker compose stop postgres; its named volume preserves data.
