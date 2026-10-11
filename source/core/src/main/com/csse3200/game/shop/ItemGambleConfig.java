package com.csse3200.game.shop;

import com.csse3200.game.files.FileLoader;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemCatalog;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One validated configuration shared by displayed odds and inventory settlement. */
public record ItemGambleConfig(int cost, List<GambleEntry> entries) {
  public ItemGambleConfig {
    if (cost <= 0) throw new IllegalArgumentException("Draw cost must be positive");
    entries = List.copyOf(entries);
    new GambleTable(entries);
    Set<String> ids = new HashSet<>();
    for (GambleEntry entry : entries) {
      if (!ids.add(entry.itemId())) throw new IllegalArgumentException("Duplicate draw outcome");
      if (entry.isBust()) {
        if (entry.quantity() != 0) throw new IllegalArgumentException("Bust grants no items");
      } else if (!ItemCatalog.contains(entry.itemId())
          || !(ItemCatalog.create(entry.itemId(), 1) instanceof ConsumableItem)) {
        throw new IllegalArgumentException("Unknown consumable reward: " + entry.itemId());
      }
    }
  }

  public GambleTable table() {
    return new GambleTable(entries);
  }

  public static ItemGambleConfig load(String path) {
    Config config = FileLoader.readClass(Config.class, path);
    if (config == null || config.entries == null) {
      throw new IllegalArgumentException("Invalid item draw configuration: " + path);
    }
    return new ItemGambleConfig(
        config.cost,
        java.util.Arrays.stream(config.entries)
            .map(entry -> new GambleEntry(entry.itemId, entry.quantity, entry.weight))
            .toList());
  }

  public static class EntryConfig {
    public String itemId;
    public int quantity;
    public int weight;
  }

  /** JSON transport; runtime data is immutable and validated. */
  public static class Config {
    public int cost;
    public EntryConfig[] entries;
  }
}
