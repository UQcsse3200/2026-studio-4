package com.csse3200.game.components.weapons;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class LifetimeComponentTest {
  @BeforeEach
  void beforeEach() {
    GameTime gameTime = Mockito.mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerEntityService(spy(new EntityService()));
  }

  @Test
  void shouldNotDisposeBeforeLifetimeElapses() {
    Entity entity = new Entity().addComponent(new LifetimeComponent(0.25f));
    ServiceLocator.getEntityService().register(entity);

    entity.update();
    verify(ServiceLocator.getEntityService(), never()).unregister(entity);
  }

  @Test
  void shouldDisposeOnceWhenLifetimeElapses() {
    Entity entity = new Entity().addComponent(new LifetimeComponent(0.2f));
    ServiceLocator.getEntityService().register(entity);

    entity.update();
    entity.update();
    entity.update();

    verify(ServiceLocator.getEntityService(), times(1)).unregister(entity);
  }

  @Test
  void shouldExpireFromExplicitDelta() {
    Entity entity = new Entity().addComponent(new LifetimeComponent(0.15f));
    ServiceLocator.getEntityService().register(entity);

    entity.getComponent(LifetimeComponent.class).update(0.15f);
    verify(ServiceLocator.getEntityService(), times(1)).unregister(entity);
  }
}
