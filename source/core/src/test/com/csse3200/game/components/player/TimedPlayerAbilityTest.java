package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.services.GameTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimedPlayerAbilityTest {
  @Test
  void shouldUseInjectedClockAndSeparateExpirationFromLifecycleCallbacks() {
    GameTime time = mock(GameTime.class);
    for (TimedPlayerAbility ability : List.of(new Invisibility(time), new LastStand(time))) {
      when(time.getTime()).thenReturn(0L);
      assertFalse(ability.isRunning());
      assertFalse(ability.update());
      assertEquals(0, ability.getRemainingMs());
      ability.start();
      long duration = ability.getDuration();
      assertTrue(ability.isRunning());
      assertEquals(duration, ability.getRemainingMs());
      when(time.getTime()).thenReturn(duration - 1);
      assertFalse(ability.update());
      assertEquals(1, ability.getRemainingMs());
      when(time.getTime()).thenReturn(duration);
      assertTrue(ability.update());
      assertEquals(0, ability.getRemainingMs());
      ability.clear();
      assertFalse(ability.isRunning());
      assertFalse(ability.update());
      assertEquals(0, ability.getRemainingMs());
      ability.start();
      assertEquals(duration, ability.getRemainingMs());
    }
  }

  @Test
  void shouldIgnoreLifecycleCallsMadeBeforeItIsAttachedToAPlayer() {
    Invisibility unattached = new Invisibility(mock(GameTime.class));
    assertNull(unattached.getOwner());
    // No controller yet, so neither of these has anywhere to go.
    assertDoesNotThrow(unattached::stop);
    assertDoesNotThrow(unattached::notifyEnded);
  }
}
