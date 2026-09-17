package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageThreeSlamTest {
  private static final float EPSILON = 0.001f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity boss;
  private Entity player;
  private PlayerActions actions;
  private GameTime time;
  private RenderService renderer;
  private FinalBossPhaseControllerComponent phases;

  @BeforeEach
  void setup() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(0.1f);
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    actions = new PlayerActions();
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(actions);
    player.create();
    config = new FinalBossStageThreeConfig();
    config.floatingDemonCount = 0; // Isolate statue/ice mechanics from rendered summons.
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
  }

  @Test
  void fiveStatuesSpawnSeparatelyInsideTheArenaReadyForTheSharedAttackSchedule() {
    assertFalse(actions.isJumpEnabled());
    startStatues();
    assertTrue(actions.isJumpEnabled());
    assertEquals(5, stage.statues.size());
    assertEquals(5, stage.getRemainingStatues());
    Rectangle arena = new Rectangle(-12f, -8f, 24f, 16f);
    for (int i = 0; i < stage.statues.size(); i++) {
      FinalBossStageThreeComponent.Statue statue = stage.statues.get(i);
      Vector2 position = statue.entity.getPosition();
      assertTrue(arena.contains(position));
      assertTrue(arena.contains(position.cpy().add(statue.entity.getScale())));
      assertEquals(10, statue.hitsRemaining);
      assertEquals(0f, statue.slamCooldown, EPSILON);
      assertEquals(0f, statue.warningRemaining);
      assertFalse(statue.airborne);
      for (int j = 0; j < i; j++) {
        assertTrue(
            statue.entity.getCenterPosition().dst(stage.statues.get(j).entity.getCenterPosition())
                > 3f);
      }
    }
    assertTrue(stage.shockwaves.isEmpty());
    assertFalse(boss.getComponent(PhysicsComponent.class).getBody().isActive());
  }

  @Test
  void statuesSpawnWithEnoughHeadroomForTheirJumpAndHealthBarInAShortArena() {
    when(boss.getComponent(FinalBossMovementComponent.class).getCamera())
        .thenReturn(new OrthographicCamera(16f, 8f));
    startStatues();
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      float spriteTop = statue.entity.getPosition().y + statue.entity.getScale().y;
      assertTrue(spriteTop + config.statueJumpHeight + 0.32f <= 4f + EPSILON);
      assertTrue(statue.entity.getPosition().y >= -4f);
    }
  }

  @Test
  void statuesAlternateBetweenMovingAndPausingBeforeTheirSlam() {
    startStatues();
    silenceSlams();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    PhysicsMovementComponent movement = statue.entity.getComponent(PhysicsMovementComponent.class);
    Vector2 before = statue.entity.getCenterPosition();
    tick(0.1f);
    assertTrue(movement.getMoving());
    movement.update();
    ServiceLocator.getPhysicsService().getPhysics().getWorld().step(0.016f, 6, 2);
    statue.entity.getComponent(PhysicsComponent.class).earlyUpdate();
    assertTrue(statue.entity.getCenterPosition().dst(before) > EPSILON);
    statue.moveRemaining = 0.01f;
    tick(0.02f);
    assertFalse(movement.getMoving());
    assertTrue(
        statue.entity.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().isZero());
    assertEquals(config.statuePause, statue.pauseRemaining, EPSILON);
    tick(config.statuePause - 0.01f);
    assertFalse(movement.getMoving());
    tick(0.02f);
    tick(0.01f);
    assertTrue(movement.getMoving());
  }

  @Test
  void aSlamWarnsAndStopsMovementBeforeTheStatueJumps() {
    startStatues();
    FinalBossStageThreeComponent.Statue first = stage.statues.getFirst();
    tick(config.statueSlamInitialDelay - 0.01f);
    assertEquals(0f, first.warningRemaining);
    assertFalse(first.airborne);
    tick(0.02f);
    assertEquals(config.statueSlamWarning, first.warningRemaining, EPSILON);
    assertFalse(first.entity.getComponent(PhysicsMovementComponent.class).getMoving());
    assertFalse(first.airborne);
    assertTrue(stage.statues.stream().skip(1).allMatch(statue -> statue.warningRemaining == 0f));
    assertTrue(stage.shockwaves.isEmpty());
    verify(renderer, never()).shake(any(), anyFloat(), anyFloat());

    tick(config.statueSlamWarning - 0.01f);
    assertFalse(first.airborne);
  }

  @Test
  void aSlamJumpRaisesTheSpriteWithoutMovingItsGroundPosition() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    assertTrue(first.airborne);
    assertEquals(0f, first.jumpHeight, EPSILON);
    Vector2 launch = first.entity.getCenterPosition();
    tick(config.statueJumpDuration / 2f);
    assertTrue(first.airborne);
    assertEquals(config.statueJumpHeight, first.jumpHeight, EPSILON);
    assertTrue(first.entity.getCenterPosition().epsilonEquals(launch, EPSILON));
    assertTrue(stage.shockwaves.isEmpty());
  }

  @Test
  void aLongFrameCannotOverlapSlamsOrMakeTwoStatuesLandTogether() {
    startStatues();
    player.getComponent(CombatStatsComponent.class).setInvulnerable(true);
    FinalBossStageThreeComponent.Statue first = stage.statues.getFirst();
    FinalBossStageThreeComponent.Statue second = stage.statues.get(1);
    tick(config.statueSlamInitialDelay);
    assertEquals(config.statueSlamWarning, first.warningRemaining, EPSILON);

    tick(3f);
    assertTrue(first.airborne);
    assertEquals(0f, second.warningRemaining);
    tick(config.statueSlamWarning);
    assertTrue(first.airborne);
    assertEquals(0f, second.warningRemaining);
    assertFalse(second.airborne);

    tick(config.statueJumpDuration);
    assertFalse(first.airborne);
    assertEquals(1, stage.shockwaves.size());
    assertEquals(config.statueSlamWarning, second.warningRemaining, EPSILON);
    assertFalse(second.airborne);
    verify(renderer, times(1)).shake(any(), anyFloat(), anyFloat());

    tick(config.statueSlamWarning);
    tick(config.statueJumpDuration);
    assertFalse(second.airborne);
    verify(renderer, times(2)).shake(any(), anyFloat(), anyFloat());
  }

  @Test
  void aSlamLandingCreatesOneExpandingRingAtTheFeetAndOneShortCameraShake() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    Vector2 floor =
        new Vector2(
            first.entity.getPosition().x + first.entity.getScale().x / 2f,
            first.entity.getPosition().y + first.entity.getScale().y * 0.15f);
    tick(config.statueJumpDuration / 2f);
    tick(config.statueJumpDuration / 2f + 0.01f);
    assertFalse(first.airborne);
    assertEquals(0f, first.jumpHeight, EPSILON);
    assertEquals(1, stage.shockwaves.size());
    FinalBossStageThreeComponent.Shockwave wave = stage.shockwaves.getFirst();
    assertTrue(wave.position.epsilonEquals(floor, EPSILON));
    assertTrue(
        wave.position.epsilonEquals(
            FinalBossStageThreeComponent.groundPosition(first.entity), EPSILON));
    assertEquals(0f, wave.radius, EPSILON);
    assertTrue(wave.maxRadius >= floor.dst(new Vector2(-12f, -8f)));
    verify(renderer).shake(stage, 0.3f, 0.10f);
    tick(0.1f);
    assertEquals(config.shockwaveSpeed * 0.1f, wave.radius, EPSILON);
    verify(renderer, times(1)).shake(any(), anyFloat(), anyFloat());
  }

  @Test
  void aLandedStatueWaitsForItsFullCooldownBeforeWarningAgain() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    tick(config.statueJumpDuration / 2f);
    tick(config.statueJumpDuration / 2f + 0.01f);
    assertEquals(13.5f, first.slamCooldown, EPSILON);
    tick(0.1f);
    stage.statues.stream().skip(1).forEach(statue -> statue.slamCooldown = 100f);
    tick(first.slamCooldown - 0.01f);
    assertEquals(0f, first.warningRemaining);
    assertFalse(first.airborne);
    tick(0.02f);
    assertEquals(config.statueSlamWarning, first.warningRemaining, EPSILON);
  }

  private FinalBossStageThreeComponent.Statue startFirstStatueJump() {
    startStatues();
    tick(config.statueSlamInitialDelay - 0.01f);
    tick(0.02f);
    tick(config.statueSlamWarning - 0.01f);
    tick(0.02f);
    return stage.statues.getFirst();
  }

  @Test
  void allFiveStatuesTakeTurnsEveryThreeSecondsAndKeepTheirOwnRecovery() {
    startStatues();
    player.getComponent(CombatStatsComponent.class).setInvulnerable(true);
    List<List<Float>> warnings = new ArrayList<>();
    List<List<Float>> landings = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      warnings.add(new ArrayList<>());
      landings.add(new ArrayList<>());
    }
    float step = 0.01f;
    for (int frame = 1; frame <= 3200; frame++) {
      boolean[] wasWarning = new boolean[5];
      boolean[] wasAirborne = new boolean[5];
      for (int i = 0; i < 5; i++) {
        wasWarning[i] = stage.statues.get(i).warningRemaining > 0f;
        wasAirborne[i] = stage.statues.get(i).airborne;
      }
      tickPhysics(step);
      for (int i = 0; i < 5; i++) {
        FinalBossStageThreeComponent.Statue statue = stage.statues.get(i);
        if (!wasWarning[i] && statue.warningRemaining > 0f) warnings.get(i).add(frame * step);
        if (wasAirborne[i] && !statue.airborne) landings.get(i).add(frame * step);
      }
    }
    for (int i = 0; i < 5; i++) {
      List<Float> warningTimes = warnings.get(i);
      assertTrue(warningTimes.size() >= 2, "Every statue must repeat its attack");
      assertFalse(landings.get(i).isEmpty());
      assertEquals(3f + 3f * i, warningTimes.getFirst(), 0.08f);
      assertEquals(13.5f, warningTimes.get(1) - landings.get(i).getFirst(), 0.08f);
      if (i > 0) {
        assertEquals(3f, warningTimes.getFirst() - warnings.get(i - 1).getFirst(), 0.03f);
        assertEquals(3f, warningTimes.get(1) - warnings.get(i - 1).get(1), 0.03f);
      }
    }
  }

  @Test
  void fiveStatuesKeepOneLandingEveryThreeSeconds() {
    assertLandingCadence(5, 3f);
  }

  @Test
  void fourStatuesIncreaseTheTotalLandingFrequency() {
    assertLandingCadence(4, 2.7f);
  }

  @Test
  void threeStatuesIncreaseTheTotalLandingFrequencyAgain() {
    assertLandingCadence(3, 2.4f);
  }

  @Test
  void twoStatuesAlternateTheirLandingsEveryTwoPointOneSeconds() {
    assertLandingCadence(2, 2.1f);
  }

  @Test
  void theLastStatueRepeatsItsLandingEveryOnePointEightSeconds() {
    assertLandingCadence(1, 1.8f);
  }

  private void assertLandingCadence(int remaining, float expectedInterval) {
    startStatues();
    player.getComponent(CombatStatsComponent.class).setInvulnerable(true);
    for (int i = remaining; i < stage.statues.size(); i++) breakStatue(stage.statues.get(i));
    List<Float> landings = new ArrayList<>();
    List<Integer> attackers = new ArrayList<>();
    float step = 0.01f;
    for (int frame = 1; frame <= 2500; frame++) {
      boolean[] wasAirborne = new boolean[remaining];
      for (int i = 0; i < remaining; i++) wasAirborne[i] = stage.statues.get(i).airborne;
      tick(step);
      for (int i = 0; i < remaining; i++) {
        if (wasAirborne[i] && !stage.statues.get(i).airborne) {
          landings.add(frame * step);
          attackers.add(i);
        }
      }
    }
    assertTrue(landings.size() >= 7, "Measure repeated slams, including the next full rotation");
    for (int i = 0; i < landings.size(); i++) {
      assertEquals(i % remaining, attackers.get(i), "Every surviving statue gets its turn");
      if (i > 0) assertEquals(expectedInterval, landings.get(i) - landings.get(i - 1), 0.04f);
    }
  }

  @Test
  void breakingPeersKeepsTheCurrentJumpIntactAndAcceleratesTheNextSlam() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    tick(config.statueJumpDuration / 2f);
    float heightBefore = first.jumpHeight;
    for (int i = 1; i < stage.statues.size(); i++) breakStatue(stage.statues.get(i));
    assertTrue(first.airborne);
    assertEquals(heightBefore, first.jumpHeight, EPSILON);
    assertTrue(stage.shockwaves.isEmpty());
    tick(config.statueJumpDuration / 2f);
    assertEquals(1, stage.shockwaves.size());
    assertEquals(0.3f, first.slamCooldown, EPSILON);
    tick(0.69f);
    assertEquals(0f, first.warningRemaining);
    tick(0.5f);
    assertEquals(config.statueSlamWarning, first.warningRemaining, EPSILON);
    assertFalse(first.airborne);
  }

  @Test
  void breakingPeersShortensAnExistingCooldownAndEvadingUsesTheFasterRecovery() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    tick(config.statueJumpDuration);
    assertEquals(13.5f, first.slamCooldown, EPSILON);
    for (int i = 1; i < stage.statues.size(); i++) breakStatue(stage.statues.get(i));
    assertEquals(0.3f, first.slamCooldown, EPSILON);
    for (int i = 0; i < config.statueEvadeHits; i++) stage.hitStatue(first);
    assertTrue(first.evadePending);
    tick(0.01f);
    assertFalse(first.evadePending);
    assertEquals(0.3f, first.slamCooldown, EPSILON);
    assertEquals(0f, first.warningRemaining);
    assertFalse(first.airborne);
  }

  @Test
  void theLastStatueCanWalkBetweenItsLandingAndTheNextSlam() {
    FinalBossStageThreeComponent.Statue first = startFirstStatueJump();
    for (int i = 1; i < stage.statues.size(); i++) breakStatue(stage.statues.get(i));
    tick(config.statueJumpDuration);
    Vector2 before = first.entity.getCenterPosition();
    assertEquals(0.075f, first.pauseRemaining, EPSILON);
    for (int frame = 0; frame < 4; frame++) tickPhysics(0.05f);
    assertTrue(first.entity.getCenterPosition().dst(before) > 0.05f);
    assertEquals(0f, first.warningRemaining);
    assertFalse(first.airborne);
  }

  @Test
  void breakingAnAirborneStatueImmediatelyStopsItAndCancelsItsPendingSlam() {
    startStatues();
    silenceSlams();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    statue.slamCooldown = 0f;
    tick(config.statueSlamInitialDelay);
    tick(config.statueSlamWarning);
    tick(config.statueJumpDuration / 2f);
    assertTrue(statue.airborne);
    assertTrue(statue.jumpHeight > 0f);
    breakStatue(statue);
    assertTrue(statue.broken);
    assertFalse(statue.airborne);
    assertEquals(0f, statue.jumpHeight);
    assertFalse(statue.entity.getComponent(PhysicsMovementComponent.class).getMoving());
    assertFalse(statue.entity.getComponent(PhysicsComponent.class).getBody().isActive());
    assertEquals(0, statue.entity.getComponent(CombatStatsComponent.class).getHealth());
    verify(ServiceLocator.getEntityService()).scheduleDisposal(statue.entity);
    tick(config.statueJumpDuration + config.statueSlamInterval);
    assertEquals(4, stage.getRemainingStatues());
    assertTrue(stage.shockwaves.isEmpty());
    assertEquals(100, playerHealth());
    verify(renderer, never()).shake(any(), anyFloat(), anyFloat());
  }

  @Test
  void aGroundedPlayerTakesDamageOnceWhenTheExpandingRingReachesThem() {
    prepareRings(new Vector2(2f, 0f));
    AtomicInteger hits = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "damageAttempted", (Integer damage, Entity attacker) -> hits.incrementAndGet());
    FinalBossStageThreeComponent.Shockwave wave = addRing(0f);
    tick(0.25f);
    assertEquals(100, playerHealth());
    tick(0.25f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
    assertTrue(wave.hitPlayer);
    tick(0.1f);
    setPlayerGroundPosition(new Vector2(4f, 0f));
    tick(0.1f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
    assertEquals(1, hits.get());
  }

  @Test
  void ringContactUsesThePlayersGroundPosition() {
    prepareRings(new Vector2(0f, 2f));
    FinalBossStageThreeComponent.Shockwave wave = addRing(2f);
    tick(0.001f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
    assertTrue(wave.hitPlayer);
  }

  @Test
  void theEmptyCentreOfAnExpandedRingDoesNotDamageThePlayer() {
    prepareRings(new Vector2());
    FinalBossStageThreeComponent.Shockwave wave = addRing(3f);
    tick(0.1f);
    assertEquals(100, playerHealth());
    assertFalse(wave.hitPlayer);
    assertTrue(wave.radius > 3f);
  }

  @Test
  void aRingCannotSkipAGroundedPlayerOnALongFrameEvenWhenItExpires() {
    prepareRings(new Vector2(3f, 0f));
    addRing(0f);
    tick(10f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
    assertTrue(stage.shockwaves.isEmpty());
  }

  @Test
  void aPlayerCrossingTheRingBetweenFramesIsHitEvenWithBothEndpointsOutside() {
    prepareRings(new Vector2(-4f, 0f));
    FinalBossStageThreeComponent.Shockwave wave = addRing(2f);
    tick(0.01f);
    assertEquals(100, playerHealth());
    setPlayerGroundPosition(new Vector2(4f, 0f));
    tick(0.01f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
    assertTrue(wave.hitPlayer);
  }

  @Test
  void jumpingOverARingConsumesContactWithoutDamageAfterLandingInsideIt() {
    prepareRings(new Vector2(2f, 0f));
    player.getEvents().trigger("dash", Vector2.Zero.cpy());
    tickPlayer(0.25f);
    assertTrue(actions.isJumping());
    assertTrue(actions.getJumpHeight() >= 0.2f);
    FinalBossStageThreeComponent.Shockwave wave = addRing(0f);
    tick(0.6f);
    assertTrue(wave.hitPlayer);
    assertEquals(100, playerHealth());
    tickPlayer(0.61f);
    assertFalse(actions.isJumping());
    tick(0.1f);
    assertEquals(100, playerHealth());
  }

  @Test
  void pressingJumpAtContactDoesNotDodgeBeforeThePlayerLeavesTheGround() {
    prepareRings(new Vector2(2f, 0f));
    player.getEvents().trigger("dash", Vector2.Zero.cpy());
    assertTrue(actions.isJumping());
    assertEquals(0f, actions.getJumpHeight());
    addRing(0f);
    tick(0.5f);
    assertEquals(100 - config.shockwaveDamage, playerHealth());
  }

  @Test
  void theLastStatueClearsEveryHazardAndRestoresDashThroughoutTheReturn() {
    prepareRings(new Vector2(2f, 0f));
    addRing(0f);
    player.getEvents().trigger("dash", Vector2.Zero.cpy());
    tickPlayer(0.2f);
    assertTrue(actions.isJumping());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) breakStatue(statue);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertTrue(stage.shockwaves.isEmpty());
    assertFalse(actions.isJumpEnabled());
    assertFalse(actions.isJumping());
    assertTrue(
        stage.statues.stream()
            .allMatch(
                statue ->
                    !statue.entity.getComponent(PhysicsComponent.class).getBody().isActive()));
    verify(renderer).clearShake(stage);
    tick(config.returnTransformDuration);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertEquals(100, playerHealth());
    assertFalse(actions.isJumpEnabled());
    tick(20f);
    assertTrue(stage.shockwaves.isEmpty());
    assertEquals(100, playerHealth());
  }

  @Test
  void playerDefeatClearsRingsStopsStatuesAndReleasesJumpMode() {
    prepareRings(new Vector2(2f, 0f));
    addRing(0f);
    player.getEvents().trigger("dash", Vector2.Zero.cpy());
    tickPlayer(0.2f);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(0.01f);
    assertFalse(actions.isJumpEnabled());
    assertFalse(actions.isJumping());
    assertTrue(stage.shockwaves.isEmpty());
    assertTrue(
        stage.statues.stream()
            .allMatch(
                statue -> !statue.entity.getComponent(PhysicsMovementComponent.class).getMoving()));
    verify(renderer).clearShake(stage);
    tick(20f);
    assertTrue(stage.shockwaves.isEmpty());
    verify(phases, never()).completeStage(FinalBossPhase.STAGE_THREE);
  }

  @Test
  void disposalClearsRingsAndJumpModeAndStopsEverySurvivingStatue() {
    prepareRings(new Vector2(2f, 0f));
    addRing(0f);
    player.getEvents().trigger("dash", Vector2.Zero.cpy());
    tickPlayer(0.2f);
    stage.dispose();
    assertFalse(actions.isJumpEnabled());
    assertFalse(actions.isJumping());
    assertTrue(stage.shockwaves.isEmpty());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      assertFalse(statue.entity.getComponent(PhysicsMovementComponent.class).getMoving());
      verify(ServiceLocator.getEntityService()).scheduleDisposal(statue.entity);
    }
    verify(renderer).clearShake(stage);
    tick(20f);
    assertEquals(100, playerHealth());
    assertTrue(stage.shockwaves.isEmpty());
  }

  @Test
  void releasingStageJumpModePreservesAnotherOwnersJumpRequest() {
    startStatues();
    Object otherOwner = new Object();
    actions.setJumpEnabled(otherOwner, true);
    stage.dispose();
    assertTrue(actions.isJumpEnabled());
    actions.setJumpEnabled(otherOwner, false);
    assertFalse(actions.isJumpEnabled());
  }

  private void startStatues() {
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  private void silenceSlams() {
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) statue.slamCooldown = 100f;
  }

  private void prepareRings(Vector2 playerGroundPosition) {
    startStatues();
    silenceSlams();
    setPlayerGroundPosition(playerGroundPosition);
    tick(0.001f);
  }

  private void setPlayerGroundPosition(Vector2 position) {
    player.setPosition(position.cpy().sub(player.getScale().x / 2f, player.getScale().y * 0.15f));
  }

  private FinalBossStageThreeComponent.Shockwave addRing(float radius) {
    FinalBossStageThreeComponent.Shockwave wave =
        new FinalBossStageThreeComponent.Shockwave(new Vector2(), 20f);
    wave.radius = radius;
    stage.shockwaves.add(wave);
    return wave;
  }

  private void breakStatue(FinalBossStageThreeComponent.Statue statue) {
    for (int i = 0; i < 10; i++)
      statue.entity.getComponent(CombatStatsComponent.class).takeDamage(1, new Entity());
  }

  private int playerHealth() {
    return player.getComponent(CombatStatsComponent.class).getHealth();
  }

  private void tickPlayer(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    actions.update();
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }

  private void tickPhysics(float delta) {
    tick(delta);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues)
      statue.entity.getComponent(PhysicsMovementComponent.class).update();
    ServiceLocator.getPhysicsService().getPhysics().getWorld().step(delta, 6, 2);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues)
      statue.entity.getComponent(PhysicsComponent.class).earlyUpdate();
    player.getComponent(PhysicsComponent.class).earlyUpdate();
  }
}
