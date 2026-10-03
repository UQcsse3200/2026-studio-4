package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Projectile behavior is tested without a renderer, physics world or global service state. */
class FinalBossStageTwoFireControllerTest {
  private static final float EPSILON = 0.0001f;
  private static final Rectangle ARENA = new Rectangle(0f, 0f, 10f, 10f);
  private static final Rectangle LARGE_ARENA = new Rectangle(-1000f, -1000f, 2000f, 2000f);
  private static final Vector2 ORIGIN = new Vector2(5f, 5f);
  private static final Vector2 DISTANT_PLAYER = new Vector2(500f, 500f);
  private static final FinalBossStageTwoFireController.WallQuery NO_WALL =
      (from, to) -> Float.POSITIVE_INFINITY;

  private FinalBossStageTwoConfig config;
  private FinalBossStageTwoFireController controller;

  @BeforeEach
  void setUp() {
    config = new FinalBossStageTwoConfig();
    controller = new FinalBossStageTwoFireController(config, new Random(731L));
  }

  @Test
  void waitsForTheInitialDelayAndThenUsesTheVolleyInterval() {
    update(0.5f, true);
    assertTrue(controller.fireballs.isEmpty());
    update(0.11f, true);
    int firstVolley = controller.fireballs.size();
    assertTrue(firstVolley >= 4 && firstVolley <= 6);
    assertTrue(controller.getCastRemaining() > 0f);

    update(config.fireVolleyInterval - 0.1f, true);
    assertEquals(firstVolley, controller.fireballs.size());
    update(0.11f, true);
    assertTrue(controller.fireballs.size() > firstVolley);
  }

  @Test
  void allThreePatternsStartAtTheCurrentOriginAndHaveIndependentConstantSpeedBalls() {
    int[] expectedCounts = {6, 4, 5};
    for (int pattern = 0; pattern < expectedCounts.length; pattern++) {
      FinalBossStageTwoFireController patterned =
          new FinalBossStageTwoFireController(config, new PatternRandom(pattern));
      patterned.update(
          0.61f,
          true,
          ORIGIN,
          0f,
          new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
          LARGE_ARENA,
          NO_WALL);

      assertEquals(expectedCounts[pattern], patterned.fireballs.size());
      for (FinalBossStageTwoFireController.Fireball ball : patterned.fireballs) {
        assertEquals(ORIGIN, ball.position);
        assertEquals(config.fireballSpeed, ball.velocity.len(), EPSILON);
        assertNotSame(ORIGIN, ball.position);
      }
      assertNotSame(patterned.fireballs.get(0).position, patterned.fireballs.get(1).position);
      assertNotEquals(patterned.fireballs.get(0).velocity, patterned.fireballs.get(1).velocity);
    }
  }

  @Test
  void laterVolleysUseTheBossNewPositionInsteadOfTheFirstSpawnPoint() {
    update(0.61f, true);
    int earlierCount = controller.fireballs.size();
    Vector2 movedOrigin = new Vector2(7f, 8f);

    controller.update(
        config.fireVolleyInterval + 0.01f,
        true,
        movedOrigin,
        0f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        LARGE_ARENA,
        NO_WALL);

    assertTrue(controller.fireballs.size() > earlierCount);
    for (int i = earlierCount; i < controller.fireballs.size(); i++) {
      assertEquals(movedOrigin, controller.fireballs.get(i).position);
    }
  }

  @Test
  void pausedFiringDoesNotRemoveOrStopExistingFireballs() {
    FinalBossStageTwoFireController.Fireball ball = addBall(1L, 1f, 5f, 4f, 0f);

    update(0.5f, false);

    assertEquals(List.of(ball), controller.fireballs);
    assertEquals(new Vector2(3f, 5f), ball.position);
    assertEquals(0.5f, ball.elapsed, EPSILON);
    assertEquals(new Vector2(4f, 0f), ball.velocity);
  }

  @Test
  void aLongPauseDoesNotAccumulateVolleysForTheNextFiringWindow() {
    update(30f, false);
    assertTrue(controller.fireballs.isEmpty());
    update(0.01f, true);
    int resumedCount = controller.fireballs.size();
    assertTrue(resumedCount >= 4 && resumedCount <= 6);
    update(0.1f, true);
    assertEquals(resumedCount, controller.fireballs.size());
  }

  @Test
  void aLongFrameEmitsAtMostOneVolleyAndHonoursTheGlobalProjectileCap() {
    update(1000f, true);
    assertTrue(controller.fireballs.size() >= 4 && controller.fireballs.size() <= 6);

    config.maxFireballs = 10;
    config.fireballLifetime = 1000f;
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    update(0.61f, true);
    assertEquals(6, controller.fireballs.size());
    update(config.fireVolleyInterval + 0.01f, true);
    assertEquals(10, controller.fireballs.size());
    update(config.fireVolleyInterval + 0.01f, true);
    assertEquals(10, controller.fireballs.size());
  }

  @Test
  void aFastBulletHitsAPlayerBetweenItsTwoFramePositionsOnlyOnce() {
    addBall(11L, 1f, 5f, 8f, 0f);
    FinalBossStageTwoFireController.Fireball miss = addBall(12L, 1f, 8f, 1f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(5f, 5f), new Vector2(5f, 5f), NO_WALL, hits::incrementAndGet);
    updateAgainstPlayer(
        1f, new Vector2(5f, 5f), new Vector2(5f, 5f), NO_WALL, hits::incrementAndGet);

    assertEquals(1, hits.get());
    assertEquals(List.of(miss), controller.fireballs);
  }

  @Test
  void aMovingPlayerCrossingAStationaryBulletIsDetected() {
    addBall(21L, 5f, 5f, 0f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(3f, 5f), new Vector2(7f, 5f), NO_WALL, hits::incrementAndGet);

    assertEquals(1, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertEquals(new Vector2(5f, 5f), controller.impacts.getFirst().position);
  }

  @Test
  void aPlayerBeforeTheWallIsHitBeforeTheBulletReachesTheWall() {
    addBall(31L, 1f, 5f, 8f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(4f, 5f), new Vector2(4f, 5f), verticalWall(6f), hits::incrementAndGet);

    assertEquals(1, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.getFirst().position.x < 4f);
  }

  @Test
  void theWallBlocksDamageToAPlayerBehindIt() {
    addBall(41L, 1f, 5f, 8f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(6f, 5f), new Vector2(6f, 5f), verticalWall(3f), hits::incrementAndGet);

    assertEquals(0, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertEquals(new Vector2(3f, 5f), controller.impacts.getFirst().position);
  }

  @Test
  void touchingAWallTakesPriorityOverAPlayerAtTheSameHitFraction() {
    addBall(51L, 1f, 5f, 8f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(1f, 5f), new Vector2(1f, 5f), (from, to) -> 0f, hits::incrementAndGet);

    assertEquals(0, hits.get());
    assertTrue(controller.fireballs.isEmpty());
  }

  @Test
  void theArenaBoundaryStopsFlightBeforeAnOutsideTarget() {
    addBall(61L, 1f, 5f, 12f, 0f);
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(11f, 5f), new Vector2(11f, 5f), NO_WALL, hits::incrementAndGet);

    assertEquals(0, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void expiryStopsFlightBeforeItCanReachALaterTarget() {
    FinalBossStageTwoFireController.Fireball ball = addBall(71L, 1f, 5f, 8f, 0f);
    ball.elapsed = config.fireballLifetime - 0.1f;
    AtomicInteger hits = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(5f, 5f), new Vector2(5f, 5f), NO_WALL, hits::incrementAndGet);

    assertEquals(0, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void firingFromInsideASolidWallIsRejected() {
    controller.update(
        1f,
        true,
        ORIGIN,
        0f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        ARENA,
        (from, to) -> from.equals(ORIGIN) ? 0f : Float.POSITIVE_INFINITY);

    assertTrue(controller.fireballs.isEmpty());
    assertEquals(0f, controller.getCastRemaining());
  }

  @Test
  void aHitConsumesItsBulletEvenIfTheDamageCallbackDoesNotReduceHealth() {
    addBall(81L, 1f, 5f, 8f, 0f);
    AtomicInteger blockedAttempts = new AtomicInteger();

    updateAgainstPlayer(
        1f, new Vector2(5f, 5f), new Vector2(5f, 5f), NO_WALL, blockedAttempts::incrementAndGet);

    assertEquals(1, blockedAttempts.get());
    assertTrue(controller.fireballs.isEmpty());
    assertEquals(1, controller.impacts.size());
    update(config.fireImpactDuration, false);
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void damageCanClearTheEncounterWithoutFurtherHitsOrAReplacementVolley() {
    addBall(91L, 1f, 5f, 8f, 0f);
    addBall(92L, 1f, 5f, 8f, 0f);
    AtomicInteger hits = new AtomicInteger();

    controller.update(
        1f,
        true,
        ORIGIN,
        0f,
        new FinalBossStageTwoProjectileTarget(
            ORIGIN,
            ORIGIN,
            0.25f,
            () -> {
              hits.incrementAndGet();
              assertEquals(1, controller.fireballs.size());
              controller.clear();
            }),
        ARENA,
        NO_WALL);

    assertEquals(1, hits.get());
    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.isEmpty());
    assertEquals(0f, controller.getCastRemaining());
  }

  @Test
  void consumingAnIdRemovesOnlyThatProjectileAndCannotConsumeAFutureVolley() {
    update(0.61f, true);
    long consumedId = controller.fireballs.getFirst().id;
    int count = controller.fireballs.size();

    assertTrue(controller.consume(consumedId));
    assertFalse(controller.consume(consumedId));
    assertEquals(count - 1, controller.fireballs.size());
    controller.clear();
    update(0.5f, true);
    assertTrue(controller.fireballs.isEmpty());
    update(0.11f, true);
    assertTrue(controller.fireballs.stream().allMatch(ball -> ball.id > consumedId));
    assertFalse(controller.consume(consumedId));
  }

  @Test
  void missingArenaClearsAllProjectilesAndEffects() {
    addBall(101L, 1f, 5f, 4f, 0f);
    controller.impacts.add(new FinalBossStageTwoFireController.Impact(ORIGIN));

    controller.update(
        0.1f,
        true,
        ORIGIN,
        0f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        null,
        NO_WALL);

    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void invalidTimeDoesNotMoveFireballsOrGenerateShots() {
    FinalBossStageTwoFireController.Fireball ball = addBall(111L, 1f, 5f, 4f, 0f);
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      update(delta, true);
    }

    assertEquals(List.of(ball), controller.fireballs);
    assertEquals(new Vector2(1f, 5f), ball.position);
    assertEquals(0f, ball.elapsed);
  }

  @Test
  void sharedGeometryKeepsHeadsJustOutsideTheShieldAtDifferentBossSizes() {
    assertEquals(1.55f, FinalBossStageTwoFireGeometry.spawnRadius(new Vector2(2f, 2f)), EPSILON);
    for (Vector2 size : List.of(new Vector2(1f, 1f), new Vector2(2f, 4f), new Vector2(4f, 2f))) {
      Vector2 originalSize = size.cpy();
      float shieldRadius =
          Math.max(size.x, size.y) * FinalBossStageTwoFireGeometry.SHIELD_SCALE / 2f;
      assertEquals(0.3f, FinalBossStageTwoFireGeometry.spawnRadius(size) - shieldRadius, EPSILON);
      assertEquals(originalSize, size);
    }
    assertEquals(
        FinalBossStageTwoFireGeometry.spawnRadius(new Vector2(4f, 2f)),
        FinalBossStageTwoFireGeometry.spawnRadius(new Vector2(2f, 4f)));
  }

  @Test
  void allPatternsStartOnTheOuterRingOfTheCurrentBossSizeAndPosition() {
    int[] expectedCounts = {6, 4, 5};
    for (int pattern = 0; pattern < expectedCounts.length; pattern++) {
      var patterned = new FinalBossStageTwoFireController(config, new PatternRandom(pattern));
      Vector2 origin = new Vector2(12f * pattern - 8f, 3f * pattern + 2f);
      float radius = FinalBossStageTwoFireGeometry.spawnRadius(new Vector2(2f, pattern + 1f));
      patterned.update(
          0.61f,
          true,
          origin,
          radius,
          new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
          LARGE_ARENA,
          NO_WALL);

      assertEquals(expectedCounts[pattern], patterned.fireballs.size());
      for (var ball : patterned.fireballs) {
        assertEquals(radius, origin.dst(ball.position), EPSILON);
        assertTrue(
            ball.position
                .cpy()
                .sub(origin)
                .nor()
                .epsilonEquals(ball.velocity.cpy().nor(), EPSILON));
        assertEquals(config.fireballSpeed, ball.velocity.len(), EPSILON);
        assertEquals(0f, ball.elapsed);
        assertNotSame(origin, ball.position);
      }
      assertNotSame(patterned.fireballs.get(0).position, patterned.fireballs.get(1).position);
      assertNotSame(patterned.fireballs.get(0).velocity, patterned.fireballs.get(1).velocity);
    }
  }

  @Test
  void emittedBallsContinueFromTheirRingPositionsWithoutFollowingTheBoss() {
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    Vector2 origin = new Vector2(8f, 7f);
    controller.update(
        0.61f,
        true,
        origin,
        2f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        LARGE_ARENA,
        NO_WALL);
    List<Vector2> initial = controller.fireballs.stream().map(ball -> ball.position.cpy()).toList();
    origin.set(100f, 100f);

    controller.update(
        0.5f,
        false,
        origin,
        9f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        LARGE_ARENA,
        NO_WALL);

    for (int index = 0; index < initial.size(); index++) {
      var ball = controller.fireballs.get(index);
      assertTrue(
          initial
              .get(index)
              .cpy()
              .mulAdd(ball.velocity, 0.5f)
              .epsilonEquals(ball.position, EPSILON));
      assertEquals(0.5f, ball.elapsed, EPSILON);
    }
    Vector2 second = controller.fireballs.get(1).position.cpy();
    controller.fireballs.getFirst().position.set(-50f, -50f);
    assertEquals(second, controller.fireballs.get(1).position);
  }

  @Test
  void blockedSpawnDirectionsCannotTeleportPastWallsOrDamageTheBlockingCover() {
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    AtomicInteger coverHits = new AtomicInteger();
    FinalBossStageTwoFireController.WallQuery wall =
        new FinalBossStageTwoFireController.WallQuery() {
          @Override
          public float firstHitFraction(Vector2 from, Vector2 to) {
            return verticalWall(5.5f).firstHitFraction(from, to);
          }

          @Override
          public void onHit(Vector2 from, Vector2 to) {
            coverHits.incrementAndGet();
          }
        };
    controller.update(
        0.61f,
        true,
        ORIGIN,
        2f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        ARENA,
        wall);

    assertEquals(3, controller.fireballs.size());
    assertTrue(controller.fireballs.stream().allMatch(ball -> ball.position.x < 5.5f));
    assertEquals(0, coverHits.get());
    assertTrue(controller.impacts.isEmpty());
  }

  @Test
  void fullyBlockedRingEmitsNeitherProjectilesNorImpactEffects() {
    AtomicInteger coverHits = new AtomicInteger();
    FinalBossStageTwoFireController.WallQuery enclosure =
        new FinalBossStageTwoFireController.WallQuery() {
          @Override
          public float firstHitFraction(Vector2 from, Vector2 to) {
            return from.equals(to) ? Float.POSITIVE_INFINITY : 0.5f;
          }

          @Override
          public void onHit(Vector2 from, Vector2 to) {
            coverHits.incrementAndGet();
          }
        };
    controller.update(
        0.61f,
        true,
        ORIGIN,
        2f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        ARENA,
        enclosure);

    assertTrue(controller.fireballs.isEmpty());
    assertTrue(controller.impacts.isEmpty());
    assertEquals(0, coverHits.get());
    assertEquals(0f, controller.getCastRemaining());
  }

  @Test
  void theOuterRingSkipsDirectionsWhoseSpawnWouldBeOutsideTheArena() {
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    Vector2 nearEdge = new Vector2(9.5f, 5f);
    controller.update(
        0.61f,
        true,
        nearEdge,
        2f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        ARENA,
        NO_WALL);

    assertEquals(3, controller.fireballs.size());
    for (var ball : controller.fireballs) {
      assertTrue(ARENA.contains(ball.position));
      assertTrue(ball.velocity.x < 0f);
    }
    controller.clear();
    controller.update(
        0.61f,
        true,
        ORIGIN,
        100f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        ARENA,
        NO_WALL);
    assertTrue(controller.fireballs.isEmpty());
  }

  @Test
  void invisiblePlacementPathDoesNotHitPlayerButVisibleFlightStillDoes() {
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    AtomicInteger hits = new AtomicInteger();
    Vector2 insideRing = new Vector2(4f, 5f);
    controller.update(
        0.61f,
        true,
        ORIGIN,
        2f,
        new FinalBossStageTwoProjectileTarget(insideRing, insideRing, 0.25f, hits::incrementAndGet),
        ARENA,
        NO_WALL);
    assertEquals(0, hits.get(), "The left spawn skips over x=4 without being a damaging sweep");
    assertEquals(6, controller.fireballs.size());

    Vector2 outsideRing = new Vector2(2f, 5f);
    controller.update(
        0.5f,
        false,
        ORIGIN,
        2f,
        new FinalBossStageTwoProjectileTarget(
            outsideRing, outsideRing, 0.25f, hits::incrementAndGet),
        ARENA,
        NO_WALL);
    assertEquals(1, hits.get());
    assertEquals(5, controller.fireballs.size());
    assertEquals(1, controller.impacts.size());
  }

  @Test
  void invalidSpawnRadiusSuppressesNewShotsWhileExistingShotsKeepFlying() {
    var ball = addBall(900, 1f, 5f, 1f, 0f);
    for (float radius :
        new float[] {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
      float previousX = ball.position.x;
      controller.update(
          0.61f,
          true,
          ORIGIN,
          radius,
          new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
          LARGE_ARENA,
          NO_WALL);
      assertEquals(List.of(ball), controller.fireballs);
      assertTrue(ball.position.x > previousX);
    }
  }

  @Test
  void blockedDirectionsDoNotWasteTheRemainingProjectileCapacity() {
    config.maxFireballs = 8;
    controller = new FinalBossStageTwoFireController(config, new PatternRandom(0));
    for (int index = 0; index < 6; index++) addBall(100 + index, 30f + index, 30f, 0f, 0f);
    controller.update(
        0.61f,
        true,
        ORIGIN,
        2f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        LARGE_ARENA,
        verticalWall(4.5f));

    assertEquals(8, controller.fireballs.size());
    assertTrue(
        controller.fireballs.stream()
            .filter(ball -> ball.id < 100)
            .allMatch(ball -> ball.position.x > ORIGIN.x));
  }

  private void update(float delta, boolean canFire) {
    controller.update(
        delta,
        canFire,
        ORIGIN,
        0f,
        new FinalBossStageTwoProjectileTarget(DISTANT_PLAYER, DISTANT_PLAYER, 0.25f, () -> {}),
        LARGE_ARENA,
        NO_WALL);
  }

  private void updateAgainstPlayer(
      float delta,
      Vector2 before,
      Vector2 now,
      FinalBossStageTwoFireController.WallQuery walls,
      Runnable hit) {
    controller.update(
        delta,
        false,
        ORIGIN,
        0f,
        new FinalBossStageTwoProjectileTarget(before, now, 0.25f, hit),
        ARENA,
        walls);
  }

  private FinalBossStageTwoFireController.Fireball addBall(
      long id, float x, float y, float vx, float vy) {
    FinalBossStageTwoFireController.Fireball ball =
        new FinalBossStageTwoFireController.Fireball(id, new Vector2(x, y), new Vector2(vx, vy));
    controller.fireballs.add(ball);
    return ball;
  }

  private static FinalBossStageTwoFireController.WallQuery verticalWall(float x) {
    return (from, to) -> {
      if (from.x == x) return 0f;
      if (to.x == from.x) return Float.POSITIVE_INFINITY;
      float fraction = (x - from.x) / (to.x - from.x);
      return fraction >= 0f && fraction <= 1f ? fraction : Float.POSITIVE_INFINITY;
    };
  }

  private static final class PatternRandom extends Random {
    private final int pattern;

    private PatternRandom(int pattern) {
      this.pattern = pattern;
    }

    @Override
    public int nextInt(int bound) {
      return pattern;
    }

    @Override
    public float nextFloat() {
      return 0.5f;
    }
  }
}
