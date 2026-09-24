# Belot — the rules this service implements

Source: <https://belot.bg/belot/rules/>, read 2026-09-24. Where the page is
silent, the gap is marked **❓ OPEN** and must be answered before the code that
depends on it is written — a guess here is a scoring bug that reads to players
as cheating.

Each rule below is meant to become a test. The section numbers are referenced
from `BUILD.md`.

---

## 1. Table

- 4 players, 2 teams, partners opposite each other.
- Dealing and play run **counter-clockwise** (обратно на часовниковата стрелка).
- 32 cards: 7, 8, 9, 10, J, Q, K, A in four suits.

## 2. Card order

Two orders, chosen per contract:

| | Order, high to low |
|---|---|
| **Trump** | J · 9 · A · 10 · K · Q · 8 · 7 |
| **Plain** | A · 10 · K · Q · J · 9 · 8 · 7 |

- **Suit contract** — the named suit uses the trump order, the other three use the plain order.
- **All trumps (всички козове)** — every suit uses the trump order.
- **No trumps (без козове)** — every suit uses the plain order.

## 3. Card points

| Card | In a trump suit | In a plain suit |
|---|---|---|
| J | **20** | 2 |
| 9 | **14** | 0 |
| A | 11 | 11 |
| 10 | 10 | 10 |
| K | 4 | 4 |
| Q | 3 | 3 |
| 8, 7 | 0 | 0 |

Plus **10 for the last trick** (последните десет).

### Deal totals — the engine must hit these exactly

| Contract | Total |
|---|---|
| Suit | **162** |
| All trumps | **258** |
| No trumps | **260** |

Two of these are arithmetic: a suit contract is 62 (trump suit) + 3 × 30 + 10 =
162; all trumps is 4 × 62 + 10 = 258. No trumps is 4 × 30 + 10 = 130 — **half of
260**, so the page's figure only works if every point in a no-trump deal is
doubled, the last trick included.

> **ANSWERED — every point in a no-trump deal counts double**, the last trick
> included: 2 × 120 + 2 × 10 = 260. An ace is 22 there, a ten 20. Implemented in
> `CardPoints.multiplier`.

## 4. Dealing

1. Deal **3 cards**, then **2 cards**, to each player — 5 in hand.
2. Bidding (§5).
3. The dealer then deals **3 more** to each player — 8 in hand.
4. If all four pass in the first round, the next player deals a fresh hand.

## 5. Bidding

- First to speak: **the player to the dealer's right** (пръв обявява играчът в дясно от раздаващия).
- Each bid must be higher than the last. Ascending order:

  `pass < ♣ < ♦ < ♥ < ♠ < no trumps < all trumps`

> **❓ OPEN 2 — Confirm the suit order.** The page's own list is garbled
> ("Spades, Diamonds, Hearts, Spades"). The order above is the usual Bulgarian
> one (спатия, каро, купа, пика) — confirm before coding, since it decides which
> bids are legal.

- Bidding ends after **three consecutive passes**.
- **Contra** — an opponent of the last bid doubles the deal's score. **Recontra**
  by the bidding side quadruples it.

> **❓ OPEN 3 — After a contra, may bidding continue with a higher contract, or
> is the contract fixed?** The engine assumes a raise is allowed and clears the
> contra with it, since the contra was aimed at the contract just outbid. See
> `BelotBiddingTest.openThreeRaisingOverAContra`.

## 6. Playing a trick

- **Follow the led suit if you can.**
- If you cannot follow **and the trick currently belongs to an opponent**, you
  must trump (цака).
- If an opponent has already trumped, you must **overtrump** if able.
- **If your partner is winning the trick, there is no obligation to trump** —
  the rule applies only "ако взятката до момента принадлежи на противника".
- In **no trumps**, only following suit is required; there is nothing to trump with.

> **❓ OPEN 4 — In all trumps, when a partner is winning, must you still play a
> higher card of the led suit if you hold one?** (In some variants the
> overtrumping obligation applies within the led suit regardless.)

> **❓ OPEN 12 — When the led suit is the one played by the trump order and you
> can follow, must you play a card that beats what is on the table?** The engine
> assumes yes, while an opponent holds the trick. `BelotTrickTest.openTwelveFollowingTrumps`
> states the assumption and what the other answer would expect instead.
>
> **❓ OPEN 13 — You must trump, but every trump you hold is too low to win.
> Must you still play one, or may you discard?** The engine assumes the trump is
> compulsory. See `BelotTrickTest.openThirteenUndertrumping`.

## 7. Declarations (анонси)

Declared when playing your **first card of the deal**.

| Combination | Points |
|---|---|
| 3 in sequence (терца) | 20 |
| 4 in sequence (кварта) | 50 |
| 5+ in sequence (квинта) | 100 |
| Four 10 / Q / K / A (каре) | 100 |
| Four 9s | 150 |
| Four J | 200 |
| **Belote** — K + Q of the trump suit | 20 |

Rules:

- **Only the team with the single highest sequence scores its sequences** —
  "премии за тях си записва само отборът, обявил най-висок такъв".
- Equal length is decided by the **starting card**; if still equal, **all
  sequence bonuses are cancelled** for both teams.
- **No trumps: declarations are forbidden**, except the last trick and capot.
- All trumps: declarations are normal.

> **❓ OPEN 5 — Do fours (карета) compete separately from sequences, or does one
> comparison cover both?** Classic belote compares them separately.
>
> **❓ OPEN 6 — Is Belote (K+Q) independent of the comparison above — i.e. does
> it always score even if the other team holds the best sequence?** Classic
> belote: yes, always.
>
> **❓ OPEN 7 — Which four wins when both teams hold one (J > 9 > A > 10 > K > Q)?**
>
> **❓ OPEN 8 — Are declarations allowed in a suit contract, or only all trumps?**
> §7 of the page implies yes for suit contracts; confirm.
>
> **❓ OPEN 14 — Six, seven or eight cards in a row: still 100, or more?** The
> page stops at five. The engine scores any run of five or more as a quinte.
>
> **❓ OPEN 15 — In all trumps, where every suit is a trump suit, how many
> belotes can a hand hold?** The engine counts one per suit holding both the
> king and the queen. See `Declarations.belotes`.

## 8. Scoring a deal

- **Capot (капо)** — one team takes all eight tricks: **+90**.
- The contracting team **makes** the contract if it scores strictly more than
  the opponents. Each team then records its own points ÷ 10, rounded.
- **Going down (вътре)** — the contracting team fails; the **opponents record
  everything**, both teams' points, ÷ 10.
- **Hanging (висящи)** — the two sides tie. The contracting team records
  nothing; its points **carry to whoever wins the next deal**. The opponents
  record theirs.
- **Contra / recontra** double or quadruple everything, bonuses included. Points
  that hang while doubled carry forward still doubled.

> **ANSWERED — the two scores are rounded together, so the sheet adds up.**
> Each goes to its nearest ten with a **five going down** (85 is 8). If the two
> then fall a point short of the deal, one comes up: when **both end in 4** the
> calling team takes the lower rounding and the other the higher (154 and 104
> are 15 and 11); otherwise **the team that took more** goes up (155 and 103 are
> 16 and 10). A point over, and the team that took fewer goes down (86 and 76
> are 9 and 7). `DealRounding`, checked over every possible split of every deal
> total.

## 9. Ending the game

- A game ends when a team reaches **151** or more; higher total wins.
- **"С капо не се излиза"** — a team cannot finish on a capot deal: if the
  winning team reached 151+ with a capot, **one more deal is played**.

> **❓ OPEN 10 — Both teams cross 151 in the same deal — who wins?** The higher
> total, which the engine implements. When the two are **level** it plays another
> deal rather than leave the game drawn — see
> `BelotGameScoringTest.openTenALevelFinish` if that is wrong.
>
> **❓ OPEN 11 — Does a deal where everyone passes count for the "no capot" rule?**
> The page excludes all-pass rounds and previous capot deals from the extra deal;
> confirm what that means in practice.

## 10. Worked examples to turn into tests

Fill these in once the OPEN questions are answered; they are the acceptance
tests for the engine.

| # | Situation | Expected |
|---|---|---|
| 1 | Suit ♠, contractor takes 90, opponents 72 | contract made — record 9 / 7 (pending §8 rounding) |
| 2 | Suit ♠, contractor 80, opponents 82 | вътре — opponents record 162 ÷ 10 |
| 3 | Suit ♠, both 81 | висящи — contractor records nothing, carries 81 |
| 4 | All trumps, one team takes every trick | 258 + 90 capot |
| 5 | Contra, contractor goes down | opponents record (162 × 2) ÷ 10 |
| 6 | Both teams hold a terz, equal length, equal top card | no sequence bonus for either |
| 7 | No trumps, player holds K+Q of a suit | nothing — declarations forbidden |
