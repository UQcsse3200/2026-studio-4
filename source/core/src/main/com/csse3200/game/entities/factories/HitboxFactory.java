package com.csse3200.game.entities.factories;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.weapons.FollowComponent;
import com.csse3200.game.components.weapons.LifetimeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Builds an unregistered weapon sensor. Callers register it with {@code EntityService}. */
public class HitboxFactory {
  public static Entity createHitbox(HitboxSpec spec) {
    Entity hitbox =
        new Entity()
            // kinematic: we move it with setPosition; it does not get pushed
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent().setLayer(spec.layer))
            // health unused; baseAttack is the damage dealt on overlap
            .addComponent(new CombatStatsComponent(0, spec.damage))
            .addComponent(new TouchAttackComponent(spec.targetLayer, spec.knockback))
            .addComponent(new LifetimeComponent(spec.lifetime));

    if (spec.owner != null) {
      hitbox.addComponent(new FollowComponent(spec.owner, spec.localOffset));
    }

    hitbox.setScale(spec.size);
    hitbox.setPosition(spec.position);
    return hitbox;
  }

  private HitboxFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
