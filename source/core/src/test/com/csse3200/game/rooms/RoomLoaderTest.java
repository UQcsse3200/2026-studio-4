package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.configs.EntryPointConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RoomLoaderTest {
  private RoomLoader loader;

  @BeforeEach
  void setUp() {
    WorldConfig world = new WorldConfig();
    world.startRoomId = "hub";
    world.startEntryPointId = "start";
    world.rooms =
        new RoomConfig[] {room("hub", entry("start", 1, 2)), room("yard", entry("gate", 7, 9))};
    loader = new RoomLoader(mock(CameraComponent.class), world);
  }

  private static RoomConfig room(String id, EntryPointConfig entryPoint) {
    RoomConfig room = new RoomConfig();
    room.id = id;
    room.entryPoints = new EntryPointConfig[] {entryPoint};
    return room;
  }

  private static EntryPointConfig entry(String id, int x, int y) {
    EntryPointConfig entry = new EntryPointConfig();
    entry.id = id;
    entry.x = x;
    entry.y = y;
    return entry;
  }

  @Test
  void shouldLandATeleportAtTheNamedEntryPoint() {
    assertEquals(new GridPoint2(7, 9), loader.getEntrySpawn("yard", "gate"));
  }

  @Test
  void shouldRefuseATeleportToAnUnknownEntryPoint() {
    assertThrows(IllegalArgumentException.class, () -> loader.getEntrySpawn("yard", "nope"));
  }

  @Test
  void shouldRefuseATeleportToAnUnknownRoom() {
    assertThrows(IllegalArgumentException.class, () -> loader.getEntrySpawn("nowhere", "gate"));
  }
}
