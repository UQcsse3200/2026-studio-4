package com.csse3200.game.components;

import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component used to store information related to combat such as health, attack, etc. Any entities
 * which engage it combat should have an instance of this class registered. This class can be
 * extended for more specific combat needs.
 */
public class CombatStatsComponent extends Component {

  private static final Logger logger = LoggerFactory.getLogger(CombatStatsComponent.class);
  private int health;
  private int maxHealth;
  private int baseAttack;
  private float movementSpeed;
  private float attackSpeed;
  private boolean invulnerable;
  private float incomingDamageMultiplier = 1f;
  private int minimumHealth;

  public CombatStatsComponent(int health, int baseAttack) {
    this.maxHealth = health;
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  public CombatStatsComponent(int health, int baseAttack, float movementSpeed, float attackSpeed) {
    this.maxHealth = health;
    setHealth(health);
    setBaseAttack(baseAttack);
    setMovementSpeed(movementSpeed);
    setAttackSpeed(attackSpeed);
  }

  /**
   * Returns true if the entity's has 0 health, otherwise false.
   *
   * @return is player dead
   */
  public boolean isDead() {
    return health == 0;
  }

  /**
   * Returns the entity's health.
   *
   * @return entity's health
   */
  public int getHealth() {
    return health;
  }

  /**
   * Sets the entity's health. Health has a minimum bound of 0.
   *
   * @return max health
   */
  public int getMaxHealth() {
    return maxHealth;
  }

  /**
   * Sets the entity's health. Health has a minimum bound of 0.
   *
   * @param maxHealth max health
   */
  public void setMaxHealth(int maxHealth) {
    if (maxHealth >= 0) {
      this.maxHealth = maxHealth;
      if (entity != null) {
        entity.getEvents().trigger("updateMaxHealth", this.maxHealth);
      }
    } else {
      logger.error("cannot set health to a negative value");
    }
  }

  /**
   * Sets the entity's health. Health has a minimum bound of 0.
   *
   * @param health health
   */
  public void setHealth(int health) {
    boolean wasDead = isDead();

    if (health > maxHealth) {
      this.health = maxHealth;
    } else if (health >= 0) {
      this.health = health;
    } else {
      this.health = 0;
    }

    if (entity != null) {
      entity.getEvents().trigger("updateHealth", this.health);
      if (!wasDead && isDead()) {
        entity.getEvents().trigger("entityDied");
      }
    }
  }

  /**
   * Adds to the player's health. The amount added can be negative.
   *
   * @param health health to add
   */
  public void addHealth(int health) {
    setHealth(this.health + health);
  }

  /**
   * Returns the entity's base attack damage.
   *
   * @return base attack damage
   */
  public int getBaseAttack() {
    return baseAttack;
  }

  /** Returns base attack with Last Stand applied, without changing charm-adjusted raw stats. */
  public int getEffectiveBaseAttack() {
    return LastStand.isActiveOn(entity)
        ? Math.round(baseAttack * LastStand.MULTIPLIER)
        : baseAttack;
  }

  /**
   * Sets the entity's attack damage. Attack damage has a minimum bound of 0.
   *
   * @param attack Attack damage
   */
  public void setBaseAttack(int attack) {
    if (attack >= 0) {
      this.baseAttack = attack;
      if (entity != null) {
        entity.getEvents().trigger("updateBaseAttack", this.baseAttack);
      }
    } else {
      logger.error("Can not set base attack to a negative attack value");
    }
  }

  /**
   * Adds to the entity's base attack damage. The amount added can be negative.
   *
   * @param attack attack damage to add
   */
  public void addBaseAttack(int attack) {
    setBaseAttack(this.baseAttack + attack);
  }

  /**
   * Returns the entity's movement speed
   *
   * @return entity's movement speed
   */
  public float getMovementSpeed() {
    return movementSpeed;
  }

  /** Returns movement speed with Last Stand applied, without changing charm-adjusted raw stats. */
  public float getEffectiveMovementSpeed() {
    return LastStand.isActiveOn(entity) ? movementSpeed * LastStand.MULTIPLIER : movementSpeed;
  }

  /**
   * Sets the entity's movement speed. Movement Speed has a minimum bound of 0.
   *
   * @param newSpeed new movement speed
   */
  public void setMovementSpeed(float newSpeed) {
    if (newSpeed >= 0) {
      this.movementSpeed = newSpeed;
      if (entity != null) {
        entity.getEvents().trigger("updateMovementSpeed", this.movementSpeed);
      }
    } else {
      logger.error("Can not set movement speed of entity to a negative value");
    }
  }

  /**
   * Adds to the player's movement speed. The amount added can be negative.
   *
   * @param speed speed to add
   */
  public void addMovementSpeed(float speed) {
    setMovementSpeed(this.movementSpeed + speed);
  }

  /**
   * Returns the entity's attack speed
   *
   * @return entity's attack speed
   */
  public float getAttackSpeed() {
    return attackSpeed;
  }

  /** Returns attack speed with Last Stand applied, without changing charm-adjusted raw stats. */
  public float getEffectiveAttackSpeed() {
    return LastStand.isActiveOn(entity) ? attackSpeed * LastStand.MULTIPLIER : attackSpeed;
  }

  /**
   * Sets the entity's attack speed. Attack Speed has a minimum bound of 0.
   *
   * @param newSpeed entity's new attack speed
   */
  public void setAttackSpeed(float newSpeed) {
    if (newSpeed >= 0) {
      this.attackSpeed = newSpeed;
      if (entity != null) {
        entity.getEvents().trigger("updateAttackSpeed", this.attackSpeed);
      }
    } else {
      logger.error("Can not set attack speed of entity to a negative value");
    }
  }

  /**
   * Adds to the player's attack speed. The amount added can be negative.
   *
   * @param speed speed to add
   */
  public void addAttackSpeed(float speed) {
    setAttackSpeed(this.attackSpeed + speed);
  }

  /**
   * Core method for dealing raw damage directly. Handles health reduction, hit reaction, and death
   * checks. Compatible with Task 2 ticket spec.
   *
   * @param damage Amount of damage to deal
   */
  public void takeDamage(int damage) {
    takeDamage(damage, null);
  }

  /**
   * Core method for dealing raw damage directly. Handles health reduction, hit reaction, and death
   * checks. Emits {@code damageTaken(Entity attacker, int healthLost, int remainingHealth)} only
   * for actual health loss, after applying damage. Direct health setters do not emit this event.
   *
   * @param damage Amount of damage to deal
   * @param attacker original damage source, including projectile entities; null for unattributed
   *     damage
   */
  public void takeDamage(int damage, Entity attacker) {
    if (damage <= 0) {
      return;
    }

    if (invulnerable || (isHostileAttacker(attacker) && Invisibility.isActiveOn(entity))) {
      triggerDamageBlocked();
      return;
    }

    int adjustedDamage = Math.round(damage * incomingDamageMultiplier);
    int newHealth = Math.max(minimumHealth, health - adjustedDamage);

    if (newHealth == health) {
      triggerDamageBlocked();
      applyHitreaction(attacker);
      return;
    }

    int previousHealth = health;
    int remainingHealth = Math.clamp(newHealth, 0, maxHealth);
    setHealth(newHealth);
    if (entity != null && remainingHealth < previousHealth) {
      entity
          .getEvents()
          .trigger("damageTaken", attacker, previousHealth - remainingHealth, remainingHealth);
    }
    applyHitreaction(attacker);
  }

  /** Identifies enemy bodies and attack entities, preserving projectiles as the actual attacker. */
  public static boolean isHostileAttacker(Entity attacker) {
    if (attacker == null) {
      return false;
    }
    TouchAttackComponent touch = attacker.getComponent(TouchAttackComponent.class);
    HitboxComponent hitbox = attacker.getComponent(HitboxComponent.class);
    ColliderComponent collider = attacker.getComponent(ColliderComponent.class);
    return (touch != null && PhysicsLayer.contains(touch.getTargetLayer(), PhysicsLayer.PLAYER))
        || (hitbox != null && PhysicsLayer.contains(hitbox.getLayer(), PhysicsLayer.NPC))
        || (collider != null && PhysicsLayer.contains(collider.getLayer(), PhysicsLayer.NPC));
  }

  /**
   * Enables or disables immunity to incoming damage.
   *
   * @param invulnerable whether incoming damage should be blocked
   */
  public void setInvulnerable(boolean invulnerable) {
    this.invulnerable = invulnerable;
  }

  /** Returns whether all incoming damage is currently blocked. */
  public boolean isInvulnerable() {
    return invulnerable;
  }

  /**
   * Sets the multiplier applied to incoming damage.
   *
   * @param multiplier non-negative incoming damage multiplier
   */
  public void setIncomingDamageMultiplier(float multiplier) {
    if (multiplier < 0f) {
      throw new IllegalArgumentException("Incoming damage multiplier must be non-negative");
    }

    incomingDamageMultiplier = multiplier;
  }

  /** Returns the current incoming damage multiplier. */
  public float getIncomingDamageMultiplier() {
    return incomingDamageMultiplier;
  }

  /**
   * Sets the lowest health reachable through takeDamage().
   *
   * <p>Direct setHealth() calls can still cross this value for scripted phase transitions.
   */
  public void setMinimumHealth(int minimumHealth) {
    if (minimumHealth < 0 || minimumHealth > maxHealth) {
      throw new IllegalArgumentException("Minimum health must be between zero and maximum health");
    }

    this.minimumHealth = minimumHealth;
  }

  /** Returns the current incoming-damage health floor. */
  public int getMinimumHealth() {
    return minimumHealth;
  }

  /**
   * Covinience method for entity-on-emtity combat. Reads base attack from the attacker and applies
   * damage.
   *
   * @param attacker The entity dealing damage
   */
  public void hit(CombatStatsComponent attacker) {
    if (attacker != null) {
      takeDamage(attacker.getEffectiveBaseAttack(), attacker.getEntity());
    }
  }

  /** Applies visual red flash and knockback hit reaction. */
  private void applyHitreaction(Entity attacker) {
    if (entity != null) {
      entity.getEvents().trigger("hitReaction", attacker);
    }
  }

  private void triggerDamageBlocked() {
    if (entity != null) {
      entity.getEvents().trigger("damageBlocked");
    }
  }
}
