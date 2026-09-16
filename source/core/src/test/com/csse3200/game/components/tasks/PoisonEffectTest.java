package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PoisonEffectTest {

  private GameTime gameTime;
  private CombatStatsComponent target;

  @BeforeEach
  void setUp() {
    gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);

    target = new CombatStatsComponent(100, 5);
  }

  @Test
  void shouldNotDealDamageBeforeFirstTickInterval() {
    when(gameTime.getTime()).thenReturn(0L);
    PoisonEffect effect = new PoisonEffect(target, 2, 1000, 4000);

    when(gameTime.getTime()).thenReturn(500L);
    boolean finished = effect.update();

    assertEquals(100, target.getHealth());
    assertFalse(finished);
  }

  @Test
  void shouldDealDamageOnceIntervalElapses() {
    when(gameTime.getTime()).thenReturn(0L);
    PoisonEffect effect = new PoisonEffect(target, 2, 1000, 4000);

    when(gameTime.getTime()).thenReturn(1000L);
    effect.update();

    assertEquals(98, target.getHealth());
  }

  @Test
  void shouldDealDamageMultipleTimesOverDuration() {
    when(gameTime.getTime()).thenReturn(0L);
    PoisonEffect effect = new PoisonEffect(target, 2, 1000, 4000);

    when(gameTime.getTime()).thenReturn(1000L);
    effect.update();
    when(gameTime.getTime()).thenReturn(2000L);
    effect.update();
    when(gameTime.getTime()).thenReturn(3000L);
    effect.update();

    assertEquals(94, target.getHealth());
  }

  @Test
  void shouldFinishOnceTotalDurationHasElapsed() {
    when(gameTime.getTime()).thenReturn(0L);
    PoisonEffect effect = new PoisonEffect(target, 2, 1000, 4000);

    when(gameTime.getTime()).thenReturn(4000L);
    boolean finished = effect.update();

    assertTrue(finished);
  }

  @Test
  void shouldNotFinishBeforeTotalDurationHasElapsed() {
    when(gameTime.getTime()).thenReturn(0L);
    PoisonEffect effect = new PoisonEffect(target, 2, 1000, 4000);

    when(gameTime.getTime()).thenReturn(3000L);
    boolean finished = effect.update();

    assertFalse(finished);
  }
}
