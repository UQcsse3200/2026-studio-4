package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.GameTime;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class StatusEffectsFactoryTest {

  // ---------------------------------------------------------
  // Utility class construction guard
  // ---------------------------------------------------------

  @Test
  void constructorIsPrivate() throws NoSuchMethodException {
    Constructor<StatusEffectsFactory> constructor =
        StatusEffectsFactory.class.getDeclaredConstructor();

    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  void constructorThrowsIllegalStateException() throws NoSuchMethodException {
    Constructor<StatusEffectsFactory> constructor =
        StatusEffectsFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException wrapped =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, wrapped.getCause());
    assertEquals("Instantiating static utility class", wrapped.getCause().getMessage());
  }

  // ---------------------------------------------------------
  // createBurn()
  // ---------------------------------------------------------

  @Test
  void createBurnReturnsABurningInstance() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);

    StatusEffect effect = StatusEffectsFactory.createBurn(combatStats);

    assertNotNull(effect);
    assertInstanceOf(Burning.class, effect);
  }

  @Test
  void createBurnReturnsANewInstanceEachCall() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);

    StatusEffect first = StatusEffectsFactory.createBurn(combatStats);
    StatusEffect second = StatusEffectsFactory.createBurn(combatStats);

    assertNotSame(first, second);
  }

  // ---------------------------------------------------------
  // createShield()
  // ---------------------------------------------------------

  @Test
  void createShieldReturnsAShieldInstance() {
    Shield shield = StatusEffectsFactory.createShield();

    assertNotNull(shield);
  }

  @Test
  void createShieldReturnsANewInstanceEachCall() {
    Shield first = StatusEffectsFactory.createShield();
    Shield second = StatusEffectsFactory.createShield();

    assertNotSame(first, second);
  }

  // ---------------------------------------------------------
  // createRegeneration()
  // ---------------------------------------------------------

  @Test
  void createRegenerationReturnsARegenerationInstance() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);

    StatusEffect effect = StatusEffectsFactory.createRegeneration(combatStats);

    assertNotNull(effect);
    assertInstanceOf(Regeneration.class, effect);
  }

  // ---------------------------------------------------------
  // createInvisibility()
  // ---------------------------------------------------------

  @Test
  void createInvisibilityReturnsAnInvisibilityEffectInstance() {
    GameTime time = mock(GameTime.class);

    TimedStatusEffect effect = StatusEffectsFactory.createInvisibility(time, 5000L);

    assertNotNull(effect);
    assertInstanceOf(InvisibilityEffect.class, effect);
  }

  @Test
  void createInvisibilityReturnsANewInstanceEachCall() {
    GameTime time = mock(GameTime.class);

    TimedStatusEffect first = StatusEffectsFactory.createInvisibility(time, 5000L);
    TimedStatusEffect second = StatusEffectsFactory.createInvisibility(time, 5000L);

    assertNotSame(first, second);
  }

  // ---------------------------------------------------------
  // createLastStand()
  // ---------------------------------------------------------

  @Test
  void createLastStandReturnsALastStandEffectInstance() {
    GameTime time = mock(GameTime.class);

    TimedStatusEffect effect = StatusEffectsFactory.createLastStand(time, 8000L);

    assertNotNull(effect);
    assertInstanceOf(LastStandEffect.class, effect);
  }

  // ---------------------------------------------------------
  // createSlow()
  // ---------------------------------------------------------

  @Test
  void createSlowReturnsASlowInstance() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);

    StatusEffect effect = StatusEffectsFactory.createSlow(combatStats);

    assertNotNull(effect);
    assertInstanceOf(Slow.class, effect);
  }

  // ---------------------------------------------------------
  // createSpeed()
  // ---------------------------------------------------------

  @Test
  void createSpeedReturnsASpeedInstance() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);

    StatusEffect effect = StatusEffectsFactory.createSpeed(combatStats);

    assertNotNull(effect);
    assertInstanceOf(Speed.class, effect);
  }

  // ---------------------------------------------------------
  // createVulnerable()
  // ---------------------------------------------------------

  @Test
  void createVulnerableReturnsAVulnerableInstance() {
    StatusEffect effect = StatusEffectsFactory.createVulnerable();

    assertNotNull(effect);
    assertInstanceOf(Vulnerable.class, effect);
  }

  @Test
  void createVulnerableReturnsANewInstanceEachCall() {
    StatusEffect first = StatusEffectsFactory.createVulnerable();
    StatusEffect second = StatusEffectsFactory.createVulnerable();

    assertNotSame(first, second);
  }

  // ---------------------------------------------------------
  // createFreeze()
  // ---------------------------------------------------------

  @Test
  void createFreezeReturnsASlowInstance() {
    CombatStatsComponent combatStats = mock(CombatStatsComponent.class);
    when(combatStats.getMovementSpeed()).thenReturn(4f);

    StatusEffect effect = StatusEffectsFactory.createFreeze(combatStats);

    assertNotNull(effect);
    assertInstanceOf(Slow.class, effect);
  }
}
