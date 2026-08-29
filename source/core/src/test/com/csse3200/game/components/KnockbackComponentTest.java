package com.csse3200.game.components;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
public class KnockbackComponentTest {

  @Test
  public void shouldKnockbackComponent() {
    Entity entity = new Entity();
    PhysicsComponent physicsComponent = Mockito.mock(PhysicsComponent.class);
    Body body = Mockito.mock(Body.class);

    when(body.getWorldCenter()).thenReturn(new Vector2(0, 0));

    when(physicsComponent.getBody()).thenReturn(body);
    entity.addComponent(physicsComponent);
    KnockbackComponent knockbackComponent = new KnockbackComponent(10f);
    entity.addComponent(knockbackComponent);

    entity.create();

    entity.setPosition(new Vector2(2, 0));
    Entity attacker = new Entity();
    attacker.setPosition(new Vector2(0, 0));

    entity.getEvents().trigger("hitReaction", attacker);

    verify(body, times(1)).applyLinearImpulse(any(Vector2.class), any(Vector2.class), eq(true));
  }
}
