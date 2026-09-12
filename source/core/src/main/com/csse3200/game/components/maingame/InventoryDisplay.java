package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.ui.UIComponent;
import java.awt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays a button to exit the Main Game screen to the Main Menu screen. */
public class InventoryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainGameExitDisplay.class);
  private static final float Z_INDEX = 2f;
  private boolean charmsPage = true;
  private Table table;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    buildPage();
    table.setVisible(false);

  }

  /**
   * Builds the inventory page depending on which inventory is being displayed
   */
  private void buildPage() {
    //Create main table
    table = new Table();
    table.setFillParent(true);
    stage.addActor(table);
    //Create initial stack
    Stack bookStack = new Stack();

    //Create book cover UI
    Image bookCover = new Image(inventory.getDrawable("UI_TravelBook_BookCover01a"));
    bookCover.setScaling(Scaling.fill); // Forces graphic to fill the stack container
    bookStack.add(bookCover);

    Table pagesContainer;
    if (charmsPage) {
      pagesContainer = charmsCreate();
    } else {
      pagesContainer = consumableCreate();
    }
    //combine all together
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

  /**
   * Changes the current page to the other inactive page and sets the flag
   */
  public void changePage() {
    if (charmsPage) {
      table.clear();
      charmsPage = false;
      buildPage();
    }
    else {
      table.clear();
      charmsPage = true;
      buildPage();
    }
  }

  /**
   * Creates the consumable inventory UI
   * @return the Consumable inventory UI
   */
  private Table consumableCreate() {
    //Overall page table with padding inside cover
    Table pagesContainer = new Table();
    pagesContainer.pad(40, 50, 40, 50);

    //left page creation
    Table leftPage = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();

    leftPage.add(new Label("Consumables", skin)).top().colspan(3).pad(25f);
    leftPage.row();
    leftPage.add(new Label("Equipped", skin)).colspan(3);
    leftPage.row();
    Table leftGrid = gridDraw(3, 3, 72);
    leftPage.add(leftGrid);

    pagesContainer.add(leftPage).size(365, 500);

    //right page creation
    Table rightPage = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    //Create Grid
    Table rightGrid = gridDraw(4, 20, 64);

    rightPage.add(rightGrid).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /**
   * Creates the consumable inventory UI
   * @return the Consumable inventory UI
   */
  private Table charmsCreate() {
    //Overall page table with padding inside cover
    Table pagesContainer = new Table();
    pagesContainer.pad(40, 50, 40, 50);

    //left page creation
    Table leftPage = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();

    leftPage.add(new Label("Charms", skin)).top().colspan(3).pad(25f);
    leftPage.row();
    //display stats

    pagesContainer.add(leftPage).size(365, 500);

    //right page creation
    Table rightPage = new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    //Create Grid
    Table rightGrid = gridDraw(4, 20, 64);

    rightPage.add(rightGrid).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /**
   * Builds a grid style inventory according to the parameters given
   * @param columns number of coloumns in the inventory
   * @param totalSlots number of total slots in the inventory
   * @param slotSize size of the slots in the inventory
   * @return a table component to be displayed in the inventory
   */
  private Table gridDraw(int columns, int totalSlots, int slotSize) {
    // Grid Table building
    Table grid = new Table();
    for (int i = 0; i < totalSlots; i++) {
      ImageButton slotBackground = new ImageButton(inventory, "inventory-box");

      grid.add(slotBackground).size(slotSize).pad(3);

      // Break to a new row after reaching the column limit
      if ((i + 1) % columns == 0) {
        grid.row();
      }
    }
    return grid;
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
    table.clear();
    super.dispose();
  }

  public void setVisible(boolean set) {
    table.setVisible(set);
  }
}
