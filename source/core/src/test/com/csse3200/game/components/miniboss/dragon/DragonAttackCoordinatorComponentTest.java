package com.csse3200.game.components.miniboss.dragon;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DragonAttackCoordinatorComponentTest {
  private CombatStatsComponent stats;
  private CombatStatsComponent targetStats;
  private DragonThunderOrbComponent orb;
  private DragonCloudDashComponent dash;
  private DragonStormZoneComponent storm;
  private DragonAttackCoordinatorComponent coordinator;

  @BeforeEach
  void setUp() {
    stats = new CombatStatsComponent(500, 20);
    targetStats = new CombatStatsComponent(100, 10);
    Entity target = new Entity().addComponent(targetStats);

    DragonPhaseComponent phase = new DragonPhaseComponent();
    orb = mock(DragonThunderOrbComponent.class);
    dash = mock(DragonCloudDashComponent.class);
    storm = mock(DragonStormZoneComponent.class);
    coordinator = new DragonAttackCoordinatorComponent(target);

    new Entity()
        .addComponent(stats)
        .addComponent(phase)
        .addComponent(orb)
        .addComponent(dash)
        .addComponent(storm)
        .addComponent(coordinator);

    phase.create();
    coordinator.create();
  }

  @Test
  void shouldWaitBeforeFirstAttack() {
    coordinator.update(0.5f);

    verify(orb, never()).tryAttack();
    verify(dash, never()).tryAttack();
    verify(storm, never()).tryAttack();
  }

  @Test
  void shouldRunPhaseOneInOrderWithRecoveryWindows() {
    when(orb.tryAttack()).thenReturn(true);
    when(dash.tryAttack()).thenReturn(true);
    when(storm.tryAttack()).thenReturn(true);

    coordinator.update(1f);
    verify(orb).tryAttack();

    when(orb.isBusy()).thenReturn(true);
    coordinator.update(5f);
    verify(dash, never()).tryAttack();

    when(orb.isBusy()).thenReturn(false);
    coordinator.update(0.1f);

    coordinator.update(0.5f);
    verify(dash, never()).tryAttack();

    coordinator.update(0.5f);
    verify(dash).tryAttack();

    when(dash.getState()).thenReturn(DragonCloudDashComponent.State.RECOVERING);
    coordinator.update(5f);
    verify(storm, never()).tryAttack();

    when(dash.getState()).thenReturn(DragonCloudDashComponent.State.READY);
    coordinator.update(0.1f);
    coordinator.update(0.1f);
    verify(storm).tryAttack();

    when(storm.isBusy()).thenReturn(false);
    coordinator.update(0.1f);
    coordinator.update(1f);

    InOrder order = inOrder(orb, dash, storm);
    order.verify(orb).tryAttack();
    order.verify(dash).tryAttack();
    order.verify(storm).tryAttack();
    order.verify(orb).tryAttack();
  }

  @Test
  void shouldWaitForBothComboSkillsBeforeDashing() {
    stats.setHealth(250);
    when(orb.tryAttack()).thenReturn(true);
    when(storm.tryAttack()).thenReturn(true);
    when(dash.tryAttack()).thenReturn(true);

    coordinator.update(1f);
    verify(orb).tryAttack();
    verify(storm).tryAttack();
    verify(dash, never()).tryAttack();

    when(orb.isBusy()).thenReturn(true);
    coordinator.update(5f);
    verify(dash, never()).tryAttack();

    when(orb.isBusy()).thenReturn(false);
    when(storm.isBusy()).thenReturn(true);
    coordinator.update(5f);
    verify(dash, never()).tryAttack();

    when(storm.isBusy()).thenReturn(false);
    coordinator.update(0.1f);
    coordinator.update(0.5f);
    verify(dash, never()).tryAttack();

    coordinator.update(0.25f);
    verify(dash).tryAttack();

    when(dash.getState()).thenReturn(DragonCloudDashComponent.State.READY);
    coordinator.update(0.1f);
    coordinator.update(0.1f);

    verify(orb, times(2)).tryAttack();
    verify(storm, times(2)).tryAttack();
  }

  @Test
  void shouldFinishCurrentAttackBeforeChangingPhasePlan() {
    when(orb.tryAttack()).thenReturn(true);
    when(orb.isBusy()).thenReturn(true);

    coordinator.update(1f);
    stats.setHealth(250);
    coordinator.update(5f);

    verify(storm, never()).tryAttack();
    verify(orb, times(1)).tryAttack();

    when(orb.isBusy()).thenReturn(false);
    coordinator.update(0.1f);

    when(storm.tryAttack()).thenReturn(true);
    coordinator.update(0.75f);

    verify(orb, times(2)).tryAttack();
    verify(storm).tryAttack();
    verify(dash, never()).tryAttack();
  }

  @Test
  void shouldRetryOnlyTheUnavailableComboSkill() {
    stats.setHealth(250);
    when(orb.tryAttack()).thenReturn(false, true);
    when(storm.tryAttack()).thenReturn(true);
    when(orb.isBusy()).thenReturn(true);

    coordinator.update(1f);
    coordinator.update(0.1f);

    verify(orb, times(2)).tryAttack();
    verify(storm, times(1)).tryAttack();
    verify(dash, never()).tryAttack();
  }

  @Test
  void shouldCancelAllSkillsOnceWhenDragonDies() {
    stats.setHealth(0);
    coordinator.update(1f);
    coordinator.dispose();

    verify(orb, times(1)).stop();
    verify(dash, times(1)).stop();
    verify(storm, times(1)).stop();
    verify(orb, never()).tryAttack();
  }

  @Test
  void shouldCancelWhenPlayerDies() {
    targetStats.setHealth(0);
    coordinator.update(0.1f);

    verify(orb).stop();
    verify(dash).stop();
    verify(storm).stop();
  }

  @Test
  void shouldIgnoreInvalidDelta() {
    coordinator.update(Float.NaN);
    coordinator.update(Float.POSITIVE_INFINITY);
    coordinator.update(-1f);
    coordinator.update(0f);

    verify(orb, never()).tryAttack();
    verify(dash, never()).tryAttack();
    verify(storm, never()).tryAttack();
  }
}
