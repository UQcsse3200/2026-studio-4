package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

/** Real combat and wall integration for the shared Stage 3 tornado contact-damage interval. */
@ExtendWith(GameExtension.class)
class FinalBossTornadoDamageIntegrationTest {
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossPhaseControllerComponent phases;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent bossStats;
  private StatusEffectsControllerComponent effects;
  private ConsumableEffectComponent consumables;
  private InventoryComponent inventory;
  private Entity player;
  private Entity boss;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    ServiceLocator.registerRenderService(mock(RenderService.class));
    playerStats = new CombatStatsComponent(100, 10);
    effects = spy(new StatusEffectsControllerComponent());
    consumables = new ConsumableEffectComponent();
    inventory = new InventoryComponent(0);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(playerStats)
            .addComponent(effects)
            .addComponent(inventory)
            .addComponent(consumables)
            .addComponent(new PlayerActions());
    player.create();
    config = new FinalBossStageThreeConfig();
    config.tornadoDamage = 1f; // Preserve the existing contact-clock and collision assertions.
    config.statueSlamInitialDelay = 10000f;
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    bossStats = new CombatStatsComponent(100, 0);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    bossStats.setHealth(40);
    tick(0.01f);
    bossStats.setHealth(20);
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  @AfterEach
  void disposeWorld() {
    stage.dispose();
    world.dispose();
  }

  @Test
  void sustainedContactDealsOneHealthPerQuarterSecondAndAttributesDamageToTheBoss() {
    addTornado(true);
    List<Entity> sources = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "damageTaken", (Entity source, Integer lost, Integer remaining) -> sources.add(source));

    tick(0.125f);
    assertEquals(100, playerStats.getHealth());
    tick(0.125f);
    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    tick(0.125f);
    assertEquals(99, playerStats.getHealth());
    tick(0.125f);

    assertEquals(98, playerStats.getHealth());
    assertEquals(List.of(boss, boss), sources);
  }

  @Test
  void leavingContactResetsTheIntervalBeforeThePlayerReturns() {
    FinalBossTornadoController.Tornado tornado = addTornado(true);
    tick(0.2f);
    player.setPosition(8f, 0f);
    tick(0.1f);
    assertEquals(100, playerStats.getHealth());
    player.setPosition(0f, 0f);
    tornado.position.set(FinalBossStageThreeComponent.groundPosition(player));

    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    tick(0.05f);

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void severalOverlappingTornadoesShareOneDamageInterval() {
    for (int index = 0; index < 4; index++) addTornado(true);

    tick(0.25f);
    assertEquals(99, playerStats.getHealth());
    tick(0.25f);

    assertEquals(98, playerStats.getHealth());
  }

  @Test
  void aLongFrameDealsOnlyOneHitAndDoesNotLeaveACatchUpDamageBacklog() {
    addTornado(true);

    tick(2.25f);
    assertEquals(99, playerStats.getHealth());
    tick(0.01f);
    assertEquals(99, playerStats.getHealth());
    tick(0.24f);

    assertEquals(98, playerStats.getHealth());
  }

  @Test
  void localZeroDamageAndInvulnerabilityProduceNeitherHealthLossNorHitFlash() {
    addTornado(true);
    playerStats.setIncomingDamageMultiplier(0f);
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    playerStats.setIncomingDamageMultiplier(1f);
    playerStats.setInvulnerable(true);

    tick(0.25f);

    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    playerStats.setInvulnerable(false);
    tick(0.25f);
    assertEquals(99, playerStats.getHealth());
    assertTrue(stage.tornadoHitRemaining > 0f);
  }

  @Test
  void spawningTornadoDoesNotAccumulateDamageBeforeItsSpawnAnimationFinishes() {
    addTornado(false);

    tick(0.59f);
    assertEquals(100, playerStats.getHealth());
    tick(0.02f);
    assertEquals(100, playerStats.getHealth());
    tick(0.22f);
    assertEquals(100, playerStats.getHealth());
    tick(0.02f);

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void phaseTransitionPauseDiscardsPartialContactTime() {
    addTornado(true);
    tick(0.2f);
    when(phases.isTransitioning()).thenReturn(true);
    tick(1f);
    assertEquals(100, playerStats.getHealth());
    when(phases.isTransitioning()).thenReturn(false);

    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    tick(0.05f);

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void concealmentPreventsDamageAndResetsThePartialContactInterval() {
    FinalBossTornadoController.Tornado tornado = addTornado(true);
    tick(0.2f);
    when(effects.isConcealed()).thenReturn(true);
    tick(0.5f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    when(effects.isConcealed()).thenReturn(false);
    tornado.position.set(FinalBossStageThreeComponent.groundPosition(player));

    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    tick(0.05f);

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void aRealStaticWallBetweenTheTornadoAndPlayerPreventsDamage() {
    FinalBossTornadoController.Tornado tornado = addTornado(true);
    Vector2 ground = FinalBossStageThreeComponent.groundPosition(player);
    tornado.position.set(ground).add(-0.9f, 0f);
    Body wall = addWall(ground.x - 0.45f, ground.y);

    tick(0.5f);

    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    world.destroyBody(wall);
    tornado.position.set(ground).add(-0.9f, 0f);
    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    tick(0.05f);
    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void aLethalTornadoHitClearsAllTornadoesAndTheHitEffectInTheSameUpdate() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    playerStats.setHealth(1);

    tick(0.25f);
    assertEquals(1, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    tick(0.25f);

    assertEquals(0, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
    assertEquals(0f, stage.tornadoHitRemaining);
    tick(1f);
    assertEquals(0, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
  }

  @Test
  void bossDeathClearsTheTornadoDamageAndItsOutstandingFlash() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    assertTrue(stage.tornadoHitRemaining > 0f);

    bossStats.setHealth(0);
    tick(0.01f);

    assertEquals(100, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
    assertEquals(0f, stage.tornadoHitRemaining);
  }

  @Test
  void leavingStageThreeClearsTornadoDamageAndItsOutstandingFlash() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    assertTrue(stage.tornadoHitRemaining > 0f);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);

    tick(0.01f);

    assertEquals(100, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
    assertEquals(0f, stage.tornadoHitRemaining);
  }

  @Test
  void breakingTheLastStatueStopsDamageAndFlashDuringTornadoDissolution() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    assertTrue(stage.tornadoHitRemaining > 0f);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      for (int hit = 0; hit < config.statueHits; hit++) stage.hitStatue(statue);
    }
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertFalse(stage.tornadoes.items.isEmpty());
    assertTrue(stage.tornadoes.items.stream().allMatch(tornado -> tornado.dissolving));
    assertEquals(0f, stage.tornadoHitRemaining);

    tick(0.3f);

    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
  }

  @Test
  void disposalClearsTornadoDamageAndItsOutstandingFlash() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    assertTrue(stage.tornadoHitRemaining > 0f);

    stage.dispose();
    tick(1f);

    assertEquals(100, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
    assertEquals(0f, stage.tornadoHitRemaining);
  }

  @Test
  void synchronousDamageListenerDisposalDoesNotRestoreTheFlashOrAdvanceStatues() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    player
        .getEvents()
        .addListener(
            "damageTaken", (Entity source, Integer lost, Integer remaining) -> stage.dispose());

    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    tick(0.25f);

    assertEquals(99, playerStats.getHealth());
    assertTrue(stage.tornadoes.items.isEmpty());
    assertEquals(0f, stage.tornadoHitRemaining);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      assertEquals(10000f, statue.pauseRemaining);
    }
    tick(1f);
    assertEquals(99, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
  }

  @Test
  void everyHalfDamageTickFlashesWhileFourTicksRemoveOnlyTwoHealth() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    List<Entity> sources = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer remaining) -> sources.add(attacker));

    for (int expectedHealth : new int[] {100, 99, 99, 98}) {
      tick(0.25f);
      assertEquals(expectedHealth, playerStats.getHealth());
      assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    }

    assertEquals(List.of(boss, boss), sources);
  }

  @Test
  void leavingContactResetsOnlyTheClockAndPreservesAnEffectiveHalfDamageTick() {
    config.tornadoDamage = 0.5f;
    FinalBossTornadoController.Tornado tornado = addTornado(true);
    tick(0.25f);
    tick(0.1f);
    player.setPosition(8f, 0f);
    tick(0.3f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    player.setPosition(0f, 0f);
    tornado.position.set(FinalBossStageThreeComponent.groundPosition(player));

    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    tick(0.05f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
  }

  @Test
  void aTemporaryPhaseTransitionPreservesEffectiveHalfDamageButResetsContactTime() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    tick(0.1f);
    when(phases.isTransitioning()).thenReturn(true);
    tick(1f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    when(phases.isTransitioning()).thenReturn(false);

    tick(0.2f);
    assertEquals(100, playerStats.getHealth());
    tick(0.05f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
  }

  @Test
  void overlappingTornadoesStillRequireTwoSharedHalfDamageTicksForOneHealth() {
    config.tornadoDamage = 0.5f;
    for (int index = 0; index < 4; index++) addTornado(true);

    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    tick(0.25f);

    assertEquals(99, playerStats.getHealth());
  }

  @Test
  void aLongFrameAccumulatesOnlyOneHalfAndDoesNotQueueExtraDamageTicks() {
    config.tornadoDamage = 0.5f;
    addTornado(true);

    tick(2.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    tick(0.01f);
    assertEquals(100, playerStats.getHealth());
    tick(0.24f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
  }

  @Test
  void localZeroDamageNeitherAccumulatesNorDiscardsEffectiveHalfDamageOrFlashes() {
    assertProtectionPreservesOnlyEffectiveHalfDamage(
        () -> playerStats.setIncomingDamageMultiplier(0f),
        () -> playerStats.setIncomingDamageMultiplier(1f));
  }

  @Test
  void invulnerabilityNeitherAccumulatesNorDiscardsEffectiveHalfDamageOrFlashes() {
    assertProtectionPreservesOnlyEffectiveHalfDamage(
        () -> playerStats.setInvulnerable(true), () -> playerStats.setInvulnerable(false));
  }

  @Test
  void concealmentNeitherAccumulatesNorDiscardsEffectiveHalfDamageOrFlashes() {
    assertProtectionPreservesOnlyEffectiveHalfDamage(
        () -> when(effects.isConcealed()).thenReturn(true),
        () -> when(effects.isConcealed()).thenReturn(false));
  }

  @Test
  void timedShieldRetainsEarlierHealthHalfButItsOwnHalfDoesNotLeakOrFlash() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    tick(0.25f);
    AtomicLong shieldTime = new AtomicLong();
    try (MockedConstruction<GameTime> ignored =
        mockConstruction(
            GameTime.class,
            (clock, context) -> when(clock.getTime()).thenAnswer(call -> shieldTime.get()))) {
      effects.activateTimed();
      assertTrue(effects.isShieldActive());
      for (int index = 0; index < 3; index++) {
        tick(0.25f);
        assertEquals(100, playerStats.getHealth());
        assertEquals(0f, stage.tornadoHitRemaining);
      }
      shieldTime.set(3001L);
      effects.update();
      assertFalse(effects.isShieldActive());

      tick(0.25f);
      assertEquals(99, playerStats.getHealth());
      assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
      tick(0.25f);
      assertEquals(99, playerStats.getHealth());
      tick(0.25f);
      assertEquals(98, playerStats.getHealth());
    }
  }

  @Test
  void absorbShieldConsumesOnePointPerPairWithoutFlashingOrLeakingItsHalfOnExpiry() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    AtomicInteger shieldPoints = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "updateShield", (Integer current, Integer maximum) -> shieldPoints.set(current));
    AtomicLong shieldTime = new AtomicLong();
    try (MockedConstruction<GameTime> ignored =
        mockConstruction(
            GameTime.class,
            (clock, context) -> when(clock.getTime()).thenAnswer(call -> shieldTime.get()))) {
      effects.activateAbsorb();
      assertTrue(effects.isShieldActive());
      for (int expectedPoints : new int[] {20, 19, 19}) {
        tick(0.25f);
        assertEquals(expectedPoints, shieldPoints.get());
        assertEquals(100, playerStats.getHealth());
        assertEquals(0f, stage.tornadoHitRemaining);
      }
      shieldTime.set(5001L);
      effects.update();
      assertFalse(effects.isShieldActive());

      tick(0.25f);
      assertEquals(100, playerStats.getHealth());
      assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
      tick(0.25f);
      assertEquals(99, playerStats.getHealth());
    }
  }

  @Test
  void consumableShieldBlocksHalfTicksAndFeedbackWhilePreservingEarlierEffectiveHalf() {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    inventory.addConsumable(ItemType.SHIELD, 2);
    assertTrue(consumables.tryUse(ItemType.SHIELD));
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    when(time.getTime()).thenReturn(ConsumableEffectComponent.DURATION_MS + 1);
    effects.update();
    assertFalse(consumables.isShielded());
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    assertTrue(consumables.tryUse(ItemType.SHIELD));
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    when(time.getTime()).thenReturn(2 * ConsumableEffectComponent.DURATION_MS + 2);
    effects.update();
    assertFalse(consumables.isShielded());
    tick(0.25f);

    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
  }

  private void assertProtectionPreservesOnlyEffectiveHalfDamage(Runnable block, Runnable unblock) {
    config.tornadoDamage = 0.5f;
    addTornado(true);
    block.run();
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    unblock.run();
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
    block.run();
    tick(0.25f);
    assertEquals(100, playerStats.getHealth());
    assertEquals(0f, stage.tornadoHitRemaining);
    unblock.run();
    tick(0.25f);
    assertEquals(99, playerStats.getHealth());
    assertEquals(FinalBossStageThreeComponent.TORNADO_HIT_DURATION, stage.tornadoHitRemaining);
  }

  private FinalBossTornadoController.Tornado addTornado(boolean fullySpawned) {
    FinalBossTornadoController.Tornado tornado =
        new FinalBossTornadoController.Tornado(
            FinalBossStageThreeComponent.groundPosition(player), 0f, stage.tornadoes.items.size());
    if (fullySpawned) tornado.elapsed = FinalBossTornadoController.SPAWN_DURATION;
    stage.tornadoes.items.add(tornado);
    return tornado;
  }

  private void tick(float delta) {
    // These tests isolate contact damage from statue locomotion, slams and their shockwaves.
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      statue.pauseRemaining = 10000f;
      statue.moveRemaining = 0f;
      statue.destination = null;
      statue.slamCooldown = 10000f;
      statue.warningRemaining = 0f;
      statue.airborne = false;
      statue.evadePending = false;
    }
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }

  private Body addWall(float x, float y) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, y);
    Body body = world.createBody(definition);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.05f, 1f);
    body.createFixture(shape, 0f);
    shape.dispose();
    return body;
  }
}
