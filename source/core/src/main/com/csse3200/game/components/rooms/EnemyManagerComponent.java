package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.SplitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/** Spawns a random group of ghosts when its room is started. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private final int numberOfBombEnemies = new Random().nextInt(10, 20);
  private final Function<Vector2, Entity> dropFactory;
  private final Set<Entity> defeatedEnemies = new HashSet<>();
  private final Queue<Vector2> pendingDropPositions = new ArrayDeque<>();
  private int numEnemies = 0;

  public EnemyManagerComponent() {
    this(position -> ItemFactory.createDrop(ItemType.STRENGTH_CHARM, position));
  }

  EnemyManagerComponent(Function<Vector2, Entity> dropFactory) {
    this.dropFactory = dropFactory;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnEnemies);
  }

  /** Creates queued drops after the physics step has finished and the Box2D world is unlocked. */
  @Override
  public void update() {
    while (!pendingDropPositions.isEmpty()) {
      Entity drop = dropFactory.apply(pendingDropPositions.remove());
      spawnEntity(drop);
    }
  }

  public void spawnEnemies(Entity target) {
    spawnBombEnemies(target);
  }

  /** Creates ghosts at random valid tiles and sets the player as their target. */
  public void spawnBombEnemies(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfBombEnemies; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity bombEnemy = NPCFactory.createBombEnemy(target);
      track(bombEnemy);
      spawnEntityAt(bombEnemy, position, true, true);
    }
  }

  public void spawnSplitEnemy(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);

    Entity splitEnemy = NPCFactory.createBombEnemy(target);
    splitEnemy.addComponent(new SplitComponent(target));

    track(splitEnemy);
    splitEnemy.getEvents().addListener("spawnChildren", this::enemyTriggerSpawn);

    spawnEntityAt(splitEnemy, position, true, true);
  }

  /**
   * Tracks an enemy by incrementing numEnemies and listening for its death.
   *
   * @param enemy The enemy being tracked.
   */
  void track(Entity enemy) { // set to not private for testing reasons
    numEnemies++;
    enemy.getEvents().addListener("entityDied", () -> onEnemyDefeated(enemy));
  }

  /** Queues one item drop, then updates enemy tracking and room-cleared state. */
  private void onEnemyDefeated(Entity enemy) {
    if (!defeatedEnemies.add(enemy)) {
      return;
    }

    // Death usually fires from a Box2D collision callback. Creating the drop here would construct
    // its PhysicsComponent while the world is locked, so defer factory invocation until update().
    pendingDropPositions.add(enemy.getPosition());

    numEnemies--;
    if (numEnemies <= 0) {
      entity.getEvents().trigger("roomCleared");
    }
  }

  /**
   * Callback function for when an enemy wishes to spawn another enemy
   *
   * <p>Provide this to a listener to give an enemy the ability to add new entities to game. A spawn
   * should be triggered BEFORE an enemy triggers its death to prevent numEnemies <= 0.
   *
   * @param newEnemy the new enemy should not be registed.
   */
  private void enemyTriggerSpawn(Entity newEnemy) {
    track(newEnemy);
    spawnEntity(newEnemy);
  }
}
