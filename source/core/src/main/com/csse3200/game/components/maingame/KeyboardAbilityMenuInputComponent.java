package com.csse3200.game.components.maingame;

/** Drives {@link AbilityMenu} from the keyboard. See {@link ModalMenuInputComponent}. */
public class KeyboardAbilityMenuInputComponent extends ModalMenuInputComponent<AbilityMenu> {
  public KeyboardAbilityMenuInputComponent() {
    super(AbilityMenu.class);
  }

  public KeyboardAbilityMenuInputComponent(AbilityMenu menu) {
    super(AbilityMenu.class, menu);
  }
}
