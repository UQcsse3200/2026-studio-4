package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Full Stage 2 attempts, real fire collisions and cancellation feeding the fireproof result. */
@ExtendWith(GameExtension.class)
class FinalBossFireproofAchievementTest {
  private static final String ACHIEVEMENT_NAME = "I'm fireproof!";

  private final List<String> unlockedNames = new ArrayList<>();
  private final List<Entity> starts = new ArrayList<>();
  private final List<Entity> hits = new ArrayList<>();
  private final List<Entity> completions = new ArrayList<>();
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoComponent stage;
  private FinalBossStageTwoFireController fire;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossAchievementComponent tracker;
  private AchievementService achievements;
  private Achievement achievement;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private StatusEffectsControllerComponent playerEffects;
  private ConsumableEffectComponent consumables;
  private InventoryComponent inventory;
  private Entity boss;
  private Entity player;
  private EntityService entities;
  private GameTime time;
  private World world;
  private long nextFireballId = 10000;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);

    AchievementConfig definition = new AchievementConfig();
    definition.name = ACHIEVEMENT_NAME;
    definition.type = "finalBoss";
    definition.bossAchievementId = "finalBossStageTwoFireproof";
    achievement = AchievementsFactory.build(definition);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
    ServiceLocator.registerAchievementService(achievements);

    config = new FinalBossStageTwoConfig();
    config.iceCoverCount = 0;
    config.icePickupCount = 0;
    config.fireballInitialDelay = 1000f;
    playerStats = new CombatStatsComponent(100, 10);
    playerEffects = spy(new StatusEffectsControllerComponent());
    consumables = new ConsumableEffectComponent();
    inventory = new InventoryComponent(0);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(playerEffects)
            .addComponent(inventory)
            .addComponent(consumables)
            .addComponent(new PhysicsComponent());
    player.setPosition(79.5f, 79.5f);
    player.create();

    phases = new FinalBossPhaseControllerComponent();
    FinalBossStageTwoArenaComponent arena = mock(FinalBossStageTwoArenaComponent.class);
    when(arena.getBounds()).thenReturn(new Rectangle(0f, 0f, 100f, 100f));
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
    boss.setPosition(49.5f, 49.5f);
    boss.create();
    bossStats.setHealth(800);
    fire = stage.getFireController();
    boss.getEvents().<Entity>addListener(FinalBossEvents.STAGE_TWO_STARTED, starts::add);
    boss.getEvents().<Entity>addListener(FinalBossEvents.STAGE_TWO_FIRE_HIT, hits::add);
    boss.getEvents().<Entity>addListener(FinalBossEvents.STAGE_TWO_COMPLETED, completions::add);
  }

  @AfterEach
  void disposeWorld() {
    stage.dispose();
    tracker.dispose();
    world.dispose();
  }

  @Test
  void shouldWaitForNormalStageCompletionAndUnlockOnlyOnce() {
    startEncounter();
    assertEquals(List.of(player), starts);
    assertStillLocked();
    advance(1f);
    assertStillLocked();

    finishWithIce();

    assertEquals(List.of(player), completions);
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    boss.getEvents().trigger(FinalBossEvents.STAGE_TWO_COMPLETED, player);
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldFailOnAHalfDamageFireCollisionEvenWithoutAnyHealthLoss() {
    startEncounter();

    hitWithFireball();

    assertEquals(100, playerStats.getHealth());
    assertEquals(List.of(player), hits);
    finishWithIce();
    assertStillLocked();
  }

  @Test
  void shouldCountFireContactDuringInvulnerability() {
    playerStats.setInvulnerable(true);
    assertProtectedContactFails();
  }

  @Test
  void shouldCountFireContactWithAZeroIncomingDamageMultiplier() {
    playerStats.setIncomingDamageMultiplier(0f);
    assertProtectedContactFails();
  }

  @Test
  void shouldCountFireContactWhenAStatusShieldAbsorbsAllDamage() {
    playerEffects.activateTimed();
    assertTrue(playerEffects.isShieldActive());
    assertProtectedContactFails();
  }

  @Test
  void shouldCountFireContactWhenAConsumableShieldBlocksTheHit() {
    inventory.addConsumable(ItemIds.SHIELD, 1);
    assertTrue(consumables.tryUse(ItemIds.SHIELD));
    assertTrue(consumables.isShielded());
    assertProtectedContactFails();
  }

  @Test
  void shouldAllowOtherDamageWhenNoStageTwoFireballHitsThePlayer() {
    startEncounter();
    playerStats.takeDamage(3, boss);
    playerStats.takeDamage(2, new Entity());
    assertEquals(95, playerStats.getHealth());
    assertTrue(hits.isEmpty());

    finishWithIce();

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldAllowAFireballStoppedByARealWallBeforeThePlayer() {
    player.setPosition(13.5f, 9.5f);
    addWall(12f, 10f);
    startEncounter();
    addFireball(new Vector2(10f, 10f), new Vector2(10f, 0f));

    advance(0.5f);

    assertTrue(fire.fireballs.isEmpty());
    assertEquals(1, fire.impacts.size());
    assertTrue(hits.isEmpty());
    finishWithIce();
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldAllowAFireballInterceptedByRealIceCover() {
    config.iceCoverCount = 1;
    startEncounter();
    FinalBossStageTwoIceController.Cover cover = stage.getIceController().covers.getFirst();
    float y = cover.bounds.y + cover.bounds.height / 2f;
    player.setPosition(cover.bounds.x + cover.bounds.width + 2f, y - 0.5f);
    advance(0.001f);
    addFireball(new Vector2(cover.bounds.x - 2f, y), new Vector2(10f, 0f));

    advance(0.7f);

    assertTrue(fire.fireballs.isEmpty());
    assertEquals(config.iceCoverHits - 1, cover.hitsRemaining);
    assertTrue(hits.isEmpty());
    finishWithIce();
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldAllowAMissedFireballThatExpiresBeforeReachingThePlayer() {
    startEncounter();
    addFireball(new Vector2(10f, 10f), new Vector2(1f, 0f));

    advance(config.fireballLifetime);

    assertTrue(fire.fireballs.isEmpty());
    assertTrue(hits.isEmpty());
    finishWithIce();
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldStillCountFireContactDuringTheFiringPause() {
    config.attackDuration = 0.2f;
    startEncounter();
    advance(0.25f);
    assertFalse(stage.isAttacking());

    hitWithFireball();
    finishWithIce();

    assertEquals(List.of(player), hits);
    assertStillLocked();
  }

  @Test
  void shouldNotUnlockForAForcedStageSkip() {
    startEncounter();

    assertTrue(phases.completeStage(FinalBossPhase.STAGE_TWO));

    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(completions.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotEraseAFireHitWhenTheStageControllerStartsAgain() {
    startEncounter();
    hitWithFireball();

    stage.startEncounter();
    assertEquals(List.of(player), starts);
    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldNotQualifyALateObserverByRepeatingTheStageStart() {
    tracker.dispose();
    startEncounter();
    replaceTracker();

    stage.startEncounter();
    assertEquals(List.of(player), starts);
    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldCancelTheAttemptOnPlayerDeathEvenIfThePlayerRevives() {
    startEncounter();
    playerStats.setHealth(0);
    playerStats.setHealth(100);

    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldCancelTheAttemptOnBossDeathEvenIfTheBossRevives() {
    startEncounter();
    bossStats.setHealth(0);
    bossStats.setHealth(800);

    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldNotUnlockAfterTheAchievementTrackerIsDisposed() {
    startEncounter();
    tracker.dispose();

    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldNotReportALateHealthThresholdAfterTheEncounterIsDisposed() {
    startEncounter();
    stage.dispose();

    bossStats.setHealth(600);

    assertTrue(completions.isEmpty());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertStillLocked();
  }

  @Test
  void shouldRememberAFireCollisionFromAnEarlierStartListener() {
    tracker.dispose();
    boss.getEvents()
        .addListener(FinalBossEvents.STAGE_TWO_STARTED, (Entity who) -> hitWithFireball());
    replaceTracker();

    startEncounter();
    assertEquals(List.of(player), hits);
    finishWithIce();

    assertStillLocked();
  }

  @Test
  void shouldStopDamageAndTheRemainingVolleyIfAFireHitListenerDisposesTheEncounter() {
    config.fireballDamage = 2f;
    startEncounter();
    boss.getEvents()
        .addListener(
            FinalBossEvents.STAGE_TWO_FIRE_HIT,
            (Entity who) -> {
              stage.dispose();
              tracker.dispose();
            });
    addFireball(player.getCenterPosition(), new Vector2());
    addFireball(player.getCenterPosition(), new Vector2());

    advance(0.01f);

    assertEquals(List.of(player), hits);
    assertEquals(100, playerStats.getHealth());
    assertTrue(fire.fireballs.isEmpty());
    advance(2f);
    assertTrue(fire.fireballs.isEmpty());
    assertStillLocked();
  }

  @Test
  void shouldNotResumeTheStageTransitionAfterAnUnlockListenerDisposesTheEncounter() {
    startEncounter();
    achievements
        .getEvents()
        .addListener(
            "achievementUnlocked",
            (String name) -> {
              stage.dispose();
              tracker.dispose();
            });

    boss.getComponent(FinalBossDamageControllerComponent.class)
        .takeStageTwoIceDamage(10000, player);

    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
    assertEquals(600, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertTrue(fire.fireballs.isEmpty());
    advance(2f);
    assertTrue(fire.fireballs.isEmpty());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  private void startEncounter() {
    assertTrue(phases.completeStage(FinalBossPhase.STAGE_ONE));
    when(time.getDeltaTime()).thenReturn(3f);
    phases.update();
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertFalse(phases.isTransitioning());
    advance(0.01f);
  }

  private void finishWithIce() {
    boss.getComponent(FinalBossDamageControllerComponent.class)
        .takeStageTwoIceDamage(10000, player);
    assertEquals(600, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
  }

  private void assertProtectedContactFails() {
    config.fireballDamage = 2f;
    startEncounter();
    hitWithFireball();
    assertEquals(100, playerStats.getHealth());
    assertEquals(List.of(player), hits);
    finishWithIce();
    assertStillLocked();
  }

  private void replaceTracker() {
    tracker.dispose();
    tracker = new FinalBossAchievementComponent(player);
    tracker.setEntity(boss);
    tracker.create();
  }

  private void hitWithFireball() {
    FinalBossStageTwoFireController.Fireball ball =
        addFireball(player.getCenterPosition(), new Vector2());
    advance(0.01f);
    assertFalse(fire.fireballs.contains(ball));
    assertFalse(fire.impacts.isEmpty());
  }

  private FinalBossStageTwoFireController.Fireball addFireball(Vector2 origin, Vector2 velocity) {
    FinalBossStageTwoFireController.Fireball ball =
        new FinalBossStageTwoFireController.Fireball(nextFireballId++, origin, velocity);
    fire.fireballs.add(ball);
    return ball;
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
    shape.setAsBox(0.2f, 2f);
    world.createBody(definition).createFixture(shape, 0f);
    shape.dispose();
  }
}
