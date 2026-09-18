package com.csse3200.game.components.npc;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.WizardCurseEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/** Add the wizard bad effect when this bullet touch player. */
public class WizardProjectileEffectComponent extends Component {
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
      // Each bullet can curse only one time, else collision can add too much stacks.
      effects.addStatusEffect(new WizardCurseEffect());
      used = true;
    }
  }
}
