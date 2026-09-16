package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class EnemyDropPolicyTest {
  @Test
  void alwaysGivesConfiguredGoldWithNoConsumableAtZeroChance() {
    RandomGenerator random = mock(RandomGenerator.class);
    assertEquals(
        List.of(new ItemDropSpec(ItemType.GOLD_COIN, 13)),
        new EnemyDropPolicy(13, 0, random).selectDrops());
    verifyNoInteractions(random);
  }

  @Test
  void deterministicallySelectsAllFourConsumables() {
    RandomGenerator random = mock(RandomGenerator.class);
    ItemType[] types = {
      ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION
    };
    for (int i = 0; i < types.length; i++) {
      when(random.nextInt(4)).thenReturn(i);
      assertEquals(
          List.of(new ItemDropSpec(ItemType.GOLD_COIN, 5), ItemDropSpec.single(types[i])),
          new EnemyDropPolicy(5, 1, random).selectDrops());
    }
    verify(random, never()).nextDouble();
  }

  @Test
  void probabilityBoundaryIsExclusiveAndGoldIsUnconditional() {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble()).thenReturn(0.3499, 0.35, 0.99);
    EnemyDropPolicy policy = new EnemyDropPolicy(5, 0.35, random);
    assertEquals(2, policy.selectDrops().size());
    assertEquals(List.of(new ItemDropSpec(ItemType.GOLD_COIN, 5)), policy.selectDrops());
    assertEquals(1, policy.selectDrops().size());
  }

  @Test
  void seededPolicyIsReproducible() {
    EnemyDropPolicy first = new EnemyDropPolicy(5, 0.35, new Random(15));
    EnemyDropPolicy second = new EnemyDropPolicy(5, 0.35, new Random(15));
    for (int i = 0; i < 100; i++) {
      assertEquals(first.selectDrops(), second.selectDrops());
    }
    assertEquals(ItemType.GOLD_COIN, new EnemyDropPolicy().selectDrops().getFirst().itemType());
  }

  @Test
  void rejectsInvalidConfiguration() {
    for (double chance : new double[] {-1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertThrows(
          IllegalArgumentException.class, () -> new EnemyDropPolicy(5, chance, new Random()));
    }
    assertThrows(IllegalArgumentException.class, () -> new EnemyDropPolicy(0, 0, new Random()));
    assertThrows(NullPointerException.class, () -> new EnemyDropPolicy(5, 0, null));
  }
}
