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
- **I** opens the existing travel book. **Q / E** move backward / forward through
  Charms, Consumables and Achievements; the category tabs also jump directly.
  On item pages, **WASD / arrow keys** select equipment or stored items, and
  **Space** equips or unequips the selection. **I / Esc** closes the book.
  These inventory keys are captured while the book is open, so Q does not use
  a potion and movement keys do not move the player. Achievements retain main's
  locked / unlocked lists and numeric progress; item actions do nothing there.
- The left item page shows equipment; the right shows stored items and details.
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
  `previousPage` supports reverse navigation; `moveActiveItem` handles
  rearranging equipped consumables.
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
book cover, parchment pages, achievement service, slot grid, tooltips and action/event
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


## Potion feedback refinement

Small, medium and large instant healing potions now use distinct C-family bottle textures.
Successful healing enhances the existing HealingPotionFeedbackComponent with a
fast expanding pink-red pulse, three rising crosses and a bright core on the main
cross over 1.2 seconds. Potion healing magnitude modestly scales the visuals;
custom healing IDs use the same path. Failed uses emit no success visuals.

ShieldPotionFeedbackComponent reuses the procedural ring design from the existing
consumable-world-effects branch. The blue barrier expands over 220 ms, pulses
with counter-rotating bright arcs, and fades during the last 450 ms. It queries
the actual ConsumableEffectComponent shield, including refresh, expiry and early
removal. No independent protection timer or stats changes are introduced.
PlayerFactory adds one component; inventory, HUD, damage mitigation and shared
rendering code are unchanged by this refinement. Procedural textures are cached,
released on disposal and rendering restores batch colour and separate blend state.


## C-family healing potion icons

Imagegen-produced transparent PNGs distinguish the built-in tiers by silhouette:
25 HP uses a small teardrop vial, 50 HP uses a heart-shaped flask with a silver
stopper, and 100 HP uses a broad crystal flask with a gold crown. The item itself
selects its texture; inventory and drops reuse that metadata. The HUD trims each
new icon using its own measured alpha bounds. RoomAssets adds the three resources
to its existing load/unload list. Custom positive healing amounts retain the
original generic potion texture. IDs, healing values, quantities and effects are
unchanged. No existing artwork is overwritten.

## Healing drops and floating numbers

All eight configured normal enemy types can now roll small, medium and large
healing potions. Existing healing drop weight is divided between the three tiers;
enemies without a healing entry exchange ten percentage points of coin weight
for healing drops. Other item weights, drop counts and boss fallback stay intact.

The existing healing feedback listens to `updateHealth` followed by successful
`itemUsed` and displays the actual gain as a rising, fading pixel-font number.
Built-in values remain 25, 50 and 100; custom 75 HP potions show +75, while a
health-capped heal shows only the HP actually restored. The feedback owns and
disposes its font without changing shared HUD styles or combat/item interfaces.
World-space font rendering disables integer positions so scaled glyphs retain
their height instead of rounding down to zero. A regression test exercises the
real bitmap-font vertex generation at the feedback's world scale.

The floating number uses the same red as the healing crosses, with a black shadow.
Final local verification on 2026-10-07 passed 82 relevant tests (zero failures,
errors or skips), desktop compilation, core JAR assembly and Spotless checks.
Yuri confirmed the floating numbers and the subsequent red colour in windowed
gameplay. A final gameplay recording has not been supplied.

## Burn Vial remote adaptation on 2026-10-07

Integrated `consumable-items` through `e456c69b`, preserving Aarash's original
Burn Vial history and the upstream four-slot QA and enemy-flame refinements.
Burn Vial reuses the existing Burning implementation, affects onscreen enemies,
and supplies player feedback, enemy-attached flame pulses and a warm screen flash.
The original item, command and rendering changes are retained. Our existing
healing icons/red numbers, shield visuals and fountain-drop animation are retained.

The only merge conflict was the hotbar test's texture fixture list; both the three
healing textures and the Burn Vial texture are loaded. No equipment-slot rewrite
or additional loot chance change was needed. Use `burnvial give` to exercise the
existing pickup and Tab/Q path, or the upstream `burnvial` QA command for its demo.

Full local verification passed 1964 tests with zero failures, errors or skips,
desktop compilation, core JAR assembly and Spotless checks. This adaptation has
not been launched for manual gameplay verification or pushed to a remote branch.

At Yuri's request, the player-head three-tile Burn Vial flame was removed locally.
Enemy-attached flames, damage pulses, the warm flash and actual Burning behavior
remain unchanged. The player-only timer/event hook and pixel texture were removed
with that visual. 57 related tests, desktop build and Spotless checks passed.

Yuri approved a distinct olive-green glass Molotov-style bottle with orange-red
fuel at its base and a cloth wick. Its transparent PNG replaces the Burn Vial
artwork at the existing asset path; the HUD trims its measured transparent padding.
Inventory and world drops reuse the existing item texture metadata. Item IDs,
quantities, damage and burning behavior are unchanged by this artwork update.
The final bottle-art version passed 61 related tests, desktop compilation, core
JAR assembly and Spotless checks. Its in-game appearance remains for manual review.


## Combined local HUD refresh — 7 October 2026

Integrated Jeremy's remote `task/197-consumable-hud@1ff6c5f9` into the local consumable
integration while retaining Sumith's Magnet Potion and the fountain/magnet movement adaptation.
The merge keeps both asset fixtures and both terminal registrations. Original contributor
commits are preserved.

The enhanced healing feedback replaces the older single-cross implementation. Three healing
tiers share the same item/use path, while using distinct bottle textures. The new shield feedback
reads the existing consumable effect rather than duplicating protection or timing. Burn Vial uses
Jeremy's updated bottle art and enemy-attached flames; the redundant player-head flame is removed.
His healing-tier loot table changes are retained, including the documented coin-weight exchange
for enemy types that previously lacked healing drops. Existing shared combat/physics interfaces
are not modified.

`con small`, `con medium`, `con large`, `con shield`, `con speed`, `con strength`, `con freeze`,
`con burn` and `con magnet` optionally accept a positive quantity, capped at 25 per request.
`con heal medium 3` is also supported. The legacy `magnet [quantity]` command delegates to the
same grant implementation, preserving its default quantity of one. `burnvial` keeps its distinct
immediate-use QA mode; `burnvial give` remains available. Grant commands never automatically use
items. Custom healing amounts retain the original generic texture.

Validation: 1,988 tests across 254 suites, zero failures/errors/skips; desktop classes and
Spotless checks passed. Tests retain the Magnet HUD/physics/lifecycle coverage, exercise actual
capped healing numbers/font rendering and shield expiry/refresh/batch restoration, and verify
both Magnet command entry points share the quantity cap. The new command regressions failed
before the adapter and passed afterwards. No full-game visual playtest was performed in this
refresh.

SonarCloud PR #221 still reports gate OK and zero concrete unresolved issues for the historical
`ed8ba209` analysis on 4 October 2026 at 17:37:01 Brisbane time. That scan does not cover Jeremy's
new head or this combined local integration. Local integration/validation are complete; the new
SonarCloud scan is unverified. No remote write or source-uploading scan is performed.

## Main compatibility (2026-10-07)

This branch integrates main at `9e70da4d`. The narrative commands and input guards,
achievement page, and save/load flow are retained. Inventory rendering changes stay
inside the book; item actions reuse the existing inventory and effect interfaces.

Version 1 saves now include optional `consumableSlots` and `charmEquipped` fields.
New saves preserve all four slot positions (including gaps), backpack-only items,
and each individual charm's equipped state. Legacy saves without these fields
retain main's original restore behavior. Duplicate charm copies are restored
individually so their effects are neither lost nor applied twice.

Before review, manually check all three book categories, keyboard and mouse
equipment changes, save/load after rearranging slots and unequipping a charm,
and inventory input alongside NPC dialogue and the exit/save dialog. Automated
coverage does not establish final visual appearance or frame-time performance.

Adaptation verification: `./gradlew test :desktop:classes :core:jar` passed
**2,180 tests across 275 suites, with zero failures, errors or skipped tests**.
`./gradlew spotlessCheck` passed. The new coverage includes all three book
categories, reverse navigation, achievement progress, item input isolation on the
achievements page, and actual JSON save/load round trips for slot and charm state.
No game was launched during this adaptation; the manual checks above remain open.
