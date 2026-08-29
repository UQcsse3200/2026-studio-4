package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RangedAttackTaskTest {
  @Test
  void shouldAttackOnlyWhenPlayerIsClose() {
    Entity player = new Entity();
    RangedAttackTask attackTask = new RangedAttackTask(player);
    AITaskComponent ai = new AITaskComponent().addTask(attackTask);
    Entity demon = new Entity().addComponent(ai);

    demon.setPosition(0f, 0f);
    player.setPosition(5f, 0f);
    assertEquals(5, attackTask.getPriority());

    player.setPosition(8f, 0f);
    assertEquals(-1, attackTask.getPriority());
  }
}
