package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.player.ConsumableEffectComponent;
import com.csse3200.game.components.player.ConsumableSelectionComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemCatalog;
import com.csse3200.game.items.ItemIds;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.Objects;

/** A compact consumable bar on the lower half of the right side of the game UI. */
public class ConsumableHotbarDisplay extends UIComponent {
  public static final String IDLE_FRAME_TEXTURE = "images/consumable-slot-idle.png";
  public static final String SELECTED_FRAME_TEXTURE = "images/consumable-slot-selected.png";
  private static final float SLOT_SIZE = 72f;
  private static final float SLOT_GAP = 6f;
  private static final float RIGHT_INSET = 24f;
  private static final float BOTTOM_FRACTION = 0.16f;
  private final Entity player;
  private final InventoryComponent inventoryComponent;
  private final ConsumableSelectionComponent selection;
  private final ConsumableEffectComponent effects;
  private final Image[] frames = new Image[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final Image[] icons = new Image[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final Label[] counts = new Label[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final Label[] pointers = new Label[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final Label[] useHints = new Label[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final SpeedPotionTimerRing[] speedRings =
      new SpeedPotionTimerRing[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private final int[] slotCounts = new int[InventoryComponent.CONSUMABLE_SLOT_COUNT];
  private Table root;
  private Label goldLabel;
  private int displayedGold;
  private Texture regularFrame;
  private Texture selectedFrame;
  private Texture ringPixel;
  private boolean disposed;

  public ConsumableHotbarDisplay(Entity player) {
    this.player = Objects.requireNonNull(player);
    inventoryComponent = Objects.requireNonNull(player.getComponent(InventoryComponent.class));
    selection = Objects.requireNonNull(player.getComponent(ConsumableSelectionComponent.class));
    effects = Objects.requireNonNull(player.getComponent(ConsumableEffectComponent.class));
  }

  @Override
  public void create() {
    super.create();
    regularFrame = ServiceLocator.getResourceService().getAsset(IDLE_FRAME_TEXTURE, Texture.class);
    selectedFrame =
        ServiceLocator.getResourceService().getAsset(SELECTED_FRAME_TEXTURE, Texture.class);
    regularFrame.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    selectedFrame.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    ringPixel = new Texture(pixmap);
    pixmap.dispose();
    buildActors();
    refreshSlots();
    player
        .getEvents()
        .addListener(ConsumableSelectionComponent.SELECTION_CHANGED, this::refreshSelection);
    player.getEvents().addListener("consumableInventoryChanged", this::refreshCount);
  }

  private void buildActors() {
    root = new Table();
    root.setName("consumable-hotbar");
    root.setFillParent(true);
    root.bottom().right().padRight(RIGHT_INSET);
    root.padBottom(
        new Value() {
          @Override
          public float get(Actor context) {
            return Math.max(180f, stage.getHeight() * BOTTOM_FRACTION);
          }
        });
    root.setTouchable(Touchable.disabled);

    Table slots = new Table();
    displayedGold = inventoryComponent.getGold();
    goldLabel = label("Gold: " + displayedGold, 0.65f);
    goldLabel.setName("consumable-gold");
    slots.add(goldLabel).colspan(3).padBottom(8f);
    slots.row();
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      String id = Integer.toString(i + 1);
      Stack slot = new Stack();
      slot.setName("consumable-slot-" + id);
      frames[i] = new Image(regularFrame);
      slot.add(frames[i]);
      speedRings[i] = new SpeedPotionTimerRing(effects, ringPixel);
      speedRings[i].setName("speed-potion-timer-ring-" + id);
      slot.add(speedRings[i]);
      icons[i] = new Image();
      icons[i].setScaling(Scaling.fit);
      icons[i].setName("consumable-icon-" + id);
      Table iconLayer = new Table();
      iconLayer.add(icons[i]).size(54f);
      slot.add(iconLayer);

      counts[i] = label("0", 0.55f);
      counts[i].setName("consumable-count-" + id);
      Table countLayer = new Table();
      countLayer.bottom().right().add(counts[i]).padRight(13f).padBottom(11f);
      slot.add(countLayer);

      pointers[i] = label(">", 0.8f);
      pointers[i].setName("consumable-pointer-" + id);
      useHints[i] = label("Q USE", 0.55f);
      useHints[i].setName("consumable-use-" + id);
      slots.add(pointers[i]).width(18f).padRight(3f);
      slots.add(slot).size(SLOT_SIZE);
      slots.add(useHints[i]).width(60f).padLeft(6f);
      slots.row().padTop(i == 0 ? 0f : SLOT_GAP);
    }
    Label cycleHint = label("TAB NEXT", 0.5f);
    cycleHint.setName("consumable-cycle-hint");
    slots.add().colspan(1);
    slots.add(cycleHint).padTop(8f);
    root.add(slots);
    stage.addActor(root);
  }

  private Label label(String text, float scale) {
    Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    style.fontColor = Color.WHITE;
    Label label = new Label(text, style);
    label.setFontScale(scale);
    return label;
  }

  /** Trim transparent padding from the source art without copying or altering its pixels. */
  private static TextureRegion iconRegion(String type, Texture texture) {
    return switch (type) {
      case ItemIds.HEALTH_POTION -> new TextureRegion(texture, 441, 266, 370, 727);
      case ItemIds.MEDIUM_HEALTH_POTION -> new TextureRegion(texture, 255, 131, 743, 1007);
      case ItemIds.LARGE_HEALTH_POTION -> new TextureRegion(texture, 163, 122, 928, 1024);
      case ItemIds.BURN_VIAL -> new TextureRegion(texture, 265, 59, 530, 1386);
      case ItemIds.SHIELD -> new TextureRegion(texture, 310, 322, 633, 653);
      case ItemIds.SPEED_POTION -> new TextureRegion(texture, 393, 220, 481, 784);
      case ItemIds.STRENGTH_POTION -> new TextureRegion(texture, 310, 173, 635, 928);
      default -> new TextureRegion(texture);
    };
  }

  private void refreshSelection(String selected) {
    if (disposed) {
      return;
    }
    for (int i = 0; i < frames.length; i++) {
      boolean active = i == selection.getSelectedIndex();
      frames[i].setDrawable(new TextureRegionDrawable(active ? selectedFrame : regularFrame));
      pointers[i].setVisible(active);
      useHints[i].setVisible(active && slotCounts[i] > 0);
    }
  }

  private void refreshCount(String type, Integer count) {
    if (disposed) {
      return;
    }
    refreshSlots();
  }

  private void refreshSlots() {
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      String type = inventoryComponent.getConsumableSlot(i);
      int count = inventoryComponent.getConsumableCount(type);
      slotCounts[i] = count;
      boolean occupied = type != null && count > 0;
      if (occupied) {
        Texture texture =
            ServiceLocator.getResourceService()
                .getAsset(ItemCatalog.create(type, 1).getTexture(), Texture.class);
        icons[i].setDrawable(new TextureRegionDrawable(iconRegion(type, texture)));
      } else {
        icons[i].setDrawable(null);
      }
      counts[i].setText(Integer.toString(count));
      counts[i].setVisible(occupied);
      icons[i].setVisible(occupied);
      speedRings[i].setVisible(ItemIds.SPEED_POTION.equals(type));
    }
    refreshSelection(selection.getSelectedType());
  }

  @Override
  public void update() {
    if (!disposed && inventoryComponent.getGold() != displayedGold) {
      displayedGold = inventoryComponent.getGold();
      goldLabel.setText("Gold: " + displayedGold);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (disposed || root == null) return;
    // Resolve late-spawned health bars before the shared stage draws, moving only our HUD.
    Actor book = stage.getRoot().findActor("inventory-book");
    if (book != null && book.isVisible() && book.getParent() == root.getParent()) {
      // Keep the open inventory above the HUD without changing the book's actor itself.
      int index = book.getZIndex();
      if (root.getZIndex() < index) index--;
      root.setZIndex(index);
    } else {
      root.toFront();
    }
  }

  @Override
  public float getZIndex() {
    return 2f;
  }

  @Override
  public void dispose() {
    disposed = true;
    if (root != null) {
      root.remove();
    }
    if (ringPixel != null) {
      ringPixel.dispose();
    }
    super.dispose();
  }
}
