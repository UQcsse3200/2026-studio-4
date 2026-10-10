package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementConfig;
import com.csse3200.game.components.achievements.AchievementsFactory;
import com.csse3200.game.components.statuseffects.Burning;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.entities.factories.FinalBossFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class FinalBossAchievementComponentTest {
  private static final String ACHIEVEMENT_NAME = "I need a break too";

  private final List<Entity> spawned = new ArrayList<>();
  private final List<String> unlockedNames = new ArrayList<>();
  private MockedStatic<FinalBossFactory> factory;
  private FinalBossStageOneConfig config;
  private FinalBossStageOneComponent stageOne;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossDamageControllerComponent damage;
  private FinalBossAchievementComponent tracker;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private Entity player;
  private Achievement achievement;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    AchievementConfig achievementConfig = new AchievementConfig();
    achievementConfig.name = ACHIEVEMENT_NAME;
    achievementConfig.type = "finalBossBreakRespected";
    achievement = AchievementsFactory.build(achievementConfig);
    AchievementService achievements = new AchievementService();
    achievements.register(achievement);
    achievements
        .getEvents()
        .addListener("achievementUnlocked", (String name) -> unlockedNames.add(name));
    ServiceLocator.registerAchievementService(achievements);

    factory = mockStatic(FinalBossFactory.class);
    factory
        .when(() -> FinalBossFactory.createExplosiveSummon(any(), any(), anyFloat(), anyFloat()))
        .thenAnswer(invocation -> new Entity());
    createEncounter();
  }

  @AfterEach
  void tearDown() {
    factory.close();
  }

  @Test
  void shouldUnlockOnlyAfterTheFullBreakAndNotifyOnce() {
    enterBreak();
    assertFalse(achievement.isUnlocked());

    advance(config.breakWindowDuration - 0.1f);
    assertEquals(FinalBossStageOneState.BREAK_WINDOW, stageOne.getState());
    assertFalse(achievement.isUnlocked());

    advance(0.11f);
    assertEquals(FinalBossStageOneState.SUMMONING_TWO, stageOne.getState());
    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);

    advance(config.bossSummonCastDuration);
    removeAllSummons();
    advance(20f);
    assertEquals(FinalBossPhase.STAGE_TWO, phases.getCurrentPhase());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldIgnoreHitsBeforeAndAfterTheBreak() {
    bossStats.takeDamage(10, player);
    enterBreak();
    advance(config.breakWindowDuration);
    bossStats.takeDamage(10, player);

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldCountAHitFromAnEarlierBreakEntryListener() {
    tracker.dispose();
    stageOne.dispose();
    createEncounter(
        boss ->
            boss.getEvents()
                .addListener(
                    FinalBossEvents.STAGE_ONE_STATE_CHANGED,
                    (FinalBossStageOneState state) -> {
                      if (state == FinalBossStageOneState.BREAK_WINDOW) {
                        bossStats.takeDamage(8, player);
                      }
                    }));
    enterBreak();
    assertEquals(98, bossStats.getHealth());

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldIgnoreAHitFromAnEarlierBreakExitListener() {
    tracker.dispose();
    stageOne.dispose();
    createEncounter(
        boss ->
            boss.getEvents()
                .addListener(
                    FinalBossEvents.STAGE_ONE_STATE_CHANGED,
                    (FinalBossStageOneState state) -> {
                      if (state == FinalBossStageOneState.SUMMONING_TWO) {
                        bossStats.takeDamage(8, player);
                      }
                    }));
    enterBreak();
    advance(config.breakWindowDuration);

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  @Test
  void shouldRejectAHitDuringTheBreak() {
    enterBreak();
    bossStats.takeDamage(8, player);
    assertEquals(98, bossStats.getHealth());

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldRejectAShieldedHitEvenWhenHealthDoesNotChange() {
    enterBreak();
    damage.enableShield();
    bossStats.takeDamage(8, player);
    assertEquals(100, bossStats.getHealth());

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldRejectAHitWhoseReducedDamageRoundsToZero() {
    enterBreak();
    bossStats.takeDamage(1, player);
    assertEquals(100, bossStats.getHealth());

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldRejectAnUnattributedHit() {
    enterBreak();
    bossStats.takeDamage(8);

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldRejectAProjectileThatWasCreatedBeforeTheBreak() {
    Entity projectile = new Entity();
    enterBreak();
    bossStats.takeDamage(8, projectile);

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldRejectAnExistingBurnThatTicksDuringTheBreak() {
    try (MockedConstruction<GameTime> clocks =
        mockConstruction(
            GameTime.class,
            (clock, context) -> {
              when(clock.getTime()).thenReturn(100L);
              when(clock.getTimeSince(anyLong())).thenReturn(10L);
            })) {
      Burning burn = new Burning(8, 1L, 1000L, bossStats);
      enterBreak();
      assertFalse(burn.update());
      assertEquals(98, bossStats.getHealth());

      finishBreakWithoutUnlocking();
    }
  }

  @Test
  void shouldNotCountNonPositiveDamageAsAHit() {
    enterBreak();
    bossStats.takeDamage(0, player);
    bossStats.takeDamage(-1, player);
    advance(config.breakWindowDuration);

    assertTrue(achievement.isUnlocked());
  }

  @Test
  void shouldCancelWhenThePhaseChangesBeforeTheBreakEnds() {
    enterBreak();
    advance(config.breakWindowDuration / 2f);
    phases.completeStage(FinalBossPhase.STAGE_ONE);
    advance(config.breakWindowDuration);

    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  @Test
  void shouldNotUnlockAfterTheTrackerIsDisposed() {
    enterBreak();
    tracker.dispose();

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldCancelWhenThePlayerDiesDuringTheBreak() {
    enterBreak();
    playerStats.setHealth(0);
    playerStats.setHealth(100);

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldCancelWhenTheBossDiesDuringTheBreak() {
    enterBreak();
    bossStats.setHealth(0);
    bossStats.setHealth(100);

    finishBreakWithoutUnlocking();
  }

  @Test
  void shouldAllowAFreshEncounterAfterAnEarlierAttemptFailed() {
    enterBreak();
    bossStats.takeDamage(8, player);
    finishBreakWithoutUnlocking();
    tracker.dispose();
    stageOne.dispose();

    createEncounter();
    enterBreak();
    advance(config.breakWindowDuration);

    assertTrue(achievement.isUnlocked());
    assertEquals(List.of(ACHIEVEMENT_NAME), unlockedNames);
  }

  private void createEncounter() {
    createEncounter(boss -> {});
  }

  private void createEncounter(Consumer<Entity> beforeTrackerCreate) {
    spawned.clear();
    config = new FinalBossStageOneConfig();
    bossStats = new CombatStatsComponent(100, 0);
    playerStats = new CombatStatsComponent(100, 0);
    player = new Entity().addComponent(playerStats);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    phases = new FinalBossPhaseControllerComponent();
    damage = new FinalBossDamageControllerComponent();
    stageOne = new FinalBossStageOneComponent(player, spawned::add, config);
    tracker = new FinalBossAchievementComponent(player);
    Entity boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(movement)
            .addComponent(phases)
            .addComponent(damage)
            .addComponent(stageOne)
            .addComponent(tracker);
    phases.create();
    damage.create();
    stageOne.create();
    beforeTrackerCreate.accept(boss);
    tracker.create();
  }

  private void enterBreak() {
    advance(config.bossIntroDuration);
    advance(config.bossTransformDuration);
    advance(config.bossSummonCastDuration);
    assertEquals(FinalBossStageOneState.WAVE_ONE, stageOne.getState());
    removeAllSummons();
    assertEquals(FinalBossStageOneState.BREAK_WINDOW, stageOne.getState());
  }

  private void removeAllSummons() {
    for (Entity summon : new ArrayList<>(spawned)) {
      summon.getEvents().trigger(FinalBossEvents.SUMMON_REMOVED, summon);
    }
  }

  private void finishBreakWithoutUnlocking() {
    advance(config.breakWindowDuration);
    assertEquals(FinalBossStageOneState.SUMMONING_TWO, stageOne.getState());
    assertFalse(achievement.isUnlocked());
    assertTrue(unlockedNames.isEmpty());
  }

  private void advance(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stageOne.update();
  }
}
