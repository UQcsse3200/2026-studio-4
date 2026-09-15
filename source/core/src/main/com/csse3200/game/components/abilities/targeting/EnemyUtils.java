package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/** Shared logic for identifying enemy entities, used across targeting strategies. */
public final class EnemyUtils {

    /**
     * @param candidate entity to check
     * @return true if the entity has a HitboxComponent on the NPC physics layer
     */
    public static boolean isEnemy(Entity candidate) {
        HitboxComponent hitbox = candidate.getComponent(HitboxComponent.class);
        if (hitbox == null) {
            return false;
        }
        Fixture fixture = hitbox.getFixture();
        if (fixture == null) {
            return false;
        }
        return PhysicsLayer.contains(PhysicsLayer.NPC, fixture.getFilterData().categoryBits);
    }

    private EnemyUtils() {
        throw new IllegalStateException("Instantiating static util class");
    }
}