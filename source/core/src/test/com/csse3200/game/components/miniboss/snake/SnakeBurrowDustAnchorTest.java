package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.SnakeMiniBossConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** Checks dust positions against the enlarged Snake's real bottom-aligned ground collider. */
@ExtendWith(GameExtension.class)
class SnakeBurrowDustAnchorTest {
  private static final float EPSILON = 0.0001f;
  private World world;
  private GameTime time;
  private SnakeMiniBossConfig config;
  private Entity snake;
  private Entity player;
  private CombatStatsComponent playerStats;
  private SnakeBurrowComponent burrow;
  private SnakeBurrowVisualComponent visual;

  @BeforeEach
  void createEnlargedSnake() {
    ServiceLocator.registerEntityService(new EntityService());
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    config = new SnakeMiniBossConfig();
    config.orbitRadius = 0f;

    playerStats = new CombatStatsComponent(100, 10);
    player = new Entity().addComponent(playerStats);
    player.setPosition(7.5f, 5.5f);
    player.create();

    visual = mock(SnakeBurrowVisualComponent.class);
    burrow = new SnakeBurrowComponent(player, config);
    burrow.setArenaBounds(new Rectangle(0f, 0f, 12f, 12f));
    snake =
        new Entity()
            .addComponent(new CombatStatsComponent(150, 10))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(visual)
            .addComponent(burrow);
    snake.setScale(3f, 3f);
    PhysicsUtils.setScaledCollider(snake, 0.9f, 0.4f);
    snake.setPosition(1.5f, 4.5f);
    snake.create();
    tick(0f);
  }

  @AfterEach
  void disposeEncounter() {
    snake.dispose();
    player.dispose();
    world.dispose();
  }

  @Test
  void initialBurrowDustStartsAtTheGroundCentre() {
    ArgumentCaptor<Vector2> centre = ArgumentCaptor.forClass(Vector2.class);

    verify(visual).startBurrow(centre.capture());

    assertGroundCentre(centre.getValue());
  }

  @Test
  void movingTrailFollowsTheGroundCentre() {
    tick(config.burrowDuration);
    tick(0.1f);
    ArgumentCaptor<Vector2> centre = ArgumentCaptor.forClass(Vector2.class);

    verify(visual).addTrail(centre.capture());

    assertEquals(SnakeBurrowComponent.State.UNDERGROUND, burrow.getState());
    assertGroundCentre(centre.getValue());
  }

  @Test
  void emergenceDustUsesTheGroundCentreWhileWarningAndDamageKeepTheirAttackCentre() {
    tick(config.burrowDuration);
    tick(config.undergroundDuration);
    ArgumentCaptor<Vector2> warning = ArgumentCaptor.forClass(Vector2.class);
    verify(visual).showWarning(warning.capture(), eq(config.burrowAttackRadius));
    assertEquals(player.getCenterPosition(), warning.getValue());
    assertEquals(100, playerStats.getHealth());

    tick(config.warningDuration);
    ArgumentCaptor<Vector2> dust = ArgumentCaptor.forClass(Vector2.class);
    verify(visual).startEmergence(dust.capture());

    assertEquals(SnakeBurrowComponent.State.EXPOSED, burrow.getState());
    assertGroundCentre(dust.getValue());
    assertEquals(warning.getValue(), burrow.getWarningCentre());
    assertEquals(warning.getValue(), snake.getCenterPosition());
    assertEquals(100 - config.burrowDamage, playerStats.getHealth());
  }

  private void assertGroundCentre(Vector2 centre) {
    // The 3 x 3 sprite has a 2.7 x 1.2 collider centred horizontally at its feet.
    assertEquals(snake.getPosition().x + 1.5f, centre.x, EPSILON);
    assertEquals(snake.getPosition().y + 0.6f, centre.y, EPSILON);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    burrow.update();
  }
}
