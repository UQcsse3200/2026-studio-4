package com.csse3200.game.items;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Selects enemy rewards without creating entities or owning their room lifecycle. */
public final class EnemyDropPolicy {
  public static final int DEFAULT_GOLD = 5;
  public static final double DEFAULT_CONSUMABLE_CHANCE = 0.35;
  private static final List<ItemType> CONSUMABLES =
      List.of(
          ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION);
  private final int gold;
  private final double consumableChance;
  private final RandomGenerator random;

  /** Production defaults: five Gold and a 35% chance of one uniformly selected consumable. */
  public EnemyDropPolicy() {
    this(DEFAULT_GOLD, DEFAULT_CONSUMABLE_CHANCE, RandomGenerator.getDefault());
  }

  /** Configurable policy with an injectable random source for deterministic tests. */
  public EnemyDropPolicy(int gold, double consumableChance, RandomGenerator random) {
    if (gold <= 0
        || !Double.isFinite(consumableChance)
        || consumableChance < 0
        || consumableChance > 1) {
      throw new IllegalArgumentException("Gold must be positive and probability between 0 and 1");
    }
    this.gold = gold;
    this.consumableChance = consumableChance;
    this.random = Objects.requireNonNull(random);
  }

  /** Returns exactly one Gold specification and at most one consumable specification. */
  public List<ItemDropSpec> selectDrops() {
    ItemDropSpec currency = new ItemDropSpec(ItemType.GOLD_COIN, gold);
    if (consumableChance == 0
        || (consumableChance < 1 && random.nextDouble() >= consumableChance)) {
      return List.of(currency);
    }
    return List.of(
        currency, ItemDropSpec.single(CONSUMABLES.get(random.nextInt(CONSUMABLES.size()))));
  }
}
