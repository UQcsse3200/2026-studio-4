package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayDeque;
import java.util.Queue;

/** Displays the name of the current game area. */
public class GameAreaDisplay extends UIComponent {
  private String gameAreaName = "";

  private Label status;
  private Table statusTable;
  private long statusExpiryMillis;


  private Table titleTable;

  public GameAreaDisplay(String gameAreaName) {
    this.gameAreaName = gameAreaName;
  }

  public static final String DISPLAY = "display";

  @Override
  public void create() {
    super.create();
    addActors();

    entity.getEvents().addListener(DISPLAY, this::showStatus);
  }

  private void addActors() {
    Label title = new Label(this.gameAreaName, skin, "large");
    titleTable = new Table();
    titleTable.add(title);
    titleTable.setFillParent(true);
    titleTable.top().left();

    status = new Label("", skin);
    statusTable = new Table();
    statusTable.add(status);
    statusTable.setFillParent(true);
    statusTable.center().top();

    stage.addActor(titleTable);
    stage.addActor(statusTable);
  }

  /** Shows a brief interaction message below the room title. */
  public void showStatus(String message) {
    status.setText(message);
    statusExpiryMillis = TimeUtils.millis() + 2500;
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (statusExpiryMillis > 0 && TimeUtils.millis() >= statusExpiryMillis) {
      status.setText("");
      statusExpiryMillis = 0;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    titleTable.remove();
    statusTable.remove();
  }
}
