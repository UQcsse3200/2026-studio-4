package com.csse3200.game.components.traps;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/**
 * Shared one-shot collision handling for a walkable status-effect trap.
 *
 * <p>Concrete subclasses provide the effect to apply; this component only decides whether a
 * collision is a valid player trigger and guarantees that a trap fires once.
 */
public abstract class TrapEffectComponent extends Component {
  private HitboxComponent hitbox;
  private boolean triggered;

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    if (hitbox == null) {
      throw new IllegalStateException("TrapEffectComponent requires a HitboxComponent");
    }
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture mine, Fixture other) {
    if (triggered || hitbox.getFixture() != mine) {
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    Object userData = other.getBody().getUserData();
    if (!(userData instanceof BodyUserData)) {
      return;
    }
    Entity target = ((BodyUserData) userData).entity;
    if (target == null) {
      return;
    }

    CombatStatsComponent combatStats = target.getComponent(CombatStatsComponent.class);
    StatusEffectsControllerComponent effects =
        target.getComponent(StatusEffectsControllerComponent.class);
    if (combatStats == null || effects == null) {
      return;
    }

    applyEffect(effects, combatStats);
    triggered = true;
    entity.getEvents().trigger("trapTriggered");
  }

  /** Applies this trap's concrete effect to a valid target. */
  protected abstract void applyEffect(
      StatusEffectsControllerComponent effects, CombatStatsComponent combatStats);
}
