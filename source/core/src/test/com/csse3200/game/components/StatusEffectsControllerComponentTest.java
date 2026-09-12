package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.components.statuseffects.Regeneration;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class StatusEffectsControllerComponentTest {
  private GameTime time;
  private StatusEffectsControllerComponent controller;
  private CombatStatsComponent stats;
  private PlayerAbilitiesComponent abilities;
  private Entity player;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    stats = new CombatStatsComponent(100, 10);
    controller = new StatusEffectsControllerComponent();
    abilities = new PlayerAbilitiesComponent(time);
    player = new Entity().addComponent(stats).addComponent(abilities).addComponent(controller);
    player.create();
  }

  @Test
  void shouldOwnActiveLifetimesAndAllowRemovalWithoutResettingCooldowns() {
    assertTrue(abilities.tryActivate(Invisibility.class));
    Invisibility effect = controller.getEffect(Invisibility.class);
    assertTrue(effect.isActive());
    assertEquals(15_000, effect.getRemainingDuration());
    List<String> ended = new ArrayList<>();
    EventListener1<String> recordEnded = ended::add;
    player.getEvents().addListener("abilityEnded", recordEnded);
    controller.removeEffect(Invisibility.class);
    controller.removeEffect(Invisibility.class);
    assertFalse(abilities.isActive(Invisibility.class));
    assertEquals(45_000, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(List.of("invisibility"), ended);
    when(time.getTime()).thenReturn(45_000L);
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertSame(effect, controller.getEffect(Invisibility.class));
    assertEquals(15_000, effect.getRemainingDuration());
  }

  @Test
  void shouldExpireBothBeforeReentrantCallbacksFromControllerQuery() {
    activateBoth();
    List<String> ended = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String name) -> {
              ended.add(name);
              assertFalse(controller.getEffect(Invisibility.class).isActive());
              assertFalse(controller.getEffect(LastStand.class).isActive());
              controller.refreshTimedEffects();
            });
    when(time.getTime()).thenReturn(15_000L);
    assertFalse(controller.getEffect(LastStand.class).isActive());
    controller.update();
    assertEquals(List.of("invisibility", "laststand"), ended);
  }

  @Test
  void shouldKeepReentrantRecastWhenFinishingOldExpirationBatch() {
    activateBoth();
    List<String> events = new ArrayList<>();
    player.getEvents().addListener("abilityUsed", (String name) -> events.add("used:" + name));
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String name) -> {
              events.add("ended:" + name);
              if (name.equals("invisibility")) {
                assertFalse(abilities.isActive(LastStand.class));
                assertTrue(abilities.tryActivate(Invisibility.class));
              }
            });
    when(time.getTime()).thenReturn(60_000L);
    controller.update();
    assertEquals(List.of("ended:invisibility", "used:invisibility", "ended:laststand"), events);
    assertTrue(abilities.isActive(Invisibility.class));
    assertEquals(15_000, abilities.getRemainingMs(Invisibility.class));
  }

  @Test
  void shouldResetBeforeCallbacksWhenControllerIsDisposedFirst() {
    activateBoth();
    List<String> ended = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    player
        .getEvents()
        .addListener("abilityFailed", (String name, String reason) -> failed.add(name));
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String name) -> {
              ended.add(name);
              assertFalse(abilities.isActive(Invisibility.class));
              assertFalse(abilities.isActive(LastStand.class));
              assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));
              assertEquals(0, abilities.getCooldownRemainingMs(LastStand.class));
              assertFalse(abilities.tryActivate(Invisibility.class));
            });
    controller.dispose();
    abilities.dispose();
    controller.dispose();
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertTrue(failed.isEmpty());
    assertNull(controller.getEffect(Invisibility.class));
  }

  @Test
  void shouldResetBeforeDeathCallbacksAndRequirePassiveReenabling() {
    activateBoth();
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String name) -> {
              assertFalse(abilities.isActive(Invisibility.class));
              assertFalse(abilities.isActive(LastStand.class));
              assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));
              assertEquals(0, abilities.getCooldownRemainingMs(LastStand.class));
            });
    stats.setHealth(0);
    stats.setHealth(19);
    stats.takeDamage(1, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertFalse(abilities.isActive(LastStand.class));
    assertTrue(abilities.tryActivate(Invisibility.class));
  }

  @Test
  void shouldNeverTickStacksDuringAbilityQueriesAndKeepFrameRemovalSemantics() {
    try (var burns = mockConstruction(Burning.class);
        var regens = mockConstruction(Regeneration.class)) {
      controller.addStatusEffect(2, 'b');
      controller.addStatusEffect(3, 'r');
      assertEquals(2, burns.constructed().size());
      assertEquals(3, regens.constructed().size());
      assertTrue(abilities.tryActivate(Invisibility.class));
      when(time.getTime()).thenReturn(15_000L);
      abilities.isActive(Invisibility.class);
      abilities.isActive(LastStand.class);
      abilities.getRemainingMs(Invisibility.class);
      abilities.getRemainingMs(LastStand.class);
      abilities.getCooldownRemainingMs(Invisibility.class);
      abilities.getCooldownRemainingMs(LastStand.class);
      abilities.update();
      burns.constructed().forEach(Mockito::verifyNoInteractions);
      regens.constructed().forEach(Mockito::verifyNoInteractions);
      when(burns.constructed().getFirst().update()).thenReturn(true);
      when(regens.constructed().getFirst().update()).thenReturn(true);
      controller.update();
      controller.update();
      verify(burns.constructed().getFirst()).update();
      verify(burns.constructed().getLast(), times(2)).update();
      verify(regens.constructed().getFirst()).update();
      verify(regens.constructed().get(1), times(2)).update();
      verify(regens.constructed().getLast(), times(2)).update();
    }
  }

  @Test
  void shouldRetainStackValidationAndRequireExplicitDependencies() {
    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(0, 'b'));
    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(-1, 'r'));
    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(1, '?'));
    Entity noStats = new Entity().addComponent(new StatusEffectsControllerComponent());
    assertThrows(IllegalStateException.class, noStats::create);
    Entity noController =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerAbilitiesComponent(time));
    assertThrows(IllegalStateException.class, noController::create);
    Invisibility duplicate = new Invisibility(time);
    assertThrows(IllegalStateException.class, () -> controller.registerEffect(duplicate));
  }

  @Test
  void shouldRegisterBeforeControllerCreateAndExpireAtExactDeadline() {
    StatusEffectsControllerComponent uncreated = new StatusEffectsControllerComponent();
    Runnable ended = mock(Runnable.class);
    Invisibility effect = new Invisibility(time);
    effect.setOnEnded(ended);
    uncreated.registerEffect(effect);
    new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(uncreated).create();
    effect.activate();
    when(time.getTime()).thenReturn(14_999L);
    assertTrue(uncreated.getEffect(Invisibility.class).isActive());
    when(time.getTime()).thenReturn(15_000L);
    uncreated.update();
    assertFalse(effect.isActive());
    verify(ended).run();
  }

  @Test
  void shouldEndStandaloneEffectsOnDeathWithoutAnAbilityComponent() {
    StatusEffectsControllerComponent standalone = new StatusEffectsControllerComponent();
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    new Entity().addComponent(combat).addComponent(standalone).create();
    Runnable ended = mock(Runnable.class);
    LastStand effect = new LastStand(time);
    effect.setOnEnded(ended);
    standalone.registerEffect(effect);
    effect.activate();
    combat.setHealth(0);
    assertFalse(effect.isActive());
    standalone.update();
    standalone.dispose();
    verify(ended).run();
  }

  @Test
  void shouldStopTickingWhenAStackCallbackDisposesController() {
    try (var burns = mockConstruction(Burning.class)) {
      controller.addStatusEffect(2, 'b');
      when(burns.constructed().getFirst().update())
          .thenAnswer(
              invocation -> {
                controller.dispose();
                return true;
              });
      assertDoesNotThrow(controller::update);
      controller.update();
      verify(burns.constructed().getFirst()).update();
      verifyNoInteractions(burns.constructed().getLast());
    }
  }

  private void activateBoth() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertTrue(abilities.tryActivate(Invisibility.class));
  }
}
