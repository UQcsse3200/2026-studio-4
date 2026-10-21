package com.csse3200.game.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.RoomDataComponent;
import com.csse3200.game.components.rooms.configs.ExitConfig;
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
  @Mock EventHandler mockedEvents;

  @BeforeEach
  void setup() {
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerRoomClearService(roomClear);
    lenient()
        .when(mockedRoom.getComponent(FollowingCameraComponent.class))
        .thenReturn(followingCamera);
    lenient().when(mockedRoom.getComponent(TerrainComponent.class)).thenReturn(terrain);
    lenient().when(mockedRoom.getComponent(RoomDataComponent.class)).thenReturn(roomData);
    lenient().when(mockedRoom.getEvents()).thenReturn(mockedEvents);
  }

  @Test
  void shouldRegisterPlayerAndRoomOnStart() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    when(roomLoader.getStartingRoom()).thenReturn(mockedRoom);
    when(roomLoader.getStartingSpawn()).thenReturn(new GridPoint2());

    roomSelector.start();

    verify(entityService).register(player);
    verify(entityService).register(mockedRoom);
  }

  @Test
  void shouldDisposeOldRoomWhenSwitching() {
    Entity room = mock(Entity.class);
    RoomSelector roomSelector = new RoomSelector(room, player, camera, roomLoader);
    when(room.getComponent(RoomDataComponent.class)).thenReturn(roomData);

    roomSelector.setRoom(mockedRoom, new GridPoint2());
    verify(room).dispose();
  }

  @Test
  void shouldRegisterNewRoomWhenSwitching() {
    Entity room = mock(Entity.class);
    RoomSelector roomSelector = new RoomSelector(room, player, camera, roomLoader);
    when(room.getComponent(RoomDataComponent.class)).thenReturn(roomData);

    roomSelector.setRoom(mockedRoom, new GridPoint2());
    verify(entityService).register(mockedRoom);
  }

  @Test
  void shouldNotifyRoomOfItsCreation() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);

    EventHandler events = new EventHandler();
    when(mockedRoom.getEvents()).thenReturn(events);
    Entity[] recievedPlayer = new Entity[1];

    events.addListener(
        "RoomCreated",
        (Entity playerSent) -> {
          recievedPlayer[0] = playerSent;
        });

    roomSelector.setRoom(mockedRoom, new GridPoint2());
    assertSame(player, recievedPlayer[0]);
  }

  @Test
  void shouldThrowOnSwitchingToSameRoom() {
    Entity room = mock(Entity.class);
    RoomSelector roomSelector = new RoomSelector(room, player, camera, roomLoader);
    var spawn = new GridPoint2();
    assertThrows(IllegalArgumentException.class, () -> roomSelector.setRoom(room, spawn));
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

  @Test
  void shouldMovePlayerToExit() {
    RoomSelector roomSelector = new RoomSelector(null, player, camera, roomLoader);
    ExitConfig exit = new ExitConfig();
    when(terrain.tileToWorldPosition(any())).thenReturn(new Vector2(2f, 2f));
    when(roomLoader.getSpawnPoint(any(ExitConfig.class))).thenReturn(new GridPoint2());
    when(roomLoader.getRoom(any(ExitConfig.class))).thenReturn(mockedRoom);

    roomSelector.setRoom(exit);
    verify(player).setPosition(new Vector2(2f, 2f));
  }
}
