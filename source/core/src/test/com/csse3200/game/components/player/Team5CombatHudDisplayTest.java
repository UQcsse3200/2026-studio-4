package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class Team5CombatHudDisplayTest {
  @Test
  void shouldFormatGoldAndClampNegativeValues() {
    assertEquals("Gold: 12", Team5CombatHudDisplay.formatGold(12));
    assertEquals("Gold: 0", Team5CombatHudDisplay.formatGold(-1));
  }

  @Test
  void shouldHideZeroQuantityAndFormatPositiveQuantity() {
    assertEquals("", Team5CombatHudDisplay.formatQuantity(0));
    assertEquals("", Team5CombatHudDisplay.formatQuantity(-1));
    assertEquals("×1", Team5CombatHudDisplay.formatQuantity(1));
    assertEquals("×3", Team5CombatHudDisplay.formatQuantity(3));
  }
}
