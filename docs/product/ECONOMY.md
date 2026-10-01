# Virtual economy

## Status
**Decisions** come from the owner. **Trial model** is the economy calculated under the owner's explicit delegation; values can be tuned after testing. **Assumptions** are not guaranteed facts. **Ideas** remain future options.

## Owner decisions
Virtual currency only, no cash value or real-money purchase/redemption. Wealth is the main success measure. PvP (duels and cups) and solo have separate balances and rankings. All six games support all three modes. No gifts or balance conversion, free games, or maximum stake. Daily issuance provides recovery. The tournament creator chooses the entry fee. A technical loser gives the opponent the whole duel bank; an interrupted duel divides its bank.

## Trial model
| Parameter | Value |
| --- | --- |
| Starting grant | 1,000 PvP and 1,000 solo, once per player |
| Minimum stake / entry | 10; no maximum beyond available funds |
| Suggested duel / solo stake | 50, not a cap |
| Suggested tournament entry | 100; creator may choose another affordable fee |
| Daily recovery | Once per track per calendar day, when wealth is below 300 |
| Recovery amount | min(100, 300 minus current track wealth) |
| Grant collection | Voluntary claim; missed days do not accumulate |
| Daily reset | 00:00 Europe/Moscow; trial assumption |
| Wealth table | Current available funds plus that player's unsettled committed funds |
| Accounting precision | 0.01 currency unit |
| PvP commission | None |
| Tournament prizes | First 50%, second 30%, third 20% of entries |

Starting grant is once per identity, not once per rejoining a chat. **Trial assumption:** balances and rankings are group-local; confirm before expanding beyond the private group. A player can claim recovery on the joining day if already eligible. A grant cannot be claimed repeatedly after losing again on the same day. Recovery is an eligibility rule, not a cap on earned wealth.

At stake 50, starting funds cover 20 complete losses. A zero balance recovers 100, enough for two such losses or the suggested cup entry. Even eight-player entry fees are determined by the creator, so recovery cannot guarantee access to every high-entry event.

No borrowing, negative balances, or double use of committed funds. Both duel participants commit the same stake; acceptance locks it for the complete match, including tiebreaks. These are trial rules selected to keep bank settlement understandable.

## PvP and tournaments
At stake s, a duel bank is 2s. Winner receives 2s gross (net +s); loser receives zero (net -s). No currency is minted. With symmetric random play, expected net change is zero before forfeits.

The cup pool is entrants times equal entry fee. No extra per-match stakes inside a cup. No bonuses or deductions.

| Entrants / entry | Pool | First | Second | Third | Other places |
| --- | --- | --- | --- | --- | --- |
| 4 / 100 | 400 | 200 | 120 | 80 | 0 |
| 8 / 100 | 800 | 400 | 240 | 160 | 0 |

For four entrants, third place receives 80 but loses 20 net. This preserves a cost of participation while rewarding bronze. For eight entrants, bronze earns 60 net. Symmetric entrants have zero expected net currency change; prizes redistribute entries. Round second and third prizes down to 0.01 and give any remainder to first, preserving the exact pool.

## Solo payouts
Multipliers are **gross return including the original stake**, not extra profit. Unsuccessful conditions return zero. Expected return is probability times gross multiplier; expected net is stake times (return minus 1).

**Assumption:** native values are equally likely and independent. Telegram documents random value ranges but does not promise uniform probabilities. [Native result ranges](https://core.telegram.org/bots/api#dice), [animation and slot outcomes](https://core.telegram.org/api/dice).

| Game / paid condition | Trial successful values | Assumed chance | Gross multiplier | Expected return |
| --- | --- | --- | --- | --- |
| Dice: chosen exact number 1-6 | Chosen number | 1/6 | 5.40 | 90% |
| Darts: board hit | 2-6 | 5/6 | 1.08 | 90% |
| Basketball: basket | 4-5 | 2/5 | 2.25 | 90% |
| Football: goal | 3-5 | 3/5 | 1.50 | 90% |
| Bowling: strike | 6 | 1/6 | 5.40 | 90% |

The sports value-to-visible-result mappings are **provisional animation assumptions**, not established by the cited range documentation. Validate every mapping against actual native animations before enabling payouts. Darts bullseye-only is an optional later condition, not an additional launch requirement.

At stake 50, a winning number or strike returns 270 (net +220); a basket returns 112.50; a goal returns 75; a dart hit returns 54. Average loss is 5 per play under the assumptions. A 100 recovery grant offsets the expected loss on 1,000 total stakes, approximately 20 plays at 50. This is an average, not a guaranteed play allowance: bankruptcy can occur much sooner.

### Slots
Each of the three reels has four symbol categories. Use the native symbol identities; do not rank the raw combination number as payout value. Triple seven is the top result. The other triple categories below are named A/B/C until their visible symbol labels are verified.

| Exclusive outcome | Count out of 64 | Gross multiplier |
| --- | --- | --- |
| Three distinct symbols | 24 | 0 |
| Exactly one pair, in any positions | 36 | 0.50 |
| Triple A | 1 | 6 |
| Triple B | 1 | 8 |
| Triple C | 1 | 10 |
| Triple seven | 1 | 16 |

A pair returns half the stake: a partial return, not a profit. At stake 50 the gross returns are 0 / 25 / 300 / 400 / 500 / 800. Triple rules take precedence; they are never also paid as pairs.

Under equal probabilities, expected return is (36 x 0.5 + 6 + 8 + 10 + 16) / 64 = 90.625%. Expected loss is 9.375 per 100 staked. Slot tiers are fixed payouts, not a progressive jackpot.

## Daily supply and inflation
At low wealth, daily help restores access without paying rich or dormant wallets automatically. At most 100 per eligible claimant per track per day enters circulation. With eight fully eligible claimants that is at most 800 per track per day; once a player reaches 300, claims stop until wealth falls again.

PvP has no sink: transfers and prizes conserve funds, but recovery creates them. There is no mathematically stationary PvP supply under these rules. Recovery can grow group supply and indirect arranged losses can funnel grants into a rich wallet. This is a deliberate trial tradeoff; do not claim the economy is inflation-free.

Solo loses about 10% of turnover on average (9.375% for Slots), offsetting some grants. Grants to low-balance players, variable turnover, and uncapped stakes mean equilibrium is not guaranteed. Separate tracks prevent solo payout growth from funding PvP.

Weekly observe grants versus solo net payouts, total supply per track, median wealth, concentration, and players unable to afford ordinary events. Adjust future grants/payouts openly, only for new commitments. Optional cosmetic sinks or cup charges are future ideas, not launch rules.

## Forfeits and interruptions
**Decision:** duel forfeit awards the entire bank to the opponent. Missing the one-minute native game emoji deadline is a technical defeat and awards the opponent the entire duel bank. Genuine interruption divides it equally; with equal stakes each gets their stake back. Partial score does not change this split. A player abandoning a losing duel is a forfeit, not a refundable interruption.

**Trial interpretation for cups:** no individual match bank exists. A technical loss advances the opponent; the entry pool remains for the final prize places. If the entire cup is interrupted and cannot complete, void its placements and split the full unsettled pool equally among original entrants, including eliminated players. Equal fees mean everyone gets their entry back; no prizes or champion are awarded. This interpretation of “split the bank” needs confirmation if the owner meant a different tournament treatment.

Solo timeout settlement remains open; a player missing the emoji deadline must not silently receive an interruption refund. Genuine solo interruption before a trustworthy result returns the stake; a known result settles normally. A settled play cannot be cancelled to reverse a loss. Distinguish player-caused forfeits from event-wide interruption before funds are committed.

## Abuse and remaining uncertainty
No gifts does not stop intentional forfeits transferring PvP wealth. Extra identities can farm grants. Never infer cheating from lucky outcomes. Agree on a group fairness policy if abuse appears.

**Open question:** if both opponents forfeit, who receives a duel bank or a tournament place? No arbitrary winner is selected here. **Recommendation:** void a double-forfeit duel and split its bank; a cup with no eligible winner is interrupted/voided under the trial rule.

Future spectator pools, seasonal resets, and cosmetic purchases are not part of the first release.
