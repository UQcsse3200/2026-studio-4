package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.services.ServiceLocator;

/**
 * Component that splits an enemy into two weaker copies when attacked by the player. This should
 * only happen once. Each new copy has half the lifespan (health) and half the base attack of the
 * original enemy.
 */
public class SplitComponent extends Component {
  private boolean hasSplit = false;
  private final Entity target;

  /**
   * @param target The entity to chase (usually the player), passed on to the split-off children.
   */
  public SplitComponent(Entity target) {
    this.target = target;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("hitReaction", this::onHitReaction);
  }

  /**
   * Runs whenever this entity is hit and survives. Queues a split into two smaller copies exactly
   * once, along with disposal of the original; both run once the current update ends.
   *
   * @param attacker The entity that caused the damage (can be null).
   */
  private void onHitReaction(Entity attacker) {
    if (hasSplit || entity == null) {
      return;
    }
    hasSplit = true;

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }

    int halfHealth = Math.max(1, stats.getMaxHealth() / 2);
    int halfAttack = Math.max(1, stats.getBaseAttack() / 2);

    // Hit reactions fire from collisions while the physics world is locked. A locked world can
    // neither create the children's bodies nor destroy the original's, so both are deferred.
    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              spawnChild(-0.5f, halfHealth, halfAttack);
              spawnChild(0.5f, halfHealth, halfAttack);
            });
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }

  /**
   * Creates one smaller chase enemy near the original entity's position.
   *
   * @param xOffset horizontal offset from the original entity's position
   * @param health health to assign to the new, smaller enemy
   * @param attack attack damage to assign to the new, smaller enemy
   */
  private void spawnChild(float xOffset, int health, int attack) {
    Entity child = NPCFactory.createChaseEnemy(target, false);

    CombatStatsComponent childStats = child.getComponent(CombatStatsComponent.class);
    if (childStats != null) {
      childStats.setMaxHealth(health);
      childStats.setHealth(health);
      childStats.setBaseAttack(attack);
    }

    child.setPosition(entity.getCenterPosition().x + xOffset, entity.getCenterPosition().y);
    entity.getEvents().trigger("spawnChildren", child);
  }
}
