package com.csse3200.game.components.shop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.csse3200.game.input.InputComponent;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Modal input below narrative priority 20 and above the ordinary stage priority 10. Mouse events
 * reach Scene2D once and are then consumed; releases stay balanced when closing synchronously.
 */
public class ShopInputComponent extends InputComponent {
  private final ShopSessionComponent session;
  private final InputProcessor shopStageInput;
  private final Set<Integer> swallowedKeys = new HashSet<>();
  private final Set<Touch> swallowedTouches = new HashSet<>();

  public ShopInputComponent(ShopSessionComponent session, InputProcessor shopStageInput) {
    super(19);
    this.session = Objects.requireNonNull(session);
    this.shopStageInput = Objects.requireNonNull(shopStageInput);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!session.isOpen()) return false;
    swallowedKeys.add(keycode);
    if (keycode == Input.Keys.ESCAPE) {
      session.close();
    } else {
      shopStageInput.keyDown(keycode);
    }
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (!swallowedKeys.remove(keycode)) return false;
    shopStageInput.keyUp(keycode);
    return true;
  }

  @Override
  public boolean keyTyped(char character) {
    if (!session.isOpen()) return false;
    shopStageInput.keyTyped(character);
    return true;
  }

  @Override
  public boolean touchDown(int x, int y, int pointer, int button) {
    if (!session.isOpen()) return false;
    swallowedTouches.add(new Touch(pointer, button));
    shopStageInput.touchDown(x, y, pointer, button);
    return true;
  }

  @Override
  public boolean touchUp(int x, int y, int pointer, int button) {
    if (!swallowedTouches.remove(new Touch(pointer, button))) return false;
    shopStageInput.touchUp(x, y, pointer, button);
    return true;
  }

  @Override
  public boolean touchDragged(int x, int y, int pointer) {
    if (!session.isOpen()
        && swallowedTouches.stream().noneMatch(touch -> touch.pointer() == pointer)) {
      return false;
    }
    shopStageInput.touchDragged(x, y, pointer);
    return true;
  }

  @Override
  public boolean mouseMoved(int x, int y) {
    if (!session.isOpen()) return false;
    shopStageInput.mouseMoved(x, y);
    return true;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    if (!session.isOpen()) return false;
    shopStageInput.scrolled(amountX, amountY);
    return true;
  }

  private record Touch(int pointer, int button) {}
}
