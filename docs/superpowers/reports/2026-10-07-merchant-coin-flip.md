# Merchant coin-flip integration — 2026-10-07

## Scope and design

User selected Candidate A in `/Users/yuri/CSSE3200/output/merchant-casino-20261007/candidate-a.png`.
Implemented in the existing Team 5 shop UI: CASINO header button, merchant sidebar, coin arena, stake controls, result, Back to Shop and Leave. No NPC, narrative, shared inventory or other-team configuration changes.

Adapted Aarash Mehta’s rules and animation from `origin/task/196-shop-display`, commit `c6d6bc454e28bfe9ca52ad351c328447b93e7ba5`. The new CoinFlipGame isolates settlement from Scene2D rendering. A fair random boolean wins or loses exactly the stake; initial/minimum stake is 10, adjustments are 10, and a balance below 10 can be wagered in full. Payout integer overflow rejects the wager.

Settlement occurs once on click, matching the original. The 0.6-second coin squash animation is cosmetic. Repeated input is blocked while animating. Returning or leaving during animation retains the settled balance and clears the animation. Reopening starts in the shop. Both pages share the existing shop session, pause and focus lifecycle. Purchases still call ShopService directly; quick-open commands are retained.

## Validation and audit

`./source/gradlew --project-dir source spotlessApply core:test spotlessCheck desktop:classes` passed. 1992 tests, 0 failures/errors/skips. Nine added tests cover deterministic win/loss, wager bounds and overflow, page navigation, purchases after returning, duplicate input, close during animation, zero/small balances, three window sizes, and synchronous close from a settlement listener. `git diff --check` passed.

Validation is automated; no manual in-game visual walkthrough was completed in this turn. Layout tests verify navigation/leave remain inside 1280x800, 906x706 and 640x480 windows. Small casino content scrolls vertically.

Reviewed Discord Team 5 and public PR requests read-only. Team 5 visible messages end September 18; October 7 public messages concern minimap/settings/chase enemies. No messages sent and no WhatsApp accessed.

SonarCloud at start/end has no analysis for shop or this task branch/PR. Shop issue query returns an empty list but this does not establish zero warnings. Latest main analysis is `9e70da4de2cf2fa38150a58352daed8aca266417`, October 7 08:21:02 UTC, gate OK, 1 bug and 36 code smells; those are not this local branch’s results. Local implementation and validation complete; new SonarCloud scan unverified. No remote scan or write triggered.

The task branch was created from clean local items and fast-forwarded to the existing shop baseline `52226a0b`. Scope is limited to shop code/tests plus this report; no demo code or purchase event mediation added. Changes are intended for local shop only. Remote shop remains unchanged until separately authorized.

## Experience order

1. Enter the Travelling Merchant shop through its existing dialogue, or use the existing `shop open` terminal command.
2. Click CASINO beside the gold balance.
3. Adjust stake with -10/+10; click FLIP and see the coin animation/result and live gold.
4. Back to Shop returns to the products with updated affordability.
5. Leave or Esc exits the whole merchant session and resumes gameplay.
