package com.csse3200.game.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.charms.StrengthCharm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(GameExtension.class)
public class StregnthCharmTest {
  @Mock private Entity player;
  @Mock private CombatStatsComponent stats;
  @Spy private InventoryComponent inventory = new InventoryComponent(100);

  @BeforeEach
  void setup() {
    when(player.getComponent(CombatStatsComponent.class)).thenReturn(stats);
    when(player.getComponent(InventoryComponent.class)).thenReturn(inventory);
    inventory.setEntity(player);
  }

  @Test
  void ShouldPickUp() {
    StrengthCharm strengthCharm = new StrengthCharm();
    strengthCharm.pickUp(player);

    assertTrue(inventory.hasCharm(strengthCharm));
    verify(stats).addBaseAttack(anyInt());
  }

  @Test
  void ShouldNotPickUpTwice() {
    StrengthCharm strengthCharm = new StrengthCharm();

    strengthCharm.pickUp(player);
    strengthCharm.pickUp(player);

    assertTrue(inventory.hasCharm(strengthCharm));
    assertEquals(1, inventory.getCharmCount());

    verify(stats, times(1)).addBaseAttack(anyInt());
  }

  @Test
  void ShouldDrop() {
    StrengthCharm strengthCharm = new StrengthCharm();

    strengthCharm.pickUp(player);
    strengthCharm.drop(player);

    assertFalse(inventory.hasCharm(strengthCharm));
    assertEquals(0, inventory.getCharmCount());

    verify(stats, times(2)).addBaseAttack(anyInt());
  }
}
