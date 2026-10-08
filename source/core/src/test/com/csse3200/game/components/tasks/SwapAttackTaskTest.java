package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SwapAttackTaskTest {
  private final long[] time = {100L};
  private Entity player;
  private Entity eye;
  private SwapAttackTask task;

  @BeforeEach
  void setUp() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getTime()).thenAnswer(invocation -> time[0]);
    ServiceLocator.registerTimeSource(gameTime);

    player = new Entity();
    player.setPosition(5f, 0f);
    eye = new Entity();
    eye.setPosition(0f, 0f);
    task = new SwapAttackTask(player);
    task.create(() -> eye);
  }

  @Test
  void swapsPositionsAfterCasting() {
    assertEquals(12, task.getPriority());
    task.start();

    time[0] += SwapAttackTask.CAST_TIME;
    task.update();

    assertEquals(5f, eye.getPosition().x);
    assertEquals(0f, player.getPosition().x);
  }

  @Test
  void doesNotSwapBeforeCastingFinishes() {
    task.start();

    time[0] += SwapAttackTask.CAST_TIME - 1;
    task.update();

    assertEquals(0f, eye.getPosition().x);
    assertEquals(5f, player.getPosition().x);
  }

  @Test
  void staysInactiveOutsideSwapRange() {
    player.setPosition(SwapAttackTask.SWAP_RANGE + 1f, 0f);

    assertEquals(-1, task.getPriority());
  }
}
