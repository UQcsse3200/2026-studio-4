package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.ui.UIComponent;

/** Small FPS readout. Hidden unless Settings → Show FPS is on. */
public class FpsOverlay extends UIComponent {
  private Label label;
  private boolean showReadout;

  @Override
  public void create() {
    super.create();
    showReadout = UserSettings.get().showFps;
    if (!showReadout || stage == null) {
      return;
    }
    label = new Label("FPS 0", skin, "small");
    label.setPosition(16f, stage.getViewport().getScreenHeight() - 28f);
    stage.addActor(label);
  }

  @Override
  public void update() {
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
    if (label != null) {
      label.remove();
    }
    super.dispose();
  }
}
