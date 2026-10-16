package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.boss.FinalBossMovementComponent;
import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.boss.FinalBossPhaseControllerComponent;
import com.csse3200.game.components.boss.FinalBossStageThreeComponent;
import com.csse3200.game.components.boss.FinalBossStageTwoArenaComponent;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.miniboss.cerberus.CerberusDeathComponent;
import com.csse3200.game.components.miniboss.snake.SnakeBurrowComponent;
import com.csse3200.game.components.miniboss.snake.SnakePoisonVolleyComponent;
import com.csse3200.game.components.miniboss.snake.SnakeShieldPickupComponent;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.*;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spawns configured enemies and tracks when the room has been cleared. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private static final Logger logger = LoggerFactory.getLogger(EnemyManagerComponent.class);
  private final EnemySpawnConfig[] spawnConfigs;
  private final Set<Entity> activeEnemies = new HashSet<>();
  private final ItemFactory itemFactory;
  private boolean disposed;
  private CameraComponent camera;
  private boolean notified = false;
  private RoomConfig roomConfig;

  /** Creates an empty manager for tests and rooms with no enemies. */
  public EnemyManagerComponent() {
    this(new EnemySpawnConfig[0]);
  }

  /** Uses the owning room's layout for configured enemies and split children. */
  public EnemyManagerComponent(
      EnemySpawnConfig[] spawnConfigs, RoomConfig roomConfig, CameraComponent camera) {
    this(spawnConfigs);
    this.roomConfig = Objects.requireNonNull(roomConfig, "roomConfig cannot be null");
    this.camera = camera;
  }

  public EnemyManagerComponent(EnemySpawnConfig[] spawnConfigs) {
    this(spawnConfigs, new ItemFactory());
  }

  /** Uses an injectable item factory without a room layout for isolated lifecycle tests. */
  EnemyManagerComponent(EnemySpawnConfig[] spawnConfigs, ItemFactory itemFactory) {
    this.spawnConfigs = spawnConfigs;
    this.itemFactory = Objects.requireNonNull(itemFactory);
  }

  /** returns true if an enemy is being spawned beyond a wall */
  protected boolean outOfBounds(Vector2 entityPos, RoomConfig room) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    List<String> spawnConfig = room.obstacles.spawns; // obstacles in current room

    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);
      for (int x = 0; x < row.length(); x++) {
        Vector2 worldPos = terrain.tileToWorldPosition(new GridPoint2(x, y));
        if ((row.charAt(x) == ' ' || row.charAt(x) == '#' || row.charAt(x) == '%')
            && entityPos.dst(worldPos) < terrain.getTileSize()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void update() {
    super.update();
    if (activeEnemies.isEmpty() && !notified) {
      var config = entity.getComponent(RoomDataComponent.class).getConfig();
      logger.info("Room Cleared");

      entity.getEvents().trigger(RoomClearService.ROOM_CLEARED, config.id);

      // Not the ideal way to know if it is end of a dungeon but works for now.
      // Should be changed later or enforce the naming convention
      if (config.id.endsWith("End")) {
        entity.getEvents().trigger(RoomClearService.DUNGEON_CLEARED, config.dungeonId);
        entity.getEvents().trigger(GameAreaDisplay.DISPLAY, "Dungeon Cleared!");
      } else {
        entity.getEvents().trigger(GameAreaDisplay.DISPLAY, "Room Cleared!");
      }

      notified = true;
    }
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  /**
   * Spawns each enemy declared by the room. After spawning scales enemies based on no. of cleared
   * dungeons.
   */
  public void spawnEnemies(Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);

    if (spawnConfigs.length == 0) {
      notified = true; // Don't notify if empty
      return;
    }

    for (EnemySpawnConfig spawn : spawnConfigs) {
      Vector2 entityPos = terrain.tileToWorldPosition(new GridPoint2(spawn.x, spawn.y));
      if (outOfBounds(entityPos, roomConfig)) {
        logger.warn("Attempt to spawn out of bounds entity at {}", entityPos);
        continue;
      }
      Entity enemy = createEnemy(spawn, target);
      track(enemy, spawn.type.name());
      enemy.addComponent(new EnemyTypeComponent(spawn.type)); // before spawnEntityAt registers it
      spawnEntityAt(enemy, new GridPoint2(spawn.x, spawn.y), true, true);
    }

    scale(RoomClearService.getDungeonClearedCount());
  }

  private Entity createEnemy(EnemySpawnConfig spawn, Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    Vector2 spawnPoints = terrain.tileToWorldPosition(spawn.x, spawn.y);

    switch (spawn.type) {
      // Chinese
      case JINGWEI:
        return NPCFactory.createJingwei(target, spawnPoints);
      case ZOMBIE:
        return NPCFactory.createZombie(target);
      case WUKONG:
        return NPCFactory.createWukong(target);
      case BUG:
        return NPCFactory.createBug(target);
      case LONGWEI:
        return NPCFactory.createLongwei(target);
      case DRAGON:
        return DragonFactory.createDragon(target, this::spawnEntity);

      // Norse
      case WOLF:
        return NPCFactory.createWolf(target);
      case CROW:
        return NPCFactory.createCrow(target, spawnPoints);
      case KNIGHT:
        return NPCFactory.createKnight(target);
      case DARK_ELVES:
        return NPCFactory.createDarkElves(target);
      case JOTUNN:
        return NPCFactory.createJotunn(target);
      case SLEIPNIR:
        return NPCFactory.createSleipnir(target, spawnPoints);

      // Egyptian
      case SANDEYE:
        return NPCFactory.createSandeye(target);
      case BEETLE:
        return NPCFactory.createBeetle(target);
      case CRAB:
        return NPCFactory.createCrab(target);
      case WASP:
        return NPCFactory.createWasp(target, spawnPoints);
      case MUMMY:
        return NPCFactory.createMummy(target);
      case SNAKE_MINI_BOSS:
        return createSnake(target);

      // Greek
      case MINOTAUR:
        return NPCFactory.createMinotaur(target);
      case MEDUSA:
        return NPCFactory.createMedusa(target);
      case HARPY:
        return NPCFactory.createHarpy(target, spawnPoints);
      case CYCLOPS:
        return NPCFactory.createCyclops(target);
      case GOLEM:
        return NPCFactory.createGolem(target);
      case CERBERUS:
        return createCerberus(spawn, target);

      case FINAL_BOSS:
        return createFinalBoss(target);
      default:
        throw new IllegalArgumentException("Unsupported enemy type: " + spawn.type);
    }
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  private void spawnAndTrackCerberusHead(Entity head, String enemyType) {
    track(head, enemyType);
    spawnEntity(head);
  }

  /** Tracks an enemy and any children it spawns. Package-private for testing. */
  void track(Entity enemy) {
    track(enemy, null);
  }

  void track(Entity enemy, String enemyType) {
    if (disposed || !activeEnemies.add(enemy)) {
      return;
    }
    enemy
        .getEvents()
        .<Entity>addListener(
            NPCFactory.PROJECTILE_SPAWNED,
            projectile -> {
              if (!disposed) {
                spawnEntity(projectile);
              }
            });
    enemy.getEvents().<Entity>addListener("cerberusProjectileSpawned", this::spawnEntity);
    enemy.getEvents().addListener("entityDied", () -> onEnemyDefeated(enemy, enemyType));
    if (enemy.getComponent(CerberusDeathComponent.class) != null) {
      enemy
          .getEvents()
          .addListener(CerberusDeathComponent.DEFEATED, () -> reportAchievementKill(enemy));
    }
    enemy
        .getEvents()
        .addListener("finalBossEncounterCompleted", () -> onEnemyDefeated(enemy, enemyType));
    enemy
        .getEvents()
        .addListener("spawnChildren", (Entity child) -> replaceWithChild(enemy, child, enemyType));
  }

  private void onEnemyDefeated(Entity enemy, String enemyType) {
    if (disposed || !activeEnemies.remove(enemy)) {
      return;
    }

    EnemyTypeComponent typeComponent = enemy.getComponent(EnemyTypeComponent.class);
    if (typeComponent != null
        && ServiceLocator.getAchievementService() != null
        && typeComponent.getType() != EnemySpawnConfig.EnemyType.CERBERUS) {
      AchievementContext ctx = new AchievementContext();
      ctx.enemyKilled = typeComponent.getType();
      ServiceLocator.getAchievementService().update(ctx);
    }

    // Capture before deferred disposal or room changes can move/remove the enemy.
    Vector2 position = enemy.getCenterPosition();
    spawnEnemyDrops(enemyType, position);
  }

  /** Reports a confirmed enemy defeat to the existing achievement system. */
  private void reportAchievementKill(Entity enemy) {
    if (disposed) {
      return;
    }

    EnemyTypeComponent typeComponent = enemy.getComponent(EnemyTypeComponent.class);
    if (typeComponent == null || ServiceLocator.getAchievementService() == null) {
      return;
    }

    AchievementContext context = new AchievementContext();
    context.enemyKilled = typeComponent.getType();
    ServiceLocator.getAchievementService().update(context);
  }

  /**
   * find nearby avaliable tile that is NOT out of bounds in positive/ negative x & y plane
   *
   * @param unspawnedEnemy the enemy to be placed
   * @return the avaliable tileset
   */
  private Vector2 findClosestAvaliableTile(Entity unspawnedEnemy, float tileSize) {
    if (tileSize >= 2) {
      return null;
    }
    Vector2 right =
        new Vector2(unspawnedEnemy.getPosition().x + tileSize, unspawnedEnemy.getPosition().y);
    Vector2 left =
        new Vector2(unspawnedEnemy.getPosition().x - tileSize, unspawnedEnemy.getPosition().y);
    Vector2 upRight =
        new Vector2(
            unspawnedEnemy.getPosition().x + tileSize, unspawnedEnemy.getPosition().y + tileSize);
    Vector2 upLeft =
        new Vector2(
            unspawnedEnemy.getPosition().x - tileSize, unspawnedEnemy.getPosition().y + tileSize);
    Vector2 downLeft =
        new Vector2(
            unspawnedEnemy.getPosition().x - tileSize, unspawnedEnemy.getPosition().y - tileSize);
    Vector2 downRight =
        new Vector2(
            unspawnedEnemy.getPosition().x + tileSize, unspawnedEnemy.getPosition().y - tileSize);
    Vector2 up =
        new Vector2(unspawnedEnemy.getPosition().x, unspawnedEnemy.getPosition().y + tileSize);
    Vector2 down =
        new Vector2(unspawnedEnemy.getPosition().x, unspawnedEnemy.getPosition().y - tileSize);

    // move x plane last (in case on top of other enemy)
    Vector2[] nearby = {up, down, upRight, upLeft, downLeft, downRight, left, right};

    for (Vector2 each : nearby) {
      if (!outOfBounds(each, roomConfig)) {
        return each;
      }
    }
    return findClosestAvaliableTile(unspawnedEnemy, tileSize + tileSize);
  }

  private void replaceWithChild(Entity parent, Entity child, String enemyType) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    float tileSize = terrain.getTileSize();
    activeEnemies.remove(parent);

    if (outOfBounds(child.getPosition(), roomConfig)) {
      logger.warn("Attempt to spawn child out of bounds entity at {}", child.getPosition());

      Vector2 newPos;
      if ((newPos = findClosestAvaliableTile(child, tileSize)) != null) {
        child.setPosition(newPos);
        track(child, enemyType);
        spawnEntity(child);
        logger.warn("Successful re-spawn child entity at {}", child.getPosition());
      }
      return;
    }
    track(child, enemyType);
    spawnEntity(child);
  }

  /** Creates and registers room-owned items after the current update is safe for spawning. */
  public void spawnItem(String itemId, int quantity, Vector2 position) {
    if (disposed) {
      return;
    }
    Objects.requireNonNull(itemId, "itemId cannot be null");
    Vector2 spawnPosition = Objects.requireNonNull(position, "position cannot be null").cpy();
    var items = ItemCatalog.createItems(itemId, quantity);
    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (disposed) {
                return;
              }
              for (Item item : items) {
                spawnEntity(ItemFactory.createItem(item, spawnPosition));
              }
            });
  }

  /** Requests enemy-specific items from the factory and registers them with this room. */
  private void spawnEnemyDrops(String enemyType, Vector2 position) {
    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (disposed) {
                return;
              }
              for (Entity item : itemFactory.createEnemyDrops(enemyType, position)) {
                spawnEntity(item);
              }
            });
  }

  /** Connects the snake's abilities to this room's bounds and camera. */
  private Entity createSnake(Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    Vector2 roomSize = terrain.tileToWorldPosition(terrain.getMapBounds(0));

    Entity snake = NPCFactory.createSnakeMiniBoss(target);
    snake
        .getComponent(SnakeBurrowComponent.class)
        .setArenaBounds(new Rectangle(0f, 0f, roomSize.x, roomSize.y));
    snake
        .getComponent(SnakePoisonVolleyComponent.class)
        .setArenaBounds(new Rectangle(0f, 0f, roomSize.x, roomSize.y));
    snake
        .getComponent(SnakeShieldPickupComponent.class)
        .setArenaBounds(new Rectangle(0f, 0f, roomSize.x, roomSize.y));

    if (camera != null) {
      snake.getComponent(SnakeShieldPickupComponent.class).setCamera(camera.getCamera());
    }
    return snake;
  }

  /** Creates Cerberus with the current room's movement and wall anchors. */
  private Entity createCerberus(EnemySpawnConfig spawn, Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    Vector2 anchorPoint = terrain.tileToWorldPosition(spawn.x, spawn.y);
    Vector2 wallAnchor = terrain.tileToWorldPosition(26, 50);
    wallAnchor.add(terrain.getTileSize() / 2f, 0f);

    return CerberusFactory.createCerberus(
        target,
        anchorPoint,
        wallAnchor,
        head -> spawnAndTrackCerberusHead(head, spawn.type.name()),
        "images/cerberus/cerberus-modular.atlas");
  }

  /** Connects Final Boss movement and arena behaviour to the room camera. */
  private Entity createFinalBoss(Entity target) {
    Entity boss = FinalBossFactory.createFinalBoss(target, this::spawnEntity);
    if (camera != null) {
      boss.getComponent(FinalBossMovementComponent.class).setCamera(camera.getCamera());
      boss.getComponent(FinalBossStageTwoArenaComponent.class)
          .setCamera(camera.getCamera(), entity.getComponent(FollowingCameraComponent.class));
    }
    return boss;
  }

  /** Returns whether the room has any living enemies. */
  public boolean isCleared() {
    return activeEnemies.isEmpty();
  }

  /** Defeats all living enemies for temporary transition testing. */
  public void clear() {
    for (Entity enemy : new ArrayList<>(activeEnemies)) {
      FinalBossStageThreeComponent stageThree =
          enemy.getComponent(FinalBossStageThreeComponent.class);
      if (stageThree != null) {
        stageThree.skipToEnding();
        continue;
      }
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats != null && !stats.isDead()) {
        stats.setHealth(0);
      }
    }
  }

  /** Initialises a freshly spawned Final Boss after room scaling and camera placement. */
  public boolean startFinalBossStage(FinalBossPhase stage) {
    if (disposed
        || (stage != FinalBossPhase.STAGE_ONE
            && stage != FinalBossPhase.STAGE_TWO
            && stage != FinalBossPhase.STAGE_THREE)) return false;
    boolean started = false;
    for (Entity enemy : new ArrayList<>(activeEnemies)) {
      FinalBossPhaseControllerComponent phases =
          enemy.getComponent(FinalBossPhaseControllerComponent.class);
      if (phases != null) started |= phases.startDebugStage(stage);
    }
    return started;
  }

  /**
   * Iterates through the active enemies and calls scale on their {@link CombatStatsComponent}
   *
   * <p>This method should be called during the room creatation.
   */
  public void scale(int mult) {
    for (Entity enemy : new ArrayList<>(activeEnemies)) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats != null) {
        stats.scale(mult);
      }
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    activeEnemies.clear();
    super.dispose();
  }
}
