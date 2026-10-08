package com.csse3200.game.components.npc;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/** Ignites oil puddles and burns players who are currently oiled. */
public class FireHitEffectComponent extends Component {
  private HitboxComponent hitbox;

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitbox.getFixture() != me) {
      return;
    }

    Entity otherEntity = ((BodyUserData) other.getBody().getUserData()).entity;
    OilPuddleComponent puddle = otherEntity.getComponent(OilPuddleComponent.class);
    if (puddle != null) {
      puddle.ignite();
      return;
    }

    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)
        || !StatusEffectsControllerComponent.isOiled(otherEntity)) {
      return;
    }

    CombatStatsComponent stats = otherEntity.getComponent(CombatStatsComponent.class);
    StatusEffectsControllerComponent effects =
        otherEntity.getComponent(StatusEffectsControllerComponent.class);
    if (stats != null && effects != null) {
      effects.addStatusEffect(StatusEffectsFactory.createOilFireBurn(stats));
    }
  }
}
