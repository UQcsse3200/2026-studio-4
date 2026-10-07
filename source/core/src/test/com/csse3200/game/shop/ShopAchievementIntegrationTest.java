package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.player.ConsumablePurchaseResult;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopAchievementIntegrationTest {
  @Test
  void achievementObserverSeesCommittedRewardAndBalance() {
    InventoryComponent inventory = new InventoryComponent(30);
    new Entity().addComponent(inventory);
    AchievementService achievements = mock(AchievementService.class);
    ServiceLocator.registerAchievementService(achievements);
    doAnswer(
            invocation -> {
              assertEquals(20, inventory.getGold());
              assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
              return null;
            })
        .when(achievements)
        .update(any());
    assertEquals(
        ConsumablePurchaseResult.SUCCESS,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10, 2));
    verify(achievements).update(argThat(context -> context.goldTotal == 20));
  }

  @Test
  void rejectedPurchaseDoesNotNotifyAchievementsOrChangeInventory() {
    InventoryComponent inventory = new InventoryComponent(5);
    AchievementService achievements = mock(AchievementService.class);
    ServiceLocator.registerAchievementService(achievements);
    assertEquals(
        ConsumablePurchaseResult.INSUFFICIENT_GOLD,
        inventory.tryPurchaseConsumable(ItemIds.HEALTH_POTION, 10));
    verifyNoInteractions(achievements);
    assertEquals(5, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }
}
