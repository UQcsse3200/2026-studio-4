package com.csse3200.game.physics.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class PhysicsMovementComponentTest {
  private GameTime time;
  private Entity entity;
  private PhysicsMovementComponent movement;
  private PhysicsComponent physics;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.016f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    physics = new PhysicsComponent();
    movement = new PhysicsMovementComponent();
    movement.setMaxSpeed(new Vector2(2.5f, 2.5f));
    entity = new Entity().addComponent(physics).addComponent(movement);
    entity.create();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.getPhysicsService().getPhysics().dispose();
  }

  @ParameterizedTest
  @ValueSource(floats = {0.016f, 0.033f, 0.08f, 0.15f})
  void boundedMovementApproachesNearbyWaypointWithoutPassingIt(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    movement.setTarget(new Vector2(0.04f, 0f), true);
    for (int i = 0; i < 30; i++) {
      entity.earlyUpdate();
      entity.update();
      ServiceLocator.getPhysicsService().getPhysics().update();
      assertTrue(
          physics.getBody().getPosition().x <= 0.04001f,
          "A waypoint must not be overshot during a long frame");
    }
    assertEquals(0.04f, physics.getBody().getPosition().x, 0.002f);
  }

  @Test
  void regularTargetRestoresFullAttackSpeedAfterBoundedMovement() {
    movement.setTarget(new Vector2(0.04f, 0f), true);
    movement.update();
    assertTrue(physics.getBody().getLinearVelocity().x < 2.5f);

    movement.setMaxSpeed(new Vector2(6f, 6f));
    movement.setTarget(new Vector2(0.04f, 0f));
    movement.update();

    assertEquals(6f, physics.getBody().getLinearVelocity().x, 0.001f);
  }
}
