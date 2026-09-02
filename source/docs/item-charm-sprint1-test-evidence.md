# Item/Charm System — Sprint 1 Test Evidence

> Verified against `main@7632062` on 3 September 2026 (Australia/Brisbane). Enemy-drop
> integration was merged in PR #74 at `a0adad1`.

## Scope

This document records the Sprint 1 verification for Team 5's Strength Charm workflow.

The integrated flow is:

1. `EnemyManagerComponent` tracks an enemy and listens for its `entityDied` event.
2. The death handler schedules item creation through `EntityService` so physics-backed entity
   creation occurs after the current update.
3. `ItemFactory.createDrop(ItemType.STRENGTH_CHARM, enemyPosition)` returns a new, positioned,
   unregistered Strength Charm entity, which the enemy manager stores and registers.
4. The player overlaps the item's `PhysicsLayer.ITEM` hitbox and triggers `itemPickup`.
5. `CharmPickupComponent` transfers the Charm to `InventoryComponent` and disposes the world entity.
6. `CharmEffectComponent` increases the player's base attack by 10 while at least one Strength Charm
   is owned, so the buff affects weapon damage.
7. Removing the last Strength Charm restores the original base attack value.

Room navigation continues to use `interact`; item pickup uses the separate `itemPickup` event. The
keyboard input emits both events for the E key without coupling their listener contracts.

## Component contract

`EnemyManagerComponent` owns the death-event listener, physics-safe deferred creation, registration,
and later cleanup of dropped items. `ItemFactory.createDrop(ItemType, Vector2)` owns item selection,
positioning, and the item entity's component setup. It returns an entity containing:

- `PhysicsComponent`
- `HitboxComponent` configured for `PhysicsLayer.ITEM`
- `ItemComponent` containing a new Strength Charm

The returned entity is positioned but deliberately unregistered. `EnemyManagerComponent` owns its
registration and keeps it in `droppedItems` for room-lifecycle cleanup.

## Automated verification

Run from the `source` directory with JDK 21:

```bash
./gradlew clean core:test
```

Local result on 3 September 2026: **355 tests passed**, including the new scene-integration
regression test. GitHub Actions also reports successful Game Unit Tests, Java Format, and Build and
Release workflows for the exact verified `main@7632062` baseline.

The focused tests are:

- `ItemFactoryTest`: verifies deterministic, independent Strength Charm drops and the required world
  components/layer.
- `CharmPickupComponentTest`: verifies interaction-gated pickup, collision filtering, and leaving pickup
  range.
- `ItemFlowIntegrationTest`: verifies Factory → ITEM collision → itemPickup → Inventory → +10
  base attack → removal → base attack restoration.
- `InventoryComponentTest`: verifies Charm add, lookup, count, and removal behaviour.
- `CharmEffectComponentTest`: verifies buff application/removal and duplicate handling.
- `EnemyManagerComponentTest`: verifies that an enemy death queues and registers a Strength Charm at
  the defeated enemy's position, in addition to room-clear and split-enemy behaviour.

## Manual Sprint demonstration

1. Start from a player with base attack 10 and an empty Charm inventory.
2. Defeat an enemy tracked by the room's `EnemyManagerComponent`.
3. Confirm a Strength Charm entity appears at the enemy death position.
4. Move the player into the item's pickup range; confirm contact alone does not collect it.
5. Press E to trigger the player's `itemPickup` action.
6. Confirm the item disappears from the world and the inventory count becomes 1.
7. Confirm player Strength (base attack) becomes 20 and weapon damage increases accordingly.
8. Remove the Strength Charm through the inventory API or the automated integration test; Sprint 1
   does not provide a player-facing removal control.
9. Confirm inventory count becomes 0 and base attack returns to 10.
10. Repeat pickup/removal to confirm the buff does not stack or persist incorrectly.

## Integration status

- PR #74 wires tracked enemy death to a physics-safe deferred Strength Charm drop in
  `EnemyManagerComponent` and is merged into `main`.
- Item Factory, Enemy Drop, Item Pickup, Inventory, non-stacking Strength buff, final-removal
  restoration, and the E-key input contract are covered by automated tests.
- A player-facing inventory removal control and pickup prompt are outside the implemented Sprint 1
  scope. Final-removal restoration is therefore demonstrated through `ItemFlowIntegrationTest`.
- The final manual game check should record enemy death, visible drop, E-key pickup, HUD count and
  Strength change. Any failure should include the commit hash, reproduction steps, expected and
  actual results, and relevant logs or screenshots.

## Manual runtime result

The desktop game was launched from the verification branch based on `main@7632062` on 3 September
2026. The game reached `MAIN_GAME`, and the runtime log recorded four successful Strength Charm
pickups. The first pickup changed base attack from 10 to 20. The following three pickups did not
apply the bonus again, providing runtime evidence that duplicate Strength Charms do not stack.

The log-backed run confirms that world Charms are reachable through the production E-key pickup
path and that Inventory → Strength buff integration executes. A screen recording or screenshot is
still required if the Sprint evidence must visually prove the exact death position and HUD values.

## Verification notes

- Local `core:test`: passed on Microsoft OpenJDK 21.0.12.1; 355 tests.
- Focused `EnemyManagerComponentTest`: 5 tests passed, including the new drop-registration check.
- Desktop runtime: reached `MAIN_GAME`; four successful pickups; base attack increased once to 20.
- Local `spotlessCheck`: blocked by a `google-java-format`/JDK tool incompatibility that raises
  `NoSuchMethodError` across all 115 Java files, including unchanged files.
- GitHub Java Format workflow: passed for `main@7632062`.
