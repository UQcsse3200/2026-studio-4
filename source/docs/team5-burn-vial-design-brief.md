# Burn Vial — New Consumable Design Brief

**Author:** Aarash Mehta (Team 5)
**Ticket:** #197 — New Consumable Items and Extensible Effects (work package C1/C3)
**Status:** Local implementation on `task/197-burn-vial`, not yet pushed or reviewed.

## Summary

A thrown consumable that sets every enemy currently visible on screen alight, dealing damage
over time. Reuses the existing `Burning` status effect (already implemented and merged on
`items` for enemy self-damage / other combat interactions) rather than introducing a new
damage-over-time system, following the same reuse pattern Yuri used for the Freeze Bomb
(which calls into the existing `FrozenEffect`/status-effect system instead of building its own).

## Gameplay rule

- **Target:** every enemy entity currently inside the player's camera viewport (identical
  targeting rule to Freeze Bomb — `StrategyOnscreen`).
- **Effect:** each targeted enemy receives one `Burning` stack: 2 damage every 1 second, for
  6 seconds total (3 ticks, 6 damage per enemy per vial).
- **Stacking / reuse:** each use applies a fresh `Burning` instance per enemy; using a second
  vial on an already-burning enemy stacks an additional burn (consistent with how
  `StatusEffectsControllerComponent` already treats multiple stacks elsewhere).
- **Failure behaviour:** mirrors Freeze Bomb — `canUse()` returns false (and consumes no stock)
  when there is no registered world camera or entity service, e.g. outside an active room.
- **Boss/immune handling:** not yet special-cased. Open question for the team: should bosses be
  exempt or resistant, the way some effects already distinguish boss behaviour? Flagging for
  agreement rather than assuming.
- **Visual feedback:** reuses the existing white-flash render-service cue Freeze Bomb uses on
  cast. No dedicated burn VFX yet — coordinate with Jeremy (HUD/visual effects owner) if a
  flame overlay on burning enemies is wanted.

## Implementation

- `BurnVial.java` (`source/core/src/main/com/csse3200/game/items/consumables/`) — extends
  `ConsumableItem`, same shape as `FreezeBomb`.
- Registered as `ItemIds.BURN_VIAL` in `ItemCatalog`.
- Placeholder texture (`images/burn_vial_pixel.png`, currently a copy of the Freeze Bomb art)
  preloaded via `RoomAssets.ITEM_TEXTURES` so the existing
  `ConsumableRoomAssetsIntegrationTest` continues to pass. Needs real art — not something I can
  produce myself; flagging for whoever owns item art.
- Unit test `BurnVialTest.java` covers: catalog registration, `canUse()` failing without a
  camera/entity service, and that only on-screen enemies receive a `Burning` stack of the
  configured duration (off-screen enemies are left untouched), matching the existing
  `FreezeBombUseTest` pattern.

## Explicitly NOT done yet (needs team agreement first)

- **Hotbar slot.** `ConsumableSelectionComponent` and `ConsumableHotbarDisplay` currently support
  exactly five consumable types (Health, Shield, Speed, Strength, Freeze Bomb) and the hotbar is
  already full. Adding Burn Vial as a sixth playable consumable means either expanding the
  hotbar or swapping/rotating a slot — both are shared-UI decisions that belong to whoever owns
  the hotbar (Jeremy) and the overall item selection (Yuri), not something I should silently
  wire in.
- **Loot table drop weights.** Not added to `default-item-drops.json`. Per the Sprint 3 plan,
  which item(s) actually get implemented/dropped is a C2 "select and formalize" decision the
  team makes after reviewing design briefs, not something to pre-empt alone.
- **Shop availability.** Out of scope for this brief; follows from the B1 catalogue decision
  Sumith is currently working.

## Acceptance criteria (once the team agrees to proceed)

- Vial can be obtained (drop or specified reward) and appears in inventory with a count.
- Using it applies `Burning` to every currently-visible enemy and consumes exactly one vial.
- Using it with none in inventory, or with no active camera/room, does nothing and consumes
  nothing.
- Existing four Sprint 2 potions and Freeze Bomb continue to work unchanged.
- Automated test (`BurnVialTest`) and a manual gameplay walkthrough both pass.
