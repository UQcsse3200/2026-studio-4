package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.Charm;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.ItemTooltip;
import com.csse3200.game.ui.UIComponent;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
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

  public InventoryDisplay(InventoryComponent inventoryComponent) {
    this.inventoryComponent = inventoryComponent;
  }

  @Override
  public void create() {
    super.create();
    buildPage();
    table.setVisible(false);
    TooltipManager manager = TooltipManager.getInstance();
    manager.initialTime = 0.2f; // Show after 0.2 seconds instead of 2 seconds
    manager.resetTime = 0.4f; // Reset delay when moving between items quickly
    manager.subsequentTime = 0.2f;
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
    boolean visible = table.isVisible();
    table.remove();
    charmsPage = !charmsPage;
    buildPage();
    table.setVisible(visible);
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
    Table leftGrid = gridDraw(3, 3, 72, "Active");
    leftPage.add(leftGrid);

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    // Create Grid
    Table rightGrid = gridDraw(4, 20, 64, "Consumable");

    rightPage.add(rightGrid).center().pad(10);
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
    // display stats

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    // Create Grid
    Table rightGrid = drawItemGrid(4, 20, 64, inventoryComponent.getCharms());

    rightPage.add(rightGrid).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /** draws an item grid from a list of items */
  private Table drawItemGrid(
      int columns, int totalSlots, int slotSize, List<? extends Item> items) {
    Table grid = new Table();
    items.sort(Comparator.comparing(Item::getName));

    for (int i = 0; i < totalSlots; i++) {

      Stack slotStack = new Stack();
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");
      slotStack.add(slotBackground);

      if (i < items.size()) {
        Item currentItem = items.get(i);
        // create an image with the items texture then extract the Drawable to draw the button
        ImageButton itemButton = new ImageButton(new Image(getTexture(currentItem)).getDrawable());
        itemButton.addListener(
            new ClickListener() {
              @Override
              public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                logger.info("Mouse enter on item: {}", currentItem.getName());
              }

              @Override
              public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                logger.info("Mouse exit on item: {}", currentItem.getName());
              }
            });
        slotStack.add(itemButton);
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

  /**
   * Builds a grid style inventory according to the parameters given
   *
   * @param columns number of columns in the inventory
   * @param totalSlots number of total slots in the inventory
   * @param slotSize size of the slots in the inventory
   * @param type what inventory the grid is being built for
   * @return a table component to be displayed in the inventory
   */
  private Table gridDraw(int columns, int totalSlots, int slotSize, String type) {
    // Grid Table building
    Table grid = new Table();
    java.util.List<Charm> charms = inventoryComponent.getCharms();
    Dictionary<Charm, Integer> charmDict = countCharms(charms);
    Enumeration<Charm> uniqueCharms = charmDict.keys();

    for (int i = 0; i < totalSlots; i++) {
      Stack slotStack = new Stack();
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");
      slotStack.add(slotBackground);
      if (Objects.equals(type, "Charms") && uniqueCharms.hasMoreElements()) {
        charmIconDraw(slotStack, charmDict, uniqueCharms);
      } else if (Objects.equals(type, "Consumable")
          && uniqueCharms.hasMoreElements()) { // Change to consumables once have
        consumableIconDraw(slotStack, charmDict, uniqueCharms, slotBackground);
        slotBackground.setUserObject("inactive:" + i);
        registerDropTarget(slotBackground);
      } else if (Objects.equals(type, "Active")) {
        slotBackground.setUserObject("active:" + i);
        registerDropTarget(slotBackground);
      }
      grid.add(slotStack).size(slotSize).pad(3);
      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        grid.row();
      }
    }
    return grid;
  }

  private void registerDropTarget(ImageButton slotBackground) {
    dragAndDrop.addTarget(
        new DragAndDrop.Target(slotBackground) {
          @Override
          public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
            return true;
          }

          @Override
          public void drop(Source source, Payload payload, float x, float y, int pointer) {
            Actor draggedGroup = payload.getDragActor();
            ImageButton currentTargetSlot = (ImageButton) getActor();
            Stack targetStack = (Stack) currentTargetSlot.getParent();

            // Parse original source slot details
            ImageButton previousSlot = (ImageButton) draggedGroup.getUserObject();
            String[] fromData = ((String) previousSlot.getUserObject()).split(":");
            String fromType = fromData[0]; // "active" or "inactive"
            int fromIndex = Integer.parseInt(fromData[1]);

            // Parse destination target slot details
            String[] toData = ((String) currentTargetSlot.getUserObject()).split(":");
            String toType = toData[0]; // "active" or "inactive"
            int toIndex = Integer.parseInt(toData[1]);

            // Complete visual UI shift
            draggedGroup.remove();
            targetStack.addActorAt(1, draggedGroup);
            draggedGroup.setUserObject(currentTargetSlot);
            if (Objects.equals(fromType, "active") && Objects.equals(toType, "inactive")) {
              entity.getEvents().trigger("moveActiveToInactiveItem", fromIndex, toIndex);
            } else if (Objects.equals(fromType, "inactive") && Objects.equals(toType, "active")) {
              entity.getEvents().trigger("moveInactiveToActiveItem", fromIndex, toIndex);
            }
          }
        });
  }

  /**
   * Draws a charm icon on top of an inventory slot
   *
   * @param slotStack The stack the icon is being drawn on top of
   * @param charmDict The dictionary containing the charms and their quantities
   * @param uniqueCharms Enumeration of the charms
   */
  /**
   * Draws a charm icon on top of an inventory slot
   *
   * @param slotStack The stack the icon is being drawn on top of
   * @param charmDict The dictionary containing the charm objects and their quantities
   * @param uniqueCharms Enumeration of the charm objects
   */
  private void charmIconDraw(
      Stack slotStack, Dictionary<Charm, Integer> charmDict, Enumeration<Charm> uniqueCharms) {
    Charm currentCharm = uniqueCharms.nextElement();
    int quantity = charmDict.get(currentCharm);

    switch (currentCharm.getName()) {
      case "Strength Charm" -> {
        ImageButton strengthCharmIcon = new ImageButton(inventory, "strengthCharm");
        strengthCharmIcon.addListener(ItemTooltip.forItem(currentCharm, skin));
        slotStack.add(strengthCharmIcon);
      }
      case "Speed Charm" -> {
        Image speedCharmIcon = new Image(skin, "button-c");
        speedCharmIcon.addListener(ItemTooltip.forItem(currentCharm, skin));
        slotStack.add(speedCharmIcon);
      }
      case "Attack Speed Charm" -> {
        Image attackSpeedCharmIcon = new Image(skin, "button-pressed-c");
        attackSpeedCharmIcon.addListener(ItemTooltip.forItem(currentCharm, skin));
        slotStack.add(attackSpeedCharmIcon);
      }
    }
    // add other charms when added

    if (quantity >= 1) {
      Table textOverlayTable = new Table();
      textOverlayTable.bottom().right();
      textOverlayTable.setTouchable(Touchable.disabled); // Prevents text from blocking hover

      Label quantityLabel = new Label(String.valueOf(quantity), skin);
      textOverlayTable.add(quantityLabel).padBottom(2).padRight(4);
      slotStack.add(textOverlayTable);
    }
  }

  /**
   * Draws a Consumable icon on top of an inventory slot
   *
   * @param slotStack The stack the icon is being drawn on top of
   * @param consumableDict The dictionary containing the consumables and their quantities
   * @param uniqueConsumable Enumeration of the consumables
   */
  private void consumableIconDraw(
      Stack slotStack,
      Dictionary<Charm, Integer> consumableDict,
      Enumeration<Charm> uniqueConsumable,
      ImageButton slotBackground) {
    // Convert to consumables when added
    Charm currentCharm = uniqueConsumable.nextElement();
    int quantity = consumableDict.get(currentCharm);

    if (currentCharm.getName().equals("Strength Charm")) {
      ImageButton strengthCharmIcon = new ImageButton(inventory, "strengthCharm");
      strengthCharmIcon.setUserObject(slotBackground);

      // Add tooltip for hover detection
      strengthCharmIcon.addListener(ItemTooltip.forItem(currentCharm, skin));

      slotStack.add(strengthCharmIcon);
      // add other consumables when added
      if (quantity >= 1) {
        Table textOverlayTable = new Table();
        textOverlayTable.bottom().right();
        textOverlayTable.setTouchable(Touchable.disabled); // Prevents text from blocking hover

        Label quantityLabel = new Label(String.valueOf(quantity), skin);
        textOverlayTable.add(quantityLabel).padBottom(2).padRight(4);
        slotStack.add(textOverlayTable);

        dragAndDrop.addSource(
            new DragAndDrop.Source(strengthCharmIcon) {
              @Override
              public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                Payload payload = new Payload();
                table.addActor(getActor());
                payload.setDragActor(getActor());
                dragAndDrop.setDragActorPosition(
                    getActor().getWidth() / 2, -getActor().getHeight() / 2);
                return payload;
              }

              @Override
              public void dragStop(
                  InputEvent event, float x, float y, int pointer, Payload payload, Target target) {
                if (target == null) {
                  ImageButton originalSlot = (ImageButton) getActor().getUserObject();
                  Stack originalStack = (Stack) originalSlot.getParent();
                  getActor().remove();
                  originalStack.addActorAt(1, getActor());
                }
              }
            });
      }
    }
  }

  /**
   * Creates a dictionary of charms and their count
   *
   * @param charms List of charms in the inventory of the player
   * @return a dictionary with charm types as keys and their count as value
   */
  private Dictionary<Charm, Integer> countCharms(java.util.List<Charm> charms) {
    java.util.Dictionary<Charm, Integer> charmsDict = new java.util.Hashtable<>();

    for (Charm charm : charms) {
      // We now use the Charm object itself as the key
      Integer currentCount = charmsDict.get(charm);

      if (currentCount == null) {
        charmsDict.put(charm, 1);
      } else {
        charmsDict.put(charm, currentCount + 1);
      }
    }
    return charmsDict;
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
    table.remove();
    super.dispose();
  }

  /** Shows or hides the inventory book. */
  public void setVisible(boolean set) {
    table.setVisible(set);
    if (set) {
      // Enemy health bars may have been added to the stage since the book was created.
      table.toFront();
    }
  }
}
