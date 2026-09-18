package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WitchPullAttackTaskTest {
  private Entity player;
  private Entity witch;
  private WitchPullAttackTask task;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(100L);
    ServiceLocator.registerTimeSource(time);

    player = new Entity().addComponent(new PhysicsComponent());
    player.setPosition(5f, 0f);
    player.create();
    witch = new Entity();
    witch.setPosition(0f, 0f);
    task = new WitchPullAttackTask(player, 6);
    task.create(() -> witch);
  }

  @Test
  void shouldPullPlayerTowardWitch() {
    assertEquals(12, task.getPriority());
    task.start();
    task.update();

    assertTrue(player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x < 0f);
  }

  @Test
  void oppositeMovementShouldEscapeOnlySlowly() {
    player.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(3f, 0f);
    task.start();
    task.update();

    float speed = player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    assertTrue(speed > 0f);
    assertTrue(speed < 1f);
  }
}
