package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.Json;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.consumables.InstantHealingPotion;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootTableTest {
  @Test
  void shouldLoadDefaultJsonAndDropOneCoin() {
    LootTable table = LootTable.defaultTable();
    table.validate();
    assertEquals(
        List.of(new ItemDropSpec(ItemIds.GOLD_COIN, 1)), table.roll(mock(RandomGenerator.class)));
  }

  @Test
  void shouldGiveEveryNormalEnemyAQuarterChanceOfFreezeBomb() {
    LootTable table = LootTable.defaultTable();
    table.validate();
    Set<String> normalEnemies =
        Set.of("CRAB", "WASP", "BEETLE", "MUMMY", "MEDUSA", "HARPY", "GOLEM", "CYCLOPS");
    assertEquals(
        normalEnemies,
        java.util.Arrays.stream(table.enemyRules)
            .map(rule -> rule.enemyType)
            .collect(java.util.stream.Collectors.toSet()));
    for (String enemyType : normalEnemies) {
      LootTable enemyTable = table.forEnemy(enemyType);
      assertEquals(1, enemyTable.rolls);
      assertEquals(0, enemyTable.noDropWeight);
      assertTrue(enemyTable.entries.length > 1);
      assertEquals(
          100, java.util.Arrays.stream(enemyTable.entries).mapToInt(entry -> entry.weight).sum());
      assertEquals(
          25,
          java.util.Arrays.stream(enemyTable.entries)
              .filter(entry -> ItemIds.FREEZE_BOMB.equals(entry.itemId))
              .mapToInt(entry -> entry.weight)
              .sum());
    }

    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(100)).thenReturn(0, 74, 99);
    assertEquals(ItemIds.GOLD_COIN, table.forEnemy("MEDUSA").roll(random).get(0).itemId());
    assertEquals(ItemIds.SPEED_CHARM, table.forEnemy("MEDUSA").roll(random).get(0).itemId());
    assertEquals(ItemIds.FREEZE_BOMB, table.forEnemy("MEDUSA").roll(random).get(0).itemId());

    RandomGenerator bombRoll = mock(RandomGenerator.class);
    when(bombRoll.nextInt(100)).thenReturn(99);
    for (String enemyType : normalEnemies) {
      assertEquals(
          List.of(new ItemDropSpec(ItemIds.FREEZE_BOMB, 1)),
          table.forEnemy(enemyType).roll(bombRoll));
    }
    for (String bossType : Set.of("CERBERUS", "FINAL_BOSS", "SNAKE_MINI_BOSS")) {
      assertEquals(table, table.forEnemy(bossType));
    }
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
