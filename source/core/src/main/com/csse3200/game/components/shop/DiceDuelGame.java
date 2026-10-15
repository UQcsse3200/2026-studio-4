package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Dice Duel: the player and the merchant each roll two dice; the higher total wins the stake. */
public final class DiceDuelGame {
  /** Smallest normal stake, matching Coin Flip. */
  public static final int MIN_STAKE = 10;

  /** Gold charged to reroll the player's dice once per round. */
  public static final int REROLL_FEE = 5;

  /** Extra winnings, as a percentage of the stake, for winning with a double. */
  public static final int DOUBLES_BONUS_PERCENT = 50;

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
    long maxWin = (long) stake + bonusFor(stake);
    return round == null && stake > 0 && gold >= stake && gold <= Integer.MAX_VALUE - maxWin;
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
    round = new Round(stake, rollDie(), rollDie(), rollDie(), rollDie(), false);
    return round;
  }

  /** True when a round is active, it has not been rerolled yet and the player can pay the fee. */
  public boolean canReroll() {
    return round != null && !round.rerolled() && inventory.getGold() >= REROLL_FEE;
  }

  /**
   * Pays the reroll fee and rerolls only the player's two dice. The merchant's dice are kept.
   *
   * @return the updated round, or null if a reroll is not allowed right now
   */
  public Round reroll() {
    if (!canReroll()) {
      return null;
    }
    inventory.addGold(-REROLL_FEE);
    round =
        new Round(round.stake(), rollDie(), rollDie(), round.merchant1(), round.merchant2(), true);
    return round;
  }

  /**
   * Ends the current round and pays out: a win returns double the stake (plus a bonus when the
   * player rolled a double), a tie returns the stake, and a loss returns nothing. Reroll fees are
   * never refunded.
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
    int bonus = 0;
    if (finished.playerTotal() > finished.merchantTotal()) {
      result = Result.WIN;
      if (finished.isPlayerDouble()) {
        bonus = bonusFor(finished.stake());
      }
      inventory.addGold(finished.stake() * 2 + bonus);
    } else if (finished.playerTotal() == finished.merchantTotal()) {
      result = Result.TIE;
      inventory.addGold(finished.stake());
    } else {
      result = Result.LOSE;
    }
    refresh();
    return new Outcome(
        result,
        finished.playerTotal(),
        finished.merchantTotal(),
        finished.stake(),
        finished.rerolled(),
        bonus);
  }

  private static int bonusFor(int stake) {
    return stake * DOUBLES_BONUS_PERCENT / 100;
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

  /** The dice rolled in one round, and whether the player has used their reroll. */
  public record Round(
      int stake, int player1, int player2, int merchant1, int merchant2, boolean rerolled) {
    public int playerTotal() {
      return player1 + player2;
    }

    public int merchantTotal() {
      return merchant1 + merchant2;
    }

    /** True when both of the player's dice show the same number. */
    public boolean isPlayerDouble() {
      return player1 == player2;
    }
  }

  /** The settled result of one round. {@code bonus} is the extra Gold won for a double. */
  public record Outcome(
      Result result, int playerTotal, int merchantTotal, int stake, boolean rerolled, int bonus) {}
}
