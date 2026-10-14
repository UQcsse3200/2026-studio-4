package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DragonPhaseComponentTest {
  private CombatStatsComponent combatStatsComponent;
  private DragonPhaseComponent dragonPhaseComponent;
  private Entity dragon;
  private int enrageEvents;

  @BeforeEach
  void setUp() {
    combatStatsComponent = new CombatStatsComponent(500, 20);
    dragonPhaseComponent = new DragonPhaseComponent();
    dragon = new Entity().addComponent(combatStatsComponent).addComponent(dragonPhaseComponent);
    dragon.getEvents().addListener(DragonPhaseComponent.ENRAGE_STARTED, () -> enrageEvents++);
  }

  @Test
  void shouldStartInPhaseOneAtFullHealth() {
    dragon.create();

    assertEquals(1, dragonPhaseComponent.getCurrentPhase());
    assertFalse(dragonPhaseComponent.isEnraged());
    assertEquals(0, enrageEvents);
  }

  @Test
  void shouldStayInPhaseOneAboveHalfHealth() {
    assertPhaseAfterHealthChanges(251, 1, 0);
  }

  @Test
  void shouldEnterPhaseTwoAtExactlyHalfHealth() {
    assertPhaseAfterHealthChanges(250, 2, 1);
  }

  @Test
  void shouldEnterPhaseTwoWhenDamageSkipsPastHalfHealth() {
    assertPhaseAfterHealthChanges(100, 2, 1);
  }

  @Test
  void shouldNotEnrageWhenKilledDirectly() {
    assertPhaseAfterHealthChanges(0, 1, 0);
  }

  private void assertPhaseAfterHealthChanges(int health, int expectedPhase, int expectedEvents) {
    dragon.create();
    combatStatsComponent.setHealth(health);

    assertEquals(expectedPhase, dragonPhaseComponent.getCurrentPhase());
    assertEquals(expectedPhase == 2, dragonPhaseComponent.isEnraged());
    assertEquals(expectedEvents, enrageEvents);
  }

  @Test
  void shouldRemainEnragedAndNotifyOnlyOnceAfterHealing() {
    dragon.create();

    combatStatsComponent.setHealth(250);
    combatStatsComponent.setHealth(500);

    assertTrue(dragonPhaseComponent.isEnraged());

    combatStatsComponent.setHealth(200);

    assertEquals(2, dragonPhaseComponent.getCurrentPhase());
    assertEquals(1, enrageEvents);
  }

  @Test
  void shouldCheckInitialHealthOnCreation() {
    combatStatsComponent.setHealth(200);

    dragon.create();

    assertEquals(2, dragonPhaseComponent.getCurrentPhase());
    assertEquals(1, enrageEvents);
  }

  @Test
  void shouldUseUpdatedMaximumHealth() {
    dragon.create();

    combatStatsComponent.setMaxHealth(1000);

    assertEquals(2, dragonPhaseComponent.getCurrentPhase());
    assertEquals(1, enrageEvents);
  }

  @Test
  void shouldHandleOddMaximumHealth() {
    combatStatsComponent.setMaxHealth(501);
    combatStatsComponent.setHealth(251);
    dragon.create();

    assertEquals(1, dragonPhaseComponent.getCurrentPhase());

    combatStatsComponent.setHealth(250);

    assertEquals(2, dragonPhaseComponent.getCurrentPhase());
    assertEquals(1, enrageEvents);
  }

  @Test
  void shouldNotEnrageWithZeroMaximumHealth() {
    combatStatsComponent.setMaxHealth(0);
    combatStatsComponent.setHealth(0);

    dragon.create();

    assertEquals(1, dragonPhaseComponent.getCurrentPhase());
    assertEquals(0, enrageEvents);
  }

  @Test
  void shouldRequireCombatStats() {
    Entity invalidDragon = new Entity().addComponent(new DragonPhaseComponent());

    assertThrows(IllegalStateException.class, invalidDragon::create);
  }

  @Test
  void shouldRemainInPhaseOneAfterScalingUntilHalfHealth() {
    CombatStatsComponent stats = new CombatStatsComponent(200, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    Entity scaledDragon = new Entity().addComponent(stats).addComponent(phase);
    int[] events = {0};

    scaledDragon.getEvents().addListener(DragonPhaseComponent.ENRAGE_STARTED, () -> events[0]++);
    scaledDragon.create();

    // Three completed dungeons: 200 -> 500 health.
    stats.scale(3);

    assertEquals(500, stats.getMaxHealth());
    assertEquals(500, stats.getHealth());
    assertEquals(1, phase.getCurrentPhase());
    assertEquals(0, events[0]);

    stats.takeDamage(249);

    assertEquals(251, stats.getHealth());
    assertEquals(1, phase.getCurrentPhase());
    assertEquals(0, events[0]);

    stats.takeDamage(1);

    assertEquals(250, stats.getHealth());
    assertEquals(2, phase.getCurrentPhase());
    assertEquals(1, events[0]);
  }
}
