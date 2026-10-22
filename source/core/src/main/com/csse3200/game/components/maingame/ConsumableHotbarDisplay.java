package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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

/** Four consumable slots along the bottom of the game UI, following the weapon slots. */
public class ConsumableHotbarDisplay extends UIComponent {
  public static final String IDLE_FRAME_TEXTURE = "images/ui/inventory/consumable-slot-idle.png";
  public static final String SELECTED_FRAME_TEXTURE =
      "images/ui/inventory/consumable-slot-selected.png";
  private static final float USE_HINT_HEIGHT = 20f;
  private static final float USE_HINT_GAP = 4f;
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
  private Label cycleHint;
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
    player.getEvents().addListener(ConsumableEffectComponent.USE_FAILED, this::flashFailed);
  }

  private void buildActors() {
    root = BottomHotbarLayout.root("consumable-hotbar");
    // The hint row sits below the shared frame baseline.
    root.padBottom(BottomHotbarLayout.BOTTOM_INSET - USE_HINT_HEIGHT - USE_HINT_GAP);
    root.padLeft(
        new Value() {
          @Override
          public float get(Actor context) {
            return BottomHotbarLayout.consumableInset(BottomHotbarLayout.availableWidth(root));
          }
        });

    Table slots = new Table();
    Value diameter = BottomHotbarLayout.slotSize(root);
    Stack[] slotActors = new Stack[InventoryComponent.CONSUMABLE_SLOT_COUNT];
    displayedGold = inventoryComponent.getGold();
    goldLabel = label("Gold: " + displayedGold, 0.65f);
    goldLabel.setName("consumable-gold");
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      slotActors[i] = createSlot(i);
    }
    cycleHint = label("TAB NEXT", 0.5f);
    cycleHint.setName("consumable-cycle-hint");
    slots.add(goldLabel).colspan(2).left().padBottom(8f);
    slots.add(cycleHint).colspan(2).right().padBottom(8f);
    slots.row();
    for (int i = 0; i < slotActors.length; i++) {
      slots
          .add(pointers[i])
          .width(diameter)
          .height(14f)
          .padRight(i < slotActors.length - 1 ? BottomHotbarLayout.SLOT_GAP : 0f);
    }
    slots.row();
    for (int i = 0; i < slotActors.length; i++) {
      slots
          .add(slotActors[i])
          .size(diameter)
          .padRight(i < slotActors.length - 1 ? BottomHotbarLayout.SLOT_GAP : 0f);
    }
    slots.row();
    for (int i = 0; i < slotActors.length; i++) {
      slots
          .add(useHints[i])
          .width(diameter)
          .height(USE_HINT_HEIGHT)
          .padTop(USE_HINT_GAP)
          .padRight(i < slotActors.length - 1 ? BottomHotbarLayout.SLOT_GAP : 0f);
    }
    root.add(slots);
    stage.addActor(root);
  }

  private Stack createSlot(int index) {
    String id = Integer.toString(index + 1);
    Stack slot =
        new Stack() {
          @Override
          protected void sizeChanged() {
            super.sizeChanged();
            if (counts[index] != null) {
              float scale = getWidth() / 72f;
              counts[index].setFontScale(0.55f * scale);
              pointers[index].setFontScale(0.8f * Math.min(1f, scale));
              useHints[index].setFontScale(0.55f * Math.min(1f, scale));
              if (index == 0) {
                goldLabel.setFontScale(0.65f * Math.min(1f, scale));
                cycleHint.setFontScale(0.5f * Math.min(1f, scale));
              }
            }
          }
        };
    slot.setName("consumable-slot-" + id);
    frames[index] = new Image(regularFrame);
    slot.add(frames[index]);
    speedRings[index] = new SpeedPotionTimerRing(effects, ringPixel);
    speedRings[index].setName("speed-potion-timer-ring-" + id);
    slot.add(speedRings[index]);
    icons[index] = new Image();
    icons[index].setScaling(Scaling.fit);
    icons[index].setName("consumable-icon-" + id);
    Table iconLayer = new Table();
    iconLayer.add(icons[index]).size(Value.percentWidth(0.75f, slot));
    slot.add(iconLayer);

    counts[index] = label("0", 0.55f);
    counts[index].setName("consumable-count-" + id);
    Table countLayer = new Table();
    countLayer
        .bottom()
        .right()
        .add(counts[index])
        .padRight(Value.percentWidth(13f / 72f, slot))
        .padBottom(Value.percentHeight(11f / 72f, slot));
    slot.add(countLayer);

    pointers[index] = label("v", 0.8f);
    pointers[index].setName("consumable-pointer-" + id);
    useHints[index] = label("Q USE", 0.55f);
    useHints[index].setName("consumable-use-" + id);
    return slot;
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
      case ItemIds.BURN_VIAL -> new TextureRegion(texture, 342, 101, 436, 1345);
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

  /** Brief red flash on a slot whose use attempt was rejected, e.g. already at full health. */
  private void flashFailed(String type) {
    if (disposed) {
      return;
    }
    for (int i = 0; i < InventoryComponent.CONSUMABLE_SLOT_COUNT; i++) {
      if (type.equals(inventoryComponent.getConsumableSlot(i))) {
        frames[i].clearActions();
        frames[i].setColor(Color.WHITE);
        frames[i].addAction(
            Actions.sequence(
                Actions.color(new Color(1f, 0.35f, 0.35f, 1f), 0.08f),
                Actions.color(Color.WHITE, 0.25f)));
      }
    }
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
