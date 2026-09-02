package com.csse3200.game.rooms;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.RoomManager;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.components.rooms.configs.WorldConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RoomFactory;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
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
    Entity firstEntrance = room(true, new GridPoint2(48, 14));
    Entity sideRoom = room(true, new GridPoint2(2, 14));
    Entity revisitedEntrance = room(true, new GridPoint2(48, 14));
    Entity player = mock(Entity.class);
    CameraComponent camera = mock(CameraComponent.class);
    EntityService entities = mock(EntityService.class);
    when(player.getEvents()).thenReturn(new EventHandler());
    when(player.getCenterPosition()).thenReturn(new Vector2());
    ServiceLocator.registerEntityService(entities);

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

      verify(player).setPosition(new Vector2(5, 14));
      manager.interact();
      manager.update();

      roomFactory.verify(() -> RoomFactory.createRoom(entrance, camera, true));
      verify(player).setPosition(new Vector2(45, 14));
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
    when (room.getComponent(FollowingCameraComponent.class)).thenReturn(followingCameraComponent);
    return room;
  }
}
