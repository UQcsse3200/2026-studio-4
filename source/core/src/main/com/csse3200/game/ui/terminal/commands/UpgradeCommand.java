package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal command that applies or reverts a weapon upgrade so QA does not need a pickup or shop:
 * {@code upgrade sword|knife}, or {@code upgrade sword|knife off} to revert.
 *
 * <p>Applying is idempotent: running {@code upgrade sword} again leaves the sword upgraded.
 */
public class UpgradeCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UpgradeCommand.class);
  private static final String OFF = "off";
  private static final Map<String, Class<? extends WeaponComponent>> WEAPONS =
      Map.of("sword", SwordWeaponComponent.class, "knife", KnifeWeaponComponent.class);

  private final Entity player;

  /**
   * @param player entity carrying the {@link WeaponUpgradeComponent}
   */
  public UpgradeCommand(Entity player) {
    this.player = player;
  }

  /**
   * Upgrades the named weapon, or reverts it when followed by {@code off}.
   *
   * @param args a weapon ({@code sword} or {@code knife}) to upgrade, optionally followed by {@code
   *     off} to revert
   * @return true if the upgrade state was set
   */
  @Override
  public boolean action(ArrayList<String> args) {
    boolean validLength = args.size() == 1 || (args.size() == 2 && OFF.equals(args.get(1)));
    String weaponName = validLength ? args.get(0) : null;
    if (weaponName == null || !WEAPONS.containsKey(weaponName)) {
      logger.debug("Invalid arguments received for 'upgrade' command: {}", args);
      return false;
    }

    WeaponUpgradeComponent upgrades = player.getComponent(WeaponUpgradeComponent.class);
    if (upgrades == null) {
      logger.debug("Player has no WeaponUpgradeComponent; cannot upgrade {}", weaponName);
      return false;
    }

    boolean on = args.size() == 1;
    return upgrades.setUpgraded(WEAPONS.get(weaponName), on);
  }
}
