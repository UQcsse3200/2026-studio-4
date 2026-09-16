package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the inventory book and its charms and consumables pages. */
public class InventoryDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(InventoryDisplay.class);
  private static final float Z_INDEX = 2f;
  private boolean charmsPage = false;
  private Table table;
  private final Entity player;
  private final Map<ItemType, Texture> textures = new EnumMap<>(ItemType.class);
  private final Map<ItemType, Label> counts = new EnumMap<>(ItemType.class);
  private final Map<ItemType, Label> timers = new EnumMap<>(ItemType.class);
  private final Map<ItemType, TextButton> useButtons = new EnumMap<>(ItemType.class);
  private Label gold;
  private Label feedback;

  /** Creates the original book without a player binding, useful for layout previews. */
  public InventoryDisplay() {
    this(null);
  }

  /** Binds the book to the same player inventory and effects used by the combat HUD. */
  public InventoryDisplay(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();
    buildPage();
    table.setVisible(false);
  }

  /** Builds the inventory page depending on which inventory is being displayed */
  private void buildPage() {
    // Create main table
    counts.clear();
    timers.clear();
    useButtons.clear();
    gold = null;
    feedback = null;
    table =
        new Table() {
          @Override
          public void act(float delta) {
            super.act(delta);
            if (isVisible()) {
              refreshInventory();
            }
          }
        };
    table.setName("inventory-book");
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
    Table pages = new Table();
    pages.pad(40, 50, 40, 50);
    Table left =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageLeft01a")).top();
    left.pad(28);
    left.add(new Label("Consumables", skin)).left().padBottom(20);
    left.row();
    gold = new Label("Gold: 0", skin);
    gold.setName("inventory-gold");
    left.add(gold).left().padBottom(16);
    left.row();
    Label help =
        new Label(
            "Use an item here or press its number during combat.\n\n7 Health: restores 25 HP\n8 Shield: blocks damage for 8s\n9 Speed: 1.5x speed for 8s\n0 Strength: 1.5x attack for 8s",
            skin);
    help.setWrap(true);
    left.add(help).width(285).left().padBottom(20);
    left.row();
    feedback = new Label("", skin);
    feedback.setName("inventory-use-feedback");
    feedback.setWrap(true);
    left.add(feedback).width(285).left();
    pages.add(left).size(365, 500);

    Table right =
        new Table().background(inventory.getDrawable("UI_TravelBook_BookPageRight01a")).top();
    right.pad(24, 18, 20, 18);
    int[] keys = {7, 8, 9, 0};
    ItemType[] types = {
      ItemType.HEALTH_POTION, ItemType.SHIELD, ItemType.SPEED_POTION, ItemType.STRENGTH_POTION
    };
    for (int i = 0; i < types.length; i++) {
      ItemType type = types[i];
      Texture texture =
          textures.computeIfAbsent(type, t -> new Texture(Gdx.files.internal(t.getTexturePath())));
      Image icon = new Image(texture);
      icon.setScaling(Scaling.fit);
      Table row = new Table();
      row.add(icon).size(40).padRight(8);
      Table info = new Table();
      Label count = new Label("", skin);
      count.setName("inventory-count-" + type.name());
      counts.put(type, count);
      info.add(count).width(185).left();
      info.row();
      Label timer = new Label("", skin);
      timer.setName("inventory-effect-" + type.name());
      timers.put(type, timer);
      info.add(timer).left();
      row.add(info).expandX().left();
      TextButton use = new TextButton("Use", skin);
      use.setName("inventory-use-" + type.name());
      useButtons.put(type, use);
      use.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (use.isDisabled()) {
                return;
              }
              ConsumableEffectComponent effects =
                  player == null ? null : player.getComponent(ConsumableEffectComponent.class);
              boolean used = effects != null && effects.tryUse(type);
              feedback.setText(
                  used ? "Used " + type.getDisplayName() : "Cannot use this item now.");
              refreshInventory();
            }
          });
      row.add(use).width(64).height(36);
      right.add(new Label("[" + keys[i] + "] " + type.getDisplayName(), skin)).left();
      right.row();
      right.add(row).width(325).padBottom(16);
      right.row();
    }
    pages.add(right).size(365, 500);
    refreshInventory();
    return pages;
  }

  /**
   * Refreshes presentation from authoritative state, including while gameplay updates are paused.
   */
  private void refreshInventory() {
    if (gold == null) {
      return;
    }
    InventoryComponent stock =
        player == null ? null : player.getComponent(InventoryComponent.class);
    ConsumableEffectComponent effects =
        player == null ? null : player.getComponent(ConsumableEffectComponent.class);
    CombatStatsComponent stats =
        player == null ? null : player.getComponent(CombatStatsComponent.class);
    gold.setText("Gold: " + (stock == null ? 0 : stock.getGold()));
    for (Map.Entry<ItemType, Label> entry : counts.entrySet()) {
      ItemType type = entry.getKey();
      int count = stock == null ? 0 : stock.getConsumableCount(type);
      entry.getValue().setText("Owned: " + count);
      boolean fullHealth =
          type == ItemType.HEALTH_POTION
              && stats != null
              && stats.getHealth() >= stats.getMaxHealth();
      useButtons
          .get(type)
          .setDisabled(
              count == 0 || effects == null || stats == null || stats.isDead() || fullHealth);
      long remaining = effects == null ? 0 : effects.getRemainingMs(type);
      timers
          .get(type)
          .setText(
              remaining > 0
                  ? String.format(Locale.ROOT, "Active: %.1fs", remaining / 1000f)
                  : fullHealth ? "Health full" : "");
    }
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
    textures.values().forEach(Texture::dispose);
    textures.clear();
    super.dispose();
  }

  /** Shows or hides the inventory book. */
  public void setVisible(boolean set) {
    table.setVisible(set);
    if (set) {
      // Enemy health bars may have been added to the stage since the book was created.
      refreshInventory();
      table.toFront();
    }
  }
}
