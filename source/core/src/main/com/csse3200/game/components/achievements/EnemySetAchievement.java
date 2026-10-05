package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import java.util.EnumSet;
import java.util.Set;

/** Unlocks after at least one kill of every enemy type in a given set. */
public class EnemySetAchievement extends Achievement {
  private final Set<EnemyType> required;
  private final Set<EnemyType> seen = EnumSet.noneOf(EnemyType.class);

  public EnemySetAchievement(Set<EnemyType> required, String name) {
    super(name);
    this.required = EnumSet.copyOf(required);
  }

  @Override
  public boolean onEnemyDied(EnemyType type) {
    if (isUnlocked() || !required.contains(type)) {
      return false;
    }
    seen.add(type);
    return seen.containsAll(required) && unlock();
  }

  public int getSeenCount() {
    return seen.size();
  }

  public int getRequiredCount() {
    return required.size();
  }
}
