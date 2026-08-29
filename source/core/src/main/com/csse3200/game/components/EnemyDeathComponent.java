package com.csse3200.game.components;

/**
 * Listens for the enemy's death event and disposes the entity when it fires. This keeps death
 * handling separate from health tracking so that CombatStatsComponent does not need to dispose
 * entities directly.
 */
public class EnemyDeathComponent extends Component {

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(
            "entityDied",
            () -> {
              entity.dispose();
            });
  }
}
