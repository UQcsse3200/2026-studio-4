package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/**
 * Displays the total run time and current dungeon time in a HUD panel in the upper-right corner.
 *
 * <p>The panel has a dark rounded background and an accent bar on its left edge. The bar is cyan
 * while exploring and pulses orange while a dungeon is being timed. The dungeon row is dimmed and
 * shows {@code --:--} when no dungeon is active. Press T to fade the panel in and out.
 */
public class TimerDisplay extends UIComponent {
  private static final float EDGE_MARGIN = 20f;

  private RunTimer runTimer;
  private boolean visible = true;

  private Table rootTable;
  private Label runTime;
  private Label dungeonCaption;
  private Label dungeonTime;

  /** Builds the HUD panel and adds it to the shared UI stage. */
  @Override
  public void create() {
    super.create();
    runTimer = ServiceLocator.getRunTimer();

    Label runCaption = new Label("RUN: ", skin, "caption");
    runCaption.setFontScale(1.2f);
    runTime = new Label("00:00", skin, "runTime");
    dungeonCaption = new Label("DUNGEON", skin, "caption");
    dungeonTime = new Label("--:--", skin, "dungeonTime");

    Drawable ab = hud.getDrawable("timer_back");
    ab.setMinWidth(190f); // Set your desired fixed width
    ab.setMinHeight(170f);
    Table content = new Table();
    content.defaults().left();
    content.add(runCaption).row();
    content.add(runTime).row();
    content.add(dungeonCaption).row();
    content.add(dungeonTime).center().row();
    content.setBackground(ab);

    rootTable = new Table();
    content.defaults().center();
    rootTable.setFillParent(true);
    rootTable.bottom().left().pad(EDGE_MARGIN);
    rootTable.add(content);
    rootTable.setTransform(true);
    rootTable.setScale(0.9f);

    stage.addActor(rootTable);
    rootTable.toFront();
  }

  /** Refreshes the time text and accent animation every frame. */
  @Override
  public void draw(SpriteBatch batch) {
    if (rootTable == null) {
      return;
    }

    boolean dungeonActive = runTimer.getCurrentDungeonId() != null;

    runTime.setText(runTimer.formatTimeWithNoMilliSec(runTimer.getTotalTime()));
    if (dungeonActive) {
      dungeonTime.setText(runTimer.formatTimeWithNoMilliSec(runTimer.getDungeonSyncedTime()));
      dungeonCaption.setText("DUNGEON: " + id(runTimer.getCurrentDungeonId()));
    } else {
      dungeonTime.setText("--:--");
      dungeonCaption.setText("DUNGEON");
    }

    rootTable.toFront();
  }

  private String id(String dungeonId) {
    return switch (dungeonId) {
      case "dungeonOne" -> "1";
      case "dungeonTwo" -> "2";
      case "dungeonThree" -> "3";
      case "dungeonFour" -> "4";
      case "finalDungeon" -> "END";
      default -> "";
    };
  }

  /** Fades the panel in or out without stopping the timers. */
  public void toggle() {
    visible = !visible;
    rootTable.setVisible(visible);
  }

  /** Handles the T key used to toggle the timer display. */
  public static class ToggleInput extends InputComponent {
    private final TimerDisplay display;

    /** Creates a keyboard handler for the supplied timer display. */
    public ToggleInput(TimerDisplay display) {
      super(5);
      this.display = display;
    }

    /** Toggles the display when T is pressed. */
    @Override
    public boolean keyDown(int keycode) {
      if (keycode == Input.Keys.T) {
        display.toggle();
        return true;
      }
      return false;
    }
  }

  /** Removes the panel from the UI stage and frees the textures it created. */
  @Override
  public void dispose() {
    super.dispose();

    if (rootTable != null) {
      rootTable.remove();
      rootTable = null;
    }
  }
}
