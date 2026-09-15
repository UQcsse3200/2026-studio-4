package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Selects every enemy currently registered with the {@code EntityService}, regardless of position
 * or camera visibility. Suitable for a "hit everything in the level/room" spell as long as your
 * game keeps only the current room's entities registered at once &mdash; if multiple rooms' worth
 * of entities can be registered simultaneously, this will also hit enemies in other rooms (see the
 * class-level note in {@link com.csse3200.game.components.abilities.LightningSpellComponent}).
 */
public class StrategyTargetAll implements EnemyTargetingStrategy {
    @Override
    public Array<Entity> selectTargets(Entity caster) {
        Array<Entity> targets = new Array<>();
        for (Entity candidate : ServiceLocator.getEntityService().getEntities()) {
            if (!candidate.equals(caster) && EnemyUtils.isEnemy(candidate)) {
                targets.add(candidate);
            }
        }
        return targets;
    }
}