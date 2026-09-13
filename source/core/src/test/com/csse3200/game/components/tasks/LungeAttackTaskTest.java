package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LungeAttackTaskTest {

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

    owner = new Entity();
    owner.setPosition(new Vector2(0f, 0f));
    movementComponent = mock(PhysicsMovementComponent.class);
    owner.addComponent(movementComponent);
    owner.create();

    taskRunner = mock(TaskRunner.class);
    when(taskRunner.getEntity()).thenReturn(owner);
  }

  @Test
  void shouldNotAcquireInvisibleTarget() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    target.setPosition(1f, 0f);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    when(effects.isConcealed()).thenReturn(true);
    assertEquals(-1, task.getPriority());
    when(effects.isConcealed()).thenReturn(false);
    assertEquals(20, task.getPriority());
  }

  @Test
  void shouldKeepPriorityPureAndWindDownThroughUpdateWhenTargetVanishes() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    target.setPosition(1f, 0f);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    task.start();
    when(gameTime.getTime()).thenReturn(500L);
    task.update();
    verify(movementComponent).setMoving(true);
    clearInvocations(movementComponent);

    when(effects.isConcealed()).thenReturn(true);
    // The query changes nothing: the lunge keeps its priority so its own update can end it.
    assertEquals(20, task.getPriority());
    assertEquals(20, task.getPriority());
    verifyNoInteractions(movementComponent);
    when(gameTime.getTime()).thenReturn(600L);
    task.update();
    assertEquals(-1, task.getPriority());
    verify(movementComponent).setMoving(false);
    verify(movementComponent).setMaxSpeed(new Vector2(2.5f, 2.5f));
  }

  @Test
  void shouldAbandonTelegraphWithoutDashEndOrCooldown() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    target.setPosition(1f, 0f);
    int[] dashEnds = {0};
    owner.getEvents().addListener("lungeDashEnd", () -> dashEnds[0]++);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    task.start();
    clearInvocations(movementComponent);

    when(gameTime.getTime()).thenReturn(200L);
    when(effects.isConcealed()).thenReturn(true);
    task.update();
    assertEquals(-1, task.getPriority());
    verify(movementComponent).setMoving(false);
    verify(movementComponent, never()).setTarget(any());
    // No dash ever started, so nothing announces one ending.
    assertEquals(0, dashEnds[0]);

    when(gameTime.getTime()).thenReturn(300L);
    task.update();
    assertEquals(-1, task.getPriority());
    task.stop();
    // Nor is a cooldown charged: the lunge is ready the moment the target is visible again.
    when(effects.isConcealed()).thenReturn(false);
    assertEquals(20, task.getPriority());
  }

  @Test
  void shouldEndDashNormallyWithDashEndAndCooldown() {
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    target.addComponent(effects);
    target.setPosition(1f, 0f);
    int[] dashEnds = {0};
    owner.getEvents().addListener("lungeDashEnd", () -> dashEnds[0]++);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    task.start();
    when(gameTime.getTime()).thenReturn(500L);
    task.update();
    verify(movementComponent).setMoving(true);
    clearInvocations(movementComponent);

    when(gameTime.getTime()).thenReturn(600L);
    when(effects.isConcealed()).thenReturn(true);
    task.update();
    verify(movementComponent).setMoving(false);
    verify(movementComponent).setMaxSpeed(new Vector2(2.5f, 2.5f));
    verify(movementComponent, never()).setMoving(true);
    assertEquals(-1, task.getPriority());
    assertEquals(1, dashEnds[0]);

    when(gameTime.getTime()).thenReturn(1600L);
    task.update();
    assertEquals(1, dashEnds[0]);
    when(effects.isConcealed()).thenReturn(false);
    task.update();
    assertEquals(-1, task.getPriority());
    task.stop();
    when(gameTime.getTime()).thenReturn(2599L);
    assertEquals(-1, task.getPriority());
    when(gameTime.getTime()).thenReturn(2600L);
    assertEquals(20, task.getPriority());
    clearInvocations(movementComponent);
    task.start();
    when(gameTime.getTime()).thenReturn(3100L);
    task.update();
    verify(movementComponent).setMoving(true);
  }

  @Test
  void shouldBeInactiveWhenTargetIsFar() {
    when(gameTime.getTime()).thenReturn(0L);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);

    target.setPosition(new Vector2(10f, 0f));

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldTriggerWhenTargetIsClose() {
    when(gameTime.getTime()).thenReturn(0L);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);

    target.setPosition(new Vector2(2f, 0f));

    assertEquals(20, task.getPriority());
  }

  @Test
  void shouldFreezeMovementOnStart() {
    when(gameTime.getTime()).thenReturn(1000L);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);

    task.start();

    verify(movementComponent).setMoving(false);
  }

  @Test
  void shouldStartDashAfterTelegraphDuration() {
    when(gameTime.getTime()).thenReturn(1000L);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    task.start();

    when(gameTime.getTime()).thenReturn(1600L);
    task.update();

    verify(movementComponent).setMaxSpeed(new Vector2(6f, 6f));
    verify(movementComponent).setTarget(any(Vector2.class));
  }

  @Test
  void shouldRespectCooldownAfterDash() {
    when(gameTime.getTime()).thenReturn(0L);
    LungeAttackTask task = new LungeAttackTask(target, 20, 2.5f);
    task.create(taskRunner);
    task.start();

    when(gameTime.getTime()).thenReturn(600L);
    task.update();

    when(gameTime.getTime()).thenReturn(1100L);
    task.update();

    task.stop();

    target.setPosition(new Vector2(1f, 0f));

    when(gameTime.getTime()).thenReturn(2000L);
    assertEquals(-1, task.getPriority());

    when(gameTime.getTime()).thenReturn(3200L);
    assertEquals(20, task.getPriority());
  }
}
