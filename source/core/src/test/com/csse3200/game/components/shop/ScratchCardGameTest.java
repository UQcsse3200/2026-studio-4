package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ScratchCardGame.CardOutcome;
import com.csse3200.game.components.shop.ScratchCardGame.Result;
import com.csse3200.game.components.shop.ScratchCardGame.Symbol;
import java.util.Random;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

class ScratchCardGameTest {
  /** Symbol indices in order: 0 = coin, 1 = potion, 2 = charm, 3 = crown. */
  private static IntSupplier symbols(int... values) {
    int[] next = {0};
    return () -> values[next[0]++];
  }

  @Test
  void buyingTakesTheStakeAndHidesEverySpot() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(1, 1, 1));
    assertTrue(game.buy());
    assertEquals(40, inventory.getGold());
    assertTrue(game.isCardActive());
    for (int spot = 0; spot < ScratchCardGame.SPOTS; spot++) {
      assertFalse(game.isScratched(spot));
      assertNull(game.symbolAt(spot));
    }
  }

  @Test
  void threeMatchingSymbolsPayTheirMultiplier() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(1, 1, 1));
    game.buy();
    assertNull(game.scratch(0));
    assertEquals(Symbol.POTION, game.symbolAt(0));
    assertNull(game.scratch(1));
    CardOutcome outcome = game.scratch(2);
    assertEquals(Result.JACKPOT, outcome.result());
    assertEquals(Symbol.POTION, outcome.matched());
    assertEquals(50, outcome.payout()); // 5x the 10 Gold stake
    assertEquals(90, inventory.getGold());
    assertFalse(game.isCardActive());
  }

  @Test
  void twoMatchingSymbolsReturnHalfTheStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(0, 2, 0));
    game.buy();
    CardOutcome outcome = game.revealAll();
    assertEquals(Result.PAIR, outcome.result());
    assertEquals(Symbol.COIN, outcome.matched());
    assertEquals(5, outcome.payout());
    assertEquals(45, inventory.getGold());
  }

  @Test
  void noMatchPaysNothing() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(0, 1, 2));
    game.buy();
    CardOutcome outcome = game.revealAll();
    assertEquals(Result.NO_MATCH, outcome.result());
    assertNull(outcome.matched());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void scratchingTheSameSpotTwiceDoesNothing() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(3, 3, 3));
    game.buy();
    game.scratch(0);
    assertNull(game.scratch(0));
    assertNull(game.scratch(5)); // not a real spot
    assertTrue(game.isCardActive());
  }

  @Test
  void cannotBuyTwiceOrRevealWithoutACard() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(0, 0, 0));
    assertNull(game.revealAll());
    game.buy();
    assertFalse(game.buy());
    assertEquals(40, inventory.getGold()); // second buy took nothing
  }

  @Test
  void stakeIsClampedLockedAndGuardedAgainstOverflow() {
    InventoryComponent inventory = new InventoryComponent(25);
    ScratchCardGame game = new ScratchCardGame(inventory, symbols(0, 0, 0));
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.adjustStake(Integer.MIN_VALUE);
    assertEquals(10, game.getStake());
    game.buy();
    game.adjustStake(10);
    assertEquals(10, game.getStake());

    InventoryComponent rich = new InventoryComponent(Integer.MAX_VALUE);
    assertFalse(new ScratchCardGame(rich, symbols(0, 0, 0)).canBuy());
  }

  @Test
  void invalidSymbolPickIsRejected() {
    InventoryComponent inventory = new InventoryComponent(50);
    ScratchCardGame game = new ScratchCardGame(inventory, () -> 9);
    assertThrows(IllegalStateException.class, game::buy);
  }

  @Test
  void weightedPicksMakeRareSymbolsLessCommon() {
    IntSupplier picks = ScratchCardGame.weightedPicks(new Random(1));
    int[] counts = new int[Symbol.values().length];
    for (int i = 0; i < 10000; i++) {
      counts[picks.getAsInt()]++;
    }
    assertTrue(counts[Symbol.COIN.ordinal()] > counts[Symbol.POTION.ordinal()]);
    assertTrue(counts[Symbol.POTION.ordinal()] > counts[Symbol.CHARM.ordinal()]);
    assertTrue(counts[Symbol.CHARM.ordinal()] > counts[Symbol.CROWN.ordinal()]);
  }
}
