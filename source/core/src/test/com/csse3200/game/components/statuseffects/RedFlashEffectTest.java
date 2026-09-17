package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@link RedFlashEffect} now only supplies red to the shared blink. These pin the behaviour the
 * damage flash and petrification already relied on, so that refactor cannot quietly change them.
 */
@ExtendWith(GameExtension.class)
class RedFlashEffectTest {
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
  }

  @Test
  void blinksRedWithoutTouchingTransparency() {
    Color tint = new RedFlashEffect(time, 600L).getTint();

    assertNotNull(tint);
    assertEquals(1f, tint.r);
    assertEquals(0.2f, tint.g);
    assertEquals(0.2f, tint.b);
    assertEquals(1f, tint.a, "a red blink must not fade the sprite out");
  }

  @Test
  void doesNotGlowSoTheLookOfEveryExistingHitIsUnchanged() {
    assertNull(new RedFlashEffect(time, 600L).getGlow());
  }

  @Test
  void stillBlinksOnTheSharedQuarterSecondPhase() {
    RedFlashEffect flash = new RedFlashEffect(time, 600L);

    assertNotNull(flash.getTint());
    when(time.getTime()).thenReturn(250L);
    assertNull(flash.getTint());
    when(time.getTime()).thenReturn(500L);
    assertNotNull(flash.getTint());
  }

  @Test
  void petrificationStillSlowsMovementWhileBlinkingRed() {
    PetrificationEffect petrified = new PetrificationEffect(time, 2000L, 0.5f);

    assertEquals(0.5f, petrified.getStatMultiplier(Stat.MOVEMENT_SPEED));
    assertEquals(1f, petrified.getStatMultiplier(Stat.ATTACK));
    assertNotNull(petrified.getTint());
    assertNull(petrified.getGlow());
  }
}
