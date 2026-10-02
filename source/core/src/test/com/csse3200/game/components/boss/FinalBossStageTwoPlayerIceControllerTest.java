package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinalBossStageTwoPlayerIceControllerTest {
  private static final float EPSILON = 0.0001f;
  private static final Rectangle ARENA = new Rectangle(-1000f, -1000f, 2000f, 2000f);
  private static final Vector2 ORIGIN = new Vector2(0f, 0f);
  private static final Vector2 DISTANT_BOSS = new Vector2(500f, 0f);
  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoPlayerIceController controller;
  private FinalBossStageTwoPickupController energy;

  @BeforeEach
  void setup() {
    config = new FinalBossStageTwoConfig();
    config.icePickupCount = 0;
    controller = new FinalBossStageTwoPlayerIceController(config);
    energy = new FinalBossStageTwoPickupController(config, new Random(1));
  }

  @Test
  void firstHeldShotAimsAtBossAndIsBornAtTheEndOfTheFrame() {
    addCharge();
    Vector2 target = new Vector2(3f, 4f);
    update(0.01f, true, target, target, null, () -> {});

    var shot = controller.shots.getFirst();
    assertEquals(ORIGIN, shot.position);
    assertNotSame(ORIGIN, shot.position);
    assertEquals(config.iceProjectileSpeed, shot.velocity.len(), EPSILON);
    assertEquals(
        new Vector2(3f, 4f).nor().scl(config.iceProjectileSpeed).x, shot.velocity.x, EPSILON);
    assertEquals(
        new Vector2(3f, 4f).nor().scl(config.iceProjectileSpeed).y, shot.velocity.y, EPSILON);
    assertEquals(0f, shot.elapsed);
    assertEquals(15f / 16f, energy.getChargeFraction(0));
  }

  @Test
  void homingTurnsByAtMostTheConfiguredRateAndPreservesSpeed() {
    var shot = addShot(1, 0f, 0f, config.iceProjectileSpeed, 0f);
    Vector2 target = new Vector2(0f, 100f);
    update(0.1f, false, target, target, null, () -> {});
    assertEquals(config.iceHomingTurnRate * 0.1f, shot.velocity.angleDeg(), 0.01f);
    assertEquals(config.iceProjectileSpeed, shot.velocity.len(), EPSILON);
    assertEquals(config.iceProjectileSpeed * 0.1f, shot.position.len(), EPSILON);
  }

  @Test
  void homingUsesTheShortTurnAcrossTheZeroDegreeBoundary() {
    Vector2 velocity = new Vector2(config.iceProjectileSpeed, 0f).rotateDeg(350f);
    var shot = new FinalBossStageTwoPlayerIceController.Shot(1, ORIGIN, velocity);
    controller.shots.add(shot);
    Vector2 target = new Vector2(100f, 0f).rotateDeg(10f);
    update(0.1f, false, target, target, null, () -> {});
    assertEquals(10f, shot.velocity.angleDeg(), 0.01f);
  }

  @Test
  void releasingOrRunningOutOfEnergyDoesNotStopExistingShots() {
    var shot = addShot(1, 0f, 0f, config.iceProjectileSpeed, 0f);
    update(0.25f, false, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    float firstX = shot.position.x;
    update(0.25f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    assertTrue(shot.position.x > firstX);
    assertEquals(0, energy.getChargeCount());
  }

  @Test
  void oneAndTwoChargesProduceExactlySixteenAndThirtyTwoShotsWithoutOverdraw() {
    for (int charges = 1; charges <= 2; charges++) {
      controller.clear();
      energy.clear();
      for (int count = 0; count < charges; count++) addCharge();
      Set<Long> fired = new HashSet<>();
      for (int frame = 0; frame < 40; frame++) {
        update(config.iceFireInterval, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
        controller.shots.forEach(shot -> fired.add(shot.id));
        assertTrue(energy.getChargeFraction(0) >= 0f);
        assertTrue(energy.getChargeFraction(1) >= 0f);
      }
      assertEquals(charges * 16, fired.size());
      assertEquals(0, energy.getChargeCount());
      assertEquals(0f, energy.getChargeFraction(0));
      assertEquals(0f, energy.getChargeFraction(1));
    }
  }

  @Test
  void aShotCanFinishOneSlotAndContinueTheSameCostIntoTheNext() {
    addCharge();
    addCharge();
    energy.consumeEnergy(31f / 32f);
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(0f, energy.getChargeFraction(0));
    assertEquals(31f / 32f, energy.getChargeFraction(1));
    assertEquals(1, controller.shots.size());
  }

  @Test
  void nonbinaryShotCostStillFiresThreeTimesWithoutLeavingAnOccupiedRoundingResidue() {
    config.iceShotsPerCharge = 3;
    addCharge();
    for (int frame = 0; frame < 4; frame++) {
      update(config.iceFireInterval, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    }
    assertEquals(3, controller.shots.size());
    assertEquals(0, energy.getChargeCount());
    assertEquals(0f, energy.getChargeFraction(0));
  }

  @Test
  void insufficientPartialEnergyIsNotOverdrawnOrChargedForAFailedShot() {
    addCharge();
    energy.consumeEnergy(31f / 32f);
    update(1f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertTrue(controller.shots.isEmpty());
    assertEquals(1f / 32f, energy.getChargeFraction(0));
  }

  @Test
  void tappingTheKeyCannotResetCooldownButReleasedTimeStillCounts() {
    addCharge();
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    update(0.01f, false, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    assertEquals(15f / 16f, energy.getChargeFraction(0));
    update(config.iceFireInterval, false, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(2, controller.shots.size());
  }

  @Test
  void longFrameFiresOnlyOneShotAndCreatesNoCatchUpDebt() {
    addCharge();
    update(100f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    assertEquals(0f, controller.shots.getFirst().elapsed);
    assertEquals(15f / 16f, energy.getChargeFraction(0));
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    assertEquals(15f / 16f, energy.getChargeFraction(0));
  }

  @Test
  void capacityFullDoesNotConsumeEnergyAndCanFireWhenTheSlotIsFreed() {
    config.maxIceProjectiles = 1;
    addCharge();
    var occupied = addShot(100, 0f, 0f, config.iceProjectileSpeed, 0f);
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1f, energy.getChargeFraction(0));
    assertTrue(controller.consume(occupied.id));
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertEquals(1, controller.shots.size());
    assertEquals(15f / 16f, energy.getChargeFraction(0));
  }

  @Test
  void invalidOutsideOrWallEmbeddedOriginsNeverSpendEnergy() {
    addCharge();
    for (Vector2 origin :
        new Vector2[] {null, new Vector2(Float.NaN, 0f), new Vector2(2000f, 0f)}) {
      controller.update(
          1f, true, origin, DISTANT_BOSS, DISTANT_BOSS, 0.5f, ARENA, null, () -> {}, energy);
    }
    update(1f, true, DISTANT_BOSS, DISTANT_BOSS, (from, to) -> 0f, () -> {});
    assertTrue(controller.shots.isEmpty());
    assertEquals(1f, energy.getChargeFraction(0));
  }

  @Test
  void nearestWallOrEqualTimeWallWinsWithoutInvokingFireDamageCallback() {
    config.iceProjectileSpeed = 10f;
    config.iceProjectileRadius = 0.5f;
    Vector2 target = new Vector2(6f, 0f); // Combined radius 1: first boss contact at x=5.
    for (float fraction : new float[] {0.3f, 0.5f}) {
      controller.clear();
      addShot(1, 0f, 0f, 10f, 0f);
      AtomicInteger hits = new AtomicInteger();
      AtomicInteger wallDamage = new AtomicInteger();
      FinalBossStageTwoFireController.WallQuery wall =
          new FinalBossStageTwoFireController.WallQuery() {
            @Override
            public float firstHitFraction(Vector2 from, Vector2 to) {
              return fraction;
            }

            @Override
            public void onHit(Vector2 from, Vector2 to) {
              wallDamage.incrementAndGet();
            }
          };
      update(1f, false, target, target, wall, hits::incrementAndGet);
      assertEquals(0, hits.get());
      assertEquals(0, wallDamage.get());
      assertTrue(controller.shots.isEmpty());
      assertEquals(fraction * 10f, controller.impacts.getFirst().position.x, EPSILON);
    }
  }

  @Test
  void bossBeforeTheWallIsHitExactlyOnceAndTheImpactExpires() {
    config.iceProjectileSpeed = 10f;
    config.iceProjectileRadius = 0.5f;
    addShot(1, 0f, 0f, 10f, 0f);
    Vector2 target = new Vector2(6f, 0f);
    AtomicInteger hits = new AtomicInteger();
    update(1f, false, target, target, (from, to) -> 0.8f, hits::incrementAndGet);
    assertEquals(1, hits.get());
    assertTrue(controller.shots.isEmpty());
    assertEquals(5f, controller.impacts.getFirst().position.x, EPSILON);
    update(config.iceImpactDuration, false, target, target, null, hits::incrementAndGet);
    assertEquals(1, hits.get());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void aShotStartingInsideAWallIsConsumedBeforeAnOverlappingBoss() {
    addShot(1, 0f, 0f, config.iceProjectileSpeed, 0f);
    AtomicInteger hits = new AtomicInteger();
    update(0.1f, false, ORIGIN, ORIGIN, (from, to) -> 0f, hits::incrementAndGet);
    assertEquals(0, hits.get());
    assertTrue(controller.shots.isEmpty());
    assertEquals(ORIGIN, controller.impacts.getFirst().position);
  }

  @Test
  void movingBossCrossingTheFlightPathIsDetectedByRelativeSweep() {
    config.iceProjectileSpeed = 10f;
    config.iceHomingTurnRate = 0.001f;
    addShot(1, 0f, 0f, 10f, 0f);
    AtomicInteger hits = new AtomicInteger();
    update(1f, false, new Vector2(5f, -5f), new Vector2(5f, 5f), null, hits::incrementAndGet);
    assertEquals(1, hits.get());
    assertTrue(controller.shots.isEmpty());
  }

  @Test
  void lifetimeWinsAtAnEqualTimeBossHitAndClipsMovementBeforeALaterHit() {
    config.iceProjectileSpeed = 10f;
    config.iceProjectileRadius = 0.5f;
    config.iceProjectileLifetime = 1f;
    for (float bossX : new float[] {11f, 12f}) {
      addShot(1, 0f, 0f, 10f, 0f);
      AtomicInteger hits = new AtomicInteger();
      Vector2 target = new Vector2(bossX, 0f);
      update(2f, false, target, target, null, hits::incrementAndGet);
      assertEquals(0, hits.get());
      assertTrue(controller.shots.isEmpty());
      assertTrue(controller.impacts.isEmpty());
    }
  }

  @Test
  void lifetimeClipsTheBossMotionToTheSamePartOfTheFrame() {
    config.iceProjectileSpeed = 2f;
    config.iceProjectileRadius = 0.1f;
    config.iceHomingTurnRate = 0.001f;
    var shot = addShot(1, 0f, 0f, 2f, 0f);
    shot.elapsed = config.iceProjectileLifetime - 0.2f;
    AtomicInteger hits = new AtomicInteger();
    controller.update(
        1f,
        false,
        ORIGIN,
        new Vector2(0.2f, -1f),
        new Vector2(0.2f, 1f),
        0.1f,
        ARENA,
        null,
        hits::incrementAndGet,
        energy);
    assertEquals(0, hits.get(), "The boss crosses this path only after the shot has expired");
    assertTrue(controller.shots.isEmpty());
  }

  @Test
  void arenaExitWinsOverAnEqualTimeHitOnABossOutsideTheArena() {
    config.iceProjectileSpeed = 10f;
    config.iceProjectileRadius = 0.5f;
    var shot = addShot(1, 9f, 5f, 10f, 0f);
    AtomicInteger hits = new AtomicInteger();
    Vector2 target = new Vector2(12f, 5f);
    controller.update(
        1f,
        false,
        ORIGIN,
        target,
        target,
        1.5f,
        new Rectangle(0f, 0f, 10f, 10f),
        null,
        hits::incrementAndGet,
        energy);
    assertEquals(0, hits.get());
    assertEquals(10f, shot.position.x, EPSILON);
    assertTrue(controller.shots.isEmpty());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void synchronousEncounterClearDuringDamageStopsFurtherHitsAndSpawning() {
    addCharge();
    addShot(100, 0f, 0f, config.iceProjectileSpeed, 0f);
    var second = addShot(101, 0f, 0f, config.iceProjectileSpeed, 0f);
    AtomicInteger hits = new AtomicInteger();
    update(
        0.1f,
        true,
        ORIGIN,
        ORIGIN,
        null,
        () -> {
          hits.incrementAndGet();
          assertEquals(
              1, controller.shots.size(), "The first shot is removed before damage callbacks");
          controller.clear();
        });
    assertEquals(1, hits.get());
    assertTrue(controller.shots.isEmpty());
    assertTrue(controller.impacts.isEmpty());
    assertEquals(ORIGIN, second.position);
    assertEquals(1f, energy.getChargeFraction(0));
  }

  @Test
  void callbackCanConsumeAnotherShotWithoutAdvancingOrDamagingFromIt() {
    addShot(100, 0f, 0f, config.iceProjectileSpeed, 0f);
    var second = addShot(101, 0f, 0f, config.iceProjectileSpeed, 0f);
    AtomicInteger hits = new AtomicInteger();
    update(
        0.1f,
        false,
        ORIGIN,
        ORIGIN,
        null,
        () -> {
          hits.incrementAndGet();
          assertTrue(controller.consume(second.id));
        });
    assertEquals(1, hits.get());
    assertEquals(ORIGIN, second.position);
    assertTrue(controller.shots.isEmpty());
  }

  @Test
  void clearPreservesUniqueIdsAndDisposeStopsAllFutureSpawning() {
    addCharge();
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    long oldId = controller.shots.getFirst().id;
    controller.clear();
    update(0.01f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertTrue(controller.shots.getFirst().id > oldId);
    assertFalse(controller.consume(oldId));
    controller.dispose();
    controller.dispose();
    float remaining = energy.getChargeFraction(0);
    update(100f, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
    assertTrue(controller.shots.isEmpty());
    assertTrue(controller.impacts.isEmpty());
    assertEquals(remaining, energy.getChargeFraction(0));
  }

  @Test
  void invalidDeltaDoesNothingAndMissingArenaClearsProjectiles() {
    addCharge();
    var shot = addShot(100, 0f, 0f, config.iceProjectileSpeed, 0f);
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      update(delta, true, DISTANT_BOSS, DISTANT_BOSS, null, () -> {});
      assertEquals(ORIGIN, shot.position);
      assertEquals(1f, energy.getChargeFraction(0));
    }
    controller.impacts.add(new FinalBossStageTwoPlayerIceController.Impact(ORIGIN));
    controller.update(
        0.1f, false, ORIGIN, DISTANT_BOSS, DISTANT_BOSS, 0.5f, null, null, () -> {}, energy);
    assertTrue(controller.shots.isEmpty());
    assertTrue(controller.impacts.isEmpty());
  }

  private FinalBossStageTwoPlayerIceController.Shot addShot(
      long id, float x, float y, float vx, float vy) {
    var shot =
        new FinalBossStageTwoPlayerIceController.Shot(id, new Vector2(x, y), new Vector2(vx, vy));
    controller.shots.add(shot);
    return shot;
  }

  private void addCharge() {
    energy.pickups.add(new FinalBossStageTwoPickupController.Pickup(ORIGIN));
    energy.update(
        0f,
        ARENA,
        new Rectangle(100f, 100f, 1f, 1f),
        new Rectangle(-0.5f, -0.5f, 1f, 1f),
        ORIGIN,
        area -> false);
  }

  private void update(
      float delta,
      boolean firing,
      Vector2 before,
      Vector2 now,
      FinalBossStageTwoFireController.WallQuery walls,
      Runnable hit) {
    controller.update(delta, firing, ORIGIN, before, now, 0.5f, ARENA, walls, hit, energy);
  }
}
