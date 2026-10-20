package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.ai.movement.ObstacleNavigation;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ObstacleFactory;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class GroundChaseTaskTest {
  private GameTime time;
  private Entity enemy;
  private Entity target;
  private PhysicsMovementComponent movement;
  private GroundChaseTask chase;
  private long now;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    when(time.getDeltaTime()).thenReturn(0.016f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    target = new Entity();
    target.setPosition(2.6f, 0f);
    movement = new PhysicsMovementComponent();
    movement.setMaxSpeed(new Vector2(2.5f, 2.5f));
    chase = new GroundChaseTask(target, 10, 3f, 10f);
    enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(movement)
            .addComponent(new ColliderComponent());
    enemy
        .getComponent(ColliderComponent.class)
        .setAsBox(new Vector2(0.9f, 0.4f), new Vector2(0.5f, 0.2f));
    enemy.addComponent(new AITaskComponent().addTask(chase));
    enemy.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.getPhysicsService().getPhysics().dispose();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void resumesPursuitAfterLungeEvenWhenPlayerLeavesAcquisitionRange(boolean chasedFirst) {
    enemy
        .getComponent(AITaskComponent.class)
        .addTask(new WanderTask(new Vector2(2f, 2f), 1f))
        .addTask(new LungeAttackTask(target, 20, 2.5f, enemy));
    if (chasedFirst) {
      Entity barrier = wall(1.4f, -1f, 0.2f, 3f);
      tick();
      assertEquals(com.csse3200.game.ai.tasks.Task.Status.ACTIVE, chase.getStatus());
      barrier.dispose();
    }
    boolean[] dashEnded = {false};
    enemy
        .getEvents()
        .addListener(
            "lungeDashEnd",
            () -> {
              dashEnded[0] = true;
              target.setPosition(enemy.getPosition().cpy().add(4f, 0f));
            });
    for (int i = 0; i < 100 && !dashEnded[0]; i++) {
      tick();
    }
    assertTrue(dashEnded[0], "The real scheduler must complete a lunge before checking resumption");
    tick();
    assertEquals(
        com.csse3200.game.ai.tasks.Task.Status.ACTIVE,
        chase.getStatus(),
        "Switching to an attack must not make the enemy forget its acquired target");
    assertTrue(movement.getMoving());
    assertEquals(target.getPosition(), movement.getTarget());
  }

  @Test
  void forgottenTargetMustReenterAcquisitionRangeAfterEscaping() {
    assertEquals(10, chase.getPriority());
    chase.start();
    chase.stop();
    target.setPosition(11f, 0f);
    assertEquals(-1, chase.getPriority());
    target.setPosition(4f, 0f);
    assertEquals(-1, chase.getPriority());
    target.setPosition(2f, 0f);
    assertEquals(10, chase.getPriority());
  }

  @Test
  void concealmentClearsRememberedTargetWhilePursuitIsInterrupted() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    assertEquals(10, chase.getPriority());
    chase.start();
    chase.stop();
    when(effects.isConcealed()).thenReturn(true);
    assertEquals(-1, chase.getPriority());
    when(effects.isConcealed()).thenReturn(false);
    target.setPosition(4f, 0f);
    assertEquals(-1, chase.getPriority());
    target.setPosition(2f, 0f);
    assertEquals(10, chase.getPriority());
  }

  @ParameterizedTest
  @ValueSource(ints = {16, 33, 80})
  void pursuesAroundPillarsInActualDungeonAnnex(int frameMillis) {
    when(time.getDeltaTime()).thenReturn(frameMillis / 1000f);
    actualDungeonWalls();
    enemy.setPosition(29f, 3.5f);
    chase.start();
    for (Vector2 point :
        List.of(
            new Vector2(25.7f, 5f),
            new Vector2(26.1f, 7.4f),
            new Vector2(29f, 7.4f),
            new Vector2(26.1f, 1.6f),
            new Vector2(29f, 2f))) {
      target.setPosition(point);
      for (int i = 0; i < 600 && enemy.getPosition().dst(point) > 0.15f; i++) {
        now += frameMillis;
        enemy.earlyUpdate();
        enemy.update();
        ServiceLocator.getPhysicsService().getPhysics().update();
      }
      assertTrue(
          enemy.getPosition().dst(point) < 0.15f,
          "Stopped at "
              + enemy.getPosition()
              + " on way to "
              + point
              + "; steering "
              + movement.getTarget()
              + "; path "
              + ObstacleNavigation.findPath(enemy, target));
    }
  }

  private void actualDungeonWalls() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(512);
    when(texture.getHeight()).thenReturn(512);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());
    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.getTileSize()).thenReturn(0.5f);
    when(terrain.getMapBounds(0)).thenReturn(new GridPoint2(64, 56));
    when(terrain.tileToWorldPosition(any(GridPoint2.class)))
        .thenAnswer(
            invocation -> {
              GridPoint2 grid = invocation.getArgument(0);
              return new Vector2(grid.x * 0.5f, grid.y * 0.5f);
            });
    WorldConfig config = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    Entity room =
        new Entity()
            .addComponent(terrain)
            .addComponent(
                new ObstacleComponent(
                    config.getRoom("dungeonOneEntrance"), RoomFactory.DUNGEON1_TILESET));
    room.create();
  }

  @Test
  void approachesPlayerStandingCloserToWallThanEnemyCanFit() {
    wall(0f, -5f, 0.2f, 10f);
    enemy.setPosition(2.6f, 0f);
    target.setPosition(0.02f, 0f);
    for (int i = 0; i < 250; i++) {
      tick();
    }
    assertTrue(
        enemy.getPosition().dst(target.getPosition()) < 0.4f,
        "Must approach the player instead of rejecting the blocked endpoint: "
            + enemy.getPosition());
    assertTrue(enemy.getPosition().x >= 0.15f, "Must keep the enemy collider outside the wall");
  }

  @Test
  void walksAroundStationaryEnemyInsteadOfPushingIntoIt() {
    Entity blocker =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new ColliderComponent())
            .addComponent(new PhysicsMovementComponent());
    blocker
        .getComponent(ColliderComponent.class)
        .setAsBox(new Vector2(0.9f, 0.4f), new Vector2(0.5f, 0.2f));
    blocker.setPosition(1.2f, 0f);
    blocker.create();
    for (int i = 0; i < 400; i++) {
      tick();
    }
    assertTrue(
        enemy.getPosition().dst(target.getPosition()) < 0.15f,
        "Must route around another solid body: " + enemy.getPosition());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void fourEnemiesCanPassThroughSingleFileDoorwayWithoutBlockingEachOther(boolean lunges) {
    wall(-2.5f, -2.5f, 0.2f, 7f);
    wall(3f, -2.5f, 0.2f, 7f);
    wall(-2.5f, -2.5f, 5.7f, 0.2f);
    wall(-2.5f, 4.5f, 5.7f, 0.2f);
    wall(-4f, 1.4f, 4f, 0.2f);
    wall(1.1f, 1.4f, 4f, 0.2f);
    target.setPosition(0.05f, 3f);
    enemy.setPosition(-0.5f, -0.5f);
    List<Entity> crowd = new ArrayList<>();
    crowd.add(enemy);
    crowd.add(crowdEnemy(0.5f, -0.5f));
    crowd.add(crowdEnemy(-0.5f, -1.1f));
    crowd.add(crowdEnemy(0.5f, -1.1f));
    if (lunges) {
      target.addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody));
      target.addComponent(new ColliderComponent());
      target
          .getComponent(ColliderComponent.class)
          .setAsBox(new Vector2(0.6f, 0.3f), new Vector2(0.3f, 0.15f));
      target.create();
      for (Entity member : crowd) {
        member
            .getComponent(AITaskComponent.class)
            .addTask(new LungeAttackTask(target, 20, 2.5f, member));
      }
    }
    // They are already aware of the player before entering the choke point.
    chase.start();
    for (int i = 0; i < 1200; i++) {
      now += 16;
      for (Entity member : crowd) {
        member.earlyUpdate();
      }
      for (Entity member : crowd) {
        member.update();
      }
      ServiceLocator.getPhysicsService().getPhysics().update();
    }
    for (Entity member : crowd) {
      assertTrue(
          member.getPosition().y > 1.62f,
          "Every enemy should pass through the opening: " + member.getPosition());
    }
  }

  private Entity crowdEnemy(float x, float y) {
    PhysicsMovementComponent steering = new PhysicsMovementComponent();
    steering.setMaxSpeed(new Vector2(2.5f, 2.5f));
    GroundChaseTask task = new GroundChaseTask(target, 10, 10f, 10f);
    Entity member =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(steering);
    member
        .getComponent(ColliderComponent.class)
        .setAsBox(new Vector2(0.9f, 0.4f), new Vector2(0.5f, 0.2f));
    member.addComponent(new AITaskComponent().addTask(task));
    member.setPosition(x, y);
    member.create();
    return member;
  }

  @Test
  void physicallyRoundsWallAndReachesPlayerInsteadOfPushingAgainstIt() {
    wall(1.4f, -1f, 0.2f, 3f);
    boolean detoured = false;
    for (int i = 0; i < 600; i++) {
      tick();
      detoured |= enemy.getPosition().y < -1.4f || enemy.getPosition().y > 2f;
    }

    assertTrue(detoured, "Enemy must travel around an end of the wall");
    assertTrue(
        enemy.getPosition().dst(target.getPosition()) < 0.15f, enemy.getPosition().toString());
  }

  @Test
  void physicallyPassesThroughNarrowDoorwayAfterTurning() {
    enemy.setPosition(-1.5f, 0f);
    target.setPosition(1f, 0.7f);
    wall(0f, -3f, 0.2f, 3f);
    wall(0f, 0.6f, 0.2f, 3f);
    for (int i = 0; i < 600; i++) {
      tick();
    }
    assertTrue(
        enemy.getPosition().dst(target.getPosition()) < 0.15f, enemy.getPosition().toString());
  }

  @Test
  void waitsAtUnreachableBarrierAndReplansAfterItIsRemoved() {
    Entity barrier = wall(1.4f, -10f, 0.2f, 20f);
    for (int i = 0; i < 40; i++) {
      tick();
    }
    assertFalse(movement.getMoving());
    assertEquals(0f, enemy.getPosition().x, 0.01f);

    barrier.dispose();
    for (int i = 0; i < 150; i++) {
      tick();
    }
    assertTrue(enemy.getPosition().dst(target.getPosition()) < 0.15f);
  }

  @Test
  void changesRouteWhenPlayerMovesToOtherSideOfObstacle() {
    wall(1.4f, -1f, 0.2f, 3f);
    for (int i = 0; i < 25; i++) {
      tick();
    }
    target.setPosition(-1f, 1f);
    for (int i = 0; i < 200; i++) {
      tick();
    }
    assertTrue(enemy.getPosition().dst(target.getPosition()) < 0.15f);
  }

  @Test
  void keepsExistingAcquisitionAndChaseDistanceLimits() {
    target.setPosition(4f, 0f);
    assertEquals(-1, chase.getPriority());
    target.setPosition(2f, 0f);
    assertEquals(10, chase.getPriority());
    chase.start();
    target.setPosition(8f, 0f);
    assertEquals(10, chase.getPriority());
    target.setPosition(11f, 0f);
    assertEquals(-1, chase.getPriority());
    chase.update();
    assertFalse(movement.getMoving());
  }

  @Test
  void cancelsMovementWhenPlayerBecomesConcealed() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    tick();
    assertTrue(movement.getMoving());

    when(effects.isConcealed()).thenReturn(true);
    tick();
    assertFalse(movement.getMoving());
    assertEquals(-1, chase.getPriority());

    when(effects.isConcealed()).thenReturn(false);
    tick();
    assertTrue(movement.getMoving());
  }

  @Test
  void schedulerLetsEnemyRoundWallBeforeLungingAtPlayer() {
    wall(1.4f, -1f, 0.2f, 3f);
    enemy.getComponent(AITaskComponent.class).addTask(new LungeAttackTask(target, 20, 2.5f, enemy));
    int[] dashCount = {0};
    enemy
        .getEvents()
        .addListener(
            "lungeDashStart",
            () -> {
              dashCount[0]++;
              assertTrue(ObstacleNavigation.isClear(enemy, target.getPosition()));
            });
    float closest = Float.POSITIVE_INFINITY;
    for (int i = 0; i < 600; i++) {
      tick();
      closest = Math.min(closest, enemy.getPosition().dst(target.getPosition()));
    }
    assertTrue(dashCount[0] > 0, "Lunging must still work after rounding the wall");
    assertTrue(closest < 0.3f, "Enemy never reached the player: " + closest);
  }

  @Test
  void anotherEnemyEnteringTelegraphPathCancelsLunge() {
    Entity blocker = crowdEnemy(1.2f, 0f);
    LungeAttackTask lunge = new LungeAttackTask(target, 20, 2.5f, enemy);
    lunge.create(() -> enemy);
    assertEquals(-1, lunge.getPriority());

    blocker.setPosition(1.2f, 2f);
    assertEquals(20, lunge.getPriority());
    lunge.start();
    blocker.setPosition(1.2f, 0f);
    now = 600;
    lunge.update();

    assertEquals(-1, lunge.getPriority());
    assertFalse(movement.getMoving());
  }

  @Test
  void blockedLungeYieldsToGroundPursuitAndRechecksAfterTelegraph() {
    Entity barrier = wall(1.4f, -1f, 0.2f, 3f);
    LungeAttackTask lunge = new LungeAttackTask(target, 20, 2.5f, enemy);
    lunge.create(() -> enemy);
    assertEquals(-1, lunge.getPriority());
    assertEquals(10, chase.getPriority());

    barrier.dispose();
    assertEquals(20, lunge.getPriority());
    lunge.start();
    wall(1.4f, -1f, 0.2f, 3f);
    now = 600;
    lunge.update();
    assertEquals(-1, lunge.getPriority());
    assertFalse(movement.getMoving());
  }

  private void tick() {
    now += 16;
    enemy.earlyUpdate();
    enemy.update();
    ServiceLocator.getPhysicsService().getPhysics().update();
  }

  private Entity wall(float x, float y, float width, float height) {
    Entity wall = ObstacleFactory.createWall(width, height);
    wall.setPosition(x, y);
    wall.create();
    return wall;
  }
}
