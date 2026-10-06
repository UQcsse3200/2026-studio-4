package com.csse3200.game.items.consumables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.statuseffects.TimedStatusEffect;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.GameTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RegenerationPotionTest {
  @Test
  void healsOnlyOnTicksWithinItsDuration() {
    AtomicLong now = new AtomicLong();
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> now.get());
    CombatStatsComponent stats = new CombatStatsComponent(100, 10);
    stats.setHealth(40);
    RegenerationPotion potion =
        new RegenerationPotion(
            ItemIds.HEALTH_POTION,
            "Regeneration Potion",
            "Restores health over time.",
            "images/health_potion_pixel.png",
            1,
            5,
            3000) {};

    TimedStatusEffect effect = potion.use(stats, time);
    assertFalse(effect.update());
    assertEquals(40, stats.getHealth());
    now.set(999);
    assertFalse(effect.update());
    assertEquals(40, stats.getHealth());
    now.set(3000);
    assertTrue(effect.update());
    assertEquals(55, stats.getHealth());
    now.set(4000);
    assertTrue(effect.update());
    assertEquals(55, stats.getHealth());
  }
}
