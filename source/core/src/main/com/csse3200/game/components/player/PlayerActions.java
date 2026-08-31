package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final float DASH_SPEED_MULTIPLIER = 5;

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 dashDirection = Vector2.Zero.cpy();
  private Vector2 facingDirection = new Vector2(0f, -1f);
  private boolean moving = false;

  private long dashInit;
  private boolean dashOn = false;
  private boolean dashCooldown = false;
  private final GameTime time = new GameTime();

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("dash", this::dash);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("specialAttack", this::specialAttack);
  }

  @Override
  public void update() {
    if (time.getTimeSince(dashInit) >= 75) {
      dashOn = false;
      entity.getEvents().trigger("dashStop");
    }
    if (time.getTimeSince(dashInit) >= 575) {
      dashCooldown = false;
    }
    if (moving) {
      updateSpeed();
    }
    if (walkDirection.y < 0) {
      entity.getEvents().trigger("idleDown");
    } else if (walkDirection.y > 0) {
      entity.getEvents().trigger("idleUp");
    } else if (walkDirection.x < 0) {
      entity.getEvents().trigger("idleLeft");
    } else if (walkDirection.x > 0) {
      entity.getEvents().trigger("idleRight");
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 velocity = body.getLinearVelocity();
    Vector2 desiredVelocity;

    float movementSpeed = combatStats.getMovementSpeed();

    if (dashOn) {
      float dashSpeed = DASH_SPEED_MULTIPLIER * movementSpeed;
      desiredVelocity = dashDirection.cpy().scl(new Vector2(dashSpeed, dashSpeed));
    } else {
      desiredVelocity = walkDirection.cpy().scl(new Vector2(movementSpeed, movementSpeed));
    }
    // impulse = (desiredVel - currentVel) * mass
    Vector2 impulse = desiredVelocity.sub(velocity).scl(body.getMass());
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  /**
   * Moves the player towards a given direction.
   *
   * @param direction direction to move in
   */
  void walk(Vector2 direction) {
    if (!dashOn) {
      this.walkDirection = direction;
      moving = true;

      if (!direction.epsilonEquals(Vector2.Zero)) {
        this.facingDirection = this.walkDirection.cpy();
      }
    }
  }

  /** Stops the player from walking. */
  void stopWalking() {
    if (!dashOn) {
      this.walkDirection = Vector2.Zero.cpy();
      updateSpeed();
      moving = false;
    }
  }

  /** Makes the player attack. */
  void attack() {
    entity.getEvents().trigger("weaponAttack", facingDirection);
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /** Makes the player to do special attack. */
  void specialAttack() {

    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /**
   * Makes the player dash. The player only dashes if the dash is not currently on or on cooldown.
   */
  void dash(Vector2 direction) {
    if (!dashOn && !dashCooldown) {
      this.dashDirection = direction.cpy();
      moving = true;
      dashOn = true;
      dashInit = time.getTime();
      dashCooldown = true;
    }
  }
}
