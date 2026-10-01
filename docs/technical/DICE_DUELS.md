# Persistent Dice duels

Branch: `feat/persistent-dice-duel`, based on `master`. The documentation plan was retained on this branch at the owner's request.

D1 implements initial persistent match state and reading it and is merged into `master`. D2 on `feat/dice-duel-attempts` adds persistent sequential attempt requests and results. Bank settlement, timeout execution, and Telegram integration remain subsequent tasks.

## Acceptance and persistence

`DuelInvitationService.accept` preserves its invitation return type. In one transaction it locks the invitation, authorizes the actor/chat, commits both stakes using `PvpWalletService`, marks the invitation accepted, and saves the initial `DiceDuelEntity`. A failure to save the duel rolls back the invitation, bank, wallet changes, and ledger. An already accepted invitation is returned without creating or resetting a match or committing stakes again. A pending invitation cannot start a match against an already settled bank.

The `dice_duel` primary key is the invitation/bank UUID. Foreign keys require both records to exist. The JPA invitation association uses `@MapsId`; identifiers never change after persistence. Participant order, chat, stake, and format are read from the invitation rather than duplicated in the match table. The service validates bank terms against the invitation when committing stakes.

The table stores round count, scores, winner side, and tiebreak state. A nullable JPA `@Version` property identifies a new entity for Spring Data persistence and protects future updates against stale writes; Hibernate initializes its nonnull database value on insertion. New duels start with zero rounds/scores, no winner, and no tiebreak, matching `DiceMatch` defaults. Future attempt handling must update this state through the existing rules service. Acceptance does not assign an attempt order or start a deadline.

`DiceDuelService.find(duelId, chatId)` returns an immutable `DiceDuel` containing a `DiceMatch` through the separate mapper. It rejects a missing duel or a different chat. It is an internal application boundary: the future Telegram adapter must verify access to the originating chat. No participant-only restriction or new spectator policy is introduced.

Persistence uses Spring Data JPA and Hibernate. The existing lock order remains invitation, bank, wallets in ascending player ID order; the new match is saved after the commitments while the invitation lock is still held. Match state is not held in service memory.

## Upgrade from the previous version

Migration 004 creates initial state for accepted invitations whose banks are still `LOCKED`. The previous application could accept stakes but could not start a match or record attempts, so these records have no game progress to preserve.

Already settled legacy banks are excluded: their wallet settlement does not establish a trustworthy Dice match result. Their invitations remain readable and repeat acceptance remains a no-op; there is no fabricated match history. Standalone banks created directly through the wallet service also do not create matches. Existing bank/ledger/wallet values are not changed by this migration.

The game state is not yet synchronized with calls that settle banks directly through the internal wallet service. D3 must make actual match completion and bank settlement atomic; the future lifecycle must similarly coordinate forfeits and interruptions before exposing those operations to Telegram.

## Sequential attempts (D2)

The owner agreed on sequential Dice attempts, a random first player in each round, and an inclusive one-minute deadline based on successful prompt delivery. Rules are recorded in [GAMES.md](../product/GAMES.md).

`DiceAttemptService.prepareRequest(duelId, chatId, roundNumber)` locks the duel and saves the randomly selected side before any prompt is sent. Repeated/concurrent preparation returns the same pending request. After its valid result, preparation creates the other participant's request. Round numbers prevent stale work from starting a later round. No second request exists while the first result is missing.

The adapter uses the returned request ID to correlate a native message to its duel and participant. `confirmPrompt` records the successful delivery timestamp and starts that request's sixty seconds. An unsent prepared request has no deadline. Reconfirmation with the same timestamp is a no-op; changing it is rejected. Timestamps use whole seconds to match Telegram message dates. The adapter must supply the original successful prompt timestamp when retrying, rather than the time of a repeated notification.

`recordAttempt` validates chat, request/duel correspondence, participant, positive native message ID, result range, and the message's send timestamp. Messages before the prompt or after the deadline are rejected; equality at the deadline is accepted even when processing happens later. A request consumes one native message. `(chat_id, message_id)` is unique across duels, so the same event cannot serve another request. An exact retry is a no-op, including after round progression or match completion; a conflicting retry is rejected.

When both attempts exist, the service passes the pair in original participant order to `DiceMatchService` and persists its returned scores, round count, winner, and tiebreak flag in the same transaction. The randomly selected throw order never changes the original participant sides. The duel row lock serializes all request/result mutations; the database also enforces one request per side per round. No match state is kept in service memory. An unfinished pair is recovered by preparing/reading its persisted request in a new transaction.

D2 records the core's decisive result and prevents further requests, but does not settle the bank. D3 must coordinate winner recording and settlement atomically before exposing a complete playable scenario. Direct internal wallet settlement remains uncoordinated with game progress as described above. There is no timeout worker, automatic forfeit/refund, live Telegram message routing, or prompt delivery recovery in D2. The adapter must verify native origin and request correlation; player/chat concurrency limits and timeout coordination with delayed updates remain later decisions.

Integration coverage includes request persistence, stable random order on retries, delayed on-time messages, inclusive deadline, invalid/foreign inputs, cross-match message reuse, concurrent request/result retries, incomplete pairs, every series format and tiebreaks, both winner sides, and rollback of an attempt when score persistence fails.

On 2026-10-01, D2 passed `gradlew.bat check bootJar`: formatting, compilation, packaging, 8 unit tests and 27 PostgreSQL integration tests, with no failures or skips. Seven integration tests cover the new attempt lifecycle. No live Telegram calls were made.

## D1 verification

Integration coverage checks accepted state in separate transactions for every series format, participant order, fractional stake, chat isolation, concurrent/repeated acceptance, full rollback when a database constraint rejects match creation, and rejection of a previously settled bank. A dedicated upgrade test applies the first three migrations, seeds legacy records, then applies migration 004 and repeats the update. It verifies that only outstanding accepted commitments receive initial state and that wallet balances and the ledger remain unchanged. Existing invitation and wallet tests remain required. Full verification: `gradlew.bat check bootJar`.

On 2026-10-01, `gradlew.bat check bootJar` passed: formatting, compilation, packaging, 8 unit tests and 20 PostgreSQL integration tests, with no failures or skips. No live Telegram calls were made.
