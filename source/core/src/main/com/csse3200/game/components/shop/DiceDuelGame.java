package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Dice Duel: the player and the merchant each roll two dice; the higher total wins the stake. */
public final class DiceDuelGame {
  /** Smallest normal stake, matching Coin Flip. */
  public static final int MIN_STAKE = 10;

  private final InventoryComponent inventory;
  // Returns one die roll (1 to 6). Passed in so tests can use fixed rolls instead of random ones.
  private final IntSupplier dice;
  private int stake = MIN_STAKE;
  // The round in progress between roll() and settle(), or null when no round is active.
  private Round round;

  public DiceDuelGame(InventoryComponent inventory, IntSupplier dice) {
    this.inventory = Objects.requireNonNull(inventory);
    this.dice = Objects.requireNonNull(dice);
    refresh();
  }

  public int getStake() {
    return stake;
  }

  /**
   * Changes the stake but keeps it between MIN_STAKE (or the player's whole balance if they have
   * less than that) and the player's Gold. Ignored while a round is in progress.
   */
  public void adjustStake(int delta) {
    if (round != null) {
      return;
    }
    int gold = inventory.getGold();
    stake = Math.clamp((long) stake + delta, Math.min(MIN_STAKE, gold), gold);
  }

  /** Re-checks the stake after the player's Gold changes. */
  public void refresh() {
    adjustStake(0);
  }

  /** True when no round is active, the player can afford the stake and a win cannot overflow. */
  public boolean canRoll() {
    int gold = inventory.getGold();
    return round == null && stake > 0 && gold >= stake && gold <= Integer.MAX_VALUE - stake;
  }

  public boolean isRoundActive() {
    return round != null;
  }

  /** The round in progress, or null. */
  public Round getRound() {
    return round;
  }

  /**
   * Takes the stake and rolls two dice each for the player and the merchant.
   *
   * @return the new round, or null if a roll is not allowed right now
   */
  public Round roll() {
    if (!canRoll()) {
      return null;
    }
    inventory.addGold(-stake);
    round = new Round(stake, rollDie(), rollDie(), rollDie(), rollDie());
    return round;
  }

  /**
   * Ends the current round and pays out: win returns double the stake, a tie returns the stake, and
   * a loss returns nothing.
   *
   * @return the outcome, or null if no round is active
   */
  public Outcome settle() {
    if (round == null) {
      return null;
    }
    Round finished = round;
    round = null;
    Result result;
    if (finished.playerTotal() > finished.merchantTotal()) {
      result = Result.WIN;
      inventory.addGold(finished.stake() * 2);
    } else if (finished.playerTotal() == finished.merchantTotal()) {
      result = Result.TIE;
      inventory.addGold(finished.stake());
    } else {
      result = Result.LOSE;
    }
    refresh();
    return new Outcome(result, finished.playerTotal(), finished.merchantTotal(), finished.stake());
  }

  private int rollDie() {
    int value = dice.getAsInt();
    if (value < 1 || value > 6) {
      throw new IllegalStateException("Die roll must be between 1 and 6, got " + value);
    }
    return value;
  }

  /** How a round ended. */
  public enum Result {
    WIN,
    TIE,
    LOSE
  }

  /** The dice rolled in one round. */
  public record Round(int stake, int player1, int player2, int merchant1, int merchant2) {
    public int playerTotal() {
      return player1 + player2;
    }

    public int merchantTotal() {
      return merchant1 + merchant2;
    }
  }

  /** The settled result of one round. */
  public record Outcome(Result result, int playerTotal, int merchantTotal, int stake) {}
}
