package com.csse3200.game.areas.attic;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.rooms.configs.ExitConfig;
import com.csse3200.game.components.rooms.configs.LightConfig;
import com.csse3200.game.components.rooms.configs.PropConfig;
import com.csse3200.game.components.rooms.configs.RoomConfig;
import com.csse3200.game.rendering.LightSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which attic props give off light, and how. Used to light dark rooms (see {@link
 * RoomConfig#darkness}): every candle, lamp, stove and tome placed in the room becomes a light,
 * plus any explicit {@link RoomConfig#lights} such as moonlight from a window.
 */
public final class AtticLighting {
  private static final float TILE = 0.5f;

  /** How a prop glows: radius in world units, colour, flicker 0..1, and source offset in pixels. */
  private record Spec(float radius, Color color, float flicker, float offsetX, float offsetY) {}

  private static final Color CANDLE = new Color(1f, 0.72f, 0.38f, 1f);
  private static final Color FIRE = new Color(1f, 0.5f, 0.2f, 1f);
  private static final Color LAMP = new Color(1f, 0.82f, 0.55f, 1f);

  private static final Map<AtticProp, Spec> SPECS = new EnumMap<>(AtticProp.class);

  static {
    SPECS.put(AtticProp.CANDLE, new Spec(1.8f, CANDLE, 0.6f, 8, 10));
    SPECS.put(AtticProp.WOOD_STOVE, new Spec(3.2f, FIRE, 0.8f, 16, 14));
    SPECS.put(AtticProp.FLOOR_LAMP, new Spec(3f, LAMP, 0.1f, 8, 40));
    SPECS.put(AtticProp.CANDLE_STAND, new Spec(2.2f, CANDLE, 0.6f, 8, 28));
    SPECS.put(AtticProp.HANGING_LANTERN, new Spec(2.8f, LAMP, 0.3f, 8, 6));
    SPECS.put(AtticProp.CRYSTAL_BALL, new Spec(1.3f, new Color(0.7f, 0.5f, 1f, 1f), 0.2f, 8, 8));
    for (AtticProp tome :
        new AtticProp[] {
          AtticProp.TOME_TEMPLE,
          AtticProp.TOME_DESERT,
          AtticProp.TOME_FROST,
          AtticProp.TOME_DRAGON,
          AtticProp.TOME_FINAL
        }) {
      SPECS.put(tome, new Spec(1.1f, tome.getGlow(), 0.15f, 8.5f, 5));
    }
  }

  private AtticLighting() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** Whether a prop gives off light. */
  public static boolean isLightSource(AtticProp prop) {
    return SPECS.containsKey(prop);
  }

  /** All lights in a room: lit props, lit exit sprites, and explicit lights from the config. */
  public static List<LightSource> lightsFor(RoomConfig room) {
    List<LightSource> lights = new ArrayList<>();
    for (PropConfig prop : room.props) {
      add(lights, AtticProp.fromName(prop.prop), prop.x, prop.y, prop.elevation);
    }
    for (ExitConfig exit : room.exits) {
      if (exit.sprite != null) {
        add(lights, AtticProp.fromName(exit.sprite), exit.x, exit.y, exit.elevation);
      }
    }
    Arrays.stream(room.lights).map(AtticLighting::toLightSource).forEach(lights::add);
    return lights;
  }

  /** A free-standing light from the config, centred on its tile. */
  private static LightSource toLightSource(LightConfig light) {
    return new LightSource(
        light.x * TILE + TILE / 2f,
        light.y * TILE + TILE / 2f,
        light.radius,
        Color.valueOf(light.color),
        light.flicker);
  }

  private static void add(
      List<LightSource> lights, AtticProp prop, int x, int y, float elevationPixels) {
    Spec spec = SPECS.get(prop);
    if (spec == null) {
      return;
    }
    lights.add(
        new LightSource(
            x * TILE + AtticSprites.toWorld(spec.offsetX()),
            y * TILE + AtticSprites.toWorld(spec.offsetY() + elevationPixels),
            spec.radius(),
            spec.color(),
            spec.flicker()));
  }
}
