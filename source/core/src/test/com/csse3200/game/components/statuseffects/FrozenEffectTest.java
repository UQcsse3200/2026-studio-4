package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link FrozenEffect}: what being frozen means, stated by the effect itself. */
@ExtendWith(GameExtension.class)
class FrozenEffectTest {
  private GameTime time;
  private FrozenEffect frozen;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    frozen = new FrozenEffect(time, 5000L);
  }

  @Test
  void stopsTheOwnerMovingAndActing() {
    assertTrue(frozen.immobilisesOwner());
  }

  @Test
  void zeroesEveryStatSoAFrozenBodyCannotHurtAnythingOnContactEither() {
    for (Stat stat : Stat.values()) {
      assertEquals(0f, frozen.getStatMultiplier(stat), "expected " + stat + " to be zeroed");
    }
  }

  @Test
  void tintsTheSpriteLightBlue() {
    Color tint = frozen.getTint();

    assertNotNull(tint);
    assertTrue(tint.b > tint.g, "blue should dominate green");
    assertTrue(tint.g > tint.r, "and green should dominate red, so the sprite reads as ice");
  }

  @Test
  void alsoGlowsSoTheFreezeShowsOnANearBlackEnemy() {
    // Multiplying a near-black sprite by any tint leaves it near black, which is how a frozen
    // Cerberus ended up looking untouched. The glow is what actually shows on a dark sprite.
    Color glow = frozen.getGlow();

    assertNotNull(glow, "a tint alone is invisible on a dark sprite");
    assertTrue(glow.b > glow.r, "the glow should read as the same ice colour");
    assertTrue(glow.a > 0f, "alpha carries the strength of an additive glow");
  }

  @Test
  void holdsTheFreezeForItsFullDurationAndThenReportsItself() {
    assertFalse(frozen.isExpired());
    assertEquals(5000L, frozen.getRemainingDuration());

    when(time.getTime()).thenReturn(4999L);
    assertFalse(frozen.isExpired());
    assertEquals(1L, frozen.getRemainingDuration());
    assertFalse(frozen.update(), "still frozen, so not ready for removal");

    when(time.getTime()).thenReturn(5000L);
    assertTrue(frozen.isExpired());
    assertEquals(0L, frozen.getRemainingDuration());
    assertTrue(frozen.update(), "expired, so the controller should drop it");
  }

  @Test
  void aZeroLengthFreezeIsAlreadyOver() {
    assertTrue(new FrozenEffect(time, 0L).isExpired());
  }
}
