package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.player.Team5CombatHudDisplay.ConsumableSlot;
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
  void shouldFormatConsumableSlotsAndClampNegativeCounts() {
    assertEquals(
        "[1] Health x3", Team5CombatHudDisplay.formatSlot(ConsumableSlot.HEALTH, 3));
    assertEquals(
        "[2] Shield x0", Team5CombatHudDisplay.formatSlot(ConsumableSlot.SHIELD, -1));
    assertEquals("[3] Speed x2", Team5CombatHudDisplay.formatSlot(ConsumableSlot.SPEED, 2));
    assertEquals(
        "[4] Strength x1", Team5CombatHudDisplay.formatSlot(ConsumableSlot.STRENGTH, 1));
  }

  @Test
  void shouldFormatSelectedConsumable() {
    assertEquals("Selected: None", Team5CombatHudDisplay.formatSelected(null));
    assertEquals(
        "Selected: Strength",
        Team5CombatHudDisplay.formatSelected(ConsumableSlot.STRENGTH));
  }
}
