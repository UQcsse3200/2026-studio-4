package com.csse3200.game.components.tutorial;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sets the player up for practice while they are in a training grounds room, and puts everything
 * back the way it was when they leave.
 *
 * <ul>
 *   <li>Every weapon counts as upgraded, so heavy attacks (K) can be tried without having bought
 *       the upgrade. Weapons that were already upgraded stay upgraded.
 *   <li>Supply stands lend consumables through {@link #lend(String)}. Whatever is still held above
 *       what the player walked in with is taken back on the way out, so practice supplies cannot be
 *       carried into a dungeon.
 * </ul>
 *
 * <p>The restore runs when the room is disposed, which is exactly when the player leaves it.
 */
public class TrainingLoadoutComponent extends Component {
  private static final List<Class<? extends WeaponComponent>> WEAPONS =
      List.of(SwordWeaponComponent.class, KnifeWeaponComponent.class, BowWeaponComponent.class);

  private final Set<String> lendableItems;
  private final Map<Class<? extends WeaponComponent>, Boolean> originalUpgrades = new HashMap<>();
  private final Map<String, Integer> originalCounts = new HashMap<>();
  private Entity player;
  private boolean applied;

  /**
   * @param lendableItems ids of the consumables the room's supply stands hand out
   */
  public TrainingLoadoutComponent(Set<String> lendableItems) {
    this.lendableItems = lendableItems;
  }

  @Override
  public void create() {
    entity.getEvents().<Entity>addListener("RoomCreated", this::apply);
  }

  private void apply(Entity player) {
    if (applied) {
      return;
    }
    applied = true;
    this.player = player;

    WeaponUpgradeComponent upgrades = player.getComponent(WeaponUpgradeComponent.class);
    if (upgrades != null) {
      for (Class<? extends WeaponComponent> weapon : WEAPONS) {
        originalUpgrades.put(weapon, upgrades.isUpgraded(weapon));
        upgrades.setUpgraded(weapon, true);
      }
    }
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    if (inventory != null) {
      for (String id : lendableItems) {
        originalCounts.put(id, inventory.getConsumableCount(id));
      }
    }
    entity
        .getEvents()
        .trigger(
            GameAreaDisplay.DISPLAY,
            "Training grounds: your weapons are enchanted, so heavy attacks (K) work here.");
  }

  /**
   * @param itemId a consumable the room lends
   * @return true if the room lends it and the player is not already holding a lent copy
   */
  public boolean canLend(String itemId) {
    InventoryComponent inventory = inventoryOrNull();
    return inventory != null
        && lendableItems.contains(itemId)
        && inventory.getConsumableCount(itemId) <= originalCounts.getOrDefault(itemId, 0);
  }

  /**
   * Gives the player one copy of a consumable, to be taken back when they leave.
   *
   * @param itemId a consumable the room lends
   * @return false if they are already holding a lent copy, or there is no player yet
   */
  public boolean lend(String itemId) {
    if (!canLend(itemId)) {
      return false;
    }
    inventoryOrNull().addConsumable(itemId);
    return true;
  }

  @Override
  public void dispose() {
    restore();
    super.dispose();
  }

  private void restore() {
    if (!applied || player == null) {
      return;
    }
    applied = false;

    WeaponUpgradeComponent upgrades = player.getComponent(WeaponUpgradeComponent.class);
    if (upgrades != null) {
      originalUpgrades.forEach(upgrades::setUpgraded);
    }
    InventoryComponent inventory = inventoryOrNull();
    if (inventory != null) {
      originalCounts.forEach(
          (id, original) -> {
            while (inventory.getConsumableCount(id) > original) {
              if (!inventory.removeConsumable(id)) {
                break;
              }
            }
          });
    }
    originalUpgrades.clear();
    originalCounts.clear();
  }

  private InventoryComponent inventoryOrNull() {
    return player == null ? null : player.getComponent(InventoryComponent.class);
  }
}
