package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.components.statuseffects.LastStandEffect;
import com.csse3200.game.components.statuseffects.Regeneration;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
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
  void shouldAnswerQueriesAtTheExactDeadlineButRemoveAndNotifyOnlyOnUpdate() {
    activateBoth();
    List<String> ended = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String name) -> {
              ended.add(name);
              // Every expired effect is off the list before the first callback runs.
              assertFalse(abilities.isActive(Invisibility.class));
              assertFalse(abilities.isActive(LastStand.class));
              assertFalse(controller.isConcealed());
            });
    when(time.getTime()).thenReturn(15_000L);
    // Queries are pure: they ignore the expired effect at once but fire nothing.
    assertFalse(abilities.isActive(LastStand.class));
    assertFalse(controller.isConcealed());
    assertNull(controller.getTint());
    assertEquals(1f, controller.getStatMultiplier(Stat.ATTACK));
    assertTrue(ended.isEmpty());
    controller.update();
    assertEquals(List.of("laststand", "invisibility"), ended);
    controller.update();
    assertEquals(2, ended.size());
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
              // Effects come off in the order they were applied: last stand was first.
            });
    when(time.getTime()).thenReturn(60_000L);
    controller.update();
    assertEquals(List.of("ended:laststand", "ended:invisibility", "used:invisibility"), events);
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
    assertEquals(List.of("laststand", "invisibility"), ended);
    assertTrue(failed.isEmpty());
    // A disposed controller keeps nothing and takes nothing new.
    StatusEffect rejected = new FakeEffect();
    assertThrows(IllegalStateException.class, () -> controller.addStatusEffect(rejected));
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
      controller.isConcealed();
      controller.getTint();
      controller.getStatMultiplier(Stat.ATTACK);
      abilities.update();
      // Queries may look at a stack, but only the frame update ticks it.
      burns.constructed().forEach(burn -> verify(burn, never()).update());
      regens.constructed().forEach(regen -> verify(regen, never()).update());
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
    assertThrows(IllegalArgumentException.class, () -> controller.addStatusEffect(null));
    Entity noStats = new Entity().addComponent(new StatusEffectsControllerComponent());
    assertThrows(IllegalStateException.class, noStats::create);
    Entity noController =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerAbilitiesComponent(time));
    assertThrows(IllegalStateException.class, noController::create);
    StatusEffect duplicate = new FakeEffect();
    controller.addStatusEffect(duplicate);
    assertThrows(IllegalStateException.class, () -> controller.addStatusEffect(duplicate));
  }

  @Test
  void shouldAcceptEffectsBeforeCreateAndRemoveThemWhenTheyReportDone() {
    StatusEffectsControllerComponent uncreated = new StatusEffectsControllerComponent();
    FakeEffect effect = new FakeEffect();
    uncreated.addStatusEffect(effect);
    new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(uncreated).create();
    uncreated.update();
    assertTrue(uncreated.hasStatusEffect(effect));
    assertEquals(0, effect.removed);
    effect.expired = true;
    assertFalse(uncreated.hasStatusEffect(effect));
    assertEquals(0, effect.removed);
    uncreated.update();
    assertEquals(1, effect.removed);
    uncreated.update();
    uncreated.removeStatusEffect(effect);
    assertEquals(1, effect.removed);
  }

  @Test
  void shouldRemoveEarlyExactlyOnceAndIgnoreUnknownEffects() {
    FakeEffect effect = new FakeEffect();
    controller.addStatusEffect(effect);
    controller.removeStatusEffect(effect);
    controller.removeStatusEffect(effect);
    controller.removeStatusEffect(null);
    controller.removeStatusEffect(new FakeEffect());
    assertEquals(1, effect.removed);
    assertFalse(controller.hasStatusEffect(effect));
  }

  @Test
  void shouldEndStandaloneEffectsOnDeathWithoutAnAbilityComponent() {
    StatusEffectsControllerComponent standalone = new StatusEffectsControllerComponent();
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    new Entity().addComponent(combat).addComponent(standalone).create();
    FakeEffect effect = new FakeEffect();
    standalone.addStatusEffect(effect);
    combat.setHealth(0);
    assertFalse(standalone.hasStatusEffect(effect));
    assertEquals(1, effect.removed);
    standalone.update();
    standalone.dispose();
    assertEquals(1, effect.removed);
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
      verify(burns.constructed().getFirst()).onRemoved();
      verify(burns.constructed().getLast(), never()).update();
      verify(burns.constructed().getLast()).onRemoved();
    }
  }

  @Test
  void shouldNotifyAKillingStackOnceWhenDeathClearsTheListMidUpdate() {
    FakeEffect killer =
        new FakeEffect() {
          @Override
          public boolean update() {
            stats.setHealth(0);
            return true;
          }
        };
    FakeEffect bystander = new FakeEffect();
    controller.addStatusEffect(killer);
    controller.addStatusEffect(bystander);
    controller.update();
    assertEquals(1, killer.removed);
    assertEquals(1, bystander.removed);
    assertFalse(controller.hasStatusEffect(bystander));
  }

  @Test
  void shouldComposeMultipliersPerStat() {
    FakeEffect slow =
        new FakeEffect() {
          @Override
          public float getStatMultiplier(Stat stat) {
            return stat == Stat.MOVEMENT_SPEED ? 0.5f : 1f;
          }
        };
    when(time.getTime()).thenReturn(0L);
    controller.addStatusEffect(slow);
    controller.addStatusEffect(StatusEffectsFactory.createLastStand(time, 1_000));
    assertEquals(LastStandEffect.MULTIPLIER, controller.getStatMultiplier(Stat.ATTACK));
    assertEquals(LastStandEffect.MULTIPLIER, controller.getStatMultiplier(Stat.ATTACK_SPEED));
    assertEquals(
        LastStandEffect.MULTIPLIER * 0.5f, controller.getStatMultiplier(Stat.MOVEMENT_SPEED));
  }

  /** Proves the controller drives anything implementing the contract, not one concrete class. */
  private static class FakeEffect implements StatusEffect {
    boolean expired;
    int removed;

    @Override
    public boolean update() {
      return expired;
    }

    @Override
    public long getRemainingDuration() {
      return expired ? 0 : 1;
    }

    @Override
    public void onRemoved() {
      removed++;
    }
  }

  private void activateBoth() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertTrue(abilities.tryActivate(Invisibility.class));
  }

  @Test
  void shouldConcealAndAmplifyAnyEntityNotJustThePlayer() {
    // No ability component anywhere: an enemy wearing the same effects behaves identically, which
    // is what naming an ability from combat, rendering and AI used to make impossible.
    CombatStatsComponent enemyStats = new CombatStatsComponent(100, 10, 4f, 2f);
    StatusEffectsControllerComponent enemyEffects = new StatusEffectsControllerComponent();
    Entity enemy = new Entity().addComponent(enemyStats).addComponent(enemyEffects);
    enemy.create();

    assertFalse(StatusEffectsControllerComponent.isConcealed(enemy));
    assertNull(StatusEffectsControllerComponent.getTint(enemy));
    assertEquals(1f, enemyEffects.getStatMultiplier(Stat.ATTACK));
    assertEquals(10, enemyStats.getEffectiveBaseAttack());

    when(time.getTime()).thenReturn(0L);
    TimedStatusEffect hidden = StatusEffectsFactory.createInvisibility(time, 5_000);
    TimedStatusEffect amplified = StatusEffectsFactory.createLastStand(time, 5_000);
    enemyEffects.addStatusEffect(hidden);
    enemyEffects.addStatusEffect(amplified);

    assertTrue(StatusEffectsControllerComponent.isConcealed(enemy));
    // Both effects compose rather than one winning.
    assertEquals(
        new Color(1f, 0.35f, 0.35f, 0.35f), StatusEffectsControllerComponent.getTint(enemy));
    assertEquals(LastStandEffect.MULTIPLIER, enemyEffects.getStatMultiplier(Stat.ATTACK));
    assertEquals(15, enemyStats.getEffectiveBaseAttack());
    assertEquals(6f, enemyStats.getEffectiveMovementSpeed());
    assertEquals(3f, enemyStats.getEffectiveAttackSpeed());

    // Queries ignore expired effects at once, with no frame update.
    when(time.getTime()).thenReturn(5_000L);
    assertFalse(StatusEffectsControllerComponent.isConcealed(enemy));
    assertNull(StatusEffectsControllerComponent.getTint(enemy));
    assertEquals(10, enemyStats.getEffectiveBaseAttack());
  }

  @Test
  void shouldTreatMissingEntitiesAndEffectlessOnesAsNotConcealed() {
    assertFalse(StatusEffectsControllerComponent.isConcealed(null));
    assertFalse(StatusEffectsControllerComponent.isConcealed(new Entity()));
    assertNull(StatusEffectsControllerComponent.getTint(null));
    assertNull(StatusEffectsControllerComponent.getTint(new Entity()));
  }

  @Test
  void shouldApplyHostileDamageToStatsWithNoEntityInsteadOfBlockingIt() {
    // A detached CombatStatsComponent has no controller to conceal it, so it is plainly hittable.
    CombatStatsComponent detached = new CombatStatsComponent(100, 10);
    detached.takeDamage(
        30, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER)));
    assertEquals(70, detached.getHealth());
  }
}
