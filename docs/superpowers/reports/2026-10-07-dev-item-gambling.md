# Dev item gambling integration — backend progress

Date: 2026-10-07, Australia/Brisbane. Status: **in progress; playable UI and initial prize configuration pending Yuezhou Wang selection**.

## Authorized scope and communication

Yuezhou requested preserving Dev's gambling feature, taking over real inventory integration, and adding the game alongside the existing Coin Flip in the merchant shop. The exact WhatsApp text and Team 5 group destination were explicitly confirmed in this conversation. The message was sent through the requested Chrome WhatsApp session at 21:07 Brisbane, verified Delivered; the user's active draft was allowed to finish before composing the approved message. No other message or GitHub write was performed.

## Branch and contribution

Clean local items was verified at 7aba55d9. Task branch task/196-item-gamble-integration was created from items then fast-forwarded to the existing local shop 567242e3 (already containing Aarash's Coin Flip). Worktree: /Users/yuri/CSSE3200/worktrees/shop. The backend progress commit initially preserved the stable items and shop references; the completed integration is audited below.

Dev Dhingia's source is origin/task/196-shop-purchase-rules at 8a31518ff38762bea6297aece7e85058c8e98c18. His GambleEntry, GambleTable, GambleResult, GambleService, GambleWallet and InventoryGambleWallet modules are selectively adapted; none of his alternate normal-purchase implementation is imported. Weighted selection, prize quantities, bust outcomes, injected Random and the seeded 20% distribution test are retained. Commit attribution credits Dev as co-author.

## Backend adaptation

GambleWallet now exposes a single settlement operation returning a status, rather than separate spend/grant calls. Winning rewards delegate to the existing shop-owned atomic InventoryComponent purchase operation, extended to a positive quantity. Balance and quantity are committed before notifications; invalid rewards and capacity overflow return rejection without charging. A normal bust charges once with no prize. Positive draw cost and weight sum bounds are enforced. Coin Flip and ordinary purchases retain their current APIs and behaviour.

Ownership: git blame identifies the modified InventoryComponent.tryPurchaseConsumable lines as Yuezhou/Venompool888's 32770bd3d and 8788db086 shop contributions. The implementation only extends those lines; no unrelated shared inventory methods, other-team implementation or configuration is changed.

## Validation

Eight real-inventory gambling tests exercise atomic observer state, invalid IDs, quantity overflow, normal bust, insufficient balance without RNG consumption, nonpositive costs, weight boundaries/overflow and Dev's seeded distribution. Five regressions failed on the original imported production code before the adaptation; all eight pass afterwards.

Full ./gradlew spotlessApply core:test spotlessCheck desktop:classes passed: 2000 tests, 0 failures/errors/skips. Final formatting check and git diff --check passed after a documentation-only comment edit. Relevant existing shop, InventoryComponent and Coin Flip tests remain green. Logs are in /Users/yuri/CSSE3200/output/item-gamble-20261007. These figures describe the initial backend commit; completed UI verification is recorded below.

## Selected A UI and local integration audit

Yuezhou selected A on October 7. The existing casino now has Coin Flip / Item Draw selectors in the same merchant session. Item Draw uses the existing wood, parchment, brass, merchant portrait and consumable textures. Its reward arena and odds list sit beside one another on large windows and stack inside a vertical scroll pane at narrow sizes. Both games share the actual inventory/gold and the existing pause, focus, Back to Shop and Leave lifecycle.

The default configuration is 10 gold, one each of HEALTH_POTION, SHIELD, SPEED_POTION, STRENGTH_POTION, FREEZE_BOMB, plus bust; all six weights are one. The immutable validated configuration supplies both settlement and displayed percentages. IDs must refer to existing consumables, cost/weights/quantities must be valid, outcomes cannot duplicate and bust cannot grant quantity. Display rounds exact 1/6 probabilities to 16.7% and says so. Prices and weights are editable in merchant-gambling.json.

Settlement occurs once before the cosmetic 0.6-second reveal; repeat/hidden controls cannot charge. Switching or closing cancels the reveal without undoing or repeating settlement. Inventory rejection charges nothing. All prize textures are loaded from the reward config even when not present in the purchase catalog, and unloaded with the shared shop assets.

Two UI regressions first failed without the selectors, then passed after implementation. Four integration tests verify the full configured price, repeated clicks/hidden games, an atomic result observer that closes the session, and vertical scrolling/navigation at 1280x800, 906x706 and 640x480. Two configuration tests verify the actual JSON and invalid rewards/duplicates/cost/bust quantity. Full ./gradlew spotlessApply core:test spotlessCheck desktop:classes core:jar passed: 2006 tests, zero failures/errors/skips. git diff --check passed.

An isolated LWJGL windowed harness renders the actual ShopFactory/ShopDisplay, with temporary 100-gold inventory, at 1280x800 and 640x480; exported framebuffer images are implemented-1280.png and implemented-640.png in the output directory. This verifies actual rendering without changing game settings. It is not a full-world merchant manual playthrough. The harness and export files remain outside the repository.

Audit: this is the user-authorized Team 5 merchant integration. Only shop UI/config/tests and the previously audited shop-owned atomic inventory method are included. Existing Coin Flip and ordinary purchase behavior retain passing regression coverage. No alternate normal shop framework, demo code, other-team files, other-team settings or Wiki changes are imported. The work is saved on task/196-item-gamble-integration, then locally fast-forwarded into shop; items remains 7aba55d9. No remote operation is authorized or performed.

## SonarCloud

Start/end branch and PR analysis queries contain no shop/Dev shop/task analysis. Exact-head analysis timestamp, gate, issue count/categories and new issues are unknown, not zero. No scan/source upload triggered. Current main analysis is 9e70da4d at 18:21:02 Brisbane, gate OK; explicit unresolved issue query finds 37 (36 CODE_SMELL, 1 BUG, 0 vulnerabilities), with no shop path issues. Historical main issues are not attributed to this local backend adaptation. Local backend and UI verification completed; new SonarCloud scan unverified. End-of-UI explicit issue inspection still shows the same main gate/categories and no scan for the shop/task head.

Discord Team 5 and public PR requests were inspected earlier in this conversation; public Team 5 messages end September 18. Current user-authorized private group responses from Dev and Aarash support taking over integration, but chat is source context rather than authorization for unrelated implementation or remote writes.
