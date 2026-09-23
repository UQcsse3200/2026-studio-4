# Item drops: Room and Item boundary

Room code chooses **what** can drop and **where**. The item registry owns **how** each item is constructed. A room never switches on charm, consumable, or currency classes.

`EnemyManagerComponent` exposes two room-owned entry points:

```java
spawnDrop(ItemType.HEALTH_POTION, 2, position);  // fixed item and quantity
spawnDrops(lootTable, position);                  // zero or more weighted results
```

Both methods register the created entities and track them for room cleanup. Enemy death schedules its drop after the physics update, then uses that enemy's configured table. Call these entry points from a safe update point rather than from a collision callback.

`ItemType` is the single item-ID registry. It contains the constructors for charms, consumables and currency. `ItemDropSpec` is only a selected `(itemId, quantity)` result. A stackable consumable or currency quantity is one world entity; a quantity of non-stackable charms becomes separate entities.

The default table is `core/assets/configs/default-item-drops.json`. A room may set `lootTable` in `configs/rooms.json` to another JSON file. Each enemy spawn can also set `lootTable` to override the room's table. For example, `{"type":"GOLEM","x":5,"y":10,"lootTable":"configs/golem-drops.json"}`. An enemy without an override uses the room table; split children and Cerberus heads inherit their parent's table. Each table supports `rolls`, `noDropWeight`, and entries with `itemId`, `weight`, `minQuantity`, and `maxQuantity`. Every referenced `itemId` and range is validated when loaded. Selection uses relative integer weights and can return no drops.

The call path is `LootTable.roll` (selection) → `ItemFactory.createDrops` (unregistered entities) → Room registration and cleanup tracking. `ItemFactory` has one public creation method and does not choose random rules.
