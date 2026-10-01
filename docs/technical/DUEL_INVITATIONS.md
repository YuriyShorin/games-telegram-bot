# Duel invitations

Branch: `feat/duel-invitations`, based on `master` with the merged Dice engine and PvP wallet.

This step implements a persistent application boundary for Dice challenges and acceptance, ahead of Telegram commands and attempt handling. It does not implement the entire Telegram duel flow. Callers supply the authenticated actor and originating chat; SDK types do not enter business services.

The invitation publishes two distinct participants, an equal stake, and a series format. Creation does not reserve funds or register players automatically. Acceptance is permitted only to the invited opponent in the same chat and commits both registered wallets atomically. Insufficient funds leave the invitation pending and move no money. A repeated acceptance returns the accepted invitation without committing again. Reusing a challenge ID with different terms is rejected.

An invitation row lock serializes acceptance. Lock order is invitation, bank, then wallets in ascending player ID order. PostgreSQL `ON CONFLICT DO NOTHING` in the repository makes concurrent creation retries safe; other persistence uses Hibernate and JPQL. The invitation ID is also its bank ID. The subsequent [persistent Dice duel step](DICE_DUELS.md) now creates initial match state in the acceptance transaction; attempts and match-driven settlement remain future tasks.

No expiration, decline, cancellation, active-match limits, or new timeout rules are introduced in this step. These behaviors require subsequent lifecycle work. Existing unanswered questions about paired prompt order and deadline timing remain in [GAMES.md](../product/GAMES.md).

The existing product/technology documentation edits are retained on this renamed branch as requested by the owner.

## Verification

`gradlew.bat check bootJar` passed on 2026-10-01, including ktlint, compilation, packaging, 8 Dice unit tests, and 16 PostgreSQL integration tests (5 invitations, 9 wallets, 2 stack), with no failures or skips. Invitation tests cover authorization, group isolation, unchanged terms, fractional amounts, rollback and retry after insufficient funds, missing registration, and concurrent creation/acceptance. Liquibase migration execution and Hibernate validation run against PostgreSQL 18.6. No live Telegram calls were made.
