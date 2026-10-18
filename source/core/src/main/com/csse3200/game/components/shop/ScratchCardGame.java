package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;

/**
 * ............... Scratch Card: the player buys a card with three hidden symbols and scratches each
 * one. Three matching symbols pay out by symbol, two matching symbols return half the stake, and no
 * match pays nothing.
 */
public final class ScratchCardGame {
  /** Smallest normal stake, matching the other casino games. */
  public static final int MIN_STAKE = 10;

  /** Number of hidden spots on a card. */
  public static final int SPOTS = 3;

  /** Percentage of the stake returned when exactly two symbols match. */
  public static final int TWO_MATCH_REFUND_PERCENT = 50;

  private final InventoryComponent inventory;
  // Returns a symbol index for each spot. Passed in so tests can use fixed cards.
  private final IntSupplier symbolPicks;
  private int stake = MIN_STAKE;
  private Symbol[] card;
  private boolean[] scratched;
  private int cardStake;

  public ScratchCardGame(InventoryComponent inventory, IntSupplier symbolPicks) {
    this.inventory = Objects.requireNonNull(inventory);
    this.symbolPicks = Objects.requireNonNull(symbolPicks);
    refresh();
  }

  /** Picks symbols at random using each symbol's weight, so rare symbols appear less often. */
  public static IntSupplier weightedPicks(RandomGenerator random) {
    Objects.requireNonNull(random);
    int total = Arrays.stream(Symbol.values()).mapToInt(Symbol::weight).sum();
    return () -> {
      int roll = random.nextInt(total);
      for (Symbol symbol : Symbol.values()) {
        roll -= symbol.weight();
        if (roll < 0) {
          return symbol.ordinal();
        }
      }
      return Symbol.values().length - 1;
    };
  }

  public int getStake() {
    return stake;
  }

  /** Changes the stake within the player's Gold. Ignored while a card is in play. */
  public void adjustStake(int delta) {
    if (card != null) {
      return;
    }
    int gold = inventory.getGold();
    stake = Math.clamp((long) stake + delta, Math.min(MIN_STAKE, gold), gold);
  }

  /** Re-checks the stake after the player's Gold changes. */
  public void refresh() {
    adjustStake(0);
  }

  /** True when no card is in play, the stake is affordable and the best prize cannot overflow. */
  public boolean canBuy() {
    int gold = inventory.getGold();
    long bestPrize = (long) stake * Symbol.CROWN.multiplier();
    return card == null
        && stake > 0
        && gold >= stake
        && gold - stake <= Integer.MAX_VALUE - bestPrize;
  }

  /**
   * Takes the stake and deals a new card with every spot hidden.
   *
   * @return true if a card was bought
   */
  public boolean buy() {
    if (!canBuy()) {
      return false;
    }
    // Deal the card before paying: paying refreshes the shop, which would otherwise clamp the stake
    int paid = stake;
    cardStake = paid;
    card = new Symbol[SPOTS];
    for (int i = 0; i < SPOTS; i++) {
      card[i] = pickSymbol();
    }
    scratched = new boolean[SPOTS];
    inventory.addGold(-paid);
    return true;
  }

  public boolean isCardActive() {
    return card != null;
  }

  /** True when the spot exists on the current card and has been scratched. */
  public boolean isScratched(int spot) {
    return card != null && spot >= 0 && spot < SPOTS && scratched[spot];
  }

  /** The symbol under a scratched spot, or null if it is still hidden or no card is in play. */
  public Symbol symbolAt(int spot) {
    return isScratched(spot) ? card[spot] : null;
  }

  /**
   * Scratches one spot. When this reveals the last hidden spot, the card is settled.
   *
   * @return the result if this scratch finished the card, otherwise null
   */
  public CardOutcome scratch(int spot) {
    if (card == null || spot < 0 || spot >= SPOTS || scratched[spot]) {
      return null;
    }
    scratched[spot] = true;
    for (boolean done : scratched) {
      if (!done) {
        return null;
      }
    }
    return settle();
  }

  /**
   * Reveals every remaining spot and settles the card, for example when the casino is closed.
   *
   * @return the result, or null if no card is in play
   */
  public CardOutcome revealAll() {
    if (card == null) {
      return null;
    }
    Arrays.fill(scratched, true);
    return settle();
  }

  private CardOutcome settle() {
    Symbol[] finished = card.clone();
    card = null;
    scratched = null;
    Symbol matched = null;
    int matches = 1;
    for (Symbol symbol : Symbol.values()) {
      int count = 0;
      for (Symbol shown : finished) {
        if (shown == symbol) {
          count++;
        }
      }
      if (count > matches) {
        matches = count;
        matched = symbol;
      }
    }
    int payout;
    Result result;
    if (matches == SPOTS) {
      result = Result.JACKPOT;
      payout = cardStake * matched.multiplier();
    } else if (matches == 2) {
      result = Result.PAIR;
      payout = cardStake * TWO_MATCH_REFUND_PERCENT / 100;
    } else {
      result = Result.NO_MATCH;
      payout = 0;
    }
    inventory.addGold(payout);
    refresh();
    return new CardOutcome(result, matched, payout, cardStake, finished);
  }

  private Symbol pickSymbol() {
    int pick = symbolPicks.getAsInt();
    if (pick < 0 || pick >= Symbol.values().length) {
      throw new IllegalStateException("Symbol pick out of range: " + pick);
    }
    return Symbol.values()[pick];
  }

  /** Symbols on a card, from most to least common. */
  public enum Symbol {
    COIN("Gold Coin", 50, 3),
    POTION("Potion", 30, 5),
    CHARM("Charm", 15, 10),
    CROWN("Crown", 5, 25);

    private final String label;
    private final int weight;
    private final int multiplier;

    Symbol(String label, int weight, int multiplier) {
      this.label = label;
      this.weight = weight;
      this.multiplier = multiplier;
    }

    /** Name shown to the player. */
    public String label() {
      return label;
    }

    /** How often the symbol appears relative to the others. */
    public int weight() {
      return weight;
    }

    /** How many times the stake is returned for three of this symbol. */
    public int multiplier() {
      return multiplier;
    }
  }

  /** How a card ended. */
  public enum Result {
    JACKPOT,
    PAIR,
    NO_MATCH
  }

  /**
   * The settled card.
   *
   * @param result jackpot, pair or no match
   * @param matched the matching symbol, or null when nothing matched
   * @param payout total Gold returned to the player
   * @param stake the stake paid for the card
   * @param symbols the three symbols on the card
   */
  public record CardOutcome(
      Result result, Symbol matched, int payout, int stake, Symbol[] symbols) {}
}
