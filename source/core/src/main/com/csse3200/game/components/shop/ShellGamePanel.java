package com.csse3200.game.components.shop;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Shell Game casino panel, styled to match {@link CoinFlipPanel} in the same Casino tab. */
public final class ShellGamePanel extends Table {
  private final ShellGame game;
  private final Consumer<ShellGame.Round> onResolved;
  private final Label result;
  private final Label stakeLabel;
  private final TextButton[] cups = new TextButton[ShellGame.CUP_COUNT];
  private final TextButton down;
  private final TextButton up;
  private final TextButton start;
  private boolean active;

  public ShellGamePanel(
      ShellGame game, CoinFlipPanel.Appearance appearance, Consumer<ShellGame.Round> onResolved) {
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    TextButton.TextButtonStyle wagerStyle = appearance.wager();
    TextButton.TextButtonStyle flipStyle = appearance.flip();
    this.game = game;
    this.onResolved = onResolved;
    setName("shell-game-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();

    Label rules =
        new Label(
            "SHELL GAME  |  Stake your gold, then pick the cup hiding the ball. Guess right to"
                + " double your stake.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    Table cupRow = new Table();
    for (int i = 0; i < cups.length; i++) {
      int cup = i;
      TextButton button = new TextButton("CUP " + (i + 1), flipStyle);
      button.setName("shell-game-cup-" + i);
      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (active && !button.isDisabled()) guess(cup);
            }
          });
      cups[i] = button;
      cupRow.add(button).width(90f).height(60f).padRight(i < cups.length - 1 ? 10f : 0f);
    }
    arena.add(cupRow).padBottom(10f);
    arena.row();
    result = new Label("Set your stake and start", goldStyle);
    result.setName("shell-game-result");
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX();
    add(arena).growX().minHeight(180f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "shell-game-down", wagerStyle, () -> adjust(-10));
    up = button("+10", "shell-game-up", wagerStyle, () -> adjust(10));
    start = button("START", "shell-game-start", flipStyle, this::start);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("shell-game-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(start).width(90f).height(42f);
    add(wager).growX();
  }

  private TextButton button(
      String text, String name, TextButton.TextButtonStyle style, Runnable action) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && !button.isDisabled()) action.run();
          }
        });
    return button;
  }

  private void adjust(int delta) {
    game.adjustStake(delta);
    refresh();
  }

  private void start() {
    if (!game.start()) {
      result.setText("This wager is unavailable. Check your gold.");
      refresh();
      return;
    }
    result.setText("The cups are shuffling... pick one!");
    pulseCups();
    refresh();
  }

  private void guess(int cup) {
    ShellGame.Round round = game.guess(cup);
    if (round == null) {
      refresh();
      return;
    }
    result.setText(
        round.won()
            ? "Correct! The ball was under cup "
                + (round.winningCup() + 1)
                + ". Won "
                + round.payout()
                + " Gold!"
            : "Wrong cup. The ball was under cup " + (round.winningCup() + 1) + ".");
    onResolved.accept(round);
    refresh();
  }

  private void pulseCups() {
    for (TextButton cup : cups) {
      cup.clearActions();
      cup.setOrigin(Align.center);
      cup.addAction(
          Actions.sequence(
              Actions.scaleTo(0.85f, 1.1f, 0.15f, Interpolation.pow2In),
              Actions.scaleTo(1f, 1f, 0.15f, Interpolation.pow2Out)));
    }
  }

  public void open() {
    active = true;
    result.setText("Set your stake and start");
    refresh();
  }

  public void close() {
    active = false;
  }

  public void refresh() {
    stakeLabel.setText(game.getStake() + " Gold");
    boolean idle = !game.isRoundActive();
    down.setDisabled(!active || !idle);
    up.setDisabled(!active || !idle);
    start.setDisabled(!active || !idle || !game.canStart());
    for (TextButton cup : cups) {
      cup.setDisabled(!active || idle);
    }
  }
}
