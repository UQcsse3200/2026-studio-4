package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.function.Function;

/** Builds the labelled rows inside the settings menu. */
public class SettingsForm {
  private final Skin skin;
  private final Table table;

  public SettingsForm(Skin skin) {
    this.skin = skin;
    table = new Table();
  }

  public Table table() {
    return table;
  }

  /** Adds a section heading. The title style is scaled down so it sits above the rows. */
  public void section(String title) {
    Label heading = new Label(title, skin, "title");
    heading.setFontScale(0.45f);
    table.row().padTop(22f);
    table.add(heading).left().colspan(2).padBottom(6f);
  }

  public void row(String name, Actor control) {
    table.row().padTop(8f);
    table.add(new Label(name, skin)).right().padRight(18f);
    table.add(control).left().growX();
  }

  /** Slider plus a live value label. The listener does not consume the drag event. */
  public Label slider(String name, Slider slider, String format) {
    return slider(name, slider, value -> String.format(format, value));
  }

  /** Volume slider shown as a percentage. */
  public Label percent(String name, Slider slider) {
    return slider(name, slider, value -> String.format("%.0f%%", value * 100f));
  }

  private Label slider(String name, Slider slider, Function<Float, String> format) {
    Label value = new Label(format.apply(slider.getValue()), skin);
    Table line = new Table();
    line.add(slider).width(200f);
    line.add(value).padLeft(10f);
    slider.addListener(
        event -> {
          value.setText(format.apply(slider.getValue()));
          return false;
        });
    row(name, line);
    return value;
  }
}
