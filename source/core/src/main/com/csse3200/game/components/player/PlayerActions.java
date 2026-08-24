package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
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
  private static final Vector2 MAX_SPEED = new Vector2(3f, 3f); // Metres per second
  private static final Vector2 DASH_SPEED = new Vector2(15f, 15f);

  private PhysicsComponent physicsComponent;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 dashDirection = Vector2.Zero.cpy();
  private boolean moving = false;

  private long dashInit;
  private boolean dashOn = false;
  private boolean dashCooldown = false;
  private final GameTime time = new GameTime();

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("dash", this::dash);
  }

  @Override
  public void update() {
    if (time.getTimeSince(dashInit) >= 75){
      dashOn = false;
    }
    if (time.getTimeSince(dashInit) >= 575) {
      dashCooldown = false;
    }
    if (moving) {
      updateSpeed();
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 velocity = body.getLinearVelocity();
    Vector2 desiredVelocity;
    if (dashOn){
      desiredVelocity = dashDirection.cpy().scl(DASH_SPEED);
    } else{
      desiredVelocity = walkDirection.cpy().scl(MAX_SPEED);
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
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /** Makes the player dash. The player only dashes if the dash is not currently on or on cooldown. */
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
