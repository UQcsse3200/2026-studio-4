package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemGambleConfigTest {
  @Test
  void assetLoadsValidRewardsAndNormalisedOdds() {
    ItemGambleConfig config = ItemGambleConfig.load("configs/shops/merchant-gambling.json");
    assertTrue(config.cost() > 0);
    assertEquals(
        1d, config.entries().stream().mapToDouble(config.table()::probabilityOf).sum(), 1e-12);
    assertTrue(config.entries().stream().anyMatch(GambleEntry::isBust));
  }

  @Test
  void rejectsInvalidCatalogRewardsDuplicatesAndNonpositiveCost() {
    List<GambleEntry> bustEntries = List.of(GambleEntry.bust(1));
    assertThrows(IllegalArgumentException.class, () -> new ItemGambleConfig(0, bustEntries));
    List<GambleEntry> invalidRewardEntries = List.of(GambleEntry.of("invalid", 1, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new ItemGambleConfig(10, invalidRewardEntries));
    List<GambleEntry> duplicateRewardEntries =
        List.of(
            GambleEntry.of(ItemIds.HEALTH_POTION, 1, 1),
            GambleEntry.of(ItemIds.HEALTH_POTION, 2, 2));
    assertThrows(
        IllegalArgumentException.class, () -> new ItemGambleConfig(10, duplicateRewardEntries));
    List<GambleEntry> invalidBustEntries = List.of(new GambleEntry(null, 1, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new ItemGambleConfig(10, invalidBustEntries));
  }
}
