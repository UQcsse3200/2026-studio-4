package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomLightingComponentTest {
  private static RoomLightingComponent room(LightSource... lights) {
    return new RoomLightingComponent(
        0.8f, Color.BLACK, List.of(lights), new Vector2(0, 0), new Vector2(10, 10));
  }

  @Test
  void unlitRoomIsUniformlyDark() {
    RoomLightingComponent lighting = room();
    assertEquals(0.8f, lighting.shadeAt(1f, 1f, 0f), 0.2f / RoomLightingComponent.BANDS);
    assertEquals(lighting.shadeAt(1f, 1f, 0f), lighting.shadeAt(9f, 9f, 0f));
  }

  @Test
  void lightIsBrightestAtItsCentreAndFadesOut() {
    RoomLightingComponent lighting = room(new LightSource(5f, 5f, 2f, Color.WHITE, 0f));
    float centre = lighting.shadeAt(5f, 5f, 0f);
    float near = lighting.shadeAt(6f, 5f, 0f);
    float outside = lighting.shadeAt(8f, 5f, 0f);
    assertEquals(0f, centre);
    assertTrue(centre < near && near < outside);
    assertEquals(lighting.shadeAt(1f, 1f, 0f), outside);
  }

  @Test
  void shadeIsSteppedIntoBands() {
    RoomLightingComponent lighting = room(new LightSource(5f, 5f, 3f, Color.WHITE, 0f));
    for (float x = 5f; x < 9f; x += 0.05f) {
      float steps = lighting.shadeAt(x, 5f, 0f) * RoomLightingComponent.BANDS;
      assertEquals((float) Math.round(steps), steps, 1e-4f);
    }
  }

  @Test
  void overlappingLightsBrightenMoreThanEitherAlone() {
    LightSource left = new LightSource(4f, 5f, 2f, Color.WHITE, 0f);
    LightSource right = new LightSource(6f, 5f, 2f, Color.WHITE, 0f);
    float between = room(left, right).shadeAt(5f, 5f, 0f);
    assertTrue(between < room(left).shadeAt(5f, 5f, 0f));
  }

  @Test
  void flamesFlickerButSteadyLightsDoNot() {
    LightSource steady = new LightSource(0f, 0f, 2f, Color.WHITE, 0f);
    LightSource candle = new LightSource(0f, 0f, 2f, Color.WHITE, 0.6f);
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    for (float t = 0f; t < 5f; t += 0.03f) {
      assertEquals(1f, steady.intensity(t));
      float i = candle.intensity(t);
      min = Math.min(min, i);
      max = Math.max(max, i);
    }
    assertTrue(max <= 1f && min > 0.7f, "flicker too strong: " + min);
    assertTrue(max - min > 0.05f, "candle should visibly flicker");
  }
}
