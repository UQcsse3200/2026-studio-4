package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
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

  @Mock Entity mockedRoom;
  @Mock FollowingCameraComponent followingCamera;
  @Mock RoomClearService roomClear;
  @Mock TerrainComponent terrain;
  @Mock RoomDataComponent roomData;
  @Mock EventHandler events;

  @BeforeEach
  void setup() {
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerRoomClearService(roomClear);
    when(mockedRoom.getComponent(FollowingCameraComponent.class)).thenReturn(followingCamera);
    when(mockedRoom.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(mockedRoom.getComponent(RoomDataComponent.class)).thenReturn(roomData);
    when(mockedRoom.getEvents()).thenReturn(events);
  }

  @Test
  void shouldRegisterPlayerOnStart() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    when(mockedRoom.getEvents()).thenReturn(mock(EventHandler.class));
    when(roomLoader.getStartingRoom()).thenReturn(mockedRoom);
    when(roomLoader.getStartingSpawn()).thenReturn(new GridPoint2());

    roomSelector.start();

    verify(entityService).register(player);
  }

  @Test
  void shouldNotifyRoomSwitchEvent() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    int[] trigger = {0};

    EventHandler events = new EventHandler();
    when(mockedRoom.getEvents()).thenReturn(events);
    events.addListener(
        RoomSelector.ROOM_SWITCHED_EVENT, (RoomConfig oldConf, RoomConfig newConf) -> trigger[0]++);

    roomSelector.setRoom(mockedRoom, new GridPoint2());

    assertEquals(1, trigger[0]);
  }

  @Test
  void shouldResetCameraOnSettingANewRoom() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);
    roomSelector.setRoom(mockedRoom, new GridPoint2());

    verify(followingCamera).setCamera(camera);
    verify(followingCamera).setTarget(player);
  }

  @Test
  void shouldMovePlayerToSpawn() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);
    when(terrain.tileToWorldPosition(any())).thenReturn(new Vector2(2f, 2f));
    roomSelector.setRoom(mockedRoom, new GridPoint2());

    verify(player).setPosition(new Vector2(2f, 2f));
  }
}
