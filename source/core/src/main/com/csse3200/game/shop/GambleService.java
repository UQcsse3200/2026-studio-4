package com.csse3200.game.shop;

import java.util.Objects;
import java.util.Random;

/**
 * Dev Dhingia's weighted "spend Gold -> win item or bust" rules, with an atomic inventory
 * settlement boundary. Randomness remains injectable and independent of rendering.
 */
public final class GambleService {

  private final GambleTable table;
  private final GambleWallet wallet;
  private final int costPerSpin;
  private final Random random;

  public GambleService(GambleTable table, GambleWallet wallet, int costPerSpin, Random random) {
    if (costPerSpin <= 0) throw new IllegalArgumentException("Draw cost must be positive");
    this.table = Objects.requireNonNull(table);
    this.wallet = Objects.requireNonNull(wallet);
    this.costPerSpin = costPerSpin;
    this.random = Objects.requireNonNull(random);
  }

  /** Spends costPerSpin gold and rolls the wheel. Nothing changes if funds are insufficient. */
  public GambleResult spin() {
    if (!wallet.hasGold(costPerSpin)) {
      return GambleResult.insufficientFunds();
    }

    GambleEntry entry = table.roll(random);
    GambleResult.Status status = wallet.settle(entry, costPerSpin);
    return new GambleResult(
        status,
        status == GambleResult.Status.WON || status == GambleResult.Status.BUST ? entry : null);
  }
}
