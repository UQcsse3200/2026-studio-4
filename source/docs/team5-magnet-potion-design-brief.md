# Magnet Potion — New Consumable Design Brief

**Author:** Sumith Bagalkot (Team 5)
**Ticket:** #197 — New Consumable Items and Extensible Effects
**Status:** Implemented on `task/197-magnet-potion` (based on `consumable-items`), awaiting review.

## Summary

A drinkable consumable that turns the player into a magnet for a short time. Every Gold coin and
consumable lying near the player slides towards them and is collected automatically when it
arrives. It is the first consumable that interacts with **world items** rather than with combat
stats or enemies, so it gives the item system a new kind of effect without touching enemy or
combat code owned by other teams.

## Gameplay rule

- **Target:** Gold coins (`CURRENCY`) and consumables (`CONSUMABLE`) lying on the ground within
  **4 world units** of the player's centre.
- **Effect:** each frame, every target moves **6 units/second** straight towards the player. When
  it is within **0.4 units**, it is collected through the item's own existing `pickUp()` rule
  (Gold goes to the Gold total, consumables to their stack count) and the world entity is removed.
- **Duration:** 10 seconds.
- **Charms are not pulled.** Picking up a charm is a build decision the player should make on
  purpose with the pickup key, so the magnet never collects one for them.
- **Repeated use:** using another potion while one is active restarts the 10 seconds instead of
  stacking two magnets (handled by the existing `ConsumableEffectComponent` refresh rule, the same
  as Speed and Strength potions).
- **Failure behaviour:** `canUse()` returns false and consumes nothing when there is no entity
  service (e.g. outside an active game), or the player is dead.
- **Boss/immunity handling:** not applicable — the potion does not affect enemies.
- **Balance:** the magnet only saves walking; it never creates loot. Radius and speed are public
  constants on `MagnetPotion`, so they are easy to tune after playtesting.
- **Visual feedback:** the player is drawn with a soft golden glow for the whole duration (using
  the existing status-effect `getGlow()` hook, so no renderer changes were needed). Its remaining duration is available through the
  existing consumable effect API. The current HUD timer ring is Speed-only; Magnet does not add
  a timer ring. Items visibly slide across the floor
  towards the player while it is active.

## Implementation

- `MagnetPotion.java` (`core/src/main/com/csse3200/game/items/consumables/`) — extends
  `ConsumableItem`. `use()` returns a `MagnetEffect` so the existing consumable system owns the
  countdown and cleanup.
- `MagnetEffect.java` (`core/src/main/com/csse3200/game/components/statuseffects/`) — extends
  `TimedStatusEffect`. Each `update()` pulls nearby Gold/consumables and collects the ones that
  arrive. Item removal uses `EntityService.scheduleDisposal()` because it runs inside the entity
  update loop.
- Registered as `ItemIds.MAGNET_POTION` in `ItemCatalog`.
- Uses the existing four-slot, pickup-order consumable hotbar on `consumable-items`: a Magnet
  Potion takes the next free slot when picked up, like every other consumable, and its icon
  comes from the generic catalog-driven hotbar icon. No hotbar or selection code was changed.
- Texture `images/magnet_potion_pixel.png` (pixel-art flask with a magnet), preloaded in
  `RoomAssets.ITEM_TEXTURES`.
- `magnet` terminal command (`MagnetCommand`) gives the player potions for QA: `magnet` or
  `magnet 3`. Both are compatibility aliases for `con magnet [quantity]`; the shared
  grant implementation caps each request at 25 and never uses the granted stock.

## Tests

`MagnetPotionTest` covers:

- Catalogue creates a `MagnetPotion` with the right category, quantity and texture.
- Invalid duration is rejected.
- Using it with no entity service does nothing and consumes nothing.
- Using it consumes exactly one potion, starts a 10-second effect, and adds a glow.
- Gold inside the radius moves towards the player by the expected step.
- Items outside the radius, and charms, are not moved or collected.
- Arriving items are collected exactly once (Gold total and consumable count increase once).
- A pulled item is eventually collected.
- Nothing is pulled once the effect has expired.

No existing tests needed changes.

## Flagged for team review

- **Shared registration lines.** `ItemIds`, `ItemCatalog`, `RoomAssets` and `MainGameScreen` each
  gain one Magnet line next to the existing Burn Vial line; both are kept.
- **Loot table / shop.** Not added to drops or the shop. Whether and where it is sold or dropped is
  a team decision once the shop catalogue is agreed.

## Acceptance criteria

- Potion can be obtained (terminal command now; drop or shop once agreed) and appears in the
  next free hotbar slot with a count.
- Using it consumes exactly one potion and makes nearby Gold/consumables fly to the player and be
  collected for 10 seconds, with the player glowing.
- Charms and far-away items are left alone.
- Using it with none in inventory does nothing.
- Existing potions, Freeze Bomb and normal pickup keep working unchanged.
- `MagnetPotionTest` and a manual gameplay walkthrough both pass.
