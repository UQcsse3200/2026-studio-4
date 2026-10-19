package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises actual green-gem pickup timing and the independent Snake challenge IDs. */
@ExtendWith(GameExtension.class)
class SnakeGreenBubbleAchievementTest {
  private static final String GREEN_BUBBLE_NAME = "Green bubble";
  private static final String NO_SHIELD_NAME = "What are those green things?";
  private final List<String> unlockedNames = new ArrayList<>();
  private GameTime time;
  private AchievementService achievements;
  private Achievement greenBubble;
  private Achievement noShield;
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
    OrthographicCamera camera = new OrthographicCamera(20f, 20f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);

    greenBubble = achievement(SnakeAchievementComponent.GREEN_BUBBLE, GREEN_BUBBLE_NAME);
    noShield = achievement(SnakeAchievementComponent.NO_SHIELD_PICKUP, NO_SHIELD_NAME);
    achievements = spy(new AchievementService());
    achievements.register(greenBubble);
    achievements.register(noShield);
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);
    ServiceLocator.registerAchievementService(achievements);

    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats);
    player.create();
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
    snake.create();
    Rectangle bounds = new Rectangle(-8f, -8f, 16f, 16f);
    burrow.setArenaBounds(bounds);
    pickups.setArenaBounds(bounds);
    tickBurrow(0f);
  }

  @AfterEach
  void cleanUp() {
    snake.dispose();
    player.dispose();
  }

  @Test
  void firstRealStageTwoPickupUnlocksGreenBubbleAndRejectsTheNoShieldVictory() {
    enterStageTwo();
    assertFalse(greenBubble.isUnlocked());

    collectGem();

    assertTrue(greenBubble.isUnlocked());
    assertTrue(shield.isActive());
    assertFalse(noShield.isUnlocked());
    snakeStats.takeDamage(snakeStats.getHealth(), player);
    assertTrue(snakeStats.isDead());
    assertFalse(noShield.isUnlocked());
    assertEquals(List.of(GREEN_BUBBLE_NAME), unlockedNames);
  }

  @Test
  void repeatedPickupsReportGreenBubbleOnlyOnce() {
    enterStageTwo();
    collectGem();
    collectGem();

    assertEquals(List.of(GREEN_BUBBLE_NAME), unlockedNames);
    verify(achievements, times(1))
        .update(
            argThat(
                context ->
                    SnakeAchievementComponent.GREEN_BUBBLE.equals(context.bossAchievementId)));
  }

  @Test
  void aRealPickupOutsideStageTwoDoesNotUnlockGreenBubble() {
    assertFalse(burrow.isStageTwo());
    pickups.start();
    tickPickups(0.01f);

    collectGem();

    assertTrue(pickups.hasCollectedShieldGem());
    assertFalse(greenBubble.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void spawningGemsAndRefillingTheShieldWithoutCollectingDoNotUnlock() {
    enterStageTwo();

    shield.refill();
    snake.getEvents().trigger(SnakeShieldPickupComponent.SHIELD_GEM_COLLECTED_EVENT);

    assertTrue(shield.isActive());
    assertFalse(pickups.hasCollectedShieldGem());
    assertFalse(greenBubble.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void standingOnAGemBeforeItsAppearanceFinishesDoesNotUnlock() {
    enterStageTwo();
    placePlayerAt(pickups.getPickupPositions().getFirst());

    tickPickups(0.1f);

    assertFalse(pickups.hasCollectedShieldGem());
    assertFalse(greenBubble.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void aPickupStillCountsWhenHealthAndShieldAreAlreadyFull() {
    enterStageTwo();
    playerStats.setHealth(playerStats.getMaxHealth());
    shield.refill();

    collectGem();

    assertTrue(greenBubble.isUnlocked());
    assertEquals(100, playerStats.getHealth());
    assertEquals(config.shieldCapacity, shield.getDurability());
  }

  @Test
  void aDeadPlayerCannotUnlockAndRevivalDoesNotRestartTheAttempt() {
    enterStageTwo();
    Vector2 gem = pickups.getPickupPositions().getFirst();
    playerStats.setHealth(0);
    snake.getEvents().trigger(SnakeShieldPickupComponent.SHIELD_GEM_COLLECTED_EVENT);
    assertFalse(greenBubble.isUnlocked());
    playerStats.setHealth(100);
    placePlayerAt(gem);

    tickPickups(0.36f);

    assertTrue(pickups.hasCollectedShieldGem());
    assertFalse(greenBubble.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void aLivingEncounterDisposedBeforePickupCannotUnlock() {
    enterStageTwo();
    Vector2 gem = pickups.getPickupPositions().getFirst();
    snake.dispose();
    placePlayerAt(gem);

    tickPickups(0.36f);
    snake.getEvents().trigger(SnakeShieldPickupComponent.SHIELD_GEM_COLLECTED_EVENT);

    assertFalse(snakeStats.isDead());
    assertFalse(pickups.hasCollectedShieldGem());
    assertFalse(greenBubble.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void bossDeathBeforeACollectionOnlyAwardsTheNoShieldVictory() {
    enterStageTwo();
    Vector2 gem = pickups.getPickupPositions().getFirst();
    snakeStats.takeDamage(snakeStats.getHealth(), player);
    placePlayerAt(gem);

    tickPickups(0.36f);
    snake.getEvents().trigger(SnakeShieldPickupComponent.SHIELD_GEM_COLLECTED_EVENT);

    assertFalse(greenBubble.isUnlocked());
    assertTrue(noShield.isUnlocked());
    assertEquals(List.of(NO_SHIELD_NAME), unlockedNames);
  }

  @Test
  void pickupIsReportedBeforeAHealingListenerKillsSnake() {
    enterStageTwo();
    playerStats.setHealth(30);
    player
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> {
              if (health > 30) {
                assertTrue(greenBubble.isUnlocked());
                assertTrue(pickups.hasCollectedShieldGem());
                snakeStats.takeDamage(snakeStats.getHealth(), player);
              }
            });
    placePlayerAt(pickups.getPickupPositions().getLast());

    tickPickups(0.36f);

    assertTrue(snakeStats.isDead());
    assertFalse(noShield.isUnlocked());
    assertEquals(45, playerStats.getHealth());
    assertEquals(List.of(GREEN_BUBBLE_NAME), unlockedNames);
  }

  @Test
  void unlockingMayDisposeTheEncounterWithoutHealingAfterCleanup() {
    enterStageTwo();
    playerStats.setHealth(30);
    achievements.getEvents().addListener("achievementUnlocked", (String name) -> snake.dispose());
    placePlayerAt(pickups.getPickupPositions().getLast());

    tickPickups(0.36f);

    assertEquals(30, playerStats.getHealth());
    assertEquals(0, pickups.getPickupCount());
    assertTrue(greenBubble.isUnlocked());
    assertFalse(noShield.isUnlocked());
    assertEquals(List.of(GREEN_BUBBLE_NAME), unlockedNames);
  }

  private Achievement achievement(String id, String name) {
    AchievementConfig definition = new AchievementConfig();
    definition.type = "snake";
    definition.bossAchievementId = id;
    definition.name = name;
    return AchievementsFactory.build(definition);
  }

  private void enterStageTwo() {
    tickBurrow(config.burrowDuration);
    tickBurrow(config.undergroundDuration);
    tickBurrow(config.warningDuration);
    snakeStats.takeDamage(75, player);
    tickBurrow(0.01f);
    assertTrue(burrow.isStageTwo());
    assertEquals(2, pickups.getPickupCount());
    tickPickups(0.01f);
  }

  private void collectGem() {
    int previousCount = pickups.getPickupCount();
    placePlayerAt(pickups.getPickupPositions().getFirst());
    tickPickups(0.36f);
    assertEquals(previousCount - 1, pickups.getPickupCount());
    assertTrue(pickups.hasCollectedShieldGem());
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
}
