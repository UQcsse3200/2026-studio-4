package com.csse3200.game.files;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.items.charms.*;

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

    for (ItemType type : ItemType.values()) {
      if (type.isConsumable()) {
        int count = inventory.getConsumableCount(type);
        if (count > 0) data.inventory.put(type.name(), count);
      }
    }

    for (Charm charm : inventory.getCharms()) {
      data.charms.add(charmId(charm));
    }

    WeaponUpgradeComponent upgrades = required(player, WeaponUpgradeComponent.class);
    if (upgrades.isUpgraded(SwordWeaponComponent.class)) data.upgradedWeapons.add("sword");
    if (upgrades.isUpgraded(KnifeWeaponComponent.class)) data.upgradedWeapons.add("knife");
    if (upgrades.isUpgraded(BowWeaponComponent.class)) data.upgradedWeapons.add("bow");

    WeaponSelectionComponent selection = required(player, WeaponSelectionComponent.class);
    if (selection.getSelectedWeapon() != null) {
      data.selectedWeapon = selection.getSelectedWeapon().name();
    }
    return save;
  }

  public static void restore(Entity player, GameSaveData save) {
    if (save == null || save.version != 1 || save.playerData == null) {
      throw new IllegalArgumentException("Invalid or unsupported save data");
    }

    InventoryComponent inventory = required(player, InventoryComponent.class);
    inventory.setGold(save.playerData.gold);

    if (save.playerData.inventory != null) {
      for (var entry : save.playerData.inventory.entrySet()) {
        ItemType type = ItemType.valueOf(entry.getKey());
        int count = entry.getValue() == null ? 0 : entry.getValue();
        if (!type.isConsumable() || count < 0) {
          throw new IllegalArgumentException("Invalid saved inventory item: " + entry.getKey());
        }
        inventory.addConsumable(type, count);
      }
    }

    if (save.playerData.charms != null) {
      for (String id : save.playerData.charms) {
        charmFromId(id).pickUp(player);
      }
    }
    WeaponSelectionComponent selection = required(player, WeaponSelectionComponent.class);

    if (save.playerData.selectedWeapon != null) {
      WeaponType selected = WeaponType.valueOf(save.playerData.selectedWeapon);
      if (!selection.equip(selected)) {
        throw new IllegalArgumentException(
            "Could not equip saved weapon: " + save.playerData.selectedWeapon);
      }
    }

    WeaponUpgradeComponent upgrades = required(player, WeaponUpgradeComponent.class);
    if (save.playerData.upgradedWeapons != null) {
      for (String id : save.playerData.upgradedWeapons) {
        switch (id) {
          case "sword" -> upgrades.setUpgraded(SwordWeaponComponent.class, true);
          case "knife" -> upgrades.setUpgraded(KnifeWeaponComponent.class, true);
          case "bow" -> upgrades.setUpgraded(BowWeaponComponent.class, true);
          default -> throw new IllegalArgumentException("Unknown saved weapon upgrade: " + id);
        }
      }
    }
    // Death restores the player alive at the checkpoint.
    required(player, CombatStatsComponent.class)
        .setHealth(required(player, CombatStatsComponent.class).getMaxHealth());
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
