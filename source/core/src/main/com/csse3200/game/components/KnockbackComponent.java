package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Component that applies aphysical knockback impulse to an entity when hit. */
public class KnockbackComponent extends Component {
  private final float strength;

  /**
   * @param strength The magnitude of the knockback force.
   */
  public KnockbackComponent(float strength) {
    this.strength = strength;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("hitReaction", this::onHitReaction);
  }

  /**
   * Calculates knockback direction vector and applies linear impulse via Box2D.
   *
   * @param attacker The entity causing the damage （can be null)
   */
  private void onHitReaction(Entity attacker) {
    if (attacker == null || entity == null) {
      return;
    }

    PhysicsComponent physicsComponent = entity.getComponent(PhysicsComponent.class);
    if (physicsComponent == null || physicsComponent.getBody() == null) {
      return;
    }

    Vector2 knockbackDir = entity.getCenterPosition().cpy().sub(attacker.getPosition()).nor();

    Vector2 impulse = knockbackDir.scl(strength);

    physicsComponent
        .getBody()
        .applyLinearImpulse(impulse, physicsComponent.getBody().getWorldCenter(), true);
  }
}
