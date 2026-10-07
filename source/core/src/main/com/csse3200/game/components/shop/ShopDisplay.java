package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ItemGambleConfig;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scene2D implementation of Yuri's selected Gemini merchant-and-list concept. */
public class ShopDisplay extends UIComponent implements ShopView {
  private static final Color INK = Color.valueOf("2a1a08");
  private static final Color BRASS = Color.valueOf("c8963e");
  private static final String MERCHANT_ATLAS = "images/shopkeeper.atlas";
  private final InventoryComponent inventoryData;
  private final ShopCatalog catalog;
  private final ShopService service;
  private final Map<ShopOffer, TextButton> buyButtons = new LinkedHashMap<>();
  private final Map<ShopOffer, Label> ownedLabels = new LinkedHashMap<>();
  private final List<Texture> panelTextures = new ArrayList<>();
  private final List<String> loadedTextures = new ArrayList<>();
  private Table root;
  private ScrollPane productScroll;
  private ScrollPane casinoScroll;
  private CoinFlipPanel casinoPanel;
  private ItemDrawPanel itemPanel;
  private ScrollPane itemScroll;
  private Table casinoContent;
  private boolean itemGame;
  private TextButton coinSelector;
  private TextButton itemSelector;
  private TextButton casinoButton;
  private Label sectionLabel;
  private Label hint;
  private boolean casino;
  private Actor previousKeyboardFocus;
  private Actor previousScrollFocus;
  private Label goldLabel;
  private Label feedback;
  private Runnable onClose;
  private boolean active;
  private boolean disposed;
  private boolean assetsLoaded;

  public ShopDisplay(InventoryComponent inventoryData, ShopCatalog catalog, ShopService service) {
    this.inventoryData = inventoryData;
    this.catalog = catalog;
    this.service = service;
  }

  @Override
  public void create() {
    super.create();
    ResourceService resources = ServiceLocator.getResourceService();
    for (ShopOffer offer : catalog.offers()) {
      if (offer.kind() == ShopProductKind.CONSUMABLE) {
        String texture = ItemCatalog.create(offer.productId(), 1).getTexture();
        if (!loadedTextures.contains(texture)) loadedTextures.add(texture);
      }
    }
    ItemGambleConfig gambleConfig = ItemGambleConfig.load("configs/shops/merchant-gambling.json");
    for (var entry : gambleConfig.entries()) {
      if (!entry.isBust()) {
        String texture = ItemCatalog.create(entry.itemId(), 1).getTexture();
        if (!loadedTextures.contains(texture)) loadedTextures.add(texture);
      }
    }
    loadedTextures.add("images/gold_coin_pixel.png");
    resources.loadTextures(loadedTextures.toArray(new String[0]));
    resources.loadTextureAtlases(new String[] {MERCHANT_ATLAS});
    resources.loadAll();
    assetsLoaded = true;

    Drawable wood = panel(Color.valueOf("3d2314"), BRASS, 3);
    Drawable parchment = panel(Color.valueOf("f5e7c8"), Color.valueOf("804e28"), 2);
    Drawable iconFrame = panel(Color.valueOf("24140c"), BRASS, 2);
    TextButton.TextButtonStyle buyStyle = buttonStyle("2e6b2a", "214e1d");
    TextButton.TextButtonStyle closeStyle = buttonStyle("7b241a", "51150f");

    root = new Table();
    root.setName("shop-root");
    root.setFillParent(true);
    root.setTouchable(Touchable.enabled);
    root.setBackground(panel(new Color(0f, 0f, 0f, 0.72f), new Color(0f, 0f, 0f, 0.72f), 1));
    Table window = new Table();
    window.setName("shop-panel");
    window.setBackground(wood);
    window.pad(14f);

    Table header = new Table();
    Label title = label("TRAVELLING MERCHANT", BRASS);
    title.setFontScale(1.2f);
    goldLabel = label("", BRASS);
    goldLabel.setName("shop-gold");
    TextButton close = new TextButton("Leave (Esc)", closeStyle);
    close.setName("shop-close");
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && onClose != null) onClose.run();
          }
        });
    header.add(title).growX().left().padRight(12f);
    header.add(goldLabel).width(120f).padRight(12f);
    casinoButton = new TextButton("CASINO", buttonStyle("986419", "6b4110"));
    casinoButton.setName("shop-casino");
    casinoButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active) showCasino(!casino);
          }
        });
    header.add(casinoButton).width(150f).height(38f).padRight(8f);
    header.add(close).width(110f).height(38f);
    window.add(header).growX().padBottom(12f);
    window.row();

    Table merchant = new Table();
    merchant.setBackground(parchment);
    merchant.pad(16f);
    TextureAtlas atlas = resources.getAsset(MERCHANT_ATLAS, TextureAtlas.class);
    Image portrait = new Image(atlas.findRegion("default"));
    merchant.add(portrait).size(96f).padBottom(12f);
    merchant.row();
    merchant.add(label("Travelling Merchant", INK)).growX().padBottom(14f);
    merchant.row();
    merchant.add(label("Only the finest wares for brave adventurers.", INK)).growX().padBottom(18f);
    merchant.row();
    sectionLabel = label("YOUR PURCHASE", INK);
    merchant.add(sectionLabel).growX().padBottom(10f);
    merchant.row();
    feedback = label("Choose your supplies. Each purchase adds one item to your inventory.", INK);
    feedback.setName("shop-feedback");
    merchant.add(feedback).growX().top();
    merchant.row();
    merchant.add().growY();

    Table offers = new Table();
    offers.setName("shop-offers");
    offers.top();
    for (ShopOffer offer : catalog.offers()) {
      if (offer.kind() != ShopProductKind.CONSUMABLE) continue;
      Item item = ItemCatalog.create(offer.productId(), 1);
      Table row = new Table();
      row.setBackground(parchment);
      row.pad(8f);
      Table frame = new Table();
      frame.setBackground(iconFrame);
      frame.add(new Image(resources.getAsset(item.getTexture(), Texture.class))).size(42f).pad(6f);
      row.add(frame).size(58f).padRight(10f);
      Table details = new Table();
      details.add(label(item.getName(), INK)).growX().left();
      details.row();
      String description =
          item.getEffectSummary().isBlank() ? item.getDescription() : item.getEffectSummary();
      Label effect = label(description, INK);
      effect.setFontScale(0.85f);
      details.add(effect).growX().left().padTop(4f);
      details.row();
      Label owned = label("", INK);
      owned.setFontScale(0.8f);
      owned.setName("shop-owned-" + offer.offerId());
      ownedLabels.put(offer, owned);
      details.add(owned).growX().left().padTop(4f);
      row.add(details).growX().padRight(8f);
      row.add(label(offer.goldPrice() + " gold", INK)).width(72f).padRight(8f);
      TextButton buy = new TextButton("BUY", buyStyle);
      buy.setName("shop-buy-" + offer.offerId());
      buy.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (!active || casino || buy.isDisabled()) return;
              ShopPurchaseResult result = service.purchase(offer.offerId());
              feedback.setText(purchaseMessage(result, item.getName()));
              refresh();
            }
          });
      buyButtons.put(offer, buy);
      row.add(buy).width(98f).height(42f);
      offers.add(row).growX().minHeight(88f).padBottom(8f);
      offers.row();
    }
    productScroll = new ScrollPane(offers);
    productScroll.setName("shop-product-scroll");
    productScroll.setScrollingDisabled(true, false);
    productScroll.setFadeScrollBars(false);
    ScrollPane merchantScroll = new ScrollPane(merchant);
    merchantScroll.setName("shop-merchant-scroll");
    merchantScroll.setScrollingDisabled(true, false);
    merchantScroll.setFadeScrollBars(false);
    CoinFlipPanel.Appearance appearance =
        new CoinFlipPanel.Appearance(
            new Label.LabelStyle(skin.getFont("font_small"), INK),
            new Label.LabelStyle(skin.getFont("font_small"), BRASS),
            buttonStyle("3d2314", "2a1a08"),
            buyStyle,
            parchment,
            wood);
    casinoPanel =
        new CoinFlipPanel(
            new CoinFlipGame(inventoryData, MathUtils::randomBoolean),
            resources.getAsset("images/gold_coin_pixel.png", Texture.class),
            appearance,
            outcome -> {
              feedback.setText(
                  outcome.won()
                      ? "Won " + outcome.stake() + " gold."
                      : "Lost " + outcome.stake() + " gold.");
              entity.getEvents().trigger("coinFlipResolved", outcome.won(), outcome.stake());
              refresh();
            });
    casinoScroll = new ScrollPane(casinoPanel);
    casinoScroll.setScrollingDisabled(true, false);
    casinoScroll.setFadeScrollBars(false);
    casinoScroll.setVisible(false);
    itemPanel =
        new ItemDrawPanel(
            inventoryData,
            gambleConfig,
            resources,
            appearance,
            outcome -> {
              feedback.setText(
                  switch (outcome.status()) {
                    case WON -> "Reward added to your inventory.";
                    case BUST ->
                        "No prize this time. The draw cost " + gambleConfig.cost() + " gold.";
                    case INSUFFICIENT_FUNDS -> "Not enough gold. Nothing charged.";
                    case QUANTITY_LIMIT -> "Inventory limit. Nothing charged.";
                    case INVALID_REWARD -> "Reward unavailable. Nothing charged.";
                  });
              entity.getEvents().trigger("itemGambleResolved", outcome);
              refresh();
            });
    itemScroll = new ScrollPane(itemPanel);
    itemScroll.setName("item-draw-scroll");
    itemScroll.setScrollingDisabled(true, false);
    itemScroll.setFadeScrollBars(false);
    casinoContent = new Table();
    Table selectors = new Table();
    coinSelector = gameSelector("Coin Flip", "casino-select-coin", appearance.wager(), false);
    itemSelector = gameSelector("Item Draw", "casino-select-item", appearance.wager(), true);
    selectors.add(coinSelector).growX().height(38f).padRight(8f);
    selectors.add(itemSelector).growX().height(38f);
    casinoContent.add(selectors).growX().padBottom(10f);
    casinoContent.row();
    casinoContent.add(new Stack(casinoScroll, itemScroll)).minHeight(0f).grow();
    casinoContent.setVisible(false);
    itemPanel.setVisible(false);
    itemScroll.setVisible(false);
    Table body = new Table();
    body.add(merchantScroll)
        .width(Value.percentWidth(0.24f, window))
        .minHeight(0f)
        .growY()
        .padRight(16f);
    body.add(new Stack(productScroll, casinoContent)).minHeight(0f).grow();
    window.add(body).minHeight(0f).grow();
    window.row();
    hint = label("One item per purchase  |  Unlimited stock  |  Esc to leave", BRASS);
    hint.setFontScale(0.85f);
    window.add(hint).growX().padTop(10f);
    root.add(window)
        .width(Value.percentWidth(0.92f, root))
        .maxWidth(1060f)
        .height(Value.percentHeight(0.86f, root))
        .maxHeight(620f);
    root.setVisible(false);
    stage.addActor(root);
  }

  private void showCasino(boolean show) {
    stage.cancelTouchFocus();
    casino = show;
    productScroll.setVisible(!show);
    casinoContent.setVisible(show);
    selectGame(false);
    casinoButton.setText(show ? "Back to Shop" : "CASINO");
    sectionLabel.setText(show ? "COIN FLIP" : "YOUR PURCHASE");
    feedback.setText(
        show
            ? "A fair 50/50 toss. Win your stake or lose it."
            : "Choose your supplies. Each purchase adds one item to your inventory.");
    hint.setText(
        show
            ? "Back returns to shop  |  Esc to leave"
            : "One item per purchase  |  Unlimited stock  |  Esc to leave");

    stage.setKeyboardFocus(root);
    stage.setScrollFocus(show ? casinoScroll : productScroll);
    refresh();
  }

  private TextButton gameSelector(
      String text, String name, TextButton.TextButtonStyle style, boolean item) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && casino && itemGame != item) selectGame(item);
          }
        });
    return button;
  }

  private void selectGame(boolean item) {
    stage.cancelTouchFocus();
    itemGame = item;
    casinoPanel.close();
    itemPanel.close();
    casinoPanel.setVisible(casino && !item);
    casinoScroll.setVisible(casino && !item);
    itemPanel.setVisible(casino && item);
    itemScroll.setVisible(casino && item);
    coinSelector.setText(item ? "Coin Flip" : "[ Coin Flip ]");
    itemSelector.setText(item ? "[ Item Draw ]" : "Item Draw");
    if (casino) {
      if (item) itemPanel.open();
      else casinoPanel.open();
      sectionLabel.setText(item ? "ITEM DRAW" : "COIN FLIP");
      feedback.setText(
          item
              ? "Spend gold for one random item or no prize. Rewards go straight to your inventory."
              : "A fair 50/50 toss. Win your stake or lose it.");
      stage.setScrollFocus(item ? itemScroll : casinoScroll);
    }
  }

  private Label label(String text, Color color) {
    Label label = new Label(text, new Label.LabelStyle(skin.getFont("font_small"), color));
    label.setWrap(true);
    return label;
  }

  private TextButton.TextButtonStyle buttonStyle(String up, String down) {
    TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
    style.font = skin.getFont("font_small");
    style.fontColor = Color.WHITE;
    style.disabledFontColor = Color.valueOf("ddd4c2");
    style.up = panel(Color.valueOf(up), Color.valueOf("8bb56b"), 2);
    style.down = panel(Color.valueOf(down), BRASS, 2);
    style.over = style.down;
    style.disabled = panel(Color.valueOf("81786a"), Color.valueOf("a79b88"), 2);
    return style;
  }

  private Drawable panel(Color fill, Color border, int edge) {
    Pixmap pixels = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
    pixels.setColor(border);
    pixels.fill();
    pixels.setColor(fill);
    pixels.fillRectangle(edge, edge, 16 - edge * 2, 16 - edge * 2);
    Texture texture = new Texture(pixels);
    pixels.dispose();
    panelTextures.add(texture);
    return new NinePatchDrawable(new NinePatch(texture, edge, edge, edge, edge));
  }

  private static String purchaseMessage(ShopPurchaseResult result, String name) {
    return switch (result) {
      case SUCCESS -> "Purchased " + name + ". Added to your inventory.";
      case INSUFFICIENT_GOLD -> "Not enough gold.";
      case QUANTITY_LIMIT -> "You cannot carry another of this item.";
      case INVALID_OFFER, UNSUPPORTED_PRODUCT -> "This item is unavailable.";
    };
  }

  @Override
  public void show(Runnable onClose) {
    if (disposed || root == null) throw new IllegalStateException("Shop view is unavailable");
    this.onClose = onClose;
    active = true;
    feedback.setText("Choose your supplies. Each purchase adds one item to your inventory.");
    root.setVisible(true);
    root.toFront();
    previousKeyboardFocus = stage.getKeyboardFocus();
    previousScrollFocus = stage.getScrollFocus();
    stage.setKeyboardFocus(root);
    stage.setScrollFocus(productScroll);
    showCasino(false);
  }

  @Override
  public void refresh() {
    if (!active) return;
    goldLabel.setText("Gold: " + inventoryData.getGold());
    if (casino) {
      casinoPanel.refresh();
      itemPanel.refresh();
    }
    buyButtons.forEach(
        (offer, button) -> {
          boolean affordable = inventoryData.getGold() >= offer.goldPrice();
          button.setDisabled(!affordable);
          button.setText(affordable ? "BUY" : "No gold");
          ownedLabels
              .get(offer)
              .setText("Owned: " + inventoryData.getConsumableCount(offer.productId()));
        });
  }

  @Override
  public void close() {
    active = false;
    onClose = null;
    if (casinoPanel != null) casinoPanel.close();
    if (itemPanel != null) itemPanel.close();
    if (root != null) {
      // Cancel press ownership before hiding, so a later release cannot buy in a reopened shop.
      stage.cancelTouchFocus();
      root.setVisible(false);
      if (ownsFocus(stage.getKeyboardFocus())) {
        stage.setKeyboardFocus(attachedFocus(previousKeyboardFocus));
      }
      if (ownsFocus(stage.getScrollFocus())) {
        stage.setScrollFocus(attachedFocus(previousScrollFocus));
      }
      previousKeyboardFocus = null;
      previousScrollFocus = null;
    }
  }

  private Actor attachedFocus(Actor actor) {
    return actor != null && actor.getStage() == stage ? actor : null;
  }

  private boolean ownsFocus(Actor actor) {
    return actor != null && actor.isDescendantOf(root);
  }

  @Override
  public float getZIndex() {
    return 1002f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (active && root != null) root.toFront();
  }

  @Override
  public void dispose() {
    disposed = true;
    if (active && onClose != null) onClose.run();
    close();
    if (root != null) root.remove();
    for (Texture texture : panelTextures) texture.dispose();
    panelTextures.clear();
    if (assetsLoaded) {
      ResourceService resources = ServiceLocator.getResourceService();
      resources.unloadAssets(loadedTextures.toArray(new String[0]));
      resources.unloadAssets(new String[] {MERCHANT_ATLAS});
      assetsLoaded = false;
    }
    super.dispose();
  }
}
