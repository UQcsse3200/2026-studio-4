package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.items.ItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies gameplay effects when a consumable item is used.
 * Currently supports Health Potion (instant heal).
 * Shield, Speed Potion, and Strength Potion (timed buffs) will be added
 * once the item-use trigger from the Pickup & Use Input task is available.
 */
public class ConsumableEffectComponent extends Component {
    private static final Logger logger = LoggerFactory.getLogger(ConsumableEffectComponent.class);

    // Amount restored by a Health Potion — tune this value as needed.
    private static final int HEALTH_POTION_HEAL_AMOUNT = 25;

    private CombatStatsComponent combatStats;

    @Override
    public void create() {
        combatStats = entity.getComponent(CombatStatsComponent.class);

        // TEMPORARY: manual trigger for testing until "itemUsed" event exists.
        // Remove this listener once Devendera's Pickup & Use Input task fires
        // the real "itemUsed" event, and replace it with:
        // entity.getEvents().addListener("itemUsed", this::applyEffect);
        entity.getEvents().addListener("debugUseConsumable", this::applyEffect);
    }

    /**
     * Applies the effect for the given consumable type.
     * Does nothing if the type is not a consumable.
     *
     * @param type the ItemType being used
     */
    public void applyEffect(ItemType type) {
        if (type == null || !type.isConsumable()) {
            return;
        }

        switch (type) {
            case HEALTH_POTION:
                applyHealthPotion();
                break;
            case SHIELD:
                // TODO: implement timed damage-block effect
                logger.info("Shield effect not yet implemented");
                break;
            case SPEED_POTION:
                // TODO: implement timed speed buff
                logger.info("Speed Potion effect not yet implemented");
                break;
            case STRENGTH_POTION:
                // TODO: implement timed strength buff
                logger.info("Strength Potion effect not yet implemented");
                break;
            default:
                logger.warn("No effect defined for consumable type: {}", type);
        }
    }

    private void applyHealthPotion() {
        if (combatStats == null) {
            logger.warn("No CombatStatsComponent found on entity — cannot heal");
            return;
        }
        int newHealth = combatStats.getHealth() + HEALTH_POTION_HEAL_AMOUNT;
        combatStats.setHealth(newHealth);
        logger.info("Health Potion used: healed {} HP", HEALTH_POTION_HEAL_AMOUNT);
    }
}