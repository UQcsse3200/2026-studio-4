package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentMatchers;

@ExtendWith(GameExtension.class)
class RoomManagerTest {
  @Test
  void shouldRegisterRoomAndPlayerAndPlacePlayerAtSpawn() {
    Entity player = mock(Entity.class);
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    when(room.getEvents()).thenReturn(mock(EventHandler.class));
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(terrain.tileToWorldPosition(ArgumentMatchers.any())).thenReturn(new Vector2(5f, 5f));
    ServiceLocator.registerEntityService(new EntityService());

    RoomManager roomManager = new RoomManager(room, player);
    roomManager.create();

    verify(room).create();
    verify(player).create();
    verify(player).setPosition(new Vector2(5f, 5f));
    assertEquals(room, roomManager.getCurrentRoom());
  }
}
