package com.csse3200.game.components.statuseffects;

/**
 * The combat stats a status effect can scale. Each effect answers for each stat separately, so a
 * slow can halve movement speed without touching damage.
 */
public enum Stat {
  ATTACK,
  MOVEMENT_SPEED,
  ATTACK_SPEED
}
