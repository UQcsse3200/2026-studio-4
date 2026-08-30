package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.utils.math.RandomUtils;
import java.util.Random;

/** Spawns a random group of ghosts when its room is started. */
public class EnemyManagerComponent extends EntityManagerComponent {
  private static final GridPoint2 FLOATING_DEMON_SPAWN = new GridPoint2(5, 8);
  private final int numberOfBombEnemies = new Random().nextInt(10, 20);

  @Override
  public void create() {
    entity.getEvents().addListener("RoomCreated", this::spawnBombEnemies);
    entity.getEvents().addListener("RoomCreated", this::spawnFloatingDemon);
  }

  /** Creates ghosts at random valid tiles and sets the player as their target. */
  public void spawnBombEnemies(Entity target) {
    GridPoint2 maxPosition = spawnableArea();
    for (int i = 0; i < numberOfBombEnemies; i++) {
      GridPoint2 position = RandomUtils.random(new GridPoint2(0, 0), maxPosition);
      Entity ghost = NPCFactory.createBombEnemy(target);
      spawnEntityAt(ghost, position, true, true);
    }
  }

  /** Creates a floating demon that patrols horizontally across the room. */
  private void spawnFloatingDemon(Entity target) {
    TerrainComponent terrain = entity.getComponent(TerrainComponent.class);
    float mapWidth = terrain.getMapBounds(0).x * terrain.getTileSize();
    Entity demon = NPCFactory.createFloatingDemon(target, 1f, mapWidth - 1f);
    spawnEntityAt(demon, FLOATING_DEMON_SPAWN, true, true);
  }
}
