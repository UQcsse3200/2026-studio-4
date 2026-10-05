package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import java.util.List;

/** Spawns the fixed rocks and barrels declared for a room. */
public class ObstacleComponent extends EntityManagerComponent {
  private final RoomConfig room;

  public ObstacleComponent(RoomConfig room) {
    this.room = room;
  }

  @Override
  public void create() {
    // Gets the list of obstacles from the spawnConfig
    List<String> spawnConfig = room.obstacles.spawns;
    // Gets the height and width of the room
    int height = room.mapHeight;
    int width = room.mapWidth;

    // Loops through the spawn config
    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);

      for (int x = 0; x < row.length(); x++) {
        char spawnType = row.charAt(x);
        // Otherwise check for the obstacle that is wanted.
        switch (spawnType) {
          case '#':
            boolean up = isVoid(spawnConfig, x, y + 1);
            boolean right = isVoid(spawnConfig, x + 1, y);
            boolean down = isVoid(spawnConfig, x, y - 1);
            boolean left = isVoid(spawnConfig, x - 1, y);
            boolean upRight = isVoid(spawnConfig, x + 1, y + 1);
            boolean upLeft = isVoid(spawnConfig, x - 1, y + 1);
            boolean downRight = isVoid(spawnConfig, x + 1, y - 1);
            boolean downLeft = isVoid(spawnConfig, x - 1, y - 1);

            Entity entity =
                ObstacleFactory.createWallFor(
                    up, right, down, left, upRight, upLeft, downRight, downLeft);
            if (entity == null) {
              continue; // no art for this combination
            }

            if (isTopFace(up, right, down, left, upRight, upLeft)) {
              spawnEntityAt(entity, new GridPoint2(x, y - 1), true, false);
            } else {
              spawnEntityAt(entity, new GridPoint2(x, y), true, true);
            }

            break;

          case 'B':
            entity = ObstacleFactory.createBarrel();
            break;
          // otherwise dont create an obstacle
          case '.':
            continue;
          default:
            continue;
        }
        //        // Spawn the entity at this location.
        //        if (entity != null) {
        //          spawnEntityAt(entity, new GridPoint2(x, y), true, true);
        //        }
      }
    }
  }

  private static boolean isTopFace(
      boolean up, boolean right, boolean down, boolean left, boolean upRight, boolean upLeft) {
    return !left && !right && (up || ((upLeft || upRight) && !down));
  }

  public void spawn(int x, int y, Character c) {
    // Otherwise check for the obstacle that is wanted.
    switch (c) {
      case '#':
        entity = ObstacleFactory.createTile();
        spawnEntityAt(entity, new GridPoint2(x, y), true, true);
        break;

      case 'B':
        entity = ObstacleFactory.createBarrel();
        spawnEntityAt(entity, new GridPoint2(x, y), true, true);
        break;
    }
  }

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
