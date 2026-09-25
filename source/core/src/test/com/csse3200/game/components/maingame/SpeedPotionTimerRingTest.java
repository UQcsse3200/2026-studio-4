package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SpeedPotionTimerRingTest {
  @Test
  void blueRingShrinksAndDisappearsWithRemainingPotionTime() {
    ConsumableEffectComponent effects = mock(ConsumableEffectComponent.class);
    Texture pixel = mock(Texture.class);
    Batch batch = mock(Batch.class);
    SpeedPotionTimerRing ring = new SpeedPotionTimerRing(effects, pixel);
    ring.setSize(72f, 72f);

    when(effects.getRemainingFraction(ItemIds.SPEED_POTION)).thenReturn(1f, 0.5f, 0f);
    ring.draw(batch, 1f);
    long full = drawCalls(batch);
    clearInvocations(batch);

    ring.draw(batch, 1f);
    long half = drawCalls(batch);
    clearInvocations(batch);

    ring.draw(batch, 1f);
    assertTrue(full > half);
    assertTrue(half > 0);
    assertEquals(0, drawCalls(batch));
  }

  private long drawCalls(Batch batch) {
    return mockingDetails(batch).getInvocations().stream()
        .filter(invocation -> invocation.getMethod().getName().equals("draw"))
        .count();
  }
}
