package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableUseComponentTest {
  @Test
  void shouldConsumeHealthPotionAndHeal() {
    Entity player = createPlayer(40);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemType.HEALTH_POTION);

    player.getEvents().trigger("useConsumable", ItemType.HEALTH_POTION);

    assertEquals(65, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(0, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void shouldNotWasteHealthPotionAtFullHealth() {
    Entity player = createPlayer(100);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemType.HEALTH_POTION);

    player.getEvents().trigger("useConsumable", ItemType.HEALTH_POTION);

    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemType.HEALTH_POTION));
  }

  @Test
  void shouldIgnoreConsumablesWithoutDemoEffects() {
    Entity player = createPlayer(40);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addConsumable(ItemType.SHIELD);

    player.getEvents().trigger("useConsumable", ItemType.SHIELD);

    assertEquals(40, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, inventory.getConsumableCount(ItemType.SHIELD));
  }

  private static Entity createPlayer(int health) {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new InventoryComponent(0))
            .addComponent(new ConsumableUseComponent());
    player.create();
    player.getComponent(CombatStatsComponent.class).setHealth(health);
    return player;
  }
}
