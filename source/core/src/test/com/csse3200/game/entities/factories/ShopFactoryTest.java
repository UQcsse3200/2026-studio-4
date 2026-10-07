package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.shop.ShopPurchaseComponent;
import com.csse3200.game.components.shop.ShopPurchaseResult;
import com.csse3200.game.components.shop.ShopSessionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopFactoryTest {
  private Stage stage;
  private ResourceService resources;
  private InventoryComponent inventory;
  private Entity player;
  private Entity shop;
  private Terminal terminal;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    stage.getViewport().update(1280, 800, true);
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerInputService(new InputService());
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    inventory = new InventoryComponent(25);
    player = new Entity().addComponent(inventory);
    terminal = new Terminal();
    shop = ShopFactory.createShop(player, terminal);
    ServiceLocator.getEntityService().register(shop);
  }

  @AfterEach
  void tearDown() {
    if (shop != null) shop.dispose();
    stage.dispose();
    resources.dispose();
  }

  private void open() {
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
  }

  private TextButton buy(String id) {
    return stage.getRoot().findActor("shop-buy-" + id);
  }

  @Test
  void purchaseUpdatesBalanceOwnedCountAndAffordability() {
    assertTrue(shop.updatesWhilePaused());
    open();
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    Label gold = stage.getRoot().findActor("shop-gold");
    Label owned = stage.getRoot().findActor("shop-owned-" + ItemIds.HEALTH_POTION);
    assertEquals(15, inventory.getGold());
    assertEquals("Gold: 15", gold.getText().toString());
    assertEquals("Owned: 1", owned.getText().toString());
    assertTrue(buy(ItemIds.STRENGTH_POTION).isDisabled());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(5, inventory.getGold());
    assertTrue(buy(ItemIds.HEALTH_POTION).isDisabled());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(5, inventory.getGold());
    assertEquals(2, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void purchaseRequestSettlesOnceAtCataloguePriceAndRefreshesDisplay() {
    open();
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, 1);
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    Label gold = stage.getRoot().findActor("shop-gold");
    Label owned = stage.getRoot().findActor("shop-owned-" + ItemIds.HEALTH_POTION);
    Label feedback = stage.getRoot().findActor("shop-feedback");
    assertEquals("Gold: 15", gold.getText().toString());
    assertEquals("Owned: 1", owned.getText().toString());
    assertTrue(feedback.getText().toString().contains("Purchased"));
  }

  @Test
  void failedPurchaseRequestReportsFailureWithoutChangingInventory() {
    open();
    inventory.addGold(-20);
    java.util.List<ShopPurchaseResult> results = new java.util.ArrayList<>();
    shop.getEvents()
        .addListener(
            "shopPurchaseResult", (String id, ShopPurchaseResult result) -> results.add(result));
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, 1);
    shop.getEvents().trigger("shopPurchaseRequested", "UNKNOWN", 1);
    assertEquals(
        java.util.List.of(ShopPurchaseResult.INSUFFICIENT_GOLD, ShopPurchaseResult.INVALID_OFFER),
        results);
    assertEquals(5, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    Label feedback = stage.getRoot().findActor("shop-feedback");
    assertTrue(feedback.getText().toString().contains("Not enough gold"));
  }

  @Test
  void closedOrDisposedShopIgnoresPurchaseRequests() {
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, 10);
    assertEquals(25, inventory.getGold());
    open();
    shop.getComponent(ShopSessionComponent.class).close();
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, 10);
    assertEquals(25, inventory.getGold());
    shop.dispose();
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, 10);
    assertEquals(25, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    shop = null;
  }

  @Test
  void debugCommandUsesTheSameModalAndLeavesTerminalClosedAfterEnter() {
    KeyboardTerminalInputComponent input = new KeyboardTerminalInputComponent(terminal);
    terminal.setOpen();
    terminal.setEnteredMessage("shop open");
    input.keyTyped('\r');
    assertFalse(terminal.isOpen());
    assertTrue(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertTrue(ServiceLocator.getEntityService().isFrozen());
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
    ServiceLocator.getInputService().keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
    assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    open();
    assertEquals(15, inventory.getGold());
    assertEquals(1, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void debugCloseAndInvalidCommandsDoNotBuyOrLeaveLocksHeld() {
    for (String command : new String[] {"shop", "shop other", "shop open extra"}) {
      terminal.setEnteredMessage(command);
      assertFalse(terminal.processMessage());
      assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    }
    terminal.setEnteredMessage("shop open");
    assertTrue(terminal.processMessage());
    terminal.setEnteredMessage("shop close");
    assertTrue(terminal.processMessage());
    assertFalse(shop.getComponent(ShopSessionComponent.class).isOpen());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    assertEquals(25, inventory.getGold());
  }

  @Test
  void malformedPurchaseRequestCannotChargeGold() {
    open();
    java.util.List<ShopPurchaseResult> results = new java.util.ArrayList<>();
    shop.getEvents()
        .addListener(
            "shopPurchaseResult", (String id, ShopPurchaseResult result) -> results.add(result));
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, -10);
    shop.getEvents().trigger("shopPurchaseRequested", ItemIds.HEALTH_POTION, (Integer) null);
    assertEquals(
        java.util.List.of(ShopPurchaseResult.INVALID_OFFER, ShopPurchaseResult.INVALID_OFFER),
        results);
    assertEquals(25, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void displayRequestsPurchaseWithoutMutatingInventoryOnItsOwn() {
    open();
    shop.getComponent(ShopPurchaseComponent.class).dispose();
    java.util.List<String> requests = new java.util.ArrayList<>();
    shop.getEvents()
        .addListener("shopPurchaseRequested", (String id, Integer price) -> requests.add(id));
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    assertEquals(java.util.List.of(ItemIds.HEALTH_POTION), requests);
    assertEquals(25, inventory.getGold());
    assertEquals(0, inventory.getConsumableCount(ItemIds.HEALTH_POTION));
  }

  @Test
  void closeAndReopenPreserveInventoryWithoutActorLeaks() {
    open();
    buy(ItemIds.HEALTH_POTION).fire(new ChangeEvent());
    TextButton close = stage.getRoot().findActor("shop-close");
    close.fire(new ChangeEvent());
    Table root = stage.getRoot().findActor("shop-root");
    assertFalse(root.isVisible());
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    open();
    assertTrue(root.isVisible());
    assertEquals(1, stage.getActors().size);
    assertEquals(15, inventory.getGold());
  }

  @Test
  void resizedPanelFitsWindowAndDisposalRemovesActorsAndLocks() {
    open();
    Table root = stage.getRoot().findActor("shop-root");
    Table panel = stage.getRoot().findActor("shop-panel");
    for (int[] size : new int[][] {{1280, 800}, {906, 706}, {640, 480}}) {
      stage.getViewport().update(size[0], size[1], true);
      root.invalidateHierarchy();
      root.validate();
      assertTrue(panel.getWidth() <= stage.getWidth());
      assertTrue(panel.getHeight() <= stage.getHeight());
      Actor close = stage.getRoot().findActor("shop-close");
      Vector2 corner = close.localToStageCoordinates(new Vector2());
      assertTrue(
          corner.y >= 0 && corner.y + close.getHeight() <= stage.getHeight(),
          "Close button must remain inside the resized window");
    }
    shop.dispose();
    shop = null;
    assertEquals(0, stage.getActors().size);
    assertFalse(ServiceLocator.getEntityService().isFrozen());
    player.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, "merchant", new Entity());
    assertEquals(0, stage.getActors().size);
  }

  @Test
  void modalOwnsKeyboardAndScrollFocusThenRestoresPreviousFocus() {
    Actor background = new Actor();
    int[] typed = {0};
    background.addListener(
        new InputListener() {
          @Override
          public boolean keyTyped(InputEvent event, char character) {
            typed[0]++;
            return true;
          }
        });
    stage.addActor(background);
    stage.setKeyboardFocus(background);
    stage.setScrollFocus(background);
    open();
    ServiceLocator.getInputService().keyTyped('x');
    assertEquals(0, typed[0]);
    assertNotSame(background, stage.getScrollFocus());
    shop.getComponent(ShopSessionComponent.class).close();
    assertSame(background, stage.getKeyboardFocus());
    assertSame(background, stage.getScrollFocus());
  }

  @Test
  void closeRestoresFocusAfterScrollingMerchantColumn() {
    Actor background = new Actor();
    stage.addActor(background);
    stage.setScrollFocus(background);
    open();
    Actor merchantScroll = stage.getRoot().findActor("shop-merchant-scroll");
    assertNotNull(merchantScroll);
    stage.setScrollFocus(merchantScroll);
    shop.getComponent(ShopSessionComponent.class).close();
    assertSame(background, stage.getScrollFocus());
  }
}
