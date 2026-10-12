package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerberusPhaseComponentTest {
  private CombatStatsComponent middleStats;
  private CombatStatsComponent leftStats;
  private CombatStatsComponent rightStats;
  private CerberusPhaseComponent phase;
  private final int[] enraged = new int[3];

  @BeforeEach
  void setUp() {
    middleStats = new CombatStatsComponent(250, 20);
    leftStats = new CombatStatsComponent(125, 10);
    rightStats = new CombatStatsComponent(125, 10);

    Entity left = new Entity().addComponent(leftStats);
    Entity right = new Entity().addComponent(rightStats);
    phase = new CerberusPhaseComponent(left, right);
    Entity middle = new Entity().addComponent(middleStats).addComponent(phase);

    middle.getEvents().addListener("enragePhaseStarted", () -> enraged[0]++);
    left.getEvents().addListener("enragePhaseStarted", () -> enraged[1]++);
    right.getEvents().addListener("enragePhaseStarted", () -> enraged[2]++);

    middle.create();
  }

  @Test
  void shouldUseCombinedHealthInsteadOfMiddleHeadPercentage() {
    middleStats.setHealth(100);

    assertEquals(1, phase.getCurrentPhase());
    assertEquals(0, enraged[0]);
  }

  @Test
  void shouldEnterPhaseTwoAtExactlyHalfCombinedHealth() {
    middleStats.setHealth(125);
    leftStats.setHealth(62);
    rightStats.setHealth(63);

    assertEquals(2, phase.getCurrentPhase());
    assertEquals(1, enraged[0]);
    assertEquals(1, enraged[1]);
    assertEquals(1, enraged[2]);
  }

  @Test
  void shouldStayInPhaseOneAboveHalfHealth() {
    middleStats.setHealth(126);
    leftStats.setHealth(62);
    rightStats.setHealth(63);

    assertEquals(1, phase.getCurrentPhase());
  }

  @Test
  void shouldKeepDeadHeadsInMaximumHealthAndNotifyOnlyLivingHeads() {
    middleStats.setHealth(0);

    assertEquals(2, phase.getCurrentPhase());
    assertEquals(0, enraged[0]);
    assertEquals(1, enraged[1]);
    assertEquals(1, enraged[2]);
  }

  @Test
  void shouldTransitionOnlyOnceEvenAfterHealing() {
    middleStats.setHealth(125);
    leftStats.setHealth(62);
    rightStats.setHealth(63);

    middleStats.setHealth(250);
    leftStats.setHealth(125);
    rightStats.setHealth(125);
    middleStats.setHealth(0);

    assertEquals(2, phase.getCurrentPhase());
    assertEquals(1, enraged[0]);
    assertEquals(1, enraged[1]);
    assertEquals(1, enraged[2]);
  }

  @Test
  void shouldUseCombinedScaledHealthWithoutPrematureEnrage() {
    // Scale each head separately, as the room does for tracked enemies.
    middleStats.scale(3);
    assertEquals(1, phase.getCurrentPhase());

    leftStats.scale(3);
    assertEquals(1, phase.getCurrentPhase());

    rightStats.scale(3);
    assertEquals(1, phase.getCurrentPhase());

    assertEquals(625, middleStats.getMaxHealth());
    assertEquals(312, leftStats.getMaxHealth());
    assertEquals(312, rightStats.getMaxHealth());

    assertEquals(625, middleStats.getHealth());
    assertEquals(312, leftStats.getHealth());
    assertEquals(312, rightStats.getHealth());

    assertEquals(0, enraged[0]);
    assertEquals(0, enraged[1]);
    assertEquals(0, enraged[2]);

    // Total maximum = 1249. Total current health = 625: above half.
    middleStats.takeDamage(312);
    leftStats.takeDamage(156);
    rightStats.takeDamage(156);

    assertEquals(625,
            middleStats.getHealth() + leftStats.getHealth() + rightStats.getHealth());
    assertEquals(1, phase.getCurrentPhase());

    // Total current health = 624: below half.
    middleStats.takeDamage(1);

    assertEquals(2, phase.getCurrentPhase());
    assertEquals(1, enraged[0]);
    assertEquals(1, enraged[1]);
    assertEquals(1, enraged[2]);

    middleStats.takeDamage(1);
    assertEquals(1, enraged[0]);
    assertEquals(1, enraged[1]);
    assertEquals(1, enraged[2]);
  }
}
