package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossTornadoControllerTest {
  private static final float EPSILON = 0.0001f;
  private final Rectangle arena = new Rectangle(0f, 0f, 20f, 20f);
  private final List<Vector2> bursts = new ArrayList<>();
  private Entity player;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private GameTime time;
  private FinalBossTornadoController controller;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    stats = new CombatStatsComponent(100, 10);
    effects = new StatusEffectsControllerComponent();
    player = new Entity().addComponent(stats).addComponent(effects);
    player.create();
    movePlayerGroundTo(17f, 17f);
    controller = newController((centre, size) -> true);
  }

  @Test
  void brokenStatuesShouldCreateAtMostFourRemnantsAndCopyTheirSpawnPositions() {
    for (int remaining = 4; remaining > 0; remaining--) {
      Vector2 requested = new Vector2(5f, remaining * 3f + 3f);
      controller.statueBroken(requested, remaining);
      requested.set(-100f, -100f);
      assertEquals(5 - remaining, controller.activeCount());
    }
    controller.statueBroken(new Vector2(10f, 10f), 1);

    assertEquals(4, controller.items.size());
    assertEquals(4, controller.activeCount());
    assertTrue(controller.items.stream().allMatch(item -> item.position.x == 5f));
    assertTrue(bursts.isEmpty());
  }

  @Test
  void finalStatueShouldBurstAndDissolveAllFourWithoutMovingOrCreatingAFifth() {
    for (int remaining = 4; remaining > 0; remaining--) {
      controller.statueBroken(new Vector2(5f, remaining * 3f + 3f), remaining);
    }
    controller.update(1f, true);
    List<Vector2> positions = controller.items.stream().map(item -> item.position.cpy()).toList();

    controller.statueBroken(new Vector2(10f, 10f), 0);
    assertEquals(4, controller.items.size());
    assertEquals(0, controller.activeCount());
    assertEquals(4, bursts.size());
    for (int i = 0; i < controller.items.size(); i++) {
      FinalBossTornadoController.Tornado item = controller.items.get(i);
      assertTrue(item.dissolving);
      assertFalse(item.chasing);
      assertNull(item.destination);
      assertEquals(
          positions.get(i).cpy().add(0f, FinalBossTornadoController.HEIGHT * 0.5f), bursts.get(i));
    }

    controller.update(0.3f, true);
    assertEquals(positions, controller.items.stream().map(item -> item.position).toList());
    controller.statueBroken(new Vector2(10f, 10f), 0);
    assertEquals(4, bursts.size());
    controller.update(0.31f, true);
    assertTrue(controller.items.isEmpty());
    controller.statueBroken(new Vector2(5f, 5f), 1);
    assertTrue(controller.items.isEmpty());
  }

  @Test
  void finalStatueWithoutExistingRemnantsShouldNotSpawnOne() {
    controller.statueBroken(new Vector2(5f, 5f), 0);
    assertEquals(0, controller.activeCount());
    assertTrue(controller.items.isEmpty());
    assertTrue(bursts.isEmpty());
  }

  @Test
  void spawnAnimationAndPausedCombatShouldNotMoveTheRemnant() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    movePlayerGroundTo(7f, 5f);
    Vector2 original = item.position.cpy();

    controller.update(FinalBossTornadoController.SPAWN_DURATION - 0.01f, true);
    assertEquals(original, item.position);
    controller.update(0.02f, true);
    assertTrue(item.chasing);
    assertTrue(item.position.x > original.x);
    assertTrue(
        original.dst(item.position) <= FinalBossTornadoController.CHASE_SPEED * 0.02f + EPSILON);
    Vector2 beforePause = item.position.cpy();
    controller.update(10f, false);
    assertEquals(beforePause, item.position);
  }

  @Test
  void farRemnantsShouldWanderSlowlyInsideTheArenaWithoutExactRandomPaths() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    float distanceTravelled = 0f;
    for (int frame = 0; frame < 100; frame++) {
      Vector2 previous = item.position.cpy();
      controller.update(0.1f, true);
      float distance = previous.dst(item.position);
      distanceTravelled += distance;
      assertFalse(item.chasing);
      assertTrue(distance <= FinalBossTornadoController.WANDER_SPEED * 0.1f + EPSILON);
      assertInsideArena(item.position);
    }
    assertTrue(distanceTravelled > 1f);
  }

  @Test
  void chaseShouldUseSeparateEnterAndReleaseRadiiWithoutOscillatingAtTheBoundary() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    movePlayerGroundTo(7f, 5f);
    controller.update(0.6f, true);
    assertTrue(item.chasing);

    movePlayerGroundTo(item.position.x + 3f, item.position.y);
    Vector2 previous = item.position.cpy();
    controller.update(0.1f, true);
    assertTrue(item.chasing);
    assertEquals(
        FinalBossTornadoController.CHASE_SPEED * 0.1f, previous.dst(item.position), EPSILON);

    movePlayerGroundTo(item.position.x + 3.6f, item.position.y);
    controller.update(0.1f, true);
    assertFalse(item.chasing);
    movePlayerGroundTo(item.position.x + 3f, item.position.y);
    controller.update(0.1f, true);
    assertFalse(item.chasing);
    movePlayerGroundTo(item.position.x + 2.4f, item.position.y);
    controller.update(0.1f, true);
    assertTrue(item.chasing);
  }

  @Test
  void invisibilityShouldReleaseThePlayerUntilTheEffectExpires() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    movePlayerGroundTo(7f, 5f);
    controller.update(0.6f, true);
    assertTrue(item.chasing);

    effects.addStatusEffect(new InvisibilityEffect(time, 1000L));
    controller.update(0.1f, true);
    assertFalse(item.chasing);
    when(time.getTime()).thenReturn(1000L);
    movePlayerGroundTo(item.position.x + 2f, item.position.y);
    controller.update(0.1f, true);
    assertTrue(item.chasing);
  }

  @Test
  void sweptMovementShouldNotTunnelThroughAThinWallEvenAfterALongFrame() {
    Rectangle wall = new Rectangle(6f, 0f, 0.02f, arena.height);
    controller =
        newController(
            (centre, size) ->
                !wall.overlaps(
                    new Rectangle(
                        centre.x - size.x * 0.5f, centre.y - size.y * 0.5f, size.x, size.y)));
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    movePlayerGroundTo(7f, 5f);

    for (int frame = 0; frame < 60; frame++) {
      Vector2 previous = item.position.cpy();
      controller.update(10f, true);
      assertTrue(item.position.x < wall.x);
      assertTrue(
          previous.dst(item.position) <= FinalBossTornadoController.CHASE_SPEED * 0.25f + EPSILON);
      assertInsideArena(item.position);
    }
  }

  @Test
  void spawnShouldClampToBoundsOrSkipAnArenaWithNoFreeSpace() {
    controller.statueBroken(new Vector2(-100f, -100f), 4);
    controller.statueBroken(new Vector2(100f, 100f), 3);
    assertEquals(2, controller.activeCount());
    controller.items.forEach(item -> assertInsideArena(item.position));

    FinalBossTornadoController blocked = newController((centre, size) -> false);
    blocked.statueBroken(new Vector2(5f, 5f), 4);
    assertTrue(blocked.items.isEmpty());
  }

  @Test
  void playerDeathAndExplicitCleanupShouldRemoveRemnantsAndRejectLaterSpawns() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    stats.setHealth(0);
    controller.update(0.1f, true);
    assertTrue(controller.items.isEmpty());
    controller.statueBroken(new Vector2(5f, 5f), 3);
    assertTrue(controller.items.isEmpty());

    stats.setHealth(100);
    FinalBossTornadoController other = newController((centre, size) -> true);
    other.statueBroken(new Vector2(5f, 5f), 4);
    other.clear();
    other.update(0.1f, true);
    other.statueBroken(new Vector2(5f, 5f), 3);
    assertTrue(other.items.isEmpty());
    assertTrue(bursts.isEmpty());
  }

  @Test
  void invalidDeltaShouldNotCorruptTheAnimationOrPosition() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    for (float delta : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      controller.update(delta, true);
      assertEquals(new Vector2(5f, 5f), item.position);
      assertEquals(0f, item.elapsed);
    }
  }

  @Test
  void statuePathShouldCopyItsEndpointsAndNotExposeMutableCoordinates() {
    Vector2 from = new Vector2(5f, 5f);
    Vector2 to = new Vector2(7f, 5f);
    FinalBossTornadoController.StatuePath path =
        new FinalBossTornadoController.StatuePath(from, to);
    from.set(0f, 0f);
    to.set(0f, 0f);
    path.from().set(0f, 0f);
    path.to().set(0f, 0f);
    assertEquals(new Vector2(5f, 5f), path.from());
    assertEquals(new Vector2(7f, 5f), path.to());
  }

  @Test
  void spawnShouldKeepStatueAndPeerClearanceEvenAtTheSameRequestedLocation() {
    Vector2 statue = new Vector2(10f, 10f);
    controller.setStatuePaths(
        () -> List.of(new FinalBossTornadoController.StatuePath(statue, statue)));
    for (int remaining = 4; remaining > 0; remaining--) {
      controller.statueBroken(statue, remaining);
    }
    assertEquals(4, controller.activeCount());
    for (int i = 0; i < controller.items.size(); i++) {
      Vector2 position = controller.items.get(i).position;
      assertTrue(position.dst(statue) >= FinalBossTornadoController.STATUE_CLEARANCE - EPSILON);
      assertInsideArena(position);
      for (int j = i + 1; j < controller.items.size(); j++) {
        assertTrue(
            position.dst(controller.items.get(j).position)
                >= FinalBossTornadoController.PEER_CLEARANCE - EPSILON);
      }
    }
  }

  @Test
  void spawnShouldSearchTheWholeCameraWhenOnlyDistantSpaceIsFree() {
    controller = newController((centre, size) -> centre.x > 14f);
    controller.statueBroken(new Vector2(3f, 5f), 4);
    assertEquals(1, controller.activeCount());
    assertTrue(controller.items.getFirst().position.x > 14f);
    assertInsideArena(controller.items.getFirst().position);
  }

  @Test
  void wanderingAndChasingShouldAvoidAReservedStatueRoute() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    controller.setStatuePaths(
        () ->
            List.of(
                new FinalBossTornadoController.StatuePath(
                    new Vector2(8f, 0f), new Vector2(8f, 20f))));
    // A cached destination that predates the reservation must not override the new route.
    item.destination = new Vector2(12f, 5f);
    item.retargetRemaining = 100f;
    movePlayerGroundTo(17f, 17f);
    for (int frame = 0; frame < 30; frame++) {
      controller.update(0.1f, true);
      assertTrue(8f - item.position.x >= FinalBossTornadoController.STATUE_CLEARANCE - EPSILON);
    }
    // A nearby player inside the reserved corridor cannot lure the tornado through it.
    movePlayerGroundTo(item.position.x + 2f, item.position.y);
    for (int frame = 0; frame < 30; frame++) {
      controller.update(0.1f, true);
      assertTrue(8f - item.position.x >= FinalBossTornadoController.STATUE_CLEARANCE - EPSILON);
    }
  }

  @Test
  void statueRoutesShouldRejectCrossingsAndEvadesButAllowRetreatFromAnIntrusion() {
    controller.statueBroken(new Vector2(10f, 10f), 4);
    assertFalse(controller.clearStatueRoute(new Vector2(5f, 10f), new Vector2(15f, 10f)));
    assertTrue(controller.clearStatueRoute(new Vector2(5f, 13f), new Vector2(15f, 13f)));
    assertFalse(controller.clearStatuePosition(new Vector2(11f, 10f)));
    assertTrue(controller.clearStatuePosition(new Vector2(13f, 10f)));
    assertTrue(controller.clearStatueRoute(new Vector2(11f, 10f), new Vector2(13f, 10f)));
    assertFalse(controller.clearStatueRoute(new Vector2(11f, 10f), new Vector2(10.5f, 10f)));
    assertFalse(controller.clearStatueRoute(new Vector2(11f, 10f), new Vector2(8f, 10f)));
  }

  @Test
  void anIntrudingStatueShouldCauseASlowRetreatInsteadOfPermanentFreezing() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    Vector2 statue = new Vector2(6f, 5f);
    controller.setStatuePaths(
        () -> List.of(new FinalBossTornadoController.StatuePath(statue, statue)));
    float previousClearance = item.position.dst(statue);
    controller.update(0.6f, true);
    assertFalse(item.chasing);
    assertTrue(item.position.dst(statue) > previousClearance);
    for (int frame = 0; frame < 30; frame++) {
      previousClearance = item.position.dst(statue);
      Vector2 previous = item.position.cpy();
      controller.update(0.1f, true);
      assertTrue(
          item.position.dst(statue)
              >= Math.min(previousClearance, FinalBossTornadoController.STATUE_CLEARANCE)
                  - EPSILON);
      assertTrue(
          previous.dst(item.position) <= FinalBossTornadoController.WANDER_SPEED * 0.1f + EPSILON);
    }
    assertTrue(item.position.dst(statue) >= FinalBossTornadoController.STATUE_CLEARANCE - EPSILON);
  }

  @Test
  void retreatShouldPauseWhenEverySafeDirectionIsBlockedThenRecoverWhenSpaceOpens() {
    boolean[] blocked = {false};
    controller = newController((centre, size) -> !blocked[0]);
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    Vector2 statue = new Vector2(6f, 5f);
    controller.setStatuePaths(
        () -> List.of(new FinalBossTornadoController.StatuePath(statue, statue)));
    blocked[0] = true;
    controller.update(0.6f, true);
    assertEquals(new Vector2(5f, 5f), item.position);
    blocked[0] = false;
    controller.update(0.1f, true);
    assertTrue(item.position.dst(statue) > 1f);
  }

  @Test
  void cameraChangesShouldContainSpawningAndFadingSpritesAndResetOldDestinations() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    item.destination = new Vector2(1.5f, 1f);
    item.chasing = true;
    arena.set(8f, 8f, 8f, 8f);
    controller.update(0.1f, true);
    assertInsideArena(item.position);
    assertNull(item.destination);
    assertFalse(item.chasing);
    assertEquals(1, controller.activeCount());

    controller.statueBroken(new Vector2(5f, 5f), 0);
    arena.set(10f, 10f, 5f, 5f);
    controller.update(0.1f, false);
    assertTrue(item.dissolving);
    assertInsideArena(item.position);
    assertEquals(1, controller.items.size());
  }

  @Test
  void cameraCorrectionShouldFindSafeSpaceAwayFromLiveStatuesAndKeepPersistentRemnants() {
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    Vector2 statue = new Vector2(9.4f, 8.425f);
    controller.setStatuePaths(
        () -> List.of(new FinalBossTornadoController.StatuePath(statue, statue)));
    arena.set(8f, 8f, 8f, 8f);
    controller.update(0.1f, true);
    assertInsideArena(item.position);
    assertTrue(item.position.dst(statue) >= FinalBossTornadoController.STATUE_CLEARANCE - EPSILON);
    assertEquals(1, controller.activeCount());
  }

  @Test
  void fullyBlockedCameraCorrectionShouldKeepTheRemnantInsideAndRetryLater() {
    boolean[] blocked = {false};
    controller = newController((centre, size) -> !blocked[0]);
    controller.statueBroken(new Vector2(5f, 5f), 4);
    FinalBossTornadoController.Tornado item = controller.items.getFirst();
    blocked[0] = true;
    arena.set(8f, 8f, 8f, 8f);
    controller.update(0.6f, true);
    assertInsideArena(item.position);
    Vector2 corrected = item.position.cpy();
    controller.update(0.1f, true);
    assertEquals(corrected, item.position);
    assertEquals(1, controller.activeCount());
    blocked[0] = false;
    movePlayerGroundTo(corrected.x + 2f, corrected.y);
    controller.update(0.1f, true);
    assertTrue(corrected.dst(item.position) > 0f);
    assertInsideArena(item.position);
  }

  @Test
  void blockedSpawnShouldRetryOnlyDuringCombatAndAppearOnceSpaceOpens() {
    boolean[] blocked = {true};
    controller = newController((centre, size) -> !blocked[0]);
    Vector2 request = new Vector2(5f, 5f);
    controller.statueBroken(request, 4);
    request.set(100f, 100f);
    assertEquals(0, controller.activeCount());
    blocked[0] = false;
    controller.update(10f, false);
    assertEquals(0, controller.activeCount());
    controller.update(0.24f, true);
    assertEquals(0, controller.activeCount());
    controller.update(0.02f, true);
    assertEquals(1, controller.activeCount());
    assertEquals(new Vector2(5f, 5f), controller.items.getFirst().position);
    controller.update(10f, true);
    assertEquals(1, controller.activeCount());
  }

  @Test
  void queuedAndVisibleRemnantsTogetherShouldNeverExceedFour() {
    boolean[] blocked = {false};
    controller = newController((centre, size) -> !blocked[0]);
    controller.statueBroken(new Vector2(5f, 5f), 4);
    blocked[0] = true;
    for (int remaining = 3; remaining > 0; remaining--) {
      controller.statueBroken(new Vector2(5f, remaining * 3f + 6f), remaining);
    }
    controller.statueBroken(new Vector2(10f, 10f), 1);
    assertEquals(1, controller.activeCount());
    blocked[0] = false;
    controller.update(0.3f, true);
    assertEquals(4, controller.activeCount());
    controller.update(1f, true);
    assertEquals(4, controller.activeCount());
  }

  @Test
  void finalStatueShouldCancelQueuedRemnantsWithoutALateSpawnOrBurst() {
    boolean[] blocked = {true};
    controller = newController((centre, size) -> !blocked[0]);
    controller.statueBroken(new Vector2(5f, 5f), 4);
    controller.statueBroken(new Vector2(8f, 5f), 3);
    controller.statueBroken(new Vector2(10f, 5f), 0);
    blocked[0] = false;
    controller.update(1f, true);
    assertEquals(0, controller.activeCount());
    assertTrue(controller.items.isEmpty());
    assertTrue(bursts.isEmpty());
  }

  @Test
  void deathAndExplicitCleanupShouldCancelQueuedRemnantsPermanently() {
    boolean[] blocked = {true};
    controller = newController((centre, size) -> !blocked[0]);
    controller.statueBroken(new Vector2(5f, 5f), 4);
    stats.setHealth(0);
    controller.update(0.3f, true);
    stats.setHealth(100);
    blocked[0] = false;
    controller.update(1f, true);
    assertTrue(controller.items.isEmpty());

    blocked[0] = true;
    FinalBossTornadoController disposed = newController((centre, size) -> !blocked[0]);
    disposed.statueBroken(new Vector2(5f, 5f), 4);
    disposed.clear();
    blocked[0] = false;
    disposed.update(1f, true);
    assertTrue(disposed.items.isEmpty());
  }

  private FinalBossTornadoController newController(BiPredicate<Vector2, Vector2> clearSpace) {
    return new FinalBossTornadoController(
        player, () -> arena, clearSpace, point -> bursts.add(point.cpy()));
  }

  private void movePlayerGroundTo(float x, float y) {
    Vector2 size = player.getScale();
    player.setPosition(x - size.x * 0.5f, y - size.y * 0.15f);
  }

  private void assertInsideArena(Vector2 position) {
    assertTrue(position.x - FinalBossTornadoController.WIDTH * 0.5f >= arena.x - EPSILON);
    assertTrue(
        position.x + FinalBossTornadoController.WIDTH * 0.5f <= arena.x + arena.width + EPSILON);
    assertTrue(position.y >= arena.y - EPSILON);
    assertTrue(position.y + FinalBossTornadoController.HEIGHT <= arena.y + arena.height + EPSILON);
  }
}
