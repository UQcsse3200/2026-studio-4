package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import java.util.List;

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
    // Gets the list of obstacles from the spawnConfig
    List<String> spawnConfig = room.obstacles.spawns;

    // Loops through the spawn config
    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);

      for (int x = 0; x < row.length(); x++) {
        char spawnType = row.charAt(x);
        // Otherwise check for the obstacle that is wanted.
        int spawny = y;
        boolean centreY = true;
        Entity entity = null;
        switch (spawnType) {
          case '#':
          case '%':
            boolean up = isVoid(spawnConfig, x, y + 1);
            boolean right = isVoid(spawnConfig, x + 1, y);
            boolean down = isVoid(spawnConfig, x, y - 1);
            boolean left = isVoid(spawnConfig, x - 1, y);
            boolean upRight = isVoid(spawnConfig, x + 1, y + 1);
            boolean upLeft = isVoid(spawnConfig, x - 1, y + 1);
            boolean downRight = isVoid(spawnConfig, x + 1, y - 1);
            boolean downLeft = isVoid(spawnConfig, x - 1, y - 1);

            boolean shift = spawnType == '#';
            entity =
                ObstacleFactory.createWallFor(
                    up, right, down, left, upRight, upLeft, downRight, downLeft, shift);

            if (entity == null) {
              continue; // no art for this combination
            }

            if (isTopFace(up, right, down, left, upRight, upLeft)) {
              spawny = spawny - 1;
              centreY = false;
            }
            break;

          case 'S':
            entity = ObstacleFactory.createSword();
            centreY = false;
            break;

          case 'B':
            entity = ObstacleFactory.createBox();
            centreY = false;
            break;
        }
        if (entity != null) {
          spawnEntityAt(entity, new GridPoint2(x, spawny), true, centreY);
        }
      }
    }
  }

  /**
   * Checks if the given config is a top face.
   *
   * @param up Whether void above
   * @param right Whether void right
   * @param down Whether void below
   * @param left Whether void left
   * @param upRight Whether void up to the right
   * @param upLeft Whether void up to the left
   * @return If the config is a top face.
   */
  private static boolean isTopFace(
      boolean up, boolean right, boolean down, boolean left, boolean upRight, boolean upLeft) {
    return !left && !right && (up || ((upLeft || upRight) && !down));
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
