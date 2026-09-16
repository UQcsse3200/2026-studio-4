package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@link RadialTextureFactory}: the circle every area visual draws. Asserted on the pixels asked
 * for rather than the finished {@link Texture}, which a headless test cannot read back.
 */
@ExtendWith(GameExtension.class)
class RadialTextureFactoryTest {
  private static final int SIZE = 16;

  /** Records the distance handed to the profile for each pixel it was asked about. */
  private static List<Float> distancesAskedFor(int size) {
    List<Float> distances = new ArrayList<>();
    RadialTextureFactory.create(
        size,
        distance -> {
          distances.add(distance);
          return 1f;
        });
    return distances;
  }

  @Test
  void asksOnlyAboutPixelsInsideTheCircle() {
    List<Float> distances = distancesAskedFor(SIZE);

    assertTrue(
        distances.stream().allMatch(d -> d < 1f), "a pixel outside the circle is left clear");
    // A circle of radius r fills pi*r^2 of the 4*r^2 square, so roughly 79% of it.
    int filled = distances.size();
    assertTrue(
        filled > SIZE * SIZE * 0.7 && filled < SIZE * SIZE * 0.85,
        "expected about three quarters of the square to be inside the circle, got " + filled);
  }

  @Test
  void reachesBothTheCentreAndTheEdge() {
    List<Float> distances = distancesAskedFor(SIZE);

    assertTrue(
        distances.stream().anyMatch(d -> d < 0.1f), "the centre of the circle must be drawn");
    assertTrue(distances.stream().anyMatch(d -> d > 0.9f), "the rim of the circle must be drawn");
  }

  @Test
  void isSymmetricAboutTheCentre() {
    List<Float> distances = distancesAskedFor(SIZE);

    // Every distance appears an even number of times: pixel centres are mirrored, never on the
    // axis, so an off-by-half-a-pixel centre would show up as an odd count.
    for (float distance : distances) {
      long occurrences = distances.stream().filter(d -> d == distance).count();
      assertEquals(0L, occurrences % 2L, "distance " + distance + " is not mirrored");
    }
  }

  @Test
  void buildsASquareTextureOfTheRequestedSize() {
    Texture texture = RadialTextureFactory.create(SIZE, distance -> 1f);

    assertEquals(SIZE, texture.getWidth());
    assertEquals(SIZE, texture.getHeight());
    assertEquals(Texture.TextureFilter.Linear, texture.getMinFilter());
    assertEquals(Texture.TextureFilter.Linear, texture.getMagFilter());
  }

  @Test
  void letsAThrowingProfileOutRatherThanReturningHalfACircle() {
    assertThrows(
        IllegalStateException.class,
        () ->
            RadialTextureFactory.create(
                SIZE,
                distance -> {
                  throw new IllegalStateException("bad profile");
                }));
  }

  @Test
  void cannotBeInstantiated() throws NoSuchMethodException {
    Constructor<RadialTextureFactory> constructor =
        RadialTextureFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
