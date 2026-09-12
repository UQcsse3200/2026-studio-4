package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponComponentTest {
  @Test
  void shouldUseLiveLastStandDamageAndCooldownWithoutChangingWeaponOrRawStats() {
    GameTime time = mock(GameTime.class);
    PlayerAbilitiesComponent abilities = new PlayerAbilitiesComponent(time);
    CombatStatsComponent combat = new CombatStatsComponent(100, 11, 3f, 2f);
    WeaponStatsComponent stats = new WeaponStatsComponent(0.6f, 0.5f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities)
            .addComponent(stats)
            .addComponent(weapon);
    wielder.create();
    abilities.unlock(LastStand.class);
    Vector2 direction = new Vector2(1f, 0f);
    wielder.getEvents().trigger("weaponAttack", direction);
    assertEquals(6, weapon.lastDamage);
    assertEquals(0.3f, stats.getRemainingCooldown(), 1e-6f);

    combat.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertFalse(weapon.attack(Vector2.Zero, direction));
    assertEquals(0.3f, stats.getRemainingCooldown(), 1e-6f);
    stats.update(0.3f);
    wielder.getEvents().trigger("weaponAttack", direction);
    assertEquals(9, weapon.lastDamage); // round(round(11 * 1.5) * 0.5)
    assertEquals(0.2f, stats.getRemainingCooldown(), 1e-6f);

    combat.addBaseAttack(2);
    combat.addAttackSpeed(2f);
    stats.update(0.2f);
    assertTrue(weapon.attack(Vector2.Zero, direction));
    assertEquals(10, weapon.lastDamage);
    assertEquals(0.1f, stats.getRemainingCooldown(), 1e-6f);
    when(time.getTime()).thenReturn(LastStand.DURATION_MS);
    stats.update(0.1f);
    assertTrue(weapon.attack(Vector2.Zero, direction));
    assertEquals(7, weapon.lastDamage);
    assertEquals(0.15f, stats.getRemainingCooldown(), 1e-6f);
    assertEquals(13, combat.getBaseAttack());
    assertEquals(4f, combat.getAttackSpeed());
    assertEquals(3f, combat.getMovementSpeed());
    assertEquals(4, weapon.createAttackCalls);
  }

  @Test
  void shouldCallCreateAttackWhenReady() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 origin = new Vector2(1f, 2f);
    Vector2 direction = new Vector2(1f, 0f);
    assertTrue(weapon.attack(origin, direction));
    assertEquals(1, weapon.createAttackCalls);
    assertEquals(origin, weapon.lastOrigin);
    assertEquals(direction, weapon.lastDirection);
  }

  @Test
  void shouldNotCreateAttackWhileCoolingDown() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 origin = new Vector2(0f, 0f);
    Vector2 direction = new Vector2(1f, 0f);
    assertTrue(weapon.attack(origin, direction));
    assertFalse(weapon.attack(origin, direction));
    assertEquals(1, weapon.createAttackCalls);
  }

  @Test
  void shouldCreateAttackAgainAfterCooldown() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 origin = new Vector2(0f, 0f);
    Vector2 direction = new Vector2(1f, 0f);
    weapon.attack(origin, direction);
    stats.update(0.5f);
    assertTrue(weapon.attack(origin, direction));
    assertEquals(2, weapon.createAttackCalls);
  }

  @Test
  void shouldFailCreateWithoutStats() {
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(weapon);
    assertThrows(IllegalStateException.class, wielder::create);
  }

  @Test
  void shouldRejectNullOriginOrDirection() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 direction = new Vector2(1f, 0f);
    Vector2 origin = new Vector2(0f, 0f);
    assertThrows(IllegalArgumentException.class, () -> weapon.attack(null, direction));
    assertThrows(IllegalArgumentException.class, () -> weapon.attack(origin, null));
  }

  @Test
  void shouldScaleHitboxDamageByWielderBaseAttack() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 0.8f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(stats)
            .addComponent(weapon);
    wielder.create();

    assertEquals(8, weapon.resolveHitboxDamage()); // round(10 * 0.8)
  }

  @Test
  void shouldFollowBaseAttackBuffs() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    Entity wielder = new Entity().addComponent(combat).addComponent(stats).addComponent(weapon);
    wielder.create();

    assertEquals(10, weapon.resolveHitboxDamage());
    combat.setBaseAttack(20); // e.g. Strength Charm picked up
    assertEquals(20, weapon.resolveHitboxDamage());
  }

  @Test
  void shouldScaleCooldownByWielderAttackSpeed() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10, 3f, 2f)) // attack speed 2x
            .addComponent(stats)
            .addComponent(weapon);
    wielder.create();

    Vector2 origin = new Vector2(0f, 0f);
    Vector2 direction = new Vector2(1f, 0f);
    assertTrue(weapon.attack(origin, direction));
    assertEquals(0.25f, stats.getRemainingCooldown(), 1e-4f); // 0.5s cooldown halved

    stats.update(0.25f);
    assertTrue(weapon.attack(origin, direction));
    assertEquals(2, weapon.createAttackCalls);
  }

  @Test
  void shouldUseBaseCooldownWithoutCombatStats() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    assertTrue(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertEquals(0.5f, stats.getRemainingCooldown(), 1e-4f);
  }

  @Test
  void shouldResolveZeroDamageWithoutCombatStats() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    assertEquals(0, weapon.resolveHitboxDamage());
  }

  private static class RecordingWeapon extends WeaponComponent {
    int createAttackCalls;
    Vector2 lastOrigin;
    Vector2 lastDirection;
    int lastDamage;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      createAttackCalls++;
      lastOrigin = origin;
      lastDirection = direction;
      lastDamage = resolveHitboxDamage();
    }
  }
}
