package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StampedeTaskTest {

  private Entity horse;
  private Entity player;
  private Entity eventEntity;

  private TaskRunner owner;
  private PhysicsComponent physicsComponent;
  private Body body;
  private CombatStatsComponent combatStats;
  private EventHandler events;
  private GameTime timeSource;
  private ServiceLocator serviceLocator;

  @BeforeEach
  void setUp() {
    horse = mock(Entity.class);
    player = mock(Entity.class);
    eventEntity = mock(Entity.class);

    owner = mock(TaskRunner.class);
    physicsComponent = mock(PhysicsComponent.class);
    body = mock(Body.class);
    combatStats = mock(CombatStatsComponent.class);
    events = mock(EventHandler.class);
    serviceLocator = mock(ServiceLocator.class);
    timeSource = mock(GameTime.class);

    when(owner.getEntity()).thenReturn(horse);
    when(horse.getComponent(PhysicsComponent.class)).thenReturn(physicsComponent);
    when(physicsComponent.getBody()).thenReturn(body);

    when(eventEntity.getEvents()).thenReturn(events);
    when(player.getComponent(CombatStatsComponent.class)).thenReturn(combatStats);

    when(horse.getPosition()).thenReturn(new Vector2(3, 0));
    when(player.getPosition()).thenReturn(new Vector2(0, 0));
    when(body.getPosition()).thenReturn(new Vector2(3, 0));
  }

  @Test
  void priorityShouldBeNegativeBeforeStart() {
    StampedeTask task = new StampedeTask(player, eventEntity);

    assertEquals(-10, task.getPriority());
  }

  @Test
  void startShouldActivateStampede() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();

    assertEquals(15, task.getPriority());
  }

  @Test
  void updateShouldMoveBodyAroundTarget() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();

    try (MockedStatic<ServiceLocator> serviceLocator =
        org.mockito.Mockito.mockStatic(ServiceLocator.class)) {

      serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(timeSource);
      when(timeSource.getDeltaTime()).thenReturn(0.1f);

      task.update();

      verify(body).setLinearVelocity(any(Vector2.class));
    }
  }

  @Test
  void updateShouldDamageTargetWhenWithinRange() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();

    try (MockedStatic<ServiceLocator> serviceLocator =
        org.mockito.Mockito.mockStatic(ServiceLocator.class)) {

      serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(timeSource);
      when(timeSource.getDeltaTime()).thenReturn(0.1f);

      task.update();

      verify(combatStats).takeDamage(1, horse);
    }
  }

  @Test
  void updateShouldNotDamageTargetWhenOutsideRange() {
    when(horse.getPosition()).thenReturn(new Vector2(10, 10));
    when(player.getPosition()).thenReturn(new Vector2(0, 0));

    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();

    try (MockedStatic<ServiceLocator> serviceLocator =
        org.mockito.Mockito.mockStatic(ServiceLocator.class)) {

      serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(timeSource);
      when(timeSource.getDeltaTime()).thenReturn(0.1f);

      task.update();

      org.mockito.Mockito.verifyNoInteractions(combatStats);
    }
  }

  @Test
  void damageShouldRespectCooldown() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();

    try (MockedStatic<ServiceLocator> serviceLocator =
        org.mockito.Mockito.mockStatic(ServiceLocator.class)) {

      serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(timeSource);
      when(timeSource.getDeltaTime()).thenReturn(0.1f);

      task.update();
      task.update();

      // The second update occurs during the 0.25-second damage cooldown.
      verify(combatStats).takeDamage(1, horse);
    }
  }

  @Test
  void stopShouldStopMovementAndDeactivateStampede() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();
    task.stop();

    verify(body).setLinearVelocity(0f, 0f);
    assertEquals(-10, task.getPriority());
  }

  @Test
  void updateShouldDoNothingWhenStampedeIsInactive() {
    StampedeTask task = new StampedeTask(player, eventEntity);
    task.create(owner);

    task.start();
    task.stop();

    try (MockedStatic<ServiceLocator> serviceLocator =
        org.mockito.Mockito.mockStatic(ServiceLocator.class)) {

      serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(timeSource);
      when(timeSource.getDeltaTime()).thenReturn(0.1f);

      task.update();

      verify(body).setLinearVelocity(0f, 0f);
    }
  }

  @Test
  void setPriorityShouldNotChangePriority() {
    StampedeTask task = new StampedeTask(player, eventEntity);

    task.setPriority(100);

    // setPriority is intentionally empty. Before activation, priority remains -10.
    assertEquals(-10, task.getPriority());
  }
}
