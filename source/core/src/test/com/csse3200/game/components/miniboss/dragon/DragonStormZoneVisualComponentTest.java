package com.csse3200.game.components.miniboss.dragon;

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
class DragonStormZoneVisualComponentTest {
  private CombatStatsComponent stats;
  private DragonStormZoneComponent storm;
  private DragonStormZoneVisualComponent visual;
  private RenderService renderer;

  @BeforeEach
  void setUp() {
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);

    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    target.setPosition(5f, 0f);

    stats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    storm = new DragonStormZoneComponent(target);
    visual = new DragonStormZoneVisualComponent();

    new Entity().addComponent(stats).addComponent(phase).addComponent(storm).addComponent(visual);

    phase.create();
    storm.create();
    visual.create();
  }

  @Test
  void shouldRemainHiddenBeforeAttack() {
    assertFalse(visual.isEffectVisible());
  }

  @Test
  void shouldDisplayWarningAndStrikeUntilZoneExpires() {
    assertTrue(storm.tryAttack());
    assertTrue(visual.isEffectVisible());

    storm.update(1f);

    assertTrue(storm.getZones().get(0).struck());
    assertTrue(visual.isEffectVisible());

    storm.update(0.25f);

    assertFalse(visual.isEffectVisible());
  }

  @Test
  void shouldHideWhenDragonDies() {
    assertTrue(storm.tryAttack());

    stats.setHealth(0);

    assertFalse(visual.isEffectVisible());
  }

  @Test
  void shouldHideWhenStormIsCancelled() {
    assertTrue(storm.tryAttack());

    storm.stop();

    assertFalse(visual.isEffectVisible());
  }

  @Test
  void shouldRegisterAndUnregisterWithRenderer() {
    verify(renderer).register(visual);

    visual.dispose();

    assertFalse(visual.isEffectVisible());
    verify(renderer).unregister(visual);
  }
}
