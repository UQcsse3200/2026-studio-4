package com.csse3200.game.components;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;

public class ExplodeComponent extends Component {
  private final Entity player;
  private HitboxComponent hitboxComponent;

  public ExplodeComponent(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    // Ignore collisions involving a different fixture
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    // Get the entity associated with the other fixture
    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity collidedEntity = ((BodyUserData) other.getBody().getUserData()).entity;

    if (collidedEntity == player) {
      explode();
    }
  }

  private void explode() {
    entity.getEvents().trigger("explode");
    /**
     * CombatStatsComponent combatStats = entity.getComponent(CombatStatsComponent.class);
     *
     * <p>if (combatStats != null) { combatStats.setHealth(0); }
     */
  }
}
