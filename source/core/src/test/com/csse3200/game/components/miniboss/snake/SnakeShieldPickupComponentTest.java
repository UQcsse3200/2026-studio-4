package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeShieldPickupComponentTest {
  private GameTime time;
  private RenderService renderer;
  private ResourceService resources;
  private SnakeMiniBossConfig config;
  private Entity player;
  private Entity snake;
  private SnakeShieldComponent shield;
  private SnakeShieldPickupComponent pickups;
  private World world;
  private OrthographicCamera camera;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    renderer = mock(RenderService.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(renderer);
    camera = new OrthographicCamera(20f, 20f);
    camera.position.set(0f, 0f, 0f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);
    resources = new ResourceService();
    resources.loadTextures(
        new String[] {
          SnakeShieldPickupComponent.GEM_TEXTURE, SnakeShieldPickupComponent.SPAWN_TEXTURE
        });
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    config = new SnakeMiniBossConfig();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 1))
            .addComponent(new StatusEffectsControllerComponent());
    player.setPosition(0f, 0f);
    player.create();
    shield = mock(SnakeShieldComponent.class);
    pickups = new SnakeShieldPickupComponent(player, config, new Random(7L));
    snake =
        new Entity()
            .addComponent(new CombatStatsComponent(150, 1))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(shield)
            .addComponent(pickups);
    snake.setPosition(8f, 8f);
    snake.create();
    pickups.setArenaBounds(new Rectangle(-8f, -8f, 16f, 16f));
  }

  @AfterEach
  void cleanUp() {
    resources.dispose();
    if (world != null) {
      world.dispose();
    }
  }

  @Test
  void waitsForStageTwoAndStartsOnlyOnceWithoutAgingNewDropsImmediately() {
    tick(100f);
    assertEquals(0, pickups.getPickupCount());
    pickups.start();
    List<Vector2> initial = pickups.getPickupPositions();
    assertEquals(2, initial.size());
    pickups.start();
    tick(100f);
    assertEquals(initial, pickups.getPickupPositions());
    tick(0.35f);
    assertEquals(2, pickups.getPickupCount());
  }

  @Test
  void limitsActiveDropsAndNeverCatchesUpWithABurstAfterALongFrame() {
    pickups.start();
    tick(0.01f);
    tick(5f);
    assertEquals(3, pickups.getPickupCount());
    tick(5f);
    assertEquals(3, pickups.getPickupCount());
    tick(100f);
    assertEquals(1, pickups.getPickupCount());
  }

  @Test
  void expiresAfterTwelveSecondsAndIgnoresInvalidOrPausedTime() {
    config.shieldGemSpawnInterval = 100f;
    pickups.start();
    tick(0.01f);
    tick(11.9f);
    assertEquals(2, pickups.getPickupCount());
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      tick(delta);
    }
    assertEquals(2, pickups.getPickupCount());
    tick(0.2f);
    assertEquals(0, pickups.getPickupCount());
  }

  @Test
  void collectsAfterAppearanceRefillsShieldAndRechecksHealingThresholdForEachGem() {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(30);
    pickups.start();
    tick(0.01f);
    List<Vector2> positions = pickups.getPickupPositions();
    placePlayerAt(positions.getFirst());
    tick(0.34f);
    verify(shield, never()).refill();
    assertEquals(30, stats.getHealth());
    tick(0.02f);
    assertEquals(1, pickups.getPickupCount());
    verify(shield).refill();
    assertEquals(45, stats.getHealth());
    tick(0.1f);
    verify(shield).refill();
    placePlayerAt(positions.getLast());
    tick(0.1f);
    assertEquals(0, pickups.getPickupCount());
    verify(shield, times(2)).refill();
    assertEquals(45, stats.getHealth());
  }

  @Test
  void pickupsRemainUsableWhenSnakeIsFrozenAndPlayerIsConcealed() {
    pickups.start();
    tick(0.01f);
    snake
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new FrozenEffect(time, 10000L));
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new InvisibilityEffect(time, 10000L));
    placePlayerAt(pickups.getPickupPositions().getFirst());
    tick(0.4f);
    verify(shield).refill();
    assertEquals(1, pickups.getPickupCount());
  }

  @Test
  void copiesBoundsAndPositionsAndKeepsDropsInsideReachablePlayerClearance() {
    Rectangle bounds = new Rectangle(0f, 0f, 10f, 10f);
    player.setPosition(1f, 4.5f);
    pickups.setArenaBounds(bounds);
    bounds.set(100f, 100f, 1f, 1f);
    createWall(new Rectangle(5f, 0f, 0.1f, 10f));
    pickups.start();
    List<Vector2> positions = pickups.getPickupPositions();
    assertEquals(2, positions.size());
    for (Vector2 position : positions) {
      assertTrue(position.x >= 0.5f && position.x < 4.5f);
      assertTrue(position.y >= 0.5f && position.y <= 9.5f);
      assertTrue(position.dst(player.getCenterPosition()) >= 1.25f);
    }
    assertTrue(positions.getFirst().dst(positions.getLast()) >= 1f);
    Vector2 actual = positions.getFirst().cpy();
    positions.getFirst().set(1000f, 1000f);
    assertEquals(actual, pickups.getPickupPositions().getFirst());
  }

  @Test
  void stillSpawnsIntoOpenRoomWhenPlayerIsStandingCloseToAWall() {
    pickups.setArenaBounds(new Rectangle(0f, 0f, 8f, 8f));
    placePlayerAt(new Vector2(0.3f, 0.5f));
    createWall(new Rectangle(0f, -10f, 0.1f, 20f));
    pickups.start();
    assertEquals(2, pickups.getPickupCount());
    for (Vector2 position : pickups.getPickupPositions()) {
      assertTrue(position.x > 0.6f);
    }
  }

  @Test
  void skipsFullyBlockedAndTooSmallRoomsWithoutSpawningThroughWalls() {
    pickups.setArenaBounds(new Rectangle(-2f, -2f, 4f, 4f));
    createWall(new Rectangle(-5f, -5f, 10f, 10f));
    pickups.start();
    tick(0.01f);
    tick(5f);
    assertEquals(0, pickups.getPickupCount());
    pickups.clear();
    pickups.setArenaBounds(new Rectangle(0f, 0f, 0.8f, 0.8f));
    pickups.start();
    assertEquals(0, pickups.getPickupCount());
  }

  @Test
  void nearbyGemCannotBeCollectedAcrossAWall() {
    pickups.start();
    tick(0.01f);
    Vector2 gem = pickups.getPickupPositions().getFirst();
    placePlayerAt(gem.cpy().add(0.5f, 0f));
    createWall(new Rectangle(gem.x + 0.24f, gem.y - 1f, 0.02f, 2f));
    tick(0.4f);
    verify(shield, never()).refill();
    assertEquals(2, pickups.getPickupCount());
    placePlayerAt(gem);
    tick(0.1f);
    verify(shield).refill();
    assertEquals(1, pickups.getPickupCount());
  }

  @Test
  void stillUsesVisibleCameraBoundsWhenRoomBoundsAreUnavailable() {
    pickups.setArenaBounds(null);
    camera.viewportWidth = 12f;
    camera.viewportHeight = 8f;
    camera.zoom = 0.75f;
    camera.update();
    pickups.start();
    assertEquals(2, pickups.getPickupCount());
    for (Vector2 position : pickups.getPickupPositions()) {
      assertInside(position, new Rectangle(-3.7f, -2.2f, 7.4f, 4.4f));
    }
  }

  @Test
  void initiallyAlternatesNearbyAndFartherDropsWithinTheCurrentView() {
    pickups.start();
    List<Vector2> positions = pickups.getPickupPositions();
    assertEquals(2, positions.size());
    float near = positions.getFirst().dst(player.getCenterPosition());
    float far = positions.getLast().dst(player.getCenterPosition());
    assertTrue(near >= 1.5f && near <= 3.5f);
    assertTrue(far >= 3.5f);
    for (Vector2 position : positions) {
      assertInside(position, new Rectangle(-7.2f, -7.2f, 14.4f, 14.4f));
    }
  }

  @Test
  void laterSpawnsFollowCameraPositionZoomAndRoomEdgesWithoutMovingExistingGems() {
    config.shieldGemMaxActive = 8;
    pickups.start();
    tick(0.01f);
    List<Vector2> initial = pickups.getPickupPositions();
    camera.position.set(6f, 0f, 0f);
    camera.zoom = 0.5f;
    camera.update();
    tick(5f);
    List<Vector2> firstMove = pickups.getPickupPositions();
    assertEquals(3, firstMove.size());
    assertEquals(initial, firstMove.subList(0, 2));
    assertInside(firstMove.getLast(), new Rectangle(1.8f, -4.2f, 5.4f, 8.4f));

    camera.position.set(-4f, 4f, 0f);
    camera.zoom = 0.4f;
    camera.update();
    tick(5f);
    List<Vector2> secondMove = pickups.getPickupPositions();
    assertEquals(4, secondMove.size());
    assertEquals(firstMove, secondMove.subList(0, 3));
    assertInside(secondMove.getLast(), new Rectangle(-7.2f, 0.8f, 6.4f, 6.4f));
  }

  @Test
  void waitsForWorldCameraInitializationAndSkipsViewsWithoutSafeRoomSpace() {
    ServiceLocator.registerWorldCamera(null);
    pickups.start();
    tick(0.01f);
    tick(5f);
    assertEquals(0, pickups.getPickupCount());
    ServiceLocator.registerWorldCamera(camera);
    tick(5f);
    assertEquals(1, pickups.getPickupCount());
    pickups.clear();
    camera.position.set(100f, 100f, 0f);
    camera.update();
    pickups.start();
    assertEquals(0, pickups.getPickupCount());
    pickups.clear();
    camera.position.set(0f, 0f, 0f);
    camera.zoom = 0.05f;
    camera.update();
    pickups.start();
    assertEquals(0, pickups.getPickupCount());
  }

  @Test
  void healsOnlyBelowFortyPercentWhileEveryGemStillRefillsTheShield() {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(39);
    collectNewGem();
    assertEquals(54, stats.getHealth());
    stats.setHealth(40);
    collectNewGem();
    assertEquals(40, stats.getHealth());
    stats.setHealth(100);
    collectNewGem();
    assertEquals(100, stats.getHealth());
    verify(shield, times(3)).refill();
  }

  @Test
  void roundsHealingToWholeHealthCapsAtNinetyPercentAndNeverLowersExistingHealth() {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    stats.setMaxHealth(105);
    stats.setHealth(30);
    collectNewGem();
    assertEquals(46, stats.getHealth());
    stats.setHealth(30);
    config.shieldGemHealFraction = 1f;
    collectNewGem();
    assertEquals(94, stats.getHealth());
    stats.setHealth(100);
    config.shieldGemHealThreshold = 1f;
    collectNewGem();
    assertEquals(100, stats.getHealth());
    config.shieldGemHealThreshold = 0.4f;
    config.shieldGemHealFraction = 0.15f;
    stats.setMaxHealth(3);
    stats.setHealth(1);
    collectNewGem();
    assertEquals(2, stats.getHealth());
    verify(shield, times(4)).refill();
  }

  @Test
  void clearCanRestartButDeathAndDisposalPermanentlyStopDrops() {
    pickups.start();
    pickups.clear();
    tick(100f);
    assertEquals(0, pickups.getPickupCount());
    pickups.start();
    assertEquals(2, pickups.getPickupCount());
    snake.getComponent(CombatStatsComponent.class).setHealth(0);
    assertEquals(0, pickups.getPickupCount());
    snake.getComponent(CombatStatsComponent.class).setHealth(150);
    pickups.start();
    assertEquals(0, pickups.getPickupCount());
    pickups.dispose();
    verify(renderer).unregister(pickups);
    assertTrue(resources.containsAsset(SnakeShieldPickupComponent.GEM_TEXTURE, Texture.class));
    assertTrue(resources.containsAsset(SnakeShieldPickupComponent.SPAWN_TEXTURE, Texture.class));
  }

  @Test
  void playerDeathClearsDropsBeforeAnotherCollectionOrSpawn() {
    pickups.start();
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(10f);
    assertEquals(0, pickups.getPickupCount());
    assertEquals(0, player.getComponent(CombatStatsComponent.class).getHealth());
    verify(shield, never()).refill();
  }

  @Test
  void smallerGemAppearsInPlaceWithSecondRowGreenEffectOnceAndRestoresBatchColour() {
    Color colour = new Color(0.2f, 0.3f, 0.4f, 0.5f);
    float original = colour.toFloatBits();
    List<Draw> draws = new ArrayList<>();
    SpriteBatch batch = recordingBatch(colour, draws);
    Texture effectTexture =
        resources.getAsset(SnakeShieldPickupComponent.SPAWN_TEXTURE, Texture.class);
    Texture gemTexture = resources.getAsset(SnakeShieldPickupComponent.GEM_TEXTURE, Texture.class);
    Vector2 position = startOneGem();
    for (int frameIndex = 0; frameIndex < 7; frameIndex++) {
      if (frameIndex > 0) {
        tick(0.051f);
      }
      draws.clear();
      pickups.render(batch);
      assertEquals(2, draws.size());
      Draw effect = draws.getFirst();
      assertSame(effectTexture, effect.region().getTexture());
      assertEquals(frameIndex * 64, effect.region().getRegionX());
      assertEquals(64, effect.region().getRegionY());
      assertEquals(64, effect.region().getRegionWidth());
      assertEquals(64, effect.region().getRegionHeight());
      assertEquals(1.1f, effect.width(), 0.0001f);
      assertEquals(1.1f, effect.height(), 0.0001f);
      assertTrue(effect.centre().epsilonEquals(position, 0.0001f));
      assertEquals(Color.WHITE, effect.colour());
      Draw gem = draws.getLast();
      assertSame(gemTexture, gem.region().getTexture());
      assertEquals(0.39f, gem.width(), 0.0001f);
      assertEquals(0.65f, gem.height(), 0.0001f);
      assertTrue(gem.centre().epsilonEquals(position, 0.0001f));
      assertEquals(frameIndex * 0.051f / 0.35f, gem.colour().a, 0.0001f);
      assertEquals(1f, gem.colour().r);
      assertEquals(1f, gem.colour().g);
      assertEquals(1f, gem.colour().b);
      assertEquals(original, colour.toFloatBits());
    }

    tick(0.045f); // The seventh effect frame ends; only the fully visible gem remains.
    draws.clear();
    pickups.render(batch);
    assertEquals(1, draws.size());
    assertSame(gemTexture, draws.getFirst().region().getTexture());
    assertEquals(Color.WHITE, draws.getFirst().colour());
  }

  @Test
  void fullyVisibleGemKeepsRotatingThenFadesBeforeExpiry() {
    Color colour = new Color(0.2f, 0.3f, 0.4f, 0.5f);
    float original = colour.toFloatBits();
    List<Draw> draws = new ArrayList<>();
    SpriteBatch batch = recordingBatch(colour, draws);
    Vector2 position = startOneGem();
    tick(0.82f); // The original ten-frame gem keeps animating after the spawn effect ends.
    pickups.render(batch);
    assertEquals(1, draws.size());
    assertEquals(162, draws.getFirst().region().getRegionX());
    assertEquals(18, draws.getFirst().region().getRegionWidth());
    assertEquals(30, draws.getFirst().region().getRegionHeight());
    assertTrue(draws.getFirst().centre().epsilonEquals(position, 0.0001f));
    tick(10.68f);
    draws.clear();
    pickups.render(batch);
    assertEquals(1, draws.size());
    assertEquals(0.5f, draws.getFirst().colour().a, 0.0001f);
    assertEquals(original, colour.toFloatBits());
  }

  @Test
  void restartingDropsResetsAppearanceAndDisposalClearsBothLayers() {
    List<Draw> draws = new ArrayList<>();
    SpriteBatch batch = recordingBatch(new Color(Color.WHITE), draws);
    startOneGem();
    tick(0.36f);
    pickups.render(batch);
    assertEquals(1, draws.size());
    pickups.clear();
    pickups.start();
    draws.clear();
    pickups.render(batch);
    assertEquals(2, draws.size());
    assertEquals(0, draws.getFirst().region().getRegionX());
    assertEquals(0f, draws.getLast().colour().a);
    pickups.dispose();
    draws.clear();
    pickups.render(batch);
    assertTrue(draws.isEmpty());
  }

  private Vector2 startOneGem() {
    config.shieldGemInitialCount = 1;
    config.shieldGemSpawnInterval = 100f;
    pickups.start();
    tick(0.01f); // Do not charge the activation frame to either animation.
    return pickups.getPickupPositions().getFirst();
  }

  private void collectNewGem() {
    config.shieldGemInitialCount = 1;
    pickups.clear();
    pickups.start();
    assertEquals(1, pickups.getPickupCount());
    placePlayerAt(pickups.getPickupPositions().getFirst());
    tick(0.01f);
    tick(0.36f);
    assertEquals(0, pickups.getPickupCount());
  }

  private static void assertInside(Vector2 position, Rectangle rectangle) {
    float epsilon = 0.0001f;
    assertTrue(position.x >= rectangle.x - epsilon);
    assertTrue(position.x <= rectangle.x + rectangle.width + epsilon);
    assertTrue(position.y >= rectangle.y - epsilon);
    assertTrue(position.y <= rectangle.y + rectangle.height + epsilon);
  }

  private SpriteBatch recordingBatch(Color colour, List<Draw> draws) {
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenAnswer(ignored -> colour.toFloatBits());
    doAnswer(
            invocation -> {
              colour.set(
                  invocation.getArgument(0),
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    doAnswer(
            invocation -> {
              Color.abgr8888ToColor(colour, (float) invocation.getArgument(0));
              return null;
            })
        .when(batch)
        .setPackedColor(anyFloat());
    doAnswer(
            invocation -> {
              float x = invocation.getArgument(1);
              float y = invocation.getArgument(2);
              float width = invocation.getArgument(3);
              float height = invocation.getArgument(4);
              draws.add(
                  new Draw(
                      invocation.getArgument(0),
                      new Vector2(x + width / 2f, y + height / 2f),
                      width,
                      height,
                      new Color(colour)));
              return null;
            })
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    return batch;
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    pickups.update();
  }

  private void placePlayerAt(Vector2 centre) {
    player.setPosition(centre.cpy().mulAdd(player.getScale(), -0.5f));
  }

  private void createWall(Rectangle rectangle) {
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(
        rectangle.x + rectangle.width / 2f, rectangle.y + rectangle.height / 2f);
    PolygonShape shape = new PolygonShape();
    try {
      shape.setAsBox(rectangle.width / 2f, rectangle.height / 2f);
      world.createBody(definition).createFixture(shape, 0f);
    } finally {
      shape.dispose();
    }
  }

  private record Draw(
      TextureRegion region, Vector2 centre, float width, float height, Color colour) {}
}
