package com.csse3200.game.components;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Timer;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;

public class ExplodeComponent extends Component {

  private static final float FUSE_TIME = 2f;

  private final Entity player;
  private HitboxComponent hitboxComponent;

  private Timer.Task explosionTask;
  private boolean playerTouchingBomb = false;

  public ExplodeComponent(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);

    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity collidedEntity =
            ((BodyUserData) other.getBody().getUserData()).entity;

    if (collidedEntity != player || playerTouchingBomb) {
      return;
    }

    playerTouchingBomb = true;

    entity.getEvents().trigger("fuseStarted");

    explosionTask =
            Timer.schedule(
                    new Timer.Task() {
                      @Override
                      public void run() {
                        if (playerTouchingBomb) {
                          damagePlayer();
                        }
                      }
                    },
                    FUSE_TIME);
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity collidedEntity =
            ((BodyUserData) other.getBody().getUserData()).entity;

    if (collidedEntity != player) {
      return;
    }

    playerTouchingBomb = false;

    if (explosionTask != null) {
      explosionTask.cancel();
      explosionTask = null;
    }

    // Optional: stop the ticking animation.
    entity.getEvents().trigger("fuseCancelled");
  }

  private void damagePlayer() {
    CombatStatsComponent playerStats =
            player.getComponent(CombatStatsComponent.class);

    if (playerStats != null) {
      playerStats.takeDamage(entity.getComponent(CombatStatsComponent.class).getBaseAttack());
    }

    entity.getComponent(CombatStatsComponent.class).setHealth(0);
  }

  @Override
  public void dispose() {
    if (explosionTask != null) {
      explosionTask.cancel();
    }

    super.dispose();
  }
}
