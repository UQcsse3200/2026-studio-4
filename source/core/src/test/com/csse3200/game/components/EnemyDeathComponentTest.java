package com.csse3200.game.components;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyDeathComponentTest {
  @Test
  void shouldDisposeEntityOnDeathEvent() {
    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    Entity entity = new Entity().addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("entityDied", new Vector2(0, 0), entity);

    verify(entityService).unregister(entity);
  }
}
