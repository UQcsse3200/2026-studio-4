# Item drops: Room and Item boundary

Room code supplies the defeated enemy type and **where** the drop appears. The item table chooses **what** and **how many**; the item registry constructs them. A room never switches on charm, consumable, or currency classes.

`EnemyManagerComponent` exposes one room-owned enemy death entry point:

```java
spawnDropsForDefeatedEnemy(enemyType, position);
```

This method selects the enemy's table, creates zero or more items, registers them, and tracks them for room cleanup. Enemy death schedules the call after the physics update and passes the existing `EnemyType` ID. Call it from a safe update point rather than from a collision callback.

`ItemType` is the single item-ID registry. It contains the constructors for charms, consumables and currency. `ItemDropSpec` is only a selected `(itemId, quantity)` result. A stackable consumable or currency quantity is one world entity; a quantity of non-stackable charms becomes separate entities.

The default table is `core/assets/configs/default-item-drops.json`. A room may set `lootTable` in `configs/rooms.json` to another JSON file. A table may contain `enemyRules` keyed by `EnemyType.name()`. For example, `{"enemyType":"GOLEM","table":{"entries":[{"itemId":"GOLD_COIN","minQuantity":3,"maxQuantity":3}]}}` inside `enemyRules` gives Golems that rule; all enemy types without an override use the table's general entries. Split children and Cerberus heads keep the parent enemy type. Each table supports `rolls`, `noDropWeight`, and entries with `itemId`, `weight`, `minQuantity`, and `maxQuantity`. Every referenced `itemId`, range, and enemy rule is validated when the room is created. Selection uses relative integer weights and can return no drops.

The current default gives every normal enemy exactly one randomly selected drop. These are initial balance values, expressed as percentages because each enemy table has 100 total weight and zero no-drop weight:

| Enemy | Gold coins | Other drops |
| --- | --- | --- |
| Crab | 70% (1–2) | Shield 20%, Health Potion 10% |
| Wasp | 70% (1–2) | Speed Potion 25%, Attack Speed Charm 5% |
| Beetle | 75% (1–2) | Shield 15%, Strength Potion 10% |
| Mummy | 65% (1–3) | Health Potion 25%, Strength Potion 5%, Strength Charm 5% |
| Medusa | 65% (1–3) | Health Potion 20%, Speed Potion 10%, Speed Charm 5% |
| Harpy | 65% (1–3) | Speed Potion 20%, Shield 10%, Attack Speed Charm 5% |
| Golem | 60% (2–4) | Shield 20%, Strength Potion 15%, Strength Charm 5% |
| Cyclops | 55% (2–5) | Health Potion 20%, Strength Potion 20%, Strength Charm 5% |

Boss types have no override and keep the general rule of one gold coin. New enemy types also use that rule until assigned their own table.

The call path is `LootTable.roll` (selection) → `ItemFactory.createDrops` (unregistered entities) → Room registration and cleanup tracking. `ItemFactory` has one public creation method and does not choose random rules.
