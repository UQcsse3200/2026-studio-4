package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponStatsComponentTest {
  @Test
  void shouldBeReadyToAttackInitially() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 1.5f);
    assertTrue(stats.canAttack());
    assertEquals(0.4f, stats.getCooldown());
    assertEquals(10, stats.getDamage());
    assertEquals(1.5f, stats.getKnockback());
    assertEquals(0f, stats.getRemainingCooldown());
  }

  @Test
  void shouldGateAttackAfterTriggerCooldown() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    assertFalse(stats.canAttack());
    assertEquals(0.4f, stats.getRemainingCooldown());
  }

  @Test
  void shouldAllowAttackAfterCooldownElapses() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    stats.update(0.4f);
    assertTrue(stats.canAttack());
    assertEquals(0f, stats.getRemainingCooldown());
  }

  @Test
  void shouldRemainGatedOnPartialTick() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    stats.update(0.2f);
    assertFalse(stats.canAttack());
    assertEquals(0.2f, stats.getRemainingCooldown(), 1e-4f);
  }

  @Test
  void shouldClampRemainingWhenUpdateOvershoots() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    stats.update(1f);
    assertTrue(stats.canAttack());
    assertEquals(0f, stats.getRemainingCooldown());
  }

  @Test
  void shouldAlwaysAllowAttackWhenCooldownIsZero() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0f, 5, 0f);
    assertTrue(stats.canAttack());
    stats.triggerCooldown();
    assertTrue(stats.canAttack());
  }

  @Test
  void shouldTreatNegativeDeltaAsZero() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    stats.update(-0.2f);
    assertEquals(0.4f, stats.getRemainingCooldown());
  }

  @Test
  void shouldRejectNegativeConstructorValues() {
    assertThrows(IllegalArgumentException.class, () -> new WeaponStatsComponent(-0.1f, 10, 0f));
    assertThrows(IllegalArgumentException.class, () -> new WeaponStatsComponent(0.1f, -1, 0f));
    assertThrows(IllegalArgumentException.class, () -> new WeaponStatsComponent(0.1f, 10, -1f));
  }

  @Test
  void shouldRejectNegativeSetters() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 1f);
    assertThrows(IllegalArgumentException.class, () -> stats.setCooldown(-1f));
    assertThrows(IllegalArgumentException.class, () -> stats.setDamage(-1));
    assertThrows(IllegalArgumentException.class, () -> stats.setKnockback(-1f));
  }

  @Test
  void shouldUpdateFromTimeSource() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.2f);
    ServiceLocator.registerTimeSource(gameTime);

    WeaponStatsComponent stats = new WeaponStatsComponent(0.4f, 10, 0f);
    stats.triggerCooldown();
    stats.update();
    assertEquals(0.2f, stats.getRemainingCooldown(), 1e-4f);
  }
}
