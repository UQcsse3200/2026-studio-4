package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileMovementComponentTest {
  @Test
  void shouldMoveInGivenDirection() {
    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    projectile.setPosition(2f, 3f);

    movement.update(0.5f);

    assertEquals(4.5f, projectile.getPosition().x);
    assertEquals(3f, projectile.getPosition().y);
  }

  @Test
  void shouldStopAndPlayEffectAtMaximumRange() {
    ProjectileMovementComponent movement =
        new ProjectileMovementComponent(new Vector2(1f, 0f), 5f, 7f);
    Entity projectile = new Entity().addComponent(movement);
    EventListener0 callback = mock(EventListener0.class);
    projectile.getEvents().addListener("projectileRangeReached", callback);

    movement.update(2f);
    movement.update(1f);

    assertEquals(7f, projectile.getPosition().x);
    verify(callback, times(1)).handle();
  }
}
