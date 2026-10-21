package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RoomClearServiceTest {
  private Entity room;

  @BeforeEach
  void setUp() {
    room = new Entity();
    ServiceLocator.registerRoomClearService(new RoomClearService());
    ServiceLocator.getRoomClearService().attachListeners(room);
  }

  @Test
  void isRoomCleared() {
    room.getEvents().trigger("RoomCleared", "Room1");
    assertTrue(ServiceLocator.getRoomClearService().isRoomCleared("Room1"));
    assertFalse(ServiceLocator.getRoomClearService().isRoomCleared("Room2"));
    assertFalse(ServiceLocator.getRoomClearService().isRoomCleared("room1"));
  }

  @Test
  void isDungeonCleared() {
    room.getEvents().trigger("DungeonCleared", "Dungeon1");
    assertTrue(ServiceLocator.getRoomClearService().isDungeonCleared("Dungeon1"));
    assertFalse(ServiceLocator.getRoomClearService().isDungeonCleared("Dungeon2"));
    assertFalse(ServiceLocator.getRoomClearService().isDungeonCleared("dungeon1"));
  }

  @Test
  void isDungeonClearedCount4() {
    room.getEvents().trigger("DungeonCleared", "Dungeon1");
    room.getEvents().trigger("DungeonCleared", "Dungeon2");
    assertTrue(ServiceLocator.getRoomClearService().isDungeonCleared("Dungeon1"));
    assertTrue(ServiceLocator.getRoomClearService().isDungeonCleared("Dungeon2"));
    assertEquals(2, ServiceLocator.getRoomClearService().getDungeonClearedCount());
    room.getEvents().trigger("DungeonCleared", "Dungeon3");
    room.getEvents().trigger("DungeonCleared", "Dungeon4");
    assertEquals(4, ServiceLocator.getRoomClearService().getDungeonClearedCount());
  }
}
