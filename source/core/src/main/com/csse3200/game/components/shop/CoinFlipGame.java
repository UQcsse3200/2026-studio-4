package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Aarash Mehta's coin-flip rules, adapted for the merchant's shared inventory and UI. */
public final class CoinFlipGame {
  private static final int MIN_STAKE = 10;

  private final InventoryComponent inventory;
  private final BooleanSupplier coinToss;
  private final Wager wager;

  public CoinFlipGame(InventoryComponent inventory, BooleanSupplier coinToss) {
    this.inventory = Objects.requireNonNull(inventory);
    this.coinToss = Objects.requireNonNull(coinToss);
    this.wager = new Wager(inventory, MIN_STAKE);
    refresh();
  }

  public int getStake() {
    return wager.stake();
  }

  public void adjustStake(int delta) {
    wager.adjust(delta);
  }

  public void refresh() {
    adjustStake(0);
  }

  public boolean canFlip() {
    int gold = inventory.getGold();
    int stake = wager.stake();
    return stake > 0 && gold >= stake && gold <= Integer.MAX_VALUE - stake;
  }

  /** Settles once immediately, like Aarash's original game; animation never changes the payout. */
  public Outcome flip() {
    if (!canFlip()) return null;
    int stake = wager.stake();
    boolean won = coinToss.getAsBoolean();
    inventory.addGold(won ? stake : -stake);
    return new Outcome(won, stake);
  }

  public record Outcome(boolean won, int stake) {}
}
