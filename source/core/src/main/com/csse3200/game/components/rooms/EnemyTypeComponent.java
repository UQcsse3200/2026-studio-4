package com.csse3200.game.components.rooms;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;

/** Tags an enemy entity with the type it was spawned as, for achievement tracking. */
public class EnemyTypeComponent extends Component {
  private final EnemyType type;

  public EnemyTypeComponent(EnemyType type) {
    this.type = type;
  }

  public EnemyType getType() {
    return type;
  }
}
