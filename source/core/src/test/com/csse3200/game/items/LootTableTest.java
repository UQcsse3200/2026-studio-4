package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.Json;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootTableTest {
  @Test
  void shouldLoadDefaultJsonAndResolveAllItemIds() {
    LootTable table = LootTable.defaultTable();
    table.validate();
    assertEquals(ItemType.values().length, table.entries.length);
  }

  @Test
  void shouldSupportNoDropAndRestrictedWeightedResults() {
    LootTable table = new LootTable();
    table.rolls = 2;
    table.noDropWeight = 1;
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemType.GOLD_COIN, 3, 4, 4)};
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextInt(4)).thenReturn(0, 3);

    List<ItemDropSpec> results = table.roll(random);

    assertEquals(List.of(new ItemDropSpec(ItemType.GOLD_COIN, 4)), results);
  }

  @Test
  void shouldRejectUnknownOrInvalidRulesBeforeSpawning() {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(null, 1, 1, 1)};
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
        List.of(new ItemDropSpec(ItemType.GOLD_COIN, 3)), table.forEnemy("GOLEM").roll(random));
    assertEquals(
        List.of(new ItemDropSpec(ItemType.HEALTH_POTION, 1)),
        table.forEnemy("MEDUSA").roll(random));
  }

  @Test
  void shouldRejectDuplicateEnemyRules() {
    LootTable table = new LootTable();
    table.entries = new LootTable.Entry[] {new LootTable.Entry(ItemType.HEALTH_POTION, 1, 1, 1)};
    table.enemyRules =
        new LootTable.EnemyRule[] {
          new LootTable.EnemyRule("GOLEM", new LootTable()),
          new LootTable.EnemyRule("GOLEM", new LootTable())
        };

    assertThrows(IllegalArgumentException.class, table::validate);
  }
}
