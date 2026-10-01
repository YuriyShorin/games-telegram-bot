# PvP wallet implementation

Branch: `feat/pvp-wallet`. Rules: [ECONOMY.md](../product/ECONOMY.md) and [PLAYTEST_RULES.md](../product/PLAYTEST_RULES.md).

This step implements group-local PvP registration (1,000 once), available and committed funds, equal duel stakes (minimum 10), winner/forfeit settlement, interruption refunds, and voluntary daily recovery. Recovery uses wealth including commitments, the 300 threshold, up to 100, and the Europe/Moscow calendar day. Solo and cups remain separate implementation tasks; no solo funds are created by PvP registration.

Amounts use exact decimal arithmetic with at most two fractional digits. A duel identifier is an idempotency key: retries must have identical group, participants in the same order, and stake. Repeating an identical settlement returns the existing result; a conflicting settlement is rejected. Failed acceptance rolls back both commitments. Database row locks serialize concurrent spending and settlement; wallet locks follow ascending player ID order. Starting grants, commitments, payouts, refunds, and recovery are recorded in an append-only ledger within the same transaction.

Duel-to-wallet foreign keys are checked at commit. This avoids acquiring wallet foreign-key locks before the ordered write locks when creating concurrent banks. Monetary columns are unconstrained PostgreSQL NUMERIC with explicit nonnegative/two-decimal checks, rather than imposing a product stake cap through a fixed precision.

Registration is explicit before acceptance. The service is an internal application boundary, not a Telegram authorization layer. Its callers must verify consent, winner evidence, withdrawals, and genuine interruptions. No public command or automatic recovery claim is enabled here.

## Package structure and persistence

Persistence classes follow table names with an `Entity` suffix: `PvpWalletEntity`, `DuelBankEntity`, and `PvpLedgerEntity`. Returned wallet and bank models share their base names: `PvpWallet` and `DuelBank`. Separate extension mappers in the shared `mapper` package convert entities with `toModel()`; services own business rules rather than mapping definitions. `WalletId` remains an embeddable identifier, not a JPA entity. Database table names and migrations are unchanged by this naming refactor.

Application classes live in shared `service`, `repository`, `entity`, `config`, and `model` packages directly under `ru.gamestelegrambot`, without feature-based roots. `PvpWalletService` owns business rules and transaction boundaries. Spring Data JPA repositories own persistence queries and pessimistic locks. Hibernate tracks wallet/bank updates and inserts ledger entities; the ledger repository exposes append operations only. Return models and pure Dice match state live in `model`, separate from persistence entities. Tests mirror the corresponding application packages.

Dice models (`DiceMatch`, `DiceRound`, `SeriesFormat`, and `MatchSide`) each have their own file. `DiceMatchService` validates and compares paired results, updates scores, and resolves series/tiebreaks. It holds no match state; `recordRound` returns a new immutable `DiceMatch`, leaving the previous state intact.

Two native PostgreSQL inserts remain in repositories: wallet registration and duel acceptance use `ON CONFLICT DO NOTHING` for atomic idempotency under concurrent requests. A check followed by an ordinary `save` would race, and a uniqueness error would invalidate the transaction. Other operations use JPA entity persistence and JPQL. These queries do not introduce a second persistence stack.

Open product question: double forfeits have no approved settlement rule. This step exposes a single-player forfeit only and does not infer double forfeits, inactivity, or chat departure. A future match lifecycle must resolve this question before exposing those events.

## Verification

Final `gradlew.bat check bootJar` passed: 8 Dice service tests, 9 PvP wallet PostgreSQL integration tests, and 2 stack integration tests, with no failures or skips. The Dice tests cover independent matches and immutable state transitions as well as series rules. The wallet tests cover concurrent registration, acceptance and settlement retries, overlapping stakes, atomic rollback on insufficient funds, fractional amounts, ledger reconciliation, forfeits, refunds, and voluntary recovery across Moscow midnight. Formatting checks, Hibernate schema validation, and Liquibase migration execution are included. No live Telegram calls were made.
