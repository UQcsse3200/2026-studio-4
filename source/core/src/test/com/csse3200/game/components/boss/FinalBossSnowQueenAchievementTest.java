package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossSnowQueenAchievementTest {
  private static final String ACHIEVEMENT_NAME = "Whoa! The Snow Queen";

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> hitTargets = new ArrayList<>();
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossAchievementComponent tracker;
  private AchievementService achievements;
  private Achievement achievement;
  private Entity boss;
  private Entity player;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));

    AchievementConfig achievementConfig = new AchievementConfig();
    achievementConfig.name = ACHIEVEMENT_NAME;
    achievementConfig.type = "finalBossStageThreeIceHit";
    achievement = AchievementsFactory.build(achievementConfig);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements
        .getEvents()
        .addListener("achievementUnlocked", (String name) -> unlockedNames.add(name));
    ServiceLocator.registerAchievementService(achievements);

    playerStats = new CombatStatsComponent(100, 10, 3f, 1f);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(playerStats)
            .addComponent(new PlayerActions());
    player.create();

    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    FinalBossStageOneComponent stageOne = mock(FinalBossStageOneComponent.class);
    when(stageOne.getState()).thenReturn(FinalBossStageOneState.INTRO);
    config = new FinalBossStageThreeConfig();
    phases = new FinalBossPhaseControllerComponent();
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    tracker = new FinalBossAchievementComponent(player);
    bossStats = new CombatStatsComponent(100, 0);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stageOne)
            .addComponent(stage)
            .addComponent(tracker);
    boss.setPosition(4f, 0f);
    boss.create();
    bossStats.setHealth(60);
    boss.getEvents()
        .addListener(FinalBossEvents.STAGE_THREE_ICE_HIT, (Entity hit) -> hitTargets.add(hit));
  }

  @Test
  void shouldUnlockOnTheFirstIceImpactWithoutWaitingForHealthLoss() {
    enterWaveOne();
    assertFalse(achievement.isUnlocked());
    assertEquals(100, playerStats.getHealth());

    // Cross the player between frames: the final bolt position is already past the hit radius.
    Vector2 start = player.getCenterPosition().add(-1f, 0f);
    stage.bolts.add(new FinalBossStageThreeComponent.Bolt(start, new Vector2(20f, 0f)));
    tick(0.1f);

    assertTrue(stage.isFrozen());
    assertTrue(player.getComponent(PlayerActions.class).areControlsLocked());
    assertEquals(100, playerStats.getHealth());
    assertEquals(List.of(player), hitTargets);
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotUnlockWhenAnActualVolleyMissesThePlayer() {
    enterWaveOne();
    tick(config.boltInterval);
    assertEquals(3, stage.bolts.size());
    player.setPosition(0f, 6f);

    tick(0.1f);

    assertFalse(stage.isFrozen());
    assertStillLocked();
    assertTrue(hitTargets.isEmpty());
  }

  @Test
  void shouldIgnoreUnrelatedPlayerDamageAndStillAllowTheFirstIceHit() {
    enterWaveOne();
    playerStats.takeDamage(5, boss);
    assertEquals(95, playerStats.getHealth());
    assertStillLocked();

    collideWithPlayer();

    assertTrue(achievement.isUnlocked());
    assertEquals(95, playerStats.getHealth());
  }

  @Test
  void shouldNotifyOnlyOnceAcrossRepeatedIceCollisions() {
    enterWaveOne();
    collideWithPlayer();
    tick(config.freezeDuration);
    assertFalse(stage.isFrozen());

    collideWithPlayer();

    assertTrue(stage.isFrozen());
    assertEquals(List.of(player, player), hitTargets);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotNotifyAgainWhenTheAchievementWasRestoredAsUnlocked() {
    achievements.restoreState(List.of(ACHIEVEMENT_NAME), Map.of());
    enterWaveOne();

    collideWithPlayer();

    assertTrue(achievement.isUnlocked());
    assertTrue(stage.isFrozen());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldIgnoreAHitForAnotherEntityWithoutConsumingTheAttempt() {
    enterWaveOne();
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, new Entity());
    assertStillLocked();

    collideWithPlayer();

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreEarlierPhasesWithoutDisablingTheStageThreeAchievement() {
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
    assertStillLocked();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
    assertStillLocked();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    finishTransition();

    collideWithPlayer();

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreHitsDuringThePhaseTransitionEvenIfWaveOneWasStartedEarly() {
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    stage.startWaveOne();
    assertTrue(phases.isTransitioning());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stage.getState());
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
    assertStillLocked();
    finishTransition();

    collideWithPlayer();

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreHitsDuringChargingAndWaveTwo() {
    enterWaveOne();
    bossStats.setHealth(20);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
    assertStillLocked();

    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);

    assertStillLocked();
  }

  @Test
  void shouldNotUnlockAfterTheTrackerIsDisposed() {
    enterWaveOne();
    tracker.dispose();

    collideWithPlayer();

    assertTrue(stage.isFrozen());
    assertStillLocked();
  }

  @Test
  void shouldIgnoreHitEventsWhileThePlayerOrBossIsDead() {
    enterWaveOne();
    playerStats.setHealth(0);
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
    assertStillLocked();
    playerStats.setHealth(100);
    bossStats.setHealth(0);
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);

    assertStillLocked();
  }

  @Test
  void shouldNotResumeAttacksOrRelockThePlayerAfterAnUnlockListenerDisposesTheEncounter() {
    enterWaveOne();
    achievements
        .getEvents()
        .addListener(
            "achievementUnlocked",
            (String name) -> {
              stage.dispose();
              tracker.dispose();
            });
    stage.bolts.add(
        new FinalBossStageThreeComponent.Bolt(player.getCenterPosition(), new Vector2()));

    // A full volley interval reveals any firing logic accidentally resumed after the callback.
    tick(config.boltInterval);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
    assertTrue(stage.bolts.isEmpty());
    tick(config.boltInterval);
    assertTrue(stage.bolts.isEmpty());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  private void enterWaveOne() {
    // Real phase changes finish the old Stage 1 attempt before the new achievement is tested.
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    finishTransition();
  }

  private void finishTransition() {
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stage.getState());
  }

  private void collideWithPlayer() {
    stage.bolts.add(
        new FinalBossStageThreeComponent.Bolt(player.getCenterPosition(), new Vector2()));
    tick(0.01f);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }

  private void assertStillLocked() {
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }
}
