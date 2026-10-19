package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TutorialPropFactoryTest {
  private static final List<String> STAND_ITEMS =
      List.of(
          ItemIds.HEALTH_POTION,
          ItemIds.SHIELD,
          ItemIds.SPEED_POTION,
          ItemIds.STRENGTH_POTION,
          ItemIds.FREEZE_BOMB,
          ItemIds.BURN_VIAL,
          ItemIds.MAGNET_POTION);

  @Test
  void iconPathsAreNamedAfterTheItemId() {
    assertEquals(
        "images/tutorial/icons/health_potion.png", TutorialPropFactory.iconPath("HEALTH_POTION"));
  }

  @Test
  void everyStandItemHasAnIconThatIsLoadedWithTheRoom() {
    List<String> textures = Arrays.asList(TutorialPropFactory.TEXTURES);
    for (String itemId : STAND_ITEMS) {
      assertTrue(textures.contains(TutorialPropFactory.iconPath(itemId)), itemId);
    }
  }

  @Test
  void everyPropTextureExistsOnDisk() {
    for (String texture : TutorialPropFactory.TEXTURES) {
      assertTrue(Gdx.files.internal(texture).exists(), texture);
    }
  }

  @Test
  void aStandForAnItemWithoutAnIconIsRefused() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TutorialPropFactory.createSupplyStand(ItemIds.GOLD_COIN));
  }
}
