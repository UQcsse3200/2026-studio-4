package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.BooleanSupplier;

/**
 * Shared frame, result display and wager controls for the Coin Flip, Higher/Lower and Shell Game
 * panels.
 */
final class CasinoPanelSupport {

  private CasinoPanelSupport() {}

  /** Sets up the frame and rules, then returns the arena for the game's own content. */
  static Table initPanel(
      Table panel, String name, String rules, CoinFlipPanel.Appearance appearance) {
    panel.setName(name);
    panel.setBackground(appearance.parchment());
    panel.pad(14f);
    panel.top();
    wrappedLabel(panel, rules, appearance.text(), 12f);
    panel.row();
    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    return arena;
  }

  /** Finishes the arena with a centred result below the game's content and adds it to the panel. */
  static Label addArena(
      Table panel,
      Table arena,
      String resultName,
      String initialResult,
      Label.LabelStyle style,
      float minHeight) {
    arena.row();
    Label result = new Label(initialResult, style);
    result.setName(resultName);
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX();
    panel.add(arena).growX().minHeight(minHeight).padBottom(12f);
    panel.row();
    return result;
  }

  /** A wrapped, growing label row - used for the rules/hint blurbs under each panel's title. */
  static void wrappedLabel(Table panel, String text, Label.LabelStyle style, float padBottom) {
    Label label = new Label(text, style);
    label.setWrap(true);
    panel.add(label).growX().padBottom(padBottom);
  }

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
