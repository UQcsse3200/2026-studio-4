package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.services.RunTimer;
import com.csse3200.game.ui.UIComponent;

/** Displays the total run time and current dungeon time in the upper-right corner. */
public class TimerDisplay extends UIComponent {
  private final RunTimer runTimer;
  private boolean visible = true;
  private Label totalLabel;
  private Label dungeonLabel;
  private Table panel;

  /** Creates a display backed by the supplied game runTimer. */
  public TimerDisplay(RunTimer runTimer) {
    this.runTimer = runTimer;
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
    dungeonLabel = new Label("Dungeon times", dungeonStyle);
    panel = new Table();
    panel.setBackground(skin.newDrawable("window-c", Color.BLACK));
    panel.pad(8f, 12f, 8f, 12f);
    panel.add(totalLabel).right().row();
    panel.add(dungeonLabel).right();
    stage.addActor(panel);
  }

  /** Updates the runTimer text, positions the labels, and keeps them above other UI actors. */
  @Override
  public void draw(SpriteBatch batch) {
    if (totalLabel == null || dungeonLabel == null) {
      return;
    }

    totalLabel.setText(runTimer.formatTime(runTimer.getTotalTime()));
    StringBuilder dungeonTimes = new StringBuilder("Dungeons:");
    runTimer
      .getDungeonTimes()
      .forEach(
        (dungeonId, seconds) ->
          dungeonTimes
            .append('\n')
            .append(displayDungeonName(dungeonId))
            .append(": ")
            .append(runTimer.formatTime(seconds)));
    if (runTimer.getDungeonTimes().isEmpty()) {
      dungeonTimes.append("\n--");
    }
    dungeonLabel.setText(dungeonTimes.toString());

    panel.setVisible(visible);

    totalLabel.setFontScale(2.5f);
    dungeonLabel.setFontScale(1.0f);

    float rightMargin = 24f;
    float topMargin = 24f;
    float spacing = 4f;

    panel.pack();
    panel.setPosition(
        stage.getWidth() - panel.getWidth() - rightMargin,
        stage.getHeight() - panel.getHeight() - topMargin);
    panel.toFront();
  }

  private String displayDungeonName(String dungeonId) {
    String spaced = dungeonId.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  /** Shows or hides both runTimer labels without stopping the timers. */
  public void toggle() {
    visible = !visible;
    panel.setVisible(visible);
  }

  /** Handles the T key used to toggle the runTimer display. */
  public static class ToggleInput extends InputComponent {
    private final TimerDisplay display;

    /** Creates a keyboard handler for the supplied runTimer display. */
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

  /** Removes both runTimer labels from the UI stage. */
  @Override
  public void dispose() {
    super.dispose();

    if (panel != null) {
      panel.remove();
      panel = null;
      totalLabel = null;
      dungeonLabel = null;
    }
  }
}
