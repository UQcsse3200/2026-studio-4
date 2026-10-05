package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.ui.UIComponent;

/** Displays the name of the current game area. */
public class GameAreaDisplay extends UIComponent {
  private String gameAreaName = "";

  private Label status;
  private Table statusTable;
  private long statusExpiryMillis;

  private Label achievement;
  private Table achievementTable;
  private long achievementExpiryMillis;

  private Table titleTable;

  public GameAreaDisplay(String gameAreaName) {
    this.gameAreaName = gameAreaName;
  }

  @Override
  public void create() {
    super.create();
    addActors();
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

    achievement = new Label("", skin);
    achievementTable = new Table();
    achievementTable.add(achievement);
    achievementTable.setFillParent(true);
    achievementTable.center().top().padTop(40f); // just below the status label

    stage.addActor(achievementTable);
    stage.addActor(titleTable);
    stage.addActor(statusTable);
  }

  /** Shows a brief interaction message below the room title. */
  public void showStatus(String message) {
    status.setText(message);
    statusExpiryMillis = TimeUtils.millis() + 2500;
  }

  /** Shows an achievement-unlocked toast below the status message. */
  public void showAchievement(String name) {
    achievement.setText("Achievement Unlocked: " + name);
    achievementExpiryMillis = TimeUtils.millis() + 4000;
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (statusExpiryMillis > 0 && TimeUtils.millis() >= statusExpiryMillis) {
      status.setText("");
      statusExpiryMillis = 0;
    }
    if (achievementExpiryMillis > 0 && TimeUtils.millis() >= achievementExpiryMillis) {
      achievement.setText("");
      achievementExpiryMillis = 0;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    titleTable.remove();
    statusTable.remove();
    achievementTable.remove();
  }
}
