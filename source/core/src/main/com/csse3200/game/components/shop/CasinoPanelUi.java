package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.BooleanSupplier;

/** Shared presentation and input helpers for the merchant's casino minigames. */
final class CasinoPanelUi {
  private CasinoPanelUi() {}

  static void configure(
      Table panel, String name, CoinFlipPanel.Appearance appearance, String rulesText) {
    panel.setName(name);
    panel.setBackground(appearance.parchment());
    panel.pad(14f);
    panel.top();
    Label rules = new Label(rulesText, appearance.text());
    rules.setWrap(true);
    panel.add(rules).growX().padBottom(12f);
    panel.row();
  }

  static Table arena(CoinFlipPanel.Appearance appearance) {
    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    return arena;
  }

  static Label arenaLabel(String name, Label.LabelStyle style) {
    Label label = new Label("", style);
    label.setName(name);
    label.setWrap(true);
    label.setAlignment(Align.center);
    return label;
  }

  static void addHint(Table panel, String text, Label.LabelStyle style) {
    Label hint = new Label(text, style);
    hint.setWrap(true);
    panel.add(hint).growX();
  }

  /** Checks the panel's current state when the event arrives, including after reopening it. */
  static TextButton button(
      String text,
      String name,
      TextButton.TextButtonStyle style,
      BooleanSupplier active,
      Runnable action) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active.getAsBoolean() && !button.isDisabled()) {
              action.run();
            }
          }
        });
    return button;
  }
}
