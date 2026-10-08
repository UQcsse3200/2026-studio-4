package com.csse3200.game.components.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.configs.PositionConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class RoomManagerTest {
  @Test
  void shouldFollowPairedDoorsAndRememberClearedRooms() {
    WorldConfig world = FileLoader.readClass(WorldConfig.class, "configs/rooms.json");

    world.startRoomId = "dungeonOneEntrance";
    world.startEntryPointId = "fromSelection";
    RoomConfig entrance = world.getRoom("dungeonOneEntrance");
    RoomConfig side = world.getRoom("dungeonOneSide");

    // sideDoor (53, 52) RIGHT in the entrance, returnDoor (20, 65) LEFT in the side room
    Entity firstEntrance = room(true, new GridPoint2(53, 52));
    Entity sideRoom = room(true, new GridPoint2(20, 65));
    Entity revisitedEntrance = room(true, new GridPoint2(53, 52));

    Entity player = mock(Entity.class);
    CameraComponent camera = mock(CameraComponent.class);
    EntityService entities = mock(EntityService.class);
    when(player.getEvents()).thenReturn(new EventHandler());
    when(player.getCenterPosition()).thenReturn(new Vector2());
    ServiceLocator.registerEntityService(entities);
    ServiceLocator.registerRunTimer(mock(RunTimer.class));

    try (MockedStatic<RoomFactory> roomFactory = mockStatic(RoomFactory.class)) {
      roomFactory
          .when(() -> RoomFactory.createRoom(entrance, camera, false))
          .thenReturn(firstEntrance);
      roomFactory.when(() -> RoomFactory.createRoom(side, camera, false)).thenReturn(sideRoom);
      roomFactory
          .when(() -> RoomFactory.createRoom(entrance, camera, true))
          .thenReturn(revisitedEntrance);

      RoomManager manager = new RoomManager(world, player, camera);
      manager.create();
      manager.interact();
      verify(firstEntrance, never()).dispose();
      manager.update();

      // Arrive at the side room's returnDoor (20, 65, LEFT)
      verify(player).setPosition(new Vector2(25, 63));
      manager.interact();
      manager.update();

      // Entrance was cleared, so it is rebuilt as a revisit
      roomFactory.verify(() -> RoomFactory.createRoom(entrance, camera, true));

      // Arrive back at the entrance's sideDoor (53, 52, RIGHT)
      verify(player).setPosition(new Vector2(52, 50));
      verify(firstEntrance).dispose();
      verify(sideRoom).dispose();
    }
  }

  private static Entity room(boolean cleared, GridPoint2 nearbyExit) {
    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    EnemyManagerComponent enemies = mock(EnemyManagerComponent.class);
    FollowingCameraComponent followingCameraComponent = mock(FollowingCameraComponent.class);
    EventHandler events = mock(EventHandler.class);
    when(terrain.tileToWorldPosition(any(GridPoint2.class)))
        .thenAnswer(
            invocation -> {
              GridPoint2 tile = invocation.getArgument(0);
              return tile.equals(nearbyExit) ? new Vector2() : new Vector2(tile.x, tile.y);
            });
    when(enemies.isCleared()).thenReturn(cleared);
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(room.getComponent(EnemyManagerComponent.class)).thenReturn(enemies);
    when(room.getEvents()).thenReturn(events);
    when(room.getComponent(FollowingCameraComponent.class)).thenReturn(followingCameraComponent);
    return room;
  }

  @Test
  void shouldScaleRoomOnStart() {
    Entity player = mock(Entity.class);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    Entity room = mock(Entity.class);
    TerrainComponent terrain = mock(TerrainComponent.class);
    EventHandler events = mock(EventHandler.class);
    EnemyManagerComponent enemies = mock(EnemyManagerComponent.class);
    when(room.getComponent(TerrainComponent.class)).thenReturn(terrain);
    when(room.getEvents()).thenReturn(events);
    when(room.getComponent(EnemyManagerComponent.class)).thenReturn(enemies);

    RoomManager roomManager = new RoomManager(player);
    RoomManager spyRoomManager = spy(roomManager);
    spyRoomManager.setCurrentRoom(room);
    spyRoomManager.start(new PositionConfig());

    verify(enemies).scale(0);
  }
}
