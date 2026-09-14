package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponSwitchComponentTest {
  private WeaponSwitchComponent switcher;

  /** Player-like wielder carrying every weapon, with only the sword equipped. */
  private Entity createWielder() {
    switcher = new WeaponSwitchComponent();
    Entity wielder =
        new Entity()
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 0f))
            .addComponent(switcher)
            .addComponent(new SwordWeaponComponent())
            .addComponent(new KnifeWeaponComponent())
            .addComponent(new BowWeaponComponent());
    wielder.create();
    wielder.getComponent(KnifeWeaponComponent.class).setEnabled(false);
    wielder.getComponent(BowWeaponComponent.class).setEnabled(false);
    return wielder;
  }

  @Test
  void shouldStartWithTheSwordEquipped() {
    createWielder();
    assertEquals(SwordWeaponComponent.class, switcher.getEquipped());
  }

  @Test
  void shouldCycleSwordKnifeBowAndWrapAround() {
    Entity wielder = createWielder();

    wielder.getEvents().trigger("switchWeapon");
    assertEquals(KnifeWeaponComponent.class, switcher.getEquipped());

    wielder.getEvents().trigger("switchWeapon");
    assertEquals(BowWeaponComponent.class, switcher.getEquipped());

    wielder.getEvents().trigger("switchWeapon");
    assertEquals(SwordWeaponComponent.class, switcher.getEquipped());
  }

  @Test
  void shouldEquipExactlyOneWeapon() {
    Entity wielder = createWielder();

    switcher.switchToNext();

    assertFalse(wielder.getComponent(SwordWeaponComponent.class).isEnabled());
    assertTrue(wielder.getComponent(KnifeWeaponComponent.class).isEnabled());
    assertFalse(wielder.getComponent(BowWeaponComponent.class).isEnabled());
  }

  @Test
  void shouldContinueFromAWeaponEquippedElsewhere() {
    Entity wielder = createWielder();
    // e.g. the "weapon bow" terminal command enables weapons directly
    wielder.getComponent(SwordWeaponComponent.class).setEnabled(false);
    wielder.getComponent(BowWeaponComponent.class).setEnabled(true);

    assertEquals(SwordWeaponComponent.class, switcher.switchToNext());
  }

  @Test
  void shouldSkipWeaponsTheWielderDoesNotCarry() {
    switcher = new WeaponSwitchComponent();
    Entity wielder =
        new Entity()
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 0f))
            .addComponent(switcher)
            .addComponent(new SwordWeaponComponent())
            .addComponent(new BowWeaponComponent());
    wielder.create();
    wielder.getComponent(BowWeaponComponent.class).setEnabled(false);

    assertEquals(BowWeaponComponent.class, switcher.switchToNext());
    assertEquals(SwordWeaponComponent.class, switcher.switchToNext());
  }

  @Test
  void shouldNotEquipAWeaponTheWielderDoesNotCarry() {
    switcher = new WeaponSwitchComponent();
    Entity wielder =
        new Entity()
            .addComponent(new WeaponStatsComponent(0.5f, 1f, 0f))
            .addComponent(switcher)
            .addComponent(new SwordWeaponComponent());
    wielder.create();

    assertFalse(switcher.equip(KnifeWeaponComponent.class));
    assertEquals(SwordWeaponComponent.class, switcher.getEquipped());
  }

  @Test
  void shouldDoNothingWithoutWeapons() {
    switcher = new WeaponSwitchComponent();
    Entity wielder = new Entity().addComponent(switcher);
    wielder.create();

    assertNull(switcher.switchToNext());
    assertNull(switcher.getEquipped());
  }

  @Test
  void shouldTriggerWeaponSwitchedWithTheNewWeapon() {
    Entity wielder = createWielder();
    List<Class<?>> switched = new ArrayList<>();
    wielder.getEvents().addListener("weaponSwitched", (Class<?> weapon) -> switched.add(weapon));

    wielder.getEvents().trigger("switchWeapon");

    assertEquals(List.of(KnifeWeaponComponent.class), switched);
  }
}
