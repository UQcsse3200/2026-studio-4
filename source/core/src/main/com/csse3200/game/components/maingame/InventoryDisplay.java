package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the inventory book and its charms and consumables pages. */
public class InventoryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(InventoryDisplay.class);
  private static final float Z_INDEX = 2f;
  private boolean charmsPage = true;
  private Table table;
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
    // Keep immediate, static inventory hints separate from other UI's tooltip settings.
    tooltipManager.initialTime = 0f;
    tooltipManager.resetTime = 0f;
    tooltipManager.subsequentTime = 0f;
    tooltipManager.animations = false;
    tooltipManager.hideAll(); // Reset the manager's internal delay after changing its timing.
  }

  /** Builds the inventory page depending on which inventory is being displayed */
  private void buildPage() {
    // Create main table
    table = new Table();
    table.setName("inventory-book");
    this.dragAndDrop = new DragAndDrop();
    table.setFillParent(true);
    stage.addActor(table);
    // Create initial stack
    Stack bookStack = new Stack();

    // Create book cover UI
    Image bookCover = new Image(inventory.getDrawable("UI_TravelBook_BookCover01a"));
    bookCover.setScaling(Scaling.fill); // Forces graphic to fill the stack container
    bookStack.add(bookCover);

    Table pagesContainer;
    if (charmsPage) {
      pagesContainer = charmsCreate();
    } else {
      pagesContainer = consumableCreate();
    }
    // combine all together
    bookStack.add(pagesContainer);
    table.add(bookStack).size(800, 500).center();

    ImageButton arrow = new ImageButton(inventory, "arrow");
    arrow.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("next inventory button clicked");
            entity.getEvents().trigger("nextPage");
          }
        });

    table.add(arrow).size(32, 32).pad(6);

    table.setVisible(true);
  }

  /** Changes the current page to the other inactive page and sets the flag */
  public void changePage() {
    charmsPage = !charmsPage;
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
    // Overall page table with padding inside cover
    Table pagesContainer = new Table();
    pagesContainer.pad(40, 50, 40, 50);

    // left page creation
    Table leftPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();

    leftPage.add(new Label("Consumables", skin)).top().colspan(3).pad(25f);
    leftPage.row();
    leftPage.add(new Label("Equipped", skin)).colspan(3);
    leftPage.row();

    Table leftGrid =
        drawItemGrid(
            3, InventoryComponent.CONSUMABLE_SLOT_COUNT, 72, getPageItems(true), true, true);
    leftGrid.setName("consumables-equipped");
    leftPage.add(leftGrid);

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    List<? extends Item> stored = getPageItems(false);
    Table rightGrid = drawItemGrid(4, Math.max(20, stored.size()), 64, stored, true, false);

    rightPage.add(scrollGrid(rightGrid)).size(300, 360).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /**
   * Creates the Charms inventory UI
   *
   * @return the Charms inventory UI
   */
  private Table charmsCreate() {
    // Overall page table with padding inside cover
    Table pagesContainer = new Table();
    pagesContainer.pad(40, 50, 40, 50);

    // left page creation
    Table leftPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();

    leftPage.add(new Label("Charms", skin)).top().colspan(3).pad(25f);
    leftPage.row();
    leftPage.add(new Label("Equipped", skin)).colspan(3);
    leftPage.row();
    List<? extends Item> equipped = getPageItems(true);
    Table leftGrid = drawItemGrid(3, Math.max(5, equipped.size()), 72, equipped, true, true);
    leftGrid.setName("charms-equipped");
    leftPage.add(scrollGrid(leftGrid)).size(250, 300);

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    // Create Grid
    List<? extends Item> stored = getPageItems(false);
    Table rightGrid = drawItemGrid(4, Math.max(20, stored.size()), 64, stored, true, false);

    rightPage.add(scrollGrid(rightGrid)).size(300, 360).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

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
          count
              .add(
                  new Label(
                      Integer.toString(inventoryComponent.getConsumableCount(currentItem.getId())),
                      skin))
              .pad(4);
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
