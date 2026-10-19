package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyFuseFlashTest {
  private static final Color WHITE = Color.WHITE;

  private GameTime time;
  private long now;
  private StatusEffectsControllerComponent effects;
  private Entity enemy;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now);
    ServiceLocator.registerTimeSource(time);
    effects = new StatusEffectsControllerComponent();
    enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(effects)
            .addComponent(mock(AnimationRenderComponent.class))
            .addComponent(new EnemyAnimationController());
    enemy.create();
  }

  @Test
  void fuseStartedBlinksWhiteUntilTheFuseDurationExpires() {
    enemy.getEvents().trigger("fuseStarted", 2f);

    assertEquals(WHITE, effects.getTint());
    assertEquals(0.5f, effects.getGlow().a);

    now = 250L;
    assertNull(effects.getTint());
    assertNull(effects.getGlow());

    now = 500L;
    assertEquals(WHITE, effects.getTint());
    now = 2000L;
    assertNull(effects.getTint());
    assertNull(effects.getGlow());
  }
}
