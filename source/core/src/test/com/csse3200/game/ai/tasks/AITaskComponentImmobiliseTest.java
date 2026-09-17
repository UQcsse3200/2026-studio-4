package com.csse3200.game.ai.tasks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A frozen enemy decides nothing. The task list is what would otherwise chase, wander or fire, so
 * one guard here covers every AI-driven enemy rather than each task learning to hold still.
 */
@ExtendWith(GameExtension.class)
class AITaskComponentImmobiliseTest {
  private GameTime time;
  private StatusEffectsControllerComponent effects;
  private AITaskComponent ai;
  private PriorityTask task;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);

    effects = new StatusEffectsControllerComponent();
    task = mock(PriorityTask.class);
    when(task.getPriority()).thenReturn(10);
    ai = new AITaskComponent(new Entity());

    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(effects)
            .addComponent(ai);
    ai.addTask(task);
    enemy.create();
  }

  @Test
  void runsItsHighestPriorityTaskNormally() {
    ai.update();

    verify(task).start();
    verify(task).update();
  }

  @Test
  void aFrozenEnemyNeitherStartsNorRunsAnyTask() {
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    ai.update();

    verify(task, never()).start();
    verify(task, never()).update();
  }

  @Test
  void aFrozenEnemyStopsPartwayThroughWhateverItWasDoing() {
    ai.update();
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    ai.update();
    ai.update();

    verify(task).start();
    verify(task).update();
  }

  @Test
  void picksUpWhereItLeftOffOnceTheFreezeEnds() {
    // The freeze pauses the AI rather than tearing its task down, so nothing has to be rebuilt.
    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    ai.update();

    when(time.getTime()).thenReturn(5000L);
    ai.update();

    verify(task).start();
    verify(task).update();
  }
}
