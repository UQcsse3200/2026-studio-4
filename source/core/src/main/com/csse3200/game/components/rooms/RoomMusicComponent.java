package com.csse3200.game.components.rooms;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.rooms.MusicManager;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.services.ServiceLocator;

/**
 * Tells the {@link MusicManager} which track this room wants: when the player arrives, and again
 * once the room is cleared.
 */
public class RoomMusicComponent extends Component {

  @Override
  public void create() {
    entity.getEvents().addListener(RoomSelector.ROOM_SWITCHED_EVENT, this::onRoomSwitched);
    entity.getEvents().addListener(RoomClearService.ROOM_CLEARED, this::onRoomCleared);
  }

  private void onRoomSwitched(RoomConfig oldConfig, RoomConfig newConfig) {
    // Any earlier clear of this room was recorded on a previous visit, so it is safe to ask.
    RoomClearService clears = ServiceLocator.getRoomClearService();
    boolean cleared = clears != null && clears.isRoomCleared(newConfig.id);
    play(newConfig.dungeonId, cleared);
  }

  private void onRoomCleared(String roomId) {
    // RoomClearService hears this same event after us, so it has not recorded the clear yet.
    RoomConfig config = entity.getComponent(RoomDataComponent.class).getConfig();
    play(config.dungeonId, true);
  }

  private void play(String dungeonId, boolean cleared) {
    MusicManager music = ServiceLocator.getMusicManager();
    if (music != null) {
      music.play(dungeonId, cleared);
    }
  }
}
