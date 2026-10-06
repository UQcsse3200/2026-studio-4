# Consumable Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make existing merchant dialogue lead to a working consumable shop using inventory gold.

**Architecture:** A shop catalogue/service owns offers and purchase dispatch; InventoryComponent commits consumable transactions. A separate shop entity owns its view, modal input, and owner-based freeze. Existing NPC interaction completion opens the shop after its greeting.

**Tech Stack:** Java 21, libGDX Scene2D, existing JSON loading, JUnit 5/Mockito, Gradle.

**Spec:** ../specs/2026-10-06-shop-design.md (approved by Yuri on 2026-10-06).

## Global Constraints

- Work stays on the existing `shop` branch based on `items` plus PR #231.
- No new dialogue system, item registry, inventory, currency store, or video player is needed.
- Only consumable offers are configured and displayed in this version.
- Supply is unlimited for this version. No selling, restocking, random offers, bulk purchasing, additional currency, or new inventory capacity rule.
- Normal gameplay remains windowed.
- Gemini supplies visual candidates through a native GPT-6/GPT-6.1 liaison using agy CLI, as required by project instructions.
- Yuri selects a candidate before UI implementation.
- Remote pushes and PR actions require separate specific authorization.

## Review Focus

- A key held before the shop opens must still reach gameplay on release; keys pressed during the shop remain swallowed after closing. Task 3 tests both.
- The dialogue's final click must not also activate a newly opened Buy button. Task 3 tests input priority and touch ownership.
- Escape/Close must consume their input even when they close the modal synchronously. Task 3 tests this.
- An empty/invalid shop configuration must not debit gold or strand the game frozen. Tasks 2 and 3 test this.
- Another component's freeze must survive shop close/disposal. Task 3 tests independent owners.

Paths below are relative to the worktree. Production Java paths start `source/core/src/main/com/csse3200/game/`; test paths start `source/core/src/test/com/csse3200/game/`. Gradle commands run in `source/`.

### Task 1: Atomic inventory purchase

**Files:** Modify `components/player/InventoryComponent.java`; extend `components/player/InventoryComponentTest.java`; create `components/player/ConsumablePurchaseResult.java`.

**Interfaces:** Produce `ConsumablePurchaseResult tryPurchaseConsumable(String itemId, int goldPrice)` and `goldChanged(int finalGold)`. Results: SUCCESS, INVALID_ITEM, INVALID_PRICE, INSUFFICIENT_GOLD, QUANTITY_LIMIT. Existing public methods/events remain compatible.

- [x] Add tests `shouldPurchaseOneConsumable`, `shouldRejectPurchaseWithoutChangingInventory`, `shouldRejectQuantityOverflow`, `shouldPublishCommittedPurchaseState`, `shouldNotifyActualGoldChangesOnly`. Assert 25 gold buying HEALTH_POTION at 10 gives 15 gold and count 1; second gives 5/count 2; third fails with unchanged values. Invalid IDs, charm ID, zero/negative prices, Integer.MAX_VALUE count fail without events. Both purchase event callbacks observe the final balance and count. Construction without attachment remains safe.
- [x] Run `./gradlew core:test --tests '*InventoryComponentTest'`; expect new API tests to fail before implementation.
- [x] Implement result enum and transaction: validate everything, update both stored values, then publish goldChanged and consumableInventoryChanged. Add goldChanged to setGold only for actual changes with an attached entity. Do not call the event-publishing addConsumable halfway through the transaction.
- [x] Rerun focused tests; expect all pass. Run `./gradlew spotlessApply core:compileJava`; inspect diff for unrelated formatting.
- [x] Commit only inventory implementation and its tests: `feat(shop): add atomic consumable purchase to inventory`.

### Task 2: Catalogue and extensible purchase service

**Files:** Create `components/shop/ShopProductKind.java`, `ShopOffer.java`, `ShopCatalog.java`, `ShopPurchaseResult.java`, `ShopPurchaseEffect.java`, `ConsumablePurchaseEffect.java`, `ShopService.java`; create `source/core/assets/configs/shops/merchant.json`; create tests `components/shop/ShopCatalogTest.java`, `ShopServiceTest.java`.

**Interfaces:**
- `ShopProductKind`: CONSUMABLE, CHARM, WEAPON_UPGRADE.
- Immutable `ShopOffer(String offerId, ShopProductKind kind, String productId, int goldPrice)` with accessors.
- `ShopCatalog.load(String assetPath)` returns validated catalogue; `List<ShopOffer> offers()` and `ShopOffer find(String offerId)` supply immutable offers and null for absent IDs. Invalid configuration throws a descriptive IllegalArgumentException before a session acquires locks.
- `ShopPurchaseResult`: SUCCESS, INSUFFICIENT_GOLD, INVALID_OFFER, UNSUPPORTED_PRODUCT, QUANTITY_LIMIT.
- `ShopPurchaseEffect.purchase(InventoryComponent inventory, String productId, int goldPrice)` returns ShopPurchaseResult; handler owns the validated inventory transaction.
- `ShopService(InventoryComponent inventory, ShopCatalog catalog, Map<ShopProductKind, ShopPurchaseEffect> effects)`; `ShopPurchaseResult purchase(String offerId)`. Constructor defensively copies the handler map. Only consumable handler is registered by the factory.

- [x] Add tests `shouldUseCataloguePrice`, `shouldRejectUnknownOffer`, `shouldRejectUnregisteredProductKind`, `shouldBuyExistingConsumables`, `shouldRejectInvalidCatalogue`. Test positive-price validation, duplicate/null IDs, unknown kinds, empty catalogue, unsupported charm/upgrade offers, failure state preservation, repeated purchases. Include a fake registered future handler to prove dispatch without implementing charm/upgrade effects.
- [x] Run `./gradlew core:test --tests '*ShopCatalogTest' --tests '*ShopServiceTest'`; expect missing classes/API failure.
- [x] Implement catalogue using the repository's JSON conventions, immutable offers and service dispatch. ConsumablePurchaseEffect maps Task 1 outcomes. Reject unknown/non-consumable item IDs for consumable offers. Configure five offers: HEALTH_POTION=10, SHIELD=15, SPEED_POTION=15, STRENGTH_POTION=20, FREEZE_BOMB=20. Obtain display metadata through ItemCatalog; no copied names or textures in JSON.
- [x] Rerun focused tests and `./gradlew spotlessApply core:compileJava`; expect pass.
- [x] Commit these files: `feat(shop): add configured offers and extensible purchase dispatch`.

### Task 3: Merchant session and modal input

**Files:** Create `components/shop/ShopView.java`, `ShopSessionComponent.java`, `ShopInputComponent.java`; test `components/shop/ShopSessionComponentTest.java`, `ShopInputComponentTest.java`. Modify the merchant record only in `source/core/assets/configs/friendlyNpcs.json` when the factory/view from Task 4 is ready.

**Interfaces:** `ShopView.show(Runnable onClose)`, `close()`, `refresh()`; `ShopSessionComponent(Entity player, ShopView view)` with `boolean isOpen()`, `void open()`, `void close()`; `ShopInputComponent(ShopSessionComponent session, InputProcessor shopStageInput)` with priority 19 (below existing narrative 20, above UI 10). Factory supplies Scene2D Stage as shopStageInput.

- [x] Add session tests `shouldOpenOnlyAfterMerchantFinished`, `shouldIgnoreDuplicateOpen`, `shouldReleaseOnlyOwnedLocks`, `shouldCloseOnDispose`, `shouldReleaseLocksIfViewFailsToOpen`. Mock view and real/mocked existing owner-aware services. Assert no open on spirit/cancel events, no duplicate show, and another freeze owner remains after close. A failing view initialization releases acquired ownership before propagating failure.
- [x] Add input tests `shouldConsumeEscapeWhenClosing`, `shouldPassReleaseForPreheldKey`, `shouldSwallowReleaseAfterClosing`, `shouldForwardShopClicksOnce`, `shouldConsumeCloseClick`, `shouldPassInputWhenClosed`, `shouldConsumeInventoryAndPauseKeys`. Test touchDown and touchUp with pointer/button ownership and closing synchronously. Simulate narrative final-click handling at priority 20 opening the session, and assert priority 19 does not receive that same touchDown.
- [x] Run `./gradlew core:test --tests '*ShopSessionComponentTest' --tests '*ShopInputComponentTest'`; expect new API failure.
- [x] Implement session on existing `(String npcId, Entity npc)` INTERACTION_FINISHED event filtered to merchant; use separate shop owner for controls/freeze, clear interaction prompt, refresh on existing count/gold events only while open. Guard stale callbacks after disposal because EventHandler cannot remove listeners. Register once per shop entity. Modal keyboard handling blocks gameplay and allows Escape to close; pointer/scroll forwarding goes once to stage and is then consumed, including close callbacks. Swallow only releases whose presses belong to the shop.
- [x] Rerun focused tests and `./gradlew spotlessApply core:compileJava`; expect pass.
- [x] Commit session/input/tests: `feat(shop): add merchant session and modal input ownership`.

### Task 4: Selected visual design and game wiring

**Files:** Create `components/shop/ShopDisplay.java`, `entities/factories/ShopFactory.java`; modify `screens/MainGameScreen.java` beside narrative registration; modify merchant record in `source/core/assets/configs/friendlyNpcs.json`. Add selected shop-only art assets if the approved design needs them; record exact paths in the plan after selection. Test `entities/factories/ShopFactoryTest.java` where GameExtension can exercise dependencies without an OpenGL scene.

**Interfaces:** `ShopFactory.createShop(Entity player)` returns an entity with ShopDisplay, ShopSessionComponent, ShopInputComponent and updatesWhilePaused=true. ShopDisplay extends existing UIComponent, implements ShopView and binds Task 2 catalogue/service plus InventoryComponent. Use ItemCatalog item names/descriptions/effect summaries/textures and existing ResourceService lifetime management.

- [ ] Present actual Gemini candidates and obtain Yuri's selection. Record chosen candidate path and any asset paths here. Do not choose visuals for Yuri or substitute a new visual direction.
- [ ] Add meaningful binding/factory tests feasible without GL: configured catalogue has exactly five visible consumables, initial funds determine affordability, purchase refreshes balances/counts, entity continues while paused. Run focused tests to confirm failure before wiring.
- [ ] Implement selected view with current gold, all five offers, owned counts, price/effect, Buy buttons, result message, Close. Disable unaffordable actions while service revalidates every purchase. Root is a modal full-stage actor; ensure it stays above HUD actors that move to front each frame. Layout fits 1280x800 and resizes; use ScrollPane for smaller windows. Remove actors/release loaded assets on disposal. Do not change shared skin files.
- [ ] Register ShopFactory beside existing NarrativeFactory registration. Remove only merchant cutsceneId, cutsceneTiming, lockMovementDuringCutscene. Preserve greeting, other NPCs, all narrative files and demo assets. These exact two cross-team edits were confirmed by Yuri's “开始制作” after the written spec.
- [ ] Run `./gradlew core:test spotlessCheck desktop:classes`; expect all pass. Inspect complete diff and blame for boundary violations.
- [ ] Commit approved UI/wiring/config/tests: `feat(shop): connect merchant dialogue to consumable shop`.

### Task 5: Windowed walkthrough and final audit

- [ ] Confirm local user settings fullscreen=false, then run `./gradlew desktop:run --console=plain`. Test greeting transition without video; buy one/repeat/unaffordable; close button/Escape; reopen; keyboard/mouse isolation; resize; another NPC dialogue/cutscene. Record actual manual results separately from automated tests. If unavailable, explicitly leave manual validation unverified.
- [ ] If a failure is found, identify ownership and limit fixes to approved shop/Team 5 changes. Follow existing cross-team bug rules for failures outside scope. Rerun only affected checks after repairs, then commit repairs locally.
- [ ] Check Sonar concrete issue results and their analyzed commit/time for the baseline dependency and any available shop scan. Do not trigger an upload without authorization. Local shop changes without an exact-head scan remain SonarCloud-unverified.
- [ ] Report local head, clean status, checks, selected visual, manual results and remaining limitations. No remote write is authorized by this plan.

## Execution handoff

Recommended execution: Native, with Codex implementing Tasks 1–5 in this session and one fresh GPT-6/GPT-6.1 reviewer checking the final branch. These tasks share a small set of transaction/session interfaces, so keeping implementation together avoids repeated context handoffs. Gemini visual delegation is separately required by project policy. Await Yuri's review of this plan and execution selection before product implementation.
