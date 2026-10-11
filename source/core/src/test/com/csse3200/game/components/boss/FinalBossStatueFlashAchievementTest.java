package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Actual statue evades, including deferred contacts and unavailable landing space. */
@ExtendWith(GameExtension.class)
class FinalBossStatueFlashAchievementTest {
  private static final String ACHIEVEMENT_NAME = "How can a statue even flash?";
  private static final float STEP = 0.01f;

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> flashes = new ArrayList<>();
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

    AchievementConfig definition = new AchievementConfig();
    definition.name = ACHIEVEMENT_NAME;
    definition.type = "finalBossStageThreeStatueFlash";
    achievement = AchievementsFactory.build(definition);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements
        .getEvents()
        .addListener("achievementUnlocked", (String name) -> unlockedNames.add(name));
    ServiceLocator.registerAchievementService(achievements);

    playerStats = new CombatStatsComponent(100, 10, 3f, 1f);
    playerStats.setInvulnerable(true);
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
        .addListener(FinalBossEvents.STAGE_THREE_STATUE_FLASH, (Entity who) -> flashes.add(who));
  }

  @AfterEach
  void disposeWorld() {
    stage.dispose();
    tracker.dispose();
    world.dispose();
  }

  @Test
  void shouldWaitUntilTheThirdContactActuallyMovesTheStatueOutsideThePhysicsCallback() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    Vector2 before = statue.entity.getCenterPosition();
    hit(statue);
    hit(statue);
    tick(STEP);
    assertEquals(8, statue.hitsRemaining);
    assertEquals(before, statue.entity.getCenterPosition());
    assertStillLocked();
    Entity weapon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(1, 1))
            .addComponent(new TouchAttackComponent(PhysicsLayer.NPC));
    weapon.setPosition(statue.entity.getPosition());
    weapon.create();
    AtomicInteger contacts = new AtomicInteger();
    statue
        .entity
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity source) -> {
              if (source != weapon) return;
              assertTrue(world.isLocked());
              assertTrue(statue.evadePending);
              assertEquals(before, statue.entity.getCenterPosition());
              assertStillLocked();
              contacts.incrementAndGet();
            });

    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);

    assertEquals(1, contacts.get());
    assertEquals(7, statue.hitsRemaining);
    assertStillLocked();
    assertTrue(flashes.isEmpty());
    tick(STEP);
    assertTrue(statue.entity.getCenterPosition().dst(before) > 2.5f);
    assertFalse(statue.evadePending);
    assertEquals(2, stage.bursts.size());
    assertEquals(List.of(player), flashes);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotCountRepeatedContactsFromTheSameAttackOrNonPositiveDamage() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    Entity repeatedAttack = new Entity();
    for (int contact = 0; contact < 5; contact++) stats(statue).takeDamage(1, repeatedAttack);
    stats(statue).takeDamage(0, new Entity());
    stats(statue).takeDamage(-1, new Entity());
    stats(statue).takeDamage(1, null);
    tick(STEP);

    assertEquals(9, statue.hitsRemaining);
    assertFalse(statue.evadePending);
    assertTrue(flashes.isEmpty());
    assertStillLocked();
    // Distinct attack sources qualify without requiring melee-only weapon metadata.
    hit(statue);
    hit(statue);
    tick(STEP);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotifyOnlyOnceWhenDifferentStatuesFlash() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue first = stage.statues.getFirst();
    putPlayerBeside(first);
    hitThreeTimes(first);
    tick(STEP);
    FinalBossStageThreeComponent.Statue second = stage.statues.get(1);
    Vector2 before = second.entity.getCenterPosition();
    putPlayerBeside(second);

    hitThreeTimes(second);
    tick(STEP);

    assertTrue(second.entity.getCenterPosition().dst(before) > 2.5f);
    assertEquals(List.of(player, player), flashes);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldStayLockedWhenNoLandingSpaceExistsAndUnlockOnlyAfterARetrySucceeds() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    Vector2 before = statue.entity.getCenterPosition();
    Body wall = blockArena();
    hitThreeTimes(statue);

    tick(STEP);

    assertEquals(before, statue.entity.getCenterPosition());
    assertTrue(statue.evadePending);
    assertTrue(stage.bursts.isEmpty());
    assertTrue(flashes.isEmpty());
    assertStillLocked();
    world.destroyBody(wall);
    tick(0.1f);
    assertStillLocked();
    tick(0.2f);
    assertTrue(statue.entity.getCenterPosition().dst(before) > 2.5f);
    assertEquals(List.of(player), flashes);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotAwardAFlashForAStatueDestroyedBeforeItsQueuedEvade() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    Vector2 before = statue.entity.getCenterPosition();
    for (int hit = 0; hit < config.statueHits; hit++) hit(statue);
    assertTrue(statue.broken);

    tick(STEP);

    assertEquals(before, statue.entity.getCenterPosition());
    assertTrue(flashes.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldCancelAQueuedFlashWhenThePlayerDies() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    hitThreeTimes(statue);
    playerStats.setHealth(0);

    tick(STEP);

    assertTrue(flashes.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldCancelAQueuedFlashWhenTheBossDies() {
    enterWaveTwo();
    hitThreeTimes(stage.statues.getFirst());
    bossStats.setHealth(0);

    tick(STEP);

    assertTrue(flashes.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldIgnoreEarlierWavesAndWrongPlayersWithoutConsumingTheRealAttempt() {
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_STATUE_FLASH, player);
    assertStillLocked();
    enterWaveOne();
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_STATUE_FLASH, player);
    assertStillLocked();
    bossStats.setHealth(20);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_STATUE_FLASH, player);
    assertStillLocked();
    finishCharge();
    boss.getEvents().trigger(FinalBossEvents.STAGE_THREE_STATUE_FLASH, new Entity());
    assertStillLocked();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);

    hitThreeTimes(statue);
    tick(STEP);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotUnlockAfterTheTrackerIsDisposedEvenIfAStatueReallyFlashes() {
    enterWaveTwo();
    tracker.dispose();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    hitThreeTimes(statue);

    tick(STEP);

    assertEquals(List.of(player), flashes);
    assertStillLocked();
  }

  @Test
  void shouldNotRunAQueuedFlashAfterTheStageIsDisposed() {
    enterWaveTwo();
    hitThreeTimes(stage.statues.getFirst());
    stage.dispose();

    tick(STEP);

    assertTrue(flashes.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotAdvanceOtherStatuesAfterAnUnlockListenerDisposesTheEncounter() {
    enterWaveTwo();
    FinalBossStageThreeComponent.Statue first = stage.statues.getFirst();
    FinalBossStageThreeComponent.Statue next = stage.statues.get(1);
    putPlayerBeside(first);
    Vector2 nextPosition = next.entity.getCenterPosition();
    float nextPause = next.pauseRemaining;
    float nextCooldown = next.slamCooldown;
    hitThreeTimes(first);
    achievements.getEvents().addListener("achievementUnlocked", (String name) -> stage.dispose());

    tick(STEP);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    assertEquals(List.of(player), flashes);
    assertEquals(nextPosition, next.entity.getCenterPosition());
    // No remaining statue may advance movement or slam timers after the callback cleans up.
    assertEquals(nextPause, next.pauseRemaining);
    assertEquals(nextCooldown, next.slamCooldown);
    assertEquals(0f, next.warningRemaining);
    assertTrue(stage.shockwaves.isEmpty());
    tick(STEP);
    assertEquals(List.of(player), flashes);
  }

  private void enterWaveOne() {
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertFalse(phases.isTransitioning());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stage.getState());
  }

  private void enterWaveTwo() {
    enterWaveOne();
    bossStats.setHealth(20);
    finishCharge();
  }

  private void finishCharge() {
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      statue.slamCooldown = 100f;
      statue.pauseRemaining = 100f;
    }
    stage.bursts.clear();
  }

  private void putPlayerBeside(FinalBossStageThreeComponent.Statue statue) {
    player.setPosition(
        statue.entity.getCenterPosition().add(-1f, 0f).sub(player.getScale().scl(0.5f)));
  }

  private CombatStatsComponent stats(FinalBossStageThreeComponent.Statue statue) {
    return statue.entity.getComponent(CombatStatsComponent.class);
  }

  private void hit(FinalBossStageThreeComponent.Statue statue) {
    stats(statue).takeDamage(1, new Entity());
  }

  private void hitThreeTimes(FinalBossStageThreeComponent.Statue statue) {
    for (int hit = 0; hit < 3; hit++) hit(statue);
  }

  private Body blockArena() {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent());
    wall.setPosition(-20f, -20f);
    wall.setScale(40f, 40f);
    wall.create();
    return wall.getComponent(PhysicsComponent.class).getBody();
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
