package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

class HigherLowerGameTest {

  /** Supplies cards[0] to the constructor's initial draw, then cards[1], cards[2], ... in order. */
  private static IntSupplier sequence(int... cards) {
    int[] index = {0};
    return () -> cards[Math.min(index[0]++, cards.length - 1)];
  }

  @Test
  void startDeductsStakeUpFrontAndDealsFirstCard() {
    InventoryComponent inventory = new InventoryComponent(50);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(5, 9));
    HigherLowerGame.Round round = game.start();
    assertEquals(HigherLowerGame.Outcome.ACTIVE, round.outcome());
    assertEquals(40, inventory.getGold());
    assertTrue(game.isRoundActive());
  }

  @Test
  void correctGuessGrowsStreakAndMultiplierUntilCashOut() {
    InventoryComponent inventory = new InventoryComponent(50);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(5, 6, 9, 11));
    game.start();
    HigherLowerGame.Round first = game.guess(true);
    assertEquals(HigherLowerGame.Outcome.CORRECT, first.outcome());
    assertEquals(1, first.streak());
    HigherLowerGame.Round second = game.guess(true);
    assertEquals(HigherLowerGame.Outcome.CORRECT, second.outcome());
    assertEquals(2, second.streak());
    int expectedPayout = (int) Math.floor(10 * Math.pow(1.5, 2));
    assertEquals(expectedPayout, game.potentialPayout());
    HigherLowerGame.Round cashed = game.cashOut();
    assertEquals(HigherLowerGame.Outcome.CASHED_OUT, cashed.outcome());
    assertEquals(expectedPayout, cashed.payout());
    assertFalse(game.isRoundActive());
    assertEquals(40 + expectedPayout, inventory.getGold());
  }

  @Test
  void wrongGuessForfeitsStakeAndResetsStreak() {
    InventoryComponent inventory = new InventoryComponent(50);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(1, 9, 2));
    game.start();
    HigherLowerGame.Round round = game.guess(true);
    assertEquals(HigherLowerGame.Outcome.BUST, round.outcome());
    assertEquals(0, round.streak());
    assertFalse(game.isRoundActive());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void tieIsAPushThatRedrawsWithoutChangingStreakOrStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(1, 7, 7, 10));
    game.start();
    HigherLowerGame.Round push = game.guess(true);
    assertEquals(HigherLowerGame.Outcome.PUSH, push.outcome());
    assertEquals(0, push.streak());
    assertTrue(game.isRoundActive());
    assertEquals(40, inventory.getGold());
    HigherLowerGame.Round next = game.guess(true);
    assertEquals(HigherLowerGame.Outcome.CORRECT, next.outcome());
    assertEquals(1, next.streak());
  }

  @Test
  void stakeOnlyAdjustableWhileIdleAndClampsToBalance() {
    InventoryComponent inventory = new InventoryComponent(25);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(5, 9));
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.start();
    game.adjustStake(10);
    assertEquals(25, game.getStake());
  }

  @Test
  void cannotStartWithoutGoldAndGuessOrCashOutWhenIdleReturnNull() {
    InventoryComponent inventory = new InventoryComponent(0);
    HigherLowerGame game = new HigherLowerGame(inventory, sequence(5, 9));
    assertFalse(game.canStart());
    assertNull(game.start());
    assertNull(game.guess(true));
    assertNull(game.cashOut());
  }
}
