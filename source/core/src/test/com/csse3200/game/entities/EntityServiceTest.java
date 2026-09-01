package com.csse3200.game.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EntityServiceTest {
  @Test
  void shouldCreateEntity() {
    EntityService entityService = new EntityService();
    Entity entity = spy(Entity.class);
    entityService.register(entity);
    verify(entity).create();
  }

  @Test
  void shouldUpdateEntities() {
    EntityService entityService = new EntityService();
    Entity entity = spy(Entity.class);
    entityService.register(entity);
    entityService.update();

    verify(entity).update();
    verify(entity).earlyUpdate();
  }

  @Test
  void shouldNotUpdateUnregisteredEntities() {
    EntityService entityService = new EntityService();
    Entity entity = spy(Entity.class);
    entityService.register(entity);
    entityService.unregister(entity);
    entityService.update();
    verify(entity, times(0)).update();
    verify(entity, times(0)).earlyUpdate();
  }

  @Test
  void shouldDisposeEntities() {
    EntityService entityService = new EntityService();
    Entity entity = mock(Entity.class);
    entityService.register(entity);
    entityService.dispose();
    verify(entity).dispose();
  }

  @Test
  void shouldNotDisposeScheduledEntityBeforeUpdate() {
    EntityService entityService = new EntityService();
    Entity entity = mock(Entity.class);
    entityService.register(entity);
    entityService.scheduleDisposal(entity);

    verify(entity, times(0)).dispose();
    verify(entity).setEnabled(false);
  }

  @Test
  void shouldDisposeScheduledEntityOnUpdate() {
    EntityService entityService = new EntityService();
    Entity entity = mock(Entity.class);
    entityService.register(entity);
    entityService.scheduleDisposal(entity);
    entityService.update();

    verify(entity).dispose();
  }

  @Test
  void shouldDisposeScheduledEntityOnlyOnce() {
    EntityService entityService = new EntityService();
    Entity entity = mock(Entity.class);
    entityService.register(entity);
    entityService.scheduleDisposal(entity);
    entityService.scheduleDisposal(entity);
    entityService.update();
    entityService.update();

    verify(entity, times(1)).dispose();
  }

  @Test
  void shouldNotRunScheduledTaskBeforeUpdate() {
    EntityService entityService = new EntityService();
    Runnable task = mock(Runnable.class);
    entityService.schedule(task);

    verify(task, times(0)).run();
  }

  @Test
  void shouldRunScheduledTaskOnUpdate() {
    EntityService entityService = new EntityService();
    Runnable task = mock(Runnable.class);
    entityService.schedule(task);
    entityService.update();

    verify(task, times(1)).run();
  }

  @Test
  void shouldRunScheduledTaskOnlyOnce() {
    EntityService entityService = new EntityService();
    Runnable task = mock(Runnable.class);
    entityService.schedule(task);
    entityService.update();
    entityService.update();

    verify(task, times(1)).run();
  }

  @Test
  void shouldRunTaskScheduledByAnotherTask() {
    EntityService entityService = new EntityService();
    Runnable innerTask = mock(Runnable.class);
    entityService.schedule(() -> entityService.schedule(innerTask));
    entityService.update();

    verify(innerTask, times(1)).run();
  }

  @Test
  void shouldDisposeEntityScheduledByTask() {
    EntityService entityService = new EntityService();
    Entity entity = mock(Entity.class);
    entityService.register(entity);
    entityService.schedule(() -> entityService.scheduleDisposal(entity));
    entityService.update();

    verify(entity, times(1)).dispose();
  }
}
