package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.services.GameTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimedStatusEffectTest {
  @Test
  void shouldUseInjectedClockAndSeparateExpirationFromLifecycleCallbacks() {
    GameTime time = mock(GameTime.class);
    Runnable ended = mock(Runnable.class);
    for (TimedStatusEffect effect :
        List.of(new Invisibility(time, 15_000, ended), new LastStand(time, 10_000, ended))) {
      when(time.getTime()).thenReturn(0L);
      assertFalse(effect.isActive());
      assertFalse(effect.update());
      assertEquals(0, effect.getRemainingDuration());
      effect.activate();
      long duration = effect.getRemainingDuration();
      assertTrue(effect.isActive());
      when(time.getTime()).thenReturn(duration - 1);
      assertFalse(effect.update());
      assertEquals(1, effect.getRemainingDuration());
      when(time.getTime()).thenReturn(duration);
      assertTrue(effect.update());
      assertEquals(0, effect.getRemainingDuration());
      effect.clear();
      assertFalse(effect.isActive());
      assertFalse(effect.update());
      assertEquals(0, effect.getRemainingDuration());
      effect.activate();
      assertEquals(duration, effect.getRemainingDuration());
    }
    verifyNoInteractions(ended);
  }
}
