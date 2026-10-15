package com.csse3200.game.components.shop;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Three-reel slot machine: spin for matching symbols, win a gold multiplier. */
public final class SlotMachineGame {
  /** Symbols in increasing order of rarity and payout. */
  public enum Symbol {
    CHERRY(2),
    BELL(5),
    STAR(10),
    DIAMOND(20);

    private final int tripleMultiplier;

    Symbol(int tripleMultiplier) {
      this.tripleMultiplier = tripleMultiplier;
    }

    public int tripleMultiplier() {
      return tripleMultiplier;
    }
  }

  private static final Symbol[] SYMBOLS = Symbol.values();
  public static final int DEFAULT_BET = 10;

  private final InventoryComponent inventory;
  private final IntSupplier reelRoll;
  private int bet = DEFAULT_BET;

  public SlotMachineGame(InventoryComponent inventory, IntSupplier reelRoll) {
    this.inventory = Objects.requireNonNull(inventory);
    this.reelRoll = Objects.requireNonNull(reelRoll);
    refresh();
  }

  public int getBet() {
    return bet;
  }

  public void adjustBet(int delta) {
    int gold = inventory.getGold();
    bet = Math.clamp((long) bet + delta, Math.min(DEFAULT_BET, gold), gold);
  }

  public void refresh() {
    adjustBet(0);
  }

  public boolean canSpin() {
    int gold = inventory.getGold();
    return bet > 0 && gold >= bet && gold <= Integer.MAX_VALUE - bet;
  }

  /** Settles once immediately, like Coin Flip; the reel animation never changes the payout. */
  public Outcome spin() {
    if (!canSpin()) return null;
    List<Symbol> reels = List.of(rollSymbol(), rollSymbol(), rollSymbol());
    int payout = payoutFor(reels);
    int delta = payout > 0 ? bet * (payout - 1) : -bet;
    inventory.addGold(delta);
    return new Outcome(reels, payout, delta);
  }

  private Symbol rollSymbol() {
    int index = Math.floorMod(reelRoll.getAsInt(), SYMBOLS.length);
    return SYMBOLS[index];
  }

  /** Returns the payout multiplier for three matching reels, or 0 for no win. */
  private int payoutFor(List<Symbol> reels) {
    if (reels.get(0) == reels.get(1) && reels.get(1) == reels.get(2)) {
      return reels.get(0).tripleMultiplier();
    }
    return 0;
  }

  public record Outcome(List<Symbol> reels, int payoutMultiplier, int goldDelta) {
    public boolean won() {
      return payoutMultiplier > 0;
    }
  }
}
