package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.Test;

class WitchCurseEffectTest {
  @Test
  void shouldSlowMovementAndBlockDashForThreeSeconds() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(20L);
    WitchCurseEffect effect = new WitchCurseEffect(time);

    when(time.getTimeSince(20L)).thenReturn(2999L);
    assertEquals(0.45f, effect.getStatMultiplier(Stat.MOVEMENT_SPEED));
    assertEquals(1f, effect.getStatMultiplier(Stat.ATTACK));
    assertTrue(effect.disablesDash());
    assertFalse(effect.update());

    when(time.getTimeSince(20L)).thenReturn(3000L);
    assertTrue(effect.update());
  }
}
