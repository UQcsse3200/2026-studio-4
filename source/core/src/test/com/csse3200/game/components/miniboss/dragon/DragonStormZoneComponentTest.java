package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonStormZoneComponentTest {
  private Entity target;
  private CombatStatsComponent targetStats;
  private CombatStatsComponent ownerStats;
  private DragonStormZoneComponent storm;

  @BeforeEach
  void setUp() {
    targetStats = new CombatStatsComponent(100, 10);
    target = new Entity().addComponent(targetStats);
    target.setScale(1f, 1f);
    target.setPosition(5f, 0f);
    ownerStats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    storm = new DragonStormZoneComponent(target);
    new Entity().addComponent(ownerStats).addComponent(phase).addComponent(storm);
    phase.create();
    storm.create();
  }

  @Test
  void shouldWarnForOneSecondThenDamageOnlyOnce() {
    assertTrue(storm.tryAttack());
    storm.update(0.5f);
    assertEquals(100, targetStats.getHealth());
    storm.update(0.5f);
    assertEquals(90, targetStats.getHealth());
    assertTrue(storm.getZones().get(0).struck());
    storm.update(1f);
    assertEquals(90, targetStats.getHealth());
    assertFalse(storm.isBusy());
  }

  @Test
  void shouldKeepZoneFixedAndAllowDodging() {
    assertTrue(storm.tryAttack());
    target.setPosition(10f, 0f);
    assertEquals(5.5f, storm.getZones().get(0).x(), 0.001f);
    storm.update(1f);
    assertEquals(100, targetStats.getHealth());
  }

  @Test
  void shouldHitPlayerWhoseHitboxOverlapsCircleEdge() {
    assertTrue(storm.tryAttack());
    target.setPosition(6.4f, 0f);
    storm.update(1f);
    assertEquals(90, targetStats.getHealth());
  }

  @Test
  void shouldCreateTwoSeparatelyWarnedZonesInPhaseTwo() {
    ownerStats.setHealth(250);
    assertTrue(storm.tryAttack());
    target.setPosition(10f, 0f);
    storm.update(0.35f);
    assertEquals(2, storm.getZones().size());
    assertEquals(10.5f, storm.getZones().get(1).x(), 0.001f);
    assertEquals(0f, storm.getZones().get(1).progress(), 0.001f);
    storm.update(0.5f);
    assertEquals(100, targetStats.getHealth());
    storm.update(0.5f);
    assertEquals(90, targetStats.getHealth());
  }

  @Test
  void shouldGiveNewSecondZoneFullWarningAfterLargeDelta() {
    ownerStats.setHealth(250);
    assertTrue(storm.tryAttack());
    storm.update(5f);
    assertEquals(90, targetStats.getHealth());
    assertEquals(0f, storm.getZones().get(1).progress(), 0.001f);
    storm.update(0.5f);
    assertEquals(90, targetStats.getHealth());
    storm.update(0.5f);
    assertEquals(80, targetStats.getHealth());
  }

  @Test
  void shouldKeepCurrentCastUnchangedWhenPhaseChanges() {
    assertTrue(storm.tryAttack());
    ownerStats.setHealth(250);
    storm.update(0.4f);
    assertEquals(1, storm.getZones().size());
  }

  @Test
  void shouldWaitForCooldownBeforeNextCast() {
    assertTrue(storm.tryAttack());
    assertFalse(storm.tryAttack());
    storm.update(1f);
    storm.update(0.5f);
    assertFalse(storm.tryAttack());
    storm.update(1f);
    assertTrue(storm.tryAttack());
  }

  @Test
  void shouldCancelAllZonesWhenOwnerDies() {
    ownerStats.setHealth(250);
    assertTrue(storm.tryAttack());
    ownerStats.setHealth(0);
    storm.update(2f);
    assertFalse(storm.isBusy());
    assertFalse(storm.tryAttack());
    assertEquals(100, targetStats.getHealth());
  }

  @Test
  void shouldCancelWhenTargetDies() {
    assertTrue(storm.tryAttack());
    targetStats.setHealth(0);
    storm.update(1f);
    assertFalse(storm.isBusy());
  }

  @Test
  void shouldIgnoreInvalidTimeAndCancelOnDisposal() {
    assertTrue(storm.tryAttack());
    storm.update(Float.NaN);
    storm.update(Float.POSITIVE_INFINITY);
    storm.update(-1f);
    assertEquals(0f, storm.getZones().get(0).progress(), 0.001f);
    assertEquals(100, targetStats.getHealth());
    storm.dispose();
    assertFalse(storm.isBusy());
    assertFalse(storm.tryAttack());
  }
}
