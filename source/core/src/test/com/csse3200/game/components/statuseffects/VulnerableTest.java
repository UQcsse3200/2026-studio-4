package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.Damage;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class VulnerableTest {
  @Test
  void shouldDoubleIncomingDamageWithoutRemovingItself() {
    Vulnerable vulnerable = new Vulnerable(5_000);
    Damage damage = new Damage(12, new Entity());

    assertFalse(vulnerable.damage(damage));
    assertEquals(24, damage.getDamage());
    assertTrue(vulnerable.getRemainingDuration() > 0);
  }

  @Test
  void shouldExpireAfterDuration() {
    Vulnerable vulnerable = new Vulnerable(20);
    waitUntil(vulnerable::update, vulnerable);
    assertTrue(vulnerable.isExpired() || vulnerable.getRemainingDuration() <= 0);
  }

  @Test
  void factoryVulnerableShouldBeDamageable() {
    StatusEffect effect = StatusEffectsFactory.createVulnerable();
    assertTrue(effect instanceof Damageable);
    Damage damage = new Damage(5, new Entity());
    assertFalse(((Damageable) effect).damage(damage));
    assertEquals(10, damage.getDamage());
  }

  private static void waitUntil(java.util.function.BooleanSupplier done, StatusEffect effect) {
    long deadline = System.currentTimeMillis() + 250;
    while (System.currentTimeMillis() < deadline && !done.getAsBoolean()) {
      effect.update();
    }
    assertTrue(done.getAsBoolean());
  }
}
