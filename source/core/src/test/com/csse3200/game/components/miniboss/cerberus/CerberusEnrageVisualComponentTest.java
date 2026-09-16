package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerberusEnrageVisualComponentTest {
  private RenderService renderService;

  private Entity left;
  private Entity middle;
  private Entity right;

  private CombatStatsComponent leftStats;
  private CombatStatsComponent middleStats;

  private CerberusPhaseComponent phase;
  private CerberusEnrageVisualComponent leftAura;
  private CerberusEnrageVisualComponent middleAura;
  private CerberusEnrageVisualComponent rightAura;

  @BeforeEach
  void setUp() {
    renderService = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderService);

    leftStats = new CombatStatsComponent(125, 10);
    middleStats = new CombatStatsComponent(250, 20);

    left = new Entity().addComponent(leftStats);
    right = new Entity().addComponent(new CombatStatsComponent(125, 10));

    phase = new CerberusPhaseComponent(left, right);

    leftAura = new CerberusEnrageVisualComponent(phase);
    middleAura = new CerberusEnrageVisualComponent(phase);
    rightAura = new CerberusEnrageVisualComponent(phase);

    left.addComponent(leftAura);
    right.addComponent(rightAura);
    middle = new Entity().addComponent(middleStats).addComponent(phase).addComponent(middleAura);

    left.create();
    right.create();
    middle.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  private void enterPhaseTwo() {
    middleStats.setHealth(124);
    leftStats.setHealth(1);

    assertEquals(2, phase.getCurrentPhase());
  }

  @Test
  void shouldRemainHiddenAndNotDrawInPhaseOne() {
    SpriteBatch batch = mock(SpriteBatch.class);

    leftAura.render(batch);
    middleAura.render(batch);
    rightAura.render(batch);

    assertEquals(1, phase.getCurrentPhase());
    assertFalse(leftAura.isAuraActive());
    assertFalse(middleAura.isAuraActive());
    assertFalse(rightAura.isAuraActive());
    verifyNoInteractions(batch);
  }

  @Test
  void shouldActivateAllLivingHeadsAtHalfCombinedHealth() {
    middleStats.setHealth(126);

    assertFalse(leftAura.isAuraActive());
    assertFalse(middleAura.isAuraActive());
    assertFalse(rightAura.isAuraActive());

    enterPhaseTwo();

    assertTrue(leftAura.isAuraActive());
    assertTrue(middleAura.isAuraActive());
    assertTrue(rightAura.isAuraActive());

    verify(renderService).register(leftAura);
    verify(renderService).register(middleAura);
    verify(renderService).register(rightAura);
  }

  @Test
  void shouldDisableOnlyTheHeadThatDies() {
    enterPhaseTwo();

    leftStats.setHealth(0);

    assertFalse(leftAura.isAuraActive());
    assertTrue(middleAura.isAuraActive());
    assertTrue(rightAura.isAuraActive());

    SpriteBatch batch = mock(SpriteBatch.class);
    leftAura.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void shouldNotActivateDeadMiddleHeadWhenItsDeathTriggersPhaseTwo() {
    middleStats.setHealth(0);

    assertEquals(2, phase.getCurrentPhase());
    assertFalse(middleAura.isAuraActive());
    assertTrue(leftAura.isAuraActive());
    assertTrue(rightAura.isAuraActive());

    middle.getEvents().trigger("enragePhaseStarted");

    assertFalse(middleAura.isAuraActive());
  }

  @Test
  void shouldActivateWhenCreatedAfterPhaseTwoHasStarted() {
    enterPhaseTwo();

    CerberusEnrageVisualComponent lateAura = new CerberusEnrageVisualComponent(phase);
    Entity lateHead =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(lateAura);
    lateHead.create();

    assertTrue(lateAura.isAuraActive());
    verify(renderService).register(lateAura);
  }

  @Test
  void shouldKeepAuraActiveAfterHealingInPhaseTwo() {
    enterPhaseTwo();

    middleStats.setHealth(250);
    leftStats.setHealth(125);

    assertEquals(2, phase.getCurrentPhase());
    assertTrue(leftAura.isAuraActive());
    assertTrue(middleAura.isAuraActive());
    assertTrue(rightAura.isAuraActive());
  }

  @Test
  void shouldUnregisterAndNeverReactivateAfterDisposal() {
    enterPhaseTwo();

    leftAura.dispose();

    assertFalse(leftAura.isAuraActive());
    verify(renderService).unregister(leftAura);

    left.getEvents().trigger("enragePhaseStarted");

    assertFalse(leftAura.isAuraActive());
    assertTrue(middleAura.isAuraActive());
    assertTrue(rightAura.isAuraActive());

    SpriteBatch batch = mock(SpriteBatch.class);
    leftAura.render(batch);

    verifyNoInteractions(batch);
  }
}
