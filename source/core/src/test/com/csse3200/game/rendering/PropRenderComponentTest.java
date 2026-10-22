package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PropRenderComponentTest {
  private static float z(RenderComponent component, float y) {
    Entity entity = new Entity().addComponent(component);
    entity.setPosition(0f, y);
    return component.getZIndex();
  }

  @Test
  void bookOnATableDrawsAfterTheTableAndItsGlow() {
    float table = z(new PropRenderComponent(new TextureRegion(), 0f, false), 5f);
    float glow =
        z(new GlowRenderComponent(Color.WHITE, 0.4f, new Vector2(), 0.44f, () -> true), 5f);
    float book = z(new PropRenderComponent(new TextureRegion(), 0.44f, false), 5f);

    // Lower z draws first.
    assertTrue(table < glow, "glow must draw over the table");
    assertTrue(glow < book, "book must draw over its glow");
  }

  @Test
  void elevatedPropStillDrawsBehindSomethingInFrontOfTheTable() {
    float book = z(new PropRenderComponent(new TextureRegion(), 0.44f, false), 5f);
    float playerInFront = -4.9f; // RenderComponent default for an entity at y = 4.9
    assertTrue(book < playerInFront);
  }

  @Test
  void rugsAlwaysDrawFirst() {
    float rug = z(new PropRenderComponent(new TextureRegion(), 0f, true), 0f);
    float farWall = z(new PropRenderComponent(new TextureRegion(), 0f, false), 1000f);
    assertEquals(PropRenderComponent.FLOOR_Z, rug);
    assertTrue(rug < farWall);
  }

  @Test
  void glowPulsesWithinItsRange() {
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    for (long t = 0; t < 4000; t += 20) {
      float a = GlowRenderComponent.pulseAlpha(t);
      min = Math.min(min, a);
      max = Math.max(max, a);
    }
    assertTrue(min >= 0.24f && max <= 0.61f, "alpha out of range: " + min + ".." + max);
    assertTrue(max - min > 0.3f, "glow should visibly pulse");
  }
}
