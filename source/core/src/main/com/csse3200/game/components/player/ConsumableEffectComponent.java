package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.items.ItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies gameplay effects when a consumable item is used.
 *
 * <p>Listens for the "itemUsed" event, fired with the ItemType being consumed. This event contract
 * matches the Pickup & Use Input task (Devendera) — see PR #162.
 */
public class ConsumableEffectComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ConsumableEffectComponent.class);

  private static final int HEALTH_POTION_HEAL_AMOUNT = 25;
  private static final long SHIELD_DURATION_MS = 8000;
  private static final long SPEED_DURATION_MS = 8000;
  private static final long STRENGTH_DURATION_MS = 8000;
  private static final float SPEED_MULTIPLIER = 1.5f;
  private static final float STRENGTH_MULTIPLIER = 1.5f;

  private CombatStatsComponent combatStats;
  private InventoryComponent inventory;

  // Shield
  private boolean shielded = false;
  private long shieldExpiryTime = 0;

  // Speed
  private boolean speedActive = false;
  private long speedExpiryTime = 0;
  private float baseMovementSpeed;

  // Strength
  private boolean strengthActive = false;
  private long strengthExpiryTime = 0;
  private int baseAttack;

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    inventory = entity.getComponent(InventoryComponent.class);
    entity.getEvents().addListener("itemUsed", this::applyEffect);
  }

  @Override
  public void update() {
    long now = System.currentTimeMillis();

    if (shielded && now >= shieldExpiryTime) {
      shielded = false;
      logger.info("Shield expired");
    }

    if (speedActive && now >= speedExpiryTime) {
      combatStats.setMovementSpeed(baseMovementSpeed);
      speedActive = false;
      logger.info("Speed Potion expired, movement speed reverted to {}", baseMovementSpeed);
    }

    if (strengthActive && now >= strengthExpiryTime) {
      combatStats.setBaseAttack(baseAttack);
      strengthActive = false;
      logger.info("Strength Potion expired, attack reverted to {}", baseAttack);
    }
  }

  /**
   * True while the player is under an active Shield effect. Damage-dealing code (e.g. enemy
   * attack/collision handling) should check this before applying damage to the player, and skip the
   * damage if true. This is the integration point other teams' combat code needs to call.
   */
  public boolean isShielded() {
    return shielded;
  }

  public void applyEffect(ItemType type) {
    if (type == null || !type.isConsumable()) {
      return;
    }
    if (inventory == null || !inventory.hasConsumable(type)) {
      logger.info("No {} available to use", type);
      return;
    }

    switch (type) {
      case HEALTH_POTION:
        applyHealthPotion();
        break;
      case SHIELD:
        applyShield();
        break;
      case SPEED_POTION:
        applySpeedPotion();
        break;
      case STRENGTH_POTION:
        applyStrengthPotion();
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
    inventory.removeConsumable(ItemType.HEALTH_POTION);
    int newHealth = combatStats.getHealth() + HEALTH_POTION_HEAL_AMOUNT;
    combatStats.setHealth(newHealth);
    logger.info("Health Potion used: healed {} HP", HEALTH_POTION_HEAL_AMOUNT);
  }

  private void applyShield() {
    inventory.removeConsumable(ItemType.SHIELD);
    shielded = true;
    shieldExpiryTime = System.currentTimeMillis() + SHIELD_DURATION_MS;
    logger.info("Shield activated for {} ms", SHIELD_DURATION_MS);
  }

  private void applySpeedPotion() {
    if (combatStats == null) {
      logger.warn("No CombatStatsComponent found on entity — cannot boost speed");
      return;
    }
    inventory.removeConsumable(ItemType.SPEED_POTION);
    if (!speedActive) {
      baseMovementSpeed = combatStats.getMovementSpeed();
    }
    combatStats.setMovementSpeed(baseMovementSpeed * SPEED_MULTIPLIER);
    speedActive = true;
    speedExpiryTime = System.currentTimeMillis() + SPEED_DURATION_MS;
    logger.info("Speed Potion used: movement speed boosted for {} ms", SPEED_DURATION_MS);
  }

  private void applyStrengthPotion() {
    if (combatStats == null) {
      logger.warn("No CombatStatsComponent found on entity — cannot boost attack");
      return;
    }
    inventory.removeConsumable(ItemType.STRENGTH_POTION);
    if (!strengthActive) {
      baseAttack = combatStats.getBaseAttack();
    }
    combatStats.setBaseAttack(Math.round(baseAttack * STRENGTH_MULTIPLIER));
    strengthActive = true;
    strengthExpiryTime = System.currentTimeMillis() + STRENGTH_DURATION_MS;
    logger.info("Strength Potion used: attack boosted for {} ms", STRENGTH_DURATION_MS);
  }
}
