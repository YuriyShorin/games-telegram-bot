# First code iteration: explicit trial assumptions

This file records rule interpretations before implementation. It supplements the five discovery documents; it does not declare unresolved ideas approved.

- Fixed series are five or seven paired rounds, including tied rounds. An equal final score triggers paired sudden death without an attempt cap.
- Until-victory means the first decisive pair, following the previously documented interpretation.
- Consent at duel acceptance or cup entry is sufficient; no repeated readiness check. **Owner decision:** players send native game emoji themselves within one minute per requested attempt. Missing the deadline causes technical defeat of the match. Timing assumptions and remaining questions are in GAMES.md.
- Economy uses ECONOMY.md trial amounts and payout tables. Payouts round down to 0.01; tournament rounding remainder goes to first place.
- The initial code iteration implements these rules as a testable game core. Native animation mappings remain provisional and no live Telegram payouts are enabled by this core alone.
- Slot triple A/B/C correspond to native reel categories 0/1/2; category 3 is seven. Visible labels remain to be verified.
- Cup pairings are shuffled once before the first match; bronze is played before the final.
- Started cup forfeits advance opponents; a withdrawing semifinalist remains withdrawn and forfeits any bronze pairing. No repeated readiness check; missing the one-minute emoji deadline causes technical defeat.
- An interrupted cup is voided, and its full unsettled pool is split equally among all original entrants. No prizes or title are awarded.
- A solo result, once recorded, settles normally; genuine interruption before a result returns the stake. Solo timeout settlement remains open; it is not automatically a refundable interruption. A duel participant cannot request a refund for losing: withdrawal is a forfeit.
- Balances and rankings are group-local. Starting grants are once per player per group, not every game or registration request.

Open interpretation questions remain in GAMES.md and TOURNAMENTS.md. This iteration does not add spectator bets, currency gifts, casinos with real-money value, or skill controls.
