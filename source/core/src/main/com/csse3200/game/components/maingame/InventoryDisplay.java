package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.Item;
import com.csse3200.game.services.AchievementService;
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
  private Table table;
  private DragAndDrop dragAndDrop;
  private InventoryComponent inventoryComponent;
  private static final String leftPageCover = "UI_TravelBook_BookPageLeft01a";
  private static final String rightPageCover = "UI_TravelBook_BookPageRight01a";
  private static final String inventoryStyle = "inventory";

  private enum Page {
    CHARMS,
    CONSUMABLES,
    ACHIEVEMENTS
  }

  private Page currentPage = Page.CHARMS;

  public InventoryDisplay(InventoryComponent inventoryComponent) {
    this.inventoryComponent = inventoryComponent;
  }

  @Override
  public void create() {
    super.create();
    buildPage();
    table.setVisible(false);
    TooltipManager manager = TooltipManager.getInstance();
    manager.initialTime = 0.02f; // Show after 0.2 seconds instead of 2 seconds
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

    Table pagesContainer =
        switch (currentPage) {
          case CHARMS -> charmsCreate();
          case CONSUMABLES -> consumableCreate();
          case ACHIEVEMENTS -> achievementsCreate();
        };

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
    currentPage =
        switch (currentPage) {
          case CHARMS -> Page.CONSUMABLES;
          case CONSUMABLES -> Page.ACHIEVEMENTS;
          case ACHIEVEMENTS -> Page.CHARMS;
        };
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
    Table leftPage = new Table().background(inventory.getDrawable(leftPageCover)).top();

    leftPage.add(new Label("Consumables", skin, inventoryStyle)).top().colspan(3).pad(25f);
    leftPage.row();
    leftPage.add(new Label("Equipped", skin, inventoryStyle)).colspan(3);
    leftPage.row();

    // Please pass in list of equipped consumables
    Table leftGrid = drawItemGrid(3, 3, 72, new ArrayList<>(), true);
    leftPage.add(leftGrid);

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage = new Table().background(inventory.getDrawable(rightPageCover));

    // Please pass in list of consumables in inventory
    Table rightGrid = drawItemGrid(4, 20, 64, new ArrayList<>(), true);

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
    Table leftPage = new Table().background(inventory.getDrawable(leftPageCover)).top();

    leftPage.add(new Label("Charms", skin, inventoryStyle)).top().colspan(3).pad(25f);
    leftPage.row();
    // display stats

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage = new Table().background(inventory.getDrawable(rightPageCover));

    // Create Grid
    Table rightGrid = drawItemGrid(4, 20, 64, inventoryComponent.getCharms(), false);

    rightPage.add(rightGrid).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  private Table achievementsCreate() {
    Table pagesContainer = new Table();
    pagesContainer.pad(40, 50, 40, 50);

    List<Achievement> all = new ArrayList<>();
    AchievementService achievementService = ServiceLocator.getAchievementService();
    if (achievementService != null) {
      all.addAll(achievementService.getAchievements());
    }

    List<Achievement> locked = all.stream().filter(a -> !a.isUnlocked()).toList();
    List<Achievement> unlocked = all.stream().filter(Achievement::isUnlocked).toList();

    Table leftPage = new Table().background(inventory.getDrawable(leftPageCover)).top();
    leftPage.add(new Label("Locked", skin, inventoryStyle)).top().pad(25f).row();
    leftPage.add(achievementList(locked, false)).grow();
    pagesContainer.add(leftPage).size(365, 500);

    Table rightPage = new Table().background(inventory.getDrawable(rightPageCover)).top();
    rightPage.add(new Label("Unlocked", skin, inventoryStyle)).top().pad(25f).row();
    rightPage.add(achievementList(unlocked, true)).grow();
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /** Builds a scrollable column of achievement names, styled for locked or unlocked. */
  private ScrollPane achievementList(List<Achievement> achievements, boolean unlockedStyle) {
    Table list = new Table();
    list.top();
    for (Achievement a : achievements) {
      Label name = new Label(a.getName(), skin, inventoryStyle);
      name.setColor(unlockedStyle ? Color.GOLD : Color.GRAY);
      list.add(name).left().pad(6).row();
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
   */
  private Table drawItemGrid(
      int columns, int totalSlots, int slotSize, List<? extends Item> items, boolean enableDrag) {
    Table grid = new Table();

    for (int i = 0; i < totalSlots; i++) {

      Stack slotStack = new Stack();
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");
      if (enableDrag) registerDropTarget(slotBackground);
      slotStack.add(slotBackground);

      if (i < items.size()) {
        Item currentItem = items.get(i);
        // create an image with the items texture then extract the Drawable to draw the button
        ImageButton itemButton = new ImageButton(new Image(getTexture(currentItem)).getDrawable());
        itemButton.addListener(ItemTooltip.forItem(currentItem, skin));
        if (enableDrag) registerDragSource(itemButton);
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

  /** Registers the item slot as a drop target */
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

  /** Added dragging behaviour to the ImageButton currently not implemented */
  private void registerDragSource(ImageButton item) {
    dragAndDrop.addSource(
        new DragAndDrop.Source(item) {
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
    changePage();
    if (set) {
      // Enemy health bars may have been added to the stage since the book was created.
      table.toFront();
    }
  }
}
