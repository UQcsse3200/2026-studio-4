package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.boss.FinalBossEvents;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.components.statuseffects.LastStandEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerPetrificationFlashTest {
  private static final Color RED = new Color(1f, 0.2f, 0.2f, 1f);
  private GameTime time;
  private long now;
  private Entity player;
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private PlayerPetrificationComponent petrification;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    ServiceLocator.registerTimeSource(time);
    stats = new CombatStatsComponent(100, 10, 3f, 1f);
    effects = new StatusEffectsControllerComponent();
    petrification = new PlayerPetrificationComponent();
    player = new Entity().addComponent(stats).addComponent(effects).addComponent(petrification);
    player.create();
  }

  @Test
  void blinkingShouldShareTheActualSlowLifetimeAndStopExactlyAtExpiry() {
    assertNull(effects.getTint());
    applyPetrification();
    assertEquals(RED, effects.getTint());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed());
    now = 250L;
    assertNull(effects.getTint());
    assertTrue(petrification.isPetrified());
    now = 500L;
    assertEquals(RED, effects.getTint());
    now = 1999L;
    assertTrue(petrification.isPetrified());
    now = 2000L;
    assertNull(effects.getTint());
    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed());
  }

  @Test
  void phaseCleanupShouldRestoreOtherAbilityTintsAndTransparency() {
    effects.addStatusEffect(new LastStandEffect(time, 10000L));
    effects.addStatusEffect(new InvisibilityEffect(time, 10000L));
    Color original = effects.getTint();
    applyPetrification();
    assertEquals(new Color(1f, 0.35f * 0.2f, 0.35f * 0.2f, 0.35f), effects.getTint());
    assertTrue(effects.isConcealed());
    now = 250L;
    assertEquals(original, effects.getTint());

    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_CLEAR_REQUESTED);
    now = 500L;
    assertEquals(original, effects.getTint());
    assertFalse(petrification.isPetrified());
    assertEquals(4.5f, stats.getEffectiveMovementSpeed());
  }

  @Test
  void deathAndDisposalShouldStopPetrificationBlinkImmediately() {
    applyPetrification();
    stats.setHealth(0);
    assertNull(effects.getTint());
    stats.setHealth(100);
    applyPetrification();
    assertEquals(RED, effects.getTint());
    petrification.dispose();
    assertNull(effects.getTint());
    applyPetrification();
    assertNull(effects.getTint());
  }

  private void applyPetrification() {
    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED, 0.5f, 2f);
  }
}
