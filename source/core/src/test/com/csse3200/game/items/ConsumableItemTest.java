package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.items.consumables.ShieldPotion;
import com.csse3200.game.items.consumables.SpeedPotion;
import com.csse3200.game.items.consumables.StrengthPotion;
import com.csse3200.game.services.GameTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsumableItemTest {
  private final List<ConsumableItem> potions =
      List.of(new ShieldPotion(1), new SpeedPotion(1), new StrengthPotion(1));

  @Test
  void timedPotionsRequireALivingPlayer() {
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    GameTime time = mock(GameTime.class);

    for (ConsumableItem potion : potions) {
      assertTrue(potion.canUse(stats, effects, time));
      assertFalse(potion.canUse(null, effects, time));
    }

    when(stats.isDead()).thenReturn(true);
    for (ConsumableItem potion : potions) {
      assertFalse(potion.canUse(stats, effects, time));
    }
  }

  @Test
  void timedPotionsStillRequireAnActiveControllerAndClock() {
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    StatusEffectsControllerComponent effects = mock(StatusEffectsControllerComponent.class);
    GameTime time = mock(GameTime.class);

    for (ConsumableItem potion : potions) {
      assertFalse(potion.canUse(stats, null, time));
      assertFalse(potion.canUse(stats, effects, null));
    }

    when(effects.isDisposed()).thenReturn(true);
    for (ConsumableItem potion : potions) {
      assertFalse(potion.canUse(stats, effects, time));
    }
  }
}
