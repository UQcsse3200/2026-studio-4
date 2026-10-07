# Aarash shop adaptation into local shop

Date: 2026-10-07 (Australia/Brisbane). Authorized by Yuezhou Wang to adapt only the shop portion into local shop.

## Source and attribution

- Existing shop baseline: 8bb124d1f5a70102adff81582328c8187b828cfa.
- Aarash source branch: task/196-shop-display; checked remote head c6d6bc454e28bfe9ca52ad351c328447b93e7ba5.
- Shop command and display request contract originate in Aarash Mehta's 10f12659 (shop UI slice), with subsequent shop formatting/background work in 0c9a9348 and 2691cc03.
- ShopCommand is adapted from Aarash's original command. The existing selected ShopDisplay now implements his presentation-only request pattern. The adapter, event-result wiring and integration tests were implemented with OpenAI Codex assistance under Yuezhou's direction. Commit attribution includes Aarash as co-author for the reused command and request-flow contribution; original source commits/branch remain intact. This is selective adaptation, not a merge of his entire branch.

## What changed

- F1 terminal -> shop open -> Enter opens the existing shop without walking to the Merchant; successful terminal processing closes the terminal automatically.
- shop close is also registered for programmatic/terminal QA. While the modal is open, ordinary terminal input is blocked, so use Leave/Esc to exit during gameplay.
- Both NPC and command entry use ShopSessionComponent, preserving world freeze, player control lock, keyboard/scroll focus and close/reopen lifecycle.
- BUY sends shopPurchaseRequested(offerId, displayedPrice) on the shop entity. ShopPurchaseComponent settles against the trusted existing catalogue and returns shopPurchaseResult(offerId, result). The display consumes results and refreshes feedback, balance and owned counts.
- A supplied positive display price cannot alter the charged catalogue price. Null/non-positive prices reject; closed/disposed shops ignore requests. Invalid offers and insufficient funds do not charge or grant items.
- The previously selected merchant/list layout, catalogue and prices are retained. Aarash's alternate panel/background duplicates the existing visual function, so no visual direction was changed. His hardcoded three-item list is adapted to the existing five-offer JSON rather than introduced as a second price source.
- CoinFlip, Burn Vial, six-slot changes and Dev's alternate shop service/catalogue are excluded.

## Scope audit

Only Team 5 shop files, the new shop command, and the existing Team 5 ShopFactory registration line in MainGameScreen were modified. Blame for that registration is Yuezhou/Venompool888, 6bf214a25. Other teams' narrative code, NPC configuration, shared skin and inventory implementation were not edited. Local items was clean at 7aba55d9; task/196-shop-display-adaptation was created from it and fast-forwarded to existing shop before implementation. The adaptation is integrated into local shop only; items is not updated.

Discord Team 5 latest visible messages remain September 18; latest October 7 pr-requests concern other teams' enemies, savefile, Cerberus and minimap, with no new visible shop contract. WhatsApp was not accessed and no messages were sent.

## Verification

- Baseline focused shop tests, spotlessCheck and desktop:classes passed.
- First RED: new purchase-request integration tests failed because the baseline had no settlement listener (balance remained unchanged / no results emitted).
- Second RED: malformed request test failed before payload validation was added.
- Final command: ./gradlew spotlessApply core:test spotlessCheck desktop:classes.
- Final result: BUILD SUCCESSFUL; 1988 tests, 0 failures, 0 errors, 0 skipped. ShopFactoryTest has 12 passing tests (7 added to its original 5).
- Tests exercise real InventoryComponent, Scene2D BUY actions, request/result wiring, trusted pricing, rejection, closed/disposed lifecycle and real terminal Enter handling. Existing modal input/focus/resize tests also pass.
- git diff --check passes. No full visible game walkthrough is claimed for this adaptation.
- Logs: /Users/yuri/CSSE3200/output/shop-aarash-adaptation-red.log, shop-aarash-adaptation-request-red.log and shop-aarash-adaptation-validation.log.

## SonarCloud and remote state

Start/end read-only checks list only main as a scanned long branch and no shop-related PR scan. The explicit shop unresolved-issues query returns an empty list, but shop is absent from the analysis branch list: there is no analysis for either source head or new local head. Quality Gate and issue counts for this adaptation are therefore unknown, not verified zero. No scan/upload was triggered. Local validation is complete; SonarCloud new scan is unverified.

No push, PR/issue edit, review request or external message was performed. Remote shop remains the baseline until separately authorized.

## Playtest order

1. Run windowed game from /Users/yuri/CSSE3200/worktrees/shop/source using ./gradlew desktop:run.
2. Start a new game; F1 -> shop open -> Enter. Expect the existing merchant layout and gameplay pause.
3. Buy one Health Potion (10 gold): balance decreases by 10, Owned increases by 1, success feedback appears. Buy other affordable offers as desired; unaffordable buttons become disabled.
4. Leave/Esc closes the shop and gameplay resumes. Repeat F1 -> shop open -> Enter: inventory/balance remain and actors are not duplicated.
5. Close, approach Travelling Merchant, press E and finish the greeting. The same shop opens with the same inventory and prices.
6. At a small window size, scroll both columns and confirm Leave remains accessible; close and verify movement resumes.

AI assistance: OpenAI Codex performed the scoped adapter, verification and report under Yuezhou Wang's instruction.
