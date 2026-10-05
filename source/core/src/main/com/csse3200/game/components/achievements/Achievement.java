package com.csse3200.game.components.achievements;

import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Base type for all achievements. Override only the hook(s) relevant to this achievement. */
public abstract class Achievement {
  private boolean unlocked = false;
  private final String name;

  protected Achievement(String name) {
    this.name = name;
  }

  public final boolean isUnlocked() {
    return unlocked;
  }

  public final String getName() {
    return name;
  }

  /** Marks this achievement unlocked. Returns true only on the call that actually unlocks it. */
  protected final boolean unlock() {
    if (unlocked) {
      return false;
    }
    unlocked = true;
    return true;
  }

  // ---- Hooks: override only what applies. Each returns true if it just unlocked. ----
  public boolean onEnemyDied(EnemyType type) { return false; }
  public boolean onPlayerDamaged() { return false; }
  public boolean onDungeonCompleted(String dungeonId) { return false; }
  public boolean onDungeonEntered(String dungeonId) { return false; }
  public boolean onSingleHitDamage(int damage) { return false; }
  public boolean onBossKilledAtLowHealth(float healthFraction) { return false; }
  public boolean onDungeonTimeElapsed(String dungeonId, float seconds) { return false; }
  public boolean onConsumableUsed() { return false; }
  public boolean onGoldChanged(int totalGold) { return false; }
}