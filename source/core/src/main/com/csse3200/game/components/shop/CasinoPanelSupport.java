package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.BooleanSupplier;

/**
 * Shared building blocks for the casino panels (Coin Flip, Higher/Lower, Shell Game): the wager
 * buttons, the stake label and the row that lays them out. Pulled out of the three panels since
 * each one built the same pieces by hand.
 */
final class CasinoPanelSupport {

  private CasinoPanelSupport() {}

  /** A button whose action only fires while {@code enabled} is true and it isn't disabled. */
  static TextButton actionButton(
      String text,
      String name,
      TextButton.TextButtonStyle style,
      BooleanSupplier enabled,
      Runnable action) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (enabled.getAsBoolean() && !button.isDisabled()) action.run();
          }
        });
    return button;
  }

  /** A centred, initially-empty label used to show the current stake. */
  static Label stakeLabel(String name, Label.LabelStyle style) {
    Label label = new Label("", style);
    label.setName(name);
    label.setAlignment(Align.center);
    return label;
  }

  /** The -10 / stake / +10 / action row shared by every casino panel. */
  static Table wagerRow(TextButton down, Label stakeLabel, TextButton up, TextButton action) {
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(action).width(90f).height(42f);
    return wager;
  }
}
