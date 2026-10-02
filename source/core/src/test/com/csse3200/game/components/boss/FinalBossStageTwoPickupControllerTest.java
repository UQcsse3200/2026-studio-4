package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ground collection and energy bookkeeping need no renderer, physics world or entity service. */
class FinalBossStageTwoPickupControllerTest {
  private static final float EPSILON = 0.0001f;
  private static final Rectangle ARENA = new Rectangle(0f, 0f, 20f, 12f);
  private static final Rectangle DISTANT_BOSS = new Rectangle(-100f, -100f, 2f, 2f);
  private static final Rectangle DISTANT_PLAYER = new Rectangle(-50f, -50f, 1f, 1f);
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoPickupController controller;

  @BeforeEach
  void setup() {
    config = new FinalBossStageTwoConfig();
    controller = new FinalBossStageTwoPickupController(config, new Random(731));
    // Mark the initial spawn attempt complete so collection tests can supply exact positions.
    controller.update(0f, ARENA, DISTANT_BOSS, DISTANT_PLAYER, null, area -> false);
  }

  @Test
  void initialSpawnCreatesOneAndFourSecondIntervalsAddAtMostOneToTheCap() {
    controller.clear();
    spawnUpdate(0f);
    assertEquals(1, controller.pickups.size());
    spawnUpdate(config.icePickupSpawnInterval - 0.25f);
    assertEquals(1, controller.pickups.size());
    spawnUpdate(0.25f);
    assertEquals(2, controller.pickups.size());
    spawnUpdate(0.25f);
    assertEquals(2, controller.pickups.size());

    spawnUpdate(600f);
    assertEquals(
        1, controller.pickups.size(), "A long stall expires old gems and creates only one");
    assertEquals(0d, controller.pickups.getFirst().elapsed);
    spawnUpdate(0.01f);
    assertEquals(1, controller.pickups.size(), "No overdue spawn debt remains");
  }

  @Test
  void walkingIntoThePickupRadiusAddsOneChargeAndACollectionBurst() {
    var pickup = addPickup(10f, 6f);
    collectUpdate(0.1f, boundsAt(9.5f, 6f, 1f), new Vector2(9f, 6f));

    assertFalse(controller.pickups.contains(pickup));
    assertEquals(1, controller.getChargeCount());
    assertEquals(1f, controller.getChargeFraction(0));
    assertEquals(0f, controller.getChargeFraction(1));
    assertEquals(1, controller.bursts.size());
    assertEquals(pickup.position, controller.bursts.getFirst().position);
    assertNotSame(pickup.position, controller.bursts.getFirst().position);
    assertTrue(controller.disappearances.isEmpty());
    collectUpdate(config.icePickupEffectDuration, DISTANT_PLAYER, null);
    assertTrue(controller.bursts.isEmpty());
    assertEquals(1, controller.getChargeCount());
    collectUpdate(config.icePickupLifetime, DISTANT_PLAYER, null);
    assertTrue(controller.disappearances.isEmpty(), "A collected gem cannot expire later");
  }

  @Test
  void dashCollectsAlongItsPathEvenIfItEndsBeyondTheGem() {
    addPickup(10f, 6f);
    collectUpdate(0.2f, boundsAt(18f, 6f, 1f), new Vector2(2f, 6f));
    assertTrue(controller.pickups.isEmpty());
    assertEquals(1, controller.getChargeCount());
  }

  @Test
  void dashEntryBeforeExpiryWinsEvenWhenTheFrameEndsAfterExpiry() {
    config.icePickupRadius = 0.5f;
    var pickup = addPickup(7f, 6f);
    pickup.elapsed = 5.75d; // 1.25 seconds remain; the combined radius is exactly two.
    collectUpdate(2f, boundsAt(10f, 6f, 5f), new Vector2(0f, 6f));
    assertTrue(controller.pickups.isEmpty());
    assertEquals(1, controller.getChargeCount(), "Entry at one second precedes expiry");
    assertTrue(controller.disappearances.isEmpty());
  }

  @Test
  void expiryWinsAtTheExactEntryTimeAndAlsoBeforeThePlayerArrives() {
    config.icePickupRadius = 0.5f;
    for (double elapsed : new double[] {6d, 6.25d}) {
      controller.clear();
      controller.update(0f, ARENA, DISTANT_BOSS, DISTANT_PLAYER, null, area -> false);
      var pickup = addPickup(7f, 6f);
      pickup.elapsed = elapsed;
      collectUpdate(2f, boundsAt(10f, 6f, 5f), new Vector2(0f, 6f));
      assertTrue(controller.pickups.isEmpty());
      assertEquals(0, controller.getChargeCount());
      assertTrue(controller.bursts.isEmpty());
    }
  }

  @Test
  void sevenSecondLifetimeIncludesTheArrivalAnimation() {
    var pickup = addPickup(10f, 6f);
    collectUpdate(6.75f, DISTANT_PLAYER, null);
    assertTrue(controller.pickups.contains(pickup));
    collectUpdate(0.25f, DISTANT_PLAYER, null);
    assertTrue(controller.pickups.isEmpty());
    assertTrue(controller.bursts.isEmpty());
    assertEquals(0, controller.getChargeCount());
  }

  @Test
  void expiryCreatesOneStationaryDisappearanceThatCannotStillBeCollected() {
    var pickup = addPickup(10f, 6f);

    collectUpdate(config.icePickupLifetime, DISTANT_PLAYER, null);

    assertTrue(controller.pickups.isEmpty());
    assertTrue(controller.bursts.isEmpty());
    assertEquals(1, controller.disappearances.size());
    var disappearance = controller.disappearances.getFirst();
    assertEquals(pickup.position, disappearance.position);
    assertNotSame(pickup.position, disappearance.position);
    assertEquals(0f, disappearance.elapsed);
    assertEquals(config.icePickupDisappearDuration, controller.getDisappearDuration());
    assertTrue(controller.isClearOfPickups(gemBounds(pickup.position, config.icePickupRadius)));

    collectUpdate(0f, boundsAt(10f, 6f, 1f), null);
    assertEquals(0, controller.getChargeCount());
    assertTrue(controller.bursts.isEmpty());
    assertEquals(1, controller.disappearances.size());
    assertSame(disappearance, controller.disappearances.getFirst());

    collectUpdate(controller.getDisappearDuration(), DISTANT_PLAYER, null);
    assertTrue(controller.disappearances.isEmpty());
    collectUpdate(0f, DISTANT_PLAYER, null);
    assertTrue(controller.disappearances.isEmpty(), "Expiration is not replayed on later frames");
  }

  @Test
  void existingDisappearancesAgeByDeltaAndNewOnesStartAtTheExpiryOvershoot() {
    var earlier = addPickup(4f, 6f);
    earlier.elapsed = config.icePickupLifetime - 0.25d;
    collectUpdate(0.25f, DISTANT_PLAYER, null);
    var existingEffect = controller.disappearances.getFirst();
    var later = addPickup(10f, 6f);
    later.elapsed = config.icePickupLifetime - 0.25d;

    collectUpdate(0.5f, DISTANT_PLAYER, null);

    assertTrue(controller.pickups.isEmpty());
    assertEquals(2, controller.disappearances.size());
    assertSame(existingEffect, controller.disappearances.getFirst());
    assertEquals(0.5f, existingEffect.elapsed, EPSILON);
    assertEquals(later.position, controller.disappearances.get(1).position);
    assertEquals(0.25f, controller.disappearances.get(1).elapsed, EPSILON);
  }

  @Test
  void stallsAtOrBeyondTheWholeDisappearanceDurationDoNotReplayOldExpiryEffects() {
    config.icePickupDisappearDuration = 0.5f;
    for (float delta : new float[] {0.75f, 50f}) {
      controller.clear();
      collectUpdate(0f, DISTANT_PLAYER, null);
      var pickup = addPickup(10f, 6f);
      pickup.elapsed = config.icePickupLifetime - 0.25d;

      collectUpdate(delta, DISTANT_PLAYER, null);

      assertTrue(controller.pickups.isEmpty());
      assertTrue(controller.disappearances.isEmpty());
      collectUpdate(0.1f, DISTANT_PLAYER, null);
      assertTrue(controller.disappearances.isEmpty());
      assertEquals(0, controller.getChargeCount());
    }
  }

  @Test
  void expiryAtTheExactTouchTimeStartsDisappearanceInsteadOfCollecting() {
    config.icePickupRadius = 0.5f;
    var pickup = addPickup(7f, 6f);
    pickup.elapsed = config.icePickupLifetime - 0.25d;

    // With a combined radius of two, this dash reaches the gem at exactly 0.25 seconds.
    collectUpdate(0.5f, boundsAt(10f, 6f, 5f), new Vector2(0f, 6f));

    assertTrue(controller.pickups.isEmpty());
    assertEquals(0, controller.getChargeCount());
    assertTrue(controller.bursts.isEmpty());
    assertEquals(1, controller.disappearances.size());
    assertEquals(pickup.position, controller.disappearances.getFirst().position);
    assertEquals(0.25f, controller.disappearances.getFirst().elapsed, EPSILON);
  }

  @Test
  void fullEnergyStillLetsAnUncollectedGemExpireWithItsDisappearance() {
    collectOne();
    collectOne();
    var pickup = addPickup(12f, 6f);
    pickup.elapsed = config.icePickupLifetime - 0.25d;

    collectUpdate(0.25f, boundsAt(12f, 6f, 1f), null);

    assertTrue(controller.pickups.isEmpty());
    assertEquals(2, controller.getChargeCount());
    assertEquals(1f, controller.getChargeFraction(0));
    assertEquals(1f, controller.getChargeFraction(1));
    assertEquals(2, controller.bursts.size(), "The uncollected gem creates no collection burst");
    assertEquals(1, controller.disappearances.size());
    assertEquals(pickup.position, controller.disappearances.getFirst().position);
    assertEquals(0f, controller.disappearances.getFirst().elapsed);
  }

  @Test
  void twoOccupiedSlotsLeaveFurtherGemsUntilEnergyIsFreed() {
    collectOne();
    collectOne();
    assertEquals(0.25f, controller.consumeEnergy(0.25f), EPSILON);
    var untouched = addPickup(10f, 6f);
    collectUpdate(0.1f, boundsAt(10f, 6f, 1f), null);
    assertTrue(controller.pickups.contains(untouched));
    assertEquals(0.75f, controller.getChargeFraction(0), EPSILON);
    assertEquals(1f, controller.getChargeFraction(1));

    controller.consumeEnergy(0.75f);
    collectUpdate(0.1f, boundsAt(10f, 6f, 1f), null);
    assertFalse(controller.pickups.contains(untouched));
    assertEquals(2, controller.getChargeCount());
    assertEquals(1f, controller.getChargeFraction(0));
    assertEquals(1f, controller.getChargeFraction(1));
  }

  @Test
  void collectingPreservesPartialEnergyAndConsumptionCrossesSlotsSequentially() {
    collectOne();
    assertEquals(0.25f, controller.consumeEnergy(0.25f), EPSILON);
    collectOne();
    assertEquals(0.75f, controller.getChargeFraction(0), EPSILON);
    assertEquals(1f, controller.getChargeFraction(1));
    assertEquals(1f, controller.consumeEnergy(1f), EPSILON);
    assertEquals(1, controller.getChargeCount());
    assertEquals(0f, controller.getChargeFraction(0));
    assertEquals(0.75f, controller.getChargeFraction(1), EPSILON);
    collectUpdate(100f, DISTANT_PLAYER, null);
    assertEquals(0.75f, controller.getChargeFraction(1), EPSILON);
    assertEquals(0.75f, controller.consumeEnergy(Float.MAX_VALUE), EPSILON);
    assertEquals(0, controller.getChargeCount());
    assertEquals(0f, controller.getChargeFraction(0));
    assertEquals(0f, controller.getChargeFraction(-1));
    assertEquals(0f, controller.getChargeFraction(2));
  }

  @Test
  void refilledFirstSlotWaitsUntilTheOlderSecondSlotIsDrained() {
    collectOne();
    collectOne();
    controller.consumeEnergy(1f);
    assertEquals(0f, controller.getChargeFraction(0));
    assertEquals(1f, controller.getChargeFraction(1));
    collectOne();
    controller.consumeEnergy(0.25f);
    assertEquals(1f, controller.getChargeFraction(0), "New energy keeps its own first blue bar");
    assertEquals(0.75f, controller.getChargeFraction(1), EPSILON);
    controller.consumeEnergy(0.75f);
    assertEquals(1f, controller.getChargeFraction(0));
    assertEquals(0f, controller.getChargeFraction(1));
    controller.consumeEnergy(0.25f);
    assertEquals(0.75f, controller.getChargeFraction(0), EPSILON);
    assertEquals(1, controller.getChargeCount());
  }

  @Test
  void fullEnergyStopsNewSpawnsButExistingGemsAgeAndSpawnCadenceResumesWithOneSlot() {
    collectOne();
    collectOne();
    var existing = addPickup(12f, 6f);
    spawnUpdate(config.icePickupSpawnInterval);
    assertEquals(1, controller.pickups.size());
    assertTrue(controller.pickups.contains(existing));
    assertEquals(4d, existing.elapsed);
    controller.consumeEnergy(1f);
    spawnUpdate(config.icePickupSpawnInterval - 0.25f);
    assertTrue(controller.pickups.isEmpty(), "The old gem expires without an immediate refill");
    spawnUpdate(0.25f);
    assertEquals(
        1, controller.pickups.size(), "One free energy slot permits the next scheduled spawn");
  }

  @Test
  void fractionalShotCostsDoNotLeaveAGhostOccupiedSlot() {
    collectOne();
    for (int shot = 0; shot < 3; shot++) controller.consumeEnergy(1f / 3f);
    assertEquals(0, controller.getChargeCount());
    assertEquals(0f, controller.getChargeFraction(0));
    collectOne();
    assertEquals(1f, controller.getChargeFraction(0));
  }

  @Test
  void invalidEnergyAmountsCannotAddOrEraseEnergy() {
    collectOne();
    for (float amount : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      assertEquals(0f, controller.consumeEnergy(amount));
      assertEquals(1f, controller.getChargeFraction(0));
    }
  }

  @Test
  void oneFreeSlotCollectsTheFirstGemOnThePathRegardlessOfListOrder() {
    collectOne();
    var farther = addPickup(14f, 6f);
    var nearer = addPickup(6f, 6f);
    collectUpdate(0.3f, boundsAt(18f, 6f, 1f), new Vector2(2f, 6f));
    assertEquals(2, controller.getChargeCount());
    assertFalse(controller.pickups.contains(nearer));
    assertTrue(controller.pickups.contains(farther));
  }

  @Test
  void newSpawnCannotBeCollectedByTheEarlierPortionOfTheSameDash() {
    controller = new FinalBossStageTwoPickupController(config, new FixedRandom(0.5f));
    Rectangle player = boundsAt(18f, 6f, 1f);
    controller.update(1f, ARENA, DISTANT_BOSS, player, new Vector2(2f, 6f), area -> true);
    assertEquals(1, controller.pickups.size());
    assertEquals(new Vector2(10f, 6f), controller.pickups.getFirst().position);
    assertEquals(0d, controller.pickups.getFirst().elapsed);
    assertEquals(0, controller.getChargeCount());
  }

  @Test
  void placementUsesFullActorBoundsClearanceOtherGemsAndArenaEdges() {
    controller.clear();
    Rectangle boss = new Rectangle(8f, 4f, 3f, 3f);
    Rectangle player = new Rectangle(2f, 2f, 2f, 2f);
    Rectangle wall = new Rectangle(15f, 0f, 1f, 12f);
    AtomicInteger queries = new AtomicInteger();
    FinalBossStageTwoPickupController.PlacementQuery placement =
        clearance -> {
          queries.incrementAndGet();
          assertFalse(clearance.overlaps(boss));
          assertFalse(clearance.overlaps(player));
          assertTrue(ARENA.contains(clearance));
          return !clearance.overlaps(wall);
        };
    controller.update(0f, ARENA, boss, player, null, placement);
    controller.update(config.icePickupSpawnInterval, ARENA, boss, player, null, placement);
    assertEquals(2, controller.pickups.size());
    assertTrue(queries.get() >= 2);
    for (var pickup : controller.pickups) {
      Rectangle clearance =
          gemBounds(pickup.position, config.icePickupRadius + config.icePickupGap);
      assertFalse(clearance.overlaps(wall));
      for (var other : controller.pickups) {
        if (other != pickup)
          assertFalse(clearance.overlaps(gemBounds(other.position, config.icePickupRadius)));
      }
    }
  }

  @Test
  void smallOrFullyBlockedArenaSkipsSpawnWithBoundedAttemptsAndZeroCountDisablesIt() {
    controller.clear();
    AtomicInteger attempts = new AtomicInteger();
    controller.update(
        0f,
        ARENA,
        DISTANT_BOSS,
        DISTANT_PLAYER,
        null,
        area -> {
          attempts.incrementAndGet();
          return false;
        });
    assertTrue(controller.pickups.isEmpty());
    assertTrue(attempts.get() > 0 && attempts.get() <= 48);
    controller.clear();
    controller.update(
        0f, new Rectangle(0f, 0f, 1f, 1f), DISTANT_BOSS, DISTANT_PLAYER, null, area -> true);
    assertTrue(controller.pickups.isEmpty());
    controller.clear();
    config.icePickupCount = 0;
    spawnUpdate(0f);
    spawnUpdate(100f);
    assertTrue(controller.pickups.isEmpty());
  }

  @Test
  void resizeDropsOutsideGemsAndArenaGetterCannotMutateControllerState() {
    var inside = addPickup(3f, 3f);
    var outside = addPickup(15f, 6f);
    addPickup(4f, 4f).elapsed = config.icePickupLifetime;
    addPickup(16f, 8f).elapsed = config.icePickupLifetime;
    collectUpdate(0f, DISTANT_PLAYER, null);
    assertEquals(2, controller.disappearances.size());
    Rectangle smaller = new Rectangle(0f, 0f, 8f, 8f);
    controller.update(0f, smaller, DISTANT_BOSS, DISTANT_PLAYER, null, area -> false);
    assertTrue(controller.pickups.contains(inside));
    assertFalse(controller.pickups.contains(outside));
    assertEquals(
        1, controller.disappearances.size(), "Resize removes effects without creating new ones");
    assertEquals(new Vector2(4f, 4f), controller.disappearances.getFirst().position);
    Rectangle copy = controller.getArenaBounds();
    copy.set(100f, 100f, 1f, 1f);
    assertEquals(smaller, controller.getArenaBounds());
    smaller.set(100f, 100f, 1f, 1f);
    assertEquals(new Rectangle(0f, 0f, 8f, 8f), controller.getArenaBounds());
  }

  @Test
  void icePlacementExclusionUsesTheWholeLogicalGemSquare() {
    addPickup(10f, 6f);
    assertFalse(controller.isClearOfPickups(new Rectangle(9.5f, 5.5f, 1f, 1f)));
    assertFalse(controller.isClearOfPickups(new Rectangle(10.3f, 6f, 0.2f, 0.2f)));
    assertTrue(controller.isClearOfPickups(new Rectangle(11f, 6f, 1f, 1f)));
    controller.clear();
    assertTrue(controller.isClearOfPickups(ARENA));
  }

  @Test
  void invalidDeltaDoesNothingAndInvalidArenaClearsTheEncounterState() {
    collectOne();
    var pickup = addPickup(12f, 6f);
    addPickup(15f, 6f).elapsed = config.icePickupLifetime;
    collectUpdate(0f, DISTANT_PLAYER, null);
    for (float delta : new float[] {-1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      collectUpdate(delta, DISTANT_PLAYER, null);
      assertTrue(controller.pickups.contains(pickup));
      assertEquals(0d, pickup.elapsed);
      assertEquals(1, controller.getChargeCount());
      assertEquals(1, controller.disappearances.size());
      assertEquals(0f, controller.disappearances.getFirst().elapsed);
    }
    controller.update(0.1f, null, DISTANT_BOSS, DISTANT_PLAYER, null, area -> true);
    assertTrue(controller.pickups.isEmpty());
    assertTrue(controller.bursts.isEmpty());
    assertTrue(controller.disappearances.isEmpty());
    assertEquals(0, controller.getChargeCount());
    assertNull(controller.getArenaBounds());
    spawnUpdate(0f);
    assertEquals(1, controller.pickups.size());
  }

  @Test
  void clearCanRestartButDisposePermanentlyStopsSpawningAndClearsEnergyAndEffects() {
    collectOne();
    addPickup(12f, 6f);
    addPickup(15f, 6f).elapsed = config.icePickupLifetime;
    collectUpdate(0f, DISTANT_PLAYER, null);
    assertEquals(1, controller.disappearances.size());
    controller.clear();
    controller.clear();
    assertTrue(controller.pickups.isEmpty());
    assertTrue(controller.bursts.isEmpty());
    assertTrue(controller.disappearances.isEmpty());
    assertEquals(0, controller.getChargeCount());
    spawnUpdate(0f);
    assertEquals(1, controller.pickups.size());
    controller.pickups.getFirst().elapsed = config.icePickupLifetime;
    collectUpdate(0f, DISTANT_PLAYER, null);
    assertEquals(1, controller.disappearances.size());
    controller.dispose();
    controller.dispose();
    spawnUpdate(100f);
    assertTrue(controller.pickups.isEmpty());
    assertTrue(controller.bursts.isEmpty());
    assertTrue(controller.disappearances.isEmpty());
    assertEquals(0, controller.getChargeCount());
    assertNull(controller.getArenaBounds());
  }

  private void collectOne() {
    addPickup(10f, 6f);
    collectUpdate(0f, boundsAt(10f, 6f, 1f), null);
  }

  private FinalBossStageTwoPickupController.Pickup addPickup(float x, float y) {
    var pickup = new FinalBossStageTwoPickupController.Pickup(new Vector2(x, y));
    controller.pickups.add(pickup);
    return pickup;
  }

  private void spawnUpdate(float delta) {
    controller.update(delta, ARENA, DISTANT_BOSS, DISTANT_PLAYER, null, area -> true);
  }

  private void collectUpdate(float delta, Rectangle player, Vector2 previous) {
    controller.update(delta, ARENA, DISTANT_BOSS, player, previous, area -> false);
  }

  private static Rectangle boundsAt(float x, float y, float size) {
    return new Rectangle(x - size / 2f, y - size / 2f, size, size);
  }

  private static Rectangle gemBounds(Vector2 centre, float radius) {
    return new Rectangle(centre.x - radius, centre.y - radius, radius * 2f, radius * 2f);
  }

  private static class FixedRandom extends Random {
    private final float value;

    FixedRandom(float value) {
      this.value = value;
    }

    @Override
    public float nextFloat() {
      return value;
    }
  }
}
