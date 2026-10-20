package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rooms.MusicManager;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RoomMusicComponentTest {

  private RoomConfig config;
  private Entity room;

  @BeforeEach
  void setup() {
    config = new RoomConfig();
    config.id = "roomMusicTestRoom";
    config.dungeonId = "dungeonOne";

    room = new Entity();
    room.addComponent(new RoomDataComponent(config));
    room.addComponent(new RoomMusicComponent());
    room.create();
  }

  @Test
  void shouldPlayFightMusicWhenEnteringAnUnclearedRoom() {
    MusicManager music = mock(MusicManager.class);
    ServiceLocator.registerMusicManager(music);

    RoomClearService clears = mock(RoomClearService.class);
    when(clears.isRoomCleared(config.id)).thenReturn(false);
    ServiceLocator.registerRoomClearService(clears);

    room.getEvents().trigger(RoomSelector.ROOM_SWITCHED_EVENT, null, config);

    verify(music).play("dungeonOne", false);
  }

  @Test
  void shouldPlayCalmMusicWhenReEnteringAClearedRoom() {
    MusicManager music = mock(MusicManager.class);
    ServiceLocator.registerMusicManager(music);

    RoomClearService clears = mock(RoomClearService.class);
    when(clears.isRoomCleared(config.id)).thenReturn(true);
    ServiceLocator.registerRoomClearService(clears);

    room.getEvents().trigger(RoomSelector.ROOM_SWITCHED_EVENT, null, config);

    verify(music).play("dungeonOne", true);
  }

  @Test
  void shouldPlayCalmMusicOnceTheRoomIsCleared() {
    MusicManager music = mock(MusicManager.class);
    ServiceLocator.registerMusicManager(music);

    room.getEvents().trigger(RoomClearService.ROOM_CLEARED, config.id);

    verify(music).play("dungeonOne", true);
  }

  @Test
  void shouldPassNullDungeonForTheHub() {
    MusicManager music = mock(MusicManager.class);
    ServiceLocator.registerMusicManager(music);
    config.dungeonId = null;

    room.getEvents().trigger(RoomClearService.ROOM_CLEARED, config.id);

    verify(music).play(null, true);
  }

  @Test
  void shouldNotCrashWithoutAMusicManager() {
    assertDoesNotThrow(
        () -> {
          room.getEvents().trigger(RoomSelector.ROOM_SWITCHED_EVENT, null, config);
          room.getEvents().trigger(RoomClearService.ROOM_CLEARED, config.id);
        });
  }
}
