package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Spawns the fixed obstacles declared for a room. */
public class ObstacleComponent extends EntityManagerComponent {
  private final RoomConfig room;

  /**
   * Create an obstacle component with the room config given
   *
   * @param room A room config.
   */
  public ObstacleComponent(RoomConfig room) {
    this.room = room;
  }

  @Override
  public void create() {
    List<String> spawnConfig = room.obstacles.spawns;

    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);
      for (int x = 0; x < row.length(); x++) {
        spawnObstacle(spawnConfig, row.charAt(x), x, y);
      }
    }
  }

  /** Decides what, if anything, belongs at this grid cell. */
  private void spawnObstacle(List<String> spawnConfig, char spawnType, int x, int y) {
    switch (spawnType) {
      case '#', '%' -> spawnWall(spawnConfig, x, y, spawnType == '#');
      case 'S' -> spawnProp(ObstacleFactory.createSword(), x, y);
      case 'B' -> spawnProp(ObstacleFactory.createBox(), x, y);
    }
  }

  /** Props are placed on the cell's bottom edge, not centred vertically. */
  private void spawnProp(Entity prop, int x, int y) {
    spawnEntityAt(prop, new GridPoint2(x, y), true, false);
  }

  /** Walls pick their art from which neighbouring cells are void. */
  private void spawnWall(List<String> spawnConfig, int x, int y, boolean shift) {
    Set<Direction> voids = EnumSet.noneOf(Direction.class);
    for (Direction d : Direction.values()) {
      if (isVoid(spawnConfig, x + d.dx, y + d.dy)) {
        voids.add(d);
      }
    }

    Entity wall = ObstacleFactory.createWallFor(voids, shift);
    if (wall == null) {
      return; // no art for this combination
    }

    boolean topFace = isTopFace(voids);
    spawnEntityAt(wall, new GridPoint2(x, topFace ? y - 1 : y), true, !topFace);
  }

  /**
   * Checks if the given config should be a top face.
   *
   * @param voids direction where the neighbouring tile is void.
   * @return If the config is a top face.
   */
  private static boolean isTopFace(Set<Direction> voids) {
    return !voids.contains(Direction.LEFT)
        && !voids.contains(Direction.RIGHT)
        && (voids.contains(Direction.UP)
            || ((voids.contains(Direction.UP_LEFT) || voids.contains(Direction.UP_RIGHT))
                && !voids.contains(Direction.DOWN)));
  }

  /**
   * Checks if the given coordinate is a void (outside the map or a ' ' Character)
   *
   * @param spawnConfig The spawn config
   * @param x The x value of the coordinate
   * @param y The y value of the coordinate
   * @return If the given coordinate is a void.
   */
  private static boolean isVoid(List<String> spawnConfig, int x, int y) {
    // Outside the spawn configuration counts as void
    if (y < 0 || y >= spawnConfig.size()) {
      return true;
    }

    String row = spawnConfig.get(y);

    // Overflow past either side counts as void
    if (x < 0 || x >= row.length()) {
      return true;
    }

    return row.charAt(x) == ' ';
  }
}
