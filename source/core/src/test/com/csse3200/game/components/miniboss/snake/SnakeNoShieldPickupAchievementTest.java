package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Qualifies an entire Snake encounter using actual vulnerable windows and green-gem collections.
 */
@ExtendWith(GameExtension.class)
class SnakeNoShieldPickupAchievementTest {
  private static final String ACHIEVEMENT_NAME = "What are those green things?";
  private static final float STEP = 0.01f;

  private final List<String> unlockedNames = new ArrayList<>();
  private GameTime time;
  private World world;
  private AchievementService achievements;
  private Achievement achievement;
  private SnakeMiniBossConfig config;
  private Entity snake;
  private Entity player;
  private CombatStatsComponent snakeStats;
  private CombatStatsComponent playerStats;
  private SnakeBurrowComponent burrow;
  private SnakeShieldComponent shield;
  private SnakeShieldPickupComponent pickups;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerEntityService(mock(EntityService.class));
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    OrthographicCamera camera = new OrthographicCamera(20f, 20f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);

    AchievementConfig definition = new AchievementConfig();
    definition.name = ACHIEVEMENT_NAME;
    definition.type = "snakeDefeatedWithoutShieldPickup";
    achievement = AchievementsFactory.build(definition);
    achievements = new AchievementService();
    achievements.register(achievement);
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
    ServiceLocator.registerAchievementService(achievements);

    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats);
    player.create();
    createSnake(false);
  }

  @AfterEach
  void cleanUp() {
    snake.dispose();
    player.dispose();
    world.dispose();
  }

  @Test
  void shouldUnlockOnlyAfterDefeatingSnakeWithoutCollectingEvenIfThePlayerTookDamage() {
    enterStageTwo();
    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
    assertEquals(2, pickups.getPickupCount());
    assertStillLocked();

    defeatSnake();

    assertEquals(SnakeBurrowComponent.State.DEAD, burrow.getState());
    assertEquals(0, pickups.getPickupCount());
    assertFalse(pickups.hasCollectedShieldGem());
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldQualifyAOneHitVictoryThatNeverNeededStageTwoDrops() {
    reachExposed();
    assertFalse(burrow.isStageTwo());
    assertEquals(0, pickups.getPickupCount());

    defeatSnake();

    assertTrue(achievement.isUnlocked());
    assertFalse(burrow.isStageTwo());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldRejectACollectionEvenAfterTheShieldIsUsedUpAndDropsAreCleared() {
    enterStageTwo();
    collectGem();
    for (int i = 0; i < config.shieldCapacity; i++) {
      assertTrue(shield.tryBlock());
    }
    assertFalse(shield.isActive());
    assertEquals(0, shield.getDurability());
    pickups.clear();
    assertTrue(pickups.hasCollectedShieldGem());

    defeatSnake();

    assertStillLocked();
  }

  @Test
  void shouldRejectARealCollectionEvenWhenHealthAndShieldAreAlreadyFull() {
    enterStageTwo();
    playerStats.setHealth(playerStats.getMaxHealth());
    shield.refill();
    assertFalse(pickups.hasCollectedShieldGem());

    collectGem();

    assertEquals(100, playerStats.getHealth());
    assertEquals(config.shieldCapacity, shield.getDurability());
    defeatSnake();
    assertStillLocked();
  }

  @Test
  void shouldAllowUncollectedDropsToExpireWithoutDisqualifyingTheEncounter() {
    enterStageTwo();
    config.shieldGemSpawnInterval = 100f;

    tickPickups(config.shieldGemLifetime + STEP);

    assertEquals(0, pickups.getPickupCount());
    assertFalse(pickups.hasCollectedShieldGem());
    assertStillLocked();
    defeatSnake();
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldAllowVictoryAfterStandingNearAGemOnTheOtherSideOfAWall() {
    enterStageTwo();
    Vector2 gem = pickups.getPickupPositions().getFirst();
    placePlayerAt(gem.cpy().add(0.5f, 0f));
    createWall(new Rectangle(gem.x + 0.24f, gem.y - 1f, 0.02f, 2f));

    tickPickups(0.36f);

    assertEquals(2, pickups.getPickupCount());
    assertFalse(pickups.hasCollectedShieldGem());
    assertFalse(shield.isActive());
    defeatSnake();
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldNotCountTouchingAGemBeforeItsAppearanceAnimationFinishes() {
    enterStageTwo();
    placePlayerAt(pickups.getPickupPositions().getFirst());

    tickPickups(0.1f);

    assertEquals(2, pickups.getPickupCount());
    assertFalse(pickups.hasCollectedShieldGem());
    defeatSnake();
    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldRecordCollectionBeforeAHealingListenerSynchronouslyKillsSnake() {
    enterStageTwo();
    playerStats.setHealth(30);
    player
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> {
              if (health > 30) {
                assertTrue(pickups.hasCollectedShieldGem());
                defeatSnake();
              }
            });
    // Collect the last of two gems: death clears the list during its backwards update loop.
    placePlayerAt(pickups.getPickupPositions().getLast());

    tickPickups(0.36f);

    assertEquals(45, playerStats.getHealth());
    assertTrue(snakeStats.isDead());
    assertEquals(0, pickups.getPickupCount());
    assertStillLocked();
  }

  @Test
  void shouldCancelAnEncounterDisposedWhileSnakeIsStillAlive() {
    reachExposed();
    snake.dispose();
    assertFalse(snakeStats.isDead());
    assertStillLocked();

    defeatSnake();

    assertStillLocked();
  }

  @Test
  void shouldNotRestoreEligibilityWhenADeadPlayerIsRevivedInTheSameEncounter() {
    reachExposed();
    playerStats.setHealth(0);
    assertStillLocked();
    playerStats.setHealth(100);

    defeatSnake();

    assertStillLocked();
  }

  @Test
  void shouldStillAwardVictoryWhenAnEarlierDeathListenerDisposesSnake() {
    snake.dispose();
    createSnake(true);
    enterStageTwo();

    defeatSnake();

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotifyOnlyOnceAcrossRepeatedDeathEventsAndCleanup() {
    reachExposed();
    defeatSnake();

    snake.getEvents().trigger("entityDied");
    snake.dispose();
    snake.getEvents().trigger("entityDied");

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldNotNotifyAgainWhenTheAchievementWasRestoredAsUnlocked() {
    achievements.restoreState(List.of(ACHIEVEMENT_NAME), Map.of());
    reachExposed();

    defeatSnake();

    assertTrue(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldGiveANewSnakeEncounterAnIndependentAttemptAfterAnEarlierCollection() {
    enterStageTwo();
    collectGem();
    defeatSnake();
    assertStillLocked();
    snake.dispose();
    createSnake(false);
    assertFalse(pickups.hasCollectedShieldGem());
    enterStageTwo();

    defeatSnake();

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  private void createSnake(boolean disposeOnDeathBeforeTracker) {
    player.setPosition(0f, 0f);
    config = new SnakeMiniBossConfig();
    snakeStats = new CombatStatsComponent(150, 10);
    burrow = new SnakeBurrowComponent(player, config);
    shield = new SnakeShieldComponent(player, config);
    pickups = new SnakeShieldPickupComponent(player, config, new Random(7L));
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(burrow)
            .addComponent(shield)
            .addComponent(pickups)
            .addComponent(mock(SnakePoisonVolleyComponent.class))
            .addComponent(new SnakeAchievementComponent(player));
    snake.setPosition(3f, 0f);
    if (disposeOnDeathBeforeTracker) {
      // Register before create(), so disposal runs ahead of every component's death listener.
      snake.getEvents().addListener("entityDied", snake::dispose);
    }
    snake.create();
    Rectangle bounds = new Rectangle(-8f, -8f, 16f, 16f);
    burrow.setArenaBounds(bounds);
    pickups.setArenaBounds(bounds);
    tickBurrow(0f);
  }

  private void reachExposed() {
    tickBurrow(config.burrowDuration);
    tickBurrow(config.undergroundDuration);
    tickBurrow(config.warningDuration);
    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertFalse(snakeStats.isInvulnerable());
  }

  private void enterStageTwo() {
    reachExposed();
    snakeStats.takeDamage(75, player);
    tickBurrow(STEP);
    assertTrue(burrow.isStageTwo());
    assertEquals(2, pickups.getPickupCount());
    // Stage activation does not age newly spawned gems on the same frame.
    tickPickups(STEP);
  }

  private void collectGem() {
    int previousCount = pickups.getPickupCount();
    placePlayerAt(pickups.getPickupPositions().getFirst());
    tickPickups(0.36f);
    assertEquals(previousCount - 1, pickups.getPickupCount());
    assertTrue(pickups.hasCollectedShieldGem());
    assertTrue(shield.isActive());
  }

  private void defeatSnake() {
    snakeStats.takeDamage(snakeStats.getHealth(), player);
    assertTrue(snakeStats.isDead());
  }

  private void tickBurrow(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    burrow.update();
  }

  private void tickPickups(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    pickups.update();
  }

  private void placePlayerAt(Vector2 centre) {
    player.setPosition(centre.cpy().mulAdd(player.getScale(), -0.5f));
  }

  private void createWall(Rectangle rectangle) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(
        rectangle.x + rectangle.width / 2f, rectangle.y + rectangle.height / 2f);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(rectangle.width / 2f, rectangle.height / 2f);
    world.createBody(definition).createFixture(shape, 0f);
    shape.dispose();
  }

  private void assertStillLocked() {
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }
}
