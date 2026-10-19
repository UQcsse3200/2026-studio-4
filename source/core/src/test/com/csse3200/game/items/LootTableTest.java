package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.Json;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootTableTest {
  private static final Set<String> BOSSES =
      Set.of("CERBERUS", "DRAGON", "SLEIPNIR", "SNAKE_MINI_BOSS", "FINAL_BOSS");
  private static final Set<String> LEGACY_NORMAL_ENEMIES =
      Set.of("CRAB", "WASP", "BEETLE", "MUMMY", "MEDUSA", "HARPY", "GOLEM", "CYCLOPS");
  private static final Set<String> CHARMS =
      Set.of(ItemIds.STRENGTH_CHARM, ItemIds.SPEED_CHARM, ItemIds.ATTACK_SPEED_CHARM);

  @Test
  void everyEnemyAndInternalSummonHasAnExplicitValidTable() {
    LootTable table = LootTable.defaultTable();
    table.validate();
    Set<String> expected = new HashSet<>();
    for (EnemyType type : EnemyType.values()) {
      expected.add(type.name());
    }
    expected.add("FINAL_BOSS_SUMMON");
    Set<String> actual = new HashSet<>();
    for (LootTable.EnemyRule rule : table.enemyRules) {
      assertTrue(actual.add(rule.enemyType), rule.enemyType);
      assertEquals(rule.table, table.forEnemy(rule.enemyType));
      rule.table.validate();
      assertEquals(1, rule.table.rolls, rule.enemyType);
      assertEquals(100, totalWeight(rule.table), rule.enemyType);
    }
    assertEquals(expected, actual);
  }

  @Test
  void everyTableEntryCanBeRolledAtBothWeightBoundaries() {
    LootTable table = LootTable.defaultTable();
    Set<String> reachableIds = new HashSet<>();
    for (LootTable.EnemyRule rule : table.enemyRules) {
      int boundary = rule.table.noDropWeight;
      for (LootTable.Entry entry : rule.table.entries) {
        reachableIds.add(entry.itemId);
        for (int choice : new int[] {boundary, boundary + entry.weight - 1}) {
          assertEquals(
              new ItemDropSpec(entry.itemId, entry.minQuantity),
              rollAt(rule.table, choice),
              rule.enemyType + ": " + entry.itemId + " at " + choice);
        }
        if (entry.maxQuantity > entry.minQuantity) {
          RandomGenerator random = mock(RandomGenerator.class);
          when(random.nextInt(totalWeight(rule.table))).thenReturn(boundary);
          when(random.nextInt(entry.maxQuantity - entry.minQuantity + 1))
              .thenReturn(entry.maxQuantity - entry.minQuantity);
          assertEquals(
              new ItemDropSpec(entry.itemId, entry.maxQuantity),
              rule.table.roll(random).get(0),
              rule.enemyType + ": maximum quantity");
        }
        boundary += entry.weight;
      }
    }
    assertEquals(ItemCatalog.ids(), reachableIds);
  }

  @Test
  void everyNormalEnemyCanDropHealingShieldCharmBurnAndMagnet() {
    LootTable table = LootTable.defaultTable();
    for (EnemyType type : EnemyType.values()) {
      if (BOSSES.contains(type.name())) {
        continue;
      }
      LootTable enemyTable = table.forEnemy(type.name());
      Set<String> ids = entryIds(enemyTable);
      assertEquals(0, enemyTable.noDropWeight, type.name());
      assertTrue(
          ids.containsAll(
              List.of(
                  ItemIds.HEALTH_POTION,
                  ItemIds.MEDIUM_HEALTH_POTION,
                  ItemIds.LARGE_HEALTH_POTION,
                  ItemIds.SHIELD,
                  ItemIds.BURN_VIAL,
                  ItemIds.MAGNET_POTION)),
          type.name());
      assertTrue(ids.stream().anyMatch(CHARMS::contains), type.name());
    }
  }

  @Test
  void legacyNormalEnemiesRetainAQuarterChanceOfFreezeBomb() {
    LootTable table = LootTable.defaultTable();
    for (String enemyType : LEGACY_NORMAL_ENEMIES) {
      LootTable enemyTable = table.forEnemy(enemyType);
      assertEquals(100, totalWeight(enemyTable), enemyType);
      assertEquals(
          25,
          Arrays.stream(enemyTable.entries)
              .filter(entry -> ItemIds.FREEZE_BOMB.equals(entry.itemId))
              .mapToInt(entry -> entry.weight)
              .sum(),
          enemyType);
    }
  }

  @Test
  void bossesCanDropEveryRegisteredItemWithTwoToFiveGold() {
    LootTable table = LootTable.defaultTable();
    for (String boss : BOSSES) {
      LootTable bossTable = table.forEnemy(boss);
      assertEquals(0, bossTable.noDropWeight, boss);
      assertEquals(ItemCatalog.ids(), entryIds(bossTable), boss);
      LootTable.Entry gold =
          Arrays.stream(bossTable.entries)
              .filter(entry -> ItemIds.GOLD_COIN.equals(entry.itemId))
              .findFirst()
              .orElseThrow();
      assertEquals(2, gold.minQuantity, boss);
      assertEquals(5, gold.maxQuantity, boss);
    }
  }

  @Test
  void explosiveSummonsHaveAnEightyPercentNoDropChance() {
    LootTable summon = LootTable.defaultTable().forEnemy("FINAL_BOSS_SUMMON");
    assertEquals(80, summon.noDropWeight);
    assertEquals(100, totalWeight(summon));
    for (int choice : new int[] {0, 79}) {
      RandomGenerator random = mock(RandomGenerator.class);
      when(random.nextInt(totalWeight(summon))).thenReturn(choice);
      assertTrue(summon.roll(random).isEmpty());
    }
    assertEquals(new ItemDropSpec(ItemIds.GOLD_COIN, 1), rollAt(summon, 80));
  }

  @Test
  void shouldLoadDefaultJsonAndDropOneCoin() {
    LootTable table = LootTable.defaultTable();
    table.validate();
    assertEquals(
        List.of(new ItemDropSpec(ItemIds.GOLD_COIN, 1)), table.roll(mock(RandomGenerator.class)));
    assertEquals(table, table.forEnemy("UNKNOWN_ENEMY"));
    assertEquals(table, table.forEnemy(null));
  }

  private static int totalWeight(LootTable table) {
    return table.noDropWeight + Arrays.stream(table.entries).mapToInt(entry -> entry.weight).sum();
  }

  private static Set<String> entryIds(LootTable table) {
    Set<String> ids = new HashSet<>();
    for (LootTable.Entry entry : table.entries) {
      ids.add(entry.itemId);
    }
    return ids;
  }

  private static ItemDropSpec rollAt(LootTable table, int choice) {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(totalWeight(table))).thenReturn(choice);
    return table.roll(random).get(0);
  }

  @Test
  void shouldSupportNoDropAndRestrictedWeightedResults() {
    LootTable table = new LootTable();
    table.rolls = 2;
    table.noDropWeight = 1;
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemIds.GOLD_COIN, 3, 4, 4)};
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(4)).thenReturn(0, 3);

    List<ItemDropSpec> results = table.roll(random);

    assertEquals(List.of(new ItemDropSpec(ItemIds.GOLD_COIN, 4)), results);
  }

  @Test
  void shouldRejectUnknownOrInvalidRulesBeforeSpawning() {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(null, 1, 1, 1)};
    assertThrows(IllegalArgumentException.class, table::validate);
    table.entries = new LootTable.Entry[] {new LootTable.Entry("Small Health Potion", 1, 1, 1)};
    assertThrows(IllegalArgumentException.class, table::validate);
    table.entries = new LootTable.Entry[0];
    table.noDropWeight = 1;
    assertTrue(table.roll(mock(RandomGenerator.class)).isEmpty());
  }

  @Test
  void shouldReadEnemyRulesFromJsonAndFallBackToGeneralRules() {
    String json =
        "{\"entries\":[{\"itemId\":\"HEALTH_POTION\"}],"
            + "\"enemyRules\":[{\"enemyType\":\"GOLEM\",\"table\":{"
            + "\"entries\":[{\"itemId\":\"GOLD_COIN\",\"minQuantity\":3,\"maxQuantity\":3}]}}]}";
    LootTable table = new Json().fromJson(LootTable.class, json);
    table.validate();
    RandomGenerator random = mock(RandomGenerator.class);

    assertEquals(
        List.of(new ItemDropSpec(ItemIds.GOLD_COIN, 3)), table.forEnemy("GOLEM").roll(random));
    assertEquals(
        List.of(new ItemDropSpec(ItemIds.HEALTH_POTION, 1)), table.forEnemy("MEDUSA").roll(random));
  }

  @Test
  void shouldReadCustomHealingAmountFromJsonAndCreateDrop() {
    LootTable table =
        new Json()
            .fromJson(
                LootTable.class,
                "{\"entries\":[{\"itemId\":\"HEALTH_POTION_75\",\"minQuantity\":2,\"maxQuantity\":2}]}");
    table.validate();

    ItemDropSpec drop = table.roll(mock(RandomGenerator.class)).get(0);
    assertEquals("HEALTH_POTION_75", drop.itemId());
    assertEquals(2, drop.quantity());
    InstantHealingPotion potion =
        (InstantHealingPotion) ItemCatalog.create(drop.itemId(), drop.quantity());
    assertEquals(75, potion.getHealing());
  }

  @Test
  void shouldRejectDuplicateEnemyRules() {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemIds.HEALTH_POTION, 1, 1, 1)};
    table.enemyRules =
        new LootTable.EnemyRule[] {
          new LootTable.EnemyRule("GOLEM", new LootTable()),
          new LootTable.EnemyRule("GOLEM", new LootTable())
        };

    assertThrows(IllegalArgumentException.class, table::validate);
  }
}
