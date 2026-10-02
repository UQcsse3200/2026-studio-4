package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
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

  @Test
  void stageTwoBlocksUnattributedPlayerMeleeAndProjectileDamage() {
    IceFight fight = startIceFight();
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 20));
    Entity projectile = new Entity();
    int[] shieldHits = {0};
    fight.boss.getEvents().addListener(FinalBossEvents.SHIELD_HIT, () -> shieldHits[0]++);

    fight.stats.takeDamage(20);
    fight.stats.takeDamage(20, player);
    fight.stats.hit(player.getComponent(CombatStatsComponent.class));
    fight.stats.takeDamage(20, projectile);

    assertEquals(100, fight.stats.getHealth());
    assertEquals(4, shieldHits[0]);
    assertTrue(fight.stats.isInvulnerable());
    assertTrue(fight.controller.isShielded());
  }

  @Test
  void authorisedIceUsesNormalDamageEventsWithoutDroppingTheVisibleShield() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    player.setPosition(8f, 3f);
    player.setScale(2f, 3f);
    int[] damageEvents = {0};
    int[] reactions = {0};
    int[] deactivations = {0};
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              damageEvents[0]++;
              assertNotSame(player, source);
              assertEquals(player.getCenterPosition(), source.getCenterPosition());
              assertEquals(25, healthLost);
              assertEquals(75, remainingHealth);
            });
    fight.boss.getEvents().addListener("hitReaction", (Entity source) -> reactions[0]++);
    fight
        .boss
        .getEvents()
        .addListener(FinalBossEvents.SHIELD_DEACTIVATED, () -> deactivations[0]++);

    fight.controller.takeStageTwoIceDamage(25, player);

    assertEquals(75, fight.stats.getHealth());
    assertEquals(1, damageEvents[0]);
    assertEquals(1, reactions[0]);
    assertEquals(0, deactivations[0]);
    assertTrue(fight.stats.isInvulnerable());
    assertTrue(fight.controller.isShielded());
  }

  @Test
  void nestedDamageAndReusingTheIceSourceCannotShareAnAuthorisedImpact() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    int[] shieldHits = {0};
    int[] damageEvents = {0};
    fight.boss.getEvents().addListener(FinalBossEvents.SHIELD_HIT, () -> shieldHits[0]++);
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              if (++damageEvents[0] > 1) {
                return;
              }
              fight.stats.takeDamage(20, player);
              fight.stats.takeDamage(20);
              fight.stats.takeDamage(20, source);
              fight.controller.takeStageTwoIceDamage(20, player);
            });

    fight.controller.takeStageTwoIceDamage(25, player);

    assertEquals(75, fight.stats.getHealth());
    assertEquals(1, damageEvents[0]);
    assertEquals(3, shieldHits[0]);
    assertTrue(fight.stats.isInvulnerable());
  }

  @Test
  void healthFloorHandsOverToStageThreeWithoutUndoingItsShield() {
    IceFight fight = startIceFight();
    fight
        .boss
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> {
              if (health <= 60) {
                fight.phases.completeStage(FinalBossPhase.STAGE_TWO);
              }
            });

    fight.controller.takeStageTwoIceDamage(500, new Entity());
    fight.controller.takeStageTwoIceDamage(500, new Entity());
    fight.stats.takeDamage(500);

    assertEquals(60, fight.stats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, fight.phases.getCurrentPhase());
    assertTrue(fight.phases.isTransitioning());
    assertTrue(fight.controller.isShielded());
    assertTrue(fight.stats.isInvulnerable());
    assertEquals(0, fight.stats.getMinimumHealth());
  }

  @Test
  void aReplacementVulnerabilityWindowIsNotOverwrittenWhenTheIceImpactReturns() {
    IceFight fight = startIceFight();
    fight
        .boss
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> {
              if (health == 60) {
                fight.phases.completeStage(FinalBossPhase.STAGE_TWO);
                fight.controller.openVulnerabilityWindow(0.5f, 30);
              }
            });

    fight.controller.takeStageTwoIceDamage(500, new Entity());

    assertEquals(60, fight.stats.getHealth());
    assertFalse(fight.controller.isShielded());
    assertFalse(fight.stats.isInvulnerable());
    assertEquals(0.5f, fight.stats.getIncomingDamageMultiplier());
    assertEquals(30, fight.stats.getMinimumHealth());
  }

  @Test
  void stageOneTransformationAndDeathCannotReceiveStageTwoIceDamage() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();
    FinalBossPhaseControllerComponent phases = new FinalBossPhaseControllerComponent();
    new Entity().addComponent(stats).addComponent(controller).addComponent(phases).create();

    controller.enableStageTwoIceProtection(60);
    controller.takeStageTwoIceDamage(25, new Entity());
    assertEquals(100, stats.getHealth());
    assertEquals(0, stats.getMinimumHealth());

    phases.completeStage(FinalBossPhase.STAGE_ONE);
    controller.enableStageTwoIceProtection(60);
    controller.takeStageTwoIceDamage(25, new Entity());
    assertEquals(100, stats.getHealth());
    assertEquals(0, stats.getMinimumHealth());

    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    controller.enableStageTwoIceProtection(60);
    stats.setHealth(0);
    controller.takeStageTwoIceDamage(25, new Entity());
    assertEquals(0, stats.getHealth());
  }

  @Test
  void damageCallbackFailureRestoresStageTwoProtection() {
    IceFight fight = startIceFight();
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              throw new IllegalStateException("Broken damage observer");
            });

    assertThrows(
        IllegalStateException.class,
        () -> fight.controller.takeStageTwoIceDamage(10, new Entity()));
    fight.stats.takeDamage(20, new Entity());

    assertEquals(90, fight.stats.getHealth());
    assertTrue(fight.stats.isInvulnerable());
    assertTrue(fight.controller.isShielded());
  }

  @Test
  void existingShieldAndWindowMethodsReplaceStageTwoIceProtection() {
    IceFight fight = startIceFight();
    fight.controller.enableShield();
    fight.controller.takeStageTwoIceDamage(20, new Entity());
    assertEquals(100, fight.stats.getHealth());

    fight.controller.enableStageTwoIceProtection(60);
    fight.controller.openVulnerabilityWindow(0.5f, 60);
    fight.stats.takeDamage(20);
    assertEquals(90, fight.stats.getHealth());
    assertFalse(fight.controller.isShielded());

    fight.controller.enableStageTwoIceProtection(60);
    fight.controller.disableStageOneProtection();
    fight.stats.takeDamage(20);
    assertEquals(70, fight.stats.getHealth());
    assertFalse(fight.stats.isInvulnerable());
  }

  private static IceFight startIceFight() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();
    FinalBossPhaseControllerComponent phases = new FinalBossPhaseControllerComponent();
    Entity boss = new Entity().addComponent(stats).addComponent(controller).addComponent(phases);
    boss.create();
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    controller.enableStageTwoIceProtection(60);
    return new IceFight(boss, stats, controller, phases);
  }

  private record IceFight(
      Entity boss,
      CombatStatsComponent stats,
      FinalBossDamageControllerComponent controller,
      FinalBossPhaseControllerComponent phases) {}
}
