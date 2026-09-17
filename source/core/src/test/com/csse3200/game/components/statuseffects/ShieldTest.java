package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShieldTest {
  @Test
  void absorbShouldReduceDamageUntilPointsRunOut() {
    Shield shield = StatusEffectsFactory.createShield();
    assertEquals(20, shield.getMax());
    assertEquals(20, shield.getCurrent());
    assertTrue(shield.activateAbsorb());
    assertTrue(shield.isActive());
    assertEquals(0, shield.modifyIncomingDamage(5));
    assertEquals(15, shield.getCurrent());
    assertEquals(5, shield.modifyIncomingDamage(20));
    assertEquals(0, shield.getCurrent());
    assertFalse(shield.isActive());
    assertEquals(8, shield.modifyIncomingDamage(8));
  }

  @Test
  void timedModeShouldBlockAllDamage() {
    Shield shield = new Shield();
    assertTrue(shield.activateTimed());
    assertEquals(0, shield.getCurrent());
    assertEquals(0, shield.modifyIncomingDamage(99));
    assertFalse(shield.activateAbsorb());
    assertFalse(shield.activateTimed());
  }

  @Test
  void shouldRejectActivationWhenNotFull() {
    Shield shield = new Shield();
    shield.activateAbsorb();
    shield.modifyIncomingDamage(1);
    assertFalse(shield.activateAbsorb());
    assertFalse(shield.activateTimed());
  }

  @Test
  void inactiveShieldShouldPassDamageThrough() {
    Shield shield = new Shield();
    assertEquals(10, shield.modifyIncomingDamage(10));
    assertEquals(-3, shield.modifyIncomingDamage(-3));
    assertFalse(shield.update());
    assertEquals(0, shield.getRemainingDuration());
  }

  @Test
  void absorbDurationShouldAppearOnRemainingDuration() {
    Shield shield = new Shield();
    shield.activateAbsorb();
    assertTrue(shield.getRemainingDuration() > 0);
    assertTrue(shield.getRemainingDuration() <= 5_000);
  }
}
