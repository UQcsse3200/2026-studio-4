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

    Entity attacker = new Entity();
    assertThrows(
        IllegalStateException.class, () -> fight.controller.takeStageTwoIceDamage(10, attacker));
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

  @Test
  void fourQuarterStrengthIceImpactsRemoveOneHealthPointThroughNormalDamageEvents() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    int[] damageEvents = {0};
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              damageEvents[0]++;
              assertEquals(1, healthLost);
              assertEquals(99, remainingHealth);
            });

    for (int impact = 0; impact < 3; impact++) {
      fight.controller.takeStageTwoIceDamage(0.25f, player);
      assertEquals(100, fight.stats.getHealth());
      assertTrue(fight.stats.isInvulnerable());
    }
    assertEquals(0, damageEvents[0]);
    fight.controller.takeStageTwoIceDamage(0.25f, player);

    assertEquals(99, fight.stats.getHealth());
    assertEquals(1, damageEvents[0]);
    assertTrue(fight.stats.isInvulnerable());
    assertTrue(fight.controller.isShielded());
  }

  @Test
  void partialDamageSurvivesLaterVolleysAndRepeatedEnablingOfTheSameProtection() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    fight.controller.takeStageTwoIceDamage(0.75f, player);

    // The damage accumulator lives on the encounter, not on an energy unit or a projectile.
    fight.controller.enableStageTwoIceProtection(60);
    fight.controller.takeStageTwoIceDamage(0.25f, player);

    assertEquals(99, fight.stats.getHealth());
    assertTrue(fight.stats.isInvulnerable());
  }

  @Test
  void blockedOrdinaryDamageNeitherAddsToNorClearsAccumulatedIceDamage() {
    IceFight fight = startIceFight();
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 20));
    fight.controller.takeStageTwoIceDamage(0.25f, player);
    fight.stats.takeDamage(1);
    fight.stats.takeDamage(1, player);
    fight.stats.hit(player.getComponent(CombatStatsComponent.class));
    fight.controller.takeStageTwoIceDamage(0.5f, player);
    assertEquals(100, fight.stats.getHealth());

    fight.controller.takeStageTwoIceDamage(0.25f, player);

    assertEquals(99, fight.stats.getHealth());
    assertTrue(fight.stats.isInvulnerable());
  }

  @Test
  void invalidAndRecursiveIceDamageCannotFillTheFractionalAccumulator() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    int[] damageEvents = {0};
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              damageEvents[0]++;
              fight.controller.takeStageTwoIceDamage(0.75f, player);
            });
    fight.controller.takeStageTwoIceDamage(0.75f, player);
    fight.controller.takeStageTwoIceDamage(Float.NaN, player);
    fight.controller.takeStageTwoIceDamage(Float.POSITIVE_INFINITY, player);
    fight.controller.takeStageTwoIceDamage(Float.NEGATIVE_INFINITY, player);
    fight.controller.takeStageTwoIceDamage(-1f, player);
    fight.controller.takeStageTwoIceDamage(0f, player);
    fight.controller.takeStageTwoIceDamage(0.25f, null);
    assertEquals(100, fight.stats.getHealth());

    fight.controller.takeStageTwoIceDamage(0.25f, player);
    assertEquals(99, fight.stats.getHealth());
    for (int impact = 0; impact < 3; impact++) {
      fight.controller.takeStageTwoIceDamage(0.25f, player);
      assertEquals(99, fight.stats.getHealth());
    }
    fight.controller.takeStageTwoIceDamage(0.25f, player);

    assertEquals(98, fight.stats.getHealth());
    assertEquals(2, damageEvents[0]);
  }

  @Test
  void replacingProtectionClearsPartialDamageBeforeStageTwoReentry() {
    for (int replacement = 0; replacement < 3; replacement++) {
      IceFight fight = startIceFight();
      Entity player = new Entity();
      fight.controller.takeStageTwoIceDamage(0.75f, player);
      switch (replacement) {
        case 0 -> fight.controller.enableShield();
        case 1 -> fight.controller.openVulnerabilityWindow(1f, 0);
        default -> fight.controller.disableStageOneProtection();
      }
      fight.controller.takeStageTwoIceDamage(0.75f, player);
      fight.controller.enableStageTwoIceProtection(60);
      fight.controller.takeStageTwoIceDamage(0.25f, player);
      assertEquals(100, fight.stats.getHealth());

      fight.controller.takeStageTwoIceDamage(0.75f, player);

      assertEquals(99, fight.stats.getHealth());
      assertTrue(fight.stats.isInvulnerable());
    }
  }

  @Test
  void rejectedStageOneTransformationAndDeadBossHitsLeaveNoFractionalDamage() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    FinalBossDamageControllerComponent controller = new FinalBossDamageControllerComponent();
    FinalBossPhaseControllerComponent phases = new FinalBossPhaseControllerComponent();
    new Entity().addComponent(stats).addComponent(controller).addComponent(phases).create();
    Entity player = new Entity();
    controller.enableStageTwoIceProtection(60);
    controller.takeStageTwoIceDamage(0.75f, player);
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    controller.enableStageTwoIceProtection(60);
    controller.takeStageTwoIceDamage(0.75f, player);

    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    controller.enableStageTwoIceProtection(60);
    controller.takeStageTwoIceDamage(0.25f, player);
    assertEquals(100, stats.getHealth());
    controller.takeStageTwoIceDamage(0.75f, player);
    assertEquals(99, stats.getHealth());

    stats.setHealth(0);
    controller.takeStageTwoIceDamage(0.75f, player);
    stats.setHealth(100);
    controller.takeStageTwoIceDamage(0.25f, player);
    assertEquals(100, stats.getHealth());
  }

  @Test
  void fractionalDamageAtTheHealthFloorPreservesTheStageThreeShield() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    fight.stats.setHealth(61);
    fight
        .boss
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> {
              if (health == 60) {
                fight.phases.completeStage(FinalBossPhase.STAGE_TWO);
              }
            });
    fight.controller.takeStageTwoIceDamage(0.75f, player);
    assertEquals(61, fight.stats.getHealth());

    fight.controller.takeStageTwoIceDamage(0.5f, player);
    fight.controller.takeStageTwoIceDamage(0.75f, player);

    assertEquals(60, fight.stats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, fight.phases.getCurrentPhase());
    assertTrue(fight.phases.isTransitioning());
    assertTrue(fight.controller.isShielded());
    assertTrue(fight.stats.isInvulnerable());
    assertEquals(0, fight.stats.getMinimumHealth());
  }

  @Test
  void aCallbackFailureDoesNotApplyTheSameAccumulatedWholeDamageTwice() {
    IceFight fight = startIceFight();
    Entity player = new Entity();
    int[] damageEvents = {0};
    fight
        .boss
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer healthLost, Integer remainingHealth) -> {
              if (++damageEvents[0] == 1) {
                throw new IllegalStateException("Broken first damage observer");
              }
            });
    fight.controller.takeStageTwoIceDamage(0.75f, player);

    assertThrows(
        IllegalStateException.class, () -> fight.controller.takeStageTwoIceDamage(0.5f, player));
    assertEquals(99, fight.stats.getHealth());
    assertTrue(fight.stats.isInvulnerable());
    fight.controller.takeStageTwoIceDamage(0.5f, player);
    assertEquals(99, fight.stats.getHealth());
    fight.controller.takeStageTwoIceDamage(0.25f, player);

    assertEquals(98, fight.stats.getHealth());
    assertEquals(2, damageEvents[0]);
  }

  @Test
  void veryLargeFiniteIceDamageCannotOverflowOrPassTheHealthFloor() {
    IceFight fight = startIceFight();

    fight.controller.takeStageTwoIceDamage(Float.MAX_VALUE, new Entity());

    assertEquals(60, fight.stats.getHealth());
    assertTrue(fight.stats.isInvulnerable());
    assertTrue(fight.controller.isShielded());
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
