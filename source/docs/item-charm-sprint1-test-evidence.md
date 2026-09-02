# Item/Charm System — Sprint 1 Test Evidence

## Scope

This document records the Sprint 1 verification for Team 5's Strength Charm workflow.

The intended flow is:

1. A room requests a dropped item after an enemy death event.
2. `ItemFactory.createDrop(ItemType.STRENGTH_CHARM, position)` returns a new, positioned,
   unregistered Strength Charm entity.
3. The room sets the drop position and registers the entity.
4. The player overlaps the item's `PhysicsLayer.ITEM` hitbox and triggers `interact`.
5. `CharmPickupComponent` transfers the Charm to `InventoryComponent` and disposes the world entity.
6. `CharmEffectComponent` increases the player's base attack by 10 while at least one Strength Charm
   is owned, so the buff affects weapon damage.
7. Removing the last Strength Charm restores the original base attack value.

## Component contract

`ItemFactory.createDrop(ItemType, Vector2)` owns item selection, positioning, and the item entity's
component setup. It returns an entity containing:

- `PhysicsComponent`
- `HitboxComponent` configured for `PhysicsLayer.ITEM`
- `ItemComponent` containing a new Strength Charm

The returned entity is deliberately unpositioned and unregistered. The Room feature owns its world
position and registration.

## Automated verification

Run from the `source` directory with JDK 21:

```bash
./gradlew core:test spotlessCheck
```

The focused tests are:

- `ItemFactoryTest`: verifies deterministic, independent Strength Charm drops and the required world
  components/layer.
- `CharmPickupComponentTest`: verifies interaction-gated pickup, collision filtering, and leaving pickup
  range.
- `ItemFlowIntegrationTest`: verifies Factory → ITEM collision → interact → Inventory → +10
  base attack → removal → base attack restoration.
- `InventoryComponentTest`: verifies Charm add, lookup, count, and removal behaviour.
- `CharmEffectComponentTest`: verifies buff application/removal and duplicate handling.

## Manual Sprint demonstration

1. Start from a player with base attack 10 and an empty Charm inventory.
2. Trigger an enemy death in a room that requests
   `ItemFactory.createDrop(ItemType.STRENGTH_CHARM, enemyPosition)`.
3. Confirm a Strength Charm entity appears at the enemy death position.
4. Move the player into the item's pickup range; confirm contact alone does not collect it.
5. Trigger the player's interact action.
6. Confirm the item disappears from the world and the inventory count becomes 1.
7. Confirm player Strength (base attack) becomes 20 and weapon damage increases accordingly.
8. Remove the Strength Charm from the inventory.
9. Confirm inventory count becomes 0 and base attack returns to 10.
10. Repeat pickup/removal to confirm the buff does not stack or persist incorrectly.

## Integration status

- Item Factory, Inventory, Charm Buff, and Item Pickup are covered by automated tests on the Team 5
  branch.
- Full scene-level Enemy → Room spawning still depends on the Enemy and Room feature branches wiring
  the agreed event and spawn contract into their game-area implementation.
- Any scene-level failure should be recorded with the commit hashes, reproduction steps, expected
  result, actual result, and relevant logs or screenshots.
