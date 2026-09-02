package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link ProjectileComponent}.
 *
 * <p>The "arrowStops..." tests replay the real game loop ({@code physicsEngine.update()} then
 * {@code entityService.update()}) with render deltas shorter than the fixed physics timestep.
 * Physics then advances the arrow further per frame than the render delta suggests, which is the
 * exact timing condition that previously let arrows tunnel through obstacles at certain ranges.
 */
@ExtendWith(GameExtension.class)
class ProjectileComponentTest {
  private static final float ARROW_SIZE = 0.25f;
  private static final float ARROW_SPEED = 5f; // matches BowWeaponComponent
  private static final float WALL_THICKNESS = 0.5f;
  private static final Vector2 ARROW_SPAWN = new Vector2(1f, 5f);
  private static final int MAX_FRAMES = 400;

  /**
   * Allowed overshoot of the arrow's centre past a wall's near face. Anything past the far face
   * means the arrow tunnelled clean through.
   */
  private static final float FACE_TOLERANCE = 0.01f;

  private PhysicsEngine physicsEngine;
  private EntityService entityService;
  private GameTime gameTime;

  @BeforeEach
  void beforeEach() {
    setUpWorld(0.1f);
    ServiceLocator.registerRenderService(new RenderService());
  }

  @Test
  void constructorRejectsNullDirection() {
    assertThrows(IllegalArgumentException.class, () -> new ProjectileComponent(null, 1f));
  }

  @Test
  void constructorRejectsZeroDirection() {
    Vector2 zeroDirection = new Vector2(0f, 0f);
    assertThrows(IllegalArgumentException.class, () -> new ProjectileComponent(zeroDirection, 1f));
  }

  @Test
  void constructorRejectsNonPositiveSpeed() {
    Vector2 direction = new Vector2(1f, 0f);
    assertThrows(IllegalArgumentException.class, () -> new ProjectileComponent(direction, 0f));
  }

  @Test
  void shouldTravelInGivenDirection() {
    Entity projectile = createProjectile(new Vector2(0f, 1f), 5f);

    Vector2 velocity =
        projectile.getComponent(PhysicsComponent.class).getBody().getLinearVelocity();
    assertEquals(0f, velocity.x, 1e-4f);
    assertEquals(5f, velocity.y, 1e-4f);
  }

  @Test
  void shouldDespawnOnEnemyHit() {
    Entity projectile = createProjectile(new Vector2(1f, 0f), 5f);
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC));
    enemy.create();

    projectile
        .getEvents()
        .trigger(
            "collisionStart",
            projectile.getComponent(HitboxComponent.class).getFixture(),
            enemy.getComponent(HitboxComponent.class).getFixture());

    verify(entityService).scheduleDisposal(projectile);
  }

  @Test
  void shouldDespawnWhenWallBlocksNextStep() {
    // Projectile centre (0.5, 0.5) steps to (1.0, 0.5); the wall box starts at x = 0.8.
    Entity projectile = createProjectile(new Vector2(1f, 0f), 5f);
    createWall(0.8f, 0f);

    projectile.update();

    verify(entityService).scheduleDisposal(projectile);
  }

  @Test
  void arrowKeepsFlyingWhenNothingBlocksIt() {
    setUpWorld(0.011f);
    Entity arrow = registerArrow();

    for (int i = 0; i < 100; i++) {
      physicsEngine.update();
      entityService.update();
    }

    assertFalse(isDisposed(arrow), "arrow with a clear path should not be despawned");
    assertTrue(
        arrow.getCenterPosition().x > ARROW_SPAWN.x + 1f, "arrow should have travelled forward");
  }

  /**
   * Fires arrows at walls placed at many ranges, at render rates faster than the physics timestep
   * (~90 and ~120 fps). The arrow must stop at the near face of every wall; sweeping the range
   * finely catches the timing-dependent tunnelling windows.
   */
  @Test
  void arrowStopsAtWallAcrossRangesAndFrameRates() {
    List<String> tunnelled = new ArrayList<>();
    for (float deltaTime : new float[] {0.011f, 0.008f}) {
      for (float range = 0.8f; range <= 3.8f; range += 0.05f) {
        float overshoot = fireArrowAtWall(range, deltaTime);
        if (overshoot > FACE_TOLERANCE) {
          tunnelled.add(
              String.format(
                  "range %.2f at dt %.3f overshot wall face by %.3f", range, deltaTime, overshoot));
        }
      }
    }
    assertTrue(
        tunnelled.isEmpty(), "arrows passed the wall face:\n" + String.join("\n", tunnelled));
  }

  /** A single frame hitch makes physics run several catch-up steps; the arrow must still stop. */
  @Test
  void arrowStopsAtWallAfterFrameHitch() {
    setUpWorld(0.011f);
    registerWall(1.5f);
    Entity arrow = registerArrow();

    // Three smooth frames, a 100ms hitch while the arrow nears the wall, then smooth frames again.
    float[] deltas = {0.011f, 0.011f, 0.011f, 0.1f, 0.011f, 0.011f};
    float maxCenterX = ARROW_SPAWN.x;
    for (float delta : deltas) {
      when(gameTime.getDeltaTime()).thenReturn(delta);
      physicsEngine.update();
      entityService.update();
      maxCenterX = Math.max(maxCenterX, arrow.getCenterPosition().x);
    }

    float wallNearFaceX = ARROW_SPAWN.x + 1.5f;
    assertTrue(
        maxCenterX <= wallNearFaceX + FACE_TOLERANCE,
        "arrow centre reached " + maxCenterX + ", past wall face at " + wallNearFaceX);
  }

  private static Entity createProjectile(Vector2 direction, float speed) {
    Entity projectile =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
            .addComponent(new ProjectileComponent(direction, speed));
    projectile.create();
    return projectile;
  }

  /** A static obstacle-layer collider at the given position, e.g. a wall segment. */
  private static Entity createWall(float x, float y) {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    wall.setPosition(x, y);
    wall.create();
    return wall;
  }

  /**
   * Runs the game loop until the arrow despawns or {@link #MAX_FRAMES} elapse.
   *
   * @param range distance from the arrow's spawn centre to the wall's near face
   * @param deltaTime render frame delta fed to both physics and component updates
   * @return how far the arrow's centre travelled past the wall's near face (0 or less means it
   *     stopped short, as it should)
   */
  private float fireArrowAtWall(float range, float deltaTime) {
    setUpWorld(deltaTime);
    registerWall(range);
    Entity arrow = registerArrow();

    float maxCenterX = ARROW_SPAWN.x;
    for (int i = 0; i < MAX_FRAMES && !isDisposed(arrow); i++) {
      physicsEngine.update();
      entityService.update();
      maxCenterX = Math.max(maxCenterX, arrow.getCenterPosition().x);
    }

    assertTrue(isDisposed(arrow), "arrow was never despawned at range " + range);
    return maxCenterX - (ARROW_SPAWN.x + range);
  }

  /** Registers fresh physics/entity services with a mocked, fixed render delta. */
  private void setUpWorld(float deltaTime) {
    gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(deltaTime);
    ServiceLocator.registerTimeSource(gameTime);

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
  }

  /** Places a wall whose near face sits {@code range} to the right of the arrow's spawn centre. */
  private void registerWall(float range) {
    Entity wall = ObstacleFactory.createWall(WALL_THICKNESS, 4f);
    wall.setPosition(ARROW_SPAWN.x + range, ARROW_SPAWN.y - 2f);
    entityService.register(wall);
  }

  /** An arrow like {@link BowWeaponComponent} fires, travelling in +x from {@link #ARROW_SPAWN}. */
  private Entity registerArrow() {
    Entity arrow =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.WEAPON))
            .addComponent(new ProjectileComponent(new Vector2(1f, 0f), ARROW_SPEED))
            .addComponent(new DisposalFlagComponent());
    arrow.setScale(ARROW_SIZE, ARROW_SIZE);
    arrow.setPosition(ARROW_SPAWN.x - ARROW_SIZE / 2f, ARROW_SPAWN.y - ARROW_SIZE / 2f);
    entityService.register(arrow);
    return arrow;
  }

  private static boolean isDisposed(Entity arrow) {
    return arrow.getComponent(DisposalFlagComponent.class).disposed;
  }

  /** Records whether its entity has been disposed, since {@link Entity} does not expose this. */
  private static class DisposalFlagComponent extends Component {
    private boolean disposed;

    @Override
    public void dispose() {
      disposed = true;
    }
  }
}
