package com.csse3200.game.shop;

/**
 * A single weighted outcome on the gamble wheel. A bust entry (itemId == null) means the spin wins
 * nothing — it still takes up a slice of the wheel so a bad outcome is possible.
 */
public record GambleEntry(String itemId, int quantity, int weight) {

  public GambleEntry {
    if (weight <= 0) {
      throw new IllegalArgumentException("weight must be positive: " + weight);
    }
    if (itemId != null && quantity < 1) {
      throw new IllegalArgumentException("quantity must be at least 1: " + quantity);
    }
  }

  /** A winning entry: {@code quantity} of {@code itemId}, with the given weight. */
  public static GambleEntry of(String itemId, int quantity, int weight) {
    return new GambleEntry(itemId, quantity, weight);
  }

  /** A losing entry: the spin costs gold and grants nothing. */
  public static GambleEntry bust(int weight) {
    return new GambleEntry(null, 0, weight);
  }

  public boolean isBust() {
    return itemId == null;
  }
}
