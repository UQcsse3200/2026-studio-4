package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.items.charms.AttackSpeedCharm;
import com.csse3200.game.items.charms.SpeedCharm;
import com.csse3200.game.items.charms.StrengthCharm;
import org.junit.jupiter.api.Test;

/** Checks that every charm reports the effect the inventory tooltip shows the player. */
class CharmEffectSummaryTest {

  @Test
  void strengthCharmShouldSummariseItsEffect() {
    assertEquals("Attack +10", new StrengthCharm().getEffectSummary());
  }

  @Test
  void speedCharmShouldSummariseItsEffect() {
    assertEquals("Speed +1", new SpeedCharm().getEffectSummary());
  }

  @Test
  void attackSpeedCharmShouldSummariseItsEffect() {
    assertEquals("Attack Speed +1", new AttackSpeedCharm().getEffectSummary());
  }
}
