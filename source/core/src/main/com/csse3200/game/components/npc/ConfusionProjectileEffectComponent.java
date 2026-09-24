package com.csse3200.game.components.npc;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.ConfusionEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/** Applies confusion when its projectile hits a visible player. */
public class ConfusionProjectileEffectComponent extends Component {
  private HitboxComponent hitbox;
  private boolean used;

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (used || hitbox.getFixture() != me) {
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    Entity player = ((BodyUserData) other.getBody().getUserData()).entity;
    if (StatusEffectsControllerComponent.isConcealed(player)) {
      return;
    }

    StatusEffectsControllerComponent effects =
        player.getComponent(StatusEffectsControllerComponent.class);
    if (effects != null) {
      effects.addStatusEffect(new ConfusionEffect());
      used = true;
    }
  }
}
