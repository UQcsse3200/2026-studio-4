package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link FlashEffect}: a timed blink, optionally one that shows on a dark sprite. */
@ExtendWith(GameExtension.class)
class FlashEffectTest {
  private static final Color TINT = new Color(0.8f, 0.4f, 1f, 1f);
  private static final Color GLOW = new Color(0.6f, 0.25f, 0.95f, 0.65f);

  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
  }

  @Test
  void blinksOnAndOffEveryQuarterSecond() {
    FlashEffect flash = new FlashEffect(time, 1000L, TINT);

    assertSame(TINT, flash.getTint(), "starts visible");
    when(time.getTime()).thenReturn(249L);
    assertSame(TINT, flash.getTint());
    when(time.getTime()).thenReturn(250L);
    assertNull(flash.getTint(), "first half period is over");
    when(time.getTime()).thenReturn(500L);
    assertSame(TINT, flash.getTint(), "and back on");
    when(time.getTime()).thenReturn(750L);
    assertNull(flash.getTint());
  }

  @Test
  void stopsTintingOnceItHasRunOut() {
    FlashEffect flash = new FlashEffect(time, 1000L, TINT);

    when(time.getTime()).thenReturn(1000L);

    assertNull(flash.getTint());
    assertNull(flash.getGlow());
  }

  @Test
  void aShortFlashShowsExactlyOnce() {
    // 400ms is what the lightning spell uses: on for 250, off for the rest, one clean blink.
    FlashEffect flash = new FlashEffect(time, 400L, TINT, GLOW);

    assertSame(TINT, flash.getTint());
    when(time.getTime()).thenReturn(250L);
    assertNull(flash.getTint());
    when(time.getTime()).thenReturn(399L);
    assertNull(flash.getTint());
  }

  @Test
  void showsTintAndGlowTogetherSoTheBlinkReadsTheSameOnAnySprite() {
    FlashEffect flash = new FlashEffect(time, 1000L, TINT, GLOW);

    assertSame(TINT, flash.getTint());
    assertSame(GLOW, flash.getGlow());

    when(time.getTime()).thenReturn(250L);
    assertNull(flash.getTint());
    assertNull(flash.getGlow(), "the glow must go dark with the tint, not linger");

    when(time.getTime()).thenReturn(500L);
    assertSame(TINT, flash.getTint());
    assertSame(GLOW, flash.getGlow());
  }

  @Test
  void glowsNothingWhenBuiltWithoutOne() {
    // The tint-only constructor is what the existing red damage blink uses; adding a glow there
    // would change how every hit in the game looks.
    FlashEffect flash = new FlashEffect(time, 1000L, TINT);

    assertSame(TINT, flash.getTint());
    assertNull(flash.getGlow());
  }

  @Test
  void leavesGameplayStatsAlone() {
    FlashEffect flash = new FlashEffect(time, 1000L, TINT, GLOW);

    for (Stat stat : Stat.values()) {
      assertEquals(1f, flash.getStatMultiplier(stat));
    }
  }
}
