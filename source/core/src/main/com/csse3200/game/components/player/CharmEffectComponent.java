package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.items.Charm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for charm add/remove events on the player entity and applies or removes the corresponding
 * stat buff via {@link CombatStatsComponent}.
 *
 * <p>This component does not store charms itself - it only reacts to events fired when a charm is
 * added to or removed from the player's inventory (see {@link InventoryComponent}).
 */
public class CharmEffectComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(CharmEffectComponent.class);
  private static final String STRENGTH_CHARM_NAME = "Strength Charm";
  private static final int STRENGTH_CHARM_BONUS = 10;

  @Override
  public void create() {
    entity.getEvents().addListener("charmAdded", this::onCharmAdded);
    entity.getEvents().addListener("charmRemoved", this::onCharmRemoved);
  }

  /**
   * Called when a charm is added to the player's inventory. Applies the charm's stat buff.
   *
   * @param charm the charm that was added
   */
  private void onCharmAdded(Charm charm) {
    if (charm == null || !STRENGTH_CHARM_NAME.equalsIgnoreCase(charm.getName())) {
      return;
    }
    CombatStatsComponent combatStats = entity.getComponent(CombatStatsComponent.class);
    if (combatStats == null) {
      logger.error("Cannot apply charm buff: entity has no CombatStatsComponent");
      return;
    }
    combatStats.addStrength(STRENGTH_CHARM_BONUS);
    logger.info("Strength Charm added: Strength now {}", combatStats.getStrength());
  }

  /**
   * Called when a charm is removed from the player's inventory. Removes the charm's stat buff.
   *
   * @param charm the charm that was removed
   */
  private void onCharmRemoved(Charm charm) {
    if (charm == null || !STRENGTH_CHARM_NAME.equalsIgnoreCase(charm.getName())) {
      return;
    }
    CombatStatsComponent combatStats = entity.getComponent(CombatStatsComponent.class);
    if (combatStats == null) {
      logger.error("Cannot remove charm buff: entity has no CombatStatsComponent");
      return;
    }
    combatStats.addStrength(-STRENGTH_CHARM_BONUS);
    logger.info("Strength Charm removed: Strength now {}", combatStats.getStrength());
  }
}
