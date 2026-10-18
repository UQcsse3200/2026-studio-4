package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.shop.RockPaperScissorsGame.Choice;
import java.util.function.Consumer;

/** Rock-Paper-Scissors screen for the merchant's casino, styled like the Coin Flip panel. */
public final class RockPaperScissorsPanel extends Table {
  private static final String READY = "Choose your stake and press Start";

  private final RockPaperScissorsGame game;
  private final Consumer<RockPaperScissorsGame.MatchOutcome> onResolved;
  private final Label stakeLabel;
  private final Label throwsLabel;
  private final Label scoreLabel;
  private final Label result;
  private final TextButton down;
  private final TextButton up;
  private final TextButton start;
  private final TextButton rock;
  private final TextButton paper;
  private final TextButton scissors;
  private boolean active;

  public RockPaperScissorsPanel(
      RockPaperScissorsGame game,
      CoinFlipPanel.Appearance appearance,
      Consumer<RockPaperScissorsGame.MatchOutcome> onResolved) {
    this.game = game;
    this.onResolved = onResolved;
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    setName("rps-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();

    Label rules =
        new Label(
            "ROCK PAPER SCISSORS  |  Best of three against the merchant. First to two round wins"
                + " takes your stake. A drawn match returns it.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    throwsLabel = arenaLabel("rps-throws", goldStyle);
    scoreLabel = arenaLabel("rps-score", goldStyle);
    result = arenaLabel("rps-result", goldStyle);
    arena.add(throwsLabel).growX().padBottom(8f);
    arena.row();
    arena.add(scoreLabel).growX().padBottom(16f);
    arena.row();
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "rps-down", appearance.wager(), () -> adjust(-10));
    up = button("+10", "rps-up", appearance.wager(), () -> adjust(10));
    start = button("START", "rps-start", appearance.flip(), this::doStart);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("rps-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(start).width(90f).height(42f);
    add(wager).growX().padBottom(10f);
    row();

    rock = button("ROCK", "rps-rock", appearance.wager(), () -> doPlay(Choice.ROCK));
    paper = button("PAPER", "rps-paper", appearance.wager(), () -> doPlay(Choice.PAPER));
    scissors =
        button("SCISSORS", "rps-scissors", appearance.wager(), () -> doPlay(Choice.SCISSORS));
    Table hands = new Table();
    hands.add(rock).growX().height(42f).padRight(8f);
    hands.add(paper).growX().height(42f).padRight(8f);
    hands.add(scissors).growX().height(42f);
    add(hands).growX().padBottom(12f);
    row();

    Label hint =
        new Label("Rock beats Scissors, Scissors beats Paper, Paper beats Rock.", textStyle);
    hint.setWrap(true);
    add(hint).growX();
    showReady();
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

  private void doStart() {
    if (game.start()) {
      throwsLabel.setText("Pick Rock, Paper or Scissors");
      showScore();
      result.setText("Round 1 of " + RockPaperScissorsGame.MAX_ROUNDS);
    } else {
      result.setText("This wager is unavailable. Check your gold.");
    }
    refresh();
  }

  private void doPlay(Choice choice) {
    RockPaperScissorsGame.Round round = game.play(choice);
    if (round == null) {
      return;
    }
    throwsLabel.setText("You: " + name(round.player()) + "   Merchant: " + name(round.merchant()));
    showScore();
    if (round.matchOutcome() != null) {
      showOutcome(round.matchOutcome());
    } else {
      String roundText =
          switch (round.result()) {
            case WIN -> "You win this round!";
            case TIE -> "Draw this round.";
            case LOSE -> "The merchant wins this round.";
          };
      result.setText(roundText + "  Round " + (game.getRoundsPlayed() + 1) + " next.");
    }
    refresh();
  }

  private void showOutcome(RockPaperScissorsGame.MatchOutcome outcome) {
    result.setText(
        switch (outcome.result()) {
          case WIN -> "You won the match! +" + outcome.stake() + " Gold";
          case TIE -> "Drawn match. Your " + outcome.stake() + " Gold is returned.";
          case LOSE -> "The merchant won the match. -" + outcome.stake() + " Gold";
        });
    onResolved.accept(outcome);
  }

  private void showScore() {
    scoreLabel.setText(
        "Score  You " + game.getPlayerWins() + " - " + game.getMerchantWins() + " Merchant");
  }

  private static String name(Choice choice) {
    return switch (choice) {
      case ROCK -> "Rock";
      case PAPER -> "Paper";
      case SCISSORS -> "Scissors";
    };
  }

  private void showReady() {
    throwsLabel.setText("You: ?   Merchant: ?");
    scoreLabel.setText("Score  You 0 - 0 Merchant");
    result.setText(READY);
  }

  /** Shows the panel ready for a new match. */
  public void open() {
    active = true;
    if (!game.isMatchActive()) {
      showReady();
    }
    refresh();
  }

  /** Hides the panel. A match left open is settled on its current score. */
  public void close() {
    RockPaperScissorsGame.MatchOutcome outcome = game.finishEarly();
    if (outcome != null) {
      onResolved.accept(outcome);
    }
    active = false;
    showReady();
    refresh();
  }

  /** Updates the stake text and which buttons can be pressed. */
  public void refresh() {
    game.refresh();
    stakeLabel.setText(game.getStake() + " Gold");
    boolean matchActive = game.isMatchActive();
    down.setDisabled(!active || matchActive);
    up.setDisabled(!active || matchActive);
    start.setDisabled(!active || !game.canStart());
    rock.setDisabled(!active || !matchActive);
    paper.setDisabled(!active || !matchActive);
    scissors.setDisabled(!active || !matchActive);
  }
}
