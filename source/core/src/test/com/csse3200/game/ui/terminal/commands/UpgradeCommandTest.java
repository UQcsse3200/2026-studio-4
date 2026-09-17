package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.weapons.BowWeaponComponent;
import com.csse3200.game.components.weapons.KnifeWeaponComponent;
import com.csse3200.game.components.weapons.SwordWeaponComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class UpgradeCommandTest {
  private WeaponUpgradeComponent upgrades;
  private UpgradeCommand command;

  @BeforeEach
  void setUp() {
    upgrades = new WeaponUpgradeComponent();
    Entity player = new Entity().addComponent(upgrades);
    player.create();
    command = new UpgradeCommand(player);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  @Test
  void shouldUpgradeSword() {
    assertTrue(command.action(args("sword")));
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void shouldStayUpgradedWhenRunTwice() {
    assertTrue(command.action(args("sword")));
    assertTrue(command.action(args("sword")));
    assertTrue(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void shouldRevertSwordUpgradeWithOff() {
    command.action(args("sword"));

    assertTrue(command.action(args("sword", "off")));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void shouldRejectInvalidArguments() {
    assertFalse(command.action(args()));
    assertFalse(command.action(args("axe")));
    assertFalse(command.action(args("sword", "on", "now")));
    assertFalse(command.action(args("sword", "maybe")));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));
  }

  @Test
  void shouldUpgradeAndRevertBow() {
    assertTrue(command.action(args("bow")));
    assertTrue(upgrades.isUpgraded(BowWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));

    assertTrue(command.action(args("bow", "off")));
    assertFalse(upgrades.isUpgraded(BowWeaponComponent.class));
  }

  @Test
  void shouldUpgradeAndRevertKnife() {
    assertTrue(command.action(args("knife")));
    assertTrue(upgrades.isUpgraded(KnifeWeaponComponent.class));
    assertFalse(upgrades.isUpgraded(SwordWeaponComponent.class));

    assertTrue(command.action(args("knife", "off")));
    assertFalse(upgrades.isUpgraded(KnifeWeaponComponent.class));
  }

  @Test
  void shouldFailWhenPlayerHasNoUpgradeComponent() {
    Entity player = new Entity();
    player.create();

    assertFalse(new UpgradeCommand(player).action(args("sword")));
  }
}
