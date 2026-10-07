package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the book without requiring a player, weapon assets, or hotbar. */
@ExtendWith(GameExtension.class)
class InventoryDisplayTest {
  private Stage stage;
  private Entity ui;
  private InventoryDisplay display;
  private InventoryComponent inventory;
  private Entity player;
  private com.csse3200.game.services.ResourceService resources;

  @BeforeEach
  void setUp() {
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    RenderService renderer = new RenderService();
    renderer.setStage(stage);
    ServiceLocator.registerRenderService(renderer);
    ServiceLocator.registerEntityService(new EntityService());
    resources = new com.csse3200.game.services.ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(
        new String[] {
          "images/health_potion_pixel.png",
          "images/health_potion_small_pixel.png",
          "images/health_potion_medium_pixel.png",
          "images/health_potion_large_pixel.png",
          "images/strength_charm_pixel.png",
          "images/shield_consumable_pixel.png"
        });
    resources.loadAll();
    inventory = new InventoryComponent(0);
    player =
        new Entity()
            .addComponent(inventory)
            .addComponent(new com.csse3200.game.components.CombatStatsComponent(100, 10));
    player.create();
    display = new InventoryDisplay(inventory);
    ui = new Entity().addComponent(display).addComponent(new InventoryActions(display));
    ServiceLocator.getEntityService().register(ui);
  }

  @AfterEach
  void tearDown() {
    if (ui != null) {
      ui.dispose();
    }
    player.dispose();
    resources.dispose();
    stage.dispose();
  }

  @Test
  void pageChangesPreserveVisibilityWithoutLeakingActors() {
    assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
    assertNull(stage.getRoot().findActor("inventory-hotbar"));
    for (int i = 0; i < 4; i++) {
      display.setVisible(true);
      display.changePage();
      assertTrue(stage.getRoot().findActor("inventory-book").isVisible());
      display.setVisible(false);
      display.changePage();
      assertFalse(stage.getRoot().findActor("inventory-book").isVisible());
      assertEquals(1, stage.getActors().size);
    }
  }

  @Test
  void resizingKeepsBookVerticallyCentred() {
    // The original book layout centres itself without reserving any hotbar space.
    for (int[] size : new int[][] {{906, 706}, {1280, 800}, {1918, 1080}}) {
      stage.getViewport().update(size[0], size[1], true);
      display.setVisible(true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.invalidateHierarchy();
      book.validate();
      Actor cover = book.getChildren().first();
      assertEquals((stage.getHeight() - cover.getHeight()) / 2f, cover.getY(), 1f);
    }
  }

  @Test
  void disposingBookRemovesItsActor() {
    ui.dispose();
    ui = null;
    assertEquals(0, stage.getActors().size);
  }

  @Test
  void consumablesPageHasFourEquipmentSlotsAndReopeningPreservesPage() {
    display.setVisible(true);
    assertNotNull(stage.getRoot().findActor("charms-equipped"));
    display.changePage();
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
    for (int i = 0; i < 4; i++) {
      assertNotNull(stage.getRoot().findActor("consumable-equipped-" + i));
    }
    assertNull(stage.getRoot().findActor("consumable-equipped-4"));
    display.setVisible(false);
    display.setVisible(true);
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
    assertNull(stage.getRoot().findActor("charms-equipped"));
  }

  private void clickItem(String slotName) {
    com.badlogic.gdx.scenes.scene2d.ui.Stack slot = stage.getRoot().findActor(slotName);
    slot.getChildren()
        .get(1)
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
  }

  @Test
  void hoverShowsFullSizeTextImmediatelyWithoutChangingOtherTooltipSettings() {
    var shared = com.badlogic.gdx.scenes.scene2d.ui.TooltipManager.getInstance();
    boolean oldAnimations = shared.animations;
    float oldInitialTime = shared.initialTime;
    float oldSubsequentTime = shared.subsequentTime;
    float oldResetTime = shared.resetTime;
    try {
      shared.animations = true;
      shared.initialTime = 0.75f;
      shared.subsequentTime = 0.55f;
      shared.resetTime = 0.65f;
      ui.dispose();
      display = new InventoryDisplay(inventory);
      ui = new Entity().addComponent(display).addComponent(new InventoryActions(display));
      ui.create();
      var charm = new com.csse3200.game.items.charms.StrengthCharm();
      charm.pickUp(player);
      display.setVisible(true);
      stage.getViewport().update(1280, 800, true);
      ((Table) stage.getRoot().findActor("inventory-book")).validate();
      var slot =
          (com.badlogic.gdx.scenes.scene2d.ui.Stack) stage.getRoot().findActor("charm-equipped-0");
      Actor item = slot.getChildren().get(1);
      com.badlogic.gdx.scenes.scene2d.ui.Tooltip<?> tooltip = null;
      for (var listener : item.getListeners()) {
        if (listener instanceof com.badlogic.gdx.scenes.scene2d.ui.Tooltip<?>) {
          tooltip = (com.badlogic.gdx.scenes.scene2d.ui.Tooltip<?>) listener;
          break;
        }
      }
      assertNotNull(tooltip);
      var background =
          assertInstanceOf(
              com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable.class,
              tooltip.getContainer().getBackground());
      var backgroundColor = background.getSprite().getColor();
      assertEquals(0f, backgroundColor.r);
      assertEquals(0f, backgroundColor.g);
      assertEquals(0f, backgroundColor.b);
      assertEquals(0.82f, backgroundColor.a);
      var content = (Table) tooltip.getActor();
      assertEquals(3, content.getChildren().size);
      for (Actor actor : content.getChildren()) {
        var label = (com.badlogic.gdx.scenes.scene2d.ui.Label) actor;
        assertFalse(label.getText().toString().isBlank());
        assertEquals(com.badlogic.gdx.graphics.Color.WHITE, label.getStyle().fontColor);
      }
      assertEquals(
          charm.getDescription(),
          ((com.badlogic.gdx.scenes.scene2d.ui.Label) content.getChildren().peek())
              .getText()
              .toString());
      assertNotSame(shared, tooltip.getManager());
      var event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
      event.setListenerActor(item);
      tooltip.enter(event, item.getWidth() / 2f, item.getHeight() / 2f, -1, null);
      assertSame(stage, tooltip.getContainer().getStage());
      assertEquals(1f, tooltip.getContainer().getScaleX());
      assertEquals(1f, tooltip.getContainer().getScaleY());
      assertEquals(1f, tooltip.getContainer().getColor().a);
      assertFalse(tooltip.getContainer().hasActions());
      assertTrue(shared.animations);
      assertEquals(0.75f, shared.initialTime);
      assertEquals(0.55f, shared.subsequentTime);
      assertEquals(0.65f, shared.resetTime);
      display.setVisible(false);
      assertNull(tooltip.getContainer().getStage());
    } finally {
      shared.animations = oldAnimations;
      shared.initialTime = oldInitialTime;
      shared.subsequentTime = oldSubsequentTime;
      shared.resetTime = oldResetTime;
    }
  }

  @Test
  void clickingCharmMovesItBetweenPagesAndUpdatesPlayerEffect() {
    var charm = new com.csse3200.game.items.charms.StrengthCharm();
    charm.pickUp(player);
    display.setVisible(true);
    clickItem("charm-equipped-0");
    assertFalse(charm.isEquipped());
    assertEquals(
        10,
        player
            .getComponent(com.csse3200.game.components.CombatStatsComponent.class)
            .getBaseAttack());
    clickItem("charm-stored-0");
    assertTrue(charm.isEquipped());
    assertEquals(
        20,
        player
            .getComponent(com.csse3200.game.components.CombatStatsComponent.class)
            .getBaseAttack());
  }

  @Test
  void clickingConsumableRetainsQuantityAndCanReequipFromBackpack() {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);
    display.changePage();
    clickItem("consumable-equipped-0");
    assertNull(inventory.getConsumableSlot(0));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
    clickItem("consumable-stored-0");
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(0));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
  }

  @Test
  void manyCharmsScrollInsideTheExistingBookPage() {
    for (int i = 0; i < 20; i++) new com.csse3200.game.items.charms.StrengthCharm().pickUp(player);
    stage.getViewport().update(1280, 800, true);
    display.setVisible(true);
    Table book = stage.getRoot().findActor("inventory-book");
    book.validate();
    Table grid = stage.getRoot().findActor("charms-equipped");
    var scroll = (com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) grid.getParent();
    assertEquals(300f, scroll.getHeight());
    assertTrue(scroll.getMaxY() > 0f);
    assertEquals(20, display.getPageItems(true).size());
    scroll.setScrollPercentY(1f);
    scroll.updateVisualScroll();
    assertEquals(scroll.getMaxY(), scroll.getVisualScrollY(), 0.01f);
    assertNotNull(stage.getRoot().findActor("charm-equipped-19"));
  }

  private void dragItem(String fromSlot, String toSlot) throws InterruptedException {
    var graphics = com.badlogic.gdx.Gdx.graphics;
    var sized = org.mockito.Mockito.mock(com.badlogic.gdx.Graphics.class);
    org.mockito.Mockito.when(sized.getWidth()).thenReturn(1280);
    org.mockito.Mockito.when(sized.getHeight()).thenReturn(800);
    com.badlogic.gdx.Gdx.graphics = sized;
    try {
      stage.getViewport().update(1280, 800, true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.validate();
      Actor source = stage.getRoot().findActor(fromSlot);
      Actor target = toSlot == null ? null : stage.getRoot().findActor(toSlot);
      var start =
          stage.stageToScreenCoordinates(
              source.localToStageCoordinates(
                  new com.badlogic.gdx.math.Vector2(
                      source.getWidth() / 2f, source.getHeight() / 2f)));
      var end =
          stage.stageToScreenCoordinates(
              target == null
                  ? new com.badlogic.gdx.math.Vector2(5f, 5f)
                  : target.localToStageCoordinates(
                      new com.badlogic.gdx.math.Vector2(
                          target.getWidth() / 2f, target.getHeight() / 2f)));
      stage.touchDown(
          Math.round(start.x), Math.round(start.y), 0, com.badlogic.gdx.Input.Buttons.LEFT);
      stage.touchDragged(Math.round(end.x), Math.round(end.y), 0);
      // DragAndDrop deliberately waits 250ms before accepting a drop.
      Thread.sleep(300);
      stage.touchUp(Math.round(end.x), Math.round(end.y), 0, com.badlogic.gdx.Input.Buttons.LEFT);
    } finally {
      com.badlogic.gdx.Gdx.graphics = graphics;
    }
  }

  @Test
  void realDragMovesConsumableIntoFourthSlotThroughInventoryActions() throws InterruptedException {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);
    display.changePage();
    dragItem("consumable-equipped-0", "consumable-equipped-3");
    assertNull(inventory.getConsumableSlot(0));
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(3));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
  }

  @Test
  void realDragOntoOccupiedSlotSwapsAssignmentsWithoutLosingStock() throws InterruptedException {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    inventory.addConsumable(com.csse3200.game.items.ItemIds.SHIELD, 2);
    display.setVisible(true);
    display.changePage();
    dragItem("consumable-equipped-0", "consumable-equipped-1");
    assertEquals(com.csse3200.game.items.ItemIds.SHIELD, inventory.getConsumableSlot(0));
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(1));
    assertEquals(2, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.SHIELD));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
  }

  @Test
  void realCharmDragUnequipsAndReequipsThroughExistingActionEvents() throws InterruptedException {
    var charm = new com.csse3200.game.items.charms.StrengthCharm();
    charm.pickUp(player);
    display.setVisible(true);
    dragItem("charm-equipped-0", "charm-stored-0");
    assertFalse(charm.isEquipped());
    assertTrue(inventory.hasCharm(charm));
    assertEquals(
        10,
        player
            .getComponent(com.csse3200.game.components.CombatStatsComponent.class)
            .getBaseAttack());
    dragItem("charm-stored-0", "charm-equipped-0");
    assertTrue(charm.isEquipped());
    assertEquals(
        20,
        player
            .getComponent(com.csse3200.game.components.CombatStatsComponent.class)
            .getBaseAttack());
  }

  @Test
  void rejectedDropLeavesOriginalSlotAndQuantityIntact() throws InterruptedException {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);
    display.changePage();
    dragItem("consumable-equipped-0", null);
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(0));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
    var slot =
        (com.badlogic.gdx.scenes.scene2d.ui.Stack)
            stage.getRoot().findActor("consumable-equipped-0");
    assertEquals(3, slot.getChildren().size);
  }

  @Test
  void originalNextPageEventStillChangesTheBook() {
    display.setVisible(true);
    assertNotNull(stage.getRoot().findActor("charms-equipped"));
    ui.getEvents().trigger("nextPage");
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
  }
}
