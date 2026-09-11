package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossProximityDamageComponentTest {
  private Entity player;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent bossStats;
  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossStageOneComponent stageOneController;
  private FinalBossProximityDamageComponent component;
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);

    playerStats = new CombatStatsComponent(100, 0);
    player = new Entity().addComponent(playerStats);
    player.setPosition(0f, 0f);

    bossStats = new CombatStatsComponent(100, 0);
    phaseController = mock(FinalBossPhaseControllerComponent.class);
    stageOneController = mock(FinalBossStageOneComponent.class);

    when(phaseController.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    when(stageOneController.getState()).thenReturn(FinalBossStageOneState.WAVE_ONE);

    Entity boss = mock(Entity.class);
    when(boss.getCenterPosition()).thenReturn(new Vector2(0.5f, 0.5f));
    when(boss.getComponent(CombatStatsComponent.class)).thenReturn(bossStats);
    when(boss.getComponent(FinalBossPhaseControllerComponent.class)).thenReturn(phaseController);
    when(boss.getComponent(FinalBossStageOneComponent.class)).thenReturn(stageOneController);

    component = new FinalBossProximityDamageComponent(player, new FinalBossStageOneConfig());
    component.setEntity(boss);
    component.create();
  }

  @Test
  void shouldDamagePeriodicallyAfterAFullInterval() {
    advance(0.5f);
    assertEquals(100, playerStats.getHealth());

    advance(0.5f);
    assertEquals(95, playerStats.getHealth());

    advance(1f);
    assertEquals(90, playerStats.getHealth());
    assertEquals(100, bossStats.getHealth());
  }

  @Test
  void shouldResetExposureWhenPlayerLeavesRange() {
    advance(0.75f);

    player.setPosition(10f, 0f);
    advance(1f);
    assertEquals(100, playerStats.getHealth());

    player.setPosition(0f, 0f);
    advance(0.25f);
    assertEquals(100, playerStats.getHealth());

    advance(0.75f);
    assertEquals(95, playerStats.getHealth());
  }

  @Test
  void shouldIncludeRadiusBoundaryButExcludeOutside() {
    player.setPosition(1.5f, 0f);
    advance(1f);
    assertEquals(95, playerStats.getHealth());

    player.setPosition(1.51f, 0f);
    advance(1f);
    assertEquals(95, playerStats.getHealth());
  }

  @Test
  void shouldStopOutsideWaveOne() {
    advance(0.75f);

    when(stageOneController.getState()).thenReturn(FinalBossStageOneState.BREAK_WINDOW);
    advance(1f);

    when(stageOneController.getState()).thenReturn(FinalBossStageOneState.WAVE_TWO);
    advance(1f);

    when(stageOneController.getState()).thenReturn(FinalBossStageOneState.COMPLETE);
    advance(1f);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void shouldStopWhenBossEntersStageTwo() {
    when(phaseController.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);

    advance(1f);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void shouldRespectPlayerInvulnerabilityAndDamageMultiplier() {
    playerStats.setInvulnerable(true);
    advance(1f);
    assertEquals(100, playerStats.getHealth());

    playerStats.setInvulnerable(false);
    playerStats.setIncomingDamageMultiplier(0.5f);
    advance(1f);
    assertEquals(97, playerStats.getHealth());
  }

  @Test
  void shouldStopAfterBossDeath() {
    bossStats.setHealth(0);

    advance(1f);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void shouldNotSendFurtherHitsToDeadPlayer() {
    int[] hitCount = {0};
    player.getEvents().addListener("hitReaction", (Entity attacker) -> hitCount[0]++);
    playerStats.setHealth(0);

    advance(1f);

    assertEquals(0, playerStats.getHealth());
    assertEquals(0, hitCount[0]);
  }

  @Test
  void shouldStopAfterDisposal() {
    advance(0.75f);
    component.dispose();

    advance(1f);

    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void shouldAvoidCatchUpDamageBurstAfterLongFrame() {
    advance(5f);

    assertEquals(95, playerStats.getHealth());
  }

  private void advance(float deltaTime) {
    when(time.getDeltaTime()).thenReturn(deltaTime);
    component.update();
  }
}
