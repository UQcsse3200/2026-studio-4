package com.csse3200.game.shop;

import java.util.Random;

/**
 * The core "spend Gold -> roll -> win item or bust" gambling transaction. Mirrors ShopService's
 * shape (funds-check -> deduct -> grant) so both can share a wallet adapter around the real
 * inventory.
 */
public final class GambleService {

  private final GambleTable table;
  private final GambleWallet wallet;
  private final int costPerSpin;
  private final Random random;

  public GambleService(GambleTable table, GambleWallet wallet, int costPerSpin, Random random) {
    this.table = table;
    this.wallet = wallet;
    this.costPerSpin = costPerSpin;
    this.random = random;
  }

  /** Spends costPerSpin gold and rolls the wheel. Nothing changes if funds are insufficient. */
  public GambleResult spin() {
    if (!wallet.hasGold(costPerSpin)) {
      return GambleResult.insufficientFunds();
    }

    wallet.spendGold(costPerSpin);
    GambleEntry entry = table.roll(random);

    if (entry.isBust()) {
      return GambleResult.bust(entry);
    }

    wallet.grantItem(entry.itemId(), entry.quantity());
    return GambleResult.won(entry);
  }
}
