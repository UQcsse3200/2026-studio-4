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
    List<String> spawnConfig = room.obstacles.spawns;
    int height = room.mapHeight;
    int width = room.mapWidth;

    for (int y = 0; y < spawnConfig.size(); y++) {
      String row = spawnConfig.get(y);

      for (int x = 0; x < row.length(); x++) {
        char spawnType = row.charAt(x);

        Entity entity;

        if (y == 1 || y == height - 1 || x == 0 || x == width - 1) {
          entity = ObstacleFactory.createTile();
        } else {
          switch (spawnType) {
            case '#':
              entity = ObstacleFactory.createTile();
              break;

            case 'B':
              entity = ObstacleFactory.createBarrel();
              break;

            case '.':
              continue;

            default:
              throw new IllegalArgumentException(
                  "Unsupported obstacle type '" + spawnType + "' at (" + x + ", " + y + ")");
          }
        }
        spawnEntityAt(entity, new GridPoint2(x, y), true, true);
      }
    }
  }
}
