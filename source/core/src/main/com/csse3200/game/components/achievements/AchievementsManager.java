package com.csse3200.game.components.achievements;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * The achievements manager for the game. Stores all of the achievements and updates them when
 * necessary.
 */
public class AchievementsManager extends Component {
  private final List<Achievement> achievements = new ArrayList<>();

  /**
   * Initializes the achievements' manager.
   *
   * @param room The room entity in which to start the achievements manager
   */
  public AchievementsManager(Entity room) {
    achievements.add(new Achievement(room, "FinalBossDefeated", 1, "Grandpa Fighter"));
  }

  /**
   * Moves the achievements manager into a new room.
   *
   * @param room The new room in which to move to.
   */
  public void newRoom(Entity room) {
    for (Achievement achievement : achievements) {
      achievement.newRoom(room);
    }
  }
}
