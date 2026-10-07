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
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.ConsumableItem;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.ItemTooltip;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Displays the inventory book and its charms and consumables pages. */
public class InventoryDisplay extends UIComponent {
  private static final float Z_INDEX = 2f;
  private boolean charmsPage = true;
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
            case Keys.Q, Keys.E -> entity.getEvents().trigger("nextPage");
            case Keys.W, Keys.UP -> moveSelection(0, -1);
            case Keys.S, Keys.DOWN -> moveSelection(0, 1);
            case Keys.A, Keys.LEFT -> moveSelection(-1, 0);
            case Keys.D, Keys.RIGHT -> moveSelection(1, 0);
            case Keys.SPACE ->
                entity
                    .getEvents()
                    .trigger(
                        selectedEquipped ? "moveActiveToInactiveItem" : "moveInactiveToActiveItem",
                        selectedIndex,
                        -1);
            case Keys.ESCAPE -> inventoryComponent.toggleDisplay();
            default -> {}
          }
          return true;
        }

        @Override
        public boolean keyUp(InputEvent event, int keycode) {
          // Pre-existing movement releases must still reach the player input handler.
          return capturedKeys.remove(keycode);
        }
      };
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
    this.inventoryComponent = inventoryComponent;
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
    label.setColor(Color.valueOf("3a2618ff"));
    return label;
  }

  /** Builds the inventory page depending on which inventory is being displayed */
  private void buildPage() {
    // Create main table
    table = new Table();
    table.setName("inventory-book");
    this.dragAndDrop = new DragAndDrop();
    table.setFillParent(true);
    stage.addActor(table);
    equippedSlots.clear();
    storedSlots.clear();

    selectionDetails = new Label("", skin);
    selectionDetails.setName("inventory-selection-details");
    selectionDetails.setWrap(true);
    selectionDetails.setColor(Color.valueOf("3a2618ff"));

    Stack bookStack = new Stack();

    Image bookCover = new Image(inventory.getDrawable("UI_TravelBook_BookCover01a"));
    bookCover.setName("inventory-book-cover");
    bookCover.setScaling(Scaling.fill);
    bookStack.add(bookCover);

    Table pagesContainer = charmsPage ? charmsCreate() : consumableCreate();
    bookStack.add(pagesContainer);

    table.add(bookStack).size(800, 560).center();

    updateSelection();

    table.setVisible(true);
  }

  /** Changes the current page to the other inactive page and sets the flag */
  public void changePage() {
    charmsPage = !charmsPage;
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
  public List<? extends Item> getPageItems(boolean equipped) {
    if (charmsPage) {
      return inventoryComponent.getCharms().stream()
          .filter(charm -> charm.isEquipped() == equipped)
          .toList();
    }
    List<Item> items = new ArrayList<>();
    if (equipped) {
      for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
        String id = inventoryComponent.getConsumableSlot(i);
        items.add(id == null ? null : ItemCatalog.create(id, 1));
      }
    } else {
      List<? extends Item> assignedItems = getPageItems(true);
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

    Table leftPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();
    leftPage.setName("inventory-page-left");
    leftPage.pad(20);

    Table tabs = new Table();
    addPageTab(tabs, "Q  CHARMS", true);
    addPageTab(tabs, "CONSUMABLES  E", false);
    leftPage.add(tabs).padBottom(10).growX().row();

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

    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a")).top();
    rightPage.setName("inventory-page-right");
    rightPage.pad(20);

    rightPage.add(createDarkLabel("Backpack")).padBottom(10).row();

    List<? extends Item> stored = getPageItems(false);
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

    Table leftPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();
    leftPage.setName("inventory-page-left");
    leftPage.pad(20);

    Table tabs = new Table();
    addPageTab(tabs, "Q  CHARMS", true);
    addPageTab(tabs, "CONSUMABLES  E", false);
    leftPage.add(tabs).padBottom(10).growX().row();

    leftPage.add(createDarkLabel("Equipped")).row();

    List<? extends Item> equipped = getPageItems(true);
    Table leftGrid = drawItemGrid(3, Math.max(5, equipped.size()), 72, equipped, true, true);
    leftGrid.setName("charms-equipped");
    leftPage.add(scrollGrid(leftGrid)).size(250, 300).row();

    Label hints = createDarkLabel("WASD/Arrows:Move Space:Equip/Unequip\nQ/E:Category I/Esc:Close");
    hints.setFontScale(0.8f);
    hints.setWrap(true);
    leftPage.add(hints).width(310).padTop(10).center();

    pagesContainer.add(leftPage).size(350, 500).padRight(20);

    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a")).top();
    rightPage.setName("inventory-page-right");
    rightPage.pad(20);

    rightPage.add(createDarkLabel("Backpack")).padBottom(10).row();

    List<? extends Item> stored = getPageItems(false);
    Table rightGrid = drawItemGrid(4, Math.max(20, stored.size()), 64, stored, true, false);

    rightPage.add(scrollGrid(rightGrid)).size(310, 250).center().padBottom(10).row();

    Table detailsTable = new Table();
    detailsTable.add(selectionDetails).width(270);
    rightPage.add(scrollGrid(detailsTable)).growX().height(100);

    pagesContainer.add(rightPage).size(350, 500);

    return pagesContainer;
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
      List<? extends Item> items,
      boolean enableDrag,
      boolean equipped) {
    Table grid = new Table();

    for (int i = 0; i < totalSlots; i++) {

      Stack slotStack = new Stack();
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");
      slotBackground.setUserObject((equipped ? "active:" : "inactive:") + i);
      slotStack.setName(
          (charmsPage ? "charm" : "consumable") + (equipped ? "-equipped-" : "-stored-") + i);
      slotStack.add(slotBackground);
      (equipped ? equippedSlots : storedSlots).add(slotStack);
      final int slotIndex = i;
      slotStack.addListener(
          new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
              if (pointer != -1) return;
              selectedEquipped = equipped;
              selectedIndex = slotIndex;
              updateSelection();
            }
          });
      if (enableDrag) registerDropTarget(slotStack);

      if (i < items.size() && items.get(i) != null) {
        Item currentItem = items.get(i);
        // create an image with the items texture then extract the Drawable to draw the button
        ImageButton itemButton = new ImageButton(new Image(getTexture(currentItem)).getDrawable());
        itemButton.addListener(ItemTooltip.forItem(currentItem, skin, tooltipManager));
        itemButton.setUserObject(slotBackground);
        if (enableDrag) {
          registerDragSource(itemButton);
          final int index = i;
          itemButton.addListener(
              new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                  entity
                      .getEvents()
                      .trigger(
                          equipped ? "moveActiveToInactiveItem" : "moveInactiveToActiveItem",
                          index,
                          -1);
                }
              });
        }
        slotStack.add(itemButton);
        if (currentItem instanceof ConsumableItem) {
          Table count = new Table();
          count.bottom().right();
          Label countLabel =
              new Label(
                  Integer.toString(inventoryComponent.getConsumableCount(currentItem.getId())),
                  skin);
          countLabel.setColor(Color.valueOf("3a2618ff"));
          count.add(countLabel).pad(4);
          count.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
          slotStack.add(count);
        }
      }

      grid.add(slotStack).size(slotSize).pad(3);
      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        grid.row();
      }
    }

    return grid;
  }

  private void addPageTab(Table tabs, String text, boolean charms) {
    TextButton.TextButtonStyle style =
        new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
    style.up = null;
    style.down = null;
    style.over = null;
    style.checked = null;
    style.fontColor = charmsPage == charms ? Color.valueOf("8b0000ff") : Color.valueOf("3a2618ff");
    style.overFontColor = style.fontColor;
    style.downFontColor = style.fontColor;

    TextButton tab = new TextButton(text, style);
    tab.setName(charms ? "inventory-tab-charms" : "inventory-tab-consumables");
    tab.setColor(Color.WHITE);
    tab.getLabel().setFontScale(0.8f);
    tab.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (charmsPage != charms) entity.getEvents().trigger("nextPage");
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

  private void moveSelection(int dx, int dy) {
    tooltipManager.hideAll();
    int columns = selectedEquipped ? 3 : 4;
    int column = selectedIndex % columns;
    if ((dx < 0 && column == 0 && !selectedEquipped)
        || (dx > 0
            && (column == columns - 1 || selectedIndex == selectedSlots().size() - 1)
            && selectedEquipped)) {
      int row = selectedIndex / columns;
      selectedEquipped = !selectedEquipped;
      int newColumns = selectedEquipped ? 3 : 4;
      selectedIndex =
          Math.min(
              row * newColumns + (selectedEquipped ? newColumns - 1 : 0),
              selectedSlots().size() - 1);
    } else {
      int candidate = selectedIndex + dx + dy * columns;
      if (dy > 0 && (selectedIndex / columns + 1) * columns < selectedSlots().size()) {
        candidate = Math.min(candidate, selectedSlots().size() - 1);
      }
      if (candidate >= 0
          && candidate < selectedSlots().size()
          && (dx == 0 || candidate / columns == selectedIndex / columns)) selectedIndex = candidate;
    }
    updateSelection();
    table.validate();
    Stack selected = selectedSlots().get(selectedIndex);
    if (selected.getParent().getParent() instanceof ScrollPane scroll) {
      scroll.scrollTo(selected.getX(), selected.getY(), selected.getWidth(), selected.getHeight());
      scroll.updateVisualScroll();
    }
  }

  private void updateSelection() {
    selectedIndex = Math.max(0, Math.min(selectedIndex, selectedSlots().size() - 1));
    for (Stack slot : equippedSlots) slot.getChildren().first().setColor(Color.WHITE);
    for (Stack slot : storedSlots) slot.getChildren().first().setColor(Color.WHITE);
    selectedSlots().get(selectedIndex).getChildren().first().setColor(Color.valueOf("d4af37ff"));
    List<? extends Item> items = getPageItems(selectedEquipped);
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
            String fromType = fromData[0]; // "active" or "inactive"
            int fromIndex = Integer.parseInt(fromData[1]);

            // Parse destination target slot details
            String[] toData = ((String) currentTargetSlot.getUserObject()).split(":");
            String toType = toData[0]; // "active" or "inactive"
            int toIndex = Integer.parseInt(toData[1]);

            if (Objects.equals(fromType, "active") && Objects.equals(toType, "inactive")) {
              entity.getEvents().trigger("moveActiveToInactiveItem", fromIndex, toIndex);
            } else if (Objects.equals(fromType, "inactive") && Objects.equals(toType, "active")) {
              entity.getEvents().trigger("moveInactiveToActiveItem", fromIndex, toIndex);
            } else if (Objects.equals(fromType, "active") && Objects.equals(toType, "active")) {
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
