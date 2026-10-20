package com.csse3200.game.components.shop;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.function.Consumer;

/** Slot Machine screen for the merchant's casino, styled like the Dice Duel panel. */
public final class SlotMachinePanel extends Table {
  private static final String READY = "Ready to Spin";

  private final SlotMachineGame game;
  private final Consumer<SlotMachineGame.Outcome> onResolved;
  private final Label stakeLabel;
  private final Label reels;
  private final Label result;
  private final TextButton down;
  private final TextButton up;
  private final TextButton spin;
  private boolean active;

  public SlotMachinePanel(
      SlotMachineGame game,
      CoinFlipPanel.Appearance appearance,
      Consumer<SlotMachineGame.Outcome> onResolved) {
    this.game = game;
    this.onResolved = onResolved;
    Label.LabelStyle textStyle = appearance.text();
    Label.LabelStyle goldStyle = appearance.gold();
    setName("slot-machine-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();

    Label rules =
        new Label(
            "SLOT MACHINE  |  Spin three reels. Three matching symbols pay out your stake"
                + " times that symbol's multiplier. Any other result loses your stake.",
            textStyle);
    rules.setWrap(true);
    add(rules).growX().padBottom(12f);
    row();

    Table arena = new Table();
    arena.setBackground(appearance.arena());
    arena.pad(16f);
    reels = arenaLabel("slot-machine-reels", goldStyle);
    result = arenaLabel("slot-machine-result", goldStyle);
    arena.add(reels).growX().padBottom(16f);
    arena.row();
    arena.add(result).growX();
    add(arena).growX().minHeight(200f).padBottom(12f);
    row();

    add(new Label("WAGER STAKE", textStyle)).growX().left().padBottom(8f);
    row();
    down = button("-10", "slot-machine-down", appearance.wager(), () -> adjust(-10));
    up = button("+10", "slot-machine-up", appearance.wager(), () -> adjust(10));
    spin = button("SPIN", "slot-machine-spin", appearance.flip(), this::doSpin);
    stakeLabel = new Label("", textStyle);
    stakeLabel.setName("slot-machine-stake");
    stakeLabel.setAlignment(Align.center);
    Table wager = new Table();
    wager.add(down).width(58f).height(42f).padRight(8f);
    wager.add(stakeLabel).growX().minWidth(48f).padRight(8f);
    wager.add(up).width(58f).height(42f).padRight(8f);
    wager.add(spin).width(90f).height(42f);
    add(wager).growX().padBottom(10f);
    row();

    Label hint =
        new Label("Three of a kind wins. Diamonds pay the most, Cherries the least.", textStyle);
    hint.setWrap(true);
    add(hint).growX();
    showEmptyReels();
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
    game.adjustBet(delta);
    refresh();
  }

  private void doSpin() {
    SlotMachineGame.Outcome outcome = game.spin();
    if (outcome == null) {
      result.setText("This wager is unavailable. Check your gold.");
    } else {
      showReels(outcome);
      result.setText(
          outcome.won()
              ? "Jackpot! You won " + outcome.goldDelta() + " Gold!"
              : "No match. You lost " + Math.abs(outcome.goldDelta()) + " Gold.");
      onResolved.accept(outcome);
    }
    refresh();
  }

  private void showReels(SlotMachineGame.Outcome outcome) {
    reels.setText(
        outcome.reels().get(0)
            + "  |  "
            + outcome.reels().get(1)
            + "  |  "
            + outcome.reels().get(2));
  }

  private void showEmptyReels() {
    reels.setText("?  |  ?  |  ?");
    result.setText(READY);
  }

  /** Shows the panel ready for a new spin. */
  public void open() {
    active = true;
    showEmptyReels();
    refresh();
  }

  /** Hides the panel. Spins settle immediately, so there is never an open round to protect. */
  public void close() {
    active = false;
    showEmptyReels();
    refresh();
  }

  /** Updates the stake text and which buttons can be pressed. */
  public void refresh() {
    game.refresh();
    stakeLabel.setText(game.getBet() + " Gold");
    down.setDisabled(!active);
    up.setDisabled(!active);
    spin.setDisabled(!active || !game.canSpin());
  }
}
