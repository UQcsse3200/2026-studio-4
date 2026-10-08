package com.csse3200.game.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;

/** Applies the saved interface scale around the centre of a widget or the whole stage. */
public final class UiScale {
  private UiScale() {
    throw new IllegalStateException("Instantiating static util class");
  }

  public static float clamp(float value) {
    if (!Float.isFinite(value)) {
      return 1f;
    }
    return Math.clamp(value, 0.2f, 2f);
  }

  /** Scales one group around its own centre. */
  public static void apply(Group group, float scale) {
    if (group == null) {
      return;
    }
    float factor = clamp(scale);
    group.setTransform(true);
    group.setOrigin(Align.center);
    group.setScale(factor);
  }

  /** Scales every UI actor on the stage around the middle of the screen. */
  public static void applyToStage(Stage stage, float scale) {
    if (stage == null) {
      return;
    }
    float factor = clamp(scale);
    stage.getRoot().setTransform(true);
    stage.getRoot().setSize(stage.getWidth(), stage.getHeight());
    stage.getRoot().setOrigin(Align.center);
    stage.getRoot().setScale(factor);
  }
}
