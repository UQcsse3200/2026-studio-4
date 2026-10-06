package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;
import java.util.HashSet;
import java.util.Set;

/**
 * Takes over input while a dialogue or cutscene is on screen so the game cannot be played behind
 * it. SPACE, ENTER or a click advance the dialogue (or skip a skippable cutscene) and ESC skips the
 * cutscene; F1 still opens the debug terminal.
 *
 * <p>Releasing a key that was already held when the dialogue opened is still passed through, so the
 * player's walk direction bookkeeping stays balanced.
 */
public class NarrativeInputComponent extends InputComponent {
  /** Above the terminal and UI stage (10) and the player (0). */
  static final int PRIORITY = 20;

  private final NarrativeManagerComponent manager;
  private final Set<Integer> swallowedKeys = new HashSet<>();
  private final Set<Integer> swallowedButtons = new HashSet<>();

  public NarrativeInputComponent(NarrativeManagerComponent manager) {
    super(PRIORITY);
    this.manager = manager;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!manager.isNarrativeActive() || keycode == Input.Keys.F1) {
      return false;
    }
    swallowedKeys.add(keycode);
    switch (keycode) {
      case Input.Keys.SPACE, Input.Keys.ENTER -> manager.advance();
      case Input.Keys.ESCAPE -> manager.skipCutscene();
      default -> {
        // swallowed: gameplay keys do nothing while a dialogue or cutscene is on screen
      }
    }
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    return swallowedKeys.remove(keycode);
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    if (!manager.isNarrativeActive()) {
      return false;
    }
    swallowedButtons.add(button);
    if (button == Input.Buttons.LEFT) {
      manager.advance();
    }
    return true;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return swallowedButtons.remove(button);
  }
}
