package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponComponentTest {
  @Test
  void shouldCallCreateAttackWhenReady() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 10, 0f);
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
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 10, 0f);
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
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 10, 0f);
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
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 10, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 direction = new Vector2(1f, 0f);
    Vector2 origin = new Vector2(0f, 0f);
    assertThrows(IllegalArgumentException.class, () -> weapon.attack(null, direction));
    assertThrows(IllegalArgumentException.class, () -> weapon.attack(origin, null));
  }

  private static class RecordingWeapon extends WeaponComponent {
    int createAttackCalls;
    Vector2 lastOrigin;
    Vector2 lastDirection;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      createAttackCalls++;
      lastOrigin = origin;
      lastDirection = direction;
    }
  }
}
