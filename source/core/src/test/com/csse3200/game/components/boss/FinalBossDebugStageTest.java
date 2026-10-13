package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Fresh-room debug entry using the real phase, combat and achievement controllers. */
@ExtendWith(GameExtension.class)
class FinalBossDebugStageTest {
  private final List<Entity> summons = new ArrayList<>();
  private final List<Entity> stageTwoStarts = new ArrayList<>();
  private final List<Entity> stageTwoCompletions = new ArrayList<>();
  private final List<FinalBossPhase> completedStages = new ArrayList<>();
  private final List<FinalBossPhase> phaseChanges = new ArrayList<>();
  private final List<String> unlockedNames = new ArrayList<>();
  private FinalBossStageOneConfig oneConfig;
  private FinalBossStageTwoConfig twoConfig;
  private FinalBossStageThreeConfig threeConfig;
  private FinalBossStageOneComponent stageOne;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossStageThreeComponent stageThree;
  private FinalBossStageTwoArenaComponent arena;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossMovementComponent movement;
  private FinalBossDamageControllerComponent protection;
  private CombatStatsComponent bossStats;
  private Entity boss;
  private Entity player;
  private OrthographicCamera camera;
  private GameTime time;
  private World world;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    PhysicsService physics = new PhysicsService();
    ServiceLocator.registerPhysicsService(physics);
    world = physics.getPhysics().getWorld();
    ServiceLocator.registerEntityService(new EntityService());
    AchievementService achievements = new AchievementService();
    ServiceLocator.registerAchievementService(achievements);
    registerAchievement(achievements, "I need a break too", "finalBossBreakRespected");
    registerAchievement(achievements, "I'm fireproof!", "finalBossStageTwoFireproof");
    achievements.getEvents().<String>addListener("achievementUnlocked", unlockedNames::add);

    oneConfig = new FinalBossStageOneConfig();
    twoConfig = new FinalBossStageTwoConfig();
    twoConfig.iceCoverCount = 0;
    twoConfig.icePickupCount = 0;
    twoConfig.fireballInitialDelay = 1000f;
    threeConfig = new FinalBossStageThreeConfig();
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerActions());
    player.setPosition(39.5f, 29.5f);
    player.create();
    camera = new OrthographicCamera(24f, 16f);
    camera.position.set(40f, 30f, 0f);
    camera.update();
  }

  @AfterEach
  void disposeWorld() {
    if (boss != null) {
      stageOne.dispose();
      stageTwo.dispose();
      stageThree.dispose();
      arena.dispose();
    }
    world.dispose();
  }

  @Test
  void shouldKeepStageOneAtItsNormalShieldedIntro() {
    createBoss(250);

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_ONE));

    assertEquals(250, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_ONE, phases.getCurrentPhase());
    assertEquals(FinalBossStageOneState.INTRO, stageOne.getState());
    assertTrue(protection.isShielded());
    assertFalse(phases.isTransitioning());
    assertTrue(phaseChanges.isEmpty());
    assertNoSkippedStageResults();
    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_THREE));
    when(time.getDeltaTime()).thenReturn(oneConfig.bossIntroDuration);
    stageOne.update();
    assertEquals(FinalBossStageOneState.TRANSFORMING, stageOne.getState());
  }

  @Test
  void shouldStartStageTwoAtScaledHealthWithItsRealShieldAndIceFloor() {
    createBoss(250);

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));

    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertEquals(200, bossStats.getHealth());
    assertEquals(150, bossStats.getMinimumHealth());
    assertTrue(protection.isShielded());
    assertTrue(bossStats.isInvulnerable());
    assertFalse(phases.isTransitioning());
    assertTrue(stageTwo.isAttacking());
    assertNotNull(arena.getBounds());
    assertEquals(List.of(FinalBossPhase.STAGE_TWO), phaseChanges);
    assertEquals(List.of(player), stageTwoStarts);
    assertNoSkippedStageResults();
    when(time.getDeltaTime()).thenReturn(10f);
    stageOne.update();
    stageThree.update();
    assertTrue(summons.isEmpty());
    assertEquals(FinalBossStageThreeState.INACTIVE, stageThree.getState());
    bossStats.takeDamage(10, player);
    assertEquals(200, bossStats.getHealth());
    protection.takeStageTwoIceDamage(1, player);
    assertEquals(199, bossStats.getHealth());
  }

  @Test
  void shouldStartStageThreeInsideTheFocusedViewWithoutStartingOrCompletingStageTwo() {
    createBoss(250);
    boss.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(5f, -3f);
    boss.getEvents()
        .addListener(
            FinalBossEvents.PHASE_CHANGED,
            (FinalBossPhase ignored) -> {
              assertInsideCamera();
              assertEquals(
                  Vector2.Zero,
                  boss.getComponent(PhysicsComponent.class).getBody().getLinearVelocity());
            });

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_THREE));

    assertEquals(150, bossStats.getHealth());
    assertEquals(50, bossStats.getMinimumHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertEquals(FinalBossStageThreeState.WAVE_ONE, stageThree.getState());
    assertFalse(phases.isTransitioning());
    assertFalse(bossStats.isInvulnerable());
    assertFalse(stageTwo.isAttacking());
    assertEquals(List.of(FinalBossPhase.STAGE_THREE), phaseChanges);
    assertTrue(stageTwoStarts.isEmpty());
    assertNoSkippedStageResults();
    when(time.getDeltaTime()).thenReturn(10f);
    stageOne.update();
    stageTwo.update();
    assertTrue(summons.isEmpty());
    assertEquals(0, stageOne.getActiveSummonCount());
    assertTrue(stageTwo.getFireController().fireballs.isEmpty());
    bossStats.takeDamage(1, player);
    assertEquals(149, bossStats.getHealth());
  }

  @Test
  void shouldUseTheEncounterConfigurationsForStageTwoHealthAndItsFloor() {
    oneConfig.stageTwoStartingHealth = 0.75f;
    twoConfig.stageThreeHealthThreshold = 0.55f;
    createBoss(333);

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));

    assertEquals(250, bossStats.getHealth());
    assertEquals(183, bossStats.getMinimumHealth());
  }

  @Test
  void shouldUseTheEncounterConfigurationsForStageThreeEntryAndWaveTwoFloor() {
    twoConfig.stageThreeHealthThreshold = 0.55f;
    threeConfig.waveTwoHealthThreshold = 0.15f;
    createBoss(333);

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_THREE));

    assertEquals(183, bossStats.getHealth());
    assertEquals(50, bossStats.getMinimumHealth());
    assertNoSkippedStageResults();
  }

  @Test
  void shouldAllowTheRealFireproofAttemptAfterAStageTwoDebugStart() {
    createBoss(250);
    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));
    assertTrue(unlockedNames.isEmpty());

    protection.takeStageTwoIceDamage(10000, player);

    assertEquals(150, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_THREE, phases.getCurrentPhase());
    assertTrue(phases.isTransitioning());
    assertEquals(List.of(player), stageTwoCompletions);
    assertEquals(List.of(FinalBossPhase.STAGE_TWO), completedStages);
    assertEquals(List.of("I'm fireproof!"), unlockedNames);
  }

  @Test
  void shouldStillFailTheRealFireproofAttemptIfFireHitsAfterADebugStart() {
    createBoss(250);
    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));
    stageTwo
        .getFireController()
        .fireballs
        .add(
            new FinalBossStageTwoFireController.Fireball(
                100L, player.getCenterPosition(), new Vector2()));
    when(time.getDeltaTime()).thenReturn(0.01f);
    stageTwo.update();
    assertTrue(stageTwo.getFireController().fireballs.isEmpty());

    protection.takeStageTwoIceDamage(10000, player);

    assertEquals(List.of(player), stageTwoCompletions);
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldRejectInvalidStagesWithoutConsumingTheFreshEntryChoice() {
    createBoss(250);
    Vector2 originalPosition = boss.getPosition();

    assertFalse(phases.startDebugStage(null));
    assertFalse(phases.startDebugStage(FinalBossPhase.DEFEATED));

    assertEquals(originalPosition, boss.getPosition());
    assertEquals(250, bossStats.getHealth());
    assertEquals(FinalBossStageOneState.INTRO, stageOne.getState());
    assertTrue(phaseChanges.isEmpty());
    assertNoSkippedStageResults();
    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_THREE));
  }

  @Test
  void shouldRejectBootingAnAlreadyProgressingStageOneEncounter() {
    createBoss(250);
    when(time.getDeltaTime()).thenReturn(oneConfig.bossIntroDuration);
    stageOne.update();

    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_TWO));
    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_THREE));

    assertEquals(FinalBossPhase.STAGE_ONE, phases.getCurrentPhase());
    assertEquals(FinalBossStageOneState.TRANSFORMING, stageOne.getState());
    assertEquals(250, bossStats.getHealth());
    assertTrue(phaseChanges.isEmpty());
  }

  @Test
  void shouldRejectRestartingOrChangingTheEntryOfAnAlreadyBootedBoss() {
    createBoss(250);
    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));
    protection.takeStageTwoIceDamage(1, player);
    Vector2 position = boss.getPosition();

    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_ONE));
    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_TWO));
    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_THREE));

    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertEquals(199, bossStats.getHealth());
    assertEquals(position, boss.getPosition());
    assertEquals(List.of(player), stageTwoStarts);
    assertEquals(List.of(FinalBossPhase.STAGE_TWO), phaseChanges);
    assertNoSkippedStageResults();
  }

  @Test
  void shouldRejectADisposedStageOneControllerBeforeAnyMutation() {
    createBoss(250);
    stageOne.dispose();

    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_THREE));

    assertEquals(250, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_ONE, phases.getCurrentPhase());
    assertTrue(phaseChanges.isEmpty());
    assertNoSkippedStageResults();
  }

  @Test
  void shouldRejectADisposedPhaseControllerBeforeAnyMutation() {
    createBoss(250);
    phases.dispose();

    assertFalse(phases.startDebugStage(FinalBossPhase.STAGE_TWO));

    assertEquals(250, bossStats.getHealth());
    assertEquals(FinalBossPhase.STAGE_ONE, phases.getCurrentPhase());
    assertTrue(phaseChanges.isEmpty());
    assertNoSkippedStageResults();
  }

  @Test
  void shouldNotStartStageTwoAfterAPhaseListenerDisposesTheController() {
    createBoss(250);
    boss.getEvents()
        .addListener(FinalBossEvents.PHASE_CHANGED, (FinalBossPhase ignored) -> phases.dispose());

    assertTrue(phases.startDebugStage(FinalBossPhase.STAGE_TWO));

    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertFalse(stageTwo.isAttacking());
    assertTrue(stageTwoStarts.isEmpty());
    assertTrue(protection.isShielded());
    assertEquals(0, bossStats.getMinimumHealth());
    assertEquals(FinalBossStageThreeState.INACTIVE, stageThree.getState());
    assertNoSkippedStageResults();
  }

  private void createBoss(int maxHealth) {
    phases = new FinalBossPhaseControllerComponent();
    protection = new FinalBossDamageControllerComponent();
    movement = new FinalBossMovementComponent(player, oneConfig, twoConfig);
    movement.setCamera(camera);
    stageOne = new FinalBossStageOneComponent(player, summons::add, oneConfig);
    arena = new FinalBossStageTwoArenaComponent(player, twoConfig);
    arena.setCamera(camera, null);
    stageTwo = new FinalBossStageTwoComponent(player, twoConfig);
    stageThree = new FinalBossStageThreeComponent(player, summons::add, threeConfig);
    bossStats = new CombatStatsComponent(maxHealth, 0);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(protection)
            .addComponent(movement)
            .addComponent(stageOne)
            .addComponent(arena)
            .addComponent(stageTwo)
            .addComponent(stageThree)
            .addComponent(new FinalBossAchievementComponent(player));
    boss.setScale(2f, 2f);
    boss.setPosition(1000f, 1000f);
    boss.create();
    boss.getEvents().<FinalBossPhase>addListener(FinalBossEvents.PHASE_CHANGED, phaseChanges::add);
    boss.getEvents()
        .<FinalBossPhase>addListener(FinalBossEvents.STAGE_COMPLETED, completedStages::add);
    boss.getEvents().<Entity>addListener(FinalBossEvents.STAGE_TWO_STARTED, stageTwoStarts::add);
    boss.getEvents()
        .<Entity>addListener(FinalBossEvents.STAGE_TWO_COMPLETED, stageTwoCompletions::add);
  }

  private void assertInsideCamera() {
    Vector2 position = boss.getPosition();
    Vector2 size = boss.getScale();
    assertTrue(position.x >= camera.position.x - camera.viewportWidth / 2f + 0.25f);
    assertTrue(position.y >= camera.position.y - camera.viewportHeight / 2f + 0.25f);
    assertTrue(position.x + size.x <= camera.position.x + camera.viewportWidth / 2f - 0.25f);
    assertTrue(position.y + size.y <= camera.position.y + camera.viewportHeight / 2f - 0.25f);
    assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
  }

  private void assertNoSkippedStageResults() {
    assertTrue(stageTwoCompletions.isEmpty());
    assertTrue(completedStages.isEmpty());
    assertTrue(unlockedNames.isEmpty());
  }

  private static void registerAchievement(
      AchievementService service, String name, String bossAchievementId) {
    AchievementConfig definition = new AchievementConfig();
    definition.name = name;
    definition.type = "finalBoss";
    definition.bossAchievementId = bossAchievementId;
    service.register(AchievementsFactory.build(definition));
  }
}
