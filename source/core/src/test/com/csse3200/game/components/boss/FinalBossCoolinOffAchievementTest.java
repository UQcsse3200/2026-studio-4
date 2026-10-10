package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
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

/** Real pickup, input and projectile collisions feeding the Stage 2 ice-hit achievement. */
@ExtendWith(GameExtension.class)
class FinalBossCoolinOffAchievementTest {
  private static final String ACHIEVEMENT_NAME = "Coolin' off, make it fun";

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> hitPlayers = new ArrayList<>();
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stage;
  private FinalBossStageTwoPlayerIceController weapon;
  private FinalBossStageTwoPickupController energy;
  private FinalBossStageTwoArenaComponent arena;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossAchievementComponent tracker;
  private AchievementService achievements;
  private Achievement achievement;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private Entity boss;
  private Entity player;
  private EntityService entities;
  private InputService inputs;
  private Input previousInput;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    previousInput = Gdx.input;
    Gdx.input = mock(Input.class);
    inputs = new InputService();
    ServiceLocator.registerInputService(inputs);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    AchievementConfig definition = new AchievementConfig();
    definition.name = ACHIEVEMENT_NAME;
    definition.type = "finalBossStageTwoIceHit";
    achievement = AchievementsFactory.build(definition);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements
        .getEvents()
        .addListener("achievementUnlocked", (String name) -> unlockedNames.add(name));
    ServiceLocator.registerAchievementService(achievements);

    config = new FinalBossStageTwoConfig();
    config.iceCoverCount = 0;
    config.icePickupCount = 0;
    config.fireballInitialDelay = 1000f;
    playerStats = new CombatStatsComponent(100, 10);
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    when(effects.modifyIncomingDamage(anyInt())).thenAnswer(call -> call.getArgument(0));
    when(effects.getStatMultiplier(Stat.ATTACK)).thenReturn(1f);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(new PhysicsComponent())
            .addComponent(new PlayerActions())
            .addComponent(effects);
    player.setPosition(9.5f, 9.5f);
    player.create();

    phases = new FinalBossPhaseControllerComponent();
    arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(-100f, -100f, 200f, 200f));
    FinalBossStageOneComponent stageOne = mock(FinalBossStageOneComponent.class);
    when(stageOne.getState()).thenReturn(FinalBossStageOneState.INTRO);
    bossStats = new CombatStatsComponent(1000, 0);
    stage = new FinalBossStageTwoComponent(player, config);
    tracker = new FinalBossAchievementComponent(player);
    boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(arena)
            .addComponent(stageOne)
            .addComponent(stage)
            .addComponent(tracker);
    boss.setPosition(19.5f, 9.5f);
    boss.create();
    bossStats.setHealth(800);
    weapon = stage.getPlayerIceController();
    energy = stage.getPickupController();
    boss.getEvents()
        .addListener(FinalBossEvents.STAGE_TWO_ICE_HIT, (Entity hit) -> hitPlayers.add(hit));
  }

  @AfterEach
  void disposeWorldAndRestoreInput() {
    stage.dispose();
    tracker.dispose();
    world.dispose();
    Gdx.input = previousInput;
  }

  @Test
  void shouldUnlockOnTheFirstFractionalImpactRatherThanPickupFiringOrHealthLoss() {
    startEncounter();
    grantCharge();
    assertEquals(1, energy.getChargeCount());
    assertStillLocked();
    fireOneShot();
    assertEquals(1, weapon.shots.size());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();

    advance(2f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(800, bossStats.getHealth());
    assertEquals(List.of(player), hitPlayers);
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldStayLockedOnAWallBlockedShotAndAllowTheNextUnobstructedHit() {
    startEncounter();
    addWall(15f, 10f);
    grantCharge();
    fireOneShot();

    advance(2f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(1, weapon.impacts.size());
    assertEquals(800, bossStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();

    // Aim above the wall; a blocked shot did not consume the first successful hit's attempt.
    player.setPosition(9.5f, 15.5f);
    boss.setPosition(19.5f, 15.5f);
    advance(0.001f);
    fireOneShot();
    advance(2f);

    assertEquals(List.of(player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotUnlockOrDamageCoverWhenARealIceObstacleStopsTheShot() {
    config.iceCoverCount = 1;
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = stage.getIceController().covers.getFirst();
    float y = cover.bounds.y + cover.bounds.height / 2f;
    player.setPosition(cover.bounds.x - 2.5f, y - 0.5f);
    boss.setPosition(cover.bounds.x + cover.bounds.width + 2.5f, y - 0.5f);
    advance(0.001f);
    grantCharge();
    fireOneShot();

    advance(0.6f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(config.iceCoverHits, cover.hitsRemaining);
    assertEquals(1, stage.getIceController().covers.size());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldUnlockIfTheBossIsReachedBeforeAWallInTheSameFlightStep() {
    startEncounter();
    addWall(21f, 10f);
    grantCharge();
    fireOneShot();

    // This flight segment crosses both the boss at x=20 and the wall at x=21.
    advance(2f);

    assertTrue(weapon.shots.isEmpty());
    assertEquals(List.of(player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotUnlockWhenAnIceShotExpiresWithoutReachingTheBoss() {
    boss.setPosition(89.5f, 9.5f);
    startEncounter();
    grantCharge();
    fireOneShot();

    advance(config.iceProjectileLifetime);

    assertTrue(weapon.shots.isEmpty());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldIgnoreOrdinaryAttacksAndUnrelatedPlayerDamage() {
    startEncounter();
    bossStats.hit(playerStats);
    bossStats.takeDamage(30, new Entity());
    playerStats.takeDamage(5, boss);

    assertEquals(800, bossStats.getHealth());
    assertEquals(95, playerStats.getHealth());
    assertTrue(hitPlayers.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotifyOnlyOnceAcrossRepeatedSuccessfulIceShots() {
    startEncounter();
    grantCharge();
    fireOneShot();
    advance(2f);
    fireOneShot();
    advance(2f);

    assertEquals(List.of(player, player), hitPlayers);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotNotifyAgainIfTheAchievementWasRestoredAsUnlocked() {
    achievements.restoreState(List.of(ACHIEVEMENT_NAME), Map.of());
    startEncounter();
    grantCharge();
    fireOneShot();

    advance(2f);

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(player), hitPlayers);
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldIgnoreAnotherPlayerWithoutConsumingTheRealPlayersAttempt() {
    startEncounter();
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, new Entity());
    assertStillLocked();
    grantCharge();
    fireOneShot();

    advance(2f);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreStageOneAndTheTransitionWithoutDisablingTheStageTwoAchievement() {
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, player);
    assertStillLocked();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    assertTrue(phases.isTransitioning());
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, player);
    assertStillLocked();
    finishTransition();
    grantCharge();
    fireOneShot();

    advance(2f);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreStaleEventsAfterStageTwoEnds() {
    startEncounter();
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());

    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, player);

    assertStillLocked();
  }

  @Test
  void shouldIgnoreEventsWhileThePlayerOrBossIsDead() {
    startEncounter();
    playerStats.setHealth(0);
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, player);
    assertStillLocked();
    playerStats.setHealth(100);
    bossStats.setMinimumHealth(0);
    bossStats.setHealth(0);
    assertTrue(bossStats.isDead());
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_ICE_HIT, player);

    assertStillLocked();
  }

  @Test
  void shouldNotUnlockAfterTheTrackerIsDisposed() {
    startEncounter();
    tracker.dispose();
    grantCharge();
    fireOneShot();

    advance(2f);

    assertEquals(List.of(player), hitPlayers);
    assertStillLocked();
  }

  @Test
  void shouldUnlockBeforeTheFirstIceHitTriggersTheStageThreeHealthFloor() {
    config.iceProjectileDamage = 1f;
    startEncounter();
    bossStats.setHealth(601);
    grantCharge();
    fireOneShot();

    advance(2f);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    assertEquals(600, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertTrue(weapon.shots.isEmpty());
    assertEquals(0, energy.getChargeCount());
  }

  @Test
  void shouldNotDamageOrResumeFiringAfterAnUnlockListenerDisposesTheEncounter() {
    config.iceProjectileDamage = 1f;
    startEncounter();
    grantCharge();
    achievements
        .getEvents()
        .addListener(
            "achievementUnlocked",
            (String name) -> {
              stage.dispose();
              tracker.dispose();
            });
    holdJ();
    advance(0.01f);
    assertEquals(1, weapon.shots.size());

    advance(2f);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    assertEquals(800, bossStats.getHealth());
    assertTrue(weapon.shots.isEmpty());
    assertEquals(0, energy.getChargeCount());
    assertFalse(stage.getIceInput().isHeld());
    advance(2f);
    assertTrue(weapon.shots.isEmpty());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  private void startEncounter() {
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    finishTransition();
  }

  private void finishTransition() {
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());
    advance(0.01f);
  }

  private void grantCharge() {
    energy.pickups.add(new FinalBossStageTwoPickupController.Pickup(player.getCenterPosition()));
    advance(0.001f);
  }

  private void fireOneShot() {
    holdJ();
    advance(0.01f);
    when(Gdx.input.isKeyPressed(Input.Keys.J)).thenReturn(false);
    inputs.keyUp(Input.Keys.J);
  }

  private void holdJ() {
    when(Gdx.input.isKeyPressed(Input.Keys.J)).thenReturn(true);
    inputs.keyDown(Input.Keys.J);
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
    entities.update();
  }

  private void assertStillLocked() {
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  private void addWall(float x, float y) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.2f, 4f);
    world.createBody(definition).createFixture(shape, 0f);
    shape.dispose();
  }
}
