package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the shop display's presentation only: it must never mutate Gold or inventory itself, only
 * request a purchase for whatever listens for it (Sprint 3 #196 B2 purchase logic).
 */
@ExtendWith(GameExtension.class)
class ShopDisplayTest {
  private static final List<ShopEntry> CATALOGUE =
      List.of(
          new ShopEntry(ItemIds.HEALTH_POTION, "Health Potion", 10),
          new ShopEntry(ItemIds.SHIELD, "Shield", 15));

  private Stage stage;
  private Entity ui;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    stage.dispose();
  }

  @Test
  void startsHiddenAndOpenCloseToggleVisibility() {
    ShopDisplay display = createDisplay(new InventoryComponent(20));
    assertFalse(display.isOpen());
    display.open();
    assertTrue(display.isOpen());
    display.close();
    assertFalse(display.isOpen());
  }

  @Test
  void rejectsEmptyOrNullConstructorArguments() {
    InventoryComponent inventory = new InventoryComponent(20);
    assertThrows(IllegalArgumentException.class, () -> new ShopDisplay(null, CATALOGUE));
    assertThrows(IllegalArgumentException.class, () -> new ShopDisplay(inventory, List.of()));
  }

  @Test
  void affordablePurchaseRequestsButDoesNotTouchInventoryItself() {
    InventoryComponent inventory = new InventoryComponent(20);
    ShopDisplay display = createDisplay(inventory);
    boolean[] requested = {false};
    ui.getEvents()
        .addListener(
            "shopPurchaseRequested",
            (String itemId, Integer price) -> {
              requested[0] = true;
              assertEquals(ItemIds.HEALTH_POTION, itemId);
              assertEquals(10, price);
            });

    display.open();
    clickBuy(ItemIds.HEALTH_POTION);

    assertTrue(requested[0]);
    // The display never deducts Gold or grants the item itself; that is B2's job.
    assertEquals(20, inventory.getGold());
    assertFalse(inventory.hasConsumable(ItemIds.HEALTH_POTION));
  }

  @Test
  void unaffordablePurchaseDoesNotRequestAPurchase() {
    InventoryComponent inventory = new InventoryComponent(1);
    ShopDisplay display = createDisplay(inventory);
    boolean[] requested = {false};
    ui.getEvents()
        .addListener("shopPurchaseRequested", (String id, Integer price) -> requested[0] = true);

    display.open();
    clickBuy(ItemIds.HEALTH_POTION);

    assertFalse(requested[0]);
    assertEquals(1, inventory.getGold());
  }

  private ShopDisplay createDisplay(InventoryComponent inventory) {
    ShopDisplay display = new ShopDisplay(inventory, CATALOGUE);
    ui = new Entity().addComponent(display);
    ServiceLocator.getEntityService().register(ui);
    return display;
  }

  private void clickBuy(String itemId) {
    int index = -1;
    for (int i = 0; i < CATALOGUE.size(); i++) {
      if (CATALOGUE.get(i).itemId().equals(itemId)) {
        index = i;
        break;
      }
    }
    assertTrue(index >= 0, "entry not found: " + itemId);
    List<TextButton> buttons = new ArrayList<>();
    collectBuyButtons(stage.getRoot(), buttons);
    buttons.get(index).toggle();
  }

  private static void collectBuyButtons(Actor actor, List<TextButton> out) {
    if (actor instanceof TextButton button && "Buy".contentEquals(button.getText())) {
      out.add(button);
    }
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collectBuyButtons(child, out);
      }
    }
  }
}
