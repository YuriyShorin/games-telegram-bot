# Product vision

## Status and vocabulary

This is a product discovery proposal, not an approved rulebook. The same labels apply across all documents:

- **Decision:** a constraint explicitly established by the owner.
- **Trial model:** a calculated rule chosen under the owner's delegation of economy design; adjustable after testing.
- **Recommendation:** a proposed choice requiring agreement or playtest evidence.
- **Assumption:** something temporarily taken as true; validate before relying on it.
- **Idea:** an optional direction, not a commitment.
- **Open question:** an unresolved choice that affects the experience.

## Decisions

The product is a multiplayer Telegram game for a private group of friends. It uses native game animations and virtual currency with no cash value. Competition, risk, friendly rivalry, spectatorship, short sessions, and recurring events are the intended experience. No real-money participation, purchase, redemption, or prizes are part of this concept.

## Vision

Turn a familiar group chat into a place where friends build virtual wealth through solo play, challenges, and cups. Stakes make outcomes consequential; separate wealth tables, taunts, and shared stories give friends reasons to return.

**Decision (owner answers):** duels, tournaments, and solo play are equally important. Friends can gather for an event or challenge one another during the day. All six games belong in the first release. Stakes and random results are sufficient; skill controls are not required. Wealth is the primary success measure, with separate PvP and solo balances and wealth rankings.

## Target players and experience

**Decision:** duels and four-player events are expected to be most popular; four- and eight-player tournaments are supported. Players send native game emoji themselves; missing the one-minute attempt deadline causes technical defeat of the match. Tournament matches are sequential while other entrants wait. Target match length is 3-5 minutes. Playful taunts are part of the desired tone.

**Assumption:** friends enjoy watching a complete random series unfold. Acceptable chat traffic remains unknown.

**Latest decisions:** every game supports all three modes. Joining a tournament or accepting a duel supplies all necessary consent. Choose a fixed series or until-victory format. The tournament creator selects entry cost. Technical defeat gives the opponent the duel bank; genuine interruption splits it.

**Delegated trial economy:** 1,000 starting funds per balance, low-balance daily help up to 100, suggested stake 50 and cup entry 100, prizes 50%/30%/20%. Solo gross return averages 90% (Slots 90.625%) under documented probability assumptions. Full formulas and recovery conditions are in ECONOMY.md; these are trial values, not claimed Telegram guarantees.

The intended emotional sequence is anticipation, a visible reversal or close finish, a clear result, and an invitation to a rematch. Losing should sting briefly without excluding someone from the next gathering. Joining late should remain worthwhile.

Native random outcomes offer suspense, not proof of sporting skill. The product should describe champions as winners of the event, without implying that luck-based results measure ability. Repeated rounds can create a match story, but do not automatically add meaningful agency.

## Core gameplay loop

1. See an invitation or challenge with its game, stakes, and expected duration.
2. Accept voluntarily, or join a small tournament before its start.
3. Watch a complete, readable series while friends follow the score.
4. See the winner, currency movement, and any title earned.
5. Choose a rematch, another opponent, or the next scheduled gathering.

Solo loop: choose a game and stake, read payout conditions, watch the result, settle the solo balance, and decide whether to continue. Tournament loop includes a bronze match for losing semifinalists before prize settlement.

**Recommendation:** challenges expire quietly if declined or ignored. Participation is opt-in. Keep one featured competition active in the chat during the first playtest.

## Product pillars

| Pillar | Purpose | Proposed first expression |
| --- | --- | --- |
| Competition | Give friends a shared outcome to care about | Equal-rule duels and four/eight-player cups |
| Economy | Make wealth and risk meaningful | Uncapped affordable stakes, pooled entries, daily grants |
| Tournaments | Turn isolated rolls into a group event | Four/eight players, elimination, final and bronze match |
| Spectatorship | Keep nonparticipants involved | Score updates, finalist anticipation, reactions |
| Progression | Compete for wealth | Separate PvP and solo wealth tables; trophies are secondary |
| Solo | An equally important independent experience | Solo stakes and payouts in a separate economy |
| Social play | Connect outcomes to existing friendships | Rematches, game-specific taunts and result summaries |

## Why players return

Grow a balance, climb a wealth table, beat a friend next time, attend the next cup, or try solo luck. Cup history can reinforce shared stories without replacing wealth as the main goal.

**Decision:** daily currency issuance provides recovery; there are no free games, currency transfers, or product-imposed stake caps. **Risk:** daily rewards can inflate balances and reward tenure. All-in losses may require waiting until the next grant. Similar random modes can become repetitive even though all six remain in scope.

## Critical assessment

- **Strong:** small cups, close scores, agreed stakes, rematches, wealth competition, and playful taunts fit an existing social group.
- **Potentially repetitive:** highest-roll spam, long sequences with no choices, and cosmetic changes to otherwise identical games.
- **Waiting risks:** sequential matches and unlimited tied attempts. Each manual emoji attempt has a one-minute deadline; a missed deadline causes technical defeat. No repeated readiness checkpoint is required.
- **Chat risks:** unsolicited challenges, repeated reminders, every statistic becoming a separate announcement, and overlapping matches obscuring results.
- **Retention risks:** early bankruptcy, dominant wallets, inconvenient cup times, and treating random outcomes as a serious skill ranking.

**Recommendation:** use compact score summaries and announce meaningful stages. Avoid public shaming for losses, personal insults, or pressure to accept a rematch. Funny summaries should refer to what happened in the game; playful commentary should suit the group's preferences.

## Open questions

1. Does “until victory” mean first decisive paired round, as the documented trial interpretation assumes, or a target number of round wins?
2. How many game messages per match feel welcome, and where should solo results appear?
3. Trial wealth is current funds including committed stakes; assess whether it creates the intended competition.
4. Trial balances are group-local; confirm before expansion to more groups.
5. What makes the first test successful? The owner has not decided yet.

See [games](GAMES.md), [tournaments](TOURNAMENTS.md), [economy](ECONOMY.md), and [roadmap](ROADMAP.md) for proposed rules and validation gates.
