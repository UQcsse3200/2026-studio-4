package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class OneAttackTaskTest {
  private Entity sleipnir;
  private Entity target;

  private CombatStatsComponent attackerStats;
  private CombatStatsComponent targetStats;
  private Vector2 attackerPosition;
  private Vector2 targetPosition;
  private PhysicsMovementComponent movementComponent;
  private TaskRunner taskRunner;
  private GameTime gameTime;

  @BeforeEach
  void setUp() {
    gameTime = mock(GameTime.class);
    when(gameTime.getTime()).thenReturn(0L);

    // This must happen before constructing OneAttackTask
    ServiceLocator.registerTimeSource(gameTime);

    sleipnir = mock(Entity.class);
    target = mock(Entity.class);

    attackerStats = mock(CombatStatsComponent.class);
    targetStats = mock(CombatStatsComponent.class);
    movementComponent = mock(PhysicsMovementComponent.class);
    taskRunner = mock(TaskRunner.class);

    attackerPosition = new Vector2(0f, 0f);
    targetPosition = new Vector2(2f, 0f);

    when(sleipnir.getPosition()).thenReturn(attackerPosition);
    when(target.getPosition()).thenReturn(targetPosition);

    when(sleipnir.getComponent(CombatStatsComponent.class)).thenReturn(attackerStats);

    when(sleipnir.getComponent(PhysicsMovementComponent.class)).thenReturn(movementComponent);

    when(target.getComponent(CombatStatsComponent.class)).thenReturn(targetStats);

    when(taskRunner.getEntity()).thenReturn(sleipnir);

    when(attackerStats.getBaseAttack()).thenReturn(10);
  }

  @Test
  void shouldDamageTargetOnceWhenWithinAttackDistance() {
    targetPosition.set(2f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      task.update();

      verify(targetStats, times(1)).takeDamage(10, sleipnir);

      // A second update must not attack again.
      task.update();

      verify(targetStats, times(1)).takeDamage(10, sleipnir);
      ServiceLocator.registerTimeSource(gameTime);
    }
  }

  @Test
  void shouldNotDamageTargetWhenOutsideAttackDistance() {
    targetPosition.set(10f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      task.update();

      verify(targetStats, never()).takeDamage(any(Integer.class), any(Entity.class));
    }
  }

  @Test
  void shouldNotDamageConcealedTarget() {
    targetPosition.set(2f, 0f);
    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(true);

      task.update();

      verify(targetStats, never()).takeDamage(any(Integer.class), any(Entity.class));
    }
  }

  @Test
  void shouldHaveHighPriorityWhenTargetIsWithinAttackDistance() {
    targetPosition.set(2f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      assertEquals(10, task.getPriority());
    }
  }

  @Test
  void shouldHaveNegativePriorityWhenTargetIsOutsideAttackDistance() {
    targetPosition.set(10f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      assertEquals(-1, task.getPriority());
    }
  }

  @Test
  void shouldHaveNegativePriorityWhenTargetIsConcealed() {
    targetPosition.set(2f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(true);

      assertEquals(-1, task.getPriority());
    }
  }

  @Test
  void shouldIncreaseSpeedAfterAttacking() {
    targetPosition.set(2f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(5f, 5f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      task.update();

      verify(movementComponent)
          .setMaxSpeed(argThat(speed -> speed.epsilonEquals(new Vector2(5.5f, 5.5f), 0.001f)));
    }
  }

  @Test
  void shouldNotIncreaseSpeedWhenAlreadyAtMaximumSpeed() {
    targetPosition.set(2f, 0f);

    OneAttackTask task = new OneAttackTask(target, 3f, 5f, new Vector2(10f, 10f));

    task.create(taskRunner);
    task.start();

    try (MockedStatic<StatusEffectsControllerComponent> concealed =
        mockStatic(StatusEffectsControllerComponent.class)) {

      concealed.when(() -> StatusEffectsControllerComponent.isConcealed(target)).thenReturn(false);

      task.update();

      verify(movementComponent, never()).setMaxSpeed(any(Vector2.class));
    }
  }
}
