package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.cutscene.CutsceneEvents;
import com.csse3200.game.components.friendlynpc.NpcInteractorComponent;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;

/**
 * Input handler for the player for keyboard and touch (mouse) input. This input handler only uses
 * keyboard input.
 */
public class KeyboardPlayerInputComponent extends InputComponent {
  /** Fired once when E has been held for one second. */
  public static final String ROOM_MAGNETIC_PICKUP = "roomMagneticPickup";

  private static final long MAGNETIC_PICKUP_HOLD_MILLIS = 1000L;
  private static final String EQUIP_WEAPON_EVENT = "equipWeapon";
  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private boolean ignoreStaleKeyUps;
  private InteractionPress interactionPress = InteractionPress.IDLE;
  private long interactionPressedAt;

  private enum InteractionPress {
    IDLE,
    HOLDING,
    TRIGGERED,
    CANCELLED
  }

  public KeyboardPlayerInputComponent() {
    super(5);
  }

  @Override
  public void create() {
    super.create();
    entity
        .getEvents()
        .addListener(
            "resetMovementInput",
            () -> {
              walkDirection.setZero();
              ignoreStaleKeyUps = true;
              cancelPickupHold();
            });
    entity.getEvents().addListener("roomPickupReset", this::cancelPickupHold);
    entity
        .getEvents()
        .addListener(CutsceneEvents.DIALOGUE_STARTED, (String id) -> cancelPickupHold());
    entity
        .getEvents()
        .addListener(CutsceneEvents.CUTSCENE_STARTED, (String id) -> cancelPickupHold());
  }

  @Override
  public void update() {
    if (interactionPress == InteractionPress.CANCELLED
        && Gdx.input != null
        && !Gdx.input.isKeyPressed(Keys.E)) {
      // A modal interface may have consumed the release while it was open.
      interactionPress = InteractionPress.IDLE;
    }
    tryTriggerMagneticPickup();
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyDown(int)
   */
  @Override
  public boolean keyDown(int keycode) {
    switch (keycode) {
      case Keys.NUM_1:
        entity.getEvents().trigger(EQUIP_WEAPON_EVENT, WeaponType.SWORD);
        return true;
      case Keys.NUM_2:
        entity.getEvents().trigger(EQUIP_WEAPON_EVENT, WeaponType.DAGGER);
        return true;
      case Keys.NUM_3:
        entity.getEvents().trigger(EQUIP_WEAPON_EVENT, WeaponType.BOW);
        return true;
      case Keys.W:
        ignoreStaleKeyUps = false;
        walkDirection.add(Vector2Utils.UP);
        triggerWalkEvent();
        return true;
      case Keys.A:
        ignoreStaleKeyUps = false;
        walkDirection.add(Vector2Utils.LEFT);
        triggerWalkEvent();
        return true;
      case Keys.S:
        ignoreStaleKeyUps = false;
        walkDirection.add(Vector2Utils.DOWN);
        triggerWalkEvent();
        return true;
      case Keys.D:
        ignoreStaleKeyUps = false;
        walkDirection.add(Vector2Utils.RIGHT);
        triggerWalkEvent();
        return true;
      case Keys.SPACE:
        entity.getEvents().trigger("dash", walkDirection);
        return true;
      case Keys.J:
        entity.getEvents().trigger("attack");
        return true;
      case Keys.K:
        // Heavy weapon attack; "specialAttack" is reserved for special abilities.
        entity.getEvents().trigger("heavyAttack");
        return true;
      case Keys.E:
        beginInteractionHold();
        return true;
      case Keys.I:
        cancelPickupHold();
        entity.getComponent(InventoryComponent.class).toggleDisplay();
        return true;
      case Keys.TAB:
        entity.getEvents().trigger(ConsumableSelectionComponent.CYCLE_REQUEST);
        return true;
      case Keys.Q:
        entity.getEvents().trigger(ConsumableSelectionComponent.USE_SELECTED_REQUEST);
        return true;
      case Keys.L:
        // Next to the J and K attacks, since casting is another thing the right hand does.
        entity.getEvents().trigger(AbilityAttunementComponent.USE_ATTUNED_REQUEST);
        return true;
      default:
        return false;
    }
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyUp(int)
   */
  @Override
  public boolean keyUp(int keycode) {
    if (ignoreStaleKeyUps
        && (keycode == Keys.W || keycode == Keys.A || keycode == Keys.S || keycode == Keys.D)) {
      return true;
    }
    switch (keycode) {
      case Keys.W:
        walkDirection.sub(Vector2Utils.UP);
        triggerWalkEvent();
        return true;
      case Keys.A:
        walkDirection.sub(Vector2Utils.LEFT);
        triggerWalkEvent();
        return true;
      case Keys.S:
        walkDirection.sub(Vector2Utils.DOWN);
        triggerWalkEvent();
        return true;
      case Keys.D:
        walkDirection.sub(Vector2Utils.RIGHT);
        triggerWalkEvent();
        return true;
      case Keys.E:
        finishInteractionHold();
        return true;
      default:
        return false;
    }
  }

  private void beginInteractionHold() {
    if (interactionPress != InteractionPress.IDLE) {
      return; // Keyboard repeats must not restart the timer or capture another batch.
    }
    if (isInteractionBlocked()) {
      interactionPress = InteractionPress.CANCELLED;
      return;
    }
    interactionPressedAt = ServiceLocator.getTimeSource().getTime();
    interactionPress = InteractionPress.HOLDING;
  }

  private void finishInteractionHold() {
    // A release at the threshold may arrive before this frame's update.
    tryTriggerMagneticPickup();
    boolean shortPress = interactionPress == InteractionPress.HOLDING;
    interactionPress = InteractionPress.IDLE;
    if (!shortPress || isInteractionBlocked()) {
      return;
    }
    // Delay the normal interaction until release so a long hold cannot open an exit or NPC.
    entity.getEvents().trigger("interact");
    if (!isInteractionBlocked()) {
      entity.getEvents().trigger("itemPickup");
    }
  }

  private void tryTriggerMagneticPickup() {
    if (interactionPress != InteractionPress.HOLDING) {
      return;
    }
    if (isInteractionBlocked()) {
      cancelPickupHold();
      return;
    }
    if (ServiceLocator.getTimeSource().getTime() - interactionPressedAt
        >= MAGNETIC_PICKUP_HOLD_MILLIS) {
      interactionPress = InteractionPress.TRIGGERED;
      entity.getEvents().trigger(ROOM_MAGNETIC_PICKUP);
    }
  }

  /**
   * Cancels this E press when gameplay input is interrupted. An already triggered attraction batch
   * is owned by the pickup component and continues independently.
   */
  public void cancelPickupHold() {
    if (interactionPress != InteractionPress.IDLE) {
      interactionPress = InteractionPress.CANCELLED;
    }
  }

  private boolean isInteractionBlocked() {
    return isNpcInteractionRunning()
        || (ServiceLocator.getEntityService() != null
            && ServiceLocator.getEntityService().isFrozen())
        || (ServiceLocator.getRunTimer() != null && ServiceLocator.getRunTimer().isPaused());
  }

  private boolean isNpcInteractionRunning() {
    NpcInteractorComponent interactor = entity.getComponent(NpcInteractorComponent.class);
    return interactor != null && interactor.isInteracting();
  }

  private void triggerWalkEvent() {
    if (walkDirection.epsilonEquals(Vector2.Zero)) {
      entity.getEvents().trigger("walkStop");
    } else {
      entity.getEvents().trigger("walk", walkDirection);
    }
  }
}
