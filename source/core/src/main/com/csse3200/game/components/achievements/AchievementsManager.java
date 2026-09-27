package com.csse3200.game.components.achievements;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;

public class AchievementsManager extends Component {
  private List<Achievement> achievements = new ArrayList<>();

  public AchievementsManager(Entity room) {
    achievements.add(new Achievement(room, "FinalBossDefeated", 1, "Grandpa Fighter"));
  }

  public void newRoom(Entity room) {
    for (Achievement achievement : achievements) {
      achievement.newRoom(room);
    }
  }
}
