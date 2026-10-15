package com.csse3200.game.components.rooms;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.rooms.RoomClearService;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/** RoomAcheivements */
public class RoomAcheivements extends Component {

  @Override
  public void create() {
    entity.getEvents().addListener(RoomClearService.DUNGEON_CLEARED, this::onDungeonCompleted);
    entity.getEvents().addListener("RoomSwitched", this::onRoomSwitched);
  }

  private void onDungeonCompleted(String dungeonId) {
    AchievementService achievements = ServiceLocator.getAchievementService();
    AchievementContext context = new AchievementContext();
    context.dungeonCompletedId = dungeonId;
    context.dungeonId = dungeonId;

    RunTimer timer = ServiceLocator.getRunTimer();
    context.dungeonSeconds = timer.getDungeonTime();
    achievements.update(context);
  }

  private void onRoomSwitched(RoomConfig oldConfig, RoomConfig newConfig) {
    String oldDungeonId = oldConfig == null ? null : oldConfig.dungeonId;
    String newDungeonId = newConfig.dungeonId;

    if (Objects.equals(oldDungeonId, newDungeonId)) {
      return;
    }

    RunTimer timer = ServiceLocator.getRunTimer();
    timer.stopDungeon();
    timer.startDungeon(newDungeonId);

    AchievementService achievements = ServiceLocator.getAchievementService();

    AchievementContext context = new AchievementContext();
    context.dungeonEnteredId = newDungeonId;
    achievements.update(context);
  }
}
