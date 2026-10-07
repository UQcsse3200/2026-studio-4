package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a merchant shop: current Gold, a catalogue of purchasable items with their price, and a
 * Buy button per entry.
 *
 * <p>This component owns presentation only. Clicking Buy does not itself touch Gold or the
 * inventory — it fires a {@code shopPurchaseRequested} event carrying the item ID, which the
 * purchase-transaction logic (Sprint 3 #196 work package B2) is expected to listen for and act on.
 * Keeping that boundary means this display can be built, tested and demoed today without
 * depending on, or duplicating, that logic.
 *
 * <p>Not yet wired to a Merchant NPC interaction (#202 depends on Team 3); in the meantime it can
 * be opened for testing via the {@code shop} terminal command, the same way {@code upgrade} lets
 * QA skip a pickup.
 */
public class ShopDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(ShopDisplay.class);
  private static final float Z_INDEX = 2f;

  private final InventoryComponent inventory;
  private final List<ShopEntry> catalogue;

  private Table table;
  private Label goldLabel;
  private final java.util.Map<String, Label> feedbackLabels = new java.util.HashMap<>();

  public ShopDisplay(InventoryComponent inventory, List<ShopEntry> catalogue) {
    if (inventory == null) {
      throw new IllegalArgumentException("inventory must not be null");
    }
    if (catalogue == null || catalogue.isEmpty()) {
      throw new IllegalArgumentException("catalogue must not be null or empty");
    }
    this.inventory = inventory;
    this.catalogue = List.copyOf(catalogue);
  }

  @Override
  public void create() {
    super.create();
    buildTable();
    table.setVisible(false);
  }

  private void buildTable() {
    table = new Table();
    table.setName("shop-display");
    table.setFillParent(true);
    table.center();
    if (stage != null) {
      stage.addActor(table);
    }

    // No background drawable is set here: "window-w" in the shared skin JSON has no matching
    // atlas region (only "window" does), so calling skin.getDrawable("window-w") would throw.
    // Whoever adopts this into the final shop UI should pick real styling with the HUD owner.
    Table panel = new Table();
    panel.pad(20f);

    panel.add(new Label("Shop", skin)).colspan(3).padBottom(12f);
    panel.row();

    goldLabel = new Label("", skin);
    panel.add(goldLabel).colspan(3).left().padBottom(10f);
    panel.row();

    for (ShopEntry entry : catalogue) {
      panel.add(new Label(entry.displayName(), skin)).left().pad(4f);
      panel.add(new Label(entry.price() + "g", skin)).pad(4f);

      TextButton buy = new TextButton("Buy", skin);
      buy.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              attemptPurchase(entry);
            }
          });
      panel.add(buy).pad(4f);
      panel.row();

      Label feedback = new Label("", skin);
      feedbackLabels.put(entry.itemId(), feedback);
      panel.add(feedback).colspan(3).left();
      panel.row();
    }

    TextButton close = new TextButton("Close", skin);
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            close();
          }
        });
    panel.add(close).colspan(3).padTop(10f);

    table.add(panel);
  }

  /** Checks affordability and, if affordable, requests the purchase; otherwise shows feedback. */
  private void attemptPurchase(ShopEntry entry) {
    Label feedback = feedbackLabels.get(entry.itemId());
    if (Boolean.TRUE.equals(inventory.hasGold(entry.price()))) {
      logger.debug("Requesting purchase of {}", entry.itemId());
      if (feedback != null) {
        feedback.setText("");
      }
      entity.getEvents().trigger("shopPurchaseRequested", entry.itemId(), entry.price());
    } else if (feedback != null) {
      feedback.setText("Not enough Gold");
    }
  }

  /** Re-reads Gold from the inventory. Call after open, and after any completed purchase. */
  public void refresh() {
    if (goldLabel != null) {
      goldLabel.setText("Gold: " + inventory.getGold());
    }
  }

  /** Shows the shop and refreshes the Gold display. */
  public void open() {
    refresh();
    table.setVisible(true);
    table.toFront();
  }

  /** Hides the shop. */
  public void close() {
    table.setVisible(false);
  }

  public boolean isOpen() {
    return table.isVisible();
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
}
