package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.csse3200.game.components.statuseffects.Stat;
import org.junit.jupiter.api.Test;

class CerberusMistSlowEffectTest {

  @Test
  void shouldHalveOnlyMovementSpeed() {
    CerberusMistSlowEffect effect = new CerberusMistSlowEffect();

    assertEquals(0.5f, effect.getStatMultiplier(Stat.MOVEMENT_SPEED));
    assertEquals(1f, effect.getStatMultiplier(Stat.ATTACK));
    assertEquals(1f, effect.getStatMultiplier(Stat.ATTACK_SPEED));
  }

  @Test
  void shouldRemainActiveUntilRemovedByMistExit() {
    CerberusMistSlowEffect effect = new CerberusMistSlowEffect();

    assertFalse(effect.update());
    assertEquals(Long.MAX_VALUE, effect.getRemainingDuration());
  }
}
