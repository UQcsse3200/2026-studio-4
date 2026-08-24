package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;
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

  public CombatStatsComponent(int health, int baseAttack) {
    this.maxHealth = health;
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  /**
   * Returns true if the entity's has 0 health, otherwise false.
   *
   * @return is player dead
   */
  public Boolean isDead() {
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
        entity.getEvents().trigger("entityDied", entity.getPosition(), entity);
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

  /**
   * Sets the entity's attack damage. Attack damage has a minimum bound of 0.
   *
   * @param attack Attack damage
   */
  public void setBaseAttack(int attack) {
    if (attack >= 0) {
      this.baseAttack = attack;
    } else {
      logger.error("Can not set base attack to a negative attack value");
    }
  }

  /**
   * Core method for dealing raw damage directly. Handles health reduction, hit reaction, and death
   * checks. Compatible with Task 2 ticket spec.
   *
   * @param damage Amount of damage to deal
   */
  public void takeDamage(int damage) {
    takeDamage(damage,null);
  }

  /**
   * Core method for dealing raw damage directly. Handles health reduction, hit reaction, and death
   * checks. Compatible with Task 2 ticket spec.
   *
   * @param damage Amount of damage to deal
   */
  public void takeDamage(int damage, Entity attacker) {
    if (damage > 0) {
      addHealth(-damage);
      if (!isDead()) {
        applyHitreaction(attacker);
      }
    }
  }

  /**
   * Covinience method for entity-on-emtity combat. Reads base attack from the attacker and applies
   * damage.
   *
   * @param attacker The entity dealing damage
   */
  public void hit(CombatStatsComponent attacker) {
    if (attacker != null) {
      takeDamage(attacker.getBaseAttack(),attacker.getEntity());
    }
  }

  /** Applies visual red flash and knockback hit reaction. */
  private void applyHitreaction(Entity attacker) {
    // TODO: Visual red flash & knockback implementation before Aug 28
    if (entity != null) {
      entity.getEvents().trigger("hitReaction",attacker);
    }
  }
}
