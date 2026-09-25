# Item drops: Room and Item boundary

The current Team 5 item flow starts when `RoomFactory` attaches an `EnemyManagerComponent` to a room. The manager creates an `ItemFactory`, which loads `core/assets/configs/default-item-drops.json`. Room code supplies the defeated enemy's type and death position; the factory owns loot selection and creates unregistered world entities. The manager registers them and owns their cleanup.

For a tracked enemy, `entityDied` calls `onEnemyDefeated`, which schedules `spawnEnemyDrops`. That calls `ItemFactory.createEnemyDrops(enemyType, position)`. The factory selects the enemy-specific `LootTable` rule, rolls one or more `ItemDropSpec(itemId, quantity)` values, resolves each stable string ID through `ItemCatalog`, and wraps each fresh `Item` in an entity. The public `EnemyManagerComponent.spawnItem(itemId, quantity, position)` method supports an explicitly selected pickup without enemy death.

`ItemCatalog` is only an ID-to-constructor map. It does not store names, descriptions, textures, categories, or effects. Those belong to `Item` and its concrete subclasses. Three healing sizes share `InstantHealingPotion`, with separate stable IDs and healing amounts. A stackable consumable or currency quantity becomes one world entity; multiple Charms become independent instances because their pickup state is per item.

The default JSON contains general entries and `enemyRules` keyed by enemy type. Each table supports `rolls`, `noDropWeight`, and entries with `itemId`, `weight`, `minQuantity`, and `maxQuantity`. Unknown IDs and invalid ranges are rejected during table validation. JSON IDs must be stable keys such as `HEALTH_POTION`, never player-facing display names. Bosses and unlisted enemies use the general one-coin rule; normal enemies use their configured weighted rules.

Pickup reads the `Item` from `ItemComponent` and calls its `pickUp` behavior. `InventoryComponent` groups consumable quantities by stable ID, while Charms remain separate objects. The use component creates the concrete consumable for that ID and invokes its `canUse` and `use` methods; successful use removes one unit and emits the ID in `itemUsed`.
