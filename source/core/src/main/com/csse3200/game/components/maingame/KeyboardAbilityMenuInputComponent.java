package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.input.InputComponent;

/**
 * Drives {@link AbilityMenu} from the keyboard and, while it is open, stops anything else seeing
 * input.
 *
 * <p>Registered above the UI stage and the debug terminal (10) so the menu is genuinely modal, and
 * below the dialogue and cutscene handler (20) so Hecate can finish speaking first. Every event is
 * swallowed while the menu is open, not only the ones that mean something here: W, A, S and D would
 * otherwise walk the player around behind the menu, and the movement lock alone does not stop the
 * walk animation or the events that go with it.
 */
public class KeyboardAbilityMenuInputComponent extends InputComponent {
  /** Above the UI stage and terminal (10), below dialogue and cutscenes (20). */
  static final int ABILITY_MENU_INPUT_ORDER = 15;

  private AbilityMenu menu;

  public KeyboardAbilityMenuInputComponent() {
    super(ABILITY_MENU_INPUT_ORDER);
  }

  public KeyboardAbilityMenuInputComponent(AbilityMenu menu) {
    this();
    this.menu = menu;
  }

  @Override
  public void create() {
    super.create();
    if (menu == null) {
      menu = entity.getComponent(AbilityMenu.class);
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
