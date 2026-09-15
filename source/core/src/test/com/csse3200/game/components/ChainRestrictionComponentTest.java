package com.csse3200.game.components;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChainRestrictionComponentTest {

  @Mock private Entity entity;
  @Mock private ChainRestrictionComponent chainRestriction;
  @Mock private Body body;

  @BeforeEach
  void setUp() {
    entity = mock(Entity.class);
    body = mock(Body.class);
    PhysicsComponent physics = mock(PhysicsComponent.class);

    when(entity.getComponent(PhysicsComponent.class)).thenReturn(physics);
    when(physics.getBody()).thenReturn(body);

    chainRestriction = new ChainRestrictionComponent(new Vector2(0f, 0f), 3f);
    chainRestriction.setEntity(entity);
    chainRestriction.create();
  }

  @Test
  void shouldLeavePositionAndVelocityUnchangedInsideRadius() {
    when(body.getPosition()).thenReturn(new Vector2(2f, 0f));

    chainRestriction.update();

    verify(entity, never()).setPosition(any(Vector2.class));
    verify(body, never()).setLinearVelocity(any(Vector2.class));
  }

  @Test
  void shouldClampOutsidePositionAndRemoveOutwardVelocity() {
    when(body.getPosition()).thenReturn(new Vector2(5f, 0f));
    when(body.getLinearVelocity()).thenReturn(new Vector2(4f, 2f));

    chainRestriction.update();

    verify(entity).setPosition(new Vector2(3f, 0f));
    verify(body).setLinearVelocity(new Vector2(0f, 2f));
  }

  @Test
  void shouldKeepInwardVelocityWhenCorrectingPosition() {
    when(body.getPosition()).thenReturn(new Vector2(5f, 0f));
    when(body.getLinearVelocity()).thenReturn(new Vector2(-2f, 1f));

    chainRestriction.update();

    verify(entity).setPosition(new Vector2(3f, 0f));
    verify(body, never()).setLinearVelocity(any(Vector2.class));
  }

  @Test
  void shouldRemoveOutwardVelocityAtBoundary() {
    when(body.getPosition()).thenReturn(new Vector2(3f, 0f));
    when(body.getLinearVelocity()).thenReturn(new Vector2(2f, 0f));

    chainRestriction.update();

    verify(entity, never()).setPosition(any(Vector2.class));
    verify(body).setLinearVelocity(new Vector2(0f, 0f));
  }

  @Test
  void shouldUseCircularBoundaryAndKeepOriginalAnchor() {
    Vector2 anchor = new Vector2(10f, 20f);
    chainRestriction = new ChainRestrictionComponent(anchor, 3f);
    chainRestriction.setEntity(entity);
    chainRestriction.create();

    anchor.set(100f, 100f);

    when(body.getPosition()).thenReturn(new Vector2(13f, 24f));
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));

    chainRestriction.update();

    ArgumentCaptor<Vector2> position = ArgumentCaptor.forClass(Vector2.class);
    verify(entity).setPosition(position.capture());

    Assertions.assertEquals(11.8f, position.getValue().x, 0.0001f);
    Assertions.assertEquals(22.4f, position.getValue().y, 0.0001f);
  }
}
