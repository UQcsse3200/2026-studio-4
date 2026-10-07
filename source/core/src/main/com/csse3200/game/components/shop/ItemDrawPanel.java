package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.shop.GambleEntry;
import com.csse3200.game.shop.GambleResult;
import com.csse3200.game.shop.GambleService;
import com.csse3200.game.shop.InventoryGambleWallet;
import com.csse3200.game.shop.ItemGambleConfig;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

/** Selected A layout: a draw arena beside the configured reward list, stacked on small screens. */
public final class ItemDrawPanel extends Table {
  private static final String GOLD_TEXTURE = "images/gold_coin_pixel.png";
  private final InventoryComponent inventory;
  private final ItemGambleConfig config;
  private final GambleService game;
  private final ResourceService resources;
  private final Consumer<GambleResult> onResolved;
  private final Table arena = new Table();
  private final Table odds = new Table();
  private final Image reward;
  private final Label result;
  private final TextButton draw;
  private boolean active;
  private boolean busy;
  private boolean narrow;

  public ItemDrawPanel(
      InventoryComponent inventory,
      ItemGambleConfig config,
      ResourceService resources,
      CoinFlipPanel.Appearance appearance,
      Consumer<GambleResult> onResolved) {
    this.inventory = inventory;
    this.config = config;
    this.resources = resources;
    this.onResolved = onResolved;
    game =
        new GambleService(
            config.table(), new InventoryGambleWallet(inventory), config.cost(), new Random());
    setName("item-draw-panel");
    setBackground(appearance.parchment());
    pad(14f);
    top();
    arena.setBackground(appearance.arena());
    arena.pad(10f);
    reward = new Image(resources.getAsset(GOLD_TEXTURE, Texture.class));
    arena.add(reward).size(64f).padBottom(8f);
    arena.row();
    result = new Label("Ready to Draw", appearance.gold());
    result.setName("item-draw-result");
    result.setWrap(true);
    result.setAlignment(Align.center);
    arena.add(result).growX().minHeight(32f).padBottom(8f);
    arena.row();
    Label cost = new Label(config.cost() + " gold per draw", appearance.gold());
    cost.setName("item-draw-cost");
    cost.setAlignment(Align.center);
    arena.add(cost).growX().padBottom(8f);
    arena.row();
    draw = new TextButton("DRAW", appearance.flip());
    draw.setName("item-draw-button");
    draw.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && !busy && !draw.isDisabled()) spin();
          }
        });
    arena.add(draw).growX().height(38f);
    odds.top();
    Label title = new Label("POSSIBLE REWARDS", appearance.text());
    title.setWrap(true);
    odds.add(title).growX().padBottom(10f);
    odds.row();
    for (GambleEntry entry : config.entries()) {
      String name =
          entry.isBust()
              ? "No prize"
              : ItemCatalog.create(entry.itemId(), 1).getName() + " x" + entry.quantity();
      Label chance =
          new Label(
              name
                  + "  |  "
                  + String.format(Locale.ROOT, "%.1f%%", config.table().probabilityOf(entry) * 100),
              appearance.text());
      chance.setName("item-draw-odds-" + (entry.isBust() ? "bust" : entry.itemId()));
      chance.setWrap(true);
      Table rewardRow = new Table();
      if (!entry.isBust()) {
        rewardRow
            .add(
                new Image(
                    resources.getAsset(
                        ItemCatalog.create(entry.itemId(), 1).getTexture(), Texture.class)))
            .size(24f)
            .padRight(8f);
      }
      rewardRow.add(chance).growX().minWidth(0f);
      odds.add(rewardRow).growX().minHeight(32f).padBottom(6f);
      odds.row();
    }
    Label rules =
        new Label(
            "One outcome per draw. No prize still costs gold. Odds are rounded.",
            appearance.text());
    rules.setWrap(true);
    odds.add(rules).growX().padTop(8f);
    arrange(false);
  }

  private void arrange(boolean stack) {
    clearChildren();
    add(arena).growX().minWidth(0f).padBottom(stack ? 12f : 0f);
    if (stack) row();
    add(odds).growX().minWidth(0f).padLeft(stack ? 0f : 14f);
    narrow = stack;
  }

  @Override
  public void layout() {
    if ((getWidth() < 520f) != narrow) arrange(getWidth() < 520f);
    super.layout();
  }

  private void spin() {
    busy = true;
    refresh();
    GambleResult outcome = game.spin();
    reward.setDrawable(new TextureRegionDrawable(resources.getAsset(GOLD_TEXTURE, Texture.class)));
    result.setText("Drawing...");
    onResolved.accept(outcome);
    if (!active) return;
    reward.addAction(Actions.sequence(Actions.delay(0.6f), Actions.run(() -> finish(outcome))));
  }

  private void finish(GambleResult outcome) {
    if (outcome.status() == GambleResult.Status.WON) {
      String id = outcome.entry().itemId();
      reward.setDrawable(
          new TextureRegionDrawable(
              resources.getAsset(ItemCatalog.create(id, 1).getTexture(), Texture.class)));
      result.setText(
          "Won " + ItemCatalog.create(id, 1).getName() + " x" + outcome.entry().quantity());
    } else {
      result.setText(
          switch (outcome.status()) {
            case BUST -> "No prize this time.";
            case INSUFFICIENT_FUNDS -> "Not enough gold. Nothing charged.";
            case QUANTITY_LIMIT -> "Inventory limit. Nothing charged.";
            default -> "Reward unavailable. Nothing charged.";
          });
    }
    busy = false;
    refresh();
  }

  public void open() {
    active = true;
    result.setText("Ready to Draw");
    reward.setDrawable(new TextureRegionDrawable(resources.getAsset(GOLD_TEXTURE, Texture.class)));
    refresh();
  }

  public void close() {
    active = false;
    busy = false;
    reward.clearActions();
    refresh();
  }

  public void refresh() {
    draw.setDisabled(!active || busy || inventory.getGold() < config.cost());
  }
}
