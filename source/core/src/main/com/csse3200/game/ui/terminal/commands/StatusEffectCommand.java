package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that applies one of the player's status effects.
 *
 * <p>
 */
public class StatusEffectCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(StatusEffectCommand.class);
  private static final Map<String, Character> STATUS_EFFECTS =
      Map.of(
          "burn", 'b',
          "regeneration", 'r',
          "slow", 's',
          "speed", 'S',
          "vulnerable", 'v',
          "freeze", 'f');

  private final Entity player;

  /**
   * @param player entity carrying the status effect controller component
   */
  public StatusEffectCommand(Entity player) {
    this.player = player;
  }

  /**
   * Adds the status effect.
   *
   * @param args single argument: {@code burn}, {@code regeneration}, {@code vulnerable} etc.
   * @return true if a status effect was added.
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !STATUS_EFFECTS.containsKey(args.getFirst())) {
      logger.debug("Invalid arguments received for 'weapon' command: {}", args);
      return false;
    }

    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(1, STATUS_EFFECTS.get(args.getFirst()));

    return true;
  }
}
