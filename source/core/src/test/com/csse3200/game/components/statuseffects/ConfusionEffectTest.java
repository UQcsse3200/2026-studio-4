package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.Test;

class ConfusionEffectTest {
  @Test
  void shouldConfuseControlsForThreeSeconds() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(100L);
    ConfusionEffect effect = new ConfusionEffect(time);

    when(time.getTimeSince(100L)).thenReturn(2999L);
    assertTrue(effect.confusesControls());
    assertFalse(effect.update());

    when(time.getTimeSince(100L)).thenReturn(3000L);
    assertTrue(effect.update());
  }
}
