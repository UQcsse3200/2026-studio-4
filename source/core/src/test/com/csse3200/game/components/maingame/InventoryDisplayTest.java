package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
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
    display = new InventoryDisplay(inventory, InventoryDisplayTest::immediateDragAndDrop);
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

  @Test
  void keyboardConfirmsThroughExistingActionsAndDetailsFollowSelection() {
    var charm = new com.csse3200.game.items.charms.StrengthCharm();
    charm.pickUp(player);
    display.setVisible(true);
    assertTrue(stage.keyDown(com.badlogic.gdx.Input.Keys.SPACE));
    assertFalse(charm.isEquipped());
    for (int i = 0; i < 3; i++) stage.keyDown(com.badlogic.gdx.Input.Keys.D);
    var details =
        (com.badlogic.gdx.scenes.scene2d.ui.Label)
            stage.getRoot().findActor("inventory-selection-details");
    assertTrue(details.getText().toString().contains(charm.getName()));
    stage.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
    assertTrue(charm.isEquipped());
  }

  @Test
  void keyboardNavigationReachesFourthConsumableSlotAndStopsAtEdges() {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    inventory.equipConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);
    stage.keyDown(com.badlogic.gdx.Input.Keys.E);
    stage.keyDown(com.badlogic.gdx.Input.Keys.W);
    stage.keyDown(com.badlogic.gdx.Input.Keys.A);
    stage.keyDown(com.badlogic.gdx.Input.Keys.D);
    stage.keyDown(com.badlogic.gdx.Input.Keys.D);
    stage.keyDown(com.badlogic.gdx.Input.Keys.S);
    stage.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
    assertNull(inventory.getConsumableSlot(3));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
  }

  @Test
  void categoryKeysAreHandledOnlyWhileOpenAndDoNotConsumeStock() {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    assertFalse(stage.keyDown(com.badlogic.gdx.Input.Keys.Q));
    display.setVisible(true);
    assertTrue(stage.keyDown(com.badlogic.gdx.Input.Keys.E));
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
    assertTrue(stage.keyDown(com.badlogic.gdx.Input.Keys.Q));
    assertNotNull(stage.getRoot().findActor("charms-equipped"));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
    assertFalse(stage.keyDown(com.badlogic.gdx.Input.Keys.I));
    assertFalse(stage.keyDown(com.badlogic.gdx.Input.Keys.F1));
  }

  @Test
  void achievementPageRetainsMainProgressAndBothPageDirections() {
    var service = new com.csse3200.game.services.AchievementService();
    var locked =
        new com.csse3200.game.components.achievements.Achievement("Locked test", c -> false);
    locked.setTarget(10);
    locked.setProgress(4);
    var unlocked =
        new com.csse3200.game.components.achievements.Achievement("Unlocked test", c -> true);
    unlocked.update(null);
    service.register(locked);
    service.register(unlocked);
    ServiceLocator.registerAchievementService(service);
    var charm = new com.csse3200.game.items.charms.StrengthCharm();
    charm.pickUp(player);
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);

    stage.keyDown(com.badlogic.gdx.Input.Keys.Q);
    Table left = stage.getRoot().findActor("inventory-page-left");
    Table right = stage.getRoot().findActor("inventory-page-right");
    assertNotNull(left.findActor("achievement-Locked test"));
    assertNotNull(right.findActor("achievement-Unlocked test"));
    var label =
        (com.badlogic.gdx.scenes.scene2d.ui.Label) left.findActor("achievement-Locked test");
    var entries = (Table) label.getParent();
    assertEquals(
        "4 / 10",
        ((com.badlogic.gdx.scenes.scene2d.ui.Label) entries.getChildren().get(1))
            .getText()
            .toString());
    assertTrue(display.getPageItems(true).isEmpty());
    for (int key :
        new int[] {
          com.badlogic.gdx.Input.Keys.W,
          com.badlogic.gdx.Input.Keys.A,
          com.badlogic.gdx.Input.Keys.S,
          com.badlogic.gdx.Input.Keys.D,
          com.badlogic.gdx.Input.Keys.SPACE
        }) {
      assertTrue(stage.keyDown(key));
    }
    assertTrue(charm.isEquipped());
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(0));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
    stage.keyDown(com.badlogic.gdx.Input.Keys.E);
    assertNotNull(stage.getRoot().findActor("charms-equipped"));
    stage.keyDown(com.badlogic.gdx.Input.Keys.E);
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
    stage.keyDown(com.badlogic.gdx.Input.Keys.E);
    assertNotNull(stage.getRoot().findActor("achievement-Locked test"));
    display.refreshPage();
    assertNotNull(stage.getRoot().findActor("achievement-Locked test"));
    stage.keyDown(com.badlogic.gdx.Input.Keys.Q);
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
  }

  @Test
  void categoryTabsJumpDirectlyAcrossAllThreePages() {
    display.setVisible(true);
    stage
        .getRoot()
        .findActor("inventory-tab-achievements")
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertTrue(display.getPageItems(true).isEmpty());
    stage
        .getRoot()
        .findActor("inventory-tab-charms")
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertNotNull(stage.getRoot().findActor("charms-equipped"));
    stage
        .getRoot()
        .findActor("inventory-tab-consumables")
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertNotNull(stage.getRoot().findActor("consumables-equipped"));
  }

  @Test
  void capturedReleasesAreHandledAfterClosingButEarlierMovementReleasesPassThrough() {
    display.setVisible(true);
    assertFalse(stage.keyUp(com.badlogic.gdx.Input.Keys.W));
    assertTrue(stage.keyDown(com.badlogic.gdx.Input.Keys.D));
    display.setVisible(false);
    assertTrue(stage.keyUp(com.badlogic.gdx.Input.Keys.D));
    assertFalse(stage.keyUp(com.badlogic.gdx.Input.Keys.D));
    assertFalse(stage.keyDown(com.badlogic.gdx.Input.Keys.D));
  }

  @Test
  void keyboardSelectionScrollsToCharmsBeyondVisibleRows() {
    for (int i = 0; i < 20; i++) new com.csse3200.game.items.charms.StrengthCharm().pickUp(player);
    stage.getViewport().update(1280, 800, true);
    display.setVisible(true);
    for (int i = 0; i < 6; i++) stage.keyDown(com.badlogic.gdx.Input.Keys.S);
    Table grid = stage.getRoot().findActor("charms-equipped");
    var scroll = (com.badlogic.gdx.scenes.scene2d.ui.ScrollPane) grid.getParent();
    assertTrue(scroll.getVisualScrollY() > 0f);
  }

  @Test
  void openInventoryConsumesKeysBeforeGameplayThroughInputService() {
    var input = new com.csse3200.game.input.InputService();
    var gameplay = mock(com.csse3200.game.input.InputComponent.class);
    when(gameplay.getPriority()).thenReturn(5);
    input.register(gameplay);
    input.register(new com.csse3200.game.input.InputDecorator(stage, 10));
    display.setVisible(true);
    for (int key :
        new int[] {
          com.badlogic.gdx.Input.Keys.Q,
          com.badlogic.gdx.Input.Keys.E,
          com.badlogic.gdx.Input.Keys.W,
          com.badlogic.gdx.Input.Keys.J,
          com.badlogic.gdx.Input.Keys.SPACE
        }) {
      assertTrue(input.keyDown(key));
      verify(gameplay, never()).keyDown(key);
    }
    display.setVisible(false);
    input.keyDown(com.badlogic.gdx.Input.Keys.Q);
    verify(gameplay).keyDown(com.badlogic.gdx.Input.Keys.Q);
    input.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
    verify(gameplay).keyDown(com.badlogic.gdx.Input.Keys.SPACE);
  }

  @Test
  void escapeClosesThroughTheInventoryToggleAndReopeningWorks() {
    inventory.setDisplay(display);
    inventory.toggleDisplay();
    assertTrue(display.isVisible());
    assertTrue(stage.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE));
    assertFalse(display.isVisible());
    inventory.toggleDisplay();
    assertTrue(display.isVisible());
    inventory.toggleDisplay();
  }

  @Test
  void bookRetainsOriginalCoverAndParchmentTextures() {
    for (int[] size : new int[][] {{906, 706}, {1280, 800}, {1918, 1080}}) {
      stage.getViewport().update(size[0], size[1], true);
      display.setVisible(true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.validate();
      var cover = (com.badlogic.gdx.scenes.scene2d.ui.Image) book.findActor("inventory-book-cover");
      assertNotNull(cover, "Inventory must use the original travel book cover");
      var coverDrawable =
          assertInstanceOf(
              com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable.class,
              cover.getDrawable());
      assertEquals(224, coverDrawable.getRegion().getRegionWidth());
      assertEquals(160, coverDrawable.getRegion().getRegionHeight());
      var left = (Table) book.findActor("inventory-page-left");
      var right = (Table) book.findActor("inventory-page-right");
      assertNotNull(left);
      assertNotNull(right);
      var leftDrawable =
          assertInstanceOf(
              com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable.class,
              left.getBackground());
      var rightDrawable =
          assertInstanceOf(
              com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable.class,
              right.getBackground());
      assertEquals(1787, leftDrawable.getRegion().getRegionX());
      assertEquals(1853, leftDrawable.getRegion().getRegionY());
      assertEquals(1029, rightDrawable.getRegion().getRegionX());
      assertEquals(612, rightDrawable.getRegion().getRegionY());
      display.changePage();
    }
  }

  @Test
  void bookPagesAndContentsFitInsideCoverAtEachViewportSize() {
    for (int[] size : new int[][] {{906, 706}, {1280, 800}, {1918, 1080}}) {
      stage.getViewport().update(size[0], size[1], true);
      display.setVisible(true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.validate();
      var cover = (com.badlogic.gdx.scenes.scene2d.ui.Image) book.findActor("inventory-book-cover");
      var left = (Table) book.findActor("inventory-page-left");
      var right = (Table) book.findActor("inventory-page-right");
      var coverPos = cover.localToStageCoordinates(new com.badlogic.gdx.math.Vector2());
      var leftPos = left.localToStageCoordinates(new com.badlogic.gdx.math.Vector2());
      var rightPos = right.localToStageCoordinates(new com.badlogic.gdx.math.Vector2());
      assertTrue(coverPos.x >= 0f && coverPos.y >= 0f);
      assertTrue(coverPos.x + cover.getWidth() <= stage.getWidth() + 1f);
      assertTrue(coverPos.y + cover.getHeight() <= stage.getHeight() + 1f);
      assertTrue(leftPos.x >= coverPos.x);
      assertTrue(leftPos.x + left.getWidth() <= rightPos.x, "Pages must leave the spine visible");
      assertTrue(rightPos.x + right.getWidth() <= coverPos.x + cover.getWidth() + 1f);
      for (Table page : new Table[] {left, right}) {
        var pagePos = page.localToStageCoordinates(new com.badlogic.gdx.math.Vector2());
        assertTrue(pagePos.y >= coverPos.y);
        assertTrue(pagePos.y + page.getHeight() <= coverPos.y + cover.getHeight() + 1f);
        for (Actor content : page.getChildren()) {
          assertTrue(
              content.getX() >= -1f && content.getY() >= -1f,
              content + " position " + content.getX() + "," + content.getY());
          assertTrue(content.getX() + content.getWidth() <= page.getWidth() + 1f);
          assertTrue(content.getY() + content.getHeight() <= page.getHeight() + 1f);
        }
      }
      display.changePage();
    }
  }

  @Test
  void bookCategoryControlsAndParchmentInkRemainReadable() {
    for (int[] size : new int[][] {{906, 706}, {1280, 800}, {1918, 1080}}) {
      stage.getViewport().update(size[0], size[1], true);
      display.setVisible(true);
      Table book = stage.getRoot().findActor("inventory-book");
      book.validate();
      for (String tabName :
          new String[] {
            "inventory-tab-charms", "inventory-tab-consumables", "inventory-tab-achievements"
          }) {
        var tab = (com.badlogic.gdx.scenes.scene2d.ui.TextButton) book.findActor(tabName);
        assertNotNull(tab);
        assertTrue(
            tab.getColor().a > 0.9f && tab.getLabel().getColor().a > 0.9f,
            "Category controls must remain visible");
      }
      var details =
          (com.badlogic.gdx.scenes.scene2d.ui.Label) book.findActor("inventory-selection-details");
      if (details != null) {
        var ink = details.getStyle().fontColor;
        assertTrue(
            details.getColor().r * ink.r < 0.6f
                && details.getColor().g * ink.g < 0.6f
                && details.getColor().b * ink.b < 0.6f,
            "Parchment requires dark readable ink");
      }
      display.changePage();
    }
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
      display = new InventoryDisplay(inventory, InventoryDisplayTest::immediateDragAndDrop);
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

  private static DragAndDrop immediateDragAndDrop() {
    DragAndDrop dragAndDrop = new DragAndDrop();
    // Keep real stage gestures and drop validation without the wall-clock acceptance delay.
    dragAndDrop.setDragTime(0);
    return dragAndDrop;
  }

  private void dragItem(String fromSlot, String toSlot) {
    var graphics = com.badlogic.gdx.Gdx.graphics;
    var sized = mock(com.badlogic.gdx.Graphics.class);
    when(sized.getWidth()).thenReturn(1280);
    when(sized.getHeight()).thenReturn(800);
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
      stage.touchUp(Math.round(end.x), Math.round(end.y), 0, com.badlogic.gdx.Input.Buttons.LEFT);
    } finally {
      com.badlogic.gdx.Gdx.graphics = graphics;
    }
  }

  @Test
  void realDragMovesConsumableIntoFourthSlotThroughInventoryActions() {
    inventory.addConsumable(com.csse3200.game.items.ItemIds.HEALTH_POTION, 3);
    display.setVisible(true);
    display.changePage();
    dragItem("consumable-equipped-0", "consumable-equipped-3");
    assertNull(inventory.getConsumableSlot(0));
    assertEquals(com.csse3200.game.items.ItemIds.HEALTH_POTION, inventory.getConsumableSlot(3));
    assertEquals(3, inventory.getConsumableCount(com.csse3200.game.items.ItemIds.HEALTH_POTION));
  }

  @Test
  void realDragOntoOccupiedSlotSwapsAssignmentsWithoutLosingStock() {
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
  void realCharmDragUnequipsAndReequipsThroughExistingActionEvents() {
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
  void rejectedDropLeavesOriginalSlotAndQuantityIntact() {
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
