# Burn Vial — New Consumable Design Brief

**Author:** Aarash Mehta (Team 5)
**Ticket:** #197 — New Consumable Items and Extensible Effects (work package C1/C3)
**Status:** Original implementation preserved from `task/197-burn-vial`; local four-slot integration
is documented in [the integration record](team5-burn-vial-four-slot-integration.md). The original
commits remain attributed to Aarash. Integration does not imply publication to a remote branch.

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
  a configured 6 seconds. The earlier "3 ticks, 6 damage" estimate was incorrect: exact tick
  timing follows the existing `Burning` implementation, which applies damage before checking
  expiry. Integration tests observe six 2-damage ticks with updates spaced 1001ms apart; this
  does not establish a frame-independent total-damage guarantee.
- **Stacking / reuse:** each use applies a fresh `Burning` instance per enemy; using a second
  vial on an already-burning enemy stacks an additional burn (consistent with how
  `StatusEffectsControllerComponent` already treats multiple stacks elsewhere).
- **Failure behaviour:** mirrors Freeze Bomb — `canUse()` returns false (and consumes no stock)
  when there is no registered world camera or entity service, e.g. outside an active room.
- **Boss/immune handling:** not yet special-cased. Open question for the team: should bosses be
  exempt or resistant, the way some effects already distinguish boss behaviour? Flagging for
  agreement rather than assuming.
- **Visual feedback:** reuses the existing white-flash render-service cue Freeze Bomb uses on
  cast, plus a short orange flame icon above the player's head (`BurnVialFeedbackComponent`),
  added so the effect is actually visible when played, not just mechanically correct. No
  per-enemy burn VFX yet — coordinate with Jeremy (HUD/visual effects owner) if a flame
  overlay on the burning enemies themselves is wanted.

## Implementation

- `BurnVial.java` (`source/core/src/main/com/csse3200/game/items/consumables/`) — extends
  `ConsumableItem`, same shape as `FreezeBomb`.
- Registered as `ItemIds.BURN_VIAL` in `ItemCatalog`.
- Placeholder texture (`images/burn_vial_pixel.png`) preloaded via `RoomAssets.ITEM_TEXTURES`
  so the existing `ConsumableRoomAssetsIntegrationTest` continues to pass. Currently a
  recolored (orange/red) copy of the Freeze Bomb art, so it's at least visually distinct — but
  it is still a placeholder, not real art; flagging for whoever owns item art.
- Unit test `BurnVialTest.java` covers: catalog registration, `canUse()` failing without a
  camera/entity service, and that only on-screen enemies receive a `Burning` stack of the
  configured duration (off-screen enemies are left untouched), matching the existing
  `FreezeBombUseTest` pattern.

## Done, but flagged for review

- **Original hotbar slot.** `ConsumableSelectionComponent.SLOTS` included `ItemIds.BURN_VIAL` as a
  6th slot; `ConsumableHotbarDisplay` already sized itself dynamically off this list, so no UI
  code changes were needed. This list is shared by every player, so flagging for Yuri/Jeremy to
  review rather than assuming a 6th slot is uncontested — happy to revert to 5 if the team
  wants a different item to take the slot instead.
- **Four-slot integration (Yuezhou / Codex, October 7).** The original sixth-slot change remains
  in Aarash's commit history; the integrated implementation uses the existing inventory-driven
  four slots. Burn Vial enters the first available slot on pickup. `burnvial give` leaves one
  vial in inventory for Tab/Q testing; `burnvial` retains Aarash's immediate-use QA command.
- Verified in a live playthrough: pick up/grant via the `burnvial` debug terminal command,
  select via Tab, use via Q — damages on-screen enemies and shows the flame feedback correctly.

## Explicitly NOT done yet (needs team agreement first)

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
