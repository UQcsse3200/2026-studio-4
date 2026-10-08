package com.csse3200.game.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Align;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Scales each interface panel from the edge it already sits on. The settings Apply and Exit bar is
 * left at normal size so it cannot leave the screen.
 */
public final class UiScale {
  public static final float MIN = 0.5f;
  public static final float MAX = 2f;
  public static final String SETTINGS_CONTENT = "settings-content";
  public static final String SETTINGS_ACTIONS = "settings-actions";
  public static final String SETTINGS_BACKDROP = "settings-backdrop";

  private static final Map<Actor, Float> baseScale = new WeakHashMap<>();

  private UiScale() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static float clamp(float value) {
    if (!Float.isFinite(value)) {
      return 1f;
    }
    return Math.clamp(value, MIN, MAX);
  }

  /** Scales one group downward from its top edge. */
  public static void apply(Group group, float scale) {
    apply(group, scale, Align.top);
  }

  /** Scales one group around the given edge. The edge itself stays put. */
  public static void apply(Group group, float scale, int align) {
    if (group == null) {
      return;
    }
    float factor = clamp(scale);
    if (group instanceof WidgetGroup widget) {
      widget.validate();
    }
    float base = baseScale.computeIfAbsent(group, actor -> actor.getScaleX());
    group.setTransform(true);
    group.setOrigin(align);
    group.setScale(base * factor);
  }

  /** Scales each panel on the stage from its own edge. The stage itself is not scaled. */
  public static void applyToStage(Stage stage, float scale) {
    if (stage == null) {
      return;
    }
    float factor = clamp(scale);
    Group root = stage.getRoot();
    root.setTransform(false);
    root.setScale(1f);
    root.setSize(stage.getWidth(), stage.getHeight());
    for (Actor actor : root.getChildren()) {
      if (!(actor instanceof Group group)) {
        continue;
      }
      if (SETTINGS_ACTIONS.equals(group.getName()) || SETTINGS_BACKDROP.equals(group.getName())) {
        apply(group, 1f, Align.bottom);
      } else if (SETTINGS_CONTENT.equals(group.getName())) {
        apply(group, factor, Align.top);
      } else {
        apply(group, factor, anchor(group));
      }
    }
  }

  private static int anchor(Group group) {
    if (group instanceof WidgetGroup widget) {
      widget.validate();
    }
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    boolean any = false;
    for (Actor child : group.getChildren()) {
      if (!child.isVisible()) {
        continue;
      }
      any = true;
      minX = Math.min(minX, child.getX());
      minY = Math.min(minY, child.getY());
      maxX = Math.max(maxX, child.getX() + child.getWidth());
      maxY = Math.max(maxY, child.getY() + child.getHeight());
    }
    if (!any || group.getWidth() <= 0f || group.getHeight() <= 0f) {
      return Align.center;
    }
    float centerX = (minX + maxX) * 0.5f;
    float centerY = (minY + maxY) * 0.5f;
    int align = 0;
    if (centerX < group.getWidth() * 0.33f) {
      align |= Align.left;
    } else if (centerX > group.getWidth() * 0.67f) {
      align |= Align.right;
    }
    if (centerY > group.getHeight() * 0.67f) {
      align |= Align.top;
    } else if (centerY < group.getHeight() * 0.33f) {
      align |= Align.bottom;
    }
    return align == 0 ? Align.center : align;
  }
}
