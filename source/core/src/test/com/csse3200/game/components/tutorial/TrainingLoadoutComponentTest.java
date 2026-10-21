package com.csse3200.game.components.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.gamearea.GameAreaDisplay;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TrainingLoadoutComponentTest {
  private static final String POTION = ItemIds.HEALTH_POTION;
  private static final String BOMB = ItemIds.FREEZE_BOMB;

  private final List<String> messages = new ArrayList<>();
  private WeaponUpgradeComponent upgrades;
  private InventoryComponent inventory;
  private Entity player;
  private Entity room;
  private TrainingLoadoutComponent loadout;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    upgrades = new WeaponUpgradeComponent();
    inventory = new InventoryComponent(0);
    player = new Entity().addComponent(upgrades).addComponent(inventory);
    player.create();

    loadout = new TrainingLoadoutComponent(Set.of(POTION, BOMB));
    room = new Entity().addComponent(loadout);
    room.create();
    room.getEvents().<String>addListener(GameAreaDisplay.DISPLAY, messages::add);
  }

  private void enterRoom() {
    room.getEvents().trigger("RoomCreated", player);
  }

  @Test
  void shouldUpgradeEveryWeaponOnEntry() {
    enterRoom();

    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertTrue(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertTrue(upgrades.isUpgraded(BowWeaponComponent.class));
  }

  @Test
  void shouldExplainTheEnchantmentOnEntry() {
    enterRoom();

    assertEquals(1, messages.size());
    assertTrue(messages.get(0).contains("heavy attacks"));
  }

  @Test
  void shouldTakeTheUpgradesBackOnLeaving() {
    enterRoom();

    room.dispose();

    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(BowWeaponComponent.class));
  }

  @Test
  void shouldKeepUpgradesThePlayerAlreadyBought() {
    upgrades.setUpgraded(SwordWeaponComponent.class, true);
    enterRoom();

    room.dispose();

    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class), "a bought upgrade is theirs");
    assertFalse(upgrades.isUpgraded(BowWeaponComponent.class));
  }

  @Test
  void shouldLendOneCopyAtATime() {
    enterRoom();

    assertTrue(loadout.lend(POTION));
    assertFalse(loadout.lend(POTION), "already holding a lent copy");
    assertEquals(1, inventory.getConsumableCount(POTION));
  }

  @Test
  void shouldLendAgainOnceTheLentCopyIsUsed() {
    enterRoom();
    loadout.lend(POTION);
    inventory.removeConsumable(POTION);

    assertTrue(loadout.lend(POTION));
  }

  @Test
  void shouldTakeLentItemsBackOnLeaving() {
    enterRoom();
    loadout.lend(POTION);
    loadout.lend(BOMB);

    room.dispose();

    assertEquals(0, inventory.getConsumableCount(POTION));
    assertEquals(0, inventory.getConsumableCount(BOMB));
  }

  @Test
  void shouldLeavePotionsThePlayerBroughtInAlone() {
    inventory.addConsumable(POTION, 2);
    enterRoom();
    assertTrue(loadout.lend(POTION), "they hold exactly what they walked in with");
    assertEquals(3, inventory.getConsumableCount(POTION));

    room.dispose();

    assertEquals(2, inventory.getConsumableCount(POTION));
  }

  @Test
  void shouldNeverTakeMoreThanWasLent() {
    inventory.addConsumable(POTION, 1);
    enterRoom();
    // They drank their own potion inside, so they end with fewer than they came in with.
    inventory.removeConsumable(POTION);

    room.dispose();

    assertEquals(0, inventory.getConsumableCount(POTION));
  }

  @Test
  void shouldOnlyLendWhatTheRoomOffers() {
    enterRoom();

    assertFalse(loadout.lend(ItemIds.SHIELD));
  }

  @Test
  void shouldNotLendBeforeThePlayerHasEntered() {
    assertFalse(loadout.canLend(POTION));
    assertFalse(loadout.lend(POTION));
  }

  @Test
  void shouldRestoreOnlyOnce() {
    enterRoom();
    room.dispose();
    inventory.addConsumable(POTION);

    loadout.dispose();

    assertEquals(1, inventory.getConsumableCount(POTION), "a second dispose must not touch it");
  }
}
