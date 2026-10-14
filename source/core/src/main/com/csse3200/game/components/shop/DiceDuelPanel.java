package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Dice Duel screen for the merchant's casino, styled like the Coin Flip panel. */
public final class DiceDuelPanel extends Table {
  private static final String READY = "Ready to Roll";

  private final DiceDuelGame game;
  private final Consumer<DiceDuelGame.Outcome> onResolved;
  private final Label stakeLabel;
  private final Label playerDice;
  private final Label merchantDice;
  private final Label result;
  private final TextButton down;
  private final TextButton up;
  private final TextButton roll;
  private final TextButton reroll;
  private final TextButton stand;
  private boolean active;

  public DiceDuelPanel(
      DiceDuelGame game,
      CoinFlipPanel.Appearance appearance,
      Consumer<DiceDuelGame.Outcome> onResolved) {
    this.game = game;
    this.onResolved = onResolved;
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    setName("dice-duel-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();

    Label rules =
        new Label(
            "DICE DUEL  |  You and the merchant each roll two dice. Higher total wins your stake."
                + " A tie returns it. You may reroll your dice once for "
                + DiceDuelGame.REROLL_FEE
                + " Gold.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    playerDice = arenaLabel("dice-duel-player", goldStyle);
    merchantDice = arenaLabel("dice-duel-merchant", goldStyle);
    result = arenaLabel("dice-duel-result", goldStyle);
    arena.add(playerDice).growX().padBottom(8f);
    arena.row();
    arena.add(merchantDice).growX().padBottom(16f);
    arena.row();
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "dice-duel-down", appearance.wager(), () -> adjust(-10));
    up = button("+10", "dice-duel-up", appearance.wager(), () -> adjust(10));
    roll = button("ROLL", "dice-duel-roll", appearance.flip(), this::doRoll);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("dice-duel-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(roll).width(90f).height(42f);
    add(wager).growX().padBottom(10f);
    row();

    reroll =
        button(
            "REROLL (-" + DiceDuelGame.REROLL_FEE + ")",
            "dice-duel-reroll",
            appearance.wager(),
            this::doReroll);
    stand = button("STAND", "dice-duel-stand", appearance.flip(), this::doStand);
    Table actions = new Table();
    actions.add(reroll).growX().height(42f).padRight(8f);
    actions.add(stand).growX().height(42f);
    add(actions).growX().padBottom(12f);
    row();

    Label hint =
        new Label(
            "Roll, then reroll once if you are behind, or stand to settle the round.", textStyle);
    hint.setWrap(true);
    add(hint).growX();
    showEmptyDice();
  }

  private static Label arenaLabel(String name, Label.LabelStyle style) {
    Label label = new Label("", style);
    label.setName(name);
    label.setWrap(true);
    label.setAlignment(Align.center);
    return label;
  }

  private TextButton button(
      String text, String name, TextButton.TextButtonStyle style, Runnable action) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && !button.isDisabled()) {
              action.run();
            }
          }
        });
    return button;
  }

  private void adjust(int delta) {
    game.adjustStake(delta);
    refresh();
  }

  private void doRoll() {
    DiceDuelGame.Round round = game.roll();
    if (round == null) {
      result.setText("This wager is unavailable. Check your gold.");
    } else {
      showRound(round);
      result.setText("Reroll for " + DiceDuelGame.REROLL_FEE + " Gold, or Stand.");
    }
    refresh();
  }

  private void doReroll() {
    DiceDuelGame.Round round = game.reroll();
    if (round == null) {
      result.setText("Reroll unavailable.");
    } else {
      showRound(round);
      result.setText("Rerolled! Stand to settle the round.");
    }
    refresh();
  }

  private void doStand() {
    DiceDuelGame.Outcome outcome = game.settle();
    if (outcome == null) {
      return;
    }
    result.setText(
        switch (outcome.result()) {
          case WIN -> "You won " + outcome.stake() + " Gold!";
          case TIE -> "Tie! Your " + outcome.stake() + " Gold is returned.";
          case LOSE -> "You lost " + outcome.stake() + " Gold.";
        });
    onResolved.accept(outcome);
    refresh();
  }

  private void showRound(DiceDuelGame.Round round) {
    playerDice.setText(
        "You:  " + round.player1() + " + " + round.player2() + " = " + round.playerTotal());
    merchantDice.setText(
        "Merchant:  "
            + round.merchant1()
            + " + "
            + round.merchant2()
            + " = "
            + round.merchantTotal());
  }

  private void showEmptyDice() {
    playerDice.setText("You:  ? + ?");
    merchantDice.setText("Merchant:  ? + ?");
    result.setText(READY);
  }

  /** Shows the panel ready for a new round. */
  public void open() {
    active = true;
    if (!game.isRoundActive()) {
      showEmptyDice();
    }
    refresh();
  }

  /** Hides the panel. A round left open is settled so the player's stake is never lost silently. */
  public void close() {
    if (game.isRoundActive()) {
      DiceDuelGame.Outcome outcome = game.settle();
      if (outcome != null) {
        onResolved.accept(outcome);
      }
    }
    active = false;
    showEmptyDice();
    refresh();
  }

  /** Updates the stake text and which buttons can be pressed. */
  public void refresh() {
    game.refresh();
    stakeLabel.setText(game.getStake() + " Gold");
    boolean roundActive = game.isRoundActive();
    down.setDisabled(!active || roundActive);
    up.setDisabled(!active || roundActive);
    roll.setDisabled(!active || !game.canRoll());
    reroll.setDisabled(!active || !game.canReroll());
    stand.setDisabled(!active || !roundActive);
  }
}
