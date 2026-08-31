package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileComponentTest {
  private static final String HOLE_TEXTURE = "images/hole.png";

  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(new RenderService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(gameTime);
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

  /** Registers a mock hole texture and returns it, so hole entities can be recognised. */
  private static Texture registerHoleTexture() {
    Texture holeTexture = mock(Texture.class);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.containsAsset(HOLE_TEXTURE, Texture.class)).thenReturn(true);
    when(resourceService.getAsset(HOLE_TEXTURE, Texture.class)).thenReturn(holeTexture);
    ServiceLocator.registerResourceService(resourceService);
    return holeTexture;
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
  void shouldFlyOverHole() {
    Texture holeTexture = registerHoleTexture();
    Entity hole =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE))
            .addComponent(new TextureRenderComponent(holeTexture));
    hole.setPosition(0.8f, 0f);
    hole.create();

    Entity projectile = createProjectile(new Vector2(1f, 0f), 5f);
    projectile.update();

    verify(entityService, never()).scheduleDisposal(projectile);
  }
}
