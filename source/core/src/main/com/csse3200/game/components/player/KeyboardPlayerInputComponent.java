package com.csse3200.game.components.player;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.utils.math.Vector2Utils;

/**
 * Input handler for the player for keyboard and touch (mouse) input. This input handler only uses
 * keyboard input.
 */
public class KeyboardPlayerInputComponent extends InputComponent {
  private static final String EQUIP_WEAPON_EVENT = "equipWeapon";
  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private boolean upPressed;
  private boolean downPressed;
  private boolean leftPressed;
  private boolean rightPressed;
  private boolean controlsConfused;

  public KeyboardPlayerInputComponent() {
    super(5);
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
      case Keys.NUM_7, Keys.NUM_8, Keys.NUM_9, Keys.NUM_0:
        ConsumableLoadoutComponent loadout = entity.getComponent(ConsumableLoadoutComponent.class);
        if (loadout != null) {
          int slot =
              switch (keycode) {
                case Keys.NUM_7 -> 0;
                case Keys.NUM_8 -> 1;
                case Keys.NUM_9 -> 2;
                default -> 3;
              };
          loadout.useSlot(slot);
        }
        return true;
      case Keys.W:
        upPressed = true;
        rebuildWalkDirection();
        return true;
      case Keys.A:
        leftPressed = true;
        rebuildWalkDirection();
        return true;
      case Keys.S:
        downPressed = true;
        rebuildWalkDirection();
        return true;
      case Keys.D:
        rightPressed = true;
        rebuildWalkDirection();
        return true;
      case Keys.SPACE:
        if (controlsAreConfused()) {
          entity.getEvents().trigger("attack");
        } else {
          entity.getEvents().trigger("dash", walkDirection);
        }
        return true;
      case Keys.J:
        if (controlsAreConfused()) {
          entity.getEvents().trigger("dash", walkDirection);
        } else {
          entity.getEvents().trigger("attack");
        }
        return true;
      case Keys.K:
        // Heavy weapon attack; "specialAttack" is reserved for special abilities.
        entity.getEvents().trigger("heavyAttack");
        return true;
      case Keys.E:
        // Keep Room navigation and Team 5 item pickup on separate event contracts.
        entity.getEvents().trigger("interact");
        entity.getEvents().trigger("itemPickup");
        return true;
      case Keys.I:
        entity.getComponent(InventoryComponent.class).toggleDisplay();
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
    switch (keycode) {
      case Keys.W:
        upPressed = false;
        rebuildWalkDirection();
        return true;
      case Keys.A:
        leftPressed = false;
        rebuildWalkDirection();
        return true;
      case Keys.S:
        downPressed = false;
        rebuildWalkDirection();
        return true;
      case Keys.D:
        rightPressed = false;
        rebuildWalkDirection();
        return true;
      default:
        return false;
    }
  }

  @Override
  public void update() {
    boolean confusedNow = controlsAreConfused();
    if (confusedNow != controlsConfused) {
      rebuildWalkDirection();
    }
  }

  private boolean controlsAreConfused() {
    return StatusEffectsControllerComponent.isControlsConfused(entity);
  }

  private void rebuildWalkDirection() {
    controlsConfused = controlsAreConfused();
    walkDirection.setZero();
    if (upPressed) walkDirection.add(Vector2Utils.UP);
    if (downPressed) walkDirection.add(Vector2Utils.DOWN);
    if (leftPressed) walkDirection.add(Vector2Utils.LEFT);
    if (rightPressed) walkDirection.add(Vector2Utils.RIGHT);
    if (controlsConfused) walkDirection.scl(-1f);
    triggerWalkEvent();
  }

  private void triggerWalkEvent() {
    if (walkDirection.epsilonEquals(Vector2.Zero)) {
      entity.getEvents().trigger("walkStop");
    } else {
      entity.getEvents().trigger("walk", walkDirection);
    }
  }
}
