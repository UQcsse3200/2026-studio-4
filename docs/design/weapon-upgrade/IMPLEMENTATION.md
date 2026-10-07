# Selected A — implementation and verification

Yuezhou Wang selected A on 2026-10-07. The user-authorized outcome is the missing weapon-upgrade purchase page inside the existing merchant shop. The selected layout has merchant and weapon list on the left, current/upgraded art and J/K comparison on the right, and the price/purchase action below.

## Local branch and dependency boundary

`task/weapon-upgrade-page` was created from clean local `items` at `7aba55d9`. Design commit: `343ac684`. After selection, merge `59b819d4` incorporated existing clean local `shop` at `9c427652`, whose 22 commits beyond items provide the merchant modal, consumable purchases, Coin Flip, Item Draw, and already-integrated dialogue dependency. The dependency files are imported as committed, without new edits to Team 3 implementation/configuration. Both local stable `items` and existing `shop` are preserved. No remote write is performed.

Task-owned edits are the Team 5 ShopDisplay, ShopFactory, ShopSessionComponent, shop purchase-result enum, and the narrow atomic purchase section of InventoryComponent, plus new shop-owned upgrade panel/catalog/adapter/config and tests. Ownership evidence: shop navigation/factory `6bf214a2`, `52226a0b`, `567242e3`, session `f174e638`, inventory purchase method `32770bd3`, `8788db08`, `2405a8d8`, authored by Yuezhou/Venompool888. No new edits to Team 3 WeaponUpgradeComponent, NPC config, narrative source, PlayerFactory, MainGameScreen or Wiki are needed.

## Behaviour

- Merchant interaction finishes into the existing shop. `WEAPONS` opens A; Back to Shop keeps the same pause/control lock. Leave/Escape closes the session and restores previous focus.
- Sword, Knife and Throwing Knife are preview choices, independent of equipped weapon. Throwing Knife maps to the existing BowWeaponComponent; no global item rename.
- Each upgrade uses the existing `WeaponUpgradeComponent.setUpgraded(..., true)` API. It is one boolean upgrade per weapon; no levels, alternate combat system or permanent-account progression is added.
- A separate `configs/shops/merchant-upgrades.json` provides trusted offer IDs and prices. Initial 60-gold values follow the prototype as provisional local balancing values. Actual player inventory supplies the balance; sample prototype gold is not applied to gameplay.
- Already upgraded, insufficient funds, unavailable component and invalid products reject without payment. Repeated clicks cannot purchase the same active upgrade twice.
- Inventory pays silently, invokes the existing upgrade API, and then emits goldChanged. Both upgrade/gold observers see payment and upgrade together. Unsupported upgrades return false before side effects and restore payment. Arbitrary third-party event subscriber exceptions/mutations remain outside the existing transaction guarantee.
- J/K values follow the existing game: all light attacks +20%; Sword 360-degree 1.35x heavy /2x cooldown; Knife 0.6x+0.6x+1.2x /3x cooldown; ranged three projectiles centre and +/-15 degrees, base damage each /2x cooldown.

## Verification status

Initial feature validation on dependency `9c427652`: `./gradlew spotlessApply core:test spotlessCheck desktop:classes core:jar` passed with **2027 tests, zero failures/errors/skips**. The new transaction, UI-entry, disposal and rejection-feedback tests were observed failing before their implementations/fixes. Regressions cover insufficient gold, repeat/reentrant buys, real event observers, selected weapon mapping, external grant/revoke, callback closure/disposal, page navigation, hidden actions, shortcuts and small-window controls.

An isolated LWJGL windowed harness rendered the actual ShopFactory/ShopDisplay at 1280x800, 960x600 and 640x480, and purchased Knife once (test fixture 150 -> 90 gold). All runs exited successfully. At 960 all primary content fits; 640 uses vertical scroll panes for the selector/techniques while purchase/navigation remain reachable. Final images are under `/Users/yuri/CSSE3200/output/weapon-upgrade-design-20261007/implemented-*.png`. This is actual UI rendering with real inventory/upgrade components, not a full-world merchant manual playthrough. Temporary harness and fixture gold remain outside the repository. Mouse and 1/2/3/U/Escape are supported; prototype Tab focus traversal is not implemented.

Independent native-agent review identified callback-disposal resource access and narrow header overflow; both were corrected. Visual inspection also corrected overlapping button captions, and a failing regression corrected lost rejection feedback. No known task-owned failure remains in the checked dependency version. New SonarCloud scan remains unverified.

## SonarCloud and communication evidence

At implementation start, only main appears in the branch analysis API. Neither shop nor task/weapon-upgrade-page has a scan. Main analysis at 2026-10-07 08:21:02 UTC covers `9e70da4d`, gate OK, 37 unresolved issues: 36 code smells, 1 bug, 0 vulnerabilities; none in shop paths. These are historical repository findings, not an attribution to this task. Exact-head local task gate/issues/categories are unknown until a new authorized analysis exists. No source upload/scan triggered.

Discord Team 5 and Team 3 checked on October 7; Team 5 visible latest September 18, Team 3 latest October 7 confirms dialogue PR #231 and planned tutorial work, without an upgrade purchase delivery. Public PR requests also checked. No WhatsApp access or outgoing message performed for this turn.

Baseline dependency checks also inspected the specific unresolved lists for PR #198 (head `1c9ad811`, analysis 2026-10-05 09:57:53 UTC) and PR #231 (head `525cc0d0`, analysis 2026-10-07 02:14:16 UTC): both gate OK, zero unresolved issues. End-of-feature main/branch checks still show the same 37 main findings and no task scan.

## Final dependency sync and verification

While this task was in progress, another local workflow advanced items to `9e70da4d` and shop to `809a4e2f`. Feature commit `cce6dfd7` was preserved, and merge `c88d33be` incorporated that already-committed shop dependency into this task without conflicts. This task is based on the current items/main version and contains shop `809a4e2f`; it did not move either branch reference.

The only new adaptation for that dependency is in the task-owned weapon-purchase method: after successful upgrade, `setGold(gold)` preserves the existing achievement notification, then the explicit gold event is published. Since the setter receives the already-committed balance, it does not emit an early or duplicate gold event. A real AchievementService observer regression failed before this adaptation and passes afterward; rejected/repeated purchases remain silent. No achievement implementation or configuration was edited.

Final `./gradlew spotlessApply core:test spotlessCheck desktop:classes core:jar` succeeds: **2156 tests, 0 failures, 0 errors, 0 skipped**. The actual LWJGL UI was rendered again on the synchronized dependency at all three window sizes and after purchase; exit 0. [Final 960-wide actual UI](implemented-a.png). Full-world NPC manual playthrough remains outside this isolated rendering check.

Final audit: user-requested Team 5 shop work, reuses existing upgrades, no duplicate combat implementation, test harness/demo gold outside repo, no new edits to another team's code/config/Wiki. Independent review accepted callback disposal, purchase messaging and achievement notification fixes. Branch kept local; no push, PR, review request, issue or message sent. SonarCloud new scan remains unverified; exact task-head gate and new issue count are unavailable.

## 1000-gold demonstration and combat verification

Yuezhou explicitly requested 1000 gold when starting a game to demonstrate and try upgrades, and asked whether purchases actually improve damage. `configs/player.json` changes only the requested initial gold value, 50 -> 1000, plus its final newline. The old gold line originates in the initial project scaffold (`5704ffa9`, Jackson Trenerry); this is the user's specific new-run currency request, not a rebalance of the neighbouring shared health, attack or movement settings. Keep this demonstration budget local pending a separate final-economy decision. PlayerFactory loads the value into InventoryComponent. MainGameScreen/GameSaveMapper still restore existing-save gold; no save file was changed.

New Team 5 `ShopWeaponCombatIntegrationTest` covers the complete purchase-to-combat path with real shop, inventory, upgrade, weapon selection, weapon components, hitbox entities, collision damage and enemy health. With base attack 10, weapon multiplier 1 and attack speed 1:

| Weapon | J before/after purchase | K after purchase | K cooldown |
| --- | --- | --- | --- |
| Sword | 10 -> 12 | 360-degree sweep, 14 damage after rounding | 1.0 s |
| Knife | 10 -> 12 | Two 6-damage slashes, then a 12-damage finishing stab | 1.5 s |
| Throwing Knife (Bow component) | 10 -> 12 | Three projectiles, 10 each; 30 total only if all connect | 1.0 s |

J retains its 0.5-second cooldown. K is locked before purchase. Tests assert enemy health loss for each actual attack hitbox as well as prevention of attacks during shared cooldown. Heavy attacks have different shapes and longer cooldowns, so they do not imply universally higher sustained single-target DPS. Existing Team 3 damage/balance code was unchanged.

Final `./gradlew spotlessApply core:test spotlessCheck desktop:classes core:jar` passed: **2162 tests, 0 failures/errors/skips**, including all six new combat integration cases. A separate windowed LWJGL probe constructs a player with the actual PlayerFactory, reads 1000 gold (no inventory fixture override), buys all three upgrades through the actual ShopDisplay, and asserts 820 gold remains. Process exit 0; log `logs/demo-runtime.log`, images `demo-*.png` under `/Users/yuri/CSSE3200/output/weapon-upgrade-design-20261007`. This isolated runtime verification does not claim a complete manual world/NPC walkthrough.

Discord Team 5 and relevant Team 3 updates were checked again; no changed weapon/shop contract appeared. SonarCloud start/end checks still contain only main branch analysis at `9e70da4d`, 2026-10-07 08:21:02 UTC, gate OK with 37 unresolved historical findings (36 code smells, 1 bug, 0 vulnerabilities). The task has no exact-head remote scan; new scan unverified and no remote state/source upload performed. New work is limited to the requested configuration value, Team 5 integration tests and this documentation.
