package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final float DASH_SPEED_MULTIPLIER = 5;
  private static final long DASH_DURATION_MS = 75;
  private static final long DASH_COOLDOWN_MS = 575;

  // Event / animation names
  private static final String WALK_UP = "walkUp";
  private static final String WALK_DOWN = "walkDown";
  private static final String WALK_LEFT = "walkLeft";
  private static final String WALK_RIGHT = "walkRight";
  private static final String IDLE_UP = "idleUp";
  private static final String IDLE_DOWN = "idleDown";
  private static final String IDLE_LEFT = "idleLeft";
  private static final String IDLE_RIGHT = "idleRight";

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 dashDirection = Vector2.Zero.cpy();
  private Vector2 facingDirection = new Vector2(0f, -1f);
  private boolean moving = false;

  private String animation;

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
    updateDashState();
    if (moving) {
      updateSpeed();
    }
    updateAnimation();
  }

  /** Ends the dash and clears the dash cooldown once their respective durations have elapsed. */
  private void updateDashState() {
    if (time.getTimeSince(dashInit) >= DASH_DURATION_MS) {
      dashOn = false;
      entity.getEvents().trigger("dashStop");
    }
    if (time.getTimeSince(dashInit) >= DASH_COOLDOWN_MS) {
      dashCooldown = false;
    }
  }

  /** Chooses between idle and walk animation logic based on whether the player is moving. */
  private void updateAnimation() {
    if (!moving) {
      updateIdleAnimation();
    } else {
      updateWalkAnimation();
    }
  }

  /** Switches to the matching idle animation once the player has stopped walking. */
  private void updateIdleAnimation() {
    if (Objects.equals(animation, WALK_DOWN)) {
      triggerAnimation(IDLE_DOWN);
    } else if (Objects.equals(animation, WALK_UP)) {
      triggerAnimation(IDLE_UP);
    } else if (Objects.equals(animation, WALK_LEFT)) {
      triggerAnimation(IDLE_LEFT);
    } else if (Objects.equals(animation, WALK_RIGHT)) {
      triggerAnimation(IDLE_RIGHT);
    }
  }

  /** Switches to the matching walk animation based on the player's current walk direction. */
  private void updateWalkAnimation() {
    if (walkDirection.y < 0 && !Objects.equals(animation, WALK_DOWN)) {
      triggerAnimation(WALK_DOWN);
    } else if (walkDirection.y > 0 && !Objects.equals(animation, WALK_UP)) {
      triggerAnimation(WALK_UP);
    } else if (walkDirection.x < 0
        && !Objects.equals(animation, WALK_LEFT)
        && walkDirection.y == 0) {
      triggerAnimation(WALK_LEFT);
    } else if (walkDirection.x > 0
        && !Objects.equals(animation, WALK_RIGHT)
        && walkDirection.y == 0) {
      triggerAnimation(WALK_RIGHT);
    }
  }

  /**
   * Fires the given animation event and records it as the current animation.
   *
   * @param animationName animation event/name to trigger
   */
  private void triggerAnimation(String animationName) {
    entity.getEvents().trigger(animationName);
    animation = animationName;
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
