package com.csse3200.game.components.rooms.configs;

/** A trap type and its fixed position within a room. */
public class TrapSpawnConfig extends PositionConfig {
  public TrapType type = TrapType.FREEZE;

  /** Values accepted by the room JSON. Concrete trap behaviour is selected by the room spawner. */
  public enum TrapType {
    FREEZE,
    BURN
  }
}
