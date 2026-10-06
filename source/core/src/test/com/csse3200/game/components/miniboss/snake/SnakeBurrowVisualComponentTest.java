package com.csse3200.game.components.miniboss.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SnakeBurrowVisualComponentTest {
  private SnakeBurrowVisualComponent visual;
  private Entity snake;
  private RenderService renderer;

  @BeforeEach
  void setUp() {
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    visual = new SnakeBurrowVisualComponent();
    snake = new Entity().addComponent(visual);
  }

  @Test
  void shouldAcceptEffectsBeforeRenderRegistration() {
    visual.startBurrow(new Vector2());
    visual.addTrail(new Vector2(1f, 0f));
    visual.showWarning(new Vector2(2f, 0f), 1f);
    assertTrue(visual.getParticleCount() > 0);
    assertTrue(visual.isWarningVisible());
    verifyNoInteractions(renderer);
  }

  @Test
  void shouldKeepWarningAtTheLockedCentreAndDamageRadius() {
    Vector2 attackCentre = new Vector2(3f, 4f);
    visual.showWarning(attackCentre, 1.25f);
    attackCentre.setZero();
    visual.getWarningCentre().setZero();
    visual.update(0.5f);
    assertEquals(new Vector2(3f, 4f), visual.getWarningCentre());
    assertEquals(1.25f, visual.getWarningRadius());
    visual.hideWarning();
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldExpireDustUsingTheRegisteredGameClock() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    visual.startEmergence(new Vector2());
    when(time.getDeltaTime()).thenReturn(0f);
    visual.update();
    assertTrue(visual.getParticleCount() > 0);
    when(time.getDeltaTime()).thenReturn(1f);
    visual.update();
    assertEquals(0, visual.getParticleCount());
  }

  @Test
  void shouldBoundRepeatedTrailEmissions() {
    for (int i = 0; i < 100; i++) {
      visual.addTrail(new Vector2((float) i, 0f));
    }
    assertTrue(visual.getParticleCount() <= 128);
    visual.update(1f);
    assertEquals(0, visual.getParticleCount());
  }

  @Test
  void shouldIgnoreInvalidTimeAndClearActiveEffects() {
    visual.startBurrow(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    int initialCount = visual.getParticleCount();
    visual.update(Float.NaN);
    visual.update(-1f);
    visual.update(Float.POSITIVE_INFINITY);
    assertEquals(initialCount, visual.getParticleCount());
    visual.clear();
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldClearAndStopEmittingOnDeath() {
    visual.create();
    visual.startBurrow(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    snake.getEvents().trigger("entityDied");
    visual.startEmergence(new Vector2());
    visual.addTrail(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
  }

  @Test
  void shouldClearAndUnregisterWhenDisposedBeforeFirstDraw() {
    visual.create();
    visual.startEmergence(new Vector2());
    visual.showWarning(new Vector2(), 1f);
    visual.dispose();
    assertEquals(0, visual.getParticleCount());
    assertFalse(visual.isWarningVisible());
    verify(renderer).register(visual);
    verify(renderer).unregister(visual);
  }
}
