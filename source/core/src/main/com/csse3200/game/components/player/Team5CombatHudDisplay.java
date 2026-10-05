package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Team 5's compact fixed-key consumable HUD, separate from the shared health HUD. */
public class Team5CombatHudDisplay extends UIComponent {
  private static final String WHITE_DRAWABLE = "white";

  /** Fixed consumable presentation order, matching keyboard quick-use slots. */
  public enum ConsumableSlot {
    HEALTH("Health", "7", ItemType.HEALTH_POTION),
    SHIELD("Shield", "8", ItemType.SHIELD),
    SPEED("Speed", "9", ItemType.SPEED_POTION),
    STRENGTH("Strength", "0", ItemType.STRENGTH_POTION);

    private final String displayName;
    private final String key;
    private final ItemType itemType;

    ConsumableSlot(String displayName, String key, ItemType itemType) {
      this.displayName = displayName;
      this.key = key;
      this.itemType = itemType;
    }

    static ConsumableSlot fromItemType(ItemType type) {
      for (ConsumableSlot slot : values()) {
        if (slot.itemType == type) {
          return slot;
        }
      }
      return null;
    }
  }

  private final Map<ConsumableSlot, Label> quantityLabels = new EnumMap<>(ConsumableSlot.class);
  private final List<Texture> iconTextures = new ArrayList<>();
  private Table table;
  private Label goldLabel;
  private final Map<ConsumableSlot, Label> effectTimes = new EnumMap<>(ConsumableSlot.class);
  private final Map<ConsumableSlot, ProgressBar> effectProgress =
      new EnumMap<>(ConsumableSlot.class);
  private int displayedGold;
  private boolean disposed;

  @Override
  public void create() {
    super.create();
    addActors();
    registerEventListeners();
    updateEffectProgress();
  }

  void registerEventListeners() {
    entity
        .getEvents()
        .addListener("consumableInventoryChanged", this::onConsumableInventoryChanged);
    entity
        .getEvents()
        .addListener(ConsumableEffectComponent.USED, (ItemType type) -> updateEffectProgress());
  }

  private void addActors() {
    table = new Table();
    table.center().right();
    table.setFillParent(true);
    table.padTop(70f).padRight(20f);
    table.setName("team5-consumables");
    table.setTouchable(Touchable.disabled);
    Label.LabelStyle style = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    style.fontColor = new Color(0.83f, 0.86f, 0.89f, 1f);
    Label.LabelStyle muted = new Label.LabelStyle(style);
    muted.fontColor = new Color(0.59f, 0.65f, 0.70f, 1f);
    Table panel = new Table();
    panel.setBackground(skin.newDrawable(WHITE_DRAWABLE, new Color(0.06f, 0.08f, 0.10f, 0.86f)));
    panel.pad(12f);
    table.add(panel).width(270f);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    displayedGold = inventory == null ? 0 : inventory.getGold();
    goldLabel = new Label(formatGold(displayedGold), style);
    panel.add(goldLabel).colspan(2).left().padBottom(8f);
    for (ConsumableSlot slot : ConsumableSlot.values()) {
      panel.row();
      Texture texture = new Texture(Gdx.files.internal(slot.itemType.getTexturePath()));
      iconTextures.add(texture);
      Image icon = new Image(texture);
      icon.setScaling(Scaling.fit);
      icon.setName("consumable-icon-" + slot.name());
      panel.add(icon).size(28f).padRight(8f).padBottom(4f);
      int count = inventory == null ? 0 : inventory.getConsumableCount(slot.itemType);
      Label label = new Label(formatSlot(slot, count), style);
      quantityLabels.put(slot, label);
      panel.add(label).left().expandX().padBottom(4f);
      if (slot != ConsumableSlot.HEALTH) {
        panel.row();
        ProgressBar.ProgressBarStyle barStyle = new ProgressBar.ProgressBarStyle();
        barStyle.background = skin.newDrawable(WHITE_DRAWABLE, new Color(0.18f, 0.23f, 0.27f, 1f));
        barStyle.background.setMinHeight(6f);
        barStyle.knobBefore = skin.newDrawable(WHITE_DRAWABLE, new Color(0.36f, 0.72f, 0.84f, 1f));
        barStyle.knobBefore.setMinWidth(0f);
        barStyle.knobBefore.setMinHeight(6f);
        ProgressBar progress = new ProgressBar(0f, 1f, 0.001f, false, barStyle);
        String effectName = slot.name().toLowerCase(Locale.ROOT);
        progress.setName("consumable-" + effectName + "-progress");
        effectProgress.put(slot, progress);
        Table effectStatus = new Table();
        effectStatus.add(progress).width(142f).height(8f).padRight(8f);
        Label remainingTime = new Label("--", muted);
        remainingTime.setName("consumable-" + effectName + "-time");
        effectTimes.put(slot, remainingTime);
        effectStatus.add(remainingTime).left();
        panel.add().width(28f);
        panel.add(effectStatus).left().padBottom(8f);
      }
    }
    stage.addActor(table);
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory != null && inventory.getGold() != displayedGold) {
      updateGold(inventory.getGold());
    }
    updateEffectProgress();
  }

  private void updateEffectProgress() {
    if (disposed) {
      return;
    }
    ConsumableEffectComponent effects = entity.getComponent(ConsumableEffectComponent.class);
    for (Map.Entry<ConsumableSlot, ProgressBar> entry : effectProgress.entrySet()) {
      long remaining = effects == null ? 0 : effects.getRemainingMs(entry.getKey().itemType);
      entry.getValue().setValue((float) remaining / ConsumableEffectComponent.DURATION_MS);
      effectTimes
          .get(entry.getKey())
          .setText(remaining > 0 ? String.format(Locale.ROOT, "%.1fs", remaining / 1000f) : "--");
    }
  }

  /** Updates currency from the real inventory. */
  public void updateGold(int gold) {
    displayedGold = Math.max(gold, 0);
    if (goldLabel != null) {
      goldLabel.setText(formatGold(displayedGold));
    }
  }

  /** Updates one consumable's stock without affecting its timed effect. */
  public void updateConsumableCount(ConsumableSlot slot, int count) {
    Label label = quantityLabels.get(slot);
    if (!disposed && label != null) {
      label.setText(formatSlot(slot, count));
    }
  }

  private void onConsumableInventoryChanged(ItemType type, int count) {
    ConsumableSlot slot = ConsumableSlot.fromItemType(type);
    if (slot != null) {
      updateConsumableCount(slot, count);
    }
  }

  static String formatGold(int gold) {
    return String.format("Gold: %d", Math.max(gold, 0));
  }

  static String formatSlot(ConsumableSlot slot, int count) {
    return String.format("[%s] %s x%d", slot.key, slot.displayName, Math.max(count, 0));
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawn by the stage.
  }

  @Override
  public void dispose() {
    disposed = true;
    super.dispose();
    if (table != null) {
      table.remove();
    }
    for (Texture texture : iconTextures) {
      texture.dispose();
    }
    iconTextures.clear();
  }
}
