package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonEnrageVisualComponentTest {
  private CombatStatsComponent stats;
  private DragonEnrageVisualComponent visual;
  private RenderService renderer;

  @BeforeEach
  void setUp() {
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);

    stats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    visual = new DragonEnrageVisualComponent();

    new Entity().addComponent(stats).addComponent(phase).addComponent(visual);

    phase.create();
    visual.create();
  }

  @Test
  void shouldRemainHiddenInPhaseOne() {
    assertFalse(visual.isAuraActive());

    stats.setHealth(251);

    assertFalse(visual.isAuraActive());
  }

  @Test
  void shouldActivateAtHalfHealthAndRemainAfterHealing() {
    stats.setHealth(250);
    assertTrue(visual.isAuraActive());

    stats.setHealth(500);
    assertTrue(visual.isAuraActive());
  }

  @Test
  void shouldHideOnDeath() {
    stats.setHealth(250);
    assertTrue(visual.isAuraActive());

    stats.setHealth(0);

    assertFalse(visual.isAuraActive());
  }

  @Test
  void shouldPulseOverTime() {
    stats.setHealth(250);

    assertEquals(0.55f, visual.getOpacity(), 0.001f);

    visual.update(0.3f);
    assertEquals(0.75f, visual.getOpacity(), 0.001f);

    visual.update(0.6f);
    assertEquals(0.35f, visual.getOpacity(), 0.001f);
  }

  @Test
  void shouldNotAdvancePulseBeforeEnrage() {
    visual.update(0.3f);

    assertEquals(0.55f, visual.getOpacity(), 0.001f);
  }

  @Test
  void shouldIgnoreInvalidTime() {
    stats.setHealth(250);

    visual.update(Float.NaN);
    visual.update(Float.POSITIVE_INFINITY);
    visual.update(-1f);

    assertEquals(0.55f, visual.getOpacity(), 0.001f);
  }

  @Test
  void shouldUnregisterAndStayHiddenAfterDisposal() {
    stats.setHealth(250);

    visual.dispose();

    assertFalse(visual.isAuraActive());
    verify(renderer).register(visual);
    verify(renderer).unregister(visual);
  }
}
