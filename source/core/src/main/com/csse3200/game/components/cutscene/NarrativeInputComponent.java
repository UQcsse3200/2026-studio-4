package com.csse3200.game.components.cutscene;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Takes over input while a dialogue or cutscene is on screen so the game cannot be played behind
 * it. SPACE, ENTER, J or a click advance the dialogue (or skip a skippable cutscene) and ESC leaves
 * the dialogue, or skips the cutscene if no dialogue is open. J is also the light attack key, which
 * costs nothing here because every gameplay key is swallowed while a dialogue is up, and it keeps
 * the player's hand where it already rests. F1 still opens the debug terminal, and while the
 * terminal is open all input is left to it.
 *
 * <p>Releasing a key that was already held when the dialogue opened is still passed through, so the
 * player's walk direction bookkeeping stays balanced.
 */
public class NarrativeInputComponent extends InputComponent {
  /** Above the terminal and UI stage (10) and the player (0). */
  static final int NARRATIVE_INPUT_ORDER = 20;

  private final NarrativeManagerComponent manager;
  private final BooleanSupplier terminalOpen;
  private final Set<Integer> swallowedKeys = new HashSet<>();
  private final Set<Integer> swallowedButtons = new HashSet<>();

  /**
   * @param manager the dialogue and cutscene manager
   * @param terminalOpen whether the debug terminal is open. While it is, keys and clicks belong to
   *     the terminal, so typing a command cannot advance a dialogue.
   */
  public NarrativeInputComponent(NarrativeManagerComponent manager, BooleanSupplier terminalOpen) {
    super(NARRATIVE_INPUT_ORDER);
    this.manager = manager;
    this.terminalOpen = terminalOpen;
  }

  private boolean shouldHandleInput() {
    return manager.isNarrativeActive() && !terminalOpen.getAsBoolean();
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!shouldHandleInput() || keycode == Input.Keys.F1) {
      return false;
    }
    swallowedKeys.add(keycode);
    switch (keycode) {
      case Input.Keys.SPACE, Input.Keys.ENTER, Input.Keys.J -> manager.advance();
      case Input.Keys.ESCAPE -> manager.leave();
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
    if (!shouldHandleInput()) {
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
