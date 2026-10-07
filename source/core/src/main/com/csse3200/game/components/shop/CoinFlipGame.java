package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Aarash Mehta's coin-flip rules, adapted for the merchant's shared inventory and UI. */
public final class CoinFlipGame {
  private final InventoryComponent inventory;
  private final BooleanSupplier coinToss;
  private int stake = 10;

  public CoinFlipGame(InventoryComponent inventory, BooleanSupplier coinToss) {
    this.inventory = Objects.requireNonNull(inventory);
    this.coinToss = Objects.requireNonNull(coinToss);
    refresh();
  }

  public int getStake() {
    return stake;
  }

  public void adjustStake(int delta) {
    int gold = inventory.getGold();
    stake = (int) Math.min(Math.max((long) stake + delta, Math.min(10, gold)), gold);
  }

  public void refresh() {
    adjustStake(0);
  }

  public boolean canFlip() {
    int gold = inventory.getGold();
    return stake > 0 && gold >= stake && gold <= Integer.MAX_VALUE - stake;
  }

  /** Settles once immediately, like Aarash's original game; animation never changes the payout. */
  public Outcome flip() {
    if (!canFlip()) return null;
    boolean won = coinToss.getAsBoolean();
    inventory.addGold(won ? stake : -stake);
    return new Outcome(won, stake);
  }

  public record Outcome(boolean won, int stake) {}
}
