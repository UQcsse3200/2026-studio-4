package com.csse3200.game.files;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.items.charms.*;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GameSaveMapper {
  private GameSaveMapper() {}

  public static GameSaveData capture(Entity player, GameSaveData.Checkpoint checkpoint) {
    return capture(player, checkpoint, null);
  }

  public static GameSaveData capture(
      Entity player,
      GameSaveData.Checkpoint checkpoint,
      GameSaveData.ResumePosition resumePosition) {
    GameSaveData save = new GameSaveData();
    save.checkpoint = copyCheckpoint(checkpoint);
    save.resumePosition = copyResumePosition(resumePosition);

    InventoryComponent inventory = required(player, InventoryComponent.class);
    GameSaveData.PlayerData data = save.playerData;
    data.gold = inventory.getGold();
    data.inventory.putAll(inventory.getConsumables());
    data.consumableSlots = new ArrayList<>();
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      data.consumableSlots.add(inventory.getConsumableSlot(i));
    }

    data.charmEquipped = new ArrayList<>();
    for (Charm charm : inventory.getCharms()) {
      data.charms.add(charmId(charm));
      data.charmEquipped.add(charm.isEquipped());
    }

    WeaponUpgradeComponent upgrades = required(player, WeaponUpgradeComponent.class);
    if (upgrades.isUpgraded(SwordWeaponComponent.class)) data.upgradedWeapons.add("sword");
    if (upgrades.isUpgraded(KnifeWeaponComponent.class)) data.upgradedWeapons.add("knife");
    if (upgrades.isUpgraded(BowWeaponComponent.class)) data.upgradedWeapons.add("bow");

    WeaponSelectionComponent selection = required(player, WeaponSelectionComponent.class);
    if (selection.getSelectedWeapon() != null) {
      data.selectedWeapon = selection.getSelectedWeapon().name();
    }

    AchievementService achievementService = ServiceLocator.getAchievementService();

    if (achievementService != null) {
      save.unlockedAchievements.addAll(achievementService.getUnlockedAchievementNames());

      save.achievementProgress.putAll(achievementService.getAchievementProgress());
    }

    return save;
  }

  public static void restore(Entity player, GameSaveData save) {
    validateSave(save);

    GameSaveData.PlayerData data = save.playerData;
    InventoryComponent inventory = required(player, InventoryComponent.class);

    inventory.setGold(data.gold);
    restoreConsumables(data.inventory, inventory);
    restoreConsumableSlots(data.consumableSlots, inventory);
    restoreCharms(player, data.charms, data.charmEquipped);
    restoreSelectedWeapon(player, data.selectedWeapon);
    restoreUpgrades(player, data.upgradedWeapons);
    restorePlayerHealth(player);
  }

  private static void validateSave(GameSaveData save) {
    if (save == null || save.version != 1 || save.playerData == null) {
      throw new IllegalArgumentException("Invalid or unsupported save data");
    }
    GameSaveData.PlayerData data = save.playerData;
    validateConsumableSlots(data);
    validateCharmEquipment(data);
  }

  private static void validateConsumableSlots(GameSaveData.PlayerData data) {
    if (data.consumableSlots == null) return;
    if (data.consumableSlots.size() != InventoryComponent.CONSUMABLE_SLOT_COUNT) {
      throw new IllegalArgumentException("Saved consumable slots must contain four positions");
    }
    Set<String> assigned = new HashSet<>();
    for (String id : data.consumableSlots) {
      if (id == null) continue;
      Integer count = data.inventory == null ? null : data.inventory.get(id);
      if (count == null
          || count <= 0
          || !ItemCatalog.contains(id)
          || !(ItemCatalog.create(id, 1) instanceof ConsumableItem)
          || !assigned.add(id)) {
        throw new IllegalArgumentException("Invalid saved consumable slot: " + id);
      }
    }
  }

  private static void validateCharmEquipment(GameSaveData.PlayerData data) {
    if (data.charmEquipped != null
        && (data.charms == null
            || data.charmEquipped.size() != data.charms.size()
            || data.charmEquipped.stream().anyMatch(Objects::isNull))) {
      throw new IllegalArgumentException("Saved charm equipment must match each owned charm");
    }
  }

  private static void restoreConsumables(
      Map<String, Integer> savedInventory, InventoryComponent inventory) {
    if (savedInventory == null) {
      return;
    }

    for (var entry : savedInventory.entrySet()) {
      String id = entry.getKey();
      Integer count = entry.getValue();

      if (count == null
          || count < 0
          || !ItemCatalog.contains(id)
          || !(ItemCatalog.create(id, 1) instanceof ConsumableItem)) {
        throw new IllegalArgumentException("Invalid saved inventory item: " + id);
      }

      if (count > 0) {
        inventory.addConsumable(id, count);
      }
    }
  }

  private static void restoreConsumableSlots(
      List<String> savedSlots, InventoryComponent inventory) {
    if (savedSlots == null) return;
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      inventory.unequipConsumable(i);
    }
    for (int i = 0; i < savedSlots.size(); i++) {
      String id = savedSlots.get(i);
      if (id != null) inventory.equipConsumable(id, i);
    }
  }

  private static void restoreCharms(
      Entity player, List<String> savedCharms, List<Boolean> savedEquipment) {
    if (savedCharms == null) {
      return;
    }

    InventoryComponent inventory = required(player, InventoryComponent.class);
    for (int i = 0; i < savedCharms.size(); i++) {
      Charm charm = charmFromId(savedCharms.get(i));
      if (savedEquipment == null) {
        charm.pickUp(player);
      } else {
        inventory.addCharm(charm);
        inventory.setCharmEquipped(charm, savedEquipment.get(i));
      }
    }
  }

  private static void restoreSelectedWeapon(Entity player, String savedWeapon) {
    if (savedWeapon == null) {
      return;
    }

    WeaponSelectionComponent selection = required(player, WeaponSelectionComponent.class);
    WeaponType selected = WeaponType.valueOf(savedWeapon);
    if (!selection.equip(selected)) {
      throw new IllegalArgumentException("Could not equip saved weapon: " + savedWeapon);
    }
  }

  private static void restoreUpgrades(Entity player, List<String> savedUpgrades) {
    if (savedUpgrades == null) {
      return;
    }

    WeaponUpgradeComponent upgrades = required(player, WeaponUpgradeComponent.class);
    for (String id : savedUpgrades) {
      switch (id) {
        case "sword" -> upgrades.setUpgraded(SwordWeaponComponent.class, true);
        case "knife" -> upgrades.setUpgraded(KnifeWeaponComponent.class, true);
        case "bow" -> upgrades.setUpgraded(BowWeaponComponent.class, true);
        default -> throw new IllegalArgumentException("Unknown saved weapon upgrade: " + id);
      }
    }
  }

  private static void restorePlayerHealth(Entity player) {
    CombatStatsComponent stats = required(player, CombatStatsComponent.class);
    // Death restores the player alive at the checkpoint.
    stats.setHealth(stats.getMaxHealth());
  }

  public static void restoreAchievements(GameSaveData save) {
    if (save == null) {
      return;
    }

    AchievementService achievementService = ServiceLocator.getAchievementService();

    if (achievementService != null) {
      achievementService.restoreState(save.unlockedAchievements, save.achievementProgress);
    }
  }

  private static String charmId(Charm charm) {
    if (charm instanceof StrengthCharm) return "strength";
    if (charm instanceof AttackSpeedCharm) return "attack_speed";
    if (charm instanceof SpeedCharm) return "speed";
    throw new IllegalArgumentException("Unsupported charm: " + charm.getClass().getName());
  }

  private static Charm charmFromId(String id) {
    return switch (id) {
      case "strength" -> new StrengthCharm();
      case "attack_speed" -> new AttackSpeedCharm();
      case "speed" -> new SpeedCharm();
      default -> throw new IllegalArgumentException("Unknown saved charm: " + id);
    };
  }

  private static GameSaveData.Checkpoint copyCheckpoint(GameSaveData.Checkpoint source) {
    if (source == null) throw new IllegalArgumentException("Checkpoint is required");
    GameSaveData.Checkpoint copy = new GameSaveData.Checkpoint();
    copy.roomId = source.roomId;
    copy.entryPointId = source.entryPointId;
    copy.tileX = source.tileX;
    copy.tileY = source.tileY;
    return copy;
  }

  private static GameSaveData.ResumePosition copyResumePosition(
      GameSaveData.ResumePosition source) {
    if (source == null) return null;
    GameSaveData.ResumePosition copy = new GameSaveData.ResumePosition();
    copy.roomId = source.roomId;
    copy.x = source.x;
    copy.y = source.y;
    return copy;
  }

  private static <T extends Component> T required(Entity entity, Class<T> type) {
    T component = entity.getComponent(type);
    if (component == null) {
      throw new IllegalStateException("Player is missing " + type.getSimpleName());
    }
    return component;
  }
}
