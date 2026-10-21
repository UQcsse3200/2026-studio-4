package com.csse3200.game.areas.attic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.rooms.configs.EntryPointConfig;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.PropConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The dungeon selection room is a dark attic library: a tileset room decorated with library props
 * and lit by candles. Its layout must be consistent and fully reachable.
 */
@ExtendWith(GameExtension.class)
class LibraryRoomConfigTest {
  private RoomConfig room;
  private List<String> grid;

  @BeforeEach
  void load() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");
    room = world.getRoom(world.startRoomId);
    grid = room.obstacles.spawns;
  }

  @Test
  void selectionRoomIsADarkAttic() {
    assertEquals(AtticSprites.ATTIC_TILESET, RoomFactory.getTileSet(room));
    assertTrue(room.props.length > 0);
    assertEquals(room.mapHeight, grid.size());
    for (String row : grid) {
      assertEquals(room.mapWidth, row.length());
    }
  }

  @Test
  void floorIsEnclosedByWalls() {
    for (int y = 0; y < grid.size(); y++) {
      for (int x = 0; x < grid.get(y).length(); x++) {
        if (!isFloor(at(x, y))) {
          continue;
        }
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
          char next = at(x + d[0], y + d[1]);
          assertTrue(
              isFloor(next) || isWall(next),
              "floor at (" + x + "," + y + ") leaks into the void");
        }
      }
    }
  }

  @Test
  void everyPropResolvesAndStandsOnFloor() {
    for (PropConfig prop : room.props) {
      assertDoesNotThrow(() -> AtticProp.fromName(prop.prop));
      assertTrue(
          isFloor(at(prop.x, prop.y)),
          prop.prop + " at (" + prop.x + "," + prop.y + ") is not on the floor");
    }
  }

  @Test
  void everyDungeonEntranceIsAGlowingTomeOnTheFloorPlan() {
    Set<String> dungeonExits =
        Set.of(
            "dungeonOneShelf",
            "dungeonTwoShelf",
            "dungeonThreeShelf",
            "dungeonFourShelf",
            "dungeonFiveShelf");
    assertEquals(dungeonExits.size(), room.exits.length);
    for (ExitConfig exit : room.exits) {
      assertTrue(dungeonExits.contains(exit.id), exit.id);
      assertNotNull(exit.sprite, exit.id + " has no sprite");
      assertNotNull(AtticProp.fromName(exit.sprite).getGlow(), exit.id + " is not a tome");
      assertTrue(isFloor(at(exit.x, exit.y)), exit.id + " is off the floor");
    }
  }

  @Test
  void roomIsDarkButHasLights() {
    assertTrue(room.darkness > 0f && room.darkness < 1f);
    // Every tome glows, plus at least the candles and stove.
    assertTrue(AtticLighting.lightsFor(room).size() > room.exits.length);
  }

  @Test
  void playerArrivesOnOpenFloor() {
    for (EntryPointConfig entry : room.entryPoints) {
      // RoomManager places the player at (x + 2, y - 2).
      assertTrue(isFloor(at(entry.x + 2, entry.y - 2)), entry.id);
    }
  }

  /** Floor in either a library room ('w', 'p', ...) or a tileset room (digits). */
  private static boolean isFloor(char c) {
    return AtticTerrain.isFloor(c) || Character.isDigit(c);
  }

  /** Wall in either a library room ('#', 'W') or a tileset room ('#', '%'). */
  private static boolean isWall(char c) {
    return AtticTerrain.isWall(c) || c == '%';
  }

  private char at(int x, int y) {
    if (y < 0 || y >= grid.size() || x < 0 || x >= grid.get(y).length()) {
      return AtticTerrain.VOID;
    }
    return grid.get(y).charAt(x);
  }
}
