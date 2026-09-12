package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.abilities.Invisibility;
import com.csse3200.game.components.player.abilities.LastStand;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerAbilitiesComponentTest {
  private static final long START = 1_000;
  private GameTime time;
  private CombatStatsComponent stats;
  private PlayerAbilitiesComponent abilities;
  private Entity player;
  private Entity hostile;
  private final List<String> used = new ArrayList<>();
  private final List<String> ended = new ArrayList<>();
  private final List<String> failed = new ArrayList<>();

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(START);
    stats = new CombatStatsComponent(100, 10, 2f, 4f);
    abilities = new PlayerAbilitiesComponent(time);
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(abilities);
    player.create();
    hostile = new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    player.getEvents().addListener("abilityUsed", (EventListener1<String>) used::add);
    player.getEvents().addListener("abilityEnded", (EventListener1<String>) ended::add);
    player
        .getEvents()
        .addListener(
            "abilityFailed", (String ability, String reason) -> failed.add(ability + ":" + reason));
  }

  @Test
  void shouldStartInactiveAndTreatMissingTargetsAsVisible() {
    assertInactive();
    assertFalse(Invisibility.isActiveOn(null));
    assertFalse(Invisibility.isActiveOn(new Entity()));
    assertFalse(Invisibility.isActiveOn(player));
    assertTrue(used.isEmpty());
    assertTrue(ended.isEmpty());
    assertTrue(failed.isEmpty());
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertTrue(Invisibility.isActiveOn(player));
  }

  @Test
  void shouldExpireInvisibilityAtExactDurationWithoutFrameUpdate() {
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertEquals(15_000, abilities.getRemainingMs(Invisibility.class));
    assertEquals(45_000, abilities.getCooldownRemainingMs(Invisibility.class));
    when(time.getTime()).thenReturn(START + 14_999);
    assertTrue(abilities.isActive(Invisibility.class));
    assertEquals(1, abilities.getRemainingMs(Invisibility.class));
    assertEquals(30_001, abilities.getCooldownRemainingMs(Invisibility.class));
    assertTrue(ended.isEmpty());

    when(time.getTime()).thenReturn(START + 15_000);
    assertFalse(abilities.isActive(Invisibility.class));
    assertEquals(0, abilities.getRemainingMs(Invisibility.class));
    assertEquals(30_000, abilities.getCooldownRemainingMs(Invisibility.class));
    abilities.update();
    when(time.getTime()).thenReturn(START + 100_000);
    assertEquals(0, abilities.getRemainingMs(Invisibility.class));
    assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(List.of("invisibility"), ended);
    assertEquals(List.of("invisibility"), used);
  }

  @Test
  void shouldRejectRepeatedCastsWithoutExtendingEitherDeadlineAndRecastAtExactCooldown() {
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertFalse(abilities.tryActivate(Invisibility.class));
    when(time.getTime()).thenReturn(START + 5_000);
    assertFalse(abilities.tryActivate(Invisibility.class));
    assertEquals(10_000, abilities.getRemainingMs(Invisibility.class));
    assertEquals(40_000, abilities.getCooldownRemainingMs(Invisibility.class));
    when(time.getTime()).thenReturn(START + 44_999);
    assertFalse(abilities.tryActivate(Invisibility.class));
    assertEquals(0, abilities.getRemainingMs(Invisibility.class));
    assertEquals(1, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(List.of("invisibility"), used);
    assertEquals(java.util.Collections.nCopies(3, "invisibility:Ability is on cooldown"), failed);

    when(time.getTime()).thenReturn(START + 45_000);
    assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertEquals(15_000, abilities.getRemainingMs(Invisibility.class));
    assertEquals(45_000, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(List.of("invisibility", "invisibility"), used);
    assertEquals(List.of("invisibility"), ended);
  }

  @Test
  void shouldRequireEnablingAndNewDamageEvenWhenAlreadyBelowThreshold() {
    stats.takeDamage(81, hostile);
    assertInactive();
    abilities.unlock(LastStand.class);
    abilities.unlock(LastStand.class);
    abilities.update();
    assertInactive();
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldRequireHealthStrictlyBelowTwentyPercent() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(79, hostile);
    assertFalse(abilities.isActive(LastStand.class));
    stats.takeDamage(1, hostile);
    assertEquals(20, stats.getHealth());
    assertInactive();
    stats.takeDamage(1, hostile);
    assertEquals(19, stats.getHealth());
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(10_000, abilities.getRemainingMs(LastStand.class));
    assertEquals(60_000, abilities.getCooldownRemainingMs(LastStand.class));
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldUseCurrentMaximumHealthWithoutIntegerRoundingOrOverflow() {
    stats.setMaxHealth(Integer.MAX_VALUE);
    stats.setHealth(429_496_730);
    abilities.unlock(LastStand.class);
    player.getEvents().trigger("damageTaken", hostile, 1, stats.getHealth());
    assertFalse(abilities.isActive(LastStand.class));
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldNotTriggerFromDirectHealthSettersOrNonhostileDamage() {
    abilities.unlock(LastStand.class);
    stats.setHealth(19);
    stats.addHealth(-1);
    abilities.update();
    assertInactive();
    stats.takeDamage(1);
    stats.takeDamage(1, new Entity());
    stats.takeDamage(1, new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.NPC)));
    assertInactive();
    assertTrue(used.isEmpty());
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
  }

  @Test
  void shouldIgnoreZeroNegativeAndBlockedDamage() {
    abilities.unlock(LastStand.class);
    stats.setHealth(19);
    stats.takeDamage(0, hostile);
    stats.takeDamage(-1, hostile);
    player.getEvents().trigger("damageTaken", hostile, 0, 19);
    player.getEvents().trigger("damageTaken", hostile, -1, 19);
    stats.setInvulnerable(true);
    stats.takeDamage(1, hostile);
    stats.setInvulnerable(false);
    stats.setIncomingDamageMultiplier(0f);
    stats.takeDamage(1, hostile);
    assertEquals(19, stats.getHealth());
    assertInactive();
    assertTrue(used.isEmpty());
    stats.setIncomingDamageMultiplier(1f);
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
  }

  @Test
  void shouldNotTriggerFromLethalDamageOrNonpositiveRemainingHealthEvents() {
    abilities.unlock(LastStand.class);
    player.getEvents().trigger("damageTaken", hostile, 100, 0);
    player.getEvents().trigger("damageTaken", hostile, 101, -1);
    assertInactive();
    stats.takeDamage(101, hostile);
    assertTrue(stats.isDead());
    assertInactive();
    assertTrue(used.isEmpty());
    assertTrue(ended.isEmpty());
  }

  @Test
  void shouldKeepLastStandDeadlinesThroughRepeatedEnablingAndDamage() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, hostile);
    when(time.getTime()).thenReturn(START + 5_000);
    abilities.unlock(LastStand.class);
    stats.takeDamage(1, hostile);
    assertEquals(5_000, abilities.getRemainingMs(LastStand.class));
    assertEquals(55_000, abilities.getCooldownRemainingMs(LastStand.class));
    when(time.getTime()).thenReturn(START + 9_999);
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(1, abilities.getRemainingMs(LastStand.class));
    assertTrue(ended.isEmpty());
    when(time.getTime()).thenReturn(START + 10_000);
    assertFalse(abilities.isActive(LastStand.class));
    assertEquals(0, abilities.getRemainingMs(LastStand.class));
    assertEquals(50_000, abilities.getCooldownRemainingMs(LastStand.class));
    abilities.update();
    assertEquals(List.of("laststand"), ended);

    when(time.getTime()).thenReturn(START + 59_999);
    abilities.unlock(LastStand.class);
    stats.takeDamage(1, hostile);
    assertFalse(abilities.isActive(LastStand.class));
    assertEquals(1, abilities.getCooldownRemainingMs(LastStand.class));
    when(time.getTime()).thenReturn(START + 60_000);
    abilities.unlock(LastStand.class);
    abilities.update();
    assertFalse(abilities.isActive(LastStand.class));
    assertEquals(0, abilities.getCooldownRemainingMs(LastStand.class));
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(10_000, abilities.getRemainingMs(LastStand.class));
    assertEquals(60_000, abilities.getCooldownRemainingMs(LastStand.class));
    assertEquals(List.of("laststand", "laststand"), used);
    assertTrue(failed.isEmpty());
  }

  @Test
  void shouldKeepAbilitiesIndependentAndNeverMutateRawStats() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertTrue(abilities.isActive(LastStand.class));
    assertTrue(abilities.isActive(Invisibility.class));
    assertEquals(10, stats.getBaseAttack());
    assertEquals(4f, stats.getAttackSpeed());
    assertEquals(2f, stats.getMovementSpeed());
    assertEquals(15, stats.getEffectiveBaseAttack());
    assertEquals(6f, stats.getEffectiveAttackSpeed());
    stats.takeDamage(1, hostile);
    assertEquals(19, stats.getHealth());
    when(time.getTime()).thenReturn(START + 10_000);
    assertEquals(10, stats.getEffectiveBaseAttack());
    assertEquals(4f, stats.getEffectiveAttackSpeed());
    assertTrue(abilities.isActive(Invisibility.class));
    assertEquals(5_000, abilities.getRemainingMs(Invisibility.class));
    assertEquals(50_000, abilities.getCooldownRemainingMs(LastStand.class));
    assertEquals(List.of("laststand", "invisibility"), used);
  }

  @Test
  void shouldClearBothEffectsBeforeReentrantEndListenersAndNotifyOnlyOnce() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryActivate(Invisibility.class));
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String ability) -> {
              assertFalse(abilities.isActive(Invisibility.class));
              assertFalse(abilities.isActive(LastStand.class));
              abilities.update();
            });
    when(time.getTime()).thenReturn(START + 100_000);
    abilities.update();
    abilities.update();
    assertInactive();
    assertEquals(List.of("invisibility", "laststand"), ended);
  }

  @Test
  void shouldEndEffectsImmediatelyOnDeathAndRequireReenablingAfterRevival() {
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryActivate(Invisibility.class));
    stats.setHealth(0);
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertInactive();
    abilities.unlock(LastStand.class);
    assertFalse(abilities.tryActivate(Invisibility.class));
    assertEquals(List.of("invisibility:Player is not alive"), failed);
    player.getEvents().trigger("entityDied");
    player.getEvents().trigger("damageTaken", hostile, 1, 1);
    assertEquals(List.of("invisibility", "laststand"), ended);

    stats.setHealth(19);
    stats.takeDamage(1, hostile);
    assertInactive();
    abilities.unlock(LastStand.class);
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isActive(LastStand.class));
    assertTrue(abilities.tryActivate(Invisibility.class));
    assertEquals(List.of("laststand", "invisibility", "laststand", "invisibility"), used);
  }

  @Test
  void shouldDisposeThroughEntityLifecycleAndLeaveRetainedCallbacksInert() {
    ServiceLocator.registerEntityService(mock(EntityService.class));
    abilities.unlock(LastStand.class);
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryActivate(Invisibility.class));
    player.dispose();
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertInactive();
    when(time.getTime()).thenReturn(START + 100_000);
    abilities.unlock(LastStand.class);
    stats.takeDamage(1, hostile);
    player.getEvents().trigger("damageTaken", hostile, 1, 1);
    player.getEvents().trigger("entityDied");
    assertFalse(abilities.tryActivate(Invisibility.class));
    abilities.dispose();
    player.dispose();
    assertInactive();
    assertEquals(List.of("laststand", "invisibility"), used);
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertTrue(failed.isEmpty());
  }

  @Test
  void shouldBeSafeBeforeCreationAndWithoutCombatStats() {
    PlayerAbilitiesComponent unattached = new PlayerAbilitiesComponent(time);
    unattached.unlock(LastStand.class);
    unattached.update();
    assertFalse(unattached.tryActivate(Invisibility.class));
    assertFalse(unattached.isActive(Invisibility.class));
    assertFalse(unattached.isActive(LastStand.class));
    assertEquals(0, unattached.getRemainingMs(Invisibility.class));
    assertEquals(0, unattached.getCooldownRemainingMs(Invisibility.class));
    assertEquals(0, unattached.getRemainingMs(LastStand.class));
    assertEquals(0, unattached.getCooldownRemainingMs(LastStand.class));
    unattached.dispose();
    unattached.dispose();

    PlayerAbilitiesComponent noStats = new PlayerAbilitiesComponent(time);
    Entity incomplete = new Entity().addComponent(noStats);
    incomplete.create();
    List<String> failures = new ArrayList<>();
    incomplete
        .getEvents()
        .addListener("abilityFailed", (String ability, String reason) -> failures.add(reason));
    noStats.unlock(LastStand.class);
    incomplete.getEvents().trigger("damageTaken", hostile, 1, 1);
    assertFalse(noStats.tryActivate(Invisibility.class));
    assertFalse(noStats.isActive(LastStand.class));
    assertEquals(List.of("Player is not alive"), failures);
    noStats.dispose();
  }

  @Test
  void shouldResolveRegisteredClockAtCreationAndRetainIt() {
    GameTime earlierClock = mock(GameTime.class);
    ServiceLocator.registerTimeSource(earlierClock);
    PlayerAbilitiesComponent registered = new PlayerAbilitiesComponent();
    ServiceLocator.registerTimeSource(time);
    CombatStatsComponent combat = new CombatStatsComponent(100, 10);
    Entity target =
        new Entity()
            .addComponent(combat)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(registered);
    target.create();
    ServiceLocator.registerTimeSource(earlierClock);
    registered.unlock(LastStand.class);
    combat.takeDamage(81, hostile);
    assertTrue(registered.tryActivate(Invisibility.class));
    when(time.getTime()).thenReturn(START + 10_000);
    assertFalse(registered.isActive(LastStand.class));
    assertEquals(5_000, registered.getRemainingMs(Invisibility.class));
    assertEquals(35_000, registered.getCooldownRemainingMs(Invisibility.class));
    assertEquals(50_000, registered.getCooldownRemainingMs(LastStand.class));
    verifyNoInteractions(earlierClock);
  }

  @Test
  void shouldPreferInjectedClockOverRegisteredClockAndAllowCastingAtTimeZero() {
    GameTime registeredClock = mock(GameTime.class);
    ServiceLocator.registerTimeSource(registeredClock);
    when(time.getTime()).thenReturn(0L);
    PlayerAbilitiesComponent injected = new PlayerAbilitiesComponent(time);
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(injected);
    target.create();
    assertTrue(injected.tryActivate(Invisibility.class));
    when(time.getTime()).thenReturn(15_000L);
    assertFalse(injected.isActive(Invisibility.class));
    assertEquals(30_000, injected.getCooldownRemainingMs(Invisibility.class));
    verifyNoInteractions(registeredClock);
  }

  @Test
  void shouldRunACastAbilityAddedWithoutChangingTheComponent() {
    abilities.register(new TestAbility(time));

    assertFalse(abilities.tryActivate(TestAbility.class));
    assertEquals(List.of("testability:Ability is locked"), failed);

    abilities.unlock(TestAbility.class);
    assertTrue(abilities.tryActivate(TestAbility.class));
    assertEquals(List.of("testability"), used);
    assertEquals(5_000, abilities.getRemainingMs(TestAbility.class));
    assertEquals(20_000, abilities.getCooldownRemainingMs(TestAbility.class));
    // The built in abilities keep their own deadlines.
    assertFalse(abilities.isActive(Invisibility.class));
    assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));

    when(time.getTime()).thenReturn(START + 5_000);
    assertFalse(abilities.isActive(TestAbility.class));
    assertEquals(List.of("testability"), ended);
    assertEquals(15_000, abilities.getCooldownRemainingMs(TestAbility.class));
    assertFalse(abilities.tryActivate(TestAbility.class));
    assertEquals(
        List.of("testability:Ability is locked", "testability:Ability is on cooldown"), failed);
  }

  @Test
  void shouldTriggerAnAddedPassiveFromDamageAlongsideLastStand() {
    abilities.register(new TestPassive(time));
    abilities.unlock(LastStand.class);

    stats.takeDamage(85, hostile);

    assertEquals(List.of("laststand", "testpassive"), used);
    assertTrue(abilities.isActive(LastStand.class));
    assertEquals(3_000, abilities.getRemainingMs(TestPassive.class));
    assertTrue(failed.isEmpty());
  }

  @Test
  void shouldGiveAnAbilityItsOwnerSoItCanActOnOtherEntities() {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(50, 5));
    enemy.create();
    TargetedAbility targeted = new TargetedAbility(enemy);
    abilities.register(targeted);

    assertSame(player, targeted.getOwner());
    assertTrue(abilities.tryActivate(TargetedAbility.class));

    // The effect landed on the enemy, and the player is not the one running anything.
    assertEquals(43, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertFalse(abilities.isActive(TargetedAbility.class));
    assertEquals(List.of("targeted"), used);
    // Its cooldown is still the component's business, not the ability's.
    assertEquals(6_000, abilities.getCooldownRemainingMs(TargetedAbility.class));
    assertFalse(abilities.tryActivate(TargetedAbility.class));
    assertEquals(50, enemy.getComponent(CombatStatsComponent.class).getMaxHealth());
  }

  /**
   * An ability whose effect lands on somebody else. It extends PlayerAbility directly, reaches the
   * world through getOwner, and needs no change to PlayerAbilitiesComponent to work.
   */
  private static final class TargetedAbility extends PlayerAbility {
    private final Entity target;

    private TargetedAbility(Entity target) {
      super("targeted", 6_000, true);
      this.target = target;
    }

    @Override
    public boolean isCastable() {
      return true;
    }

    @Override
    public void start() {
      assertNotNull(getOwner());
      target.getComponent(CombatStatsComponent.class).takeDamage(7, getOwner());
    }

    @Override
    public void stop() {
      // Nothing lingers on the player to stop.
    }

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public long getRemainingMs() {
      return 0;
    }
  }

  /** An ability that owns a timed effect on the player, the same way Invisibility does. */
  private abstract static class EffectAbility extends PlayerAbility {
    private final TimedStatusEffect effect;
    private StatusEffectsControllerComponent effects;

    private EffectAbility(String name, TimedStatusEffect effect, long cooldown, boolean unlocked) {
      super(name, cooldown, unlocked);
      this.effect = effect;
    }

    @Override
    protected void attach(StatusEffectsControllerComponent controller, Runnable onEnded) {
      effects = controller;
      effect.setOnEnded(onEnded);
      controller.registerEffect(effect);
    }

    @Override
    public void start() {
      effect.activate();
    }

    @Override
    public void stop() {
      if (effects != null) {
        effects.removeEffect(effect);
      }
    }

    @Override
    public boolean isRunning() {
      return effect.isActive();
    }

    @Override
    public long getRemainingMs() {
      return effect.getRemainingDuration();
    }
  }

  /** A cast ability that starts locked, declared entirely outside PlayerAbilitiesComponent. */
  private static final class TestAbility extends EffectAbility {
    private TestAbility(GameTime time) {
      super("testability", new TimedStatusEffect(time, 5_000), 20_000, false);
    }

    @Override
    public boolean isCastable() {
      return true;
    }
  }

  /** A passive that starts on any hostile hit, declared entirely outside the component. */
  private static final class TestPassive extends EffectAbility {
    private TestPassive(GameTime time) {
      super("testpassive", new TimedStatusEffect(time, 3_000), 9_000, true);
    }

    @Override
    public boolean triggersOnDamage(
        CombatStatsComponent combat, Entity attacker, int healthLost, int remainingHealth) {
      return healthLost > 0 && CombatStatsComponent.isHostileAttacker(attacker);
    }
  }

  @Test
  void shouldRunAnInstantAbilityThatAppliesNoStatusEffect() {
    InstantAbility instant = new InstantAbility();
    abilities.register(instant);

    assertTrue(abilities.tryActivate(InstantAbility.class));
    assertEquals(1, instant.starts);
    assertEquals(List.of("instant"), used);
    // It finishes at once, so it never runs and never ends.
    assertFalse(abilities.isActive(InstantAbility.class));
    assertEquals(0, abilities.getRemainingMs(InstantAbility.class));
    assertTrue(ended.isEmpty());
    // Its cooldown is still the component's business.
    assertEquals(8_000, abilities.getCooldownRemainingMs(InstantAbility.class));
    assertFalse(abilities.tryActivate(InstantAbility.class));
    assertEquals(List.of("instant:Ability is on cooldown"), failed);
    assertEquals(1, instant.starts);

    when(time.getTime()).thenReturn(START + 8_000);
    assertTrue(abilities.tryActivate(InstantAbility.class));
    assertEquals(2, instant.starts);
  }

  /**
   * An ability that finishes the moment it starts. It extends PlayerAbility directly and never
   * touches the status effects system, which is the case the ability and effect split exists for.
   */
  private static final class InstantAbility extends PlayerAbility {
    private int starts;

    private InstantAbility() {
      super("instant", 8_000, true);
    }

    @Override
    public boolean isCastable() {
      return true;
    }

    @Override
    public void start() {
      starts++;
    }

    @Override
    public void stop() {
      // Nothing to stop.
    }

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public long getRemainingMs() {
      return 0;
    }
  }

  private void assertInactive() {
    assertFalse(abilities.isActive(Invisibility.class));
    assertFalse(abilities.isActive(LastStand.class));
    assertEquals(0, abilities.getRemainingMs(Invisibility.class));
    assertEquals(0, abilities.getCooldownRemainingMs(Invisibility.class));
    assertEquals(0, abilities.getRemainingMs(LastStand.class));
    assertEquals(0, abilities.getCooldownRemainingMs(LastStand.class));
  }
}
