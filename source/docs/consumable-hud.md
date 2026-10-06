# Consumable HUD and inventory equipment

Related feature: #197. The HUD uses main's `ItemIds`, `ItemCatalog`,
`InventoryComponent` and `ConsumableEffectComponent`.

## Controls and equipment

- Four physical consumable slots start empty. New item IDs fill the first empty
  slot in pickup order; repeated pickups stack. Using the last unit clears its
  assignment without moving other slots. Item types that do not fit remain in
  the backpack.
- **Tab** cycles all four slots, including empty slots. **Q** requests use of
  the selected item through the existing effect component. Empty slots and
  rejected uses consume nothing. Freeze Bomb and healing potion variants use
  their existing catalog/effect implementations.
- **I** opens the existing inventory book. Its arrow switches between Charms
  and Consumables. The left page shows equipment; the right shows stored items.
  Clicking equips/unequips; dragging can choose a consumable slot or swap two
  occupied slots without losing stock. Dropping outside a target changes nothing.
- Charms still equip automatically on pickup. Unequipping keeps the charm in
  inventory and removes its existing effect; reequipping applies it once.
  Large charm inventories scroll within the existing book pages.
- Hovering an item displays its text beside the pointer immediately, without
  scaling or fading. The inventory owns its tooltip manager, so other UI keeps
  its existing tooltip timing and animation settings.
- Item introductions use the existing `Item.getDescription()` field; effect
  summaries use `getEffectSummary()`. New items supply their description in
  their existing constructor. No separate UI text registry is maintained.
- The existing `nextPage`, `moveActiveToInactiveItem` and
  `moveInactiveToActiveItem` events are handled by `InventoryActions`. An
  additional `moveActiveItem` event handles rearranging equipped consumables.
  The display binds item data and emits requests; it does not apply effects.
- Counts use `consumableInventoryChanged(String, count)`. Empty slots hide their
  icon, count and Q hint. Gold follows the actual inventory balance. The Speed
  timer ring follows its slot and the existing effect's remaining duration.
- Weapon keys 1–3 and the bottom weapon/spell bar retain main's behavior.
  Consumable number keys 7/8/9/0 remain unbound.

## Drop visuals and scope

`ItemFactory` uses the drop-specific `ItemDropAnimationComponent` to render
world items. Enemy rewards start at one common visual point, receive radial
velocities through the existing physics body and rise along a 0.7-second visual
arc. Terrain raycasts limit their travel near walls, and their bodies stop at
landing. Motion randomness is separate from loot rolls. Configured loot probabilities,
item quantities, room ownership and pickup events retain their existing behavior.

After landing, the whole icon rocks left and right about its bottom-centre pivot
for 3.2 seconds with decreasing amplitude. The pivot stays fixed and the texture
keeps its shape, like a lever rotating about its base. Each item samples its own
0.5–0.7-second rocking period once at creation, independently of loot randomness. A wider mosaic reflection with a
bright additive core follows the same bottom-pivot rotation and sweeps for one second
every 2.1 seconds. Regions and the vertex buffer are cached. The reflection
restores SpriteBatch color and blend state, including when drawing throws.

The shared `RotatingTextureRenderComponent` remains identical to main. The
specialized drop renderer keeps deformation out of weapon rendering. The inventory retains main's
book cover, two page-building methods, slot grid, tooltips and action/event
structure, with data binding, four consumable slots and bounded scrolling added.

## Verification

Run from `source`:

```sh
./gradlew core:test spotlessCheck desktop:classes --offline
```

Historical verification from the remote HUD implementation follows; these counts
do not describe the current combined working tree.

Initial HUD integration result on 2026-10-04: **1,322 tests in 193 suites; zero failures, errors or skipped
tests**. Formatting and desktop compilation passed.

Pickup-order update on 2026-10-04: `./gradlew test` passed **1,325 tests** with
zero failures, errors or skipped tests. Regression coverage checks Shield picked
first, duplicate stacking, clearing and reusing a gap without moving Strength,
Q using the displayed item, pickups made before player creation, and removed
7 / 8 / 9 / 0 bindings leaving stock and effects untouched. The updated
game also started successfully at the main menu.

Coverage exercises the real enum-based inventory and effect components:
quantity/visibility updates, successful and rejected Q use, four-slot selection
and wrapping, shield/speed/strength effects, speed arc refresh/expiry/removal,
Gold refresh, 1920×1080 / 1280×720 / 906×600 layouts and HUD replacement/disposal.
Input tests check that Tab/Q trigger requests on key-down only.

A temporary local LWJGL smoke harness also entered the actual `MainGameScreen`,
seeded five of each existing potion and 25 Gold, and sent Tab, Tab, Q through the
real `InputService`. It verified Speed selection, stock dropping from five to
four and a live speed effect, then captured the rendered scene below. The fixture
is verification setup, not a change to starting inventory. This does not replace
a full manual gameplay walkthrough.

![Right-side consumable HUD with an active Speed potion](images/consumable-hud.png)

For a manual walkthrough, start `./gradlew desktop:run`, enter gameplay, collect
potions with the existing pickup interaction, cycle with Tab and use with Q.
Check empty/full-health use, effect refresh/expiry, Gold pickups, inventory book
visibility and resizing. Confirm no duplicate old consumable HUD appears.

## Local ItemIds integration (2026-10-06)

The original HUD and pickup-order commits by Jeremyzihanwu are retained through a
merge of `origin/consumable-items`. The integration adapts their four physical slots
to the current ItemCatalog/String IDs, without reintroducing the retired ItemType
API or loadout component. Freeze Bomb and other registered consumables use the same
slot assignment and their catalog textures. Existing local consumable feedback and
the eight-second shield using the original Shield mitigation are retained.
The earlier verification counts above describe the original remote implementation.
Tests cover stock, slot assignment/swapping, Tab/Q including slot four, potion
and Freeze Bomb effects, actual Scene2D dragging and rejected drops, charm
apply/remove transitions, many charms inside scroll panes, existing page events,
page visibility/layering, bottom-pivot rocking with independent random periods
and settling completion, common burst
origins, radial flight and terrain limits, cached geometry and batch
state restoration. Rendering appearance and frame time still need assessment
in the actual game; headless tests do not establish those properties.


## Consumable-items adaptation (2026-10-06)

Merged `origin/consumable-items` at `a6a4a209` into
`task/197-consumable-hud`. Its eight-second consumable shield now delegates damage
mitigation to the existing Shield implementation. WASP, MUMMY, MEDUSA and CYCLOPS
join the other normal enemies with a configured shield drop chance. Their gold
weights change as specified by the remote loot table; the remaining weights are
preserved. The four slots, current inventory interactions, immediate tooltip,
fountain drops and independently timed bottom-pivot rocking remain in place.

Adaptation verification: 166 relevant tests passed with no failures, errors or
skips; desktop classes, core JAR and Spotless checks passed. Shield lifetime,
every configured normal enemy shield roll, four-slot input, inventory actions
and approved drop animation regressions were exercised. No game was launched
for this adaptation; runtime confirmation remains with Yuri.

Pre-push verification of the combined local version: 1933 tests,
0 failures, 0 errors and 0 skips.
Desktop classes, core JAR and Spotless checks passed. No game was launched.
