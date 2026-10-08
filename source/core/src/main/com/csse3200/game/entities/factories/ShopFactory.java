package com.csse3200.game.entities.factories;

import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ConsumablePurchaseEffect;
import com.csse3200.game.components.shop.ShopCatalog;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.components.shop.ShopInputComponent;
import com.csse3200.game.components.shop.ShopProductKind;
import com.csse3200.game.components.shop.ShopService;
import com.csse3200.game.components.shop.ShopSessionComponent;
import com.csse3200.game.components.shop.WeaponUpgradeCatalog;
import com.csse3200.game.components.shop.WeaponUpgradePurchaseEffect;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.commands.ShopCommand;
import java.util.Map;
import java.util.Objects;

/** Builds the merchant shop on an entity that remains active while gameplay is frozen. */
public final class ShopFactory {
  public static Entity createShop(Entity player) {
    InventoryComponent inventory =
        Objects.requireNonNull(player.getComponent(InventoryComponent.class));
    ShopCatalog catalog = ShopCatalog.load("configs/shops/merchant.json");
    ShopService service =
        new ShopService(
            inventory, catalog, Map.of(ShopProductKind.CONSUMABLE, new ConsumablePurchaseEffect()));
    WeaponUpgradeComponent upgrades = player.getComponent(WeaponUpgradeComponent.class);
    WeaponUpgradeCatalog upgradeCatalog =
        WeaponUpgradeCatalog.load("configs/shops/merchant-upgrades.json");
    ShopService upgradeService =
        new ShopService(
            inventory,
            upgradeCatalog.catalog(),
            Map.of(ShopProductKind.WEAPON_UPGRADE, new WeaponUpgradePurchaseEffect(upgrades)));
    ShopDisplay display =
        new ShopDisplay(inventory, catalog, service, upgradeCatalog, upgradeService, upgrades);
    ShopSessionComponent session = new ShopSessionComponent(player, display);
    Entity shop =
        new Entity()
            .addComponent(display)
            .addComponent(session)
            .addComponent(
                new ShopInputComponent(session, ServiceLocator.getRenderService().getStage()));
    shop.setUpdatesWhilePaused(true);
    return shop;
  }

  /** Registers Aarash's QA entry on the game's existing terminal and returns the same shop. */
  public static Entity createShop(Entity player, Terminal terminal) {
    Objects.requireNonNull(terminal);
    Entity shop = createShop(player);
    terminal.addCommand("shop", new ShopCommand(shop));
    return shop;
  }

  private ShopFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
