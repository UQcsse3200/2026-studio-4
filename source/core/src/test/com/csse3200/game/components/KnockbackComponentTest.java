package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class KnockbackComponentTest {

  @Test
  void shouldKnockbackComponent() {
    Entity entity = new Entity();
    PhysicsComponent physicsComponent = mock(PhysicsComponent.class);
    Body body = mock(Body.class);

    when(body.getWorldCenter()).thenReturn(new Vector2(0, 0));

    when(physicsComponent.getBody()).thenReturn(body);
    entity.addComponent(physicsComponent);
    KnockbackComponent knockbackComponent = new KnockbackComponent(10f);
    entity.addComponent(knockbackComponent);

    entity.create();

    entity.setPosition(new Vector2(2, 0));
    Entity attacker = new Entity();
    attacker.setPosition(new Vector2(0, 0.5f));

    entity.getEvents().trigger("hitReaction", attacker);

    ArgumentCaptor<Vector2> impulseCaptor = ArgumentCaptor.forClass(Vector2.class);
    verify(body, times(1))
        .applyLinearImpulse(impulseCaptor.capture(), any(Vector2.class), eq(true));

    assertEquals(10f, impulseCaptor.getValue().x, 0.001f);
    assertEquals(0f, impulseCaptor.getValue().y, 0.001f);
  }

  @Test
  void shouldNotApplyKnockbackWhenAttackerIsNull() {
    Entity entity = new Entity();
    PhysicsComponent physicsComponent = mock(PhysicsComponent.class);
    Body body = mock(Body.class);

    when(physicsComponent.getBody()).thenReturn(body);
    entity.addComponent(physicsComponent);

    KnockbackComponent knockbackComponent = new KnockbackComponent(10f);
    entity.addComponent(knockbackComponent);
    entity.create();

    entity.getEvents().trigger("hitReaction", (Entity) null);

    verify(body, never()).applyLinearImpulse(any(), any(), anyBoolean());
  }

  @Test
  void shouldNotApplyKnockbackWhenPhysicsComponentMissing() {
    Entity entity = new Entity();
    KnockbackComponent knockbackComponent = new KnockbackComponent(10f);
    entity.addComponent(knockbackComponent);
    entity.create();

    Entity attacker = new Entity();
    attacker.setPosition(new Vector2(0, 0.5f));

    assertDoesNotThrow(() -> entity.getEvents().trigger("hitReaction", attacker));
  }
}
