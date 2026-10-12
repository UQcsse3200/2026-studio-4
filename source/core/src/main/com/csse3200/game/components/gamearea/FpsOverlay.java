package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.ui.UiScale;

/** Small FPS readout. Hidden unless Settings → Show FPS is on. */
public class FpsOverlay extends UIComponent {
  private Table table;
  private Label label;
  private boolean showReadout;

  @Override
  public void create() {
    super.create();
    applyVisibility();
  }

  @Override
  public void update() {
    applyVisibility();
  }

  /** Shows or hides the counter from the saved setting, including after Apply in the same run. */
  private void applyVisibility() {
    if (UiScale.settingsOpen(stage)) {
      return;
    }
    showReadout = UserSettings.get().showFps;
    if (showReadout && table == null && stage != null) {
      label = new Label("FPS 0", skin, "caption");
      table = new Table();
      table.setFillParent(true);
      table.bottom().right().padBottom(16f).padRight(16f);
      table.add(label);
      stage.addActor(table);
    }
    if (table != null) {
      table.setVisible(showReadout);
    }
    if (!showReadout || label == null || Gdx.graphics == null) {
      return;
    }
    label.setText("FPS " + Gdx.graphics.getFramesPerSecond());
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public void dispose() {
    if (table != null) {
      table.remove();
    }
    super.dispose();
  }
}
