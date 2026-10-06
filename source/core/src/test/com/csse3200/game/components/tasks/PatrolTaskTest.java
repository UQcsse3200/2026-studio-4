package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PatrolTaskTest {
  private Entity enemy;
  private TaskRunner owner;
  private PhysicsMovementComponent movementComponent;
  private EventHandler events;

  @BeforeEach
  void setUp() {
    enemy = mock(Entity.class);
    owner = mock(TaskRunner.class);
    movementComponent = mock(PhysicsMovementComponent.class);
    events = mock(EventHandler.class);

    when(owner.getEntity()).thenReturn(enemy);
    when(enemy.getComponent(PhysicsMovementComponent.class)).thenReturn(movementComponent);
    when(enemy.getEvents()).thenReturn(events);
  }

  @Test
  void shouldPatrolThreePointsAndKeepProgress() {
    Vector2 leftPoint = new Vector2(1f, 3f);
    Vector2 topPoint = new Vector2(5f, 5f);
    Vector2 rightPoint = new Vector2(10f, 3f);
    PatrolTask patrolTask = new PatrolTask(new Vector2[] {leftPoint, topPoint, rightPoint});
    AITaskComponent aiTaskComponent = new AITaskComponent(new Entity()).addTask(patrolTask);
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

  @Test
  void defaultPriorityShouldBeTwo() {
    PatrolTask task = new PatrolTask(createThreePatrolPoints());

    assertEquals(1, task.getPriority());
  }

  @Test
  void setPriorityShouldUpdatePriority() {
    PatrolTask task = new PatrolTask(createThreePatrolPoints());
    task.setPriority(5);

    assertEquals(1, task.getPriority());
  }

  @Test
  void startShouldSetInitialTargetAndStartMovement() {
    Vector2 firstPoint = new Vector2(1, 1);
    PatrolTask task =
        new PatrolTask(new Vector2[] {firstPoint, new Vector2(5, 1), new Vector2(5, 5)});

    task.create(owner);
    task.start();

    verify(movementComponent).setTarget(firstPoint);
    verify(movementComponent).setMoving(true);
    verify(events).trigger("patrolStart");
  }

  @Test
  void updateShouldMoveToNextPointWhenCurrentPointIsReached() {
    Vector2 firstPoint = new Vector2(1, 1);
    Vector2 secondPoint = new Vector2(5, 1);

    PatrolTask task = new PatrolTask(new Vector2[] {firstPoint, secondPoint, new Vector2(5, 5)});

    when(enemy.getPosition()).thenReturn(new Vector2(1, 1));

    task.create(owner);
    task.start();
    task.update();

    verify(movementComponent).setTarget(secondPoint);
  }

  @Test
  void updateShouldNotMoveToNextPointWhenPointIsTooFarAway() {
    Vector2 firstPoint = new Vector2(1, 1);
    Vector2 secondPoint = new Vector2(5, 1);

    PatrolTask task = new PatrolTask(new Vector2[] {firstPoint, secondPoint, new Vector2(5, 5)});

    when(enemy.getPosition()).thenReturn(new Vector2(2, 2));

    task.create(owner);
    task.start();
    task.update();

    // Only the initial target should have been set.
    verify(movementComponent).setTarget(firstPoint);
  }

  @Test
  void updateShouldMoveToNextPointWithinDistanceThreshold() {
    Vector2 firstPoint = new Vector2(1, 1);
    Vector2 secondPoint = new Vector2(5, 1);

    PatrolTask task = new PatrolTask(new Vector2[] {firstPoint, secondPoint, new Vector2(5, 5)});

    // Distance is exactly 0.2, so the task should advance.
    when(enemy.getPosition()).thenReturn(new Vector2(1.2f, 1));

    task.create(owner);
    task.start();
    task.update();

    when(enemy.getPosition()).thenReturn(secondPoint);
    assertEquals(secondPoint, enemy.getPosition());
  }

  @Test
  void updateShouldCycleBackToFirstPointAfterLastPoint() {
    Vector2 firstPoint = new Vector2(1, 1);
    Vector2 secondPoint = new Vector2(5, 1);
    Vector2 thirdPoint = new Vector2(5, 5);

    PatrolTask task = new PatrolTask(new Vector2[] {firstPoint, secondPoint, thirdPoint});

    task.create(owner);
    task.start();

    when(enemy.getPosition()).thenReturn(firstPoint);
    task.update();

    when(enemy.getPosition()).thenReturn(secondPoint);
    task.update();

    when(enemy.getPosition()).thenReturn(thirdPoint);
    task.update();

    when(enemy.getPosition()).thenReturn(firstPoint);
    assertEquals(firstPoint, enemy.getPosition());
  }

  @Test
  void stopShouldStopMovement() {
    PatrolTask task = new PatrolTask(createThreePatrolPoints());

    task.create(owner);
    task.start();
    task.stop();

    verify(movementComponent).setMoving(false);
  }

  @Test
  void fourPointLayoutShouldCreateSquarePatrolBounds() {
    Vector2 origin = new Vector2(10, 20);
    Vector2 size = new Vector2(5, 8);

    PatrolTask task = new PatrolTask(new Vector2[] {origin, size}, enemy);
    task.create(owner);
    task.start();

    verify(movementComponent).setTarget(new Vector2(10, 20));

    when(enemy.getPosition()).thenReturn(new Vector2(10, 20));
    task.update();

    // Bounds order: bottom-left -> top-right.
    verify(movementComponent).setTarget(new Vector2(15, 28));

    when(enemy.getPosition()).thenReturn(new Vector2(15, 28));
    task.update();

    verify(movementComponent).setTarget(new Vector2(10, 28));

    when(enemy.getPosition()).thenReturn(new Vector2(10, 28));
    task.update();

    verify(movementComponent).setTarget(new Vector2(10, 20));
  }

  private Vector2[] createThreePatrolPoints() {
    return new Vector2[] {new Vector2(0, 0), new Vector2(5, 0), new Vector2(5, 5)};
  }
}
