package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LootDropPolicyTest {
  @Test
  void shouldAlwaysDropGoldWithoutConsumableWhenRollMisses() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.75);

    LootDropPolicy policy = new LootDropPolicy(0.25, random);

    assertEquals(List.of(ItemType.GOLD_COIN), policy.rollDrops());
  }

  @Test
  void shouldAddOneRandomConsumableWhenRollSucceeds() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.10);
    when(random.nextInt(4)).thenReturn(2);

    LootDropPolicy policy = new LootDropPolicy(0.25, random);

    assertEquals(List.of(ItemType.GOLD_COIN, ItemType.SPEED_POTION), policy.rollDrops());
    verify(random).nextInt(4);
  }

  @Test
  void shouldSupportBoundaryChances() {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.0);
    when(random.nextInt(4)).thenReturn(0);

    assertEquals(List.of(ItemType.GOLD_COIN), new LootDropPolicy(0.0, random).rollDrops());
    assertEquals(
        List.of(ItemType.GOLD_COIN, ItemType.HEALTH_POTION),
        new LootDropPolicy(1.0, random).rollDrops());
  }

  @Test
  void shouldRejectInvalidConfiguration() {
    Random random = mock(Random.class);

    assertThrows(IllegalArgumentException.class, () -> new LootDropPolicy(-0.01, random));
    assertThrows(IllegalArgumentException.class, () -> new LootDropPolicy(1.01, random));
    assertThrows(IllegalArgumentException.class, () -> new LootDropPolicy(Double.NaN, random));
    assertThrows(NullPointerException.class, () -> new LootDropPolicy(0.25, null));
  }
}
