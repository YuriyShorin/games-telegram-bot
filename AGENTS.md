# Project instructions

This repository contains a Telegram multiplayer game bot.

Before implementation, read the product documentation under `docs/product/`.

Use focused branches following `docs/technical/BRANCHING.md`; do not combine the whole game implementation in one branch.

Organize application code into shared top-level `service`, `repository`, `entity`, `config`, and `model` packages under `ru.gamestelegrambot`. Do not add feature-based package roots such as `economy` or `game.dice`. Keep persistence access in Spring Data JPA repositories, transaction boundaries and business rules in services, and return models separate from JPA entities. Use Hibernate for entity persistence; keep any necessary database-specific queries in repositories and document their purpose. Pure game state and value objects belong in `model`.

Do not make product decisions silently.

Keep each model in its own file. Game models hold state; game services implement round comparison, scoring, progression, and tiebreak rules. Services must not hold mutable state shared between matches.
If game rules are ambiguous, document the question or assumption first.

Keep changes focused on the current task.Теп
