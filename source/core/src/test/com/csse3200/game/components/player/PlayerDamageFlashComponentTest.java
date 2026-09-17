package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.boss.FinalBossPhase;
import com.csse3200.game.components.boss.FinalBossPhaseControllerComponent;
import com.csse3200.game.components.boss.FinalBossProximityDamageComponent;
import com.csse3200.game.components.boss.FinalBossStageOneComponent;
import com.csse3200.game.components.boss.FinalBossStageOneState;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerDamageFlashComponentTest {
  private static final Color RED = new Color(1f, 0.2f, 0.2f, 1f);
  private GameTime time;
  private long now;
  private Entity player;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private PlayerDamageFlashComponent feedback;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    ServiceLocator.registerTimeSource(time);
    stats = new CombatStatsComponent(100, 10);
    effects = new StatusEffectsControllerComponent();
    feedback = new PlayerDamageFlashComponent();
    player = new Entity().addComponent(stats).addComponent(effects).addComponent(feedback);
    player.create();
  }

  @Test
  void healthLossShouldBlinkImmediatelyAndExpireWithoutAnUpdateTick() {
    stats.takeDamage(5);
    assertEquals(95, stats.getHealth());
    assertEquals(RED, effects.getTint());

    now = 250L;
    assertNull(effects.getTint());
    now = 500L;
    assertEquals(RED, effects.getTint());
    now = 600L;
    assertNull(effects.getTint());
  }

  @Test
  void healingAndUnchangedHealthShouldNotFlash() {
    stats.setHealth(100);
    assertNull(effects.getTint());
    stats.takeDamage(10);
    now = 600L;
    stats.addHealth(5);
    assertNull(effects.getTint());
    stats.takeDamage(1);
    assertEquals(RED, effects.getTint());
  }

  @Test
  void invulnerabilityAndFullyAbsorbedHitsShouldNotFlash() {
    stats.setInvulnerable(true);
    stats.takeDamage(5);
    assertEquals(100, stats.getHealth());
    assertNull(effects.getTint());

    stats.setInvulnerable(false);
    effects.activateTimed();
    stats.takeDamage(5);
    assertEquals(100, stats.getHealth());
    assertNull(effects.getTint());
  }

  @Test
  void repeatedDamageShouldRefreshOneFlashWithoutMultiplyingItsTint() {
    stats.takeDamage(5);
    now = 100L;
    stats.takeDamage(5);
    assertEquals(RED, effects.getTint());
    now = 600L;
    assertEquals(RED, effects.getTint());
    now = 700L;
    assertNull(effects.getTint());
  }

  @Test
  void deathAndDisposedFeedbackShouldRemoveTheFlash() {
    stats.takeDamage(5);
    stats.setHealth(0);
    assertNull(effects.getTint());
    stats.setHealth(100);
    stats.takeDamage(5);
    assertEquals(RED, effects.getTint());
    feedback.dispose();
    stats.takeDamage(5);
    assertNull(effects.getTint());
  }

  @Test
  void disposedStatusControllerShouldRejectFurtherVisualEffects() {
    stats.takeDamage(5);
    effects.dispose();
    stats.takeDamage(5);
    assertEquals(90, stats.getHealth());
    assertNull(effects.getTint());
  }

  @Test
  void waveOneProximityDamageShouldFlashOnlyWhenTheDamageIntervalCompletes() {
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    FinalBossStageOneComponent stageOne = mock(FinalBossStageOneComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    when(stageOne.getState()).thenReturn(FinalBossStageOneState.WAVE_ONE);
    Entity boss = mock(Entity.class);
    when(boss.getCenterPosition()).thenReturn(new Vector2(0.5f, 0.5f));
    when(boss.getComponent(CombatStatsComponent.class))
        .thenReturn(new CombatStatsComponent(100, 10));
    when(boss.getComponent(FinalBossPhaseControllerComponent.class)).thenReturn(phases);
    when(boss.getComponent(FinalBossStageOneComponent.class)).thenReturn(stageOne);
    FinalBossProximityDamageComponent proximity =
        new FinalBossProximityDamageComponent(player, new FinalBossStageOneConfig());
    proximity.setEntity(boss);
    proximity.create();

    when(time.getDeltaTime()).thenReturn(0.5f);
    proximity.update();
    assertEquals(100, stats.getHealth());
    assertNull(effects.getTint());
    proximity.update();
    assertEquals(95, stats.getHealth());
    assertEquals(RED, effects.getTint());

    now = 600L;
    stats.setInvulnerable(true);
    when(time.getDeltaTime()).thenReturn(1f);
    proximity.update();
    assertEquals(95, stats.getHealth());
    assertNull(effects.getTint());
  }
}
