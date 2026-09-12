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
        List.of(
            new TimedStatusEffect(time, 15_000),
            new InvisibilityEffect(time, 15_000),
            new LastStandEffect(time, 10_000))) {
      effect.setOnEnded(ended);
      when(time.getTime()).thenReturn(0L);
      assertFalse(effect.isActive());
      assertFalse(effect.update());
      assertEquals(0, effect.getRemainingDuration());
      effect.activate();
      long duration = effect.getDuration();
      assertTrue(effect.isActive());
      assertEquals(duration, effect.getRemainingDuration());
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
    // Expiring an effect never runs its callback; only the controller does that.
    verifyNoInteractions(ended);
  }

  @Test
  void shouldRunTheEndCallbackOnlyWhenAskedAndOnlyWhenSet() {
    TimedStatusEffect effect = new TimedStatusEffect(mock(GameTime.class), 1_000);
    assertDoesNotThrow(effect::notifyEnded);
    Runnable ended = mock(Runnable.class);
    effect.setOnEnded(ended);
    effect.notifyEnded();
    verify(ended).run();
  }
}
