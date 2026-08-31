package com.csse3200.game.components;

/**
 * Handles the disposal of an enemy entity when its death event is triggered.
 *
 * <p>This component listens for the entityDied event. When the event fires, the component either
 * triggers a death animation or immediately disposes of the entity, depending on the configured
 * value of hasDeathAnimation. Separating death handling from health tracking allows
 * CombatStatsComponent to remain responsible for combat statistics without directly disposing of
 * entities.
 */
public class EnemyDeathComponent extends Component {

  /** Indicates whether the entity should play a death animation before being disposed. */
  private final boolean hasDeathAnimation;

  /** Creates component that immediately disposes of the entity when it dies. */
  public EnemyDeathComponent() {
    this(false);
  }

  /**
   * Creates an enemyDeathComponent with the specified death-animation behavior.
   *
   * @param hasDeathAnimation true to trigger the event when the entity dies; false to dispose of
   *     the entity immediately
   */
  public EnemyDeathComponent(boolean hasDeathAnimation) {
    this.hasDeathAnimation = hasDeathAnimation;
  }

  /**
   * Registers a listener for the entity's event.
   *
   * <p>When the event is triggered, the registered listener invokes disposal
   */
  @Override
  public void create() {
    entity.getEvents().addListener("entityDied", this::disposal);
  }

  /**
   * Handles the entity's death.
   *
   * <p>If death animations are enabled, this method triggers the {@code dieAnimation} event.
   * Otherwise, it immediately disposes of the entity.
   */
  private void disposal() {
    if (hasDeathAnimation) {
      entity.getEvents().trigger("dieAnimation");
    } else {
      entity.dispose();
    }
  }
}
