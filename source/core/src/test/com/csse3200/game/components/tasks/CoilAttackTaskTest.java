package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CoilAttackTaskTest {

  private GameTime gameTime;
  private PhysicsMovementComponent movementComponent;
  private Entity owner;
  private Entity target;
  private TaskRunner taskRunner;

  @BeforeEach
  void setUp() {
    gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);

    target = new Entity();
    target.setPosition(new Vector2(5f, 0f));
    target.addComponent(new CombatStatsComponent(100, 5));

    owner = new Entity();
    owner.setPosition(new Vector2(0f, 0f));
    movementComponent = mock(PhysicsMovementComponent.class);
    owner.addComponent(movementComponent);
    owner.create();

    taskRunner = mock(TaskRunner.class);
    when(taskRunner.getEntity()).thenReturn(owner);
  }

  @Test
  void shouldBeInactiveWhenTargetIsFar() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);

    target.setPosition(new Vector2(10f, 0f));

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldTriggerWhenTargetIsClose() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);

    target.setPosition(new Vector2(2f, 0f));

    assertEquals(20, task.getPriority());
  }

  @Test
  void shouldFreezeMovementOnStart() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);

    task.start();

    verify(movementComponent).setMoving(false);
  }

  @Test
  void shouldBeginCoilingAfterTelegraphDuration() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);
    task.start();

    when(gameTime.getTime()).thenReturn(700L);
    task.update();

    verify(movementComponent).setMaxSpeed(new Vector2(5f, 5f));
    verify(movementComponent).setTarget(any(Vector2.class));
    verify(movementComponent).setMoving(true);
  }

  @Test
  void shouldPoisonTargetAfterCoilCompletes() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);
    task.start();

    when(gameTime.getTime()).thenReturn(700L);
    task.update();

    when(gameTime.getTime()).thenReturn(1200L);
    task.update();

    when(gameTime.getTime()).thenReturn(2200L);
    task.update();

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    assertEquals(98, targetStats.getHealth());
  }

  @Test
  void shouldRespectCooldownAfterCoilFinishes() {
    when(gameTime.getTime()).thenReturn(0L);
    CoilAttackTask task = new CoilAttackTask(target, 2.5f);
    task.create(taskRunner);
    task.start();

    when(gameTime.getTime()).thenReturn(700L);
    task.update();
    when(gameTime.getTime()).thenReturn(1200L);
    task.update();
    when(gameTime.getTime()).thenReturn(5200L);
    task.update();

    task.stop();
    target.setPosition(new Vector2(1f, 0f));

    when(gameTime.getTime()).thenReturn(6000L);
    assertEquals(-1, task.getPriority());

    when(gameTime.getTime()).thenReturn(9000L);
    assertEquals(20, task.getPriority());
  }
}
