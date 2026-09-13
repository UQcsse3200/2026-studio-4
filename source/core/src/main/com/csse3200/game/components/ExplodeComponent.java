package com.csse3200.game.components;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Timer;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;

public class ExplodeComponent extends Component {

  private final Entity player;
  private HitboxComponent hitboxComponent;

  private Timer.Task explosionTask;
  private boolean playerTouchingBomb = false;
  float fuseTime;

  public ExplodeComponent(Entity player, float fuseTime) {
    this.player = player;
    this.fuseTime = fuseTime;
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

    Entity collidedEntity = ((BodyUserData) other.getBody().getUserData()).entity;

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
                } else {
                  entity.getComponent(CombatStatsComponent.class).setHealth(0);
                }
              }
            },
            fuseTime);
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return;
    }

    Entity collidedEntity = ((BodyUserData) other.getBody().getUserData()).entity;

    if (collidedEntity != player) {
      return;
    }

    playerTouchingBomb = false;
  }

  private void damagePlayer() {
    CombatStatsComponent playerStats = player.getComponent(CombatStatsComponent.class);

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
