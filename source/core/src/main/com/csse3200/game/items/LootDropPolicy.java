package com.csse3200.game.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Selects the item types produced for one defeated enemy.
 *
 * <p>Gold is guaranteed. At most one additional consumable is selected using the configured drop
 * chance. The caller remains responsible for creating and registering the corresponding entities.
 * This implementation was developed with assistance from OpenAI Codex and reviewed by Yuezhou Wang.
 */
public final class LootDropPolicy {
  private static final List<ItemType> CONSUMABLE_TYPES =
      List.of(
          ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION);

  private final double consumableDropChance;
  private final Random random;

  /**
   * Creates a drop policy without imposing a game-balance value on callers.
   *
   * @param consumableDropChance probability from 0.0 to 1.0 of an additional consumable
   * @param random random source, injectable for deterministic tests
   */
  public LootDropPolicy(double consumableDropChance, Random random) {
    if (Double.isNaN(consumableDropChance)
        || consumableDropChance < 0.0
        || consumableDropChance > 1.0) {
      throw new IllegalArgumentException("consumableDropChance must be between 0.0 and 1.0");
    }
    this.consumableDropChance = consumableDropChance;
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  /** Returns one guaranteed gold drop and, when selected, one random consumable. */
  public List<ItemType> rollDrops() {
    List<ItemType> drops = new ArrayList<>();
    drops.add(ItemType.GOLD_COIN);
    if (random.nextDouble() < consumableDropChance) {
      drops.add(CONSUMABLE_TYPES.get(random.nextInt(CONSUMABLE_TYPES.size())));
    }
    return List.copyOf(drops);
  }
}
