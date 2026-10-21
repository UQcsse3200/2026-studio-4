package com.csse3200.game.components.maingame;

/** Drives {@link TeleportMenu} from the keyboard. See {@link ModalMenuInputComponent}. */
public class KeyboardTeleportMenuInputComponent extends ModalMenuInputComponent<TeleportMenu> {
  public KeyboardTeleportMenuInputComponent() {
    super(TeleportMenu.class);
  }

  public KeyboardTeleportMenuInputComponent(TeleportMenu menu) {
    super(TeleportMenu.class, menu);
  }
}
