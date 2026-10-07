package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GambleServiceTest {

  static class FakeWallet implements GambleWallet {
    int gold;
    final List<String> grants = new ArrayList<>();
    boolean spendGoldCalled = false;

    FakeWallet(int startingGold) {
      this.gold = startingGold;
    }

    @Override
    public boolean hasGold(int amount) {
      return gold >= amount;
    }

    @Override
    public void spendGold(int amount) {
      spendGoldCalled = true;
      gold -= amount;
    }

    @Override
    public void grantItem(String itemId, int quantity) {
      for (int i = 0; i < quantity; i++) {
        grants.add(itemId);
      }
    }
  }

  /** Always returns a fixed value from nextInt(bound), so a roll is deterministic. */
  static class FixedRandom extends Random {
    private final int fixedValue;

    FixedRandom(int fixedValue) {
      this.fixedValue = fixedValue;
    }

    @Override
    public int nextInt(int bound) {
      return fixedValue;
    }
  }

  @Test
  void spinWinsAndGrantsItemWhenRollLandsOnWinningEntry() {
    FakeWallet wallet = new FakeWallet(50);
    GambleTable table =
        new GambleTable(List.of(GambleEntry.of("health_potion", 1, 1), GambleEntry.bust(99)));
    GambleService gamble = new GambleService(table, wallet, 10, new FixedRandom(0));

    GambleResult result = gamble.spin();

    assertEquals(GambleResult.Status.WON, result.status());
    assertEquals(40, wallet.gold);
    assertEquals(List.of("health_potion"), wallet.grants);
  }

  @Test
  void spinBustsAndGrantsNothingWhenRollLandsOnBustEntry() {
    FakeWallet wallet = new FakeWallet(50);
    GambleTable table =
        new GambleTable(List.of(GambleEntry.of("health_potion", 1, 1), GambleEntry.bust(99)));
    GambleService gamble = new GambleService(table, wallet, 10, new FixedRandom(50));

    GambleResult result = gamble.spin();

    assertEquals(GambleResult.Status.BUST, result.status());
    assertEquals(40, wallet.gold, "gold is still spent on a bust");
    assertTrue(wallet.grants.isEmpty());
  }

  @Test
  void rejectedWhenNotEnoughGold() {
    FakeWallet wallet = new FakeWallet(5);
    GambleTable table = new GambleTable(List.of(GambleEntry.of("health_potion", 1, 1)));
    GambleService gamble = new GambleService(table, wallet, 10, new FixedRandom(0));

    GambleResult result = gamble.spin();

    assertEquals(GambleResult.Status.INSUFFICIENT_FUNDS, result.status());
    assertFalse(wallet.spendGoldCalled, "gold must not be touched when funds are insufficient");
    assertEquals(5, wallet.gold);
  }

  @Test
  void rollsLandWithinExpectedDistributionOverManySpins() {
    GambleEntry winning = GambleEntry.of("health_potion", 1, 1);
    GambleEntry bust = GambleEntry.bust(4); // winning should land ~20% of the time
    GambleTable table = new GambleTable(List.of(winning, bust));

    int wins = 0;
    int trials = 100_000;
    Random realRandom = new Random(42); // fixed seed keeps this test deterministic
    for (int i = 0; i < trials; i++) {
      FakeWallet wallet = new FakeWallet(10);
      GambleService gamble = new GambleService(table, wallet, 10, realRandom);
      if (gamble.spin().status() == GambleResult.Status.WON) {
        wins++;
      }
    }

    double observedRate = wins / (double) trials;
    assertTrue(
        Math.abs(observedRate - 0.2) < 0.01,
        "expected ~20% win rate, observed " + (observedRate * 100) + "%");
  }
}
