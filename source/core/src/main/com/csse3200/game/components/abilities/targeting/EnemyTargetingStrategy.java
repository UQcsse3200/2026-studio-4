package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;

/**
 * Selects which enemy entities a spell should affect. Implementations decide the "how" (all
 * enemies, closest enemy, on-screen enemies, etc.); the spell component decides the "what happens
 * to them" (damage, stun, ...). This separation lets new targeting modes be added without touching
 * any spell's effect logic, and lets the same strategy be reused across multiple spells.
 */
public interface EnemyTargetingStrategy {
    /**
     * @param caster the entity casting the spell
     * @return enemies selected by this strategy; never null, may be empty
     */
    Array<Entity> selectTargets(Entity caster);
}

