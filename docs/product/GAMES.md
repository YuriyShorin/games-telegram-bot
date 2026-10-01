# Games

## Status and decisions
All six games support solo, duels, and tournaments. Stakes and random outcomes suffice; no skill controls. **Owner decision:** players send native game emoji themselves. Each requested attempt has a one-minute deadline; missing it causes technical defeat of the entire match. No additional readiness confirmation is required. Other cup entrants wait.

The creator chooses a published series format. **Owner decision:** either the best result over a chosen number of matches in a series, or play until victory. **Trial interpretation:** a round is one paired set of native attempts; it is not a separately staked match.

## Selectable formats
- **Fixed series:** choose 5 or 7 paired rounds. A decisive pair gives one point to its winner; a tied pair consumes a round but awards no point. After all rounds, higher round-win count wins. If the score is tied, continue paired rounds until one is decisive.
- **Until victory:** continue paired attempts until one pair is decisive; its winner wins the match.

These interpretations are documented assumptions, not a silent replacement with first-to-three/four. **Open question:** by “until victory,” did the owner instead mean a chosen number of round wins? The above first-decisive-pair rule is the smallest literal trial interpretation.

No final draw or attempt limit. Both players get an attempt before a pair is compared, unless a missed one-minute deadline ends the match by technical defeat. No additional stake for tied rounds. Target 3-5 minutes applies mainly to series; until-victory and single solo plays can finish much faster. Do not pad play with artificial delays.

## Native outcomes and trial scoring
[Telegram documents the available value ranges](https://core.telegram.org/bots/api#dice) and [slot combination representation](https://core.telegram.org/api/dice). Equal probabilities and sports animation mappings below are assumptions to verify, not promises from that documentation. Solo returns and formulas are in [ECONOMY.md](ECONOMY.md).

## Dice
- **Concept:** random number contest.
- **PvP format:** compare the two face values; higher wins, equal ties.
- **Solo:** choose one number 1-6 before staking; exact match pays 5.40 gross.
- **Session:** 3-5 minute series target; single paid roll or until-victory is shorter.
- **Suitability:** strong for PvP/cups, moderate for spectators. Clear rules but repetition risk.
- **Ideas:** themed cups and records.
- **Open question:** does the series maintain attention without player decisions?

## Darts
- **Concept:** compare graded throw outcomes.
- **Trial PvP score:** native result levels 1-6, higher wins; this is arcade scoring, not traditional dartboard points.
- **Solo:** board hit, provisionally levels 2-6, pays 1.08 gross; level 1 loses.
- **Session:** series target 3-5 minutes; one solo throw is shorter.
- **Suitability:** promising for PvP/cups and visible deciding throws, provided levels are readable.
- **Ideas:** future bullseye-only bet and themed cups.
- **Open question:** confirm every animation's ordered score and the miss/hit mapping before enabling the game.

## Basketball
- **Concept:** baskets versus misses.
- **Trial PvP score:** successful basket = 1, miss = 0; equal outcomes tie. Do not rank different basket animations as skill points.
- **Solo:** a basket, provisionally native levels 4-5, pays 2.25 gross.
- **Session:** 3-5 minute series target; ties may extend it.
- **Suitability:** high for PvP, cups, and spectators near match point.
- **Risks:** binary results can create many ties. Under assumed success chance 2/5, each pair ties with probability 0.52.
- **Ideas:** basketball-themed cup.
- **Open question:** verify which animations score a basket.

## Football
- **Concept:** penalty shootout.
- **Trial PvP score:** goal = 1, miss = 0; equal outcomes tie.
- **Solo:** a goal, provisionally native levels 3-5, pays 1.50 gross.
- **Session:** 3-5 minute series target; no hard maximum.
- **Suitability:** high for recognizable PvP/cup suspense and spectator comebacks.
- **Risk:** mechanically similar to Basketball; both remain in scope by owner choice.
- **Ideas:** themed football cup.
- **Open question:** verify goal/miss animation mapping.

## Bowling
- **Concept:** compare visible throw result levels.
- **Trial PvP score:** native levels 1-6, higher wins. This is not traditional ten-frame bowling and does not assume native value equals actual pin count.
- **Solo:** strike, provisionally native level 6, pays 5.40 gross.
- **Session:** 3-5 minute series target; one solo throw is shorter.
- **Suitability:** promising for PvP/cups and large deciding throws.
- **Ideas:** strike-themed event.
- **Open question:** verify outcome ordering and strike animation.

## Slots
- **Concept:** combination-based random play.
- **Solo format:** one paid spin; exclusive outcome tiers pay according to ECONOMY.md.
- **PvP format:** compare the same gross payout tier at a common nominal stake, not the raw native value. Triple seven > triple C > triple B > triple A > pair > three distinct. Equal tiers tie even if symbols differ.
- **Session:** series target 3-5 minutes; one solo spin and first-decisive-pair contests can finish sooner.
- **PvP/cup suitability:** supported, but frequent equal tiers and explanation cost need testing.
- **Spectator suitability:** noteworthy triples are exciting; routine solo spins may clutter the group.
- **Ideas:** later special events or progressive jackpot.
- **Open question:** verify the visible labels for the three non-seven triple categories. Do not assign cosmetic symbol names before verification.
- **Rule:** no solo payout in PvP Slots. Only the PvP bank is settled, avoiding extra currency creation.

## Consent, forfeits, and interruptions
Accepting a duel or joining a tournament supplies consent without another readiness check. Players send each native game emoji themselves. **Owner decision:** missing the one-minute attempt deadline causes technical defeat of the match. Explicit withdrawal after lock remains a trial forfeit. Leaving the chat alone does not settle the match; a missed attempt deadline does.

Technical defeat gives the opponent the duel bank; in a cup it advances the opponent without a separate match payout. Genuine duel interruption splits the bank equally regardless of partial score. Cup-wide and solo interruption rules are in ECONOMY.md.

**Owner decisions (2026-10-01):** Dice duel attempts are sequential. The first player in each paired round is selected randomly; that choice is saved and not redrawn on retries. The second player is prompted only after the first player's valid attempt. Each player's minute starts after successful sending of their bot prompt. A native message sent no later than the deadline is accepted even when processed later; equality at the deadline is allowed. Message timestamps supplied by the Telegram adapter are authoritative, rather than processing time.

**Open questions:** timeout delivery coordination and solo timeout settlement. Do not silently treat a player timeout as a refundable interruption.

## Session fit
| Engagement | Natural format |
| --- | --- |
| Under 2 minutes | Single solo game or until-victory duel |
| 3-5 minutes | Fixed PvP series, duration to validate |
| 12-20 minutes plus ties/transitions | Four-player sequential cup including bronze |
| 24-40 minutes plus ties/transitions | Eight-player sequential cup including bronze |
| Several hours | Future asynchronous policy, outside first release |
| Several weeks | Future seasons, wealth remains the primary goal |
