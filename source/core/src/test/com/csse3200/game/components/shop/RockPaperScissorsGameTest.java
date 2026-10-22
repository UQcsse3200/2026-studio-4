package com.csse3200.game.components.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.RockPaperScissorsGame.Choice;
import com.csse3200.game.components.shop.RockPaperScissorsGame.Result;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

class RockPaperScissorsGameTest {
  /** Merchant picks in order: 0 = rock, 1 = paper, 2 = scissors. */
  private static IntSupplier picks(int... values) {
    int[] next = {0};
    return () -> values[next[0]++];
  }

  @Test
  void handsBeatTheRightHands() {
    assertTrue(Choice.ROCK.beats(Choice.SCISSORS));
    assertTrue(Choice.PAPER.beats(Choice.ROCK));
    assertTrue(Choice.SCISSORS.beats(Choice.PAPER));
    assertFalse(Choice.ROCK.beats(Choice.PAPER));
    assertFalse(Choice.ROCK.beats(Choice.ROCK));
  }

  @Test
  void winningTwoRoundsWinsTheMatchEarly() {
    InventoryComponent inventory = new InventoryComponent(50);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(2, 2));
    assertTrue(game.start());
    assertEquals(40, inventory.getGold()); // stake taken
    assertNull(game.play(Choice.ROCK).matchOutcome()); // 1-0, match continues
    RockPaperScissorsGame.Round last = game.play(Choice.ROCK);
    assertEquals(Result.WIN, last.matchOutcome().result());
    assertEquals(2, game.getRoundsPlayed());
    assertFalse(game.isMatchActive());
    assertEquals(60, inventory.getGold()); // +10 overall
  }

  @Test
  void losingTwoRoundsLosesTheStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(1, 1));
    game.start();
    game.play(Choice.ROCK);
    assertEquals(Result.LOSE, game.play(Choice.ROCK).matchOutcome().result());
    assertEquals(40, inventory.getGold());
  }

  @Test
  void drawnMatchRefundsTheStake() {
    InventoryComponent inventory = new InventoryComponent(50);
    // win, lose, draw -> 1-1 after three rounds
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(2, 1, 0));
    game.start();
    game.play(Choice.ROCK);
    game.play(Choice.ROCK);
    RockPaperScissorsGame.MatchOutcome outcome = game.play(Choice.ROCK).matchOutcome();
    assertEquals(Result.TIE, outcome.result());
    assertEquals(3, game.getRoundsPlayed());
    assertEquals(50, inventory.getGold());
  }

  @Test
  void cannotPlayWithoutAMatchOrStartTwice() {
    InventoryComponent inventory = new InventoryComponent(50);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(0, 0, 0));
    assertNull(game.play(Choice.ROCK));
    game.start();
    assertFalse(game.start());
    assertEquals(40, inventory.getGold()); // second start took nothing
  }

  @Test
  void stakeIsClampedAndLockedDuringAMatch() {
    InventoryComponent inventory = new InventoryComponent(25);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(0, 0, 0));
    game.adjustStake(Integer.MAX_VALUE);
    assertEquals(25, game.getStake());
    game.adjustStake(Integer.MIN_VALUE);
    assertEquals(10, game.getStake());
    game.start();
    game.adjustStake(10);
    assertEquals(10, game.getStake());
  }

  @Test
  void finishingEarlySettlesOnCurrentScore() {
    InventoryComponent inventory = new InventoryComponent(50);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, picks(2));
    game.start();
    game.play(Choice.ROCK); // 1-0
    assertEquals(Result.WIN, game.finishEarly().result());
    assertEquals(60, inventory.getGold());
    assertNull(game.finishEarly());
  }

  @Test
  void invalidMerchantPickIsRejected() {
    InventoryComponent inventory = new InventoryComponent(50);
    RockPaperScissorsGame game = new RockPaperScissorsGame(inventory, () -> 3);
    game.start();
    assertThrows(IllegalStateException.class, () -> game.play(Choice.ROCK));
  }
}
