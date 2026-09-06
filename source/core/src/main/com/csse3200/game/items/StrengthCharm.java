package com.csse3200.game.items;

import com.csse3200.game.components.CombatStatsComponent;

public class StrengthCharm extends StatCharm<Integer> {
  public StrengthCharm() {
    super("Strength Charm", "Something that makes you stronger", 10);
  }

  @Override
  public void applyStatChange(CombatStatsComponent combatStats) {
    if (checkApplied()) return;

    combatStats.addBaseAttack(value);
    applied = true;
  }

  @Override
  public void removeStatChange(CombatStatsComponent combatStats) {
    combatStats.addBaseAttack(-value);
  }
}
