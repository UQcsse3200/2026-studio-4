package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.ui.UIComponent;

/** Displays the name of the current game area. */
public class GameAreaDisplay extends UIComponent {
  private String gameAreaName = "";
  private Label title;
  private Label status;
  private long statusExpiryMillis;

  public GameAreaDisplay(String gameAreaName) {
    this.gameAreaName = gameAreaName;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    title = new Label(this.gameAreaName, skin, "large");
    status = new Label("", skin);
    stage.addActor(title);
    stage.addActor(status);
  }

  /** Shows a brief interaction message below the room title. */
  public void showStatus(String message) {
    status.setText(message);
    statusExpiryMillis = TimeUtils.millis() + 2500;
  }

  @Override
  public void draw(SpriteBatch batch) {
    int screenHeight = Gdx.graphics.getHeight();
    float offsetX = 10f;
    float offsetY = 30f;

    title.setPosition(offsetX, screenHeight - offsetY);
    status.setPosition(offsetX, screenHeight - offsetY - 28f);
    if (statusExpiryMillis > 0 && TimeUtils.millis() >= statusExpiryMillis) {
      status.setText("");
      statusExpiryMillis = 0;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    title.remove();
    status.remove();
  }
}
