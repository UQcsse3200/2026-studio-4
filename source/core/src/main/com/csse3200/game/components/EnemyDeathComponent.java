package com.csse3200.game.components;

/**
 * Listens for the enemy's death event and disposes the entity when it fires. This keeps death
 * handling separate from health tracking so that CombatStatsComponent does not need to dispose
 * entities directly.
 */
public class EnemyDeathComponent extends Component {
  private final boolean hasDeathAnimation;

  public EnemyDeathComponent() {
    this(false);
  }

  public EnemyDeathComponent(boolean hasDeathAnimation) {
    this.hasDeathAnimation = hasDeathAnimation;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("entityDied", this::disposal);
  }

  private void disposal() {
    if (hasDeathAnimation) {
      entity.getEvents().trigger("dieAnimation");
    } else {
      entity.dispose();
    }
  }
}
