package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.statuseffects.Damageable;
import com.csse3200.game.components.statuseffects.Shield;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class StatusEffectsControllerComponentTest {

  @Mock private CombatStatsComponent combatStatsComponent;

  @Mock private Entity entity;

  @Mock private EventHandler eventHandler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(entity.getComponent(CombatStatsComponent.class)).thenReturn(combatStatsComponent);

    when(entity.getEvents()).thenReturn(eventHandler);
  }

  private StatusEffectsControllerComponent createController() {
    StatusEffectsControllerComponent controller = new StatusEffectsControllerComponent();

    controller.setEntity(entity);
    controller.create();

    return controller;
  }

  // ---------------------------------------------------------
  // create()
  // ---------------------------------------------------------

  @Test
  void createCachesCombatStatsComponent() {
    StatusEffectsControllerComponent controller = createController();

    assertNotNull(controller);

    verify(entity).getComponent(CombatStatsComponent.class);
  }

  @Test
  void createThrowsIfCombatStatsComponentMissing() {
    when(entity.getComponent(CombatStatsComponent.class)).thenReturn(null);

    StatusEffectsControllerComponent controller = new StatusEffectsControllerComponent();

    controller.setEntity(entity);

    IllegalStateException exception = assertThrows(IllegalStateException.class, controller::create);

    assertEquals(
        "StatusEffectsController requires CombatStatsComponent on the same entity.",
        exception.getMessage());
  }

  // ---------------------------------------------------------
  // addStatusEffect()
  // ---------------------------------------------------------

  @Test
  void addBurningStatusEffectCreatesCorrectNumberOfEffects() {
    StatusEffect burn = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(() -> StatusEffectsFactory.createBurn(combatStatsComponent)).thenReturn(burn);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(3, 'b');

      factory.verify(() -> StatusEffectsFactory.createBurn(combatStatsComponent), times(3));
    }
  }

  @Test
  void addRegenerationStatusEffectCreatesCorrectNumberOfEffects() {
    StatusEffect regeneration = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory
          .when(() -> StatusEffectsFactory.createRegeneration(combatStatsComponent))
          .thenReturn(regeneration);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(4, 'r');

      factory.verify(() -> StatusEffectsFactory.createRegeneration(combatStatsComponent), times(4));
    }
  }

  @Test
  void addSlowStatusEffectCreatesCorrectNumberOfEffects() {
    StatusEffect slow = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(() -> StatusEffectsFactory.createSlow(combatStatsComponent)).thenReturn(slow);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(2, 's');

      factory.verify(() -> StatusEffectsFactory.createSlow(combatStatsComponent), times(2));
    }
  }

  @Test
  void addSpeedStatusEffectCreatesCorrectNumberOfEffects() {
    StatusEffect speed = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(() -> StatusEffectsFactory.createSpeed(combatStatsComponent)).thenReturn(speed);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(5, 'S');

      factory.verify(() -> StatusEffectsFactory.createSpeed(combatStatsComponent), times(5));
    }
  }

  @Test
  void addVulnerableStatusEffectCreatesCorrectNumberOfEffects() {
    StatusEffect vulnerable = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createVulnerable).thenReturn(vulnerable);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(3, 'v');

      factory.verify(StatusEffectsFactory::createVulnerable, times(3));
    }
  }

  @Test
  void addFreezeStatusEffectCreatesOneFreeze() {
    StatusEffect freeze = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory
          .when(() -> StatusEffectsFactory.createFreeze(combatStatsComponent))
          .thenReturn(freeze);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(5, 'f');

      /*
       * Notice that the implementation currently only creates
       * ONE freeze regardless of the number of stacks.
       */
      factory.verify(() -> StatusEffectsFactory.createFreeze(combatStatsComponent), times(1));
    }
  }

  @Test
  void addStatusEffectRejectsZeroStacks() {
    StatusEffectsControllerComponent controller = createController();

    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(0, 'b'));
  }

  @Test
  void addStatusEffectRejectsNegativeStacks() {
    StatusEffectsControllerComponent controller = createController();

    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(-1, 'b'));
  }

  @Test
  void addStatusEffectRejectsInvalidCharacter() {
    StatusEffectsControllerComponent controller = createController();

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(1, 'x'));

    assertEquals(
        "statusEffect must be a valid character representation of a status effect.",
        exception.getMessage());
  }

  // ---------------------------------------------------------
  // Shield
  // ---------------------------------------------------------

  @Test
  void activateAbsorbCreatesShield() {
    Shield shield = mock(Shield.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createShield).thenReturn(shield);

      when(shield.getCurrent()).thenReturn(50);
      when(shield.getMax()).thenReturn(100);

      StatusEffectsControllerComponent controller = createController();

      controller.activateAbsorb();

      verify(shield).activateAbsorb();

      verify(eventHandler).trigger("updateShield", 50, 100);
    }
  }

  @Test
  void activateTimedCreatesShield() {
    Shield shield = mock(Shield.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createShield).thenReturn(shield);

      when(shield.getCurrent()).thenReturn(25);
      when(shield.getMax()).thenReturn(100);

      StatusEffectsControllerComponent controller = createController();

      controller.activateTimed();

      verify(shield).activateTimed();

      verify(eventHandler).trigger("updateShield", 25, 100);
    }
  }

  @Test
  void shieldIsOnlyCreatedOnce() {
    Shield shield = mock(Shield.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createShield).thenReturn(shield);

      when(shield.getCurrent()).thenReturn(50);
      when(shield.getMax()).thenReturn(100);

      StatusEffectsControllerComponent controller = createController();

      controller.activateAbsorb();
      controller.activateTimed();

      factory.verify(StatusEffectsFactory::createShield, times(1));

      verify(shield).activateAbsorb();
      verify(shield).activateTimed();
    }
  }

  @Test
  void modifyIncomingDamageUsesShield() {
    Shield shield = mock(Shield.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createShield).thenReturn(shield);

      when(shield.getCurrent()).thenReturn(30);
      when(shield.getMax()).thenReturn(100);
      when(shield.modifyIncomingDamage(80)).thenReturn(50);

      StatusEffectsControllerComponent controller = createController();

      controller.activateAbsorb();

      clearInvocations(eventHandler);

      int result = controller.modifyIncomingDamage(80);

      assertEquals(50, result);

      verify(shield).modifyIncomingDamage(80);

      verify(eventHandler).trigger("updateShield", 30, 100);
    }
  }

  // ---------------------------------------------------------
  // update()
  // ---------------------------------------------------------

  @Test
  void updateCallsUpdateOnAllStatusEffects() {
    StatusEffect effect = mock(StatusEffect.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(() -> StatusEffectsFactory.createBurn(combatStatsComponent)).thenReturn(effect);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(3, 'b');

      controller.update();

      verify(effect, times(3)).update();
    }
  }

  @Test
  void updateRemovesStatusEffectsThatReturnTrue() {
    StatusEffect removedEffect = mock(StatusEffect.class);
    StatusEffect remainingEffect = mock(StatusEffect.class);

    when(removedEffect.update()).thenReturn(true);
    when(remainingEffect.update()).thenReturn(false);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory
          .when(() -> StatusEffectsFactory.createBurn(combatStatsComponent))
          .thenReturn(removedEffect);

      factory
          .when(() -> StatusEffectsFactory.createRegeneration(combatStatsComponent))
          .thenReturn(remainingEffect);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(1, 'b');
      controller.addStatusEffect(1, 'r');

      controller.update();

      /*
       * Call update a second time.
       *
       * If removedEffect was successfully removed,
       * it should not receive another update.
       */
      controller.update();

      verify(removedEffect, times(1)).update();
      verify(remainingEffect, times(2)).update();
    }
  }

  @Test
  void updateTriggersShieldUiWhenShieldExists() {
    Shield shield = mock(Shield.class);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory.when(StatusEffectsFactory::createShield).thenReturn(shield);

      when(shield.getCurrent()).thenReturn(40);
      when(shield.getMax()).thenReturn(100);

      StatusEffectsControllerComponent controller = createController();

      controller.activateAbsorb();

      clearInvocations(eventHandler);

      controller.update();

      verify(eventHandler).trigger("updateShield", 40, 100);
    }
  }

  // ---------------------------------------------------------
  // damage()
  // ---------------------------------------------------------

  @Test
  void damageOnlyDamagesDamageableEffects() {
    StatusEffect normalEffect = mock(StatusEffect.class);

    StatusEffect damageableEffect =
        mock(StatusEffect.class, withSettings().extraInterfaces(Damageable.class));

    Damageable damageable = (Damageable) damageableEffect;

    Damage damage = mock(Damage.class);

    when(damageable.damage(damage)).thenReturn(false);

    try (MockedStatic<StatusEffectsFactory> factory = mockStatic(StatusEffectsFactory.class)) {

      factory
          .when(() -> StatusEffectsFactory.createBurn(combatStatsComponent))
          .thenReturn(normalEffect);

      factory
          .when(() -> StatusEffectsFactory.createRegeneration(combatStatsComponent))
          .thenReturn(damageableEffect);

      StatusEffectsControllerComponent controller = createController();

      controller.addStatusEffect(1, 'b');
      controller.addStatusEffect(1, 'r');

      controller.damage(damage);

      verify(damageable).damage(damage);
      verifyNoInteractions(normalEffect);
    }
  }
}
