package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.RoomDataComponent;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
public class RoomSelectorTest {
  @Mock RoomLoader roomLoader;
  @Mock EntityService entityService;
  @Mock Entity player;
  @Mock CameraComponent camera;

  @Mock Entity startRoom;
  @Mock FollowingCameraComponent followingCamera;
  @Mock RoomClearService roomClear;
  @Mock TerrainComponent terrain;
  @Mock RoomDataComponent roomData;

  @BeforeEach
  void setup() {
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerRoomClearService(roomClear);
    when(startRoom.getComponent(FollowingCameraComponent.class)).thenReturn(followingCamera);
    when(startRoom.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(startRoom.getComponent(RoomDataComponent.class)).thenReturn(roomData);
  }

  @Test
  void shouldRegisterPlayerOnStart() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    when(startRoom.getEvents()).thenReturn(mock(EventHandler.class));
    when(roomLoader.getStartingRoom()).thenReturn(startRoom);
    when(roomLoader.getStartingSpawn()).thenReturn(new GridPoint2());

    roomSelector.start();

    verify(entityService).register(player);
  }

  @Test
  void shouldNotifyRoomSwitchEvent() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    int[] trigger = {0};

    EventHandler events = new EventHandler();
    when(startRoom.getEvents()).thenReturn(events);
    events.addListener(
        RoomSelector.ROOM_SWITCHED_EVENT, (RoomConfig oldConf, RoomConfig newConf) -> trigger[0]++);

    roomSelector.setRoom(startRoom, new GridPoint2());

    assertEquals(1, trigger[0]);
  }
}
