package com.csse3200.game.components.player;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.utils.math.Vector2Utils;

public class KeyboardPlayerInputComponent extends InputComponent {
  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private ItemType selectedConsumable;

  public KeyboardPlayerInputComponent() {
    super(5);
  }

  @Override
  public boolean keyDown(int keycode) {
    switch (keycode) {
      case Keys.W:
        walkDirection.add(Vector2Utils.UP);
        triggerWalkEvent();
        return true;
      case Keys.A:
        walkDirection.add(Vector2Utils.LEFT);
        triggerWalkEvent();
        return true;
      case Keys.S:
        walkDirection.add(Vector2Utils.DOWN);
        triggerWalkEvent();
        return true;
      case Keys.D:
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
        entity.getEvents().trigger("specialAttack");
        return true;
      case Keys.E:
        entity.getEvents().trigger("interact");
        entity.getEvents().trigger("itemPickup");
        return true;
      case Keys.NUM_7:
        selectConsumable(ItemType.HEALTH_POTION);
        return true;
      case Keys.NUM_8:
        selectConsumable(ItemType.SHIELD);
        return true;
      case Keys.NUM_9:
        selectConsumable(ItemType.SPEED_POTION);
        return true;
      case Keys.NUM_0:
        selectConsumable(ItemType.STRENGTH_POTION);
        return true;
      case Keys.U:
        useSelectedConsumable();
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean keyUp(int keycode) {
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
      default:
        return false;
    }
  }

  public ItemType getSelectedConsumable() {
    return selectedConsumable;
  }

  private void selectConsumable(ItemType type) {
    selectedConsumable = type;
    entity.getEvents().trigger("selectedConsumableChanged", type);
  }

  private void useSelectedConsumable() {
    if (selectedConsumable == null) {
      return;
    }
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null || !inventory.hasConsumable(selectedConsumable)) {
      return;
    }
    inventory.removeConsumable(selectedConsumable);
    entity.getEvents().trigger("itemUsed", selectedConsumable);
  }

  private void triggerWalkEvent() {
    if (walkDirection.epsilonEquals(Vector2.Zero)) {
      entity.getEvents().trigger("walkStop");
    } else {
      entity.getEvents().trigger("walk", walkDirection);
    }
  }
}
