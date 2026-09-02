package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
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
  void shouldAddBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addBaseAttack(10);
    assertEquals(30, combat.getBaseAttack());

    combat.addBaseAttack(-10);
    assertEquals(20, combat.getBaseAttack());
  }

  @Test
  void shouldTriggerBaseAttackUpdate() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    Entity entity = new Entity().addComponent(combat);
    entity.create();
    int[] updatedAttack = {0};
    entity
        .getEvents()
        .addListener("updateBaseAttack", attack -> updatedAttack[0] = (Integer) attack);

    combat.addBaseAttack(10);

    assertEquals(30, updatedAttack[0]);
  }

  @Test
  void shouldGetSetMovementSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 30, 3f, 4f);
    assertEquals(3f, combat.getMovementSpeed());

    combat.setMovementSpeed(5f);
    assertEquals(5f, combat.getMovementSpeed());

    combat.setMovementSpeed(-4f);
    assertEquals(5f, combat.getMovementSpeed());
  }

  @Test
  void shouldAddMovementSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4f, 7f);
    combat.addMovementSpeed(-3);
    assertEquals(1f, combat.getMovementSpeed());

    combat.addMovementSpeed(6f);
    combat.addMovementSpeed(-8F);
    assertEquals(7, combat.getMovementSpeed());
  }

  @Test
  void shouldGetSetAttackSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 30, 3f, 4f);
    assertEquals(4f, combat.getAttackSpeed());

    combat.setAttackSpeed(2f);
    assertEquals(2f, combat.getAttackSpeed());

    combat.setAttackSpeed(-4f);
    assertEquals(2f, combat.getAttackSpeed());
  }

  @Test
  void shouldAddAttackSpeed() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20, 4f, 7f);
    combat.addAttackSpeed(-7);
    assertEquals(0, combat.getAttackSpeed());

    combat.addAttackSpeed(6f);
    combat.addAttackSpeed(-2F);
    assertEquals(4f, combat.getAttackSpeed());
  }
}
