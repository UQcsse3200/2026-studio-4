package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.InvisibilityPotionComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * F1 debug abilities: {@code ability invisibility} applies the real 15s stealth effect and bypasses
 * potion inventory and the 45s cooldown.
 */
public class AbilityCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(AbilityCommand.class);

  private final Entity player;

  /**
   * @param player entity that owns {@link InvisibilityPotionComponent}
   */
  public AbilityCommand(Entity player) {
    this.player = player;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !"invisibility".equals(args.get(0))) {
      logger.debug("Invalid arguments received for 'ability' command: {}", args);
      return false;
    }
    InvisibilityPotionComponent invisibility =
        player.getComponent(InvisibilityPotionComponent.class);
    if (invisibility == null) {
      logger.debug("Player is missing InvisibilityPotionComponent");
      return false;
    }
    return invisibility.applyForQa();
  }
}
