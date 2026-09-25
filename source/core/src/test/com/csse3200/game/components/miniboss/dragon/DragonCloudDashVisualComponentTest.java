package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonCloudDashVisualComponentTest {
  private Entity dragon;
  private Entity target;
  private CombatStatsComponent stats;
  private DragonCloudDashComponent dash;
  private DragonCloudDashVisualComponent visual;
  private RenderService renderer;

  @BeforeEach
  void setUp() {
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    target.setPosition(5f, 0f);
    stats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    dash = new DragonCloudDashComponent(target);
    visual = new DragonCloudDashVisualComponent();
    dragon =
        new Entity()
            .addComponent(stats)
            .addComponent(phase)
            .addComponent(dash)
            .addComponent(visual);
    phase.create();
    dash.create();
    visual.create();
  }

  private void step(Vector2 from, Vector2 to) {
    dragon.getEvents().trigger(DragonCloudDashMovementComponent.DASH_STEP, from, to);
  }

  private void startDash() {
    assertTrue(dash.tryAttack());
    dash.update(0.8f);
  }

  @Test
  void shouldShowWarningOnlyDuringWarningState() {
    assertFalse(visual.isWarningVisible());
    assertTrue(dash.tryAttack());
    assertTrue(visual.isWarningVisible());
    dash.update(0.8f);
    assertFalse(visual.isWarningVisible());
    dash.finishDash();
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldKeepWarningOriginAndDirectionLocked() {
    assertTrue(dash.tryAttack());
    Vector2 origin = visual.getWarningOrigin();
    Vector2 direction = visual.getWarningDirection();
    target.setPosition(0f, 10f);
    dragon.setPosition(1f, 1f);
    visual.update(0.2f);
    assertEquals(origin, visual.getWarningOrigin());
    assertEquals(direction, visual.getWarningDirection());
    visual.getWarningDirection().setZero();
    assertEquals(direction, visual.getWarningDirection());
  }

  @Test
  void shouldEmitSmokeByDistanceAndExpireIt() {
    startDash();
    step(new Vector2(), new Vector2(0.125f, 0f));
    assertEquals(0, visual.getPuffCount());
    step(new Vector2(0.125f, 0f), new Vector2(0.5f, 0f));
    assertEquals(2, visual.getPuffCount());
    visual.update(0.4f);
    assertEquals(2, visual.getPuffCount());
    visual.update(0.5f);
    assertEquals(0, visual.getPuffCount());
  }

  @Test
  void shouldNotEmitDuringWarningOrRecovery() {
    assertTrue(dash.tryAttack());
    step(new Vector2(), new Vector2(1f, 0f));
    assertEquals(0, visual.getPuffCount());
    dash.update(0.8f);
    dash.finishDash();
    step(new Vector2(), new Vector2(1f, 0f));
    assertEquals(0, visual.getPuffCount());
  }

  @Test
  void shouldClearSmokeOnDeath() {
    startDash();
    step(new Vector2(), new Vector2(1f, 0f));
    assertTrue(visual.getPuffCount() > 0);
    stats.setHealth(0);
    assertEquals(0, visual.getPuffCount());
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldClearSmokeWhenDashIsCancelled() {
    startDash();
    step(new Vector2(), new Vector2(1f, 0f));
    dash.stop();
    visual.update(0.01f);
    assertEquals(0, visual.getPuffCount());
  }

  @Test
  void shouldIgnoreInvalidTime() {
    startDash();
    step(new Vector2(), new Vector2(0.5f, 0f));
    visual.update(Float.NaN);
    visual.update(-1f);
    visual.update(Float.POSITIVE_INFINITY);
    assertEquals(2, visual.getPuffCount());
  }

  @Test
  void shouldUnregisterAndClearOnDisposal() {
    startDash();
    step(new Vector2(), new Vector2(0.5f, 0f));
    visual.dispose();
    assertEquals(0, visual.getPuffCount());
    verify(renderer).register(visual);
    verify(renderer).unregister(visual);
  }
}
