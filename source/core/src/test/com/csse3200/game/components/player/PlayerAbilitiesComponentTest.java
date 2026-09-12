package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
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
    player = new Entity().addComponent(stats).addComponent(abilities);
    player.create();
    hostile = new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    player.getEvents().addListener("abilityUsed", (String ability) -> used.add(ability));
    player.getEvents().addListener("abilityEnded", (String ability) -> ended.add(ability));
    player
        .getEvents()
        .addListener(
            "abilityFailed", (String ability, String reason) -> failed.add(ability + ":" + reason));
  }

  @Test
  void shouldStartInactiveAndTreatMissingTargetsAsVisible() {
    assertInactive();
    assertFalse(PlayerAbilitiesComponent.isInvisible(null));
    assertFalse(PlayerAbilitiesComponent.isInvisible(new Entity()));
    assertFalse(PlayerAbilitiesComponent.isInvisible(player));
    assertTrue(used.isEmpty());
    assertTrue(ended.isEmpty());
    assertTrue(failed.isEmpty());
    assertTrue(abilities.tryInvisibility());
    assertTrue(PlayerAbilitiesComponent.isInvisible(player));
  }

  @Test
  void shouldExpireInvisibilityAtExactDurationWithoutFrameUpdate() {
    assertTrue(abilities.tryInvisibility());
    assertEquals(15_000, abilities.getInvisibilityRemainingMs());
    assertEquals(45_000, abilities.getInvisibilityCooldownRemainingMs());
    when(time.getTime()).thenReturn(START + 14_999);
    assertTrue(abilities.isInvisible());
    assertEquals(1, abilities.getInvisibilityRemainingMs());
    assertEquals(30_001, abilities.getInvisibilityCooldownRemainingMs());
    assertTrue(ended.isEmpty());

    when(time.getTime()).thenReturn(START + 15_000);
    assertFalse(abilities.isInvisible());
    assertEquals(0, abilities.getInvisibilityRemainingMs());
    assertEquals(30_000, abilities.getInvisibilityCooldownRemainingMs());
    abilities.update();
    when(time.getTime()).thenReturn(START + 100_000);
    assertEquals(0, abilities.getInvisibilityRemainingMs());
    assertEquals(0, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(List.of("invisibility"), ended);
    assertEquals(List.of("invisibility"), used);
  }

  @Test
  void shouldRejectRepeatedCastsWithoutExtendingEitherDeadlineAndRecastAtExactCooldown() {
    assertTrue(abilities.tryInvisibility());
    assertFalse(abilities.tryInvisibility());
    when(time.getTime()).thenReturn(START + 5_000);
    assertFalse(abilities.tryInvisibility());
    assertEquals(10_000, abilities.getInvisibilityRemainingMs());
    assertEquals(40_000, abilities.getInvisibilityCooldownRemainingMs());
    when(time.getTime()).thenReturn(START + 44_999);
    assertFalse(abilities.tryInvisibility());
    assertEquals(0, abilities.getInvisibilityRemainingMs());
    assertEquals(1, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(List.of("invisibility"), used);
    assertEquals(java.util.Collections.nCopies(3, "invisibility:Ability is on cooldown"), failed);

    when(time.getTime()).thenReturn(START + 45_000);
    assertEquals(0, abilities.getInvisibilityCooldownRemainingMs());
    assertTrue(abilities.tryInvisibility());
    assertEquals(15_000, abilities.getInvisibilityRemainingMs());
    assertEquals(45_000, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(List.of("invisibility", "invisibility"), used);
    assertEquals(List.of("invisibility"), ended);
  }

  @Test
  void shouldRequireEnablingAndNewDamageEvenWhenAlreadyBelowThreshold() {
    stats.takeDamage(81, hostile);
    assertInactive();
    abilities.enableLastStand();
    abilities.enableLastStand();
    abilities.update();
    assertInactive();
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldRequireHealthStrictlyBelowTwentyPercent() {
    abilities.enableLastStand();
    stats.takeDamage(79, hostile);
    assertFalse(abilities.isLastStandActive());
    stats.takeDamage(1, hostile);
    assertEquals(20, stats.getHealth());
    assertInactive();
    stats.takeDamage(1, hostile);
    assertEquals(19, stats.getHealth());
    assertTrue(abilities.isLastStandActive());
    assertEquals(10_000, abilities.getLastStandRemainingMs());
    assertEquals(60_000, abilities.getLastStandCooldownRemainingMs());
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldUseCurrentMaximumHealthWithoutIntegerRoundingOrOverflow() {
    stats.setMaxHealth(Integer.MAX_VALUE);
    stats.setHealth(429_496_730);
    abilities.enableLastStand();
    player.getEvents().trigger("damageTaken", hostile, 1, stats.getHealth());
    assertFalse(abilities.isLastStandActive());
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    assertEquals(List.of("laststand"), used);
  }

  @Test
  void shouldNotTriggerFromDirectHealthSettersOrNonhostileDamage() {
    abilities.enableLastStand();
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
    assertTrue(abilities.isLastStandActive());
  }

  @Test
  void shouldIgnoreZeroNegativeAndBlockedDamage() {
    abilities.enableLastStand();
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
    assertTrue(abilities.isLastStandActive());
  }

  @Test
  void shouldNotTriggerFromLethalDamageOrNonpositiveRemainingHealthEvents() {
    abilities.enableLastStand();
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
    abilities.enableLastStand();
    stats.takeDamage(81, hostile);
    when(time.getTime()).thenReturn(START + 5_000);
    abilities.enableLastStand();
    stats.takeDamage(1, hostile);
    assertEquals(5_000, abilities.getLastStandRemainingMs());
    assertEquals(55_000, abilities.getLastStandCooldownRemainingMs());
    when(time.getTime()).thenReturn(START + 9_999);
    assertTrue(abilities.isLastStandActive());
    assertEquals(1, abilities.getLastStandRemainingMs());
    assertTrue(ended.isEmpty());
    when(time.getTime()).thenReturn(START + 10_000);
    assertFalse(abilities.isLastStandActive());
    assertEquals(0, abilities.getLastStandRemainingMs());
    assertEquals(50_000, abilities.getLastStandCooldownRemainingMs());
    abilities.update();
    assertEquals(List.of("laststand"), ended);

    when(time.getTime()).thenReturn(START + 59_999);
    abilities.enableLastStand();
    stats.takeDamage(1, hostile);
    assertFalse(abilities.isLastStandActive());
    assertEquals(1, abilities.getLastStandCooldownRemainingMs());
    when(time.getTime()).thenReturn(START + 60_000);
    abilities.enableLastStand();
    abilities.update();
    assertFalse(abilities.isLastStandActive());
    assertEquals(0, abilities.getLastStandCooldownRemainingMs());
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    assertEquals(10_000, abilities.getLastStandRemainingMs());
    assertEquals(60_000, abilities.getLastStandCooldownRemainingMs());
    assertEquals(List.of("laststand", "laststand"), used);
    assertTrue(failed.isEmpty());
  }

  @Test
  void shouldKeepAbilitiesIndependentAndNeverMutateRawStats() {
    abilities.enableLastStand();
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryInvisibility());
    assertTrue(abilities.isLastStandActive());
    assertTrue(abilities.isInvisible());
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
    assertTrue(abilities.isInvisible());
    assertEquals(5_000, abilities.getInvisibilityRemainingMs());
    assertEquals(50_000, abilities.getLastStandCooldownRemainingMs());
    assertEquals(List.of("laststand", "invisibility"), used);
  }

  @Test
  void shouldClearBothEffectsBeforeReentrantEndListenersAndNotifyOnlyOnce() {
    abilities.enableLastStand();
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryInvisibility());
    player
        .getEvents()
        .addListener(
            "abilityEnded",
            (String ability) -> {
              assertFalse(abilities.isInvisible());
              assertFalse(abilities.isLastStandActive());
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
    abilities.enableLastStand();
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryInvisibility());
    stats.setHealth(0);
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertInactive();
    abilities.enableLastStand();
    assertFalse(abilities.tryInvisibility());
    assertEquals(List.of("invisibility:Player is not alive"), failed);
    player.getEvents().trigger("entityDied");
    player.getEvents().trigger("damageTaken", hostile, 1, 1);
    assertEquals(List.of("invisibility", "laststand"), ended);

    stats.setHealth(19);
    stats.takeDamage(1, hostile);
    assertInactive();
    abilities.enableLastStand();
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    assertTrue(abilities.tryInvisibility());
    assertEquals(List.of("laststand", "invisibility", "laststand", "invisibility"), used);
  }

  @Test
  void shouldDisposeThroughEntityLifecycleAndLeaveRetainedCallbacksInert() {
    ServiceLocator.registerEntityService(mock(EntityService.class));
    abilities.enableLastStand();
    stats.takeDamage(81, hostile);
    assertTrue(abilities.tryInvisibility());
    player.dispose();
    assertEquals(List.of("invisibility", "laststand"), ended);
    assertInactive();
    when(time.getTime()).thenReturn(START + 100_000);
    abilities.enableLastStand();
    stats.takeDamage(1, hostile);
    player.getEvents().trigger("damageTaken", hostile, 1, 1);
    player.getEvents().trigger("entityDied");
    assertFalse(abilities.tryInvisibility());
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
    unattached.enableLastStand();
    unattached.update();
    assertFalse(unattached.tryInvisibility());
    assertFalse(unattached.isInvisible());
    assertFalse(unattached.isLastStandActive());
    assertEquals(0, unattached.getInvisibilityRemainingMs());
    assertEquals(0, unattached.getInvisibilityCooldownRemainingMs());
    assertEquals(0, unattached.getLastStandRemainingMs());
    assertEquals(0, unattached.getLastStandCooldownRemainingMs());
    unattached.dispose();
    unattached.dispose();

    PlayerAbilitiesComponent noStats = new PlayerAbilitiesComponent(time);
    Entity incomplete = new Entity().addComponent(noStats);
    incomplete.create();
    List<String> failures = new ArrayList<>();
    incomplete
        .getEvents()
        .addListener("abilityFailed", (String ability, String reason) -> failures.add(reason));
    noStats.enableLastStand();
    incomplete.getEvents().trigger("damageTaken", hostile, 1, 1);
    assertFalse(noStats.tryInvisibility());
    assertFalse(noStats.isLastStandActive());
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
    Entity target = new Entity().addComponent(combat).addComponent(registered);
    target.create();
    ServiceLocator.registerTimeSource(earlierClock);
    registered.enableLastStand();
    combat.takeDamage(81, hostile);
    assertTrue(registered.tryInvisibility());
    when(time.getTime()).thenReturn(START + 10_000);
    assertFalse(registered.isLastStandActive());
    assertEquals(5_000, registered.getInvisibilityRemainingMs());
    assertEquals(35_000, registered.getInvisibilityCooldownRemainingMs());
    assertEquals(50_000, registered.getLastStandCooldownRemainingMs());
    verifyNoInteractions(earlierClock);
  }

  @Test
  void shouldPreferInjectedClockOverRegisteredClockAndAllowCastingAtTimeZero() {
    GameTime registeredClock = mock(GameTime.class);
    ServiceLocator.registerTimeSource(registeredClock);
    when(time.getTime()).thenReturn(0L);
    PlayerAbilitiesComponent injected = new PlayerAbilitiesComponent(time);
    Entity target =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(injected);
    target.create();
    assertTrue(injected.tryInvisibility());
    when(time.getTime()).thenReturn(15_000L);
    assertFalse(injected.isInvisible());
    assertEquals(30_000, injected.getInvisibilityCooldownRemainingMs());
    verifyNoInteractions(registeredClock);
  }

  private void assertInactive() {
    assertFalse(abilities.isInvisible());
    assertFalse(abilities.isLastStandActive());
    assertEquals(0, abilities.getInvisibilityRemainingMs());
    assertEquals(0, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(0, abilities.getLastStandRemainingMs());
    assertEquals(0, abilities.getLastStandCooldownRemainingMs());
  }
}
