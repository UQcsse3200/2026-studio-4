package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Higher/Lower card-guessing logic: guess whether the next card is higher or lower than the current
 * one. Each correct guess grows a multiplier; the player can cash out at any time or keep pushing
 * their luck until a wrong guess loses the stake.
 *
 * <p>Pure logic, no UI, following the same split as {@link CoinFlipGame}.
 */
public final class HigherLowerGame {
  public static final int MIN_RANK = 1;
  public static final int MAX_RANK = 13;
  private static final int MIN_STAKE = 10;
  private static final double MULTIPLIER_PER_CORRECT = 1.5;

  private final InventoryComponent inventory;
  private final IntSupplier cardDraw;
  private final Wager wager;
  private int currentCard;
  private int streak;
  private boolean roundActive;

  /**
   * @param inventory the player's Gold source and sink
   * @param cardDraw supplies a card rank between {@link #MIN_RANK} and {@link #MAX_RANK} inclusive;
   *     exposed so tests can inject deterministic draws instead of real randomness
   */
  public HigherLowerGame(InventoryComponent inventory, IntSupplier cardDraw) {
    this.inventory = Objects.requireNonNull(inventory);
    this.cardDraw = Objects.requireNonNull(cardDraw);
    this.wager = new Wager(inventory, MIN_STAKE);
    currentCard = drawCard();
    refresh();
  }

  private int drawCard() {
    int card = cardDraw.getAsInt();
    if (card < MIN_RANK || card > MAX_RANK) {
      throw new IllegalStateException("cardDraw must return a value between 1 and 13");
    }
    return card;
  }

  public int getStake() {
    return wager.stake();
  }

  public int getCurrentCard() {
    return currentCard;
  }

  public int getStreak() {
    return streak;
  }

  public boolean isRoundActive() {
    return roundActive;
  }

  /**
   * @return the current cash-out multiplier, growing 1.5x per correct guess in the streak
   */
  public double currentMultiplier() {
    return Math.pow(MULTIPLIER_PER_CORRECT, streak);
  }

  /**
   * @return how much Gold cashing out right now would pay, floored to a whole coin
   */
  public int potentialPayout() {
    return (int) Math.floor(wager.stake() * currentMultiplier());
  }

  /** Clamps the stake between {@code MIN_STAKE} and the player's current Gold. Only while idle. */
  public void adjustStake(int delta) {
    if (roundActive) {
      return;
    }
    wager.adjust(delta);
  }

  public void refresh() {
    if (!roundActive) {
      adjustStake(0);
    }
  }

  public boolean canStart() {
    int gold = inventory.getGold();
    return !roundActive && wager.stake() > 0 && gold >= wager.stake();
  }

  /** Deducts the stake up front and deals the first card of a new round. */
  public Round start() {
    if (!canStart()) {
      return null;
    }
    inventory.addGold(-wager.stake());
    roundActive = true;
    streak = 0;
    currentCard = drawCard();
    return new Round(Outcome.ACTIVE, currentCard, streak, 0);
  }

  /**
   * Draws the next card and resolves the guess. A tie is a push: card redraws, streak unchanged.
   */
  public Round guess(boolean higher) {
    if (!roundActive) {
      return null;
    }
    int nextCard = drawCard();
    if (nextCard == currentCard) {
      currentCard = nextCard;
      return new Round(Outcome.PUSH, currentCard, streak, 0);
    }
    boolean correct = higher ? nextCard > currentCard : nextCard < currentCard;
    currentCard = nextCard;
    if (correct) {
      streak++;
      return new Round(Outcome.CORRECT, currentCard, streak, 0);
    }
    roundActive = false;
    streak = 0;
    return new Round(Outcome.BUST, currentCard, 0, 0);
  }

  /** Banks the current multiplier's payout and ends the round. */
  public Round cashOut() {
    if (!roundActive) {
      return null;
    }
    int payout = potentialPayout();
    inventory.addGold(payout);
    roundActive = false;
    int finalStreak = streak;
    streak = 0;
    return new Round(Outcome.CASHED_OUT, currentCard, finalStreak, payout);
  }

  public enum Outcome {
    ACTIVE,
    CORRECT,
    PUSH,
    BUST,
    CASHED_OUT
  }

  public record Round(Outcome outcome, int card, int streak, int payout) {}
}
