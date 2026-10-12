package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises complete burrow movement against real static Box2D walls. */
@ExtendWith(GameExtension.class)
class SnakeBurrowWallCollisionTest {
  private static final Rectangle ROOM = new Rectangle(0f, 0f, 12f, 12f);
  private World world;
  private GameTime time;
  private SnakeMiniBossConfig config;
  private Entity snake;
  private Entity player;
  private CombatStatsComponent snakeStats;
  private CombatStatsComponent playerStats;
  private PhysicsComponent physics;
  private SnakeBurrowComponent burrow;

  @AfterEach
  void disposeWorld() {
    if (world != null) {
      world.dispose();
      world = null;
    }
  }

  @Test
  void undergroundMovementStopsBeforeTheWholeSnakeEntersAWall() {
    assertUndergroundMovementStaysBeforeWall(0.65f);
  }

  @Test
  void aLongUndergroundFrameCannotTunnelThroughAThinWall() {
    assertUndergroundMovementStaysBeforeWall(3f);
  }

  @Test
  void overlappingSpawnMovesTheWholeSnakeToClearFloorWithoutCrossingTheWall() {
    for (boolean stageTwo : new boolean[] {false, true}) {
      Rectangle wall = new Rectangle(3.75f, 0f, 0.25f, 12f);
      createEncounter(stageTwo, false, wall, 2f);

      assertTrue(snake.getPosition().x + snake.getScale().x <= wall.x);
      assertFalse(physics.getBody().isActive());
      moveCentre(player, new Vector2(1.5f, 6f));

      reachWarning();
      tick(config.warningDuration);

      assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
      assertTrue(snake.getPosition().x + snake.getScale().x <= wall.x);
      assertTrue(physics.getBody().isActive());
    }
  }

  @Test
  void warningSearchKeepsTheEmergenceOnTheReachableSideOfAWall() {
    for (boolean stageTwo : new boolean[] {false, true}) {
      createEncounter(stageTwo, false);
      createWall(new Rectangle(5.5f, 0f, 0.25f, 12f));
      moveCentre(player, new Vector2(6.3f, 6f));
      config.burrowSpeed = 0f;

      reachWarning();

      assertTrue(
          burrow.getWarningCentre().x + config.burrowAttackRadius <= 5.5f,
          "The warning must remain on the Snake's side of the wall");
      for (int frame = 0; frame < 4; frame++) {
        tick(config.warningDuration / 4f);
        assertTrue(snake.getPosition().x + snake.getScale().x <= 5.5f);
      }
      assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
      assertEquals(100, playerStats.getHealth());
    }
  }

  @Test
  void newlyBlockedWarningFootprintCancelsEmergenceAndDamage() {
    for (boolean stageTwo : new boolean[] {false, true}) {
      createEncounter(stageTwo, false);
      config.burrowSpeed = 0f;
      reachWarning();
      Vector2 warning = burrow.getWarningCentre();
      createWall(new Rectangle(warning.x - 0.1f, warning.y - 0.1f, 0.2f, 0.2f));

      tick(config.warningDuration);

      assertCancelledAttack();
      assertEquals(new Vector2(3f, 6f), snake.getCenterPosition());
    }
  }

  @Test
  void newlyBlockedWarningRouteCannotTeleportThroughTheWall() {
    for (boolean stageTwo : new boolean[] {false, true}) {
      createEncounter(stageTwo, false);
      config.burrowSpeed = 0f;
      reachWarning();
      createWall(new Rectangle(5.5f, 0f, 0.25f, 12f));

      tick(config.warningDuration);

      assertCancelledAttack();
      assertTrue(snake.getPosition().x + snake.getScale().x <= 5.5f);
    }
  }

  @Test
  void clearFloorPreservesWarningDamageAndStageSpecificRecovery() {
    for (boolean stageTwo : new boolean[] {false, true}) {
      createEncounter(stageTwo, false);
      reachWarning();
      assertEquals(player.getCenterPosition(), burrow.getWarningCentre());

      tick(config.warningDuration / 2f);

      assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
      assertEquals(100, playerStats.getHealth());
      assertFalse(physics.getBody().isActive());

      tick(config.warningDuration / 2f);

      assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
      assertEquals(100 - config.burrowDamage, playerStats.getHealth());
      assertFalse(snakeStats.isInvulnerable());
      assertTrue(physics.getBody().isActive());

      tick(config.exposedDuration);

      assertEquals(
          stageTwo ? SnakeBurrowComponent.State.SPITTING : SnakeBurrowComponent.State.BURROWING,
          burrow.getState());
    }
  }

  private void assertUndergroundMovementStaysBeforeWall(float delta) {
    for (boolean stageTwo : new boolean[] {false, true}) {
      for (boolean horizontal : new boolean[] {false, true}) {
        createEncounter(stageTwo, horizontal);
        Rectangle wall =
            horizontal ? new Rectangle(0f, 5.5f, 12f, 0.25f) : new Rectangle(5.5f, 0f, 0.25f, 12f);
        createWall(wall);
        tick(config.burrowDuration);

        tick(delta);

        Vector2 position = snake.getPosition();
        Vector2 size = snake.getScale();
        assertTrue(
            horizontal ? position.y + size.y <= wall.y : position.x + size.x <= wall.x,
            "Underground movement must stop before crossing or overlapping a wall");
        assertFalse(physics.getBody().isActive());
        assertEquals(100, playerStats.getHealth());
        assertEquals(stageTwo, burrow.isStageTwo());
      }
    }
  }

  private void assertCancelledAttack() {
    assertNotEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertNotEquals(SnakeBurrowComponent.State.SPITTING, burrow.getState());
    assertEquals(100, playerStats.getHealth());
    assertTrue(snakeStats.isInvulnerable());
    assertFalse(physics.getBody().isActive());
  }

  private void createEncounter(boolean stageTwo, boolean horizontal) {
    createEncounter(stageTwo, horizontal, null, 1f);
  }

  private void createEncounter(
      boolean stageTwo, boolean horizontal, Rectangle initialWall, float snakeSize) {
    disposeWorld();
    PhysicsService service = new PhysicsService();
    ServiceLocator.registerPhysicsService(service);
    world = service.getPhysics().getWorld();
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    config = new SnakeMiniBossConfig();
    config.orbitRadius = 0f;
    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats);
    moveCentre(player, horizontal ? new Vector2(6f, 8f) : new Vector2(8f, 6f));
    player.create();
    snakeStats = new CombatStatsComponent(150, 10);
    physics = new PhysicsComponent();
    burrow = new SnakeBurrowComponent(player, config);
    burrow.setArenaBounds(ROOM);
    snake =
        new Entity()
            .addComponent(snakeStats)
            .addComponent(physics)
            .addComponent(new ColliderComponent())
            .addComponent(burrow);
    snake.setScale(snakeSize, snakeSize);
    moveCentre(snake, horizontal ? new Vector2(6f, 3f) : new Vector2(3f, 6f));
    snake.create();
    if (stageTwo) {
      snakeStats.setHealth(75);
    }
    if (initialWall != null) {
      createWall(initialWall);
    }
    tick(0f);
  }

  private void createWall(Rectangle area) {
    BodyDef definition = new BodyDef();
    definition.type = BodyType.StaticBody;
    definition.position.set(area.x + area.width / 2f, area.y + area.height / 2f);
    Body wall = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    try {
      shape.setAsBox(area.width / 2f, area.height / 2f);
      wall.createFixture(shape, 0f);
    } finally {
      shape.dispose();
    }
  }

  private void reachWarning() {
    tick(config.burrowDuration);
    tick(config.undergroundDuration);
    assertEquals(SnakeBurrowComponent.State.WARNING, burrow.getState());
  }

  private void moveCentre(Entity entity, Vector2 centre) {
    entity.setPosition(centre.cpy().mulAdd(entity.getScale(), -0.5f));
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    burrow.update();
  }
}
