package com.csse3200.game.components.shop;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
    CasinoPanelSupport.initPanel(this, "shell-game-panel", appearance.parchment());

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
      TextButton button =
          CasinoPanelSupport.actionButton(
              "CUP " + (i + 1), "shell-game-cup-" + i, flipStyle, () -> active, () -> guess(cup));
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
    down =
        CasinoPanelSupport.actionButton(
            "-10", "shell-game-down", wagerStyle, () -> active, () -> adjust(-10));
    up =
        CasinoPanelSupport.actionButton(
            "+10", "shell-game-up", wagerStyle, () -> active, () -> adjust(10));
    start =
        CasinoPanelSupport.actionButton(
            "START", "shell-game-start", flipStyle, () -> active, this::start);
    stakeLabel = CasinoPanelSupport.stakeLabel("shell-game-stake", textStyle);
    add(CasinoPanelSupport.wagerRow(down, stakeLabel, up, start)).growX();
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
