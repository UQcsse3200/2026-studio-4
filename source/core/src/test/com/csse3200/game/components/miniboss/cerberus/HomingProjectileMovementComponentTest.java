package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class HomingProjectileMovementComponentTest {
  @Test
  void shouldChangeDirectionWhenTargetMoves() {
    Entity target = new Entity();
    target.setPosition(10f, 0f);

    Entity projectile = new Entity();
    HomingProjectileMovementComponent movement =
        new HomingProjectileMovementComponent(target, 2f, 10f);
    projectile.addComponent(movement);

    movement.update(0.5f);
    assertEquals(1f, projectile.getPosition().x, 0.0001f);
    assertEquals(0f, projectile.getPosition().y, 0.0001f);

    target.setPosition(1f, 10f);
    movement.update(0.5f);

    assertEquals(1f, projectile.getPosition().x, 0.0001f);
    assertEquals(1f, projectile.getPosition().y, 0.0001f);
  }

  @Test
  void shouldStopAtMaximumTravelDistanceAndReportOnce() {
    Entity target = new Entity();
    target.setPosition(10f, 0f);

    Entity projectile = new Entity();
    HomingProjectileMovementComponent movement =
        new HomingProjectileMovementComponent(target, 2f, 3f);
    projectile.addComponent(movement);

    int[] reached = {0};
    projectile.getEvents().addListener("projectileRangeReached", () -> reached[0]++);

    movement.update(2f);
    movement.update(2f);

    assertEquals(new Vector2(3f, 0f), projectile.getPosition());
    assertEquals(1, reached[0]);
  }

  @Test
  void shouldNotOvershootNearbyTarget() {
    Entity target = new Entity();
    target.setPosition(0.25f, 0f);

    Entity projectile = new Entity();
    HomingProjectileMovementComponent movement =
        new HomingProjectileMovementComponent(target, 2f, 10f);
    projectile.addComponent(movement);

    movement.update(1f);

    assertEquals(0.25f, projectile.getPosition().x, 0.0001f);
  }
}
