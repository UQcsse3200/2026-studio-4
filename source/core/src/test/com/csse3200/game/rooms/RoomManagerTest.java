package com.csse3200.game.rooms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.rooms.RoomManager;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class RoomManagerTest {
  private static Entity createMockRoom() {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);

    when(room.getEvents()).thenReturn(mock(EventHandler.class));
    when(terrain.tileToWorldPosition(Mockito.any())).thenReturn(new Vector2());
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);

    return room;
  }

  @Test
  void shouldCreatePlayerAndRoom() {
    Entity player = mock(Entity.class);
    Entity room = createMockRoom();

    RoomManager roomManager = new RoomManager(room, player);
    ServiceLocator.registerEntityService(new EntityService());

    roomManager.create();

    verify(room).create();
    verify(player).create();
  }

  @Test
  void shouldSwitchRooms() {
    Entity room = mock(Entity.class);
    Entity player = mock(Entity.class);
    Entity newRoom = createMockRoom();

    RoomManager roomManager = new RoomManager(room, player);
    ServiceLocator.registerEntityService(new EntityService());
    roomManager.switchRoom(newRoom);

    verify(room).dispose();
    verify(newRoom).create();
    assertEquals(newRoom, roomManager.getCurrentRoom());
  }
}
