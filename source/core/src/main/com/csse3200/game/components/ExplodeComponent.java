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
  private boolean playerOverlapping = false;
  private boolean fuseLit = false;
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

  /**
   * Re-checks concealment every frame while the player is physically overlapping. A bomb that was
   * touched by an invisible player did not notice it; once the invisibility wears off while they
   * are still in contact, the fuse lights without requiring the fixtures to separate and re-touch.
   */
  @Override
  public void update() {
    if (playerOverlapping && !fuseLit && !StatusEffectsControllerComponent.isConcealed(player)) {
      lightFuse();
    }
  }

  /**
   * Handles the start of a collision involving this entity.
   *
   * <p>Collisions involving a different fixture are ignored. The other fixture's body user data is
   * then checked to determine whether it belongs to the configured player entity. An invisible
   * player is undetectable, so brushing past one does not light the fuse.
   *
   * @param me the fixture belonging to this entity
   * @param other the fixture belonging to the other colliding entity
   */
  private void onCollisionStart(Fixture me, Fixture other) {
    if (!isPlayerFixture(me, other)) {
      return;
    }

    playerOverlapping = true;

    if (fuseLit || StatusEffectsControllerComponent.isConcealed(player)) {
      return;
    }

    lightFuse();
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (!isPlayerFixture(me, other)) {
      return;
    }

    playerOverlapping = false;
  }

  private boolean isPlayerFixture(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return false;
    }

    if (!(other.getBody().getUserData() instanceof BodyUserData)) {
      return false;
    }

    return ((BodyUserData) other.getBody().getUserData()).entity == player;
  }

  private void lightFuse() {
    fuseLit = true;

    entity.getEvents().trigger("fuseStarted");

    explosionTask =
        Timer.schedule(
            new Timer.Task() {
              @Override
              public void run() {
                if (playerOverlapping && !StatusEffectsControllerComponent.isConcealed(player)) {
                  damagePlayer();
                } else {
                  entity.getComponent(CombatStatsComponent.class).setHealth(0);
                }
              }
            },
            fuseTime);
  }

  private void damagePlayer() {
    if (!this.getEntity().getComponent(CombatStatsComponent.class).isDead()) {
      CombatStatsComponent playerStats = player.getComponent(CombatStatsComponent.class);

      if (playerStats != null) {
        playerStats.takeDamage(entity.getComponent(CombatStatsComponent.class).getBaseAttack());
      }

      entity.getComponent(CombatStatsComponent.class).setHealth(0);
    }
  }

  @Override
  public void dispose() {
    if (explosionTask != null) {
      explosionTask.cancel();
    }

    super.dispose();
  }
}
