package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PatrolTaskTest {
  @Test
  void shouldMoveRightThenTurnAtMapEdge() {
    PatrolTask patrolTask = new PatrolTask(1f, 10f);
    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(patrolTask);
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    Entity enemy = new Entity().addComponent(aiTaskComponent).addComponent(movement);

    EventListener0 callback = mock(EventListener0.class);
    enemy.getEvents().addListener("patrolStart", callback);

    patrolTask.start();
    assertEquals(10f, movement.getTarget().x);
    verify(callback).handle();

    enemy.setPosition(10f, 3f);
    patrolTask.update();
    assertEquals(1f, movement.getTarget().x);
    assertEquals(3f, movement.getTarget().y);

    enemy.setPosition(1f, 3f);
    patrolTask.update();
    assertEquals(10f, movement.getTarget().x);
  }
}
