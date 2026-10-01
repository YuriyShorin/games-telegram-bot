# Technology stack

Checked against primary project documentation on 2026-09-30. This document fixes the technology choices before game implementation. The scaffold's build and database integration checks have passed; live Telegram operation is not tested yet.

## Status and version policy

- **Owner decision:** Java 25, latest Kotlin, ktlint, PostgreSQL, Liquibase, and Hibernate. The Telegram integration is delegated for analysis and selection.
- **Working interpretation:** “latest” means latest stable release, excluding alpha, beta, RC, milestone, nightly, and SNAPSHOT builds. Java stays on major 25 by explicit instruction, with a current 25.x maintenance release at installation time.
- Pin exact versions in the eventual build and database image. Do not use dynamic `+` dependencies or a moving `latest` image tag. Latest means a checked version at an update, not a different dependency graph on every build.
- Recheck versions immediately before changing the build; the numbers below are a dated snapshot.
- **Confirmed owner answers:** use stable Spring Boot, Kotlin as the application language with JDK 25, and Docker for local PostgreSQL and integration tests.

## Required technologies

| Technology | Selected stable version | Role and evidence |
| --- | --- | --- |
| Java | 25, current maintenance patch | JDK and JVM target explicitly selected by the owner |
| Kotlin | 2.4.20 | Primary application language; [official release history](https://kotlinlang.org/docs/releases.html) |
| ktlint | 1.8.0 | Kotlin formatting and linting; latest stable rather than 2.0 alpha; [official releases](https://github.com/ktlint/ktlint/releases) |
| PostgreSQL | 18.6 | Persistent game and economy data; PostgreSQL 19 is still beta; [18.6 release notes](https://www.postgresql.org/docs/18/release-18-6.html) |
| Liquibase Community | 5.0.4 | Versioned schema migrations; [official download and release](https://www.liquibase.com/download-community) |
| Hibernate ORM | 7.4.11.Final | ORM and transactional persistence; [release and compatibility table](https://hibernate.org/orm/releases/7.4/) |

## Application, build, and supporting tools

The following tools are configured in the Kotlin scaffold. They do not introduce another persistence stack.

| Technology | Version | Purpose and evidence |
| --- | --- | --- |
| Spring Boot | 4.1.1 | Confirmed stable framework, replacing 4.2.0-SNAPSHOT; [stable requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Spring Data JPA | Managed by Spring Boot | JPA repositories and transactions backed by the selected Hibernate; align the Spring family |
| Gradle | 9.7.1 | Existing wrapper already points to this stable release; [official releases](https://gradle.org/releases/) |
| Gradle Kotlin DSL | With the selected Gradle | build.gradle.kts/settings.gradle.kts for the Kotlin project |
| ktlint Gradle plugin | 14.2.0 | Separate version from the ktlint engine; [plugin release](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint) |
| PostgreSQL JDBC driver | 42.7.13 | JDBC access for Hibernate/Liquibase; [official current driver](https://jdbc.postgresql.org/download/) |
| JUnit Jupiter / Platform | 6.1.3 | Unit and integration test framework, aligned through its BOM; [current guide](https://docs.junit.org/6.1.3/overview.html) |
| Testcontainers | 2.0.5 | PostgreSQL integration tests against the real database engine; [official dependencies](https://java.testcontainers.org/) |

**Confirmed:** Docker Compose for a local PostgreSQL instance and a Docker-compatible runtime for Testcontainers. Docker Engine must be running for container checks; the CLI alone is insufficient. No remote infrastructure choice is made here.

## Telegram: selected integration

**Selection under owner delegation:** official Telegram Bot API through the maintained Java SDK `rubenlagus/TelegramBots`, version **10.3.0**. Use the plain `telegrambots-longpolling` and `telegrambots-client` modules, aligned at the same SDK version (its BOM can keep modules together).

**Recommended initial update delivery:** long polling. It fits a single private-group bot without requiring a public inbound endpoint. **Updated owner decision:** players send native game emoji themselves. Native outcomes must come from incoming player game messages, rather than bot sendDice attempts. The Telegram flow enforces the one-minute attempt deadline and handles a missed deadline as technical defeat. Timing assumptions and remaining questions are in GAMES.md. Local pseudo-random numbers are only suitable as test fixtures, not substitutes for native animations.

| Option examined | Assessment |
| --- | --- |
| TelegramBots Java SDK | Selected: published 10.3.0 supports Bot API 10.3, reusable from Kotlin, ready-made request/update models and polling/client modules |
| kotlin-telegram-bot | Kotlin-oriented alternative, but its latest published release examined is 10.0.0, behind Bot API 10.3; its release notes describe a long prior release gap |
| Own HTTP client | Technically viable for a small subset, but would require maintaining request models, error handling, and update transport ourselves |

Sources: [TelegramBots 10.3.0 releases](https://github.com/rubenlagus/TelegramBots/releases), [Kotlin wrapper releases](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot/releases), [official Bot API methods](https://core.telegram.org/bots/api).

Do not add the Telegram SDK's Spring Boot starter by default: its 10.3.0 POM imports Spring Boot **3.5.5**, while the proposed application uses Boot **4.1.1**. This does not prove incompatibility, but it does not establish Boot 4 support either. Register and stop the plain polling module through application lifecycle instead. [Starter's versioned POM](https://github.com/rubenlagus/TelegramBots/blob/v10.3.0/telegrambots-springboot-longpolling-starter/pom.xml).

The SDK is not an official Telegram-maintained library; the underlying Bot API is official. Keep SDK request/update types at the Telegram boundary, separate from game rules and JPA entities.

## Compatibility checks still required

- Spring Boot 4.1.1 supports Java 25. Hibernate's 7.4 compatibility table lists Java 25 and Spring Boot 4.1. Those are documented family-level matches, not a test result for this application.
- Kotlin 2.4.20 lists Gradle 9.7.0 as the maximum fully supported version, while the latest Gradle patch is 9.7.1. Keep the requested latest stable patch, but verify compilation and plugin tasks. [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html).
- Boot 4.1.1's dependency table manages Hibernate 7.4.5.Final, Liquibase 5.0.3, and JUnit 6.0.3. The requested latest releases above therefore require explicit, aligned overrides. Keep all Hibernate modules aligned, all JUnit modules aligned, and Kotlin plugins/runtime aligned. [Boot managed versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html).
- Spring warns that overriding its tested dependency set can affect compatibility. Verify latest overrides with application startup, Liquibase migrations, PostgreSQL transactions, tests, and ktlint. If a real incompatibility appears, report it and agree on an exception rather than silently selecting an older release. [Boot dependency management](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html).
- Use Kotlin's Spring/JPA compiler plugins and explicit entity openness where required by Hibernate. Use ordinary entity classes; keep data-class value objects outside persistence entities.
- Liquibase owns schema changes; Hibernate validates the schema rather than updating or recreating it automatically. Currency uses exact decimal/minor-unit arithmetic, never floating-point balances.

## Scope of this step

The build and application scaffold now use the confirmed stack. Versions are explicit in build.gradle.kts; the database image is pinned in compose.yaml and the integration test. Liquibase creates an application schema; no game tables or game rules are implemented in this step. Telegram SDK dependencies are present, but polling is not enabled and no bot is connected. See [README.md](../../README.md) for startup and verification commands. Existing product documents remain the source of game rules.

## Recorded verification

- Gradle `check bootJar` passed after Kotlin/Gradle formatting corrections.
- Both PostgreSQL 18.6 integration tests passed, with zero failures, errors, or skips: Liquibase created the schema and recorded the migration, and Hibernate opened and committed a native-query transaction.
- Compose configuration validation passed. Testcontainers used an isolated database; existing databases were not modified.
- Packaged JAR contents confirm Kotlin 2.4.20, Boot 4.1.1, Hibernate 7.4.11.Final, Liquibase 5.0.4, pgJDBC 42.7.13, and TelegramBots 10.3.0.
- No live Telegram calls were made. Entity mappings, concurrency, currency settlement, and game behavior require their own checks when implemented.
