package com.csse3200.game.entities;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.extensions.GameExtension;
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
}
