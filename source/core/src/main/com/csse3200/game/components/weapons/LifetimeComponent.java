package com.csse3200.game.components.weapons;

import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Unregisters the entity after {@code lifetime} seconds. Does not call {@code entity.dispose()}
 * from {@code update()} (nested LibGDX iterators).
 */
public class LifetimeComponent extends Component {
  private float remaining;
  private boolean expired;

  public LifetimeComponent(float lifetime) {
    this.remaining = lifetime;
  }

  public void update(float dt) {
    if (expired) {
      return;
    }
    remaining -= dt;
    if (remaining <= 0f) {
      expired = true;
      // Do not entity.dispose() here: Entity.update() is already iterating components.
      entity.setEnabled(false);
      PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
      if (physics != null) {
        physics.dispose();
      }
      if (ServiceLocator.getEntityService() != null) {
        ServiceLocator.getEntityService().unregister(entity);
      }
    }
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }
}
