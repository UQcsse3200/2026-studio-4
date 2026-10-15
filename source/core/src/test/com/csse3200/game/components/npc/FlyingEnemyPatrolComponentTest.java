package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FlyingEnemyPatrolComponentTest {

  @Test
  void shouldUsePositionAssignedBeforeComponentCreation() {
    AITaskComponent ai = new AITaskComponent();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    FlyingEnemyPatrolComponent patrol = new FlyingEnemyPatrolComponent();

    Entity enemy = new Entity().addComponent(ai).addComponent(movement).addComponent(patrol);

    enemy.setPosition(10f, 20f);
    patrol.create();
    ai.update();

    assertEquals(new Vector2(8f, 20f), movement.getTarget());

    enemy.setPosition(8f, 20f);
    ai.update();

    assertEquals(new Vector2(10f, 21.5f), movement.getTarget());
  }

  @Test
  void shouldKeepSpawnOriginWhenMovedBeforePatrolStarts() {
    AITaskComponent ai = new AITaskComponent();
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    FlyingEnemyPatrolComponent patrol = new FlyingEnemyPatrolComponent();

    Entity enemy = new Entity().addComponent(ai).addComponent(movement).addComponent(patrol);

    enemy.setPosition(10f, 20f);
    patrol.create();

    // Simulate displacement before patrol first becomes active.
    enemy.setPosition(30f, 40f);
    ai.update();

    assertEquals(new Vector2(8f, 20f), movement.getTarget());
  }
}
