package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WizardPullAttackTaskTest {
  private Entity player;
  private Entity wizard;
  private WizardPullAttackTask task;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(100L);
    ServiceLocator.registerTimeSource(time);

    player = new Entity().addComponent(new PhysicsComponent());
    player.setPosition(5f, 0f);
    player.create();
    wizard = new Entity();
    wizard.setPosition(0f, 0f);
    task = new WizardPullAttackTask(player, 6);
    task.create(() -> wizard);
  }

  @Test
  void shouldPullPlayerTowardWizard() {
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

  @Test
  void shouldUseMeleeAttackAfterPlayerIsPulledClose() {
    player.setPosition(1f, 0f);
    int[] meleeAttacks = {0};
    wizard.getEvents().addListener("wizardMeleeAttack", () -> meleeAttacks[0]++);

    task.start();
    task.update();

    assertEquals(1, meleeAttacks[0]);
  }
}
