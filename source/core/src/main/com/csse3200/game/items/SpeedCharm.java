package com.csse3200.game.items;

import com.csse3200.game.components.CombatStatsComponent;

public class SpeedCharm extends StatCharm<Float> {

  public SpeedCharm() {
    super("Speed Charm", "You feel kinda fast", 0.3f, "");
  }

  @Override
  public void applyStatChange(CombatStatsComponent combatStats) {
    if (checkApplied()) return;

    combatStats.addMovementSpeed(value);
    applied = true;
  }

  @Override
  public void removeStatChange(CombatStatsComponent combatStats) {
    if (!applied) return;
    combatStats.addMovementSpeed(-value);
    applied = false;
  }
}
