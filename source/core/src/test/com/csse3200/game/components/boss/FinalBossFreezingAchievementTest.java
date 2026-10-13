package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
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
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Real three-bolt volleys exercise streak counting and whole-volley misses. */
@ExtendWith(GameExtension.class)
class FinalBossFreezingAchievementTest {
  private static final String ACHIEVEMENT_NAME = "Brrr, it's freezing!";
  private static final String SNOW_QUEEN_NAME = "Whoa! The Snow Queen";
  private static final float STEP = 0.01f;

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> hits = new ArrayList<>();
  private final List<Entity> misses = new ArrayList<>();
  private final List<String> outcomes = new ArrayList<>();
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossAchievementComponent tracker;
  private AchievementService achievements;
  private Achievement achievement;
  private Achievement snowQueen;
  private Entity boss;
  private Entity player;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    MathUtils.random.setSeed(2026L);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    ServiceLocator.registerRenderService(mock(RenderService.class));

    achievements = new AchievementService();
    achievement = register(ACHIEVEMENT_NAME, "finalBossStageThreeIceHitStreak");
    snowQueen = register(SNOW_QUEEN_NAME, "finalBossStageThreeIceHit");
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
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
        .addListener(
            FinalBossEvents.STAGE_THREE_ICE_HIT,
            (Entity who) -> {
              hits.add(who);
              outcomes.add("hit");
            });
    boss.getEvents()
        .addListener(
            FinalBossEvents.STAGE_THREE_ICE_VOLLEY_MISSED,
            (Entity who) -> {
              misses.add(who);
              outcomes.add("miss");
            });
  }

  @AfterEach
  void disposeWorld() {
    stage.dispose();
    tracker.dispose();
    world.dispose();
  }

  @Test
  void shouldUnlockSnowQueenOnTheFirstHitAndFreezingOnlyOnTheThirdRealVolleyHit() {
    enterWaveOne();
    assertStillLocked();

    landVolley();
    assertTrue(snowQueen.isUnlocked());
    assertEquals(100, playerStats.getHealth());
    assertEquals(List.of(SNOW_QUEEN_NAME), unlockedNames);
    assertStillLocked();
    landVolley();
    assertStillLocked();
    landVolley();

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(player, player, player), hits);
    assertTrue(misses.isEmpty());
    assertEquals(List.of(SNOW_QUEEN_NAME, ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldPreserveTheStreakThroughFreezeTheMandatoryStrikeAndUnrelatedDamage() {
    enterWaveOne();
    landVolley();
    finishFreezeCombo();
    assertEquals(100 - config.strikeDamage, playerStats.getHealth());
    playerStats.takeDamage(5, boss);
    assertEquals(95 - config.strikeDamage, playerStats.getHealth());
    assertStillLocked();

    landVolley();
    finishFreezeCombo();
    assertEquals(95 - 2 * config.strikeDamage, playerStats.getHealth());
    landVolley();

    assertTrue(achievement.isUnlocked());
    assertTrue(misses.isEmpty());
  }

  @Test
  void shouldResetAfterAnEntireRealVolleyExpiresAndRequireThreeNewHits() {
    enterWaveOne();
    landVolley();
    landVolley();
    expireVolley();
    assertEquals(List.of(player), misses);

    landVolley();
    assertStillLocked();
    landVolley();
    assertStillLocked();
    landVolley();

    assertTrue(achievement.isUnlocked());
    assertEquals(5, hits.size());
    assertEquals(List.of(SNOW_QUEEN_NAME, ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldKeepTheStreakWhenBothSideBoltsExpireButTheSameVolleyStillHits() {
    enterWaveOne();
    landVolley();
    landVolley();
    finishFreezeCombo();
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    holdAway(volley);
    expireNextStep(volley.get(0));
    expireNextStep(volley.get(2));
    tick(STEP);
    assertEquals(List.of(volley.get(1)), stage.bolts);
    assertTrue(misses.isEmpty());
    assertStillLocked();

    placeOnPlayer(volley.get(1));
    tick(STEP);

    assertTrue(achievement.isUnlocked());
    assertEquals(3, hits.size());
    assertTrue(misses.isEmpty());
  }

  @Test
  void shouldSettleAnOlderVolleyMissBeforeANewerVolleyHitInTheSameUpdate() {
    enterWaveOne();
    landVolley();
    landVolley();
    finishFreezeCombo();
    List<FinalBossStageThreeComponent.Bolt> older = spawnVolley();
    holdAway(older);
    tick(config.boltInterval);
    assertEquals(6, stage.bolts.size());
    List<FinalBossStageThreeComponent.Bolt> newer = new ArrayList<>(stage.bolts.subList(3, 6));
    older.forEach(this::expireNextStep);
    newer.forEach(this::placeOnPlayer);
    outcomes.clear();

    tick(STEP);

    assertEquals(List.of("miss", "hit"), outcomes);
    assertEquals(List.of(player), misses);
    assertStillLocked();
    landVolley();
    assertStillLocked();
    landVolley();
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldCountThreeSimultaneousBoltsAsOnlyOneSuccessfulVolley() {
    enterWaveOne();
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    volley.forEach(this::placeOnPlayer);

    tick(STEP);

    assertEquals(List.of(player), hits);
    assertTrue(stage.bolts.isEmpty());
    assertTrue(misses.isEmpty());
    assertStillLocked();
    landVolley();
    assertStillLocked();
    landVolley();
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldNotifyOnlyOnceAcrossMoreHitsAndMissesAfterUnlocking() {
    enterWaveOne();
    landVolley();
    landVolley();
    landVolley();
    expireVolley();
    landVolley();
    landVolley();

    assertTrue(achievement.isUnlocked());
    assertEquals(5, hits.size());
    assertEquals(1, misses.size());
    assertEquals(List.of(SNOW_QUEEN_NAME, ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotNotifyAgainForARestoredAchievementOrSuppressSnowQueen() {
    achievements.restoreState(List.of(ACHIEVEMENT_NAME), Map.of());
    enterWaveOne();

    landVolley();
    landVolley();
    landVolley();

    assertTrue(achievement.isUnlocked());
    assertTrue(snowQueen.isUnlocked());
    assertEquals(List.of(SNOW_QUEEN_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreAnotherTargetsHitsAndMissesWithoutChangingThePlayersStreak() {
    enterWaveOne();
    landVolley();
    Entity anotherPlayer = new Entity();
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, anotherPlayer);
    landVolley();
    assertStillLocked();
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_VOLLEY_MISSED, anotherPlayer);

    landVolley();

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(SNOW_QUEEN_NAME, ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreEarlierPhasesTransitionsChargingAndWaveTwo() {
    reportHit();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    reportHit();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    stage.startWaveOne();
    assertTrue(phases.isTransitioning());
    reportHit();
    assertStillLocked();
    finishTransition();
    landVolley();
    landVolley();
    assertStillLocked();
    bossStats.setHealth(20);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    reportHit();
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    reportHit();

    assertStillLocked();
  }

  @Test
  void shouldCancelTheStreakOnPlayerDeathEvenIfHealthIsRestored() {
    enterWaveOne();
    landVolley();
    landVolley();
    playerStats.setHealth(0);
    reportHit();
    assertStillLocked();
    playerStats.setHealth(100);
    landVolley();

    assertTrue(stage.isFrozen());
    assertStillLocked();
  }

  @Test
  void shouldCancelTheStreakWhenTheBossDiesOrThePhaseEnds() {
    enterWaveOne();
    landVolley();
    landVolley();
    bossStats.setHealth(0);
    reportHit();
    assertStillLocked();
    bossStats.setHealth(60);
    reportHit();
    assertStillLocked();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_THREE));
    assertEquals(FinalBossPhase.DEFEATED, phases.getCurrentPhase());
    reportHit();

    assertStillLocked();
  }

  @Test
  void shouldStopTrackingAfterDisposalAndCancelPendingProjectilesOnStageDisposal() {
    enterWaveOne();
    landVolley();
    landVolley();
    tracker.dispose();
    landVolley();
    assertEquals(3, hits.size());
    assertStillLocked();
    finishFreezeCombo();
    spawnVolley();

    stage.dispose();
    tick(config.boltLifetime + config.boltInterval);

    assertTrue(stage.bolts.isEmpty());
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
    assertEquals(3, hits.size());
    assertTrue(misses.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotFireAgainWhenAWholeVolleyMissCallbackDisposesTheEncounter() {
    enterWaveOne();
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    holdAway(volley);
    volley.forEach(this::expireNextStep);
    boss.getEvents()
        .addListener(
            FinalBossEvents.STAGE_THREE_ICE_VOLLEY_MISSED,
            (Entity who) -> {
              stage.dispose();
              tracker.dispose();
            });

    // This would also advance the next shot's countdown to zero without the callback guard.
    tick(config.boltInterval);

    assertEquals(List.of(player), misses);
    assertTrue(hits.isEmpty());
    assertTrue(stage.bolts.isEmpty());
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
    tick(config.boltInterval);
    assertTrue(stage.bolts.isEmpty());
    assertEquals(List.of(player), misses);
    assertTrue(hits.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotResumeFiringWhenTheThirdHitUnlockCallbackDisposesTheEncounter() {
    enterWaveOne();
    landVolley();
    landVolley();
    finishFreezeCombo();
    achievements
        .getEvents()
        .addListener(
            "achievementUnlocked",
            (String name) -> {
              if (ACHIEVEMENT_NAME.equals(name)) {
                stage.dispose();
                tracker.dispose();
              }
            });
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    holdAway(volley);
    placeOnPlayer(volley.get(1));

    tick(config.boltInterval);
    tick(config.boltInterval);

    assertEquals(List.of(SNOW_QUEEN_NAME, ACHIEVEMENT_NAME), unlockedNames);
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
    assertTrue(stage.bolts.isEmpty());
    assertEquals(3, hits.size());
    assertTrue(misses.isEmpty());
  }

  private Achievement register(String name, String bossAchievementId) {
    AchievementConfig definition = new AchievementConfig();
    definition.name = name;
    definition.type = "finalBoss";
    definition.bossAchievementId = bossAchievementId;
    Achievement result = AchievementsFactory.build(definition);
    achievements.register(result);
    return result;
  }

  private void enterWaveOne() {
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

  private List<FinalBossStageThreeComponent.Bolt> spawnVolley() {
    assertFalse(stage.isFrozen());
    assertTrue(stage.bolts.isEmpty());
    tick(config.boltInterval);
    assertEquals(3, stage.bolts.size());
    return new ArrayList<>(stage.bolts);
  }

  private void landVolley() {
    finishFreezeCombo();
    int previousHits = hits.size();
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    // Adjust trajectories, preserving the real shot's shared three-bolt identity.
    holdAway(volley);
    placeOnPlayer(volley.get(1));
    tick(STEP);
    assertTrue(stage.isFrozen());
    assertTrue(stage.bolts.isEmpty());
    assertEquals(previousHits + 1, hits.size());
  }

  private void expireVolley() {
    finishFreezeCombo();
    int previousMisses = misses.size();
    List<FinalBossStageThreeComponent.Bolt> volley = spawnVolley();
    holdAway(volley);
    volley.forEach(this::expireNextStep);
    tick(STEP);
    assertFalse(stage.isFrozen());
    assertTrue(stage.bolts.isEmpty());
    assertEquals(previousMisses + 1, misses.size());
  }

  private void finishFreezeCombo() {
    if (!stage.isFrozen()) return;
    tick(config.teleportDelay);
    tick(config.strikeDelay);
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
  }

  private void holdAway(List<FinalBossStageThreeComponent.Bolt> volley) {
    for (FinalBossStageThreeComponent.Bolt bolt : volley) {
      bolt.position.set(9f, 6f);
      bolt.velocity.setZero();
      bolt.elapsed = 0f;
    }
  }

  private void placeOnPlayer(FinalBossStageThreeComponent.Bolt bolt) {
    bolt.position.set(player.getCenterPosition());
    bolt.velocity.setZero();
    bolt.elapsed = 0f;
  }

  private void expireNextStep(FinalBossStageThreeComponent.Bolt bolt) {
    bolt.elapsed = config.boltLifetime - STEP / 2f;
  }

  private void reportHit() {
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_ICE_HIT, player);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }

  private void assertStillLocked() {
    assertFalse(achievement.isUnlocked());
    assertFalse(unlockedNames.contains(ACHIEVEMENT_NAME));
  }
}
