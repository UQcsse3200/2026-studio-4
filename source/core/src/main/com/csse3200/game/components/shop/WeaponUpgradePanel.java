package com.csse3200.game.components.shop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.weapons.WeaponUpgradeComponent;
import com.csse3200.game.services.ResourceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Selected concept A: a weapon selector beside its before/after art and one-time upgrade. */
public final class WeaponUpgradePanel extends Table {
  private final InventoryComponent inventory;
  private final ShopCatalog catalog;
  private final ShopService service;
  private final WeaponUpgradeComponent upgrades;
  private final ResourceService resources;
  private final Consumer<ShopPurchaseResult> onResolved;
  private final Map<ShopOffer, TextButton> selectors = new LinkedHashMap<>();
  private final Map<ShopOffer, Label> states = new LinkedHashMap<>();
  private final Map<ShopOffer, Image> selectorIcons = new LinkedHashMap<>();
  private final Label name;
  private final Label status;
  private final Label heavy;
  private final Label feedback;
  private final Label cost;
  private final Image base;
  private final Image preview;
  private final TextButton buy;
  private ShopOffer selected;
  private ShopOffer lastPurchased;
  private String purchaseFailure;
  private boolean active;
  private boolean purchasing;

  /** Loaded visual assets shared by the merchant portrait, weapon previews and payment row. */
  public record Assets(
      ResourceService resources, TextureAtlas.AtlasRegion portrait, Texture coin) {}

  public WeaponUpgradePanel(
      InventoryComponent inventory,
      WeaponUpgradeCatalog upgradeCatalog,
      ShopService service,
      WeaponUpgradeComponent upgrades,
      Assets assets,
      CoinFlipPanel.Appearance appearance,
      Consumer<ShopPurchaseResult> onResolved) {
    this.inventory = inventory;
    this.catalog = upgradeCatalog.catalog();
    this.service = service;
    this.upgrades = upgrades;
    this.resources = assets.resources();
    this.onResolved = onResolved;
    selected = catalog.offers().getFirst();
    setName("weapon-upgrade-panel");
    Table left = new Table();
    left.top();
    Table merchant = new Table();
    merchant.setBackground(appearance.parchment());
    merchant.pad(10f);
    merchant.add(new Image(assets.portrait())).size(48f).padRight(8f);
    merchant.add(label("Travelling Merchant\nWeapon Upgrades", appearance.text(), .8f)).growX();
    merchant.row();
    merchant
        .add(
            label(
                "A sharper edge and a new heavy attack. Choose a weapon to upgrade.",
                appearance.text(),
                .7f))
        .colspan(2)
        .growX()
        .padTop(8f);
    left.add(merchant).growX().padBottom(10f);
    left.row();
    left.add(label("SELECT WEAPON  [1 - 3]", appearance.gold(), .75f)).growX().padBottom(8f);
    left.row();
    for (ShopOffer offer : catalog.offers()) {
      var descriptor = WeaponUpgradeCatalog.describe(offer.productId());
      TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(appearance.wager());
      style.checked = appearance.wager().over;
      TextButton selector = new TextButton(displayName(offer.productId()), style);
      selector.setName("weapon-select-" + offer.productId());
      selector.setProgrammaticChangeEvents(false);
      selector.clearChildren();
      selector.pad(7f);
      Image selectorIcon = new Image(resources.getAsset(descriptor.texture(), Texture.class));
      selectorIcon.setName("weapon-selector-icon-" + offer.productId());
      selectorIcons.put(offer, selectorIcon);
      selector.add(selectorIcon).size(32f).padRight(8f);
      Table text = new Table();
      selector.getLabel().setFontScale(.75f);
      selector.getLabel().setWrap(true);
      text.add(selector.getLabel()).growX().left();
      text.row();
      Label state = label("", appearance.gold(), .6f);
      state.setName("weapon-state-" + offer.productId());
      states.put(offer, state);
      text.add(state).growX().left().padTop(4f);
      selector.add(text).growX();
      selector.add(label(offer.goldPrice() + " G", appearance.gold(), .7f)).width(43f);
      selector.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              if (active) select(offer);
            }
          });
      selector.addListener(
          new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
              if (active && (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE)) {
                select(offer);
                return true;
              }
              return false;
            }
          });
      selectors.put(offer, selector);
      left.add(selector).growX().minHeight(58f).padBottom(7f);
      left.row();
    }
    left.add().growY();
    ScrollPane leftScroll = new ScrollPane(left);
    leftScroll.setName("weapon-upgrade-selector-scroll");
    leftScroll.setScrollingDisabled(true, false);
    leftScroll.setFadeScrollBars(false);

    Table right = new Table();
    right.setBackground(appearance.arena());
    right.pad(10f);
    Table heading = new Table();
    name = label("", appearance.gold(), 1f);
    name.setName("weapon-upgrade-name");
    status = label("", appearance.gold(), .6f);
    status.setName("weapon-upgrade-status");
    heading.add(name).growX().left();
    heading.add(status).width(160f).right();
    right.add(heading).growX().padBottom(8f);
    right.row();
    base = new Image();
    base.setName("weapon-upgrade-base");
    preview = new Image();
    preview.setName("weapon-upgrade-preview");
    Table comparison = new Table();
    comparison.setBackground(appearance.arena());
    comparison.add(spriteFrame(base, "CURRENT FORM", "Light attack [J]", appearance)).growX();
    comparison.add(label("->", appearance.gold(), 1.2f)).width(44f);
    comparison.add(spriteFrame(preview, "UPGRADED FORM", "+20% damage & [K]", appearance)).growX();
    right.add(comparison).growX().height(100f).padBottom(8f);
    right.row();
    Table techniques = new Table();
    techniques.setBackground(appearance.parchment());
    techniques.pad(10f).top();
    techniques
        .add(label("COMBAT TECHNIQUE LEDGER", appearance.text(), .7f))
        .growX()
        .left()
        .padBottom(8f);
    techniques.row();
    Label light =
        label("J  LIGHT ATTACK\n+20% damage on every light attack.", appearance.text(), .8f);
    light.setName("weapon-upgrade-light");
    techniques.add(light).growX().padBottom(10f);
    techniques.row();
    heavy = label("", appearance.text(), .8f);
    heavy.setName("weapon-upgrade-heavy");
    techniques.add(heavy).growX().padBottom(10f);
    techniques.row();
    feedback = label("", appearance.text(), .7f);
    feedback.setName("weapon-upgrade-feedback");
    techniques.add(feedback).growX();
    techniques.row();
    techniques.add().growY();
    ScrollPane techniqueScroll = new ScrollPane(techniques);
    techniqueScroll.setName("weapon-upgrade-technique-scroll");
    techniqueScroll.setScrollingDisabled(true, false);
    techniqueScroll.setFadeScrollBars(false);
    right.add(techniqueScroll).grow().minHeight(0f).padBottom(8f);
    right.row();
    Table payment = new Table();
    cost = label("", appearance.gold(), .7f);
    cost.setName("weapon-upgrade-cost");
    payment.add(new Image(assets.coin())).size(20f).padRight(7f);
    payment.add(cost).growX().padRight(8f);
    buy = new TextButton("", appearance.flip());
    buy.setName("weapon-upgrade-buy");
    buy.getLabel().setFontScale(.75f);
    buy.getLabel().setWrap(true);
    buy.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            purchase();
          }
        });
    payment.add(buy).width(Value.percentWidth(.52f, right)).height(44f);
    right.add(payment).growX().height(52f);
    add(leftScroll).width(Value.percentWidth(.28f, this)).growY().minHeight(0f).padRight(12f);
    add(right).grow().minHeight(0f);
    setVisible(false);
    refresh();
  }

  private Table spriteFrame(
      Image image, String title, String caption, CoinFlipPanel.Appearance appearance) {
    Table frame = new Table();
    frame.pad(5f);
    frame.add(label(title, appearance.gold(), .6f)).growX();
    frame.row();
    frame.add(image).size(48f).padTop(3f).padBottom(3f);
    frame.row();
    frame.add(label(caption, appearance.gold(), .6f)).growX();
    return frame;
  }

  private static Label label(String text, Label.LabelStyle style, float scale) {
    Label label = new Label(text, style);
    label.setFontScale(scale);
    label.setWrap(true);
    return label;
  }

  private static String displayName(String id) {
    return "bow".equals(id) ? "Throwing Knife" : WeaponUpgradeCatalog.describe(id).name();
  }

  private static String heavyDescription(String id) {
    return switch (id) {
      case "sword" ->
          "K  WHIRLWIND CLEAVE\n360-degree sweep at 1.35x base damage. Heavy cooldown: 2x normal.";
      case "knife" ->
          "K  FLURRY & FINISHING STAB\n"
              + "Two slashes at 0.6x base damage each, then a 1.2x finishing stab. Heavy cooldown:"
              + " 3x normal.";
      case "bow" ->
          "K  TRIPLE FAN SHOT\n"
              + "Three projectiles in a +/-15-degree spread, each at base shot damage. Heavy"
              + " cooldown: 2x normal.";
      default -> "This upgrade is unavailable.";
    };
  }

  private void select(ShopOffer offer) {
    selected = offer;
    purchaseFailure = null;
    refresh();
  }

  private void purchase() {
    if (!active || purchasing || buy.isDisabled()) return;
    purchasing = true;
    try {
      ShopOffer purchased = selected;
      ShopPurchaseResult result = service.purchase(purchased.offerId());
      showPurchaseResult(purchased, result);
    } finally {
      finishPurchase();
    }
  }

  private void showPurchaseResult(ShopOffer purchased, ShopPurchaseResult result) {
    if (result == ShopPurchaseResult.SUCCESS) lastPurchased = purchased;
    // Settlement publishes synchronous events. A listener may have closed or disposed this panel
    // and unloaded its textures before settlement returned, so recheck before refreshing the UI.
    if (!active) return;
    purchaseFailure =
        switch (result) {
          case SUCCESS, ALREADY_UPGRADED -> null;
          case INSUFFICIENT_GOLD -> "Not enough gold. Nothing charged.";
          case INVALID_OFFER, UNSUPPORTED_PRODUCT, QUANTITY_LIMIT ->
              "Upgrade unavailable. Nothing charged.";
        };
    refresh();
    onResolved.accept(result);
  }

  private void finishPurchase() {
    purchasing = false;
    if (active) refresh();
  }

  public void open() {
    purchaseFailure = null;
    active = true;
    setVisible(true);
    refresh();
  }

  public void close() {
    active = false;
    setVisible(false);
    buy.setDisabled(true);
  }

  /** Resolves current availability, gold and upgrade state without changing equipment. */
  public void refresh() {
    if (!active) {
      buy.setDisabled(true);
      return;
    }
    selectors.forEach(this::refreshSelector);
    refreshSelectedWeapon();
    refreshPayment();
  }

  private boolean isUpgraded(ShopOffer offer) {
    return upgrades != null
        && upgrades.isUpgraded(WeaponUpgradeCatalog.weaponClass(offer.productId()));
  }

  private void refreshSelector(ShopOffer offer, TextButton button) {
    boolean upgraded = isUpgraded(offer);
    states.get(offer).setText(selectorState(offer, upgraded));
    var descriptor = WeaponUpgradeCatalog.describe(offer.productId());
    String texture = upgraded ? descriptor.upgradedTexture() : descriptor.texture();
    selectorIcons
        .get(offer)
        .setDrawable(new TextureRegionDrawable(resources.getAsset(texture, Texture.class)));
    button.setChecked(offer.equals(selected));
  }

  private String selectorState(ShopOffer offer, boolean upgraded) {
    if (upgrades == null) return "UNAVAILABLE";
    if (upgraded) return "UPGRADED";
    if (inventory.getGold() < offer.goldPrice()) {
      return "NEED " + (offer.goldPrice() - inventory.getGold()) + " G";
    }
    return "READY";
  }

  private void refreshSelectedWeapon() {
    var descriptor = WeaponUpgradeCatalog.describe(selected.productId());
    name.setText(displayName(selected.productId()).toUpperCase());
    base.setDrawable(
        new TextureRegionDrawable(resources.getAsset(descriptor.texture(), Texture.class)));
    preview.setDrawable(
        new TextureRegionDrawable(resources.getAsset(descriptor.upgradedTexture(), Texture.class)));
    heavy.setText(heavyDescription(selected.productId()));
  }

  private void refreshPayment() {
    boolean upgraded = isUpgraded(selected);
    boolean affordable = inventory.getGold() >= selected.goldPrice();
    status.setText(upgradeStatus(upgraded, affordable));
    buy.setDisabled(purchasing || upgrades == null || upgraded || !affordable);
    buy.setText(purchaseCaption(upgraded, affordable));
    cost.setText("Cost: " + selected.goldPrice() + " G\nPurse: " + inventory.getGold() + " G");
    feedback.setText(purchaseFeedback(upgraded));
  }

  private String upgradeStatus(boolean upgraded, boolean affordable) {
    if (upgrades == null) return "UPGRADE UNAVAILABLE";
    if (upgraded) return "UPGRADED / K UNLOCKED";
    return affordable ? "BASE / READY" : "BASE / NEED GOLD";
  }

  private String purchaseCaption(boolean upgraded, boolean affordable) {
    if (upgrades == null) return "Upgrade unavailable";
    if (upgraded) return "Already upgraded";
    if (affordable) return "UPGRADE - " + selected.goldPrice() + " G [U]";
    return "Need " + (selected.goldPrice() - inventory.getGold()) + " more gold";
  }

  private String purchaseFeedback(boolean upgraded) {
    if (upgraded) {
      if (selected.equals(lastPurchased)) {
        return displayName(selected.productId()) + " upgraded. K heavy attack unlocked.";
      }
      return "K heavy attack unlocked. One upgrade per weapon this run.";
    }
    if (purchaseFailure != null) return purchaseFailure;
    if (upgrades == null) return "This weapon upgrade is unavailable.";
    return "One upgrade per weapon this run. Selection does not equip.";
  }

  /** Only the selected A's documented shortcuts are handled; Enter never purchases globally. */
  public boolean keyDown(int keycode) {
    if (!active) return false;
    List<ShopOffer> offers = catalog.offers();
    int index =
        switch (keycode) {
          case Input.Keys.NUM_1 -> 0;
          case Input.Keys.NUM_2 -> 1;
          case Input.Keys.NUM_3 -> 2;
          default -> -1;
        };
    if (index >= 0 && index < offers.size()) {
      select(offers.get(index));
      return true;
    }
    if (keycode == Input.Keys.U) {
      purchase();
      return true;
    }
    return false;
  }
}
