package com.csse3200.game.components;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChainRestrictionComponentTest {

  @Mock private Entity entity;
  @Mock private PhysicsComponent physicsComponent;
  @Mock private Body body;

  private ChainRestrictionComponent chainRestrictionComponent;

  @BeforeEach
  void setUp() {
    Vector2 anchorPoint = new Vector2(0f, 0f);
    chainRestrictionComponent = new ChainRestrictionComponent(anchorPoint, 10f);
    when(entity.getComponent(PhysicsComponent.class)).thenReturn(physicsComponent);
    chainRestrictionComponent.setEntity(entity);
    chainRestrictionComponent.create();
  }

  @Test
  void shouldNotPullWhenInsideRadius() {
    when(entity.getPosition()).thenReturn(new Vector2(5f, 0f));
    chainRestrictionComponent.update();
    verify(physicsComponent, never()).getBody();
  }

  @Test
  void shouldPullWhenOutsideRadius() {
    when(entity.getPosition()).thenReturn(new Vector2(15f, 0f));
    when(physicsComponent.getBody()).thenReturn(body);
    when(body.getWorldCenter()).thenReturn(new Vector2(15f, 0f));
    chainRestrictionComponent.update();
    Vector2 expectedImpulse = new Vector2(-20f, 0f);
    verify(body).applyLinearImpulse(eq(expectedImpulse), any(Vector2.class), eq(true));
  }
}
