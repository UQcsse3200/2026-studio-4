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
 *
 * <p>Tracks how many active Strength Charms the player currently holds so that the buff is applied
 * exactly once and removed exactly once, even if multiple Strength Charms are picked up or dropped.
 * This prevents the Strength buff from stacking incorrectly on duplicate charms.
 */
public class CharmEffectComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(CharmEffectComponent.class);
  private static final String STRENGTH_CHARM_NAME = "Strength Charm";
  private static final int STRENGTH_CHARM_BONUS = 10;

  // Number of Strength Charms currently held. The buff is only applied when this goes from 0 to 1,
  // and only removed when it drops back to 0, so holding/dropping duplicates never stacks the buff.
  private int activeStrengthCharms = 0;

  @Override
  public void create() {
    entity.getEvents().addListener("charmAdded", this::onCharmAdded);
    entity.getEvents().addListener("charmRemoved", this::onCharmRemoved);
  }

  /**
   * Called when a charm is added to the player's inventory. Applies the charm's stat buff the first
   * time a Strength Charm is added; further duplicates are tracked but do not re-apply the buff.
   *
   * @param charm the charm that was added
   */
  private void onCharmAdded(Charm charm) {
    if (!isStrengthCharm(charm)) {
      return;
    }

    activeStrengthCharms++;
    if (activeStrengthCharms > 1) {
      logger.debug(
          "Strength Charm added but buff already active ({} held); skipping re-apply",
          activeStrengthCharms);
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
   * Called when a charm is removed from the player's inventory. Removes the charm's stat buff only
   * once the last held Strength Charm is removed.
   *
   * @param charm the charm that was removed
   */
  private void onCharmRemoved(Charm charm) {
    if (!isStrengthCharm(charm)) {
      return;
    }

    if (activeStrengthCharms == 0) {
      logger.warn("Strength Charm removed but none were tracked as active; ignoring");
      return;
    }

    activeStrengthCharms--;
    if (activeStrengthCharms > 0) {
      logger.debug(
          "Strength Charm removed but {} still held; keeping buff active", activeStrengthCharms);
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

  private static boolean isStrengthCharm(Charm charm) {
    return charm != null && STRENGTH_CHARM_NAME.equalsIgnoreCase(charm.getName());
  }
}
