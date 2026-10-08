package com.csse3200.game.items;

import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Room-owned drop rules. JSON can supply the public fields through FileLoader. */
public class LootTable {
  public int rolls = 1;
  public int noDropWeight;
  public Entry[] entries = new Entry[0];
  public EnemyRule[] enemyRules = new EnemyRule[0];

  /** Optional rules for one enemy type; other types use the enclosing table. */
  public static class EnemyRule {
    public String enemyType;
    public LootTable table;

    public EnemyRule() {}

    public EnemyRule(String enemyType, LootTable table) {
      this.enemyType = enemyType;
      this.table = table;
    }
  }

  public static class Entry {
    public String itemId;
    public int weight = 1;
    public int minQuantity = 1;
    public int maxQuantity = 1;

    public Entry() {}

    public Entry(String itemId, int weight, int minQuantity, int maxQuantity) {
      this.itemId = itemId;
      this.weight = weight;
      this.minQuantity = minQuantity;
      this.maxQuantity = maxQuantity;
    }
  }

  /** Loads the same JSON default used by rooms; drop weights have one source of truth. */
  public static LootTable defaultTable() {
    LootTable table = FileLoader.readClass(LootTable.class, "configs/default-item-drops.json");
    if (table == null) {
      throw new IllegalStateException("Unable to load default item drops");
    }
    return table;
  }

  /** Validates a table once when it is loaded, before any entity is created. */
  public void validate() {
    if (rolls < 0 || noDropWeight < 0 || entries == null || enemyRules == null) {
      throw new IllegalArgumentException("Invalid loot table rolls, no-drop weight or entries");
    }
    long total = noDropWeight;
    for (Entry entry : entries) {
      if (entry == null
          || entry.itemId == null
          || !ItemCatalog.contains(entry.itemId)
          || entry.weight <= 0
          || entry.minQuantity <= 0
          || entry.maxQuantity < entry.minQuantity) {
        throw new IllegalArgumentException("Invalid loot table entry");
      }
      total += entry.weight;
    }
    if (rolls > 0 && (total == 0 || total > Integer.MAX_VALUE)) {
      throw new IllegalArgumentException(
          "Loot table weight must be between 1 and Integer.MAX_VALUE");
    }
    Set<String> enemyTypes = new HashSet<>();
    for (EnemyRule rule : enemyRules) {
      if (rule == null
          || rule.enemyType == null
          || rule.enemyType.isBlank()
          || rule.table == null
          || !enemyTypes.add(rule.enemyType)) {
        throw new IllegalArgumentException("Invalid or duplicate enemy loot rule");
      }
      rule.table.validate();
    }
  }

  /** Uses an enemy-specific table when configured, otherwise the room's general table. */
  public LootTable forEnemy(String enemyType) {
    if (enemyType != null) {
      for (EnemyRule rule : enemyRules) {
        if (rule.enemyType.equals(enemyType)) {
          return rule.table;
        }
      }
    }
    return this;
  }

  /** Each roll yields zero or one specification, so a table may yield zero or many drops. */
  public List<ItemDropSpec> roll(RandomGenerator random) {
    Objects.requireNonNull(random, "random cannot be null");
    validate();
    int total = noDropWeight;
    for (Entry entry : entries) {
      total += entry.weight;
    }
    List<ItemDropSpec> results = new ArrayList<>();
    for (int roll = 0; roll < rolls; roll++) {
      int choice = random.nextInt(total);
      if (choice < noDropWeight) {
        continue;
      }
      choice -= noDropWeight;
      for (Entry entry : entries) {
        if (choice < entry.weight) {
          int quantity = entry.minQuantity;
          if (entry.maxQuantity > quantity) {
            quantity += random.nextInt(entry.maxQuantity - quantity + 1);
          }
          results.add(new ItemDropSpec(entry.itemId, quantity));
          break;
        }
        choice -= entry.weight;
      }
    }
    return results;
  }
}
