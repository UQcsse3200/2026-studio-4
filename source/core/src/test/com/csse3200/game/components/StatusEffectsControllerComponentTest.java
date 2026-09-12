package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.Invisibility;
import com.csse3200.game.components.statuseffects.LastStand;
import com.csse3200.game.components.statuseffects.Regeneration;
import com.csse3200.game.components.statuseffects.TimedEffect;
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
  void shouldOwnActiveLifetimesAndAllowStoppingWithoutResettingCooldowns() {
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertTrue(abilities.isActive(Invisibility.class));
    assertEquals(15_000, abilities.getRemainingMs(Invisibility.class));
    List<String> ended = new ArrayList<>();
    EventListener1<String> recordEnded = ended::add;
    player.getEvents().addListener("abilityEnded", recordEnded);
    abilities.stop(Invisibility.class);
    abilities.stop(Invisibility.class);
    assertFalse(abilities.isActive(Invisibility.class));
    assertEquals(45_000, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(List.of("invisibility"), ended);
    when(time.getTime()).thenReturn(45_000L);
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertEquals(15_000, abilities.getRemainingMs(Invisibility.class));
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
              assertFalse(abilities.isActive(Invisibility.class));
              assertFalse(abilities.isActive(LastStand.class));
              controller.refreshTimedEffects();
            });
    when(time.getTime()).thenReturn(15_000L);
    assertFalse(abilities.isActive(LastStand.class));
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
    // A disposed controller keeps nothing and takes nothing new.
    TimedEffect rejected = new FakeTimedEffect();
    assertThrows(IllegalStateException.class, () -> controller.registerEffect(rejected));
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
    TimedEffect duplicate = new FakeTimedEffect();
    controller.registerEffect(duplicate);
    assertThrows(IllegalStateException.class, () -> controller.registerEffect(duplicate));
  }

  @Test
  void shouldRegisterBeforeControllerCreateAndExpireAtExactDeadline() {
    StatusEffectsControllerComponent uncreated = new StatusEffectsControllerComponent();
    Runnable ended = mock(Runnable.class);
    FakeTimedEffect effect = new FakeTimedEffect();
    effect.onEnded = ended;
    uncreated.registerEffect(effect);
    new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(uncreated).create();
    effect.active = true;
    uncreated.refreshTimedEffects();
    assertTrue(effect.isActive());
    effect.expired = true;
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
    FakeTimedEffect effect = new FakeTimedEffect();
    effect.onEnded = ended;
    standalone.registerEffect(effect);
    effect.active = true;
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

  /** Proves the controller drives anything implementing the contract, not one concrete class. */
  private static final class FakeTimedEffect implements TimedEffect {
    private boolean active;
    private boolean expired;
    private Runnable onEnded;

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public boolean update() {
      return active && expired;
    }

    @Override
    public long getRemainingDuration() {
      return active && !expired ? 1 : 0;
    }

    @Override
    public void clear() {
      active = false;
    }

    @Override
    public void notifyEnded() {
      if (onEnded != null) {
        onEnded.run();
      }
    }
  }

  private void activateBoth() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertTrue(abilities.tryActivate(Invisibility.class));
  }
}
