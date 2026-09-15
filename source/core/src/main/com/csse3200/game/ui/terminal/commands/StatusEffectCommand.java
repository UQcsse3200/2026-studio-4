package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that equips one of the player's weapons: {@code weapon knife|sword|bow}.
 *
 * <p>The player carries all weapon components at once; equipping enables the chosen one and
 * disables the rest, since only enabled weapons respond to attack input.
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
   * Equips the named weapon.
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
