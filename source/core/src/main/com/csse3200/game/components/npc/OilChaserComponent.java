package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.OilPuddleFactory;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Makes an oil chaser burst when its hitbox reaches the player. */
public class OilChaserComponent extends Component {
  public static final long OILED_DURATION = 6500;

  private final Consumer<Entity> puddleSpawner;
  private HitboxComponent hitbox;
  private boolean exploded;

  public OilChaserComponent(Consumer<Entity> puddleSpawner) {
    this.puddleSpawner = puddleSpawner;
  }

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (exploded || hitbox.getFixture() != me) {
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    Entity player = ((BodyUserData) other.getBody().getUserData()).entity;
    if (StatusEffectsControllerComponent.isConcealed(player)) {
      return;
    }

    exploded = true;
    CombatStatsComponent playerStats = player.getComponent(CombatStatsComponent.class);
    if (playerStats != null) {
      playerStats.takeDamage(1, entity);
    }
    StatusEffectsControllerComponent effects =
        player.getComponent(StatusEffectsControllerComponent.class);
    if (effects != null) {
      effects.applyOiled(OILED_DURATION);
    }

    Vector2 centre = entity.getCenterPosition().cpy();
    ServiceLocator.getEntityService()
        .schedule(() -> puddleSpawner.accept(OilPuddleFactory.createPuddle(centre, puddleSpawner)));
    entity.getComponent(CombatStatsComponent.class).setHealth(0);
  }
}
