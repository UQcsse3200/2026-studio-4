package com.csse3200.game.items;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
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
    inventory.setEntity(player);
  }

  @Test
  void ShouldApplyStats() {
    StrengthCharm strengthCharm = new StrengthCharm();
    inventory.addCharm(strengthCharm);

    verify(stats).addBaseAttack(strengthCharm.value);
  }

  @Test
  void ShouldNotApplyStatsTwice() {
    StrengthCharm strengthCharm = new StrengthCharm();
    inventory.addCharm(strengthCharm);

    verify(stats).addBaseAttack(strengthCharm.value);

    inventory.addCharm(strengthCharm);
    verify(stats, times(1)).addBaseAttack(anyInt());
  }
}
