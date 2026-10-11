package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.utils.Json;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponSelectionComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GameSaveMapperTest {
  @Test
  void jsonRoundTripPreservesAchievementsAlongsideEquipmentAndStock() {
    Entity original = player();
    InventoryComponent inventory = original.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemIds.SHIELD, 2);
    inventory.equipConsumable(ItemIds.SHIELD, 3);
    StrengthCharm charm = new StrengthCharm();
    charm.pickUp(original);
    inventory.setCharmEquipped(charm, false);

    String earnedName = "earned achievement";
    String pendingName = "pending achievement";
    Achievement earned = new Achievement(earnedName, context -> false);
    Achievement pending = new Achievement(pendingName, context -> false);
    earned.restoreState(true, 7f);
    pending.restoreState(false, 3f);
    AchievementService achievements = new AchievementService();
    achievements.register(earned);
    achievements.register(pending);
    ServiceLocator.registerAchievementService(achievements);
    GameSaveData save = roundTrip(GameSaveMapper.capture(original, new GameSaveData.Checkpoint()));

    earned.restoreState(false, 0f);
    pending.restoreState(false, 0f);
    Entity restored = player();
    GameSaveMapper.restore(restored, save);
    GameSaveMapper.restoreAchievements(save);

    InventoryComponent restoredInventory = restored.getComponent(InventoryComponent.class);
    assertEquals(2, restoredInventory.getConsumableCount(ItemIds.SHIELD));
    assertEquals(ItemIds.SHIELD, restoredInventory.getConsumableSlot(3));
    assertNull(restoredInventory.getConsumableSlot(0));
    assertFalse(restoredInventory.getCharms().getFirst().isEquipped());
    assertEquals(List.of(earnedName), achievements.getUnlockedAchievementNames());
    assertEquals(7f, earned.getProgress());
    assertFalse(pending.isUnlocked());
    assertEquals(3f, pending.getProgress());
  }

  @Test
  void jsonRoundTripPreservesSlotOrderEmptyPositionsAndBackpackStock() {
    Entity original = player();
    InventoryComponent inventory = original.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemIds.HEALTH_POTION, 3);
    inventory.addConsumable(ItemIds.SHIELD, 2);
    inventory.addConsumable(ItemIds.SPEED_POTION, 4);
    inventory.addConsumable(ItemIds.STRENGTH_POTION, 5);
    inventory.addConsumable(ItemIds.FREEZE_BOMB, 6);
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      inventory.unequipConsumable(i);
    }
    inventory.equipConsumable(ItemIds.FREEZE_BOMB, 1);
    inventory.equipConsumable(ItemIds.HEALTH_POTION, 3);

    GameSaveData save = roundTrip(GameSaveMapper.capture(original, new GameSaveData.Checkpoint()));
    Entity restored = player();
    GameSaveMapper.restore(restored, save);
    InventoryComponent restoredInventory = restored.getComponent(InventoryComponent.class);

    assertEquals(
        Arrays.asList(null, ItemIds.FREEZE_BOMB, null, ItemIds.HEALTH_POTION),
        save.playerData.consumableSlots);
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      assertEquals(inventory.getConsumableSlot(i), restoredInventory.getConsumableSlot(i));
    }
    assertEquals(inventory.getConsumables(), restoredInventory.getConsumables());
  }

  @Test
  void jsonRoundTripPreservesIndividualDuplicateCharmStatesAndEffects() {
    Entity original = player();
    InventoryComponent inventory = original.getComponent(InventoryComponent.class);
    StrengthCharm first = new StrengthCharm();
    StrengthCharm stored = new StrengthCharm();
    StrengthCharm third = new StrengthCharm();
    first.pickUp(original);
    stored.pickUp(original);
    third.pickUp(original);
    inventory.setCharmEquipped(stored, false);

    GameSaveData save = roundTrip(GameSaveMapper.capture(original, new GameSaveData.Checkpoint()));
    Entity restored = player();
    GameSaveMapper.restore(restored, save);
    InventoryComponent restoredInventory = restored.getComponent(InventoryComponent.class);

    assertEquals(List.of("strength", "strength", "strength"), save.playerData.charms);
    assertEquals(List.of(true, false, true), save.playerData.charmEquipped);
    assertEquals(3, restoredInventory.getCharmCount());
    assertTrue(restoredInventory.getCharms().get(0).isEquipped());
    assertFalse(restoredInventory.getCharms().get(1).isEquipped());
    assertTrue(restoredInventory.getCharms().get(2).isEquipped());
    assertEquals(30, restored.getComponent(CombatStatsComponent.class).getBaseAttack());
    restoredInventory.setCharmEquipped(restoredInventory.getCharms().get(1), true);
    assertEquals(40, restored.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void legacyJsonWithoutEquipmentFieldsKeepsOriginalRestoration() {
    GameSaveData legacy = new GameSaveData();
    legacy.playerData.gold = 23;
    legacy.playerData.inventory.put(ItemIds.SHIELD, 2);
    legacy.playerData.charms.addAll(List.of("strength", "strength"));
    // FileLoader's writer includes the concrete HashMap type required by LibGDX Json.
    // Null equipment fields are omitted, giving the format written before these fields existed.
    String legacyJson = FileLoader.json.prettyPrint(legacy);
    assertFalse(legacyJson.contains("consumableSlots"));
    assertFalse(legacyJson.contains("charmEquipped"));
    GameSaveData save = FileLoader.json.fromJson(GameSaveData.class, legacyJson);
    assertNull(save.playerData.consumableSlots);
    assertNull(save.playerData.charmEquipped);

    Entity restored = player();
    GameSaveMapper.restore(restored, save);
    InventoryComponent inventory = restored.getComponent(InventoryComponent.class);
    assertEquals(23, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.SHIELD));
    assertEquals(ItemIds.SHIELD, inventory.getConsumableSlot(0));
    assertEquals(2, inventory.getCharmCount());
    assertTrue(inventory.getCharms().stream().allMatch(Charm::isEquipped));
    assertEquals(30, restored.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void explicitEmptySlotsAndUnequippedCharmsStayEmptyAndUnequipped() {
    Entity original = player();
    InventoryComponent inventory = original.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemIds.SHIELD, 2);
    inventory.unequipConsumable(0);
    StrengthCharm charm = new StrengthCharm();
    charm.pickUp(original);
    inventory.setCharmEquipped(charm, false);
    Entity restored = player();
    GameSaveMapper.restore(
        restored, roundTrip(GameSaveMapper.capture(original, new GameSaveData.Checkpoint())));

    InventoryComponent restoredInventory = restored.getComponent(InventoryComponent.class);
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      assertNull(restoredInventory.getConsumableSlot(i));
    }
    assertEquals(2, restoredInventory.getConsumableCount(ItemIds.SHIELD));
    assertFalse(restoredInventory.getCharms().get(0).isEquipped());
    assertEquals(10, restored.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void invalidSlotAssignmentsAreRejectedBeforeChangingThePlayer() {
    GameSaveData save = new GameSaveData();
    save.playerData.gold = 42;
    save.playerData.inventory.put(ItemIds.SHIELD, 1);
    save.playerData.consumableSlots = Arrays.asList(ItemIds.SHIELD, ItemIds.SHIELD, null, null);
    Entity restored = player();
    assertThrows(IllegalArgumentException.class, () -> GameSaveMapper.restore(restored, save));
    assertEquals(0, restored.getComponent(InventoryComponent.class).getGold());

    save.playerData.consumableSlots = Arrays.asList(ItemIds.FREEZE_BOMB, null, null, null);
    assertThrows(IllegalArgumentException.class, () -> GameSaveMapper.restore(restored, save));
    save.playerData.consumableSlots = List.of(ItemIds.SHIELD);
    assertThrows(IllegalArgumentException.class, () -> GameSaveMapper.restore(restored, save));
  }

  @Test
  void charmStateMustMatchEveryOwnedOccurrence() {
    GameSaveData save = new GameSaveData();
    save.playerData.charms = List.of("strength", "strength");
    save.playerData.charmEquipped = List.of(false);
    Entity restored = player();
    assertThrows(IllegalArgumentException.class, () -> GameSaveMapper.restore(restored, save));
    save.playerData.charmEquipped = Arrays.asList(true, null);
    Entity restoredWithNullState = player();
    assertThrows(
        IllegalArgumentException.class, () -> GameSaveMapper.restore(restoredWithNullState, save));
  }

  private static Entity player() {
    return new Entity()
        .addComponent(new InventoryComponent(0))
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new WeaponUpgradeComponent())
        .addComponent(new SwordWeaponComponent())
        .addComponent(new WeaponSelectionComponent());
  }

  private static GameSaveData roundTrip(GameSaveData save) {
    Json json = new Json();
    return json.fromJson(GameSaveData.class, json.toJson(save));
  }
}
