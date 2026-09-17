package com.csse3200.game.items;

import java.util.Objects;

/**
 * A caller-selected item drop and its quantity.
 *
 * <p>Enemy and Room features own the decision about which specifications to create. Item code only
 * validates and represents that decision. Multiple specifications may contain the same item type,
 * allowing multiple consumables to be dropped at once. This implementation was developed with
 * assistance from OpenAI Codex and reviewed by Yuezhou Wang.
 *
 * @param itemType item selected by the caller
 * @param quantity positive quantity represented by the world entity
 */
public record ItemDropSpec(ItemType itemType, int quantity) {
  public ItemDropSpec {
    Objects.requireNonNull(itemType, "itemType cannot be null");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }

  /** Creates a single-unit drop. */
  public static ItemDropSpec single(ItemType itemType) {
    return new ItemDropSpec(itemType, 1);
  }
}
