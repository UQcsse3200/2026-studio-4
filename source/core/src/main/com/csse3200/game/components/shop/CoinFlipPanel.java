package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Selected Gemini coin-flip layout, reusing Aarash's coin art and squash animation. */
public final class CoinFlipPanel extends Table {
  private final CoinFlipGame game;
  private final Consumer<CoinFlipGame.Outcome> onResolved;
  private final Label stakeLabel;
  private final Label result;
  private final TextButton down;
  private final TextButton up;
  private final TextButton flip;
  private final Image coin;
  private boolean active;
  private boolean animating;
  private CoinFlipGame.Outcome lastOutcome;

  public CoinFlipPanel(
      CoinFlipGame game,
      Texture coinTexture,
      Appearance appearance,
      Consumer<CoinFlipGame.Outcome> onResolved) {
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    TextButton.TextButtonStyle wagerStyle = appearance.wager();
    TextButton.TextButtonStyle flipStyle = appearance.flip();
    this.game = game;
    this.onResolved = onResolved;
    setName("coin-flip-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();
    Label rules = new Label("COIN FLIP  |  50/50 chance. Win: +stake. Lose: -stake.", textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();
    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    coin = new Image(coinTexture);
    coin.setOrigin(Align.center);
    arena.add(coin).size(96f).padBottom(16f);
    arena.row();
    result = new Label("Ready to Flip", goldStyle);
    result.setName("coin-flip-result");
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();
    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "coin-flip-down", wagerStyle, () -> adjust(-10));
    up = button("+10", "coin-flip-up", wagerStyle, () -> adjust(10));
    flip = button("FLIP", "coin-flip-button", flipStyle, this::flip);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("coin-flip-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(flip).width(90f).height(42f);
    add(wager).growX().padBottom(12f);
    row();
    Label hint =
        new Label(
            "Stake changes by 10 gold. Below 10 gold, you can bet your remaining coins.",
            textStyle);
    hint.setWrap(true);
    add(hint).growX();
  }

  public record Appearance(
      Label.LabelStyle text,
      Label.LabelStyle gold,
      TextButton.TextButtonStyle wager,
      TextButton.TextButtonStyle flip,
      Drawable parchment,
      Drawable arena) {}

  private TextButton button(
      String text, String name, TextButton.TextButtonStyle style, Runnable action) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && !animating && !button.isDisabled()) action.run();
          }
        });
    return button;
  }

  private void adjust(int delta) {
    game.adjustStake(delta);
    refresh();
  }

  private void flip() {
    // Mark busy before settlement: inventory notifications can synchronously refresh the panel.
    animating = true;
    lastOutcome = game.flip();
    if (lastOutcome == null) {
      animating = false;
      result.setText("This wager is unavailable. Check your gold.");
      refresh();
      return;
    }
    result.setText("Flipping...");
    refresh();
    onResolved.accept(lastOutcome);
    if (!active) return;
    coin.clearActions();
    coin.addAction(
        Actions.sequence(
            Actions.scaleTo(0f, 1f, 0.3f, Interpolation.pow2In),
            Actions.scaleTo(1f, 1f, 0.3f, Interpolation.pow2Out),
            Actions.run(this::finishAnimation)));
  }

  private void finishAnimation() {
    animating = false;
    coin.setScale(1f);
    if (lastOutcome != null) {
      result.setText(
          lastOutcome.won()
              ? "You won " + lastOutcome.stake() + " Gold!"
              : "You lost " + lastOutcome.stake() + " Gold.");
    }
    refresh();
  }

  public void open() {
    active = true;
    lastOutcome = null;
    result.setText("Ready to Flip");
    refresh();
  }

  public void close() {
    active = false;
    coin.clearActions();
    finishAnimation();
  }

  public void refresh() {
    if (!animating) {
      game.refresh();
      stakeLabel.setText(game.getStake() + " Gold");
    }
    down.setDisabled(!active || animating);
    up.setDisabled(!active || animating);
    flip.setDisabled(!active || animating || !game.canFlip());
  }
}
