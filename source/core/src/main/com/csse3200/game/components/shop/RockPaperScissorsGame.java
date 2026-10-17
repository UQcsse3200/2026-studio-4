package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Rock-Paper-Scissors against the merchant, best of three. The first to two round wins takes the
 * match; after three rounds the player with more round wins takes it. A drawn match returns the
 * stake.
 */
public final class RockPaperScissorsGame {
  /** Smallest normal stake, matching the other casino games. */
  public static final int MIN_STAKE = 10;

  /** Most rounds played in one match. */
  public static final int MAX_ROUNDS = 3;

  /** Round wins needed to take the match early. */
  public static final int WINS_NEEDED = 2;

  private final InventoryComponent inventory;
  // Returns the merchant's pick as 0 (rock), 1 (paper) or 2 (scissors). Passed in for testing.
  private final IntSupplier merchantPicks;
  private int stake = MIN_STAKE;
  private boolean matchActive;
  private int matchStake;
  private int playerWins;
  private int merchantWins;
  private int roundsPlayed;

  public RockPaperScissorsGame(InventoryComponent inventory, IntSupplier merchantPicks) {
    this.inventory = Objects.requireNonNull(inventory);
    this.merchantPicks = Objects.requireNonNull(merchantPicks);
    refresh();
  }

  public int getStake() {
    return stake;
  }

  /** Changes the stake within the player's Gold. Ignored while a match is in progress. */
  public void adjustStake(int delta) {
    if (matchActive) {
      return;
    }
    int gold = inventory.getGold();
    stake = Math.clamp((long) stake + delta, Math.min(MIN_STAKE, gold), gold);
  }

  /** Re-checks the stake after the player's Gold changes. */
  public void refresh() {
    adjustStake(0);
  }

  /** True when no match is running, the stake is affordable and a win cannot overflow. */
  public boolean canStart() {
    int gold = inventory.getGold();
    return !matchActive && stake > 0 && gold >= stake && gold <= Integer.MAX_VALUE - stake;
  }

  /**
   * Takes the stake and starts a new best-of-three match.
   *
   * @return true if the match started
   */
  public boolean start() {
    if (!canStart()) {
      return false;
    }
    inventory.addGold(-stake);
    matchActive = true;
    matchStake = stake;
    playerWins = 0;
    merchantWins = 0;
    roundsPlayed = 0;
    return true;
  }

  public boolean isMatchActive() {
    return matchActive;
  }

  public int getPlayerWins() {
    return playerWins;
  }

  public int getMerchantWins() {
    return merchantWins;
  }

  public int getRoundsPlayed() {
    return roundsPlayed;
  }

  /**
   * Plays one round with the player's choice. When this round ends the match, the match is settled
   * and the returned round includes the match outcome.
   *
   * @return the round played, or null if no match is running
   */
  public Round play(Choice playerChoice) {
    if (!matchActive || playerChoice == null) {
      return null;
    }
    Choice merchantChoice = merchantPick();
    Result result;
    if (playerChoice.beats(merchantChoice)) {
      result = Result.WIN;
      playerWins++;
    } else if (merchantChoice.beats(playerChoice)) {
      result = Result.LOSE;
      merchantWins++;
    } else {
      result = Result.TIE;
    }
    roundsPlayed++;
    MatchOutcome outcome = null;
    if (playerWins >= WINS_NEEDED || merchantWins >= WINS_NEEDED || roundsPlayed >= MAX_ROUNDS) {
      outcome = settle();
    }
    return new Round(playerChoice, merchantChoice, result, outcome);
  }

  /**
   * Ends the current match early on the current score, for example when the casino is closed.
   *
   * @return the match outcome, or null if no match is running
   */
  public MatchOutcome finishEarly() {
    return matchActive ? settle() : null;
  }

  private MatchOutcome settle() {
    matchActive = false;
    Result result;
    if (playerWins > merchantWins) {
      result = Result.WIN;
      inventory.addGold(matchStake * 2);
    } else if (playerWins == merchantWins) {
      result = Result.TIE;
      inventory.addGold(matchStake);
    } else {
      result = Result.LOSE;
    }
    refresh();
    return new MatchOutcome(result, playerWins, merchantWins, matchStake);
  }

  private Choice merchantPick() {
    int pick = merchantPicks.getAsInt();
    if (pick < 0 || pick >= Choice.values().length) {
      throw new IllegalStateException("Merchant pick must be 0, 1 or 2, got " + pick);
    }
    return Choice.values()[pick];
  }

  /** A hand the player or merchant can throw. */
  public enum Choice {
    ROCK,
    PAPER,
    SCISSORS;

    /** True when this hand beats the other one. */
    public boolean beats(Choice other) {
      return (this == ROCK && other == SCISSORS)
          || (this == PAPER && other == ROCK)
          || (this == SCISSORS && other == PAPER);
    }
  }

  /** The result of a round or a match, from the player's point of view. */
  public enum Result {
    WIN,
    TIE,
    LOSE
  }

  /** One round. {@code matchOutcome} is set only when this round ended the match. */
  public record Round(Choice player, Choice merchant, Result result, MatchOutcome matchOutcome) {}

  /** The settled result of a whole match. */
  public record MatchOutcome(Result result, int playerWins, int merchantWins, int stake) {}
}
