# Item drops: Room and Item boundary

Room code chooses **what** can drop and **where**. The item registry owns **how** each item is constructed. A room never switches on charm, consumable, or currency classes.

`EnemyManagerComponent` exposes two room-owned entry points:

```java
spawnDrop(ItemType.HEALTH_POTION, 2, position);  // fixed item and quantity
spawnDrops(lootTable, position);                  // zero or more weighted results
```

Both methods register the created entities and track them for room cleanup. Enemy death schedules its drop after the physics update. Room passes the enemy's existing `EnemyType` ID to the item-owned table selector, then calls the same `spawnDrops(table, position)` path. Call these entry points from a safe update point rather than from a collision callback.

`ItemType` is the single item-ID registry. It contains the constructors for charms, consumables and currency. `ItemDropSpec` is only a selected `(itemId, quantity)` result. A stackable consumable or currency quantity is one world entity; a quantity of non-stackable charms becomes separate entities.

The default table is `core/assets/configs/default-item-drops.json`. A room may set `lootTable` in `configs/rooms.json` to another JSON file. A table may contain `enemyRules` keyed by `EnemyType.name()`. For example, `{"enemyType":"GOLEM","table":{"entries":[{"itemId":"GOLD_COIN","minQuantity":3,"maxQuantity":3}]}}` inside `enemyRules` gives Golems that rule; all enemy types without an override use the table's general entries. Split children and Cerberus heads keep the parent enemy type. The current default JSON has no enemy overrides, so current gameplay drops stay the same. Each table supports `rolls`, `noDropWeight`, and entries with `itemId`, `weight`, `minQuantity`, and `maxQuantity`. Every referenced `itemId`, range, and enemy rule is validated when the room is created. Selection uses relative integer weights and can return no drops.

The call path is `LootTable.roll` (selection) → `ItemFactory.createDrops` (unregistered entities) → Room registration and cleanup tracking. `ItemFactory` has one public creation method and does not choose random rules.
