package com.csse3200.game.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.player.CharmEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ItemFactory;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFlowIntegrationTest {
  @Test
  void shouldTransferFactoryDropToInventoryAndApplyBuff() {
    Entity player = createPlayer();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);
    Entity droppedItem = ItemFactory.createDrop();
    Charm droppedCharm = droppedItem.getComponent(ItemComponent.class).getCharm();

    inventory.addCharm(droppedCharm);

    assertEquals(1, inventory.getCharmCount());
    assertSame(droppedCharm, inventory.getCharms().get(0));
    assertEquals(20, combatStats.getStrength());

    inventory.removeCharm(droppedCharm);

    assertEquals(0, inventory.getCharmCount());
    assertEquals(10, combatStats.getStrength());
  }

  private Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new InventoryComponent(0))
            .addComponent(new CharmEffectComponent());
    player.create();
    player.getComponent(CombatStatsComponent.class).setStrength(10);
    return player;
  }
}
