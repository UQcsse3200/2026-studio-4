package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.rooms.ObstacleComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeBurrowRoomTraversalTest {
  private static final float TILE_SIZE = 0.5f;
  private static final Vector2 GROUND_HALF_SIZE = new Vector2(0.9f, 0.4f);
  private static final Vector2 GROUND_OFFSET = new Vector2(0f, -0.6f);
  private PhysicsService physics;
  private ResourceService resources;
  private Entity room;
  private Entity snake;
  private Entity player;
  private GameTime time;
  private RoomConfig config;
  private SnakeBurrowArena arena;

  @BeforeEach
  void createActualSnakeRoomWalls() {
    physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerWorldCamera(new OrthographicCamera(20f, 16f));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    resources = spy(new ResourceService());
    resources.loadTextureAtlases(new String[] {"images/snake.atlas"});
    resources.loadTextures(SnakePoisonAssets.paths());
    resources.loadTextures(
        new String[] {
          SnakePlayerHitVisualComponent.HIT_SHEET,
          SnakeShieldComponent.SHIELD_TEXTURE,
          SnakeShieldPickupComponent.GEM_TEXTURE,
          SnakeShieldPickupComponent.SPAWN_TEXTURE
        });
    resources.loadAll();
    Texture tileset = mock(Texture.class);
    when(tileset.getWidth()).thenReturn(512);
    when(tileset.getHeight()).thenReturn(512);
    doReturn(tileset).when(resources).getAsset(RoomFactory.DUNGEON2_TILESET, Texture.class);
    ServiceLocator.registerResourceService(resources);

    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    config = world.getRoom("dungeonTwoEnd");
    TiledMap map = new TiledMap();
    map.getLayers().add(new TiledMapTileLayer(config.mapWidth, config.mapHeight, 16, 16));
    TerrainComponent terrain =
        new TerrainComponent(
            new OrthographicCamera(),
            map,
            mock(TiledMapRenderer.class),
            TerrainOrientation.ORTHOGONAL,
            TILE_SIZE);
    room =
        new Entity()
            .addComponent(terrain)
            .addComponent(new ObstacleComponent(config, RoomFactory.DUNGEON2_TILESET));
    room.create();
    arena =
        new SnakeBurrowArena(
            new Rectangle(0f, 0f, config.mapWidth * TILE_SIZE, config.mapHeight * TILE_SIZE));
  }

  @AfterEach
  void disposeRoom() {
    if (snake != null) {
      snake.dispose();
    }
    if (player != null) {
      player.dispose();
    }
    if (room != null) {
      room.dispose();
    }
    if (resources != null) {
      resources.dispose();
    }
    if (physics != null) {
      physics.getPhysics().getWorld().dispose();
    }
  }

  @Test
  void centralOpeningAllowsTravelInBothDirectionsAcrossEveryWallBand() {
    Vector2 lower = new Vector2(13.25f, 1.4f);
    Vector2 upper = new Vector2(13.25f, 23.9f);

    assertGroundRoute(lower, upper);
    assertGroundRoute(upper, lower);
  }

  @Test
  void everyHorizontalLaneFitsSnakeAndConnectsBothSidesOfTheRoom() {
    // Include the outermost lanes: top-face wall art extends below its configured row.
    for (float y : new float[] {1.4f, 3.5f, 8.75f, 11f, 16.75f, 18f, 23.9f}) {
      Vector2 left = new Vector2(2.5f, y);
      Vector2 right = new Vector2(27.5f, y);
      assertGroundRoute(left, right);
      assertGroundRoute(right, left);
    }
  }

  @Test
  void configuredSpawnConnectsToBothLanesAndBothExitApproaches() {
    Vector2 start = configuredSpawn().add(GROUND_OFFSET);
    Vector2 centralLane = new Vector2(13.25f, 16.4f);
    Vector2 leftLane = new Vector2(3f, 16.4f);
    Vector2 rightLane = new Vector2(27f, 16.4f);
    Vector2 doorApproach =
        new Vector2(
            config.getExit("returnDoor").x * TILE_SIZE + 1f,
            config.getExit("returnDoor").y * TILE_SIZE + 0.25f);
    Vector2 shelfApproach =
        new Vector2(
            config.getExit("returnShelf").x * TILE_SIZE + 0.25f,
            config.getExit("returnShelf").y * TILE_SIZE + 0.25f);

    Vector2 spawnLane = new Vector2(13.25f, start.y);
    assertGroundRoute(start, spawnLane);
    assertGroundRoute(spawnLane, centralLane);
    assertGroundRoute(centralLane, leftLane);
    assertGroundRoute(leftLane, doorApproach);
    assertGroundRoute(doorApproach, leftLane);
    assertGroundRoute(leftLane, rightLane);
    assertGroundRoute(rightLane, centralLane);
    Vector2 lowerCentre = new Vector2(13.25f, shelfApproach.y);
    assertGroundRoute(centralLane, lowerCentre);
    assertGroundRoute(lowerCentre, shelfApproach);
    assertGroundRoute(shelfApproach, lowerCentre);
    assertGroundRoute(lowerCentre, spawnLane);
    assertGroundRoute(spawnLane, start);
  }

  @Test
  void stageOneControllerFollowsPlayerAcrossTheOpeningAndBack() {
    assertControllerCrossesRoom(false);
  }

  @Test
  void stageTwoControllerFollowsPlayerAcrossTheOpeningAndBack() {
    assertControllerCrossesRoom(true);
  }

  @Test
  void stageOneControllerDetoursLeftAroundLongRightWallAndReturns() {
    assertControllerDetours(false, true);
  }

  @Test
  void stageTwoControllerDetoursLeftAroundLongRightWallAndReturns() {
    assertControllerDetours(true, true);
  }

  @Test
  void stageOneControllerDetoursRightAroundLongLeftWallAndReturns() {
    assertControllerDetours(false, false);
  }

  @Test
  void stageTwoControllerDetoursRightAroundLongLeftWallAndReturns() {
    assertControllerDetours(true, false);
  }

  @Test
  void controllerUpdatesItsDetourWhenPlayerMovesBehindTheWall() {
    SnakeBurrowComponent burrow = createControllerAt(new Vector2(20f, 17.6f), false);
    Vector2 originalTarget = new Vector2(20f, 10f);
    movePlayer(originalTarget);
    boolean startedDetour = false;
    for (int frame = 0; frame < 400; frame++) {
      advanceWithoutCrossingWalls(burrow);
      if (snake.getCenterPosition().x < 18f) {
        startedDetour = true;
        break;
      }
    }
    assertTrue(startedDetour, "Snake never moved away from the blocked direct approach");

    Vector2 movedTarget = new Vector2(24f, 10f);
    assertDetourReachesPlayer(burrow, movedTarget, true);
  }

  private void assertControllerDetours(boolean stageTwo, boolean rightWall) {
    Vector2 start = rightWall ? new Vector2(20f, 17.6f) : new Vector2(6f, 8.5f);
    Vector2 destination = rightWall ? new Vector2(20f, 10f) : new Vector2(6f, 3.5f);
    assertFalse(
        arena.isPathClear(
            start.cpy().add(GROUND_OFFSET), destination.cpy().add(GROUND_OFFSET), GROUND_HALF_SIZE),
        "The regression needs a wall blocking the direct approach");
    SnakeBurrowComponent burrow = createControllerAt(start, stageTwo);

    assertDetourReachesPlayer(burrow, destination, rightWall);
    assertDetourReachesPlayer(burrow, start, rightWall);
    assertEquals(stageTwo, burrow.isStageTwo());
  }

  private void assertDetourReachesPlayer(
      SnakeBurrowComponent burrow, Vector2 destination, boolean rightWall) {
    movePlayer(destination);
    boolean reachedOpening = false;
    boolean emergedNearPlayer = false;
    for (int frame = 0; frame < 1200; frame++) {
      advanceWithoutCrossingWalls(burrow);
      float x = snake.getCenterPosition().x;
      reachedOpening |= rightWall ? x < 14.3f : x > 12.2f;
      if (burrow.getState() == SnakeBurrowComponent.State.EXPOSED
          && snake.getCenterPosition().dst(destination) < 0.1f) {
        emergedNearPlayer = true;
        break;
      }
    }
    assertTrue(reachedOpening, "Snake did not route around the wall tip");
    assertTrue(
        emergedNearPlayer, "Snake did not reach player at " + destination + " within 60 seconds");
  }

  private void advanceWithoutCrossingWalls(SnakeBurrowComponent burrow) {
    Vector2 before = snake.getCenterPosition().add(GROUND_OFFSET);
    burrow.update();
    Vector2 after = snake.getCenterPosition().add(GROUND_OFFSET);
    assertTrue(
        arena.isPathClear(before, after, GROUND_HALF_SIZE),
        "Snake crossed a real room wall from " + before + " to " + after);
  }

  private void movePlayer(Vector2 centre) {
    player.setPosition(centre.cpy().mulAdd(player.getScale(), -0.5f));
  }

  private SnakeBurrowComponent createControllerAt(Vector2 start, boolean stageTwo) {
    player = new Entity().addComponent(new CombatStatsComponent(1000, 0));
    player.create();
    snake = NPCFactory.createSnakeMiniBoss(player);
    snake.setPosition(start.cpy().mulAdd(snake.getScale(), -0.5f));
    SnakeBurrowComponent burrow = snake.getComponent(SnakeBurrowComponent.class);
    burrow.setArenaBounds(
        new Rectangle(0f, 0f, config.mapWidth * TILE_SIZE, config.mapHeight * TILE_SIZE));
    snake.create();
    if (stageTwo) {
      CombatStatsComponent stats = snake.getComponent(CombatStatsComponent.class);
      stats.setHealth(stats.getMaxHealth() / 2);
    }
    when(time.getDeltaTime()).thenReturn(0.05f);
    return burrow;
  }

  private void assertControllerCrossesRoom(boolean stageTwo) {
    SnakeBurrowComponent burrow = createControllerAt(configuredSpawn(), stageTwo);
    for (Vector2 destination :
        new Vector2[] {new Vector2(8f, 17f), new Vector2(20f, 17f), new Vector2(8f, 17f)}) {
      player.setPosition(destination.cpy().mulAdd(player.getScale(), -0.5f));
      boolean emergedNearPlayer = false;
      for (int frame = 0; frame < 800; frame++) {
        burrow.update();
        Vector2 groundCentre = snake.getCenterPosition().add(GROUND_OFFSET);
        assertTrue(
            arena.isPathClear(groundCentre, groundCentre, GROUND_HALF_SIZE),
            "Snake overlapped a real room wall at " + groundCentre);
        if (burrow.getState() == SnakeBurrowComponent.State.EXPOSED
            && snake.getCenterPosition().dst(destination) < 0.1f) {
          emergedNearPlayer = true;
          break;
        }
      }
      assertTrue(emergedNearPlayer, "Snake did not reach player at " + destination);
      assertEquals(stageTwo, burrow.isStageTwo());
    }
  }

  private void assertGroundRoute(Vector2 from, Vector2 to) {
    assertTrue(
        arena.isPathClear(from, to, GROUND_HALF_SIZE), "Blocked route " + from + " to " + to);
    assertEquals(to, arena.findReachableCentre(from, to, GROUND_HALF_SIZE).orElseThrow());
    Vector2 current = from.cpy();
    for (int frame = 0; frame < 600 && current.dst2(to) > 0.000001f; frame++) {
      Vector2 desired = current.cpy().add(to.cpy().sub(current).limit(0.12f));
      Vector2 next = arena.constrainMovement(current, desired, GROUND_HALF_SIZE);
      assertTrue(next.dst2(to) < current.dst2(to), "Snake stopped at " + current);
      current = next;
    }
    assertTrue(current.epsilonEquals(to, 0.001f));
  }

  private Vector2 configuredSpawn() {
    for (EnemySpawnConfig spawn : config.enemySpawns) {
      if (spawn.type == EnemySpawnConfig.EnemyType.SNAKE_MINI_BOSS) {
        return new Vector2((spawn.x + 0.5f) * TILE_SIZE, (spawn.y + 0.5f) * TILE_SIZE);
      }
    }
    throw new IllegalStateException("Snake room has no Snake spawn");
  }
}
