package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThunderOrbMovementComponentTest {
  private static final float EPSILON = 0.0001f;

  private Entity target;
  private Entity orb;
  private ThunderOrbMovementComponent movement;
  private int expiryEvents;

  @BeforeEach
  void setUp() {
    target = new Entity();
    target.setPosition(10f, 0f);

    movement = new ThunderOrbMovementComponent(target, 2f, 90f, 3f);
    orb = new Entity().addComponent(movement);
    orb.getEvents().addListener(ThunderOrbMovementComponent.EXPIRED, () -> expiryEvents++);
    orb.create();
  }

  @Test
  void shouldMoveTowardsInitialTargetAtConfiguredSpeed() {
    movement.update(0.5f);

    assertEquals(1f, orb.getPosition().x, EPSILON);
    assertEquals(0f, orb.getPosition().y, EPSILON);
    assertFalse(movement.isStopped());
  }

  @Test
  void shouldLimitTurningWhenTargetMoves() {
    target.setPosition(0f, 10f);

    movement.update(0.5f);

    // 90 degrees per second for half a second permits a 45-degree turn.
    float expected = (float) Math.sqrt(0.5);
    assertEquals(expected, orb.getPosition().x, EPSILON);
    assertEquals(expected, orb.getPosition().y, EPSILON);
  }

  @Test
  void shouldTurnAcrossZeroUsingTheShorterDirection() {
    Entity otherTarget = new Entity();
    otherTarget.setPosition(new Vector2(10f, 0f).rotateDeg(350f));

    ThunderOrbMovementComponent otherMovement =
        new ThunderOrbMovementComponent(otherTarget, 2f, 90f, 3f);
    Entity otherOrb = new Entity().addComponent(otherMovement);
    otherOrb.create();

    otherTarget.setPosition(new Vector2(10f, 0f).rotateDeg(10f));
    otherMovement.update(0.1f);

    assertEquals(359f, otherOrb.getPosition().angleDeg(), 0.01f);
  }

  @Test
  void shouldExpireAtExactlyItsLifetime() {
    movement.update(2f);

    assertFalse(movement.isStopped());
    assertEquals(0, expiryEvents);

    movement.update(1f);

    assertTrue(movement.isStopped());
    assertEquals(1, expiryEvents);
    assertEquals(6f, orb.getPosition().x, EPSILON);
  }

  @Test
  void shouldLimitMovementToRemainingLifetimeAndExpireOnce() {
    movement.update(10f);
    Vector2 finalPosition = orb.getPosition().cpy();

    movement.update(1f);

    assertEquals(6f, finalPosition.x, EPSILON);
    assertEquals(finalPosition, orb.getPosition());
    assertEquals(1, expiryEvents);
  }

  @Test
  void shouldStopImmediatelyWithoutEmittingExpiryAfterHit() {
    movement.update(0.5f);
    movement.stop();
    Vector2 hitPosition = orb.getPosition().cpy();

    movement.update(10f);

    assertTrue(movement.isStopped());
    assertEquals(hitPosition, orb.getPosition());
    assertEquals(0, expiryEvents);
  }

  @Test
  void shouldStillExpireWhenSpawnedAtTargetPosition() {
    Entity samePositionTarget = new Entity();
    ThunderOrbMovementComponent otherMovement =
        new ThunderOrbMovementComponent(samePositionTarget, 2f, 90f, 3f);
    Entity otherOrb = new Entity().addComponent(otherMovement);
    int[] events = {0};
    otherOrb.getEvents().addListener(ThunderOrbMovementComponent.EXPIRED, () -> events[0]++);
    otherOrb.create();

    otherMovement.update(3f);

    assertTrue(otherMovement.isStopped());
    assertEquals(1, events[0]);
    assertTrue(Float.isFinite(otherOrb.getPosition().x));
    assertTrue(Float.isFinite(otherOrb.getPosition().y));
  }

  @Test
  void shouldIgnoreInvalidElapsedTime() {
    movement.update(0f);
    movement.update(-1f);
    movement.update(Float.NaN);
    movement.update(Float.POSITIVE_INFINITY);

    assertEquals(new Vector2(), orb.getPosition());
    assertFalse(movement.isStopped());
    assertEquals(0, expiryEvents);

    movement.update(3f);
    assertEquals(1, expiryEvents);
  }

  @Test
  void shouldRejectInvalidSettings() {
    assertThrows(
        IllegalArgumentException.class, () -> new ThunderOrbMovementComponent(null, 2f, 90f, 3f));

    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ThunderOrbMovementComponent(target, invalid, 90f, 3f));
      assertThrows(
          IllegalArgumentException.class,
          () -> new ThunderOrbMovementComponent(target, 2f, invalid, 3f));
      assertThrows(
          IllegalArgumentException.class,
          () -> new ThunderOrbMovementComponent(target, 2f, 90f, invalid));
    }
  }
}
