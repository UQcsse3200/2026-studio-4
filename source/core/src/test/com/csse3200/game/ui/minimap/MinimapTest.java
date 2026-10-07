package com.csse3200.game.ui.minimap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.minimap.Minimap.RoomType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** MinimapTest */
@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
public class MinimapTest {
  @Spy private Table left;
  @Spy private Table right;
  @Spy private Table center;

  @Mock private Entity player;
  @Mock private EventHandler events;
  @Mock private RenderService renderService;
  @Mock private ResourceService resourceService;

  private Set<String> clearedIds = new HashSet<>();
  private RoomConfig roomConfig;
  private Minimap minimapSpy;

  @BeforeEach
  void setup() {
    roomConfig = new RoomConfig();
    roomConfig.id = "room";
    roomConfig.exits = new ExitConfig[] {};

    minimapSpy = spy(new Minimap(roomConfig, clearedIds, left, right, center));
    minimapSpy.setEntity(player);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(resourceService);
  }

  @AfterEach
  void tearDown() {
    clearedIds.clear();
  }

  @Test
  void correctlyDeterminesRoomType() {
    String clearedRoomId = "room";
    String startRoomId = "startRoom";
    clearedIds.add(clearedRoomId);

    var minimap = new Minimap(roomConfig, clearedIds, startRoomId);
    assertEquals(RoomType.CLEARED, minimap.determineRoomType(clearedRoomId));
    assertEquals(RoomType.HOME, minimap.determineRoomType(startRoomId));
    assertEquals(RoomType.DEFAULT, minimap.determineRoomType("Random room"));
    assertEquals(RoomType.DEFAULT, minimap.determineRoomType(null));
  }

  @Test
  void shouldClearTablesWhenRebuilding() {
    var minimap =
        new Minimap(roomConfig, clearedIds, left, right, center) {
          void attachRoom(Table table, RoomType type) {}
          ;
        };
    minimap.onRoomChanged(roomConfig);

    verify(left).clearChildren();
    verify(right).clearChildren();
  }

  @Test
  void attachesOnlyLeftRooms() {
    var newRoom = new RoomConfig();
    newRoom.id = "newRoom";

    var exit = new ExitConfig();
    exit.destinationRoomId = "room2";
    exit.side = "LEFT";

    var exit2 = new ExitConfig();
    exit2.destinationRoomId = "room3";
    exit2.side = "LEFT";

    newRoom.exits = new ExitConfig[] {exit, exit2};

    doAnswer(invocation -> new Actor()).when(minimapSpy).attachRoom(any(), any());
    minimapSpy.onRoomChanged(newRoom);

    verify(minimapSpy, times(2)).attachRoom(left, RoomType.DEFAULT);
    verify(minimapSpy, never()).attachRoom(right, RoomType.DEFAULT);
  }

  @Test
  void attachesOnlyRightRooms() {
    var newRoom = new RoomConfig();
    newRoom.id = "newRoom";

    var exit = new ExitConfig();
    exit.destinationRoomId = "room2";
    exit.side = "RIGHT";

    var exit2 = new ExitConfig();
    exit2.destinationRoomId = "room3";
    exit2.side = "RIGHT";

    newRoom.exits = new ExitConfig[] {exit, exit2};

    doAnswer(invocation -> new Actor()).when(minimapSpy).attachRoom(any(), any());
    minimapSpy.onRoomChanged(newRoom);

    verify(minimapSpy, times(2)).attachRoom(right, RoomType.DEFAULT);
    verify(minimapSpy, never()).attachRoom(left, RoomType.DEFAULT);
  }

  @Test
  void attachesListenerOnCreate() {
    var minimap =
        new Minimap(roomConfig, clearedIds, left, right, center) {
          void attachRoom(Table table, RoomType type) {}
          ;
        };

    when(player.getEvents()).thenReturn(events);
    when(resourceService.getAsset(any(), any())).thenReturn(mock(Texture.class));
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    minimap.setEntity(player);

    minimap.create();
    verify(events).addListener(eq(Minimap.ROOM_CHANGE_EVENT), any(EventListener1.class));
  }

  @Test
  void shouldRebuildAndSetRoomOnRoomChange() {
    var newRoom = new RoomConfig();
    newRoom.id = "newRoom";

    var exit = new ExitConfig();
    exit.destinationRoomId = "room2";
    exit.side = "RIGHT";

    var exit2 = new ExitConfig();
    exit2.destinationRoomId = "room3";
    exit2.side = "RIGHT";

    newRoom.exits = new ExitConfig[] {exit, exit2};

    doAnswer(invocation -> new Actor()).when(minimapSpy).attachRoom(any(), any());
    minimapSpy.onRoomChanged(newRoom);
    verify(minimapSpy).setCurrentRoom(newRoom);

    // Since rebuild is private check the left right containers are cleared,
    // which only happens in rebuild
    verify(left).clearChildren();
    verify(right).clearChildren();
  }
}
