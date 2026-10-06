# Consumable shop: first playable version

Status: proposed design for Yuri's review; product implementation has not started.

## Goal and agreed scope

After talking to the Travelling Merchant, the player can buy existing consumables with inventory gold, see the updated quantities, close the shop, and resume the game. Yuri confirmed consumables only for the first visible shop, with extension interfaces for charms and weapon upgrades and no UI for those categories.

Work stays on the existing `shop` branch based on `items` plus PR #231. No new dialogue system, item registry, inventory, currency store, or video player is needed. Remote pushes and PR actions require separate specific authorization.

## Approach

Use a Team 5 shop service and session component alongside the existing narrative system. Remove the merchant's demo-video step from its NPC configuration and open the shop from the existing `(npcId, npc)` interaction-finished event, filtered to `merchant`. Preserve the greeting, NPC interaction sequence, and other NPCs.

Alternatives considered: putting purchase logic directly in Scene2D buttons is smaller initially but couples transaction rules to rendering; extending the narrative manager to route shop scenes changes Team 3's generic interfaces. A separate shop service plus an existing completion event gives reusable purchase rules and the smallest cross-team integration surface.

## Player flow and proposed defaults

1. Press E near the merchant and advance the existing greeting.
2. The greeting ends and the shop opens immediately, without the demo video.
3. The shop shows gold and a fixed consumable catalogue. Each entry shows its existing icon, name, effect description, unit price, owned quantity, and buy action.
4. One purchase buys one unit. Gold and owned counts refresh immediately. Insufficient gold disables buying and explains why; the purchase service also checks the balance itself.
5. Close button or Escape closes the shop. World updates and controls resume. Talking again reopens the same catalogue and current inventory.

Initial proposed configurable prices: HEALTH_POTION 10 gold, SHIELD 15 gold, SPEED_POTION 15 gold, STRENGTH_POTION 20 gold, FREEZE_BOMB 20 gold. These are playable test values, not final economy balancing. Supply is unlimited for this version. No selling, restocking, random offers, bulk purchasing, additional currency, or new inventory capacity rule.

No debug gold grant is part of normal gameplay. Automated tests create funded inventories; manual testing can use existing development tools if needed.

## Data and extension boundary

- A shop catalogue configuration contains merchant/catalogue ID and offers: offer ID, product kind, product ID, positive integer gold price. Names, effects, and textures come from ItemCatalog and existing items.
- Product kinds include CONSUMABLE, CHARM, and WEAPON_UPGRADE. Only consumable offers are configured and displayed in this version.
- A purchase-effect interface validates whether a product can be received and applies the fulfillment within the purchase transaction. Only a consumable implementation is registered. Unsupported kinds fail explicitly; no placeholder deduction or fake successful purchase is allowed.
- The purchase service receives an offer ID and resolves the trusted catalogue price; the UI cannot supply its own price. Results distinguish success, insufficient gold, invalid offer, unsupported product, and quantity limit.
- Future charm and upgrade handlers can register through the same boundary without adding branches to the display. Do not implement their inventory or upgrade effects in this version.

## Transaction contract

InventoryComponent remains the single source of gold and consumable counts. Add a narrowly scoped inventory operation to purchase a valid consumable for a validated positive price. It checks the item type, balance, and integer count overflow before mutation; an expected rejection changes neither gold nor quantity.

Commit both values before publishing inventory notifications so listeners observe the completed purchase. Publish the existing consumableInventoryChanged event and a goldChanged event with the final balance. Gold setters also publish goldChanged when attached to a player and the balance actually changes. Constructors remain safe before entity attachment. Tests include final-state observations from event callbacks.

Transactions run on the game's existing single update/input thread. The failure guarantee covers validation rejection, not arbitrary exceptions thrown by unrelated event subscribers. Avoid removing or changing existing public inventory methods.

## Session, input, and lifetime

Shop UI and session live on a separate entity that continues updating while the world is frozen. Use the existing owner-based EntityService freeze and PlayerActions control locks. Closing/disposal releases only the shop's own owners, never another system's lock.

While open, the shop consumes gameplay keys, including inventory-toggle and pause keys; Escape closes the shop. Mouse input reaches the Scene2D shop controls and cannot attack behind the modal. Balance key/button releases when the modal opens or closes, and prevent the click advancing the last dialogue line from also buying an item. Opening is idempotent; stale events cannot create two sessions. Disposal closes the shop and releases its locks. Normal gameplay remains windowed.

## Visual design handoff

Gemini supplies visual candidates through a native GPT-6/GPT-6.1 liaison using agy CLI, as required by project instructions. Provide existing shopkeeper and consumable textures, game screenshots, current UI assets, the 1280x800 window target, resizing constraints, and the interaction/data contract above. Yuri selects a candidate before UI implementation. Do not show charm or weapon upgrade tabs or placeholders. Layout, colors, and decoration remain Gemini's design task; this document specifies information and behavior only.

## Modification boundaries requiring confirmation

New Team 5 files: shop catalogue, purchase service and effect interfaces, session/input/display components, factory, and focused tests. Team 5 inventory changes: the atomic consumable purchase operation and gold-change notification in InventoryComponent.

Cross-team/shared changes, proposed only:

- `source/core/assets/configs/friendlyNpcs.json`: Wouter Teunisse / Team 3 NPC configuration, introduced by PR #216. Remove only the merchant's cutsceneId, cutsceneTiming, and lockMovementDuringCutscene fields; retain merchant greeting and every other NPC record. Do not edit/delete merchant_opens_shop.json or its video assets.
- `source/core/src/main/com/csse3200/game/screens/MainGameScreen.java`: shared initialization; the nearby narrative registration was added by Benjamin Chau / Team 3 in PR #231. Add ShopFactory registration beside narrative initialization. Do not alter the narrative registration, HUD assembly, or lifecycle behavior of other systems.

These edits require Yuri's specific confirmation under AGENTS.md before touching product files. Blame and PR history support the attribution above; latest Discord confirmation is unavailable because the Chrome connection is disconnected.

## Acceptance and verification

- Successful and repeated purchases deduct the exact configured price and add one item each time.
- Insufficient funds, invalid items/prices, unsupported categories, and maximum integer quantities leave inventory unchanged.
- Event listeners see the final gold and quantity; relevant HUD counts and shop affordability refresh.
- Merchant-only completion opens the shop; other NPC completion does not. Closing, repeated opening, and disposal do not leak freezes or controls locks.
- Windowed manual walkthrough covers greeting-to-shop transition, buy success/failure, Escape/close, input isolation, reopening, resizing, and another NPC's dialogue/cutscene.
- Run focused meaningful tests, core:test, spotlessCheck, and desktop:classes after implementation; commit this turn's changes locally. Check Sonar's concrete findings for relevant scanned heads, while reporting the local shop head as unverified if no matching scan exists.

## Delivery order

After this written design is reviewed: create the detailed implementation plan; prepare Gemini visual candidates while settling catalogue/purchase tests; implement the transaction service and extension interface; add the approved integration and modal lifetime/input; implement the selected visual design; run tests and windowed playtest; save local commits. Pushes, approvals, and PR creation are separate actions.
