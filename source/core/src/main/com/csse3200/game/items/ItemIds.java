package com.csse3200.game.items;

/** Stable IDs used by JSON drops, inventory counts and UI events. */
public final class ItemIds {
  public static final String STRENGTH_CHARM = "STRENGTH_CHARM";
  public static final String SPEED_CHARM = "SPEED_CHARM";
  public static final String ATTACK_SPEED_CHARM = "ATTACK_SPEED_CHARM";
  public static final String HEALTH_POTION = "HEALTH_POTION";
  public static final String MEDIUM_HEALTH_POTION = "MEDIUM_HEALTH_POTION";
  public static final String LARGE_HEALTH_POTION = "LARGE_HEALTH_POTION";
  public static final String SHIELD = "SHIELD";
  public static final String SPEED_POTION = "SPEED_POTION";
  public static final String STRENGTH_POTION = "STRENGTH_POTION";
  public static final String FREEZE_BOMB = "FREEZE_BOMB";
  public static final String GOLD_COIN = "GOLD_COIN";

  private ItemIds() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
