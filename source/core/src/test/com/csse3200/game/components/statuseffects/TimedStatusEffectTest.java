package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.services.GameTime;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TimedStatusEffectTest {
  @Test
  void shouldCountDownFromCreationOnTheInjectedClock() {
    GameTime time = mock(GameTime.class);
    Runnable ended = mock(Runnable.class);
    List<Function<GameTime, TimedStatusEffect>> makers =
        List.of(
            clock -> new TimedStatusEffect(clock, 15_000),
            clock -> StatusEffectsFactory.createInvisibility(clock, 15_000),
            clock -> StatusEffectsFactory.createLastStand(clock, 10_000));
    for (Function<GameTime, TimedStatusEffect> maker : makers) {
      when(time.getTime()).thenReturn(1_000L);
      TimedStatusEffect effect = maker.apply(time);
      effect.setOnEnded(ended);
      long duration = effect.getDuration();
      assertFalse(effect.isExpired());
      assertFalse(effect.update());
      assertEquals(duration, effect.getRemainingDuration());
      when(time.getTime()).thenReturn(1_000L + duration - 1);
      assertFalse(effect.update());
      assertEquals(1, effect.getRemainingDuration());
      when(time.getTime()).thenReturn(1_000L + duration);
      assertTrue(effect.isExpired());
      assertTrue(effect.update());
      assertEquals(0, effect.getRemainingDuration());
      when(time.getTime()).thenReturn(1_000L + duration + 5_000);
      assertEquals(0, effect.getRemainingDuration());
    }
    // Expiring an effect never runs its callback; only the controller does that on removal.
    verifyNoInteractions(ended);
  }

  @Test
  void shouldRunTheEndCallbackOnlyWhenRemovedAndOnlyWhenSet() {
    TimedStatusEffect effect = new TimedStatusEffect(mock(GameTime.class), 1_000);
    assertDoesNotThrow(effect::onRemoved);
    Runnable ended = mock(Runnable.class);
    effect.setOnEnded(ended);
    effect.onRemoved();
    verify(ended).run();
  }

  @Test
  void shouldDescribeWhatEachEffectDoesWithoutTouchingRawStats() {
    GameTime time = mock(GameTime.class);
    TimedStatusEffect plain = new TimedStatusEffect(time, 1_000);
    assertFalse(plain.concealsOwner());
    assertNull(plain.getTint());
    for (Stat stat : Stat.values()) {
      assertEquals(1f, plain.getStatMultiplier(stat));
    }

    TimedStatusEffect hidden = StatusEffectsFactory.createInvisibility(time, 1_000);
    assertTrue(hidden.concealsOwner());
    assertEquals(0.35f, hidden.getTint().a);
    for (Stat stat : Stat.values()) {
      assertEquals(1f, hidden.getStatMultiplier(stat));
    }

    TimedStatusEffect amplified = StatusEffectsFactory.createLastStand(time, 1_000);
    assertFalse(amplified.concealsOwner());
    assertEquals(1f, amplified.getTint().r);
    for (Stat stat : Stat.values()) {
      assertEquals(LastStandEffect.MULTIPLIER, amplified.getStatMultiplier(stat));
    }
  }
}
