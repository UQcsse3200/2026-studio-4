package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.configs.EntryPointConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class RoomLoaderTest {
  private CameraComponent camera;
  private WorldConfig world;
  private RoomLoader loader;

  @BeforeEach
  void setUp() {
    world = new WorldConfig();
    world.startRoomId = "hub";
    world.startEntryPointId = "start";
    world.rooms =
        new RoomConfig[] {room("hub", entry("start", 1, 2)), room("yard", entry("gate", 7, 9))};
    world.rooms[1].trainingGrounds = true;
    camera = mock(CameraComponent.class);
    loader = new RoomLoader(camera, world);
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
  void shouldValidateWorldOnContructor() {
    var mockConfig = mock(WorldConfig.class);
    new RoomLoader(camera, mockConfig);

    verify(mockConfig).validate();
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

  @Test
  void shouldBuildTrainingGroundsWithTheTrainingFactory() {
    Entity room = new Entity();
    try (MockedStatic<RoomFactory> factory = mockStatic(RoomFactory.class)) {
      factory
          .when(() -> RoomFactory.createTrainingRoom(world.getRoom("yard"), camera, false))
          .thenReturn(room);

      assertSame(room, loader.getRoom("yard", false));

      factory.verify(() -> RoomFactory.createRoom(world.getRoom("yard"), camera, false), never());
    }
  }

  @Test
  void shouldBuildEveryOtherRoomWithTheUsualFactory() {
    Entity room = new Entity();
    try (MockedStatic<RoomFactory> factory = mockStatic(RoomFactory.class)) {
      factory
          .when(() -> RoomFactory.createRoom(world.getRoom("hub"), camera, true))
          .thenReturn(room);

      assertSame(room, loader.getRoom("hub", true));
      assertSame(room, loader.getRoom("hub", true));

      factory.verify(
          () -> RoomFactory.createTrainingRoom(world.getRoom("hub"), camera, true), never());
    }
  }

  @Test
  void shouldChooseTheFactoryForTheStartingRoomToo() {
    Entity room = new Entity();
    try (MockedStatic<RoomFactory> factory = mockStatic(RoomFactory.class)) {
      factory
          .when(() -> RoomFactory.createRoom(world.getRoom("hub"), camera, false))
          .thenReturn(room);

      assertSame(room, loader.getStartingRoom());
    }
  }
}
