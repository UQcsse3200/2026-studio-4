/**
 * Shared weapon foundation used by knife, sword, and bow implementations.
 *
 * <p>Attach a {@link com.csse3200.game.components.weapons.WeaponStatsComponent} and a subclass of
 * {@link com.csse3200.game.components.weapons.WeaponComponent} to a wielder. Hitbox damage is
 * {@code round(wielder.baseAttack * weapon.multiplier)}; subclasses should call {@link
 * com.csse3200.game.components.weapons.WeaponComponent#resolveHitboxDamage()} when filling a spec.
 * Melee {@code createAttack} implementations should pass the wielder as the hitbox owner so the
 * sensor follows movement. Bow splash should omit the owner so the hitbox stays in world space.
 */
package com.csse3200.game.components.weapons;
