package com.csse3200.game.items;

import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Room-owned drop rules. JSON can supply the public fields through FileLoader. */
public class LootTable {
  public int rolls = 1;
  public int noDropWeight;
  public Entry[] entries = new Entry[0];

  public static class Entry {
    public ItemType itemId;
    public int weight = 1;
    public int minQuantity = 1;
    public int maxQuantity = 1;

    public Entry() {}

    public Entry(ItemType itemId, int weight, int minQuantity, int maxQuantity) {
      this.itemId = itemId;
      this.weight = weight;
      this.minQuantity = minQuantity;
      this.maxQuantity = maxQuantity;
    }
  }

  /** Loads and validates a table before the room uses it. */
  public static LootTable load(String path) {
    LootTable table = FileLoader.readClass(LootTable.class, Objects.requireNonNull(path));
    if (table == null) {
      throw new IllegalStateException("Unable to load loot table: " + path);
    }
    table.validate();
    return table;
  }

  /** Loads the same JSON default used by rooms; drop weights have one source of truth. */
  public static LootTable defaultTable() {
    return load("configs/default-item-drops.json");
  }

  /** Validates a table once when it is loaded, before any entity is created. */
  public void validate() {
    if (rolls < 0 || noDropWeight < 0 || entries == null) {
      throw new IllegalArgumentException("Invalid loot table rolls, no-drop weight or entries");
    }
    long total = noDropWeight;
    for (Entry entry : entries) {
      if (entry == null
          || entry.itemId == null
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
