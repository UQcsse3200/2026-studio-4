package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.function.Function;

/** Builds the labelled cards inside the settings menu. */
public class SettingsForm {
  private final Skin skin;
  private final Table root;
  private Table card;

  public SettingsForm(Skin skin) {
    this.skin = skin;
    root = new Table();
  }

  public Table table() {
    return root;
  }

  /** Adds a titled card. Later rows are added to this card until the next section. */
  public void section(String title, String caption) {
    card = new Table();
    card.pad(12f, 18f, 14f, 18f);
    card.setBackground(skin.getDrawable("window-w"));

    Label heading = new Label(title, skin, "title");
    heading.setFontScale(0.42f);
    card.add(heading).left().colspan(2).padBottom(2f);

    if (caption != null && !caption.isEmpty()) {
      card.row();
      Label note = new Label(caption, skin, "caption");
      note.setWrap(true);
      card.add(note).left().growX().colspan(2).width(560f).padBottom(6f);
    }

    root.row().padTop(14f);
    root.add(card).growX().pad(0f, 8f, 0f, 8f);
  }

  public void row(String name, Actor control) {
    card.row().padTop(8f);
    card.add(new Label(name, skin)).right().padRight(18f).top();
    card.add(control).left().growX();
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
    line.add(slider).width(220f);
    line.add(value).padLeft(12f).width(72f);
    slider.addListener(
        event -> {
          value.setText(format.apply(slider.getValue()));
          return false;
        });
    row(name, line);
    return value;
  }
}
