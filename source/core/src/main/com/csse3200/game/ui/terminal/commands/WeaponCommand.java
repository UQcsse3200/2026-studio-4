package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
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
public class WeaponCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(WeaponCommand.class);
  private static final Map<String, Class<? extends WeaponComponent>> WEAPONS =
      Map.of(
          "knife", KnifeWeaponComponent.class,
          "sword", SwordWeaponComponent.class,
          "bow", BowWeaponComponent.class);

  private final Entity player;

  /**
   * @param player entity carrying the weapon components to switch between
   */
  public WeaponCommand(Entity player) {
    this.player = player;
  }

  /**
   * Equips the named weapon.
   *
   * @param args single argument: {@code knife}, {@code sword}, or {@code bow}
   * @return true if a weapon was equipped
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !WEAPONS.containsKey(args.get(0))) {
      logger.debug("Invalid arguments received for 'weapon' command: {}", args);
      return false;
    }

    Class<? extends WeaponComponent> selected = WEAPONS.get(args.get(0));
    for (Class<? extends WeaponComponent> weaponClass : WEAPONS.values()) {
      WeaponComponent weapon = player.getComponent(weaponClass);
      if (weapon != null) {
        weapon.setEnabled(weaponClass == selected);
      }
    }
    return true;
  }
}
