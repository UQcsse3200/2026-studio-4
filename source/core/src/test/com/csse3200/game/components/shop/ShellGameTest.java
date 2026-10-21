package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.player.InventoryComponent;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

class ShellGameTest {

  private static IntSupplier sequence(int... cups) {
    int[] index = {0};
    return () -> cups[Math.min(index[0]++, cups.length - 1)];
  }

  @Test
  void correctGuessDeductsStakeThenPaysDoubleOnWin() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShellGame game = new ShellGame(inventory, sequence(1));
    assertTrue(game.start());
    assertEquals(40, inventory.getGold());
    ShellGame.Round round = game.guess(1);
    assertTrue(round.won());
    assertEquals(1, round.winningCup());
    assertEquals(20, round.payout());
    assertEquals(60, inventory.getGold());
    assertFalse(game.isRoundActive());
  }

  @Test
  void wrongGuessForfeitsStakeAndRevealsWinningCup() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShellGame game = new ShellGame(inventory, sequence(2));
    game.start();
    ShellGame.Round round = game.guess(0);
    assertFalse(round.won());
    assertEquals(2, round.winningCup());
    assertEquals(0, round.payout());
    assertEquals(40, inventory.getGold());
    assertFalse(game.isRoundActive());
  }

  @Test
  void stakeOnlyAdjustableWhileIdleAndClampsToBalance() {
    InventoryComponent inventory = new InventoryComponent(25);
    ShellGame game = new ShellGame(inventory, sequence(0));
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.start();
    game.adjustStake(10);
    assertEquals(25, game.getStake());
  }

  @Test
  void cannotStartWithoutGoldAndGuessWhenIdleReturnsNull() {
    InventoryComponent inventory = new InventoryComponent(0);
    ShellGame game = new ShellGame(inventory, sequence(0));
    assertFalse(game.canStart());
    assertFalse(game.start());
    assertNull(game.guess(0));
  }

  @Test
  void guessRejectsCupOutsideValidRange() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShellGame game = new ShellGame(inventory, sequence(0));
    game.start();
    assertThrows(IllegalArgumentException.class, () -> game.guess(-1));
    assertThrows(IllegalArgumentException.class, () -> game.guess(ShellGame.CUP_COUNT));
  }
}
