package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.Color;
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
import com.csse3200.game.components.player.ConsumableSelectionComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.items.ItemType;
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
  private final Image[] frames = new Image[ConsumableSelectionComponent.SLOTS.length];
  private final Image[] icons = new Image[ConsumableSelectionComponent.SLOTS.length];
  private final Label[] counts = new Label[ConsumableSelectionComponent.SLOTS.length];
  private final Label[] pointers = new Label[ConsumableSelectionComponent.SLOTS.length];
  private final Label[] useHints = new Label[ConsumableSelectionComponent.SLOTS.length];
  private final int[] slotCounts = new int[ConsumableSelectionComponent.SLOTS.length];
  private Table root;
  private Texture regularFrame;
  private Texture selectedFrame;
  private boolean disposed;

  public ConsumableHotbarDisplay(Entity player) {
    this.player = Objects.requireNonNull(player);
    inventoryComponent = Objects.requireNonNull(player.getComponent(InventoryComponent.class));
    selection = Objects.requireNonNull(player.getComponent(ConsumableSelectionComponent.class));
  }

  @Override
  public void create() {
    super.create();
    regularFrame = ServiceLocator.getResourceService().getAsset(IDLE_FRAME_TEXTURE, Texture.class);
    selectedFrame =
        ServiceLocator.getResourceService().getAsset(SELECTED_FRAME_TEXTURE, Texture.class);
    regularFrame.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    selectedFrame.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    buildActors();
    refreshSelection(selection.getSelectedType());
    for (int i = 0; i < ConsumableSelectionComponent.SLOTS.length; i++) {
      refreshCount(
          ConsumableSelectionComponent.SLOTS[i],
          inventoryComponent.getConsumableCount(ConsumableSelectionComponent.SLOTS[i]));
    }
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
            return stage.getHeight() * BOTTOM_FRACTION;
          }
        });
    root.setTouchable(Touchable.disabled);

    Table slots = new Table();
    for (int i = 0; i < ConsumableSelectionComponent.SLOTS.length; i++) {
      ItemType type = ConsumableSelectionComponent.SLOTS[i];
      Stack slot = new Stack();
      slot.setName("consumable-slot-" + type.name().toLowerCase());
      frames[i] = new Image(regularFrame);
      slot.add(frames[i]);

      Texture texture =
          ServiceLocator.getResourceService().getAsset(type.getTexturePath(), Texture.class);
      icons[i] = new Image(new TextureRegionDrawable(iconRegion(type, texture)));
      icons[i].setScaling(Scaling.fit);
      icons[i].setName("consumable-icon-" + type.name().toLowerCase());
      Table iconLayer = new Table();
      iconLayer.add(icons[i]).size(54f);
      slot.add(iconLayer);

      counts[i] = label("0", 0.55f);
      counts[i].setName("consumable-count-" + type.name().toLowerCase());
      Table countLayer = new Table();
      countLayer.bottom().right().add(counts[i]).padRight(13f).padBottom(11f);
      slot.add(countLayer);

      pointers[i] = label(">", 0.8f);
      useHints[i] = label("Q USE", 0.55f);
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
  private static TextureRegion iconRegion(ItemType type, Texture texture) {
    return switch (type) {
      case HEALTH_POTION -> new TextureRegion(texture, 377, 325, 519, 634);
      case SHIELD -> new TextureRegion(texture, 310, 322, 633, 653);
      case SPEED_POTION -> new TextureRegion(texture, 393, 220, 481, 784);
      case STRENGTH_POTION -> new TextureRegion(texture, 310, 173, 635, 928);
      case FREEZE_BOMB -> new TextureRegion(texture);
      default -> throw new IllegalArgumentException("Not a consumable: " + type);
    };
  }

  private void refreshSelection(ItemType selected) {
    if (disposed) {
      return;
    }
    for (int i = 0; i < frames.length; i++) {
      boolean active = ConsumableSelectionComponent.SLOTS[i] == selected;
      frames[i].setDrawable(new TextureRegionDrawable(active ? selectedFrame : regularFrame));
      pointers[i].setVisible(active);
      useHints[i].setVisible(active && slotCounts[i] > 0);
    }
  }

  private void refreshCount(ItemType type, Integer count) {
    if (disposed) {
      return;
    }
    for (int i = 0; i < ConsumableSelectionComponent.SLOTS.length; i++) {
      if (ConsumableSelectionComponent.SLOTS[i] == type) {
        slotCounts[i] = count;
        counts[i].setText(Integer.toString(count));
        boolean occupied = count > 0;
        counts[i].setVisible(occupied);
        icons[i].setVisible(occupied);
        useHints[i].setVisible(occupied && selection.getSelectedType() == type);
        return;
      }
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // The shared Scene2D stage draws this actor.
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
    super.dispose();
  }
}
