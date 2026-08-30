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
  void shouldRunQueuedActionAfterEntityUpdate() {
    EntityService entityService = new EntityService();
    List<String> order = new ArrayList<>();
    Entity entity =
        new Entity() {
          @Override
          public void update() {
            order.add("entity");
            entityService.runAfterUpdate(() -> order.add("after"));
          }
        };
    entityService.register(entity);

    entityService.update();

    assertEquals(List.of("entity", "after"), order);
  }
}
