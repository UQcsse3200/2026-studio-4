package com.csse3200.game.items;

import com.csse3200.game.components.CombatStatsComponent;

/** A Charm type that increases the players base attack. */
public class StrengthCharm extends StatCharm<Integer> {
  public StrengthCharm() {
    super(
        "Strength Charm",
        "Something that makes you stronger",
        10,
        "images/strength_charm_pixel.png");
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
