# Project instructions

This repository contains a Telegram multiplayer game bot.

Before implementation, read the product documentation under `docs/product/`.

Before adding or changing code, read and follow [docs/technical/NAMING.md](docs/technical/NAMING.md). It is the authoritative project reference for naming, entity/model correspondence, mappers, persistence mutability, and constants.

Use focused branches following `docs/technical/BRANCHING.md`; do not combine the whole game implementation in one branch.

Organize application code into shared top-level `service`, `repository`, `entity`, `config`, and `model` packages under `ru.gamestelegrambot`. Do not add feature-based package roots such as `economy` or `game.dice`. Keep persistence access in Spring Data JPA repositories, transaction boundaries and business rules in services, and return models separate from JPA entities. Use Hibernate for entity persistence; keep any necessary database-specific queries in repositories and document their purpose. Pure game state and value objects belong in `model`.

Do not make product decisions silently.

Use `var` for persisted properties in JPA entities and embeddables, including identifiers populated by the persistence provider. Do not imply persistence immutability with Kotlin `val`; use immutable `val` properties in application models instead. Never change an entity's primary key or embedded identifier after persistence, and never mutate identifiers used as map/set keys. Keep append-only column restrictions such as `updatable = false` where applicable.

Name JPA entities after their tables in singular PascalCase with the `Entity` suffix (for example, `pvp_wallet` -> `PvpWalletEntity`). Corresponding application models use the same base name without a suffix (`PvpWallet`). Keep entity-to-model mapping in separate files in the shared top-level `mapper` package, using extension functions such as `fun DuelBankEntity.toModel(): DuelBank`. Services call these mappers rather than declaring private `snapshot()` conversions. Embeddable identifiers are not entities and retain the `Id` suffix.

Avoid magic numbers in application logic. Declare fixed business values and reusable numeric parameters as named constants in a Kotlin `companion object` (`const val` for primitives, `val` for values such as `BigDecimal`). Reuse shared rules instead of duplicating their literals across services or persistence queries. Ordinary zero/one counters, test fixtures, and fixed migration/schema literals do not require mechanical extraction.

Keep each model in its own file. Game models hold state; game services implement round comparison, scoring, progression, and tiebreak rules. Services must not hold mutable state shared between matches.
If game rules are ambiguous, document the question or assumption first.

Keep changes focused on the current task.Теп
