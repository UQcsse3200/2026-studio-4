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

        Entity entity;
        // If its at the edges add a tile anyway
        if (y == 1 || y == height - 1 || x == 0 || x == width - 1) {
          entity = ObstacleFactory.createTile();
        } else {
          // Otherwise check for the obstacle that is wanted.
          switch (spawnType) {
            case '#':
              entity = ObstacleFactory.createTile();
              break;

            case 'B':
              entity = ObstacleFactory.createBarrel();
              break;
            // otherwise dont create an obstacle
            case '.':
              continue;

            default:
              throw new IllegalArgumentException(
                  "Unsupported obstacle type '" + spawnType + "' at (" + x + ", " + y + ")");
          }
        }
        // Spawn the entity at this location.
        spawnEntityAt(entity, new GridPoint2(x, y), true, true);
      }
    }
  }
}
