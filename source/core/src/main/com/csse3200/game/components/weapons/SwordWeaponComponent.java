package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;

/**
 * Slower sweeping attack. Spawns a short range radiant hitbox just in front of the wielder that follows them for
 * its brief lifetime.
 */
public class SwordWeaponComponent extends WeaponComponent {
    private static final Vector2 SIZE = new Vector2(1.0f, 1.5f);
    private static final float LIFETIME = 1.0f;
    private static final float REACH = 0.5f;


    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {

    }
}
