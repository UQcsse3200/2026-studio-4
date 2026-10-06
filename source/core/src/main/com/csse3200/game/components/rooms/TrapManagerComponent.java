package com.csse3200.game.components.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.components.rooms.configs.TrapSpawnConfig;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.TrapFactory;

/** Spawns the fixed status-effect traps declared for a room. */
public class TrapManagerComponent extends EntityManagerComponent {
  private final TrapSpawnConfig[] spawnConfigs;

  public TrapManagerComponent(TrapSpawnConfig[] spawnConfigs) {
    this.spawnConfigs = spawnConfigs == null ? new TrapSpawnConfig[0] : spawnConfigs;
  }

  @Override
  public void create() {
    for (TrapSpawnConfig spawn : spawnConfigs) {
      Entity trap = createTrap(spawn);
      spawnEntityAt(trap, new GridPoint2(spawn.x, spawn.y), true, true);
    }
  }

  /** Converts the JSON-only discriminator into a concrete, typed trap entity. */
  private static Entity createTrap(TrapSpawnConfig spawn) {
    return switch (spawn.type) {
      case FREEZE -> TrapFactory.createFreezeTrap();
      case BURN -> TrapFactory.createBurnTrap();
    };
  }
}
