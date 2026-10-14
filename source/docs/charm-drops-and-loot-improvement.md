# Charm Drops Fix and Loot Improvement

Sprint 4 task: [#297](https://github.com/UQcsse3200/2026-studio-4/issues/297).
Development branch: `fix/charm-drops-and-loot-improvement`.

## Existing implementation and gaps

The merged consumable implementation already registers 13 built-in item IDs,
including all three charms, Burn Vial and Magnet Potion. The room's enemy manager
selects rewards on death, and the item factory expands charm quantities into
independent world items. Pickup, inventory ownership, charm equipment and
consumable use already share the existing item interfaces.

Before this change, only eight of the 25 room enemy types had explicit loot rules.
The other 17 types used a one-coin fallback. Burn Vial and Magnet Potion appeared
in none of the enemy tables, and Crab and Beetle had no charm reward. The merchant
catalogue did not offer Magnet Potion. Stage-one Final Boss explosive summons
were spawned as room entities without a death-loot listener.

## Merchant purchases

`configs/shops/merchant.json` now offers `MAGNET_POTION` for **20 gold**. The
existing shop catalogue, purchase service and consumable transaction add one
potion and charge once. An unaffordable purchase preserves the player's balance
and potion count. The shop renders the item using its existing catalogue metadata.

The merchant now sells all nine registered consumable types and all three charms.
Existing prices are retained; the additional offers are:

| Item | Gold |
| --- | ---: |
| Medium Health Potion | 20 |
| Large Health Potion | 35 |
| Burn Vial | 20 |
| Strength Charm | 40 |
| Speed Charm | 40 |
| Attack Speed Charm | 40 |

Charms use the existing shop category dispatch and inventory ownership/equipment
interfaces. Each purchase adds a fresh, **unequipped** charm to the backpack;
players equip it through the existing inventory controls. Payment and ownership
are committed before gold and charm notifications are published. Invalid items,
invalid prices and insufficient funds leave the inventory unchanged. The same
scrolling shop list displays both categories, including each charm's owned count.

## Enemy loot coverage and probabilities

`configs/default-item-drops.json` contains explicit rules for all 25
`EnemySpawnConfig.EnemyType` values and the internal `FINAL_BOSS_SUMMON` type.
Unknown or null types retain the existing one-coin fallback.

Each normal enemy has one guaranteed item roll with total weight 100. Freeze Bomb
retains its 25% probability. Burn Vial and Magnet Potion each receive 5%, taken
from the previous gold weight. Crab and Beetle additionally gain a 5% Strength
Charm chance, also taken from gold. Every normal enemy can now drop a charm, all
three healing sizes, Shield, Burn Vial and Magnet Potion. The complete set of
enemy tables covers all 13 registered IDs; individual normal enemies retain
their existing themed pools.

The new normal rules reuse the corresponding existing enemy archetypes:

| New enemy types | Existing table used as their configuration baseline |
| --- | --- |
| Wolf, Longwei, Minotaur, Sandeye | Medusa |
| Bug, Dark Elves | Crab |
| Zombie | Beetle |
| Knight | Golem |
| Wukong | Mummy |
| Jotunn | Cyclops |
| Crow | Wasp |
| Jingwei | Harpy |

Cerberus, Dragon, Sleipnir, Snake Mini Boss and Final Boss each have one roll:

| Reward | Chance per roll | Quantity |
| --- | ---: | ---: |
| Gold | 20% | 2–5 |
| Shield | 10% | 1 |
| Small / medium / large health potion | 7% / 7% / 6% | 1 |
| Speed Potion / Strength Potion | 5% each | 1 |
| Freeze Bomb | 15% | 1 |
| Burn Vial / Magnet Potion | 5% each | 1 |
| Strength / Speed / Attack Speed Charm | 5% each | 1 |

Cerberus keeps the existing reward timing for each of its three tracked heads.
The Final Boss reward remains tied to encounter completion.

Explosive summons have one roll with an 80% no-drop chance. The remaining chances
are gold 8%, small healing 3%, shield 2%, freeze 2%, burn 1%, magnet 1%, and each
charm 1%. Their death listener uses the room-owned item factory without adding
summons to the room-clear enemy set. Duplicate death events produce at most one
reward request. Scripted summon disposal and stage-three encounter objects do
not generate these death rewards; room disposal cancels queued rewards.

## Verification

Automated checks cover table coverage and every weighted entry boundary,
quantity ranges, configured Magnet purchases and rejection, deferred death-loot
timing, duplicate death events, split-child loot inheritance, summon cleanup,
room disposal and encounter completion.

The pickup integration test selects every registered item from the configured
Final Boss pool, creates world entities and real Box2D fixtures, sends collision
and pickup events, and checks the real inventory. All three charm types are also
checked for independent instances, stat changes and idempotent equipment
transitions. These collision events are injected; visual gameplay and physical
contact generation require a separate windowed playthrough.

Verified on October 12, 2026: the selected 20 test classes completed **166 tests**
with zero failures, errors or skips. Core compilation, `:desktop:compileJava`,
the project's full `formatCheck`, and `git diff --check` passed. The selected
classes include the loot, room lifecycle, item factory/catalogue, shop
catalogue/service/factory/session/input, inventory, pickup, charm and Magnet
Potion integration checks.

The merchant expansion was verified on October 14, 2026: 17 selected test classes
completed **127 tests** with zero failures, errors or skips, and the full
`formatCheck` passed. Coverage includes purchasing every configured offer through
the shop buttons, independent charm instances, equipment effects, rejected
transactions, inventory event consistency, owned counts, closed-shop protection,
and existing weapon-upgrade and gambling behaviour.

Static review found no additional actionable issue in the task diff. The eight
pre-existing HUD/terminal-command files were preserved against a saved local
baseline and excluded from attribution to this task. On October 14, 2026, the
user confirmed that purchase checks for all charms and consumables passed in
the updated windowed game.
