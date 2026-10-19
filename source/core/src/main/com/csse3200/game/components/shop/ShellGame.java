package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Shell Game logic: stake, watch three cups shuffle, then pick the one hiding the ball. Guess right
 * and double your stake; guess wrong and lose it.
 *
 * <p>Pure logic, no UI, following the same split as {@link CoinFlipGame} and {@link
 * HigherLowerGame}.
 */
public final class ShellGame {
  public static final int CUP_COUNT = 3;
  private static final int MIN_STAKE = 10;
  private static final int PAYOUT_MULTIPLIER = 2;

  private final InventoryComponent inventory;
  private final IntSupplier cupPicker;
  private int stake = MIN_STAKE;
  private int winningCup = -1;
  private boolean roundActive;

  /**
   * @param inventory the player's Gold source and sink
   * @param cupPicker supplies which cup (0 to {@link #CUP_COUNT} - 1) hides the ball; exposed so
   *     tests can inject a deterministic result instead of real randomness
   */
  public ShellGame(InventoryComponent inventory, IntSupplier cupPicker) {
    this.inventory = Objects.requireNonNull(inventory);
    this.cupPicker = Objects.requireNonNull(cupPicker);
    refresh();
  }

  private int pickCup() {
    int cup = cupPicker.getAsInt();
    if (cup < 0 || cup >= CUP_COUNT) {
      throw new IllegalStateException(
          "cupPicker must return a value between 0 and " + (CUP_COUNT - 1));
    }
    return cup;
  }

  public int getStake() {
    return stake;
  }

  public boolean isRoundActive() {
    return roundActive;
  }

  /** Clamps the stake between {@code MIN_STAKE} and the player's current Gold. Only while idle. */
  public void adjustStake(int delta) {
    if (roundActive) {
      return;
    }
    int gold = inventory.getGold();
    stake = Math.clamp((long) stake + delta, Math.min(MIN_STAKE, gold), gold);
  }

  public void refresh() {
    if (!roundActive) {
      adjustStake(0);
    }
  }

  public boolean canStart() {
    int gold = inventory.getGold();
    return !roundActive && stake > 0 && gold >= stake;
  }

  /** Deducts the stake up front and hides the ball under a cup, ready for a guess. */
  public boolean start() {
    if (!canStart()) {
      return false;
    }
    inventory.addGold(-stake);
    winningCup = pickCup();
    roundActive = true;
    return true;
  }

  /**
   * Resolves the player's guess against the hidden cup. Pays double the stake on a correct guess.
   */
  public Round guess(int cup) {
    if (!roundActive) {
      return null;
    }
    if (cup < 0 || cup >= CUP_COUNT) {
      throw new IllegalArgumentException("cup must be between 0 and " + (CUP_COUNT - 1));
    }
    boolean won = cup == winningCup;
    int revealedCup = winningCup;
    roundActive = false;
    winningCup = -1;
    if (won) {
      int payout = stake * PAYOUT_MULTIPLIER;
      inventory.addGold(payout);
      return new Round(true, revealedCup, payout);
    }
    return new Round(false, revealedCup, 0);
  }

  public record Round(boolean won, int winningCup, int payout) {}
}
