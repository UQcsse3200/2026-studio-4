package com.csse3200.game.items;

import java.util.Objects;

/**
 * A caller-selected item drop and its quantity.
 *
 * <p>LootTable selects these specifications. This record validates and represents that decision.
 * Multiple specifications may contain the same item ID, allowing multiple consumables to be dropped
 * at once. This implementation was developed with assistance from OpenAI Codex and reviewed by
 * Yuezhou Wang.
 *
 * @param itemId stable item ID selected by the caller
 * @param quantity positive unit count; Charms become separate world entities
 */
public record ItemDropSpec(String itemId, int quantity) {
  public ItemDropSpec {
    Objects.requireNonNull(itemId, "itemId cannot be null");
    if (!ItemCatalog.contains(itemId)) {
      throw new IllegalArgumentException("Unknown item ID: " + itemId);
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }

  /** Creates a single-unit drop. */
  public static ItemDropSpec single(String itemId) {
    return new ItemDropSpec(itemId, 1);
  }
}
