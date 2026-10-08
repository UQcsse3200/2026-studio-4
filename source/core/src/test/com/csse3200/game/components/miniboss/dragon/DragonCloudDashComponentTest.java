package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonCloudDashComponentTest {
  private Entity dragon;
  private Entity target;
  private CombatStatsComponent dragonStats;
  private CombatStatsComponent targetStats;
  private DragonCloudDashComponent dash;

  @BeforeEach
  void setUp() {
    dragonStats = new CombatStatsComponent(500, 20);
    targetStats = new CombatStatsComponent(100, 10);

    target = new Entity().addComponent(targetStats);
    target.setPosition(5f, 0f);

    DragonPhaseComponent phase = new DragonPhaseComponent();
    dash = new DragonCloudDashComponent(target);
    dragon = new Entity().addComponent(dragonStats).addComponent(phase).addComponent(dash);

    phase.create();
    dash.create();
  }

  @Test
  void shouldWaitForWarningBeforeDashing() {
    assertTrue(dash.tryAttack());
    assertEquals(DragonCloudDashComponent.State.WARNING, dash.getState());

    dash.update(0.4f);
    assertEquals(DragonCloudDashComponent.State.WARNING, dash.getState());

    dash.update(0.4f);
    assertEquals(DragonCloudDashComponent.State.DASHING, dash.getState());
    assertEquals(new Vector2(), dragon.getPosition());
  }

  @Test
  void shouldKeepDirectionLockedWhenTargetMoves() {
    assertTrue(dash.tryAttack());

    target.setPosition(0f, 5f);
    dash.update(0.8f);

    assertEquals(1f, dash.getLockedDirection().x, 0.001f);
    assertEquals(0f, dash.getLockedDirection().y, 0.001f);
  }

  @Test
  void shouldProtectLockedDirectionFromExternalChanges() {
    assertTrue(dash.tryAttack());

    dash.getLockedDirection().set(0f, -1f);

    assertEquals(new Vector2(1f, 0f), dash.getLockedDirection());
  }

  @Test
  void shouldRejectAnotherAttackUntilRecoveryFinishes() {
    assertTrue(dash.tryAttack());
    assertFalse(dash.tryAttack());

    dash.update(0.8f);
    assertFalse(dash.tryAttack());

    dash.finishDash();
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());
    assertFalse(dash.tryAttack());

    dash.update(0.5f);
    assertFalse(dash.tryAttack());

    dash.update(0.5f);
    assertEquals(DragonCloudDashComponent.State.READY, dash.getState());
    assertTrue(dash.tryAttack());
  }

  @Test
  void shouldUseShorterRecoveryWhenEnraged() {
    dragonStats.setHealth(250);

    assertTrue(dash.tryAttack());
    dash.update(0.8f);
    dash.finishDash();

    dash.update(0.5f);
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());

    dash.update(0.25f);
    assertEquals(DragonCloudDashComponent.State.READY, dash.getState());
  }

  @Test
  void shouldNotSkipWarningWhenFinishIsCalledEarly() {
    assertTrue(dash.tryAttack());

    dash.finishDash();

    assertEquals(DragonCloudDashComponent.State.WARNING, dash.getState());
  }

  @Test
  void shouldStopImmediatelyWhenDragonDies() {
    assertTrue(dash.tryAttack());

    dragonStats.setHealth(0);

    assertEquals(DragonCloudDashComponent.State.STOPPED, dash.getState());
    assertFalse(dash.tryAttack());
  }

  @Test
  void shouldCancelWhenTargetDies() {
    assertTrue(dash.tryAttack());

    targetStats.setHealth(0);
    dash.update(0.1f);

    assertEquals(DragonCloudDashComponent.State.STOPPED, dash.getState());
  }

  @Test
  void shouldRejectOverlappingTarget() {
    target.setPosition(dragon.getPosition());

    assertFalse(dash.tryAttack());
    assertEquals(DragonCloudDashComponent.State.READY, dash.getState());
  }

  @Test
  void shouldIgnoreInvalidDelta() {
    assertTrue(dash.tryAttack());

    dash.update(Float.NaN);
    dash.update(Float.POSITIVE_INFINITY);
    dash.update(-1f);
    dash.update(0f);

    assertEquals(DragonCloudDashComponent.State.WARNING, dash.getState());

    dash.update(0.8f);
    assertEquals(DragonCloudDashComponent.State.DASHING, dash.getState());
  }

  @Test
  void shouldRemainStoppedAfterDisposal() {
    assertTrue(dash.tryAttack());

    dash.dispose();
    dash.update(10f);

    assertEquals(DragonCloudDashComponent.State.STOPPED, dash.getState());
    assertFalse(dash.tryAttack());
  }
}
