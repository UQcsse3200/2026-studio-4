package com.csse3200.game.components.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.shop.ItemGambleConfig;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Scene2D implementation of Yuri's selected Gemini merchant-and-list concept. */
public class ShopDisplay extends UIComponent implements ShopView {
  private static final Color INK = Color.valueOf("2a1a08");
  private static final Color BRASS = Color.valueOf("c8963e");
  private static final String MERCHANT_ATLAS = "images/friendly-npcs/shopkeeper.atlas";
  private static final String GOLD_TEXTURE = "images/items/currency/gold_coin_pixel.png";
  private static final String SMALL_FONT = "font_small";
  private static final String CASINO_LABEL = "CASINO";
  private static final String SUPPLIES_HELP =
      "Choose your supplies. Each purchase adds one item to your inventory.";
  private static final String GOLD_SUFFIX = " gold.";
  private static final String LOST_PREFIX = "Lost ";
  private final InventoryComponent inventoryData;
  private final ShopCatalog catalog;
  private final ShopService service;
  private final Map<ShopOffer, TextButton> buyButtons = new LinkedHashMap<>();
  private final Map<ShopOffer, Label> ownedLabels = new LinkedHashMap<>();
  private final List<Texture> panelTextures = new ArrayList<>();
  private final List<String> loadedTextures = new ArrayList<>();
  private final WeaponUpgradeCatalog upgradeCatalog;
  private final ShopService upgradeService;
  private final WeaponUpgradeComponent upgrades;
  private WeaponUpgradePanel weaponPanel;
  private TextButton weaponsButton;
  private Table shopBody;
  private Label title;
  private boolean weapons;
  private Table root;
  private ScrollPane productScroll;
  private ScrollPane casinoScroll;
  private Table casinoContent;
  private final Map<CasinoGame, CasinoView> casinoGames = new EnumMap<>(CasinoGame.class);
  private CasinoGame selectedGame = CasinoGame.COIN_FLIP;
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
    this(inventoryData, catalog, service, null, null, null);
  }

  public ShopDisplay(
      InventoryComponent inventoryData,
      ShopCatalog catalog,
      ShopService service,
      WeaponUpgradeCatalog upgradeCatalog,
      ShopService upgradeService,
      WeaponUpgradeComponent upgrades) {
    this.upgradeCatalog = upgradeCatalog;
    this.upgradeService = upgradeService;
    this.upgrades = upgrades;
    this.inventoryData = inventoryData;
    this.catalog = catalog;
    this.service = service;
  }

  @Override
  public void create() {
    super.create();
    ResourceService resources = ServiceLocator.getResourceService();
    ItemGambleConfig gambleConfig = ItemGambleConfig.load("configs/shops/merchant-gambling.json");
    loadAssets(resources, gambleConfig);

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
    window.add(createHeader(window, closeStyle)).growX().padBottom(12f);
    window.row();

    ScrollPane merchantScroll = createMerchant(resources, parchment);
    productScroll = createOffers(resources, parchment, iconFrame, buyStyle);
    CoinFlipPanel.Appearance appearance =
        new CoinFlipPanel.Appearance(
            new Label.LabelStyle(skin.getFont(SMALL_FONT), INK),
            new Label.LabelStyle(skin.getFont(SMALL_FONT), BRASS),
            buttonStyle("3d2314", "2a1a08"),
            buyStyle,
            parchment,
            wood);
    createCasinoPanels(resources, gambleConfig, appearance);
    Table body = new Table();
    body.add(merchantScroll)
        .width(Value.percentWidth(0.24f, window))
        .minHeight(0f)
        .growY()
        .padRight(16f);
    body.add(new Stack(productScroll, casinoContent)).minHeight(0f).grow();
    shopBody = body;
    addBody(window, body, resources, appearance);
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

  private void loadAssets(ResourceService resources, ItemGambleConfig gambleConfig) {
    for (ShopOffer offer : catalog.offers()) {
      if (offer.kind() == ShopProductKind.CONSUMABLE || offer.kind() == ShopProductKind.CHARM) {
        addTexture(ItemCatalog.create(offer.productId(), 1).getTexture());
      }
    }
    for (var entry : gambleConfig.entries()) {
      if (!entry.isBust()) addTexture(ItemCatalog.create(entry.itemId(), 1).getTexture());
    }
    collectUpgradeTextures();
    loadedTextures.add(GOLD_TEXTURE);
    resources.loadTextures(loadedTextures.toArray(new String[0]));
    resources.loadTextureAtlases(new String[] {MERCHANT_ATLAS});
    resources.loadAll();
    assetsLoaded = true;
  }

  private void collectUpgradeTextures() {
    if (upgradeCatalog == null) return;
    for (ShopOffer offer : upgradeCatalog.offers()) {
      var descriptor = WeaponUpgradeCatalog.describe(offer.productId());
      addTexture(descriptor.texture());
      addTexture(descriptor.upgradedTexture());
    }
  }

  private void addTexture(String texture) {
    if (!loadedTextures.contains(texture)) loadedTextures.add(texture);
  }

  private Table createHeader(Table window, TextButton.TextButtonStyle closeStyle) {
    Table header = new Table();
    title = label("TRAVELLING MERCHANT", BRASS);
    title.setFontScale(1.2f);
    goldLabel = label("", BRASS);
    goldLabel.setName("shop-gold");
    TextButton close = new TextButton("Leave (Esc)", closeStyle);
    close.getLabel().setFontScale(.75f);
    close.getLabel().setWrap(true);
    close.setName("shop-close");
    close.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && onClose != null) onClose.run();
          }
        });
    header.add(title).growX().left().padRight(12f);
    header
        .add(goldLabel)
        .width(Value.percentWidth(.12f, window))
        .minWidth(64f)
        .maxWidth(120f)
        .padRight(12f);
    casinoButton = new TextButton(CASINO_LABEL, buttonStyle("986419", "6b4110"));
    casinoButton.getLabel().setFontScale(.75f);
    casinoButton.getLabel().setWrap(true);
    casinoButton.setName("shop-casino");
    casinoButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active) showCasino(!casino);
          }
        });
    header
        .add(casinoButton)
        .width(Value.percentWidth(.13f, window))
        .minWidth(80f)
        .maxWidth(150f)
        .height(38f)
        .padRight(8f);
    if (upgradeCatalog != null) {
      weaponsButton = new TextButton("WEAPONS", buttonStyle("986419", "6b4110"));
      weaponsButton.getLabel().setFontScale(.75f);
      weaponsButton.getLabel().setWrap(true);
      weaponsButton.setName("shop-weapons");
      weaponsButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (active) showWeapons(!weapons);
            }
          });
      header
          .add(weaponsButton)
          .width(Value.percentWidth(.13f, window))
          .minWidth(80f)
          .maxWidth(130f)
          .height(38f)
          .padRight(8f);
    }
    header.add(close).width(110f).height(38f);
    return header;
  }

  private ScrollPane createMerchant(ResourceService resources, Drawable parchment) {
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
    feedback = label(SUPPLIES_HELP, INK);
    feedback.setName("shop-feedback");
    merchant.add(feedback).growX().top();
    merchant.row();
    merchant.add().growY();

    ScrollPane merchantScroll = new ScrollPane(merchant);
    merchantScroll.setName("shop-merchant-scroll");
    merchantScroll.setScrollingDisabled(true, false);
    merchantScroll.setFadeScrollBars(false);
    return merchantScroll;
  }

  private ScrollPane createOffers(
      ResourceService resources,
      Drawable parchment,
      Drawable iconFrame,
      TextButton.TextButtonStyle buyStyle) {
    Table offers = new Table();
    offers.setName("shop-offers");
    offers.top();
    for (ShopOffer offer : catalog.offers()) {
      if (offer.kind() != ShopProductKind.CONSUMABLE && offer.kind() != ShopProductKind.CHARM)
        continue;
      offers
          .add(createOfferRow(offer, resources, parchment, iconFrame, buyStyle))
          .growX()
          .minHeight(88f)
          .padBottom(8f);
      offers.row();
    }
    ScrollPane scroll = new ScrollPane(offers);
    scroll.setName("shop-product-scroll");
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);
    return scroll;
  }

  private Table createOfferRow(
      ShopOffer offer,
      ResourceService resources,
      Drawable parchment,
      Drawable iconFrame,
      TextButton.TextButtonStyle buyStyle) {
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
            if (!active || casino || weapons || buy.isDisabled()) return;
            ShopPurchaseResult result = service.purchase(offer.offerId());
            feedback.setText(purchaseMessage(result, item.getName()));
            refresh();
          }
        });
    buyButtons.put(offer, buy);
    row.add(buy).width(98f).height(42f);
    return row;
  }

  private void createCasinoPanels(
      ResourceService resources,
      ItemGambleConfig gambleConfig,
      CoinFlipPanel.Appearance appearance) {
    CoinFlipPanel casinoPanel =
        new CoinFlipPanel(
            new CoinFlipGame(inventoryData, MathUtils::randomBoolean),
            resources.getAsset(GOLD_TEXTURE, Texture.class),
            appearance,
            outcome -> {
              feedback.setText(
                  outcome.won()
                      ? "Won " + outcome.stake() + GOLD_SUFFIX
                      : LOST_PREFIX + outcome.stake() + GOLD_SUFFIX);
              entity.getEvents().trigger("coinFlipResolved", outcome.won(), outcome.stake());
              refresh();
            });
    casinoScroll = new ScrollPane(casinoPanel);
    casinoScroll.setScrollingDisabled(true, false);
    casinoScroll.setFadeScrollBars(false);
    casinoScroll.setVisible(false);
    ItemDrawPanel itemPanel =
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
                        "No prize this time. The draw cost " + gambleConfig.cost() + GOLD_SUFFIX;
                    case INSUFFICIENT_FUNDS -> "Not enough gold. Nothing charged.";
                    case QUANTITY_LIMIT -> "Inventory limit. Nothing charged.";
                    case INVALID_REWARD -> "Reward unavailable. Nothing charged.";
                  });
              entity.getEvents().trigger("itemGambleResolved", outcome);
              refresh();
            });
    ScrollPane itemScroll = new ScrollPane(itemPanel);
    itemScroll.setName("item-draw-scroll");
    itemScroll.setScrollingDisabled(true, false);
    itemScroll.setFadeScrollBars(false);
    registerGame(
        CasinoGame.COIN_FLIP,
        casinoPanel,
        casinoScroll,
        casinoPanel::open,
        casinoPanel::close,
        casinoPanel::refresh,
        appearance);
    registerGame(
        CasinoGame.ITEM_DRAW,
        itemPanel,
        itemScroll,
        itemPanel::open,
        itemPanel::close,
        itemPanel::refresh,
        appearance);
    createMinigamePanels(appearance);
    createGuessingGamePanels(appearance);
    createSlotMachinePanel(appearance);
    casinoContent = new Table();
    Table selectorRow = null;
    CasinoGame[] gamesInOrder = CasinoGame.values();
    for (int index = 0; index < gamesInOrder.length; index++) {
      if (index % 3 == 0) {
        selectorRow = new Table();
        casinoContent.add(selectorRow).growX().padBottom(6f);
        casinoContent.row();
      }
      boolean lastInRow = index % 3 == 2 || index == gamesInOrder.length - 1;
      selectorRow
          .add(casinoGames.get(gamesInOrder[index]).selector())
          .growX()
          .uniformX()
          .height(38f)
          .padRight(lastInRow ? 0f : 8f);
    }
    Stack games = new Stack();
    for (CasinoView view : casinoGames.values()) {
      games.add(view.scroll());
      view.panel().setVisible(false);
      view.scroll().setVisible(false);
    }
    casinoContent.add(games).minHeight(0f).grow();
    casinoContent.setVisible(false);
  }

  /** Creates Dice Duel, Rock-Paper-Scissors and Scratch Card panels. */
  private void createMinigamePanels(CoinFlipPanel.Appearance appearance) {
    DiceDuelPanel dicePanel =
        new DiceDuelPanel(
            new DiceDuelGame(inventoryData, () -> MathUtils.random(1, 6)),
            appearance,
            outcome -> {
              feedback.setText(
                  switch (outcome.result()) {
                    case WIN -> "Won " + (outcome.stake() + outcome.bonus()) + GOLD_SUFFIX;
                    case TIE -> "Tie. Your stake was returned.";
                    case LOSE -> LOST_PREFIX + outcome.stake() + GOLD_SUFFIX;
                  });
              entity.getEvents().trigger("diceDuelResolved", outcome);
              refresh();
            });
    registerGame(
        CasinoGame.DICE_DUEL,
        dicePanel,
        gameScroll(dicePanel, "dice-duel-scroll"),
        dicePanel::open,
        dicePanel::close,
        dicePanel::refresh,
        appearance);

    RockPaperScissorsPanel rpsPanel =
        new RockPaperScissorsPanel(
            new RockPaperScissorsGame(inventoryData, () -> MathUtils.random(2)),
            appearance,
            outcome -> {
              feedback.setText(
                  switch (outcome.result()) {
                    case WIN -> "Won " + outcome.stake() + GOLD_SUFFIX;
                    case TIE -> "Drawn match. Your stake was returned.";
                    case LOSE -> LOST_PREFIX + outcome.stake() + GOLD_SUFFIX;
                  });
              entity.getEvents().trigger("rockPaperScissorsResolved", outcome);
              refresh();
            });
    registerGame(
        CasinoGame.ROCK_PAPER_SCISSORS,
        rpsPanel,
        gameScroll(rpsPanel, "rps-scroll"),
        rpsPanel::open,
        rpsPanel::close,
        rpsPanel::refresh,
        appearance);

    ScratchCardPanel scratchPanel =
        new ScratchCardPanel(
            new ScratchCardGame(
                inventoryData, ScratchCardGame.weightedPicks(RandomGenerator.getDefault())),
            appearance,
            outcome -> {
              feedback.setText(
                  outcome.payout() > 0
                      ? "Card paid " + outcome.payout() + GOLD_SUFFIX
                      : "No match. The card cost " + outcome.stake() + GOLD_SUFFIX);
              entity.getEvents().trigger("scratchCardResolved", outcome);
              refresh();
            });
    registerGame(
        CasinoGame.SCRATCH_CARD,
        scratchPanel,
        gameScroll(scratchPanel, "scratch-card-scroll"),
        scratchPanel::open,
        scratchPanel::close,
        scratchPanel::refresh,
        appearance);
  }

  /** Adds Higher/Lower and Shell Game to the shared casino selector. */
  private void createGuessingGamePanels(CoinFlipPanel.Appearance appearance) {
    HigherLowerPanel higherLowerPanel =
        new HigherLowerPanel(
            new HigherLowerGame(inventoryData, () -> MathUtils.random(1, 13)),
            appearance,
            round -> {
              feedback.setText(
                  switch (round.outcome()) {
                    case ACTIVE -> "New round dealt.";
                    case CORRECT -> "Correct guess! Streak " + round.streak() + ".";
                    case PUSH -> "Push - same rank, redraw.";
                    case BUST -> "Wrong guess. Stake lost.";
                    case CASHED_OUT -> "Cashed out " + round.payout() + GOLD_SUFFIX;
                  });
              entity.getEvents().trigger("higherLowerResolved", round);
              refresh();
            });
    registerGame(
        CasinoGame.HIGHER_LOWER,
        higherLowerPanel,
        gameScroll(higherLowerPanel, "higher-lower-scroll"),
        higherLowerPanel::open,
        higherLowerPanel::close,
        higherLowerPanel::refresh,
        appearance);
    ShellGamePanel shellGamePanel =
        new ShellGamePanel(
            new ShellGame(inventoryData, () -> MathUtils.random(0, ShellGame.CUP_COUNT - 1)),
            appearance,
            round -> {
              feedback.setText(
                  round.won()
                      ? "Found it! Won " + round.payout() + GOLD_SUFFIX
                      : "Wrong cup. Lost your stake.");
              entity.getEvents().trigger("shellGameResolved", round);
              refresh();
            });
    registerGame(
        CasinoGame.SHELL_GAME,
        shellGamePanel,
        gameScroll(shellGamePanel, "shell-game-scroll"),
        shellGamePanel::open,
        shellGamePanel::close,
        shellGamePanel::refresh,
        appearance);
  }

  /** Adds Slot Machine to the shared casino selector. */
  private void createSlotMachinePanel(CoinFlipPanel.Appearance appearance) {
    SlotMachinePanel slotPanel =
        new SlotMachinePanel(
            new SlotMachineGame(
                inventoryData, () -> MathUtils.random(SlotMachineGame.Symbol.values().length - 1)),
            appearance,
            outcome -> {
              feedback.setText(
                  outcome.won()
                      ? "Jackpot! Won " + outcome.goldDelta() + GOLD_SUFFIX
                      : LOST_PREFIX + Math.abs(outcome.goldDelta()) + GOLD_SUFFIX);
              entity.getEvents().trigger("slotMachineResolved", outcome);
              refresh();
            });
    registerGame(
        CasinoGame.SLOT_MACHINE,
        slotPanel,
        gameScroll(slotPanel, "slot-machine-scroll"),
        slotPanel::open,
        slotPanel::close,
        slotPanel::refresh,
        appearance);
  }

  private static ScrollPane gameScroll(Table panel, String name) {
    ScrollPane scroll = new ScrollPane(panel);
    scroll.setName(name);
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);
    return scroll;
  }

  private void registerGame(
      CasinoGame game,
      Table panel,
      ScrollPane scroll,
      Runnable open,
      Runnable close,
      Runnable refresh,
      CoinFlipPanel.Appearance appearance) {
    TextButton selector = gameSelector(game.label, game.selectorName, appearance.wager(), game);
    selector.getLabel().setWrap(true);
    casinoGames.put(game, new CasinoView(panel, scroll, selector, open, close, refresh));
  }

  /** The games offered in the merchant's casino, in selector order. */
  private enum CasinoGame {
    COIN_FLIP(
        "Coin Flip",
        "casino-select-coin",
        "COIN FLIP",
        "A fair 50/50 toss. Win your stake or lose it."),
    ITEM_DRAW(
        "Item Draw",
        "casino-select-item",
        "ITEM DRAW",
        "Spend gold for one random item or no prize. Rewards go straight to your inventory."),
    DICE_DUEL(
        "Dice Duel",
        "casino-select-dice",
        "DICE DUEL",
        "Roll two dice against the merchant. Higher total wins; a tie returns your stake."),
    ROCK_PAPER_SCISSORS(
        "Rock Paper Scissors",
        "casino-select-rps",
        "ROCK PAPER SCISSORS",
        "Best of three against the merchant. A drawn match returns your stake."),
    SCRATCH_CARD(
        "Scratch Card",
        "casino-select-scratch",
        "SCRATCH CARD",
        "Scratch three spots. Three of a kind wins; a pair returns half your stake."),
    SLOT_MACHINE(
        "Slot Machine",
        "casino-select-slot",
        "SLOT MACHINE",
        "Spin three reels. Three matching symbols pay out the symbol's multiplier on"
            + " your stake."),
    HIGHER_LOWER(
        "Higher/Lower",
        "casino-select-higher-lower",
        "HIGHER / LOWER",
        "Guess right to grow your multiplier. Cash out any time."),
    SHELL_GAME(
        "Shell Game",
        "casino-select-shell-game",
        "SHELL GAME",
        "Pick the cup hiding the ball to double your stake.");

    private final String label;
    private final String selectorName;
    private final String section;
    private final String help;

    CasinoGame(String label, String selectorName, String section, String help) {
      this.label = label;
      this.selectorName = selectorName;
      this.section = section;
      this.help = help;
    }
  }

  /** One casino game's panel, scroll area, selector button and lifecycle callbacks. */
  private record CasinoView(
      Table panel,
      ScrollPane scroll,
      TextButton selector,
      Runnable open,
      Runnable close,
      Runnable refresh) {}

  private void addBody(
      Table window, Table body, ResourceService resources, CoinFlipPanel.Appearance appearance) {
    if (upgradeCatalog != null) {
      TextureAtlas atlas = resources.getAsset(MERCHANT_ATLAS, TextureAtlas.class);
      weaponPanel =
          new WeaponUpgradePanel(
              inventoryData,
              upgradeCatalog,
              upgradeService,
              upgrades,
              new WeaponUpgradePanel.Assets(
                  resources,
                  atlas.findRegion("default"),
                  resources.getAsset(GOLD_TEXTURE, Texture.class)),
              appearance,
              result -> {
                entity.getEvents().trigger("weaponUpgradePurchaseResolved", result);
                refresh();
              });
      window.add(new Stack(body, weaponPanel)).minHeight(0f).grow();
      root.addListener(
          new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
              return active && weapons && weaponPanel.keyDown(keycode);
            }
          });
    } else {
      window.add(body).minHeight(0f).grow();
    }
  }

  private void showCasino(boolean show) {
    stage.cancelTouchFocus();
    weapons = false;
    if (weaponPanel != null) weaponPanel.close();
    if (weaponsButton != null) weaponsButton.setText("WEAPONS");
    shopBody.setVisible(true);
    title.setText("TRAVELLING MERCHANT");
    casino = show;
    productScroll.setVisible(!show);
    casinoContent.setVisible(show);
    selectGame(CasinoGame.COIN_FLIP);
    casinoButton.setText(show ? "Back to Shop" : CASINO_LABEL);
    sectionLabel.setText(show ? CasinoGame.COIN_FLIP.section : "YOUR PURCHASE");
    feedback.setText(show ? CasinoGame.COIN_FLIP.help : SUPPLIES_HELP);
    hint.setText(
        show
            ? "Back returns to shop  |  Esc to leave"
            : "One item per purchase  |  Unlimited stock  |  Esc to leave");

    stage.setKeyboardFocus(root);
    stage.setScrollFocus(show ? casinoScroll : productScroll);
    refresh();
  }

  private void showWeapons(boolean show) {
    if (!show) {
      showCasino(false);
      return;
    }
    stage.cancelTouchFocus();
    weapons = true;
    casino = false;
    casinoGames.values().forEach(view -> view.close().run());
    productScroll.setVisible(false);
    casinoContent.setVisible(false);
    shopBody.setVisible(false);
    casinoButton.setText(CASINO_LABEL);
    weaponsButton.setText("Back to Shop");
    title.setText("WEAPON UPGRADES");
    weaponPanel.open();
    hint.setText("One upgrade per weapon this run  |  Selection does not equip  |  Esc to leave");
    stage.setKeyboardFocus(root);
    stage.setScrollFocus(weaponPanel);
    refresh();
  }

  private TextButton gameSelector(
      String text, String name, TextButton.TextButtonStyle style, CasinoGame game) {
    TextButton button = new TextButton(text, style);
    button.setName(name);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (active && casino && selectedGame != game) selectGame(game);
          }
        });
    return button;
  }

  private void selectGame(CasinoGame game) {
    stage.cancelTouchFocus();
    selectedGame = game;
    casinoGames.values().forEach(view -> view.close().run());
    casinoGames.forEach(
        (option, view) -> {
          boolean shown = casino && option == game;
          view.panel().setVisible(shown);
          view.scroll().setVisible(shown);
          view.selector().setText(option == game ? "[ " + option.label + " ]" : option.label);
        });
    if (!casino) return;
    CasinoView selected = casinoGames.get(game);
    selected.open().run();
    sectionLabel.setText(game.section);
    feedback.setText(game.help);
    stage.setScrollFocus(selected.scroll());
  }

  private Label label(String text, Color color) {
    Label label = new Label(text, new Label.LabelStyle(skin.getFont(SMALL_FONT), color));
    label.setWrap(true);
    return label;
  }

  private TextButton.TextButtonStyle buttonStyle(String up, String down) {
    TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
    style.font = skin.getFont(SMALL_FONT);
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
      case ALREADY_UPGRADED -> "This weapon is already upgraded.";
      case INVALID_OFFER, UNSUPPORTED_PRODUCT -> "This item is unavailable.";
    };
  }

  @Override
  public void show(Runnable onClose) {
    if (disposed || root == null) throw new IllegalStateException("Shop view is unavailable");
    this.onClose = onClose;
    active = true;
    feedback.setText(SUPPLIES_HELP);
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
    if (weaponPanel != null) weaponPanel.refresh();
    if (casino) {
      casinoGames.values().forEach(view -> view.refresh().run());
    }
    buyButtons.forEach(
        (offer, button) -> {
          boolean affordable = inventoryData.getGold() >= offer.goldPrice();
          button.setDisabled(!affordable);
          button.setText(affordable ? "BUY" : "No gold");
          ownedLabels.get(offer).setText("Owned: " + ownedCount(offer));
        });
  }

  private long ownedCount(ShopOffer offer) {
    if (offer.kind() == ShopProductKind.CHARM) {
      return inventoryData.getCharms().stream()
          .filter(charm -> charm.getId().equals(offer.productId()))
          .count();
    }
    return inventoryData.getConsumableCount(offer.productId());
  }

  @Override
  public void close() {
    active = false;
    onClose = null;
    if (weaponPanel != null) weaponPanel.close();
    casinoGames.values().forEach(view -> view.close().run());
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
