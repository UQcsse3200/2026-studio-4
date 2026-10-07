package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponUpgradePurchaseTest {
  @Test
  void observersSeePaidUpgradeFromBothNotifications() {
    InventoryComponent inventory = new InventoryComponent(100);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity player = new Entity().addComponent(inventory).addComponent(upgrades);
    player.create();
    List<String> notifications = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "weaponUpgraded",
            (Class<?> weapon) -> {
              assertEquals(SwordWeaponComponent.class, weapon);
              assertEquals(40, inventory.getGold());
              assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
              notifications.add("upgrade");
            });
    player
        .getEvents()
        .addListener(
            "goldChanged",
            (Integer gold) -> {
              assertEquals(40, gold);
              assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
              notifications.add("gold");
            });
    assertEquals("SUCCESS", purchase(inventory, upgrades, SwordWeaponComponent.class, 60));
    assertEquals(List.of("upgrade", "gold"), notifications);
    assertEquals(1.2f, upgrades.getLightDamageMultiplier(SwordWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(KnifeWeaponComponent.class));
  }

  @Test
  void insufficientFundsDoNotUnlockHeavyAttack() {
    InventoryComponent inventory = new InventoryComponent(59);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    assertEquals("INSUFFICIENT_GOLD", purchase(inventory, upgrades, BowWeaponComponent.class, 60));
    assertEquals(59, inventory.getGold());
    assertFalse(upgrades.isUpgraded(BowWeaponComponent.class));
  }

  @Test
  void repeatedAndReentrantPurchasesChargeOnce() {
    InventoryComponent inventory = new InventoryComponent(200);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    Entity player = new Entity().addComponent(inventory).addComponent(upgrades);
    player.create();
    player
        .getEvents()
        .addListener(
            "weaponUpgraded",
            (Class<?> weapon) ->
                assertEquals(
                    "ALREADY_UPGRADED",
                    purchase(inventory, upgrades, KnifeWeaponComponent.class, 60)));
    assertEquals("SUCCESS", purchase(inventory, upgrades, KnifeWeaponComponent.class, 60));
    assertEquals("ALREADY_UPGRADED", purchase(inventory, upgrades, KnifeWeaponComponent.class, 60));
    assertEquals(140, inventory.getGold());
  }

  @Test
  void unsupportedUpgradeRestoresGoldWithoutNotifications() {
    InventoryComponent inventory = new InventoryComponent(100);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent(Map.of());
    Entity player = new Entity().addComponent(inventory).addComponent(upgrades);
    player.create();
    player
        .getEvents()
        .addListener("goldChanged", (Integer gold) -> fail("Rejected purchase notified"));
    player
        .getEvents()
        .addListener("weaponUpgraded", (Class<?> weapon) -> fail("Rejected upgrade notified"));
    assertEquals("INVALID_WEAPON", purchase(inventory, upgrades, SwordWeaponComponent.class, 60));
    assertEquals(100, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void invalidRequestsDoNotChargeGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    WeaponUpgradeComponent upgrades = new WeaponUpgradeComponent();
    assertEquals("INVALID_PRICE", purchase(inventory, upgrades, SwordWeaponComponent.class, 0));
    assertEquals("INVALID_PRICE", purchase(inventory, upgrades, SwordWeaponComponent.class, -1));
    assertEquals("INVALID_WEAPON", purchase(inventory, upgrades, null, 60));
    assertEquals("INVALID_WEAPON", purchase(inventory, null, SwordWeaponComponent.class, 60));
    assertEquals(100, inventory.getGold());
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  private String purchase(
      InventoryComponent inventory,
      WeaponUpgradeComponent upgrades,
      Class<? extends WeaponComponent> weapon,
      int price) {
    return inventory.tryPurchaseWeaponUpgrade(upgrades, weapon, price).toString();
  }
}
