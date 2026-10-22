package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Higher/Lower casino panel, styled to match {@link CoinFlipPanel} in the same Casino tab. */
public final class HigherLowerPanel extends Table {
  private static final String[] RANK_NAMES = {
    "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"
  };

  private final HigherLowerGame game;
  private final Consumer<HigherLowerGame.Round> onResolved;
  private final Label cardLabel;
  private final Label result;
  private final Label stakeLabel;
  private final Label multiplierLabel;
  private final TextButton down;
  private final TextButton up;
  private final TextButton start;
  private final TextButton higher;
  private final TextButton lower;
  private final TextButton cashOut;
  private boolean active;

  public HigherLowerPanel(
      HigherLowerGame game,
      CoinFlipPanel.Appearance appearance,
      Consumer<HigherLowerGame.Round> onResolved) {
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    TextButton.TextButtonStyle wagerStyle = appearance.wager();
    TextButton.TextButtonStyle flipStyle = appearance.flip();
    this.game = game;
    this.onResolved = onResolved;
    CasinoPanelSupport.initPanel(this, "higher-lower-panel", appearance.parchment());

    Label rules =
        new Label(
            "HIGHER / LOWER  |  Guess right to grow your multiplier. Cash out any time.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    cardLabel = new Label("", goldStyle);
    cardLabel.setName("higher-lower-card");
    cardLabel.setAlignment(Align.center);
    arena.add(cardLabel).padBottom(8f);
    arena.row();
    multiplierLabel = new Label("", textStyle);
    multiplierLabel.setName("higher-lower-multiplier");
    multiplierLabel.setAlignment(Align.center);
    arena.add(multiplierLabel).padBottom(8f);
    arena.row();
    result = new Label("Set your stake and start", goldStyle);
    result.setName("higher-lower-result");
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down =
        CasinoPanelSupport.actionButton(
            "-10", "higher-lower-down", wagerStyle, () -> active, () -> adjust(-10));
    up =
        CasinoPanelSupport.actionButton(
            "+10", "higher-lower-up", wagerStyle, () -> active, () -> adjust(10));
    start =
        CasinoPanelSupport.actionButton(
            "START", "higher-lower-start", flipStyle, () -> active, this::start);
    stakeLabel = CasinoPanelSupport.stakeLabel("higher-lower-stake", textStyle);
    add(CasinoPanelSupport.wagerRow(down, stakeLabel, up, start)).growX().padBottom(12f);
    row();

    higher =
        CasinoPanelSupport.actionButton(
            "HIGHER", "higher-lower-higher", flipStyle, () -> active, () -> guess(true));
    lower =
        CasinoPanelSupport.actionButton(
            "LOWER", "higher-lower-lower", flipStyle, () -> active, () -> guess(false));
    cashOut =
        CasinoPanelSupport.actionButton(
            "CASH OUT", "higher-lower-cashout", wagerStyle, () -> active, this::cashOut);
    Table playRow = new Table();
    playRow.add(lower).growX().height(42f).padRight(8f);
    playRow.add(higher).growX().height(42f).padRight(8f);
    playRow.add(cashOut).growX().height(42f);
    add(playRow).growX().padBottom(12f);
    row();

    Label hint =
        new Label(
            "Stake changes by 10 gold before you start. Each correct guess is worth 1.5x more.",
            textStyle);
    hint.setWrap(true);
    add(hint).growX();
  }

  private void adjust(int delta) {
    game.adjustStake(delta);
    refresh();
  }

  private void start() {
    HigherLowerGame.Round round = game.start();
    if (round == null) {
      result.setText("This wager is unavailable. Check your gold.");
      refresh();
      return;
    }
    result.setText("Higher or lower than this card?");
    onResolved.accept(round);
    pulseCard();
    refresh();
  }

  private void guess(boolean higherGuess) {
    HigherLowerGame.Round round = game.guess(higherGuess);
    if (round == null) {
      refresh();
      return;
    }
    switch (round.outcome()) {
      case CORRECT ->
          result.setText("Correct! Streak " + round.streak() + " - cash out or keep going.");
      case PUSH -> result.setText("Push - same rank again, try once more.");
      case BUST -> result.setText("Wrong guess. You lost your stake.");
      default -> {
        // ACTIVE and CASHED_OUT never come back from guess(); nothing to show for them here.
      }
    }
    onResolved.accept(round);
    pulseCard();
    refresh();
  }

  private void cashOut() {
    HigherLowerGame.Round round = game.cashOut();
    if (round == null) {
      refresh();
      return;
    }
    result.setText("Cashed out " + round.payout() + " Gold at streak " + round.streak() + "!");
    onResolved.accept(round);
    refresh();
  }

  private void pulseCard() {
    cardLabel.clearActions();
    cardLabel.setOrigin(Align.center);
    cardLabel.addAction(
        Actions.sequence(Actions.scaleTo(1.3f, 1.3f, 0.1f), Actions.scaleTo(1f, 1f, 0.15f)));
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
    cardLabel.setText("Current: " + RANK_NAMES[game.getCurrentCard() - 1]);
    multiplierLabel.setText(
        game.isRoundActive()
            ? String.format(
                "Cash out: %d Gold (x%.1f)", game.potentialPayout(), game.currentMultiplier())
            : "No active round");
    boolean idle = !game.isRoundActive();
    down.setDisabled(!active || !idle);
    up.setDisabled(!active || !idle);
    start.setDisabled(!active || !idle || !game.canStart());
    higher.setDisabled(!active || idle);
    lower.setDisabled(!active || idle);
    cashOut.setDisabled(!active || idle);
  }
}
