package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final float DASH_SPEED_MULTIPLIER = 5;
  private static final long DASH_DURATION_MS = 75;
  private static final long DASH_COOLDOWN_MS = 575;
  private static final float JUMP_DURATION = 0.85f;
  private static final float JUMP_HEIGHT = 0.9f;
  private static final float JUMP_COOLDOWN = 0.35f;

  // Event / animation names
  private static final String DASH_STOP = "dashStop";
  private static final String WALK_UP = "walkUp";
  private static final String WALK_DOWN = "walkDown";
  private static final String WALK_LEFT = "walkLeft";
  private static final String WALK_RIGHT = "walkRight";
  private static final String IDLE_UP = "idleUp";
  private static final String IDLE_DOWN = "idleDown";
  private static final String IDLE_LEFT = "idleLeft";
  private static final String IDLE_RIGHT = "idleRight";

  private final Set<Object> controlLocks = new HashSet<>();
  private final Set<Object> jumpOwners = new HashSet<>();
  private boolean jumping;
  private float jumpElapsed;
  private float jumpCooldownRemaining;
  private AnimationRenderComponent animator;

  /**
   * Locks movement, dash and attacks without changing speed buffs. Each owner releases its own
   * lock.
   */
  public void setControlsLocked(Object owner, boolean locked) {
    if (locked) {
      controlLocks.add(owner);
      cancelJump();
      if (dashOn) entity.getEvents().trigger(DASH_STOP);
      dashOn = false;
      if (physicsComponent != null) physicsComponent.getBody().setLinearVelocity(0f, 0f);
    } else {
      controlLocks.remove(owner);
    }
  }

  public boolean areControlsLocked() {
    return !controlLocks.isEmpty();
  }

  /** Replaces the dash action with jumping while at least one owner requests it. */
  public void setJumpEnabled(Object owner, boolean enabled) {
    boolean wasEnabled = isJumpEnabled();
    if (enabled) {
      jumpOwners.add(owner);
    } else {
      jumpOwners.remove(owner);
    }
    if (!wasEnabled && isJumpEnabled()) {
      if (dashOn && entity != null) entity.getEvents().trigger(DASH_STOP);
      dashOn = false;
      dashCooldown = false;
      moving = !walkDirection.isZero();
      if (physicsComponent != null) {
        if (moving) updateSpeed();
        else physicsComponent.getBody().setLinearVelocity(0f, 0f);
      }
    } else if (!isJumpEnabled()) {
      cancelJump();
    }
  }

  public boolean isJumpEnabled() {
    return !jumpOwners.isEmpty();
  }

  public boolean isJumping() {
    return jumping;
  }

  /** Visual height above the ground; the player's physical position stays on the floor. */
  public float getJumpHeight() {
    if (!jumping) return 0f;
    float progress = jumpElapsed / JUMP_DURATION;
    return JUMP_HEIGHT * 4f * progress * (1f - progress);
  }

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
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("dash", this::dash);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("specialAttack", this::specialAttack);
    entity.getEvents().addListener("entityDied", this::cancelJump);
  }

  @Override
  public void update() {
    updateDashState();
    updateJumpState();
    if (areControlsLocked()) {
      physicsComponent.getBody().setLinearVelocity(0f, 0f);
      updateIdleAnimation();
      return;
    }
    if (moving) {
      updateSpeed();
    }
    updateAnimation();
  }

  private void updateJumpState() {
    if (areControlsLocked() || (combatStats != null && combatStats.isDead())) {
      cancelJump();
      return;
    }
    if (!isJumpEnabled()) return;
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;
    if (jumping) {
      jumpElapsed += delta;
      if (jumpElapsed >= JUMP_DURATION) {
        jumping = false;
        jumpCooldownRemaining = Math.max(0f, JUMP_COOLDOWN - (jumpElapsed - JUMP_DURATION));
        jumpElapsed = 0f;
      }
    } else {
      jumpCooldownRemaining = Math.max(0f, jumpCooldownRemaining - delta);
    }
    updateJumpOffset();
  }

  private void jump() {
    if (jumping || jumpCooldownRemaining > 0f || combatStats.isDead()) return;
    jumping = true;
    jumpElapsed = 0f;
    updateJumpOffset();
  }

  private void cancelJump() {
    jumping = false;
    jumpElapsed = 0f;
    jumpCooldownRemaining = 0f;
    updateJumpOffset();
  }

  private void updateJumpOffset() {
    if (animator != null) animator.setVerticalOffset(getJumpHeight());
  }

  @Override
  public void dispose() {
    jumpOwners.clear();
    cancelJump();
    super.dispose();
  }

  /** Ends the dash and clears the dash cooldown once their respective durations have elapsed. */
  private void updateDashState() {
    // dashInit is 0 until the first dash. Do not treat that as an expired dash.
    if (dashOn && time.getTimeSince(dashInit) >= DASH_DURATION_MS) {
      dashOn = false;
      entity.getEvents().trigger(DASH_STOP);
    }
    if (dashCooldown && time.getTimeSince(dashInit) >= DASH_COOLDOWN_MS) {
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
    if (areControlsLocked()) {
      physicsComponent.getBody().setLinearVelocity(0f, 0f);
      return;
    }
    Body body = physicsComponent.getBody();
    Vector2 velocity = body.getLinearVelocity();
    Vector2 desiredVelocity;

    float movementSpeed = combatStats.getEffectiveMovementSpeed();

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
      this.walkDirection = direction.cpy();
      moving = true;

      if (!this.walkDirection.epsilonEquals(Vector2.Zero)) {
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
    if (areControlsLocked()) return;
    entity.getEvents().trigger("weaponAttack", facingDirection);
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /** Makes the player to do special attack. */
  void specialAttack() {
    if (areControlsLocked()) return;
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /**
   * Makes the player dash. The player only dashes if the dash is not currently on or on cooldown.
   */
  void dash(Vector2 direction) {
    if (areControlsLocked()) return;
    if (isJumpEnabled()) {
      jump();
      return;
    }
    if (!dashOn && !dashCooldown) {
      this.dashDirection =
          direction.epsilonEquals(Vector2.Zero) ? facingDirection.cpy() : direction.cpy();
      moving = true;
      dashOn = true;
      dashInit = time.getTime();
      dashCooldown = true;
    }
  }
}
