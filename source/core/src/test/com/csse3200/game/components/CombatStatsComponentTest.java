package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatStatsComponentTest {
  @Test
  void shouldSetGetHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getHealth());
    assertEquals(100, combat.getMaxHealth());

    combat.setHealth(150);
    assertEquals(100, combat.getHealth());

    combat.setHealth(50);
    assertEquals(50, combat.getHealth());

    combat.setHealth(-50);
    assertEquals(0, combat.getHealth());
  }

  @Test
  void shouldCheckIsDead() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertFalse(combat.isDead());

    combat.setHealth(0);
    assertTrue(combat.isDead());
  }

  @Test
  void shouldAddHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addHealth(-500);
    assertEquals(0, combat.getHealth());

    combat.addHealth(100);
    combat.addHealth(-20);
    assertEquals(80, combat.getHealth());
  }

  @Test
  void shouldSetGetBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(20, combat.getBaseAttack());

    combat.setBaseAttack(150);
    assertEquals(150, combat.getBaseAttack());

    combat.setBaseAttack(-50);
    assertEquals(150, combat.getBaseAttack());
  }

  @Test
  void shouldSetGetStrength() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(0, combat.getStrength());

    combat.setStrength(10);
    assertEquals(10, combat.getStrength());

    combat.setStrength(-5);
    assertEquals(0, combat.getStrength());
  }

  @Test
  void shouldAddStrength() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addStrength(10);
    assertEquals(10, combat.getStrength());

    combat.addStrength(-10);
    assertEquals(0, combat.getStrength());
  }

  @Test
  void shouldNotAllowNegativeStrength() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addStrength(5);
    combat.addStrength(-100);
    assertEquals(0, combat.getStrength());
  }
}
