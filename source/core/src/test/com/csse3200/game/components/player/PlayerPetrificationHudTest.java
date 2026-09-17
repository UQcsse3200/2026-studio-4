package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.boss.FinalBossEvents;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerPetrificationHudTest {
  private long now;
  private Entity player;
  private CombatStatsComponent stats;
  private PlayerPetrificationComponent petrification;
  private PlayerStatsDisplay display;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    ServiceLocator.registerTimeSource(time);

    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    stats = new CombatStatsComponent(100, 10, 3f, 1f);
    petrification = new PlayerPetrificationComponent();
    display = new PlayerStatsDisplay();
    player =
        new Entity()
            .addComponent(stats)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(petrification)
            .addComponent(display);
    player.create();
  }

  @Test
  void hitShouldSlowAndShowPetrified() {
    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED, 0.5f, 2f);
    display.update();

    assertTrue(petrification.isPetrified());
    assertEquals(1.5f, stats.getEffectiveMovementSpeed());
    assertEquals("Petrified", petrification.getHudText());
    assertEquals("Petrified", hudText());
  }

  @Test
  void expireShouldRestoreSpeedAndHideHud() {
    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED, 0.5f, 2f);
    now = 2000L;
    display.update();

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    assertEquals("", petrification.getHudText());
    assertEquals("", hudText());
  }

  @Test
  void clearShouldRemoveSlowAndHideHud() {
    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED, 0.5f, 2f);
    player.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_CLEAR_REQUESTED);
    display.update();

    assertFalse(petrification.isPetrified());
    assertEquals(3f, stats.getEffectiveMovementSpeed());
    assertEquals("", petrification.getHudText());
    assertEquals("", hudText());
  }

  private String hudText() {
    try {
      Field field = PlayerStatsDisplay.class.getDeclaredField("petrifiedLabel");
      field.setAccessible(true);
      return ((Label) field.get(display)).getText().toString();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
