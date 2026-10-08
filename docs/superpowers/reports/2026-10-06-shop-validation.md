# Consumable shop implementation and validation

Implemented product head: `a030e01e` on local `shop`. Yuri selected Gemini candidate A and approved the design, Native implementation plan and the two specific shared-file integration edits.

## Delivered behavior

- The existing merchant greeting leads directly to a consumable shop, using NPC interaction completion; the merchant demo video step is removed from its record only.
- The selected merchant/list layout uses existing shopkeeper and item art, existing item names/effect summaries, gold, unit prices, owned quantities, Buy and Leave/Escape actions.
- Five configurable consumables are available: health potion 10 gold, shield 15, speed potion 15, strength potion 20, freeze bomb 20. These prices remain test economy values. Normal player configuration already starts with 50 gold; no debug grant was added.
- Purchases add one item and charge the trusted catalogue price. Invalid/unsupported purchases, insufficient gold and count overflow do not alter inventory.
- Charm and weapon upgrade categories have an extension boundary but no active fulfillment or visible entry.
- The modal owns its freeze/control lock, keyboard/scroll focus and input consumption. Closing/reopening/disposal release its ownership and restore attached prior focus. Both columns scroll when a smaller window needs it.

## Changes across team boundaries

The approved shared changes are exactly the merchant's three video-step fields in `friendlyNpcs.json` and one ShopFactory registration plus import in `MainGameScreen.java`. Other NPC records and narrative implementations were preserved. No shared skin files or video assets were modified.

## Verification

Final `./gradlew core:test spotlessCheck desktop:classes`: BUILD SUCCESSFUL, **1981 tests, 0 failures, 0 errors, 0 skipped**. Focused tests exercised actual Scene2D actors and purchase bindings, not just mocks of the shop.

An independent GPT-6-astra review found one UI issue: at 640x480 the merchant column's minimum content height pushed the close button outside the viewport. A failing regression was observed, both columns became independently scrollable with shrinkable body cells, and the regression then passed. The reviewer repeated the same real Scene2D layout probe and accepted the fix:

| Window | Close button position and size | Product scroll position and size |
|---|---|---|
| 1280x800 | (1046,658), 110x38 | (395,132), 761x514 |
| 906x706 | (746,605), 110x38 | (267,91), 589x502 |
| 640x480 | (490,394), 110x38 | (197,75), 403x307 |

All are inside their viewports. At 640x480 the scroll range is 445 and a wheel event changed scroll position.

Two further issues were covered by RED-to-GREEN regressions: a gold event subscriber changing item counts must not be followed by an obsolete count notification; and background stage focus must not receive typing while the shop is open. These are fixed.

The latest game build was launched with the existing user preference `fullscreen=false`, for a 1280x800 window. Startup logs confirm MAIN_MENU. Computer Use reports `Invalid app: java` and its app list does not expose the Java game, so the full visible mouse walkthrough, GPU appearance, movement after close and another NPC's visible narrative remain **manually unverified**. Headless tests and layout probes are not represented as manual playtesting.

## Review scope decisions

- Existing addGold/addConsumable overflow behavior outside the new purchase path was not changed; unrelated inventory operations retain their prior behavior. Cost if that separate issue occurs: it requires its own attributed investigation.
- Arbitrary exceptions from unrelated event subscribers are outside the agreed validation-rejection guarantee; callbacks remain synchronous. Cost: such an exception can interrupt notifications after a committed transaction.
- Native LWJGL keyboard repeat produces keyTyped rather than repeated keyDown; no fix was added for an unsupported repeated-keyDown sequence. Cost if another backend is adopted: revisit input ownership against that backend.
- Charm/upgrade fulfillment and selling/restocking/bulk purchasing remain outside the approved first version. Unsupported categories explicitly reject without deducting gold.
- Real visual appearance and complete walkthrough remain for Yuri's windowed playtest because this environment cannot target the Java UI.

No review minors were deferred. The confirmed review findings were fixed and independently rechecked.

## Sonar and repository state

PR #231 dependency analysis corresponds to `0bc11ffaea236f71bf1065f88636084b2ebe6e6e`, analyzed 2026-10-06 15:04:59 Brisbane (05:04:59 UTC): Quality Gate OK, 0 unresolved bugs/vulnerabilities/code smells. This does **not** verify the new local shop head. SonarCloud new scan remains unverified; no upload was triggered.

Changes are locally committed. No remote push, PR creation/approval, merge into items, or main update was performed. Discord could not be refreshed because the Chrome browser connection was unavailable. WhatsApp was not accessed.

## Windowed playtest route

Start the game, approach the Travelling Merchant in the initial room, press E and advance its existing greeting. Check a purchase changes gold and owned quantity, spend down to an unaffordable offer, close with Leave/Escape and reopen. Shrink the window if desired to verify both columns scroll and Leave remains accessible. Check another NPC's original dialogue/cutscene afterward.
