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
  private final Vector2 spawnPoints = new Vector2(10, 10);

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
    Vector2 leftPoint = new Vector2(spawnPoints.x - 2, spawnPoints.y);
    Vector2 topPoint = new Vector2(spawnPoints.x, spawnPoints.y + 1.5f);
    Vector2 rightPoint = new Vector2(spawnPoints.x + 2, spawnPoints.y);
    PatrolTask patrolTask = new PatrolTask(spawnPoints, true);
    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(patrolTask);
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    Entity enemy1 = new Entity().addComponent(aiTaskComponent).addComponent(movement);

    EventListener0 callback = mock(EventListener0.class);
    enemy1.getEvents().addListener("patrolStart", callback);

    patrolTask.start();
    assertEquals(leftPoint, movement.getTarget());
    verify(callback).handle();
    enemy1.setPosition(leftPoint);
    patrolTask.update();

    assertEquals(topPoint, movement.getTarget());
    enemy1.setPosition(topPoint);
    patrolTask.update();

    assertEquals(rightPoint, movement.getTarget());
    enemy1.setPosition(rightPoint);
    patrolTask.update();

    assertEquals(leftPoint, movement.getTarget());
  }

  @Test
  void shouldPatrolFourPointsAndKeepProgress() {
    Vector2 bottomLeft = new Vector2(spawnPoints);
    int extra = 10;
    Vector2 bottomRight = new Vector2(spawnPoints.x + extra, spawnPoints.y);
    Vector2 topLeft = new Vector2(spawnPoints.x, spawnPoints.y + extra);
    Vector2 topRight = new Vector2(spawnPoints.x + extra, spawnPoints.y + extra);
    PatrolTask patrolTask = new PatrolTask(spawnPoints, false);
    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(patrolTask);
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    Entity enemy1 = new Entity().addComponent(aiTaskComponent).addComponent(movement);

    EventListener0 callback = mock(EventListener0.class);
    enemy1.getEvents().addListener("patrolStart", callback);

    patrolTask.start();
    assertEquals(bottomLeft, movement.getTarget());
    verify(callback).handle();
    enemy1.setPosition(bottomLeft);
    patrolTask.update();

    assertEquals(topRight, movement.getTarget());
    enemy1.setPosition(topRight);
    patrolTask.update();

    assertEquals(topLeft, movement.getTarget());
    enemy1.setPosition(topLeft);
    patrolTask.update();

    assertEquals(bottomRight, movement.getTarget());
    enemy1.setPosition(bottomRight);
    patrolTask.update();

    assertEquals(bottomLeft, movement.getTarget());
  }

  @Test
  void setPriorityShouldUpdatePriority() {
    // no phase two in flying enemy - should always be priority 5
    PatrolTask task = new PatrolTask(spawnPoints, true);
    task.create(owner);
    task.start();
    assertEquals(5, task.getPriority());
  }

  @Test
  void setPriorityShouldUpdatePriorityMiniboss() {
    PatrolTask task = new PatrolTask(spawnPoints, false);
    task.create(owner);
    task.start();
    assertEquals(1, task.getPriority());
  }

  @Test
  void startShouldSetInitialTargetAndStartMovement() {
    Vector2 firstPoint = new Vector2(spawnPoints.x - 2, spawnPoints.y);
    PatrolTask task = new PatrolTask(spawnPoints, true);

    task.create(owner);
    task.start();

    verify(movementComponent).setTarget(firstPoint);
    verify(movementComponent).setMoving(true);
    verify(events).trigger("patrolStart");
  }

  @Test
  void startShouldSetInitialTargetAndStartMovementMiniBoss() {
    PatrolTask task = new PatrolTask(spawnPoints, false);

    task.create(owner);
    task.start();

    verify(movementComponent).setTarget(spawnPoints);
    verify(movementComponent).setMoving(true);
    verify(events).trigger("patrolStart");
  }

  @Test
  void updateShouldNotMoveToNextPointWhenPointIsTooFarAway() {
    Vector2 first = new Vector2(spawnPoints.x - 2, spawnPoints.y);

    PatrolTask task = new PatrolTask(spawnPoints, true);

    when(enemy.getPosition()).thenReturn(spawnPoints);

    task.create(owner);
    task.start();
    task.update();

    // Only the initial target should have been set.
    verify(movementComponent).setTarget(first);
  }

  @Test
  void updateShouldMoveToNextPointWithinDistanceThreshold() {
    Vector2 secondPoint = new Vector2(5, 1);

    PatrolTask task = new PatrolTask(spawnPoints, true);

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

    PatrolTask task = new PatrolTask(spawnPoints, true);

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
    PatrolTask task = new PatrolTask(spawnPoints, true);

    task.create(owner);
    task.start();
    task.stop();

    verify(movementComponent).setMoving(false);
  }

  @Test
  void fourPointLayoutShouldCreateSquarePatrolBounds() {
    Vector2 left = new Vector2(spawnPoints.x - 2, spawnPoints.y);
    Vector2 top = new Vector2(spawnPoints.x, spawnPoints.y + 1.5f);
    Vector2 right = new Vector2(spawnPoints.x - 2f, spawnPoints.y);

    PatrolTask task = new PatrolTask(spawnPoints, true);
    task.create(owner);
    task.start();

    verify(movementComponent).setTarget(left);

    when(enemy.getPosition()).thenReturn(left);
    task.update();

    // Bounds order: bottom-left -> top-right.
    verify(movementComponent).setTarget(top);

    when(enemy.getPosition()).thenReturn(top);
    task.update();

    verify(movementComponent).setTarget(top);

    when(enemy.getPosition()).thenReturn(top);
    task.update();

    verify(movementComponent).setTarget(right);
  }
}
