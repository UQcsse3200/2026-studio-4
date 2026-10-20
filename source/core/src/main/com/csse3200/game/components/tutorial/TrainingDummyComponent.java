package com.csse3200.game.components.tutorial;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Makes an entity a practice target: it takes hits like an enemy, so every weapon, spell and status
 * effect works on it, but it is put back to full health after each one and never dies.
 *
 * <p>Each hit is announced with {@link #HIT} so the room can keep score and the {@link
 * DamagePopupDisplay} can show how hard it landed.
 *
 * <p>Needs a {@link CombatStatsComponent} on the same entity. A target has to be a dynamic body for
 * weapon hitboxes (which are kinematic) to register on it, so it is held in place every frame
 * instead of being made static.
 */
public class TrainingDummyComponent extends Component {
  /** Triggered on the target with {@code (String label, Integer damage, Integer totalHits)}. */
  public static final String HIT = "trainingTargetHit";

  private final String label;
  private CombatStatsComponent stats;
  private PhysicsComponent physics;
  private int hits;

  /**
   * @param label what the target is called in score messages, e.g. "Training Dummy"
   */
  public TrainingDummyComponent(String label) {
    this.label = label;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    physics = entity.getComponent(PhysicsComponent.class);
    entity.getEvents().<Entity, Integer, Integer>addListener("damageTaken", this::onDamageTaken);
  }

  @Override
  public void update() {
    // Knockback and being walked into would otherwise slide the target across the room.
    if (physics != null && physics.getBody() != null) {
      physics.getBody().setLinearVelocity(0f, 0f);
    }
  }

  /**
   * @return how many hits this target has taken since the room was entered
   */
  public int getHits() {
    return hits;
  }

  private void onDamageTaken(Entity attacker, Integer healthLost, Integer remainingHealth) {
    hits++;
    stats.setHealth(stats.getMaxHealth());
    entity.getEvents().trigger(HIT, label, healthLost, hits);
  }
}
