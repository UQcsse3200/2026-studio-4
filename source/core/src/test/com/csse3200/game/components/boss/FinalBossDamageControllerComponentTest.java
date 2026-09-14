package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossDamageControllerComponentTest {
  @Test
  void shouldStartShieldedAndBlockDamage() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();

    Entity boss = new Entity().addComponent(stats).addComponent(controller);

    int[] shieldHits = {0};
    boss.getEvents().addListener(FinalBossEvents.SHIELD_HIT, () -> shieldHits[0]++);

    boss.create();
    stats.takeDamage(30);

    assertTrue(controller.isShielded());
    assertTrue(stats.isInvulnerable());
    assertEquals(100, stats.getHealth());
    assertEquals(1, shieldHits[0]);
  }

  @Test
  void shouldOpenCappedVulnerabilityWindow() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();

    Entity boss = new Entity().addComponent(stats).addComponent(controller);
    boss.create();

    controller.openVulnerabilityWindow(0.25f, 90);
    stats.takeDamage(100);
    stats.takeDamage(100);

    assertFalse(controller.isShielded());
    assertFalse(stats.isInvulnerable());
    assertEquals(0.25f, stats.getIncomingDamageMultiplier());
    assertEquals(90, stats.getMinimumHealth());
    assertEquals(90, stats.getHealth());
  }

  @Test
  void shouldRemoveStageOneProtection() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();

    Entity boss = new Entity().addComponent(stats).addComponent(controller);
    boss.create();

    controller.openVulnerabilityWindow(0.25f, 90);
    controller.disableStageOneProtection();
    stats.takeDamage(20);

    assertFalse(controller.isShielded());
    assertFalse(stats.isInvulnerable());
    assertEquals(1f, stats.getIncomingDamageMultiplier());
    assertEquals(0, stats.getMinimumHealth());
    assertEquals(80, stats.getHealth());
  }
}
