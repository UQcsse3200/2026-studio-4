package com.csse3200.game.components.spells.targeting;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;

/**
 * Selects which enemy entities an ability should affect. Implementations determine targeting
 * specifics (i.e. all enemies, closest enemy, on-screen enemies) Keeps targeting separate from
 * ability effects
 */
public interface EnemyTargetingStrategy {
  /**
   * @param caster the entity casting the spell
   * @return enemies selected by this strategy; never null, may be empty
   */
  Array<Entity> selectTargets(Entity caster);
}
