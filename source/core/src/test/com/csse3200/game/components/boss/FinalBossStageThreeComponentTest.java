package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageThreeComponentTest {
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private FinalBossPhaseControllerComponent phases;

  @BeforeEach
  void setup() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(0.1f);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    player.create();
    config = new FinalBossStageThreeConfig();
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
  }

  @Test
  void powerfulHitStopsAtTwentyPercentAndStartsInvulnerableCharge() {
    CombatStatsComponent stats = boss.getComponent(CombatStatsComponent.class);
    stats.takeDamage(10000, player);
    assertEquals(20, stats.getHealth());
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    assertTrue(stats.isInvulnerable());
    stats.takeDamage(10, player);
    assertEquals(20, stats.getHealth());
    assertEquals(0f, stage.hitRemaining);
    tick(2.9f);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    tick(0.11f);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    assertEquals(5, stage.getRemainingStatues());
    assertFalse(boss.getComponent(PhysicsComponent.class).getBody().isActive());
  }

  @Test
  void statuesRequireTenPositiveHitsRegardlessOfDamageAndIgnoreFurtherHits() {
    startStatues();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    CombatStatsComponent stats = statue.entity.getComponent(CombatStatsComponent.class);
    assertEquals(10, stats.getHealth());
    stats.takeDamage(0, player);
    stats.takeDamage(-1, player);
    assertEquals(10, statue.hitsRemaining);
    for (int i = 0; i < 9; i++) stats.takeDamage(999, new Entity());
    assertFalse(statue.broken);
    assertEquals(1, statue.hitsRemaining);
    assertEquals(1, stats.getHealth());
    stats.takeDamage(1, new Entity());
    assertTrue(statue.broken);
    assertEquals(0, stats.getHealth());
    assertFalse(statue.entity.getComponent(PhysicsComponent.class).getBody().isActive());
    verify(ServiceLocator.getEntityService()).scheduleDisposal(statue.entity);
    assertEquals(4, stage.getRemainingStatues());
    stats.takeDamage(999, new Entity());
    assertEquals(0, statue.hitsRemaining);
  }

  @Test
  void onlyTheFifthBrokenStatueReturnsLivingInvulnerableGrandpaOnce() {
    startStatues();
    AtomicInteger completed = new AtomicInteger();
    boss.getEvents().addListener("finalBossEncounterCompleted", completed::incrementAndGet);
    assertEquals(5, stage.statues.size());
    for (int statueIndex = 0; statueIndex < 5; statueIndex++) {
      FinalBossStageThreeComponent.Statue statue = stage.statues.get(statueIndex);
      for (int hit = 0; hit < 10; hit++) {
        assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
        assertEquals(0, completed.get());
        statue.entity.getComponent(CombatStatsComponent.class).takeDamage(10, new Entity());
      }
      assertEquals(4 - statueIndex, stage.getRemainingStatues());
    }
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    int health = player.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(0, completed.get());
    assertEquals(1.2f, config.returnTransformDuration);
    tick(config.returnTransformDuration - 0.01f);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertEquals(0, completed.get());
    tick(0.02f);
    assertEquals(health, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertFalse(boss.getComponent(CombatStatsComponent.class).isDead());
    assertTrue(boss.getComponent(CombatStatsComponent.class).isInvulnerable());
    assertTrue(boss.getComponent(PhysicsComponent.class).getBody().isActive());
    assertEquals(1, completed.get());
    tick(1f);
    assertEquals(1, completed.get());
    verify(phases, times(1)).completeStage(FinalBossPhase.STAGE_THREE);
  }

  @Test
  void iceHitLeadsToOneTeleportStrikeThatDamagesThawsAndImmediatelyRetreats() {
    AtomicInteger hits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageAttempted", (Integer damage, Entity attacker) -> hits.incrementAndGet());
    freezePlayer();
    assertEquals(3f, config.freezeDuration);
    assertEquals(1f, config.teleportDelay);
    PlayerActions actions = player.getComponent(PlayerActions.class);
    assertTrue(stage.isFrozen());
    assertTrue(actions.areControlsLocked());

    Vector2 before = boss.getCenterPosition();
    tick(0.99f);
    assertTrue(boss.getCenterPosition().epsilonEquals(before, 0.001f));
    assertEquals(0, hits.get());
    tick(0.02f);
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) <= 1.1f);
    assertEquals(0, hits.get());
    tick(0.20f);
    assertEquals(0, hits.get());
    assertTrue(stage.isFrozen());
    tick(0.06f);
    assertEquals(1, hits.get());
    assertEquals(
        100 - config.strikeDamage, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) > 2f);
    assertFalse(stage.isFrozen());
    assertFalse(actions.areControlsLocked());
    assertTrue(stage.thawRemaining > 0f);
    assertTrue(stage.bolts.isEmpty());

    tick(config.boltInterval - 0.01f);
    assertTrue(stage.bolts.isEmpty());
    assertEquals(1, hits.get());
    tick(0.02f);
    assertEquals(3, stage.bolts.size());
    assertEquals(1, hits.get());
  }

  @Test
  void teleportStrikeThawsOnlyItsOwnControlLock() {
    freezePlayer();
    Object dialogue = addOtherControlLock();
    tick(config.teleportDelay);
    tick(config.strikeDelay);
    assertEquals(
        100 - config.strikeDamage, player.getComponent(CombatStatsComponent.class).getHealth());
    assertFreezeReleasedButOtherLockRetained(dialogue);
  }

  @Test
  void bossPhysicallyWandersDuringTheFirstSecondBeforeTeleporting() {
    freezePlayer();
    Vector2 before = boss.getCenterPosition();
    tick(0.4f);
    boss.getComponent(PhysicsMovementComponent.class).update();
    ServiceLocator.getPhysicsService().getPhysics().getWorld().step(0.016f, 6, 2);
    boss.getComponent(PhysicsComponent.class).earlyUpdate();
    assertTrue(boss.getCenterPosition().dst(before) > 0.001f);
    assertTrue(boss.getCenterPosition().dst(before) < 0.2f);
    tick(0.59f);
    assertTrue(stage.bursts.stream().noneMatch(burst -> !burst.ice && !burst.grey));
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) > 2f);
    tick(0.02f);
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) <= 1.1f);
    assertTrue(stage.bursts.stream().anyMatch(burst -> !burst.ice && !burst.grey));
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void missedStrikeDoesNotRepeatAndNaturalThawReleasesOnlyTheBossControlLock() {
    AtomicInteger hits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageAttempted", (Integer damage, Entity attacker) -> hits.incrementAndGet());
    freezePlayer();
    Object dialogue = addOtherControlLock();
    tick(config.teleportDelay);
    // An external displacement can move the player out of range during the strike windup.
    player.setPosition(-5f, 0f);
    tick(config.strikeDelay);
    assertEquals(0, hits.get());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(stage.isFrozen());
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) > 2f);
    tick(config.freezeDuration - stage.freezeElapsed - 0.01f);
    assertTrue(stage.isFrozen());
    assertTrue(stage.bolts.isEmpty());
    assertEquals(0, hits.get());
    tick(0.02f);
    assertFreezeReleasedButOtherLockRetained(dialogue);
    assertEquals(0, hits.get());
  }

  @Test
  void chargeCancelsFreezeAndPendingStrikeWithoutRemovingAnotherControlLock() {
    freezePlayer();
    Object dialogue = addOtherControlLock();
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    assertFreezeReleasedButOtherLockRetained(dialogue);
    tick(config.teleportDelay + config.strikeDelay);
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(stage.bolts.isEmpty());
  }

  @Test
  void playerDeathCancelsFreezeAndPendingStrikeWithoutRemovingAnotherControlLock() {
    freezePlayer();
    Object dialogue = addOtherControlLock();
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(0.01f);
    assertFreezeReleasedButOtherLockRetained(dialogue);
    tick(config.freezeDuration);
    assertEquals(0, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(stage.bolts.isEmpty());
    assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
  }

  @Test
  void disposalReleasesOnlyItsOwnControlLockAndCancelsPendingStrike() {
    freezePlayer();
    Object dialogue = addOtherControlLock();
    stage.dispose();
    assertFreezeReleasedButOtherLockRetained(dialogue);
    tick(config.freezeDuration);
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(stage.bolts.isEmpty());
  }

  @Test
  void fastBoltCannotSkipPlayerBetweenFrames() {
    assertEquals(
        0f,
        FinalBossStageThreeComponent.segmentDistanceSquared(
            new Vector2(-10, 0), new Vector2(10, 0), new Vector2()));
    assertEquals(
        4f,
        FinalBossStageThreeComponent.segmentDistanceSquared(
            new Vector2(-10, 0), new Vector2(10, 0), new Vector2(0, 2)));
  }

  @Test
  void transitionInstallsStageThreeFloorBeforeAnyLaterComponentUpdate() {
    FinalBossPhaseControllerComponent realPhases = new FinalBossPhaseControllerComponent();
    FinalBossStageThreeComponent realStage =
        new FinalBossStageThreeComponent(player, Entity::create, config);
    Entity otherBoss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(realPhases)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(realStage);
    otherBoss.create();
    CombatStatsComponent health = otherBoss.getComponent(CombatStatsComponent.class);
    health.setHealth(40);
    realPhases.completeStage(FinalBossPhase.STAGE_ONE);
    realPhases.completeStage(FinalBossPhase.STAGE_TWO);
    when(time.getDeltaTime()).thenReturn(3f);
    realPhases.update();
    // Deliberately do not run realStage.update(): combat may occur first next frame.
    health.takeDamage(10000, player);
    assertEquals(20, health.getHealth());
    assertEquals(FinalBossStageThreeState.CHARGING, realStage.getState());
  }

  @Test
  void repeatedWeaponContactAndSourceLessDamageDoNotCountAsNewAttacks() {
    startStatues();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    CombatStatsComponent health = statue.entity.getComponent(CombatStatsComponent.class);
    Entity swordSwing = new Entity();
    health.takeDamage(10, swordSwing);
    health.takeDamage(10, swordSwing);
    health.takeDamage(10);
    assertEquals(9, statue.hitsRemaining);
    health.takeDamage(10, new Entity());
    assertEquals(8, statue.hitsRemaining);
  }

  private void freezePlayer() {
    stage.bolts.add(
        new FinalBossStageThreeComponent.Bolt(player.getCenterPosition(), new Vector2()));
    tick(0.01f);
  }

  private Object addOtherControlLock() {
    Object dialogue = new Object();
    player.getComponent(PlayerActions.class).setControlsLocked(dialogue, true);
    return dialogue;
  }

  private void assertFreezeReleasedButOtherLockRetained(Object owner) {
    assertFalse(stage.isFrozen());
    assertTrue(stage.thawRemaining > 0f);
    PlayerActions actions = player.getComponent(PlayerActions.class);
    assertTrue(actions.areControlsLocked());
    actions.setControlsLocked(owner, false);
    assertFalse(actions.areControlsLocked());
  }

  private void startStatues() {
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    tick(config.chargeDuration);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }
}
