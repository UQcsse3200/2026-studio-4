package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.Test;

class LastStandEffectTest {
  @Test
  void shouldAmplifyEveryStatAndTintRed() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    LastStandEffect lastStand = new LastStandEffect(time, 10_000);

    assertFalse(lastStand.concealsOwner());
    assertEquals(1f, lastStand.getTint().r, 1e-4f);
    assertEquals(0.35f, lastStand.getTint().g, 1e-4f);
    for (Stat stat : Stat.values()) {
      assertEquals(LastStandEffect.MULTIPLIER, lastStand.getStatMultiplier(stat));
    }
    assertFalse(lastStand.update());
    assertEquals(10_000, lastStand.getRemainingDuration());
  }
}
