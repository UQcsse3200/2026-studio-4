package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerberusAttackCoordinatorTest {
  private Entity left;
  private Entity middle;
  private Entity right;

  private CombatStatsComponent leftStats;
  private CombatStatsComponent middleStats;

  private CerberusPhaseComponent phase;
  private CerberusAttackCoordinator coordinator;

  private boolean leftReady;
  private boolean middleReady;
  private boolean rightReady;

  @BeforeEach
  void setUp() {
    leftStats = new CombatStatsComponent(125, 10);
    middleStats = new CombatStatsComponent(250, 20);

    left = new Entity().addComponent(leftStats);
    right = new Entity().addComponent(new CombatStatsComponent(125, 10));

    phase = new CerberusPhaseComponent(left, right);
    middle = new Entity().addComponent(middleStats).addComponent(phase);
    middle.create();

    coordinator = new CerberusAttackCoordinator(left, middle, right, phase);

    leftReady = true;
    middleReady = true;
    rightReady = true;

    coordinator.register(left, () -> leftReady);
    coordinator.register(middle, () -> middleReady);
    coordinator.register(right, () -> rightReady);
  }

  @Test
  void shouldAttackInOrderRegardlessOfRequestOrder() {
    assertFalse(coordinator.tryStart(right));
    assertFalse(coordinator.tryStart(middle));
    assertTrue(coordinator.tryStart(left));

    coordinator.finish(left);

    assertFalse(coordinator.tryStart(left));
    assertFalse(coordinator.tryStart(right));
    assertTrue(coordinator.tryStart(middle));

    coordinator.finish(middle);

    assertFalse(coordinator.tryStart(left));
    assertTrue(coordinator.tryStart(right));

    coordinator.finish(right);

    assertTrue(coordinator.tryStart(left));
  }

  @Test
  void shouldBlockOtherHeadsUntilCurrentAttackFinishes() {
    assertTrue(coordinator.tryStart(left));

    assertFalse(coordinator.tryStart(left));
    assertFalse(coordinator.tryStart(middle));
    assertFalse(coordinator.tryStart(right));

    coordinator.finish(right);

    assertFalse(coordinator.tryStart(middle));

    coordinator.finish(left);

    assertTrue(coordinator.tryStart(middle));
  }

  @Test
  void shouldSkipHeadsThatAreNotReady() {
    leftReady = false;

    assertFalse(coordinator.tryStart(left));
    assertTrue(coordinator.tryStart(middle));

    coordinator.finish(middle);

    rightReady = false;
    leftReady = true;

    assertFalse(coordinator.tryStart(right));
    assertTrue(coordinator.tryStart(left));
  }

  @Test
  void shouldWaitWhenNoHeadIsReadyAndRecoverLater() {
    leftReady = false;
    middleReady = false;
    rightReady = false;

    assertFalse(coordinator.tryStart(left));
    assertFalse(coordinator.tryStart(middle));
    assertFalse(coordinator.tryStart(right));

    rightReady = true;

    assertTrue(coordinator.tryStart(right));
  }

  @Test
  void shouldSkipDeadHeadEvenWhenItsSkillReportsReady() {
    leftStats.setHealth(0);

    assertEquals(1, phase.getCurrentPhase());
    assertFalse(coordinator.tryStart(left));
    assertTrue(coordinator.tryStart(middle));
  }

  @Test
  void shouldReleaseTurnWhenAttackingHeadDies() {
    assertTrue(coordinator.tryStart(left));

    leftStats.setHealth(0);

    assertEquals(1, phase.getCurrentPhase());
    assertTrue(coordinator.tryStart(middle));
    assertFalse(coordinator.tryStart(left));
  }

  @Test
  void shouldAllowConcurrentAttacksInPhaseTwoWithoutRestartingActiveAttack() {
    assertTrue(coordinator.tryStart(left));
    middleStats.setHealth(125);
    leftStats.setHealth(1);
    middleStats.setHealth(124);

    assertEquals(2, phase.getCurrentPhase());

    assertFalse(coordinator.tryStart(left));
    assertTrue(coordinator.tryStart(middle));
    assertTrue(coordinator.tryStart(right));

    assertFalse(coordinator.tryStart(middle));
    assertFalse(coordinator.tryStart(right));

    coordinator.finish(left);

    assertTrue(coordinator.tryStart(left));
  }

  @Test
  void shouldStillRequireReadinessAndLivingHeadInPhaseTwo() {
    middleStats.setHealth(0);

    assertEquals(2, phase.getCurrentPhase());

    leftReady = false;

    assertFalse(coordinator.tryStart(left));
    assertFalse(coordinator.tryStart(middle));
    assertTrue(coordinator.tryStart(right));

    leftReady = true;

    assertTrue(coordinator.tryStart(left));
  }
}
