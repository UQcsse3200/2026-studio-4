package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponUpgradeComponentTest {
  @Test
  void shouldNotBeUpgradedInitially() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();

    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertEquals(1f, upgrades.getLightDamageMultiplier(SwordWeaponComponent.class));
    assertEquals(1f, upgrades.getHeavyDamageMultiplier(SwordWeaponComponent.class));
    assertEquals(1f, upgrades.getHeavyCooldownMultiplier(SwordWeaponComponent.class));
  }

  @Test
  void shouldApplySwordUpgradeStats() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();

    assertTrue(upgrades.setUpgraded(SwordWeaponComponent.class, true));

    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertEquals(1.2f, upgrades.getLightDamageMultiplier(SwordWeaponComponent.class));
    assertEquals(1.35f, upgrades.getHeavyDamageMultiplier(SwordWeaponComponent.class));
    assertEquals(2f, upgrades.getHeavyCooldownMultiplier(SwordWeaponComponent.class));
  }

  @Test
  void shouldStayUpgradedWhenAppliedTwice() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();

    assertTrue(upgrades.setUpgraded(SwordWeaponComponent.class, true));
    assertTrue(upgrades.setUpgraded(SwordWeaponComponent.class, true));

    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void shouldRevertUpgrade() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    upgrades.setUpgraded(SwordWeaponComponent.class, true);

    assertTrue(upgrades.setUpgraded(SwordWeaponComponent.class, false));

    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertEquals(1f, upgrades.getLightDamageMultiplier(SwordWeaponComponent.class));
  }

  @Test
  void shouldRejectWeaponWithoutUpgradeStats() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();

    assertFalse(upgrades.setUpgraded(KnifeWeaponComponent.class, true));
    assertFalse(upgrades.isUpgraded(KnifeWeaponComponent.class));
  }

  @Test
  void shouldNotAffectOtherWeapons() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    upgrades.setUpgraded(SwordWeaponComponent.class, true);

    assertEquals(1f, upgrades.getLightDamageMultiplier(KnifeWeaponComponent.class));
    assertEquals(1f, upgrades.getLightDamageMultiplier(BowWeaponComponent.class));
  }

  @Test
  void shouldTriggerEventOnlyWhenUpgradeStateChanges() {
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity wielder = new Entity().addComponent(upgrades);
    wielder.create();
    int[] events = {0};
    wielder.getEvents().addListener("weaponUpgraded", (Class<?> weapon) -> events[0]++);

    upgrades.setUpgraded(SwordWeaponComponent.class, true);
    upgrades.setUpgraded(SwordWeaponComponent.class, true);
    assertEquals(1, events[0]);

    upgrades.setUpgraded(SwordWeaponComponent.class, false);
    assertEquals(2, events[0]);
  }

  @Test
  void shouldRejectNullStatsMap() {
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeComponent(null));
  }

  @Test
  void shouldRejectNegativeUpgradeStats() {
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeStats(-1f, 1f, 1f));
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeStats(1f, -1f, 1f));
    assertThrows(IllegalArgumentException.class, () -> new WeaponUpgradeStats(1f, 1f, -1f));
  }
}
