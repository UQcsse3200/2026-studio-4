package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.ItemTooltip;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Displays the inventory book and its charms, consumables and achievements pages. */
public class InventoryDisplay extends UIComponent {
  private static final float Z_INDEX = 2f;
  private static final String LEFT_PAGE = "UI_TravelBook_BookPageLeft01a";
  private static final String RIGHT_PAGE = "UI_TravelBook_BookPageRight01a";

  private static final String MOVE_INACTIVE_TO_ACTIVE = "moveInactiveToActiveItem";
  private static final String MOVE_ACTIVE_TO_INACTIVE = "moveActiveToInactiveItem";
  private static final String DARK_TEXT_COLOR = "3a2618ff";
  private static final String LEFT_PAGE_NAME = "inventory-page-left";
  private static final String RIGHT_PAGE_NAME = "inventory-page-right";
  private static final String ACTIVE_SLOT = "active";

  private enum Page {
    CHARMS,
    CONSUMABLES,
    ACHIEVEMENTS
  }

  private Page currentPage = Page.CHARMS;
  private Table table;
  private final List<Stack> equippedSlots = new ArrayList<>();
  private final List<Stack> storedSlots = new ArrayList<>();
  private final Set<Integer> capturedKeys = new HashSet<>();
  private boolean selectedEquipped = true;
  private int selectedIndex;
  private Label selectionDetails;
  private final InputListener keyboardNavigation =
      new InputListener() {
        @Override
        public boolean keyDown(InputEvent event, int keycode) {
          if (!isVisible() || keycode == Keys.I || keycode == Keys.F1) return false;
          capturedKeys.add(keycode);
          switch (keycode) {
            case Keys.Q -> entity.getEvents().trigger("previousPage");
            case Keys.E -> entity.getEvents().trigger("nextPage");
            case Keys.W, Keys.UP -> moveSelection(0, -1);
            case Keys.S, Keys.DOWN -> moveSelection(0, 1);
            case Keys.A, Keys.LEFT -> moveSelection(-1, 0);
            case Keys.D, Keys.RIGHT -> moveSelection(1, 0);
            case Keys.SPACE -> toggleSelectedEquipment();
            case Keys.ESCAPE -> inventoryComponent.toggleDisplay();
            default -> {
              // Capture unrelated keys while the book is open to prevent player actions.
            }
          }
          return true;
        }

        private void toggleSelectedEquipment() {
          if (currentPage == Page.ACHIEVEMENTS) return;
          entity
              .getEvents()
              .trigger(
                  selectedEquipped ? MOVE_ACTIVE_TO_INACTIVE : MOVE_INACTIVE_TO_ACTIVE,
                  selectedIndex,
                  -1);
        }

        private void moveSelection(int dx, int dy) {
          if (selectedSlots().isEmpty()) return;
          tooltipManager.hideAll();
          int columns = selectedEquipped ? 3 : 4;
          if (crossesPageEdge(dx, columns)) {
            int row = selectedIndex / columns;
            selectedEquipped = !selectedEquipped;
            int newColumns = selectedEquipped ? 3 : 4;
            selectedIndex =
                Math.min(
                    row * newColumns + (selectedEquipped ? newColumns - 1 : 0),
                    selectedSlots().size() - 1);
          } else {
            moveWithinGrid(dx, dy, columns);
          }
          updateSelection();
          table.validate();
          Stack selected = selectedSlots().get(selectedIndex);
          if (selected.getParent().getParent() instanceof ScrollPane scroll) {
            scroll.scrollTo(
                selected.getX(), selected.getY(), selected.getWidth(), selected.getHeight());
            scroll.updateVisualScroll();
          }
        }

        private boolean crossesPageEdge(int dx, int columns) {
          int column = selectedIndex % columns;
          if (selectedEquipped) {
            return dx > 0 && (column == columns - 1 || selectedIndex == selectedSlots().size() - 1);
          }
          return dx < 0 && column == 0;
        }

        private void moveWithinGrid(int dx, int dy, int columns) {
          int candidate = selectedIndex + dx + dy * columns;
          if (dy > 0 && (selectedIndex / columns + 1) * columns < selectedSlots().size()) {
            candidate = Math.min(candidate, selectedSlots().size() - 1);
          }
          if (candidate >= 0
              && candidate < selectedSlots().size()
              && (dx == 0 || candidate / columns == selectedIndex / columns)) {
            selectedIndex = candidate;
          }
        }

        @Override
        public boolean keyUp(InputEvent event, int keycode) {
          // Pre-existing movement releases must still reach the player input handler.
          return capturedKeys.remove(keycode);
        }
      };
  private final Supplier<DragAndDrop> dragAndDropFactory;
  private DragAndDrop dragAndDrop;
  private InventoryComponent inventoryComponent;
  private final TooltipManager tooltipManager =
      new TooltipManager() {
        @Override
        protected void showAction(Tooltip tooltip) {
          tooltip.getContainer().clearActions();
          tooltip.getContainer().setScale(1f);
          tooltip.getContainer().getColor().a = 1f;
        }

        @Override
        protected void hideAction(Tooltip tooltip) {
          tooltip.getContainer().clearActions();
          tooltip.getContainer().remove();
        }
      };

  public InventoryDisplay(InventoryComponent inventoryComponent) {
    this(inventoryComponent, DragAndDrop::new);
  }

  InventoryDisplay(
      InventoryComponent inventoryComponent, Supplier<DragAndDrop> dragAndDropFactory) {
    this.inventoryComponent = inventoryComponent;
    this.dragAndDropFactory = dragAndDropFactory;
  }

  @Override
  public void create() {
    super.create();
    buildPage();
    table.setVisible(false);
    stage.addCaptureListener(keyboardNavigation);
    // Keep immediate, static inventory hints separate from other UI's tooltip settings.
    tooltipManager.initialTime = 0f;
    tooltipManager.resetTime = 0f;
    tooltipManager.subsequentTime = 0f;
    tooltipManager.animations = false;
    tooltipManager.hideAll(); // Reset the manager's internal delay after changing its timing.
  }

  private Label createDarkLabel(String text) {
    Label label = new Label(text, skin);
    label.setColor(Color.valueOf(DARK_TEXT_COLOR));
    return label;
  }

  /** Builds the inventory page depending on which inventory is being displayed */
  private void buildPage() {
    // Create main table
    table = new Table();
    table.setName("inventory-book");
    this.dragAndDrop = dragAndDropFactory.get();
    table.setFillParent(true);
    stage.addActor(table);
    equippedSlots.clear();
    storedSlots.clear();

    selectionDetails = new Label("", skin);
    selectionDetails.setName("inventory-selection-details");
    selectionDetails.setWrap(true);
    selectionDetails.setColor(Color.valueOf(DARK_TEXT_COLOR));

    Stack bookStack = new Stack();

    Image bookCover = new Image(inventory.getDrawable("UI_TravelBook_BookCover01a"));
    bookCover.setName("inventory-book-cover");
    bookCover.setScaling(Scaling.fill);
    bookStack.add(bookCover);

    Table pagesContainer =
        switch (currentPage) {
          case CHARMS -> charmsCreate();
          case CONSUMABLES -> consumableCreate();
          case ACHIEVEMENTS -> achievementsCreate();
        };
    bookStack.add(pagesContainer);

    table.add(bookStack).size(800, 560).center();

    updateSelection();

    table.setVisible(true);
  }

  /** Advances to the next book category, including main's achievements page. */
  public void changePage() {
    showPage(Page.values()[(currentPage.ordinal() + 1) % Page.values().length]);
  }

  /** Returns to the previous book category. */
  public void previousPage() {
    showPage(
        Page.values()[(currentPage.ordinal() + Page.values().length - 1) % Page.values().length]);
  }

  private void showPage(Page page) {
    if (currentPage == page) return;
    currentPage = page;
    selectedIndex = 0;
    refreshPage();
  }

  /** Refreshes the current page after an inventory action without changing pages. */
  public void refreshPage() {
    tooltipManager.hideAll();
    boolean visible = table.isVisible();
    table.remove();
    buildPage();
    table.setVisible(visible);
  }

  /** Item snapshot used by the grids and the existing action component. */
  public List<Item> getPageItems(boolean equipped) {
    if (currentPage == Page.ACHIEVEMENTS) return List.of();
    if (currentPage == Page.CHARMS) {
      return inventoryComponent.getCharms().stream()
          .filter(charm -> charm.isEquipped() == equipped)
          .<Item>map(charm -> charm)
          .toList();
    }
    List<Item> items = new ArrayList<>();
    if (equipped) {
      for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
        String id = inventoryComponent.getConsumableSlot(i);
        items.add(id == null ? null : ItemCatalog.create(id, 1));
      }
    } else {
      List<Item> assignedItems = getPageItems(true);
      for (String id : inventoryComponent.getConsumableIds()) {
        boolean assigned =
            assignedItems.stream().anyMatch(item -> item != null && id.equals(item.getId()));
        if (!assigned) items.add(ItemCatalog.create(id, 1));
      }
    }
    return items;
  }

  public InventoryComponent getInventoryComponent() {
    return inventoryComponent;
  }

  private ScrollPane scrollGrid(Table grid) {
    ScrollPane scroll = new ScrollPane(grid, skin);
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);
    return scroll;
  }

  /**
   * Creates the consumable inventory UI
   *
   * @return the Consumable inventory UI
   */
  private Table consumableCreate() {
    Table pagesContainer = new Table();
    pagesContainer.pad(30, 40, 30, 40);

    Table leftPage = new Table().background(inventory.getDrawable(LEFT_PAGE)).top();
    leftPage.setName(LEFT_PAGE_NAME);
    leftPage.pad(20);

    leftPage.add(pageTabs()).padBottom(10).growX().row();

    leftPage.add(createDarkLabel("Equipped")).row();

    Table leftGrid =
        drawItemGrid(
            3, InventoryComponent.CONSUMABLE_SLOT_COUNT, 72, getPageItems(true), true, true);
    leftGrid.setName("consumables-equipped");
    leftPage.add(leftGrid).padTop(10).padBottom(20).row();

    Label hints =
        createDarkLabel(
            "WASD / Arrows : Move\nSpace : Equip / Unequip\nQ / E : Category\nI / Esc : Close");
    hints.setFontScale(0.8f);
    hints.setWrap(true);
    leftPage.add(hints).width(310).padTop(10).center();

    pagesContainer.add(leftPage).size(350, 500).padRight(20);

    Table rightPage = new Table().background(inventory.getDrawable(RIGHT_PAGE)).top();
    rightPage.setName(RIGHT_PAGE_NAME);
    rightPage.pad(20);

    rightPage.add(createDarkLabel("Backpack")).padBottom(10).row();

    List<Item> stored = getPageItems(false);
    Table rightGrid = drawItemGrid(4, Math.max(20, stored.size()), 64, stored, true, false);

    rightPage.add(scrollGrid(rightGrid)).size(310, 250).center().padBottom(10).row();

    Table detailsTable = new Table();
    detailsTable.add(selectionDetails).width(270);
    rightPage.add(scrollGrid(detailsTable)).growX().height(100);

    pagesContainer.add(rightPage).size(350, 500);

    return pagesContainer;
  }

  /**
   * Creates the Charms inventory UI
   *
   * @return the Charms inventory UI
   */
  private Table charmsCreate() {
    Table pagesContainer = new Table();
    pagesContainer.pad(30, 40, 30, 40);

    Table leftPage = new Table().background(inventory.getDrawable(LEFT_PAGE)).top();
    leftPage.setName(LEFT_PAGE_NAME);
    leftPage.pad(20);

    leftPage.add(pageTabs()).padBottom(10).growX().row();

    leftPage.add(createDarkLabel("Equipped")).row();

    List<Item> equipped = getPageItems(true);
    Table leftGrid = drawItemGrid(3, Math.max(5, equipped.size()), 72, equipped, true, true);
    leftGrid.setName("charms-equipped");
    leftPage.add(scrollGrid(leftGrid)).size(250, 300).row();

    Label hints = createDarkLabel("WASD/Arrows:Move Space:Equip/Unequip\nQ/E:Category I/Esc:Close");
    hints.setFontScale(0.8f);
    hints.setWrap(true);
    leftPage.add(hints).width(310).padTop(10).center();

    pagesContainer.add(leftPage).size(350, 500).padRight(20);

    Table rightPage = new Table().background(inventory.getDrawable(RIGHT_PAGE)).top();
    rightPage.setName(RIGHT_PAGE_NAME);
    rightPage.pad(20);

    rightPage.add(createDarkLabel("Backpack")).padBottom(10).row();

    List<Item> stored = getPageItems(false);
    Table rightGrid = drawItemGrid(4, Math.max(20, stored.size()), 64, stored, true, false);

    rightPage.add(scrollGrid(rightGrid)).size(310, 250).center().padBottom(10).row();

    Table detailsTable = new Table();
    detailsTable.add(selectionDetails).width(270);
    rightPage.add(scrollGrid(detailsTable)).growX().height(100);

    pagesContainer.add(rightPage).size(350, 500);

    return pagesContainer;
  }

  private Table achievementsCreate() {
    Table pagesContainer = new Table();
    pagesContainer.pad(30, 40, 30, 40);

    List<Achievement> all = new ArrayList<>();
    AchievementService achievementService = ServiceLocator.getAchievementService();
    if (achievementService != null) {
      all.addAll(achievementService.getAchievements());
    }

    List<Achievement> locked = all.stream().filter(a -> !a.isUnlocked()).toList();
    List<Achievement> unlocked = all.stream().filter(Achievement::isUnlocked).toList();

    Table leftPage = new Table().background(inventory.getDrawable(LEFT_PAGE)).top();
    leftPage.setName(LEFT_PAGE_NAME);
    leftPage.pad(20);
    leftPage.add(pageTabs()).padBottom(10).growX().row();
    leftPage.add(createDarkLabel("Locked achievements")).top().pad(15f).row();
    leftPage.add(achievementList(locked, false)).grow();
    leftPage.row();
    Label hints = createDarkLabel("Q / E : Category    I / Esc : Close");
    hints.setFontScale(0.8f);
    leftPage.add(hints).padTop(10);
    pagesContainer.add(leftPage).size(350, 500).padRight(20);

    Table rightPage = new Table().background(inventory.getDrawable(RIGHT_PAGE)).top();
    rightPage.setName(RIGHT_PAGE_NAME);
    rightPage.pad(20);
    rightPage.add(createDarkLabel("Unlocked achievements")).top().pad(15f).row();
    rightPage.add(achievementList(unlocked, true)).grow();
    pagesContainer.add(rightPage).size(350, 500);

    return pagesContainer;
  }

  /** Builds a scrollable column of achievement names, styled for locked or unlocked. */
  private ScrollPane achievementList(List<Achievement> achievements, boolean unlockedStyle) {
    Table list = new Table();
    list.top();
    for (Achievement a : achievements) {
      Label name = createDarkLabel("> " + a.getName());

      name.setName("achievement-" + a.getName());
      name.setWrap(true);
      list.add(name).left().width(280f).pad(6).row();

      if (!unlockedStyle && a.getTarget() > 0) {
        int shown = (int) Math.min(a.getProgress(), a.getTarget());
        Label progress = new Label(shown + " / " + (int) a.getTarget(), skin, "achievements_lock");
        progress.setColor(Color.valueOf(DARK_TEXT_COLOR));
        list.add(progress).left().padLeft(6).padBottom(20).row();
      }
    }
    ScrollPane scrollPane = new ScrollPane(list, skin);
    scrollPane.setScrollingDisabled(true, false);
    scrollPane.setFadeScrollBars(false);
    return scrollPane;
  }

  /**
   * draws an item grid from a list of items
   *
   * @param enableDrag set this to true to add drag functionality
   * @param equipped whether the grid represents active equipment
   */
  private Table drawItemGrid(
      int columns,
      int totalSlots,
      int slotSize,
      List<Item> items,
      boolean enableDrag,
      boolean equipped) {
    Table grid = new Table();

    for (int i = 0; i < totalSlots; i++) {
      Item item = i < items.size() ? items.get(i) : null;
      Stack slotStack = createItemSlot(i, item, enableDrag, equipped);
      grid.add(slotStack).size(slotSize).pad(3);
      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        grid.row();
      }
    }

    return grid;
  }

  private Stack createItemSlot(int index, Item item, boolean enableDrag, boolean equipped) {
    Stack slotStack = new Stack();
    ImageButton slotBackground = new ImageButton(inventory, "inventory-box");
    slotBackground.setUserObject((equipped ? "active:" : "inactive:") + index);
    slotStack.setName(
        (currentPage == Page.CHARMS ? "charm" : "consumable")
            + (equipped ? "-equipped-" : "-stored-")
            + index);
    slotStack.add(slotBackground);
    (equipped ? equippedSlots : storedSlots).add(slotStack);
    slotStack.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer != -1) return;
            selectedEquipped = equipped;
            selectedIndex = index;
            updateSelection();
          }
        });
    if (enableDrag) registerDropTarget(slotStack);
    if (item != null) {
      addSlotItem(slotStack, slotBackground, item, enableDrag, equipped, index);
    }
    return slotStack;
  }

  private void addSlotItem(
      Stack slotStack,
      ImageButton slotBackground,
      Item currentItem,
      boolean enableDrag,
      boolean equipped,
      int slotIndex) {
    // create an image with the items texture then extract the Drawable to draw the button
    ImageButton itemButton = new ImageButton(new Image(getTexture(currentItem)).getDrawable());
    itemButton.addListener(ItemTooltip.forItem(currentItem, skin, tooltipManager));
    itemButton.setUserObject(slotBackground);
    if (enableDrag) {
      registerDragSource(itemButton);
      final int index = slotIndex;
      itemButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              entity
                  .getEvents()
                  .trigger(equipped ? MOVE_ACTIVE_TO_INACTIVE : MOVE_INACTIVE_TO_ACTIVE, index, -1);
            }
          });
    }
    slotStack.add(itemButton);
    if (currentItem instanceof ConsumableItem) addConsumableCount(slotStack, currentItem);
  }

  private void addConsumableCount(Stack slotStack, Item currentItem) {
    Table count = new Table();
    count.bottom().right();
    Label countLabel =
        new Label(
            Integer.toString(inventoryComponent.getConsumableCount(currentItem.getId())), skin);
    countLabel.setColor(Color.valueOf(DARK_TEXT_COLOR));
    count.add(countLabel).pad(4);
    count.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    slotStack.add(count);
  }

  private Table pageTabs() {
    Table tabs = new Table();
    addPageTab(tabs, "CHARMS", Page.CHARMS);
    addPageTab(tabs, "CONSUMABLES", Page.CONSUMABLES);
    addPageTab(tabs, "ACHIEVEMENTS", Page.ACHIEVEMENTS);
    return tabs;
  }

  private void addPageTab(Table tabs, String text, Page page) {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.up = null;
    style.down = null;
    style.over = null;
    style.checked = null;
    style.fontColor = Color.valueOf(currentPage == page ? "8b0000ff" : DARK_TEXT_COLOR);
    style.overFontColor = style.fontColor;
    style.downFontColor = style.fontColor;

    TextButton tab = new TextButton(text, style);
    tab.setName("inventory-tab-" + page.name().toLowerCase(java.util.Locale.ROOT));
    tab.setColor(Color.WHITE);
    tab.getLabel().setFontScale(0.65f);
    tab.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            showPage(page);
          }
        });
    tabs.add(tab).expandX().fillX().height(38).pad(2);
  }

  public boolean isVisible() {
    return table != null && table.isVisible();
  }

  private List<Stack> selectedSlots() {
    return selectedEquipped ? equippedSlots : storedSlots;
  }

  private void updateSelection() {
    if (selectedSlots().isEmpty()) return;
    selectedIndex = Math.clamp(selectedIndex, 0, selectedSlots().size() - 1);
    for (Stack slot : equippedSlots) slot.getChildren().first().setColor(Color.WHITE);
    for (Stack slot : storedSlots) slot.getChildren().first().setColor(Color.WHITE);
    selectedSlots().get(selectedIndex).getChildren().first().setColor(Color.valueOf("d4af37ff"));
    List<Item> items = getPageItems(selectedEquipped);
    Item item = selectedIndex < items.size() ? items.get(selectedIndex) : null;
    if (item == null) {
      selectionDetails.setText("Empty slot\nSelect an item with WASD or the mouse.");
    } else {
      List<String> lines = ItemTooltip.lines(item);
      selectionDetails.setText(
          item.getName()
              + "  |  Space: "
              + (selectedEquipped ? "Unequip" : "Equip")
              + "\n"
              + String.join("\n", lines.subList(1, lines.size())));
    }
  }

  private static Texture getTexture(Item item) {
    return ServiceLocator.getResourceService().getAsset(item.getTexture(), Texture.class);
  }

  /** Registers the item slot as a drop target */
  private void registerDropTarget(Stack slotStack) {
    dragAndDrop.addTarget(
        new DragAndDrop.Target(slotStack) {
          @Override
          public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
            return true;
          }

          @Override
          public void drop(Source source, Payload payload, float x, float y, int pointer) {
            Actor draggedGroup = payload.getDragActor();
            ImageButton currentTargetSlot =
                (ImageButton) ((Stack) getActor()).getChildren().first();

            // Parse original source slot details
            ImageButton previousSlot = (ImageButton) draggedGroup.getUserObject();
            String[] fromData = ((String) previousSlot.getUserObject()).split(":");
            String fromType = fromData[0]; // ACTIVE_SLOT or "inactive"
            int fromIndex = Integer.parseInt(fromData[1]);

            // Parse destination target slot details
            String[] toData = ((String) currentTargetSlot.getUserObject()).split(":");
            String toType = toData[0]; // ACTIVE_SLOT or "inactive"
            int toIndex = Integer.parseInt(toData[1]);

            if (Objects.equals(fromType, ACTIVE_SLOT) && Objects.equals(toType, "inactive")) {
              entity.getEvents().trigger(MOVE_ACTIVE_TO_INACTIVE, fromIndex, toIndex);
            } else if (Objects.equals(fromType, "inactive")
                && Objects.equals(toType, ACTIVE_SLOT)) {
              entity.getEvents().trigger(MOVE_INACTIVE_TO_ACTIVE, fromIndex, toIndex);
            } else if (Objects.equals(fromType, ACTIVE_SLOT)
                && Objects.equals(toType, ACTIVE_SLOT)) {
              entity.getEvents().trigger("moveActiveItem", fromIndex, toIndex);
            }
          }
        });
  }

  /** Drags a preview so rejected drops leave the original item in its slot. */
  private void registerDragSource(ImageButton item) {
    dragAndDrop.addSource(
        new DragAndDrop.Source(item) {
          @Override
          public Payload dragStart(InputEvent event, float x, float y, int pointer) {
            Payload payload = new Payload();
            Image preview = new Image(item.getStyle().imageUp);
            preview.setSize(item.getWidth(), item.getHeight());
            preview.setUserObject(item.getUserObject());
            payload.setDragActor(preview);
            dragAndDrop.setDragActorPosition(
                getActor().getWidth() / 2, -getActor().getHeight() / 2);
            return payload;
          }
        });
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    tooltipManager.hideAll();
    stage.removeCaptureListener(keyboardNavigation);
    capturedKeys.clear();
    table.remove();
    super.dispose();
  }

  /** Shows or hides the inventory book. */
  public void setVisible(boolean set) {
    if (set) refreshPage();
    else tooltipManager.hideAll();
    table.setVisible(set);
    if (set) {
      // Enemy health bars may have been added to the stage since the book was created.
      table.toFront();
    }
  }
}
