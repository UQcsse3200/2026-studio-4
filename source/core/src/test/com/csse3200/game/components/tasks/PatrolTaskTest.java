package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
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
  void shouldPatrolThreePointsAndKeepProgress() {
    Vector2 leftPoint = new Vector2(1f, 3f);
    Vector2 topPoint = new Vector2(5f, 5f);
    Vector2 rightPoint = new Vector2(10f, 3f);
    PatrolTask patrolTask = new PatrolTask(leftPoint, topPoint, rightPoint);
    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(patrolTask);
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    Entity enemy = new Entity().addComponent(aiTaskComponent).addComponent(movement);

    EventListener0 callback = mock(EventListener0.class);
    enemy.getEvents().addListener("patrolStart", callback);

    patrolTask.start();
    assertEquals(leftPoint, movement.getTarget());
    verify(callback).handle();

    enemy.setPosition(leftPoint);
    patrolTask.update();
    assertEquals(topPoint, movement.getTarget());

    patrolTask.start();
    assertEquals(topPoint, movement.getTarget());

    enemy.setPosition(topPoint);
    patrolTask.update();
    assertEquals(rightPoint, movement.getTarget());

    enemy.setPosition(rightPoint);
    patrolTask.update();
    assertEquals(leftPoint, movement.getTarget());
  }
}
