package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.items.WeaponItem.WeaponType;
import com.csse3200.game.ui.terminal.commands.WeaponCommand;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeaponSelectionComponentTest {
  private Entity player;
  private WeaponSelectionComponent selection;
  private final List<WeaponType> changes = new ArrayList<>();

  @BeforeEach
  void setUp() {
    selection = new WeaponSelectionComponent();
    player =
        new Entity()
            .addComponent(spy(new SwordWeaponComponent()))
            .addComponent(spy(new KnifeWeaponComponent()))
            .addComponent(spy(new BowWeaponComponent()))
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 2f))
            .addComponent(selection);
    player.getEvents().addListener("weaponSelected", (EventListener1<WeaponType>) changes::add);
    selection.create();
  }

  @Test
  void startsWithSwordAndSwitchesExclusivelyThroughEvents() {
    assertEquipped(WeaponType.SWORD);
    for (WeaponType type : List.of(WeaponType.DAGGER, WeaponType.BOW, WeaponType.SWORD)) {
      player.getEvents().trigger("equipWeapon", type);
      assertEquipped(type);
    }
    assertEquals(
        List.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.BOW, WeaponType.SWORD), changes);
    var weapons = selection.getWeapons();
    assertThrows(UnsupportedOperationException.class, weapons::clear);
  }

  @Test
  void repeatedAndInvalidSelectionsPreserveStateAndDoNotEmitEvents() {
    assertTrue(selection.equip(WeaponType.SWORD));
    assertFalse(selection.equip(null));
    assertEquipped(WeaponType.SWORD);
    assertEquals(List.of(WeaponType.SWORD), changes);
  }

  @Test
  void missingWeaponDoesNotDisableCurrentWeapon() {
    WeaponSelectionComponent partial = new WeaponSelectionComponent();
    Entity wielder =
        new Entity().addComponent(spy(new SwordWeaponComponent())).addComponent(partial);
    partial.create();
    assertFalse(partial.equip(WeaponType.BOW));
    assertEquals(WeaponType.SWORD, partial.getSelectedWeapon());
    assertEnabled(wielder.getComponent(SwordWeaponComponent.class), true);
  }

  @Test
  void terminalUsesSharedSelectionAndPreservesCooldown() {
    WeaponStatsComponent stats = player.getComponent(WeaponStatsComponent.class);
    stats.triggerCooldown();
    WeaponCommand command = new WeaponCommand(player);
    for (String name : List.of("knife", "bow", "sword")) {
      assertTrue(command.action(new ArrayList<>(List.of(name))));
      assertEquals(0.5f, stats.getRemainingCooldown());
      assertFalse(stats.canAttack());
    }
    assertEquals(
        List.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.BOW, WeaponType.SWORD), changes);
    assertFalse(command.action(new ArrayList<>(List.of("unknown"))));
    assertFalse(command.action(new ArrayList<>()));
    assertEquipped(WeaponType.SWORD);
  }

  private void assertEquipped(WeaponType expected) {
    assertEquals(expected, selection.getSelectedWeapon());
    for (int i = 0; i < selection.getWeapons().size(); i++) {
      assertEnabled(
          player.getComponent(selection.getWeapons().get(i).type),
          WeaponSelectionComponent.SLOT_WEAPONS.get(i) == expected);
    }
  }

  private void assertEnabled(WeaponComponent weapon, boolean expected) {
    clearInvocations(weapon);
    weapon.triggerUpdate();
    verify(weapon, times(expected ? 1 : 0)).update();
  }
}
