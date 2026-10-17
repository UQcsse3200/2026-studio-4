package com.csse3200.game.saving;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.GameSaveMapper;
import com.csse3200.game.rooms.RoomSelector;
import com.csse3200.game.services.RunTimer;

public class RunSaveAdapter {
  private final Entity player;
  private final RoomSelector rooms;
  private final RunTimer timer;

  public RunSaveAdapter(Entity player, RoomSelector rooms, RunTimer timer) {
    this.player = player;
    this.rooms = rooms;
    this.timer = timer;
  }

  public GameSaveData capture() {
    GameSaveData data = GameSaveMapper.capture(player);
    data.roomId = RunSaveState.getCurrentRoomId();
    data.playTimeSeconds = timer.getTotalTime();
    data.dungeonTimesSeconds.putAll(timer.getDungeonTimes());
    return data;
  }

  public void restorePlayer(GameSaveData data) {
    GameSaveMapper.restore(player, data);
    GameSaveMapper.restoreAchievements(data);
    timer.restoreRun(data.playTimeSeconds, data.dungeonTimesSeconds);
  }
}
