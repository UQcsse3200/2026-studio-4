package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the inventory book and a persistent, decorative six-slot hotbar. */
public class InventoryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(InventoryDisplay.class);
  private static final float Z_INDEX = 2f;
  private static final float HOTBAR_SLOT_SIZE = 96f;
  private static final float HOTBAR_TOP_INSET = 48f;
  private static final float HOTBAR_SLOT_GAP = 6f;
  private static final float HOTBAR_SIDE_INSET = 340f;
  private boolean charmsPage = true;
  private Table table;
  private Table hotbarTable;
  private Texture hotbarTexture;

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    buildPage();
    table.setVisible(false);
    buildHotbar();
  }

  /** Keeps the hotbar on the stage independently of the book's visibility and page changes. */
  private void buildHotbar() {
    hotbarTexture = new Texture(Gdx.files.internal("images/ui/ui_big_pieces.png"));
    hotbarTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    // The large circular frame in the sprite sheet's bottom-right minimap section.
    TextureRegionDrawable slot =
        new TextureRegionDrawable(new TextureRegion(hotbarTexture, 718, 288, 130, 130));

    hotbarTable =
        new Table() {
          @Override
          protected void sizeChanged() {
            super.sizeChanged();
            // Slot sizes depend on the viewport, so discard the groups' cached preferred sizes.
            for (Actor child : getChildren()) {
              if (child instanceof Table group) {
                group.invalidate();
              }
            }
          }
        };
    hotbarTable.setName("inventory-hotbar");
    hotbarTable.setFillParent(true);
    // Keep the groups inward beside the stats, with clearance below long room titles.
    hotbarTable.top().padTop(HOTBAR_TOP_INSET).padLeft(HOTBAR_SIDE_INSET);
    hotbarTable.padRight(
        new Value() {
          @Override
          public float get(Actor context) {
            float groupsWidth =
                2f * (3f * hotbarSlotSize(context.getWidth()) + 2f * HOTBAR_SLOT_GAP);
            // On narrower windows retain room for Exit and a gap between the two groups.
            return Math.min(
                HOTBAR_SIDE_INSET,
                Math.max(96f, context.getWidth() - HOTBAR_SIDE_INSET - groupsWidth - 24f));
          }
        });
    hotbarTable.setTouchable(Touchable.disabled);
    hotbarTable.add(createHotbarGroup(slot));
    hotbarTable.add().expandX();
    hotbarTable.add(createHotbarGroup(slot));
    stage.addActor(hotbarTable);
  }

  private Table createHotbarGroup(TextureRegionDrawable slot) {
    Table group = new Table();
    Value slotSize =
        new Value() {
          @Override
          public float get(Actor context) {
            return hotbarSlotSize(stage.getWidth());
          }
        };
    for (int i = 0; i < 3; i++) {
      Image circle = new Image(slot);
      circle.setScaling(Scaling.fit);
      group.add(circle).size(slotSize).padRight(i < 2 ? HOTBAR_SLOT_GAP : 0f);
    }
    return group;
  }

  /** Use the enlarged circles where space permits, shrinking only for narrow windows. */
  private float hotbarSlotSize(float width) {
    return Math.min(
        HOTBAR_SLOT_SIZE,
        Math.max(1f, (width - HOTBAR_SIDE_INSET - 96f - 24f - 4f * HOTBAR_SLOT_GAP) / 6f));
  }

  /** Builds the inventory page depending on which inventory is being displayed */
  private void buildPage() {
    // Create main table
    table = new Table();
    table.setName("inventory-book");
    table.setFillParent(true);
    // Keep the book centred normally; on short windows leave room for the larger hotbar.
    table.padTop(
        new Value() {
          @Override
          public float get(Actor context) {
            float hotbarBottom = HOTBAR_TOP_INSET + hotbarSlotSize(context.getWidth());
            return Math.max(0f, 2f * (hotbarBottom + 12f) + 500f - context.getHeight());
          }
        });
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
    hotbarTable.toFront();
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
    Table leftGrid = gridDraw(3, 3, 72);
    leftPage.add(leftGrid);

    pagesContainer.add(leftPage).size(365, 500);

    // right page creation
    Table rightPage =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a"));

    // Create Grid
    Table rightGrid = gridDraw(4, 20, 64);

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
    Table rightGrid = gridDraw(4, 20, 64);

    rightPage.add(rightGrid).center().pad(10);
    pagesContainer.add(rightPage).size(365, 500);

    return pagesContainer;
  }

  /**
   * Builds a grid style inventory according to the parameters given
   *
   * @param columns number of columns in the inventory
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
    table.remove();
    hotbarTable.remove();
    hotbarTexture.dispose();
    super.dispose();
  }

  /** Shows or hides only the inventory book; the hotbar remains visible. */
  public void setVisible(boolean set) {
    table.setVisible(set);
    if (set) {
      // Enemy health bars may have been added to the stage since the book was created.
      table.toFront();
    }
  }
}
