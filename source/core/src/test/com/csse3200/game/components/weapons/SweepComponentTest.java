package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link SweepComponent}.
 *
 * <p>NOTE: assumes {@code ServiceLocator.registerTimeSource(GameTime)} exists to install a mock
 * time source, and that a {@code GameExtension} resets {@code ServiceLocator} between tests. Adjust
 * these to match your project's actual test scaffolding if the names differ.
 */
@ExtendWith(GameExtension.class)
class SweepComponentTest {
  private GameTime gameTime;

  @BeforeEach
  void setUp() {
    gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
  }

  @Test
  void constructorRejectsZeroDuration() {
    assertThrows(IllegalArgumentException.class, () -> new SweepComponent(0f, 0f, 90f, 1f));
  }

  @Test
  void constructorRejectsNegativeDuration() {
    assertThrows(IllegalArgumentException.class, () -> new SweepComponent(-1f, 0f, 90f, 1f));
  }

  @Test
  void constructorRejectsNegativeRadius() {
    assertThrows(IllegalArgumentException.class, () -> new SweepComponent(1f, 0f, 90f, -0.1f));
  }

  @Test
  void constructorAcceptsZeroRadius() {
    assertDoesNotThrow(() -> new SweepComponent(1f, 0f, 90f, 0f));
  }

  @Test
  void updateIsNoOpWithoutFollowComponent() {
    Entity hitbox = new Entity();
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 2f);
    hitbox.addComponent(sweep);

    assertDoesNotThrow(sweep::update);
    // Should return before ever touching the time source.
    verify(gameTime, never()).getDeltaTime();
  }

  @Test
  void updateSetsOffsetAtStartAngleOnFirstFrame() {
    Entity owner = new Entity();
    Entity hitbox = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    hitbox.addComponent(follow);
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 2f);
    hitbox.addComponent(sweep);

    when(gameTime.getDeltaTime()).thenReturn(0f);
    sweep.update();

    Vector2 expected = new Vector2(2f, 0f).setAngleDeg(0f);
    Vector2 actual = follow.getLocalOffset();
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }

  @Test
  void updateInterpolatesAngleAtHalfDuration() {
    Entity owner = new Entity();
    Entity hitbox = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    hitbox.addComponent(follow);
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 1f);
    hitbox.addComponent(sweep);

    when(gameTime.getDeltaTime()).thenReturn(0.5f); // half of the 1s duration
    sweep.update();

    Vector2 expected = new Vector2(1f, 0f).setAngleDeg(45f);
    Vector2 actual = follow.getLocalOffset();
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }

  @Test
  void updateClampsAtEndAngleOncePastDuration() {
    Entity owner = new Entity();
    Entity hitbox = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    hitbox.addComponent(follow);
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 1f);
    hitbox.addComponent(sweep);

    when(gameTime.getDeltaTime()).thenReturn(5f); // far beyond the 1s duration
    sweep.update();

    Vector2 expected = new Vector2(1f, 0f).setAngleDeg(90f);
    Vector2 actual = follow.getLocalOffset();
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }

  @Test
  void updateAccumulatesElapsedTimeAcrossCalls() {
    Entity owner = new Entity();
    Entity hitbox = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    hitbox.addComponent(follow);
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 1f);
    hitbox.addComponent(sweep);

    when(gameTime.getDeltaTime()).thenReturn(0.25f);
    sweep.update(); // elapsed 0.25s -> t=0.25 -> 22.5 deg
    sweep.update(); // elapsed 0.5s  -> t=0.5  -> 45 deg

    Vector2 expected = new Vector2(1f, 0f).setAngleDeg(45f);
    Vector2 actual = follow.getLocalOffset();
    assertEquals(expected.x, actual.x, 0.01f);
    assertEquals(expected.y, actual.y, 0.01f);
  }

  @Test
  void negativeDeltaTimeTreatedAsZero() {
    Entity owner = new Entity();
    Entity hitbox = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    hitbox.addComponent(follow);
    SweepComponent sweep = new SweepComponent(1f, 0f, 90f, 1f);
    hitbox.addComponent(sweep);

    when(gameTime.getDeltaTime()).thenReturn(-1f);
    sweep.update();

    Vector2 expected = new Vector2(1f, 0f).setAngleDeg(0f); // no progress should be made
    Vector2 actual = follow.getLocalOffset();
    assertEquals(expected.x, actual.x, 0.001f);
    assertEquals(expected.y, actual.y, 0.001f);
  }
}
