package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakePoisonVolleyComponentTest {
  private final List<Draw> draws = new ArrayList<>();
  private final Color batchColour = new Color(0.8f, 0.2f, 0.3f, 0.6f);
  private GameTime time;
  private ResourceService resources;
  private RenderService renderer;
  private SnakeMiniBossConfig config;
  private Entity player;
  private Entity snake;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent snakeStats;
  private StatusEffectsControllerComponent playerEffects;
  private StatusEffectsControllerComponent snakeEffects;
  private SnakePoisonVolleyComponent poison;
  private SnakePlayerHitVisualComponent impact;
  private SpriteBatch batch;
  private World world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    resources = new ResourceService();
    resources.loadTextures(SnakePoisonAssets.paths());
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    config = new SnakeMiniBossConfig();
    playerStats = new CombatStatsComponent(100, 1);
    playerEffects = new StatusEffectsControllerComponent();
    player = new Entity().addComponent(playerStats).addComponent(playerEffects);
    player.setPosition(8f, 0f);
    player.create();
    snakeStats = new CombatStatsComponent(150, 1);
    snakeEffects = new StatusEffectsControllerComponent();
    impact = new SnakePlayerHitVisualComponent(player);
    poison = new SnakePoisonVolleyComponent(player, config);
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(snakeEffects)
            .addComponent(impact)
            .addComponent(poison);
    snake.create();
    configureBatch();
  }

  @AfterEach
  void cleanUp() {
    resources.dispose();
    if (world != null) {
      world.dispose();
    }
  }

  @Test
  void emitsFourOffsetFansUsingOneLockedAimAndIgnoresDuplicateWaves() {
    poison.beginSpit();
    player.setPosition(0f, 8f);
    for (int wave = 0; wave < 4; wave++) {
      poison.fireVolley(wave);
      poison.fireVolley(wave);
      assertEquals((wave + 1) * 4, poison.getProjectileCount());
    }
    poison.fireVolley(4);
    poison.endSpit();
    render();
    assertEquals(16, draws.size());
    for (int wave = 0; wave < 4; wave++) {
      for (int shot = 0; shot < 4; shot++) {
        float expected = (324f + wave * 6f + shot * 18f) % 360f;
        assertEquals(expected, draws.get(wave * 4 + shot).angle(), 0.001f);
      }
    }
  }

  @Test
  void fliesStraightWithoutRetargetingOrFollowingTheSnake() {
    fireOne();
    render();
    Vector2 start = draws.getFirst().centre();
    player.setPosition(0f, 20f);
    snake.setPosition(30f, 30f);
    primeFlight();
    tick(0.5f);
    render();
    assertEquals(start.x + 1.5f, draws.getFirst().centre().x, 0.0001f);
    assertEquals(start.y, draws.getFirst().centre().y, 0.0001f);
    assertEquals(0f, draws.getFirst().angle());
  }

  @Test
  void newlyStartedChargeAndShotsDoNotConsumeTheControllersElapsedFrame() {
    oneShotConfig();
    poison.beginSpit();
    tick(1.5f);
    render();
    assertEquals(0, draws.getFirst().region().getRegionX());
    assertEquals(0, draws.getFirst().region().getRegionY());
    poison.fireVolley(0);
    poison.endSpit();
    tick(1.5f);
    render();
    assertTrue(draws.getFirst().centre().epsilonEquals(0.95f, 0.5f, 0.000001f));
    assertEquals(0, draws.getFirst().region().getRegionX());
    assertEquals(100, playerStats.getHealth());
    tick(0.19f);
    render();
    assertEquals(64, draws.getFirst().region().getRegionX());
    assertEquals(128, draws.getFirst().region().getRegionY());
    assertTrue(draws.getFirst().centre().epsilonEquals(0.95f, 0.5f, 0.000001f));
  }

  @Test
  void originalSheetsProvideFormationFlightAndFiniteImpactFrames() {
    player.setPosition(2f, 0f);
    fireOne();
    primeFlight();
    render();
    assertSame(texture(1), draws.getFirst().region().getTexture());
    tick(0.08f);
    render();
    assertEquals(64, draws.getFirst().region().getRegionX());
    tick(0.4f);
    render();
    assertSame(texture(2), draws.getFirst().region().getTexture());
    assertEquals(0, draws.getFirst().region().getRegionX());
    tick(0.27f);
    render();
    assertEquals(64, draws.getFirst().region().getRegionX());
    assertEquals(128, draws.getFirst().region().getRegionY());
    tick(0.02f);
    assertEquals(0, poison.getProjectileCount());
  }

  @Test
  void aSweptHitDealsOneLowDirectDamageAndDoesNotKeepDamagingDuringFade() {
    player.setPosition(2f, 0f);
    fireOne();
    primeFlight();
    tick(1f);
    assertEquals(99, playerStats.getHealth());
    assertTrue(impact.isPlaying());
    tick(1f);
    tick(10f);
    assertEquals(99, playerStats.getHealth());
    assertEquals(0, poison.getProjectileCount());
  }

  @Test
  void nearestWallProtectsThePlayerButAPlayerBeforeTheWallIsHit() {
    createWall(new Rectangle(2f, -2f, 0.05f, 5f));
    player.setPosition(4f, 0f);
    fireOne();
    primeFlight();
    tick(2f);
    assertEquals(100, playerStats.getHealth());
    assertFalse(impact.isPlaying());
    poison.clear();

    player.setPosition(1.2f, 0f);
    fireOne();
    primeFlight();
    tick(2f);
    assertEquals(99, playerStats.getHealth());
    assertTrue(impact.isPlaying());
  }

  @Test
  void willNotCreateShotsInsideOrAcrossAWallAtTheMouth() {
    createWall(new Rectangle(0.7f, -1f, 0.05f, 3f));
    fireOne();
    assertEquals(0, poison.getProjectileCount());
    snake.setPosition(0.2f, 0f);
    fireOne();
    assertEquals(0, poison.getProjectileCount());
  }

  @Test
  void immunityAndConcealmentDoNotTriggerGreenHitFeedback() {
    player.setPosition(2f, 0f);
    playerStats.setInvulnerable(true);
    fireOne();
    primeFlight();
    tick(1f);
    assertEquals(100, playerStats.getHealth());
    assertFalse(impact.isPlaying());
    poison.clear();
    playerStats.setInvulnerable(false);
    fireOne();
    playerEffects.addStatusEffect(new InvisibilityEffect(time, 10000L));
    primeFlight();
    tick(1f);
    assertEquals(100, playerStats.getHealth());
    assertFalse(impact.isPlaying());
    render();
    assertSame(texture(1), draws.getFirst().region().getTexture());
  }

  @Test
  void invalidPausedAndFrozenUpdatesPreserveProjectilePosition() {
    fireOne();
    primeFlight();
    render();
    Vector2 before = draws.getFirst().centre();
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      tick(delta);
    }
    FrozenEffect freeze = new FrozenEffect(time, 10000L);
    snakeEffects.addStatusEffect(freeze);
    tick(3f);
    render();
    assertEquals(before, draws.getFirst().centre());
    snakeEffects.removeStatusEffect(freeze);
    tick(0.1f);
    render();
    assertEquals(before.x + 0.3f, draws.getFirst().centre().x, 0.0001f);
  }

  @Test
  void shotsHaveBoundedLifetimeAndRepeatedAttacksHaveBoundedStorage() {
    player.setPosition(100f, 0f);
    fireOne();
    primeFlight();
    tick(5f);
    render();
    assertSame(texture(2), draws.getFirst().region().getTexture());
    tick(0.28f);
    assertEquals(0, poison.getProjectileCount());
    config.poisonShotsPerVolley = 4;
    config.poisonVolleyCount = 4;
    for (int attack = 0; attack < 20; attack++) {
      poison.beginSpit();
      for (int wave = 0; wave < 4; wave++) {
        poison.fireVolley(wave);
      }
    }
    assertEquals(128, poison.getProjectileCount());
  }

  @Test
  void drawsOriginalColoursOnceAndRestoresTheBatchEvenWithSnakeGlow() {
    snakeEffects.addStatusEffect(new FrozenEffect(time, 10000L));
    // An already-fired projectile keeps its artwork while the Snake is frozen.
    snakeEffects.clearStatusEffects();
    fireOne();
    snakeEffects.addStatusEffect(new FrozenEffect(time, 10000L));
    float originalColour = batchColour.toFloatBits();
    render();
    assertEquals(1, draws.size());
    assertEquals(Color.WHITE, draws.getFirst().colour());
    assertEquals(originalColour, batch.getPackedColor());
    assertEquals(Float.POSITIVE_INFINITY, poison.getZIndex());
  }

  @Test
  void lethalPlayerHitClearsOtherProjectilesDuringIteration() {
    player.setPosition(2f, 0f);
    playerStats.setHealth(1);
    config.poisonFanDegrees = 0f;
    config.poisonWaveOffsetDegrees = 0f;
    poison.beginSpit();
    poison.fireVolley(0);
    poison.endSpit();
    primeFlight();
    assertDoesNotThrow(() -> tick(1f));
    assertTrue(playerStats.isDead());
    assertEquals(0, poison.getProjectileCount());
    poison.beginSpit();
    poison.fireVolley(0);
    assertFalse(poison.isSpitting());
    assertEquals(0, poison.getProjectileCount());
  }

  @Test
  void snakeDeathDuringDamageCallbackSafelyCancelsRemainingShots() {
    player.setPosition(2f, 0f);
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer damage, Integer health) -> snakeStats.setHealth(0));
    config.poisonFanDegrees = 0f;
    config.poisonWaveOffsetDegrees = 0f;
    poison.beginSpit();
    poison.fireVolley(0);
    primeFlight();
    assertDoesNotThrow(() -> tick(1f));
    assertEquals(99, playerStats.getHealth());
    assertEquals(0, poison.getProjectileCount());
    assertFalse(poison.isSpitting());
  }

  @Test
  void disposalClearsChargeAndShotsWithoutUnloadingSharedTextures() {
    poison.beginSpit();
    poison.fireVolley(0);
    poison.dispose();
    poison.beginSpit();
    poison.fireVolley(0);
    render();
    assertEquals(0, poison.getProjectileCount());
    assertFalse(poison.isSpitting());
    assertTrue(draws.isEmpty());
    for (String path : SnakePoisonAssets.paths()) {
      assertTrue(resources.containsAsset(path, Texture.class));
    }
    verify(renderer).unregister(poison);
  }

  private void oneShotConfig() {
    config.poisonShotsPerVolley = 1;
    config.poisonVolleyCount = 1;
    config.poisonWaveOffsetDegrees = 0f;
  }

  private void fireOne() {
    oneShotConfig();
    poison.beginSpit();
    poison.fireVolley(0);
    poison.endSpit();
  }

  private void primeFlight() {
    tick(0.01f); // Emission frame must not advance the new shot.
    tick(0.2f); // Finish formation at its fixed mouth position.
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    poison.update();
  }

  private void render() {
    draws.clear();
    poison.render(batch);
  }

  private Texture texture(int index) {
    return resources.getAsset(SnakePoisonAssets.paths()[index], Texture.class);
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

  private void configureBatch() {
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenAnswer(ignored -> batchColour.toFloatBits());
    doAnswer(
            invocation -> {
              batchColour.set(
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
              Color.abgr8888ToColor(batchColour, (float) invocation.getArgument(0));
              return null;
            })
        .when(batch)
        .setPackedColor(anyFloat());
    doAnswer(
            invocation -> {
              float x = invocation.getArgument(1);
              float y = invocation.getArgument(2);
              float halfSize = config.poisonVisualSize / 2f;
              draws.add(
                  new Draw(
                      invocation.getArgument(0),
                      new Vector2(x + halfSize, y + halfSize),
                      invocation.getArgument(9),
                      new Color(batchColour)));
              return null;
            })
        .when(batch)
        .draw(
            any(TextureRegion.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());
  }

  private record Draw(TextureRegion region, Vector2 centre, float angle, Color colour) {}
}
