package com.csse3200.game.saving;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.GameSaveMapper;
import com.csse3200.game.services.RunTimer;

public class RunSaveAdapter {
  private final Entity player;
  private final RunTimer timer;

  public RunSaveAdapter(Entity player, RunTimer timer) {
    this.player = player;
    this.timer = timer;
  }

  public GameSaveData capture() {
    GameSaveData data = GameSaveMapper.capture(player);
    GameSaveData.ResumePosition resume = new GameSaveData.ResumePosition();
    resume.roomId = RunSaveState.getCurrentRoomId();
    resume.x = player.getPosition().x;
    resume.y = player.getPosition().y;
    data.resumePosition = resume;
    data.playTimeSeconds = timer.getTotalTime();
    data.dungeonTimesSeconds.putAll(timer.getDungeonTimes());
    return data;
  }

  public void restorePlayer(GameSaveData data) {
    GameSaveMapper.restore(player, data);
    GameSaveMapper.restoreAchievements(data);
    GameSaveMapper.restoreRoomClears(data);
    timer.restoreRun(data.playTimeSeconds, data.dungeonTimesSeconds);
  }
}
