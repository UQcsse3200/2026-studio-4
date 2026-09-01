package com.csse3200.game.components;

import com.csse3200.game.services.ServiceLocator;

/**
 * Listens for the enemy's death event and disposes the entity when it fires. This keeps death
 * handling separate from health tracking so that CombatStatsComponent does not need to dispose
 * entities directly.
 */
public class EnemyDeathComponent extends Component {

  @Override
  public void create() {
    entity.getEvents().addListener("entityDied", this::disposal);
  }

  /**
   * Queues the entity for disposal rather than disposing it here, because death usually fires from
   * a collision event where the physics world cannot destroy the entity's body yet.
   */
  private void disposal() {
    ServiceLocator.getEntityService().scheduleDisposal(this.getEntity());
  }
}
