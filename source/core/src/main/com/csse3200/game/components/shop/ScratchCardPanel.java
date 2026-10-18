package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Scratch Card screen for the merchant's casino, styled like the Coin Flip panel. */
public final class ScratchCardPanel extends Table {
  private static final String HIDDEN = "?";
  private static final String READY = "Choose your stake and buy a card";

  private final ScratchCardGame game;
  private final Consumer<ScratchCardGame.CardOutcome> onResolved;
  private final Label stakeLabel;
  private final Label result;
  private final TextButton[] spots = new TextButton[ScratchCardGame.SPOTS];
  private final TextButton down;
  private final TextButton up;
  private final TextButton buy;
  private final TextButton scratchAll;
  private boolean active;

  public ScratchCardPanel(
      ScratchCardGame game,
      CoinFlipPanel.Appearance appearance,
      Consumer<ScratchCardGame.CardOutcome> onResolved) {
    this.game = game;
    this.onResolved = onResolved;
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    setName("scratch-card-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();

    Label rules =
        new Label(
            "SCRATCH CARD  |  Three matching symbols win: Coin 3x, Potion 5x, Charm 10x,"
                + " Crown 25x. Two matching return half your stake.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    Table spotRow = new Table();
    for (int i = 0; i < spots.length; i++) {
      int spot = i;
      spots[i] = button(HIDDEN, "scratch-spot-" + i, appearance.wager(), () -> doScratch(spot));
      spotRow.add(spots[i]).width(110f).height(70f).padRight(i < spots.length - 1 ? 10f : 0f);
    }
    arena.add(spotRow).padBottom(16f);
    arena.row();
    result = new Label("", goldStyle);
    result.setName("scratch-result");
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "scratch-down", appearance.wager(), () -> adjust(-10));
    up = button("+10", "scratch-up", appearance.wager(), () -> adjust(10));
    buy = button("BUY CARD", "scratch-buy", appearance.flip(), this::doBuy);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("scratch-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(buy).width(110f).height(42f);
    add(wager).growX().padBottom(10f);
    row();

    scratchAll = button("SCRATCH ALL", "scratch-all", appearance.wager(), this::doScratchAll);
    add(scratchAll).growX().height(42f).padBottom(12f);
    row();

    Label hint = new Label("Click each ? to scratch it, or Scratch All.", textStyle);
    hint.setWrap(true);
    add(hint).growX();
    showReady();
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

  private void doBuy() {
    if (game.buy()) {
      for (TextButton spot : spots) {
        spot.setText(HIDDEN);
      }
      result.setText("Scratch the three spots!");
    } else {
      result.setText("This wager is unavailable. Check your gold.");
    }
    refresh();
  }

  private void doScratch(int spot) {
    ScratchCardGame.Symbol shown = null;
    ScratchCardGame.CardOutcome outcome = game.scratch(spot);
    if (outcome == null) {
      shown = game.symbolAt(spot);
      if (shown != null) {
        spots[spot].setText(shown.label());
      }
    } else {
      showOutcome(outcome);
    }
    refresh();
  }

  private void doScratchAll() {
    ScratchCardGame.CardOutcome outcome = game.revealAll();
    if (outcome != null) {
      showOutcome(outcome);
    }
    refresh();
  }

  private void showOutcome(ScratchCardGame.CardOutcome outcome) {
    for (int i = 0; i < spots.length; i++) {
      spots[i].setText(outcome.symbols()[i].label());
    }
    result.setText(
        switch (outcome.result()) {
          case JACKPOT ->
              "Three " + outcome.matched().label() + "s! You won " + outcome.payout() + " Gold!";
          case PAIR -> "A pair! " + outcome.payout() + " Gold returned.";
          case NO_MATCH -> "No match. Better luck next card.";
        });
    onResolved.accept(outcome);
  }

  private void showReady() {
    for (TextButton spot : spots) {
      spot.setText(HIDDEN);
    }
    result.setText(READY);
  }

  /** Shows the panel ready for a new card. */
  public void open() {
    active = true;
    if (!game.isCardActive()) {
      showReady();
    }
    refresh();
  }

  /** Hides the panel. A card left unfinished is revealed and settled so nothing is lost. */
  public void close() {
    ScratchCardGame.CardOutcome outcome = game.revealAll();
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
    boolean cardActive = game.isCardActive();
    down.setDisabled(!active || cardActive);
    up.setDisabled(!active || cardActive);
    buy.setDisabled(!active || !game.canBuy());
    scratchAll.setDisabled(!active || !cardActive);
    for (int i = 0; i < spots.length; i++) {
      spots[i].setDisabled(!active || !cardActive || game.isScratched(i));
    }
  }
}
