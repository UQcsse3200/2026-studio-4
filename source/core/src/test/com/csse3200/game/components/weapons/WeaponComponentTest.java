package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponComponentTest {
  @Test
  void shouldOnlyCreateAttackWhenOffCooldown() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 1f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.create();

    Vector2 origin = new Vector2(0f, 0f);
    Vector2 direction = new Vector2(1f, 0f);
    assertTrue(weapon.attack(origin, direction));
    assertFalse(weapon.attack(origin, direction));
    assertEquals(1, weapon.createAttackCalls);

    stats.update(0.5f);
    assertTrue(weapon.attack(origin, direction));
    assertEquals(2, weapon.createAttackCalls);
  }

  @Test
  void shouldResolveHitboxDamageFromWielderBaseAttack() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.5f, 0.8f, 0f);
    RecordingWeapon weapon = new RecordingWeapon();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(stats)
            .addComponent(weapon);
    wielder.create();

    assertEquals(8, weapon.resolveHitboxDamage());
  }

  private static class RecordingWeapon extends WeaponComponent {
    int createAttackCalls;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      createAttackCalls++;
    }
  }
}
