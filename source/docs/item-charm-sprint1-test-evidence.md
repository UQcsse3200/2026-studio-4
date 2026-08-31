# Item/Charm System — Sprint 1 Test Evidence

## Scope

This document records the Sprint 1 verification for Team 5's Strength Charm workflow.

The intended flow is:

1. `EnemyManagerComponent` listens to each tracked enemy's `entityDied` event and snapshots its
   final position.
2. Drop creation is queued until the room manager's next update, after the physics step has
   unlocked.
3. `ItemFactory.createDrop(ItemType.STRENGTH_CHARM, position)` returns a new, positioned,
   unregistered Strength Charm entity.
4. The room registers and owns the dropped entity.
5. The player overlaps the item's `PhysicsLayer.ITEM` hitbox and triggers `interact`.
6. `CharmPickupComponent` transfers the Charm to `InventoryComponent` and disposes the world entity.
7. `CharmEffectComponent` increases Strength by 10 while at least one Strength Charm is owned.
8. Removing the last Strength Charm restores the original Strength value.

## Component contract

`ItemFactory.createDrop(ItemType, Vector2)` owns item selection, positioning, and the item entity's
component setup. It returns an entity containing:

- `PhysicsComponent`
- `HitboxComponent` configured for `PhysicsLayer.ITEM`
- `ItemComponent` containing a new Strength Charm

The returned entity is positioned but deliberately unregistered. The Room feature owns its
registration and lifecycle.

## Automated verification

Run from the `source` directory with JDK 21:

```bash
./gradlew core:test spotlessCheck
```

The focused tests are:

- `ItemFactoryTest`: verifies deterministic, independent Strength Charm drops and the required world
  components/layer.
- `EnemyManagerComponentTest`: verifies the agreed Enemy → Room contract, deferred factory creation,
  final-position snapshots, duplicate-death protection, production Strength Charm creation, room
  registration, and room-owned disposal.
- `CharmPickupComponentTest`: verifies interaction-gated pickup, collision filtering, and leaving pickup
  range.
- `ItemFlowIntegrationTest`: verifies Factory → ITEM collision → interact → Inventory → +10 Strength →
  removal → base Strength restoration.
- `InventoryComponentTest`: verifies Charm add, lookup, count, and removal behaviour.
- `CharmEffectComponentTest`: verifies buff application/removal and duplicate handling.

## Manual Sprint demonstration

1. Start from a player with base Strength 10 and an empty Charm inventory.
2. Trigger an enemy death in a room that requests
   `ItemFactory.createDrop(ItemType.STRENGTH_CHARM, enemyPosition)`.
3. Confirm a Strength Charm entity appears at the enemy death position.
4. Move the player into the item's pickup range; confirm contact alone does not collect it.
5. Trigger the player's interact action.
6. Confirm the item disappears from the world and the inventory count becomes 1.
7. Confirm player Strength becomes 20.
8. Remove the Strength Charm from the inventory.
9. Confirm inventory count becomes 0 and Strength returns to 10.
10. Repeat pickup/removal to confirm the buff does not stack or persist incorrectly.

## Integration status

- Item Factory, Inventory, Charm Buff, and Item Pickup are covered by automated tests on the Team 5
  branch.
- Enemy → Room → Strength Charm spawning is wired through `EnemyManagerComponent` using the agreed
  `entityDied` contract.
- Collision-driven enemy disposal is tracked separately by bug #54 and PR #56. That fix must land
  before the complete lethal-weapon collision path can be demonstrated without the Box2D world-lock
  crash.
- Any scene-level failure should be recorded with the commit hashes, reproduction steps, expected
  result, actual result, and relevant logs or screenshots.

## AI assistance disclosure

OpenAI Codex assisted with repository inspection, implementation review, test design, validation,
and this documentation update. Yuezhou Wang reviewed the resulting work and remains responsible for
its correctness and integration.
