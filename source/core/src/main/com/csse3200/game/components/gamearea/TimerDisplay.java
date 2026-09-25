package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.Timer;
import com.csse3200.game.ui.UIComponent;

/** Displays the total run time and current dungeon time in the upper-right corner. */
public class TimerDisplay extends UIComponent {
  private final Timer timer;
  private boolean visible = true;
  private Label totalLabel;
  private Label dungeonLabel;

  /** Creates a display backed by the supplied game timer. */
  public TimerDisplay(Timer timer) {
    this.timer = timer;
  }

  /** Creates and adds the total and dungeon labels to the shared UI stage. */
  @Override
  public void create() {
    super.create();
    Label.LabelStyle totalStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    Label.LabelStyle dungeonStyle = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    totalStyle.fontColor = Color.GREEN.cpy();
    dungeonStyle.fontColor = Color.GREEN.cpy();

    totalLabel = new Label("Total: 00:00.00", totalStyle);
    dungeonLabel = new Label("Dungeon: 00:00.00", dungeonStyle);
    stage.addActor(dungeonLabel);
    stage.addActor(totalLabel);

    dungeonLabel.toFront();
    totalLabel.toFront();
  }

  /** Updates the timer text, positions the labels, and keeps them above other UI actors. */
  @Override
  public void draw(SpriteBatch batch) {
    if (totalLabel == null || dungeonLabel == null) {
      return;
    }

    totalLabel.setText(timer.formatTime(timer.getTotalTime()));
    dungeonLabel.setText("Dungeon " + timer.formatTime(timer.getDungeonTime()));

    totalLabel.setVisible(visible);
    dungeonLabel.setVisible(visible);

    totalLabel.setFontScale(2.5f);
    dungeonLabel.setFontScale(1.0f);

    float rightMargin = 24f;
    float topMargin = 24f;
    float spacing = 4f;

    totalLabel.setPosition(
        stage.getWidth() - totalLabel.getWidth() - rightMargin,
        stage.getHeight() - totalLabel.getHeight() - topMargin);

    dungeonLabel.setPosition(
        stage.getWidth() - dungeonLabel.getWidth() - rightMargin,
        totalLabel.getY() - dungeonLabel.getHeight() - spacing);

    dungeonLabel.toFront();
    totalLabel.toFront();
  }

  /** Shows or hides both timer labels without stopping the timers. */
  public void toggle() {
    visible = !visible;
    totalLabel.setVisible(visible);
    dungeonLabel.setVisible(visible);
  }

  /** Handles the T key used to toggle the timer display. */
  public static class ToggleInput extends InputComponent {
    private final TimerDisplay display;

    /** Creates a keyboard handler for the supplied timer display. */
    public ToggleInput(TimerDisplay display) {
      super(20);
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

  /** Removes both timer labels from the UI stage. */
  @Override
  public void dispose() {
    super.dispose();

    if (totalLabel != null) {
      totalLabel.remove();
      totalLabel = null;
    }

    if (dungeonLabel != null) {
      dungeonLabel.remove();
      dungeonLabel = null;
    }
  }
}
