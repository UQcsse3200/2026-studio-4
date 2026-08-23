package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;

/**
 * Listens for the enemy's death event and disposes the entity when it fires. This keeps death
 * handling separate from health tracking so that CombatStatsComponent does not need to dispose
 * entities directly.
 */
public class DeathComponent extends Component {

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(
            "enemyDied",
            (Vector2 position, Entity deadEntity) -> {
              entity.dispose();
            });
  }
}
