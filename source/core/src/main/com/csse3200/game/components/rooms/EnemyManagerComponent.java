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
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.HitboxComponent;
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

    scale(entity.getComponent(RoomDataComponent.class).getRoomClear().getDungeonClearedCount());
  }

  private Entity createEnemy(EnemySpawnConfig spawn, Entity target) {

    switch (spawn.type) {
      // Chinese
      case JINGWEI:
        return createFlyingEnemy(spawn, target, "images/jingwei.atlas");
      case ZOMBIE:
        return NPCFactory.createBombEnemy(target, "images/zombie.atlas", 2f);
      case WUKONG:
        return createScaledGiant(target, "images/wukong.atlas", 3f);
      case BUG:
        return NPCFactory.createChaseEnemy(target, true, "images/bug.atlas");
      case LONGWEI:
        return NPCFactory.createChaseEnemy(target, false, "images/longwei.atlas");
      case DRAGON:
        return DragonFactory.createDragon(target, this::spawnEntity);

      // Norse
      case WOLF:
        return NPCFactory.createChaseEnemy(target, false, "images/wolf.atlas");
      case CROW:
        return createFlyingEnemy(spawn, target, "images/crow.atlas");
      case KNIGHT:
        return createScaledBombEnemy(target, "images/knight.atlas", 0.75f, 1f);
      case DARK_ELVES:
        return NPCFactory.createChaseEnemy(target, true, "images/dark_elves.atlas");
      case JOTUNN:
        return createScaledGiant(target, "images/jotunn.atlas", 3f);
      case SLEIPNIR:
        return createSleipnir(target);

      // Egyptian
      case SANDEYE:
        return NPCFactory.createChaseEnemy(target, false, "images/sandeye.atlas");
      case BEETLE:
        return createScaledBombEnemy(target, "images/beetle.atlas", 0.75f, 0.75f);
      case CRAB:
        return createCrab(target);
      case WASP:
        return createFlyingEnemy(spawn, target, "images/wasp.atlas");
      case MUMMY:
        return createMummy(target);
      case SNAKE_MINI_BOSS:
        return createSnake(target);

      // Greek
      case MINOTAUR:
        return createMinotaur(target);
      case GOLEM:
        return createGolem(target);
      case MEDUSA:
        return createMedusa(target);
      case HARPY:
        return createFlyingEnemy(spawn, target, "images/harpy.atlas");
      case CYCLOPS:
        return createScaledGiant(target, "images/cyclops.atlas", 1.5f);
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

  /** Creates a flying enemy with patrol points relative to its spawn tile. */
  private Entity createFlyingEnemy(EnemySpawnConfig spawn, Entity target, String atlasPath) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    Vector2 leftPoint = terrain.tileToWorldPosition(spawn.x - 4, spawn.y);
    Vector2 topPoint = terrain.tileToWorldPosition(spawn.x, spawn.y + 3);
    Vector2 rightPoint = terrain.tileToWorldPosition(spawn.x + 4, spawn.y);

    return NPCFactory.createFloatingDemon(
        target, leftPoint, topPoint, rightPoint, this::spawnEntity, atlasPath);
  }

  /** Applies the existing visual scale for bomb-type enemies. */
  private Entity createScaledBombEnemy(Entity target, String atlasPath, float width, float height) {
    Entity enemy = NPCFactory.createBombEnemy(target, atlasPath, 2f);
    enemy.setScale(width, height);
    return enemy;
  }

  /** Shared setup for Wukong, Jotunn and Cyclops. */
  private Entity createScaledGiant(Entity target, String atlasPath, float size) {
    Entity enemy = NPCFactory.createGiantEnemy(target, atlasPath);
    enemy.setScale(size, size);
    enemy
        .getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(1f, 1f),
            new Vector2(enemy.getCenterPosition().x, enemy.getCenterPosition().y / 2f));
    PhysicsUtils.setScaledCollider(enemy, 0.3f, 0.3f);
    return enemy;
  }

  private Entity createCrab(Entity target) {
    Entity crab = NPCFactory.createChaseEnemy(target, true, "images/crab.atlas");
    crab.setScale(1.5f, 1f);
    crab.getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(1f, 0.5f),
            new Vector2(crab.getCenterPosition().x, crab.getCenterPosition().y / 2f));
    return crab;
  }

  private Entity createMummy(Entity target) {
    Entity mummy = NPCFactory.createGiantEnemy(target, "images/mummy.atlas");
    mummy
        .getComponent(HitboxComponent.class)
        .setAsBox(new Vector2(1f, 1.5f), mummy.getCenterPosition());
    PhysicsUtils.setScaledCollider(mummy, 0.3f, 0.3f);
    return mummy;
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

  private Entity createMinotaur(Entity target) {
    Entity minotaur = NPCFactory.createChaseEnemy(target, false, "images/minotaur.atlas");
    minotaur.setScale(1.5f, 1.5f);
    return minotaur;
  }

  private Entity createGolem(Entity target) {
    Entity golem = createScaledBombEnemy(target, "images/golem.atlas", 0.9f, 0.7f);
    golem
        .getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(1f, 1f),
            new Vector2(golem.getCenterPosition().x, golem.getCenterPosition().y / 2f));
    PhysicsUtils.setScaledCollider(golem, 0.3f, 0.3f);
    return golem;
  }

  private Entity createMedusa(Entity target) {
    Entity medusa = NPCFactory.createChaseEnemy(target, true, "images/medusa.atlas");
    medusa.setScale(1f, 1f);
    medusa.getComponent(HitboxComponent.class).setAsBox(new Vector2(1f, 1f));
    return medusa;
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

  /** Preserves Sleipnir's existing patrol bounds. */
  private Entity createSleipnir(Entity target) {
    Vector2[] mapBounds = {new Vector2(0f, 0f), new Vector2(10f, 10f)};
    return NPCFactory.createSleipnir(target, mapBounds);
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
