package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;

/**
 * Drives {@link TeleportMenu} from the keyboard and, while it is open, stops anything else seeing
 * input.
 *
 * <p>Registered at the same priority as the ability menu: above the UI stage and the debug
 * terminal, below the dialogue and cutscene handler so the NPC can finish speaking first. Every
 * event is swallowed while the menu is open, so W, A, S and D cannot walk the player around behind
 * it.
 */
public class KeyboardTeleportMenuInputComponent extends InputComponent {
  static final int TELEPORT_MENU_INPUT_ORDER = 15;

  private TeleportMenu menu;

  public KeyboardTeleportMenuInputComponent() {
    super(TELEPORT_MENU_INPUT_ORDER);
  }

  public KeyboardTeleportMenuInputComponent(TeleportMenu menu) {
    this();
    this.menu = menu;
  }

  @Override
  public void create() {
    super.create();
    if (menu == null) {
      menu = entity.getComponent(TeleportMenu.class);
    }
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!isOpen()) {
      return false;
    }
    switch (keycode) {
      case Keys.UP, Keys.W -> menu.moveSelection(-1);
      case Keys.DOWN, Keys.S -> menu.moveSelection(1);
      case Keys.ENTER, Keys.NUMPAD_ENTER, Keys.SPACE, Keys.E -> menu.confirm();
      case Keys.ESCAPE, Keys.Q -> menu.close();
      default -> {
        // Swallowed below so nothing behind the menu reacts to it.
      }
    }
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    return isOpen();
  }

  @Override
  public boolean keyTyped(char character) {
    return isOpen();
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    return isOpen();
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return isOpen();
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    return isOpen();
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    return isOpen();
  }

  private boolean isOpen() {
    return menu != null && menu.isOpen();
  }
}
