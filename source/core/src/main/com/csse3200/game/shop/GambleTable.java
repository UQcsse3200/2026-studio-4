package com.csse3200.game.shop;

import java.util.List;
import java.util.Random;

/**
 * Weighted random selection over a fixed set of GambleEntry outcomes, mirroring the existing
 * LootTable pattern. The Random is injected so rolls are deterministic and testable.
 */
public final class GambleTable {

  private final List<GambleEntry> entries;
  private final int totalWeight;

  public GambleTable(List<GambleEntry> entries) {
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("GambleTable needs at least one entry");
    }
    this.entries = List.copyOf(entries);
    int total = 0;
    for (GambleEntry entry : entries) {
      if (total > Integer.MAX_VALUE - entry.weight()) {
        throw new IllegalArgumentException("Total gambling weight exceeds supported range");
      }
      total += entry.weight();
    }
    this.totalWeight = total;
  }

  public int totalWeight() {
    return totalWeight;
  }

  public double probabilityOf(GambleEntry entry) {
    return entry.weight() / (double) totalWeight;
  }

  /** Rolls a single weighted outcome using the supplied RNG. */
  public GambleEntry roll(Random rng) {
    int roll = rng.nextInt(totalWeight);
    int cumulative = 0;
    for (GambleEntry entry : entries) {
      cumulative += entry.weight();
      if (roll < cumulative) {
        return entry;
      }
    }
    return entries.get(entries.size() - 1);
  }
}
