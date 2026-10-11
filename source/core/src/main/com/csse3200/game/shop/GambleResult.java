package com.csse3200.game.shop;

/** Outcome of a single GambleService.spin() call. */
public record GambleResult(Status status, GambleEntry entry) {

  public enum Status {
    WON,
    BUST,
    INSUFFICIENT_FUNDS,
    INVALID_REWARD,
    QUANTITY_LIMIT
  }

  public static GambleResult won(GambleEntry entry) {
    return new GambleResult(Status.WON, entry);
  }

  public static GambleResult bust(GambleEntry entry) {
    return new GambleResult(Status.BUST, entry);
  }

  public static GambleResult insufficientFunds() {
    return new GambleResult(Status.INSUFFICIENT_FUNDS, null);
  }
}
