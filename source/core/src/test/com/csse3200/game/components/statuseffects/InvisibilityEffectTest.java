package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.Test;

class InvisibilityEffectTest {
  @Test
  void shouldConcealWithoutChangingStats() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    InvisibilityEffect hidden = new InvisibilityEffect(time, 15_000);

    assertTrue(hidden.concealsOwner());
    assertEquals(0.35f, hidden.getTint().a, 1e-4f);
    for (Stat stat : Stat.values()) {
      assertEquals(1f, hidden.getStatMultiplier(stat));
    }
    assertFalse(hidden.update());
    assertEquals(15_000, hidden.getRemainingDuration());
  }
}
