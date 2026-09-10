package com.csse3200.game.components.player;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.items.ItemType;

/** Temporary number-key bindings for the Sprint 2 consumable demo. */
public class DemoConsumableInputComponent extends InputComponent {
  public DemoConsumableInputComponent() {
    super(4);
  }

  @Override
  public boolean keyDown(int keycode) {
    ItemType itemType = itemTypeForKey(keycode);
    if (itemType == null) {
      return false;
    }

    entity.getEvents().trigger("useConsumable", itemType);
    return true;
  }

  static ItemType itemTypeForKey(int keycode) {
    return switch (keycode) {
      case Keys.NUM_1 -> ItemType.HEALTH_POTION;
      case Keys.NUM_2 -> ItemType.SHIELD;
      case Keys.NUM_3 -> ItemType.SPEED_POTION;
      case Keys.NUM_4 -> ItemType.STRENGTH_POTION;
      default -> null;
    };
  }
}
