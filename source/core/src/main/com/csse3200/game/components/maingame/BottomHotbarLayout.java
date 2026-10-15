package com.csse3200.game.components.maingame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.csse3200.game.components.player.InventoryComponent;

/** Shared geometry for the three weapon slots followed by four consumable slots. */
final class BottomHotbarLayout {
  static final float BOTTOM_INSET = 48f;
  private static final float LEFT_INSET = 340f;
  static final float SLOT_GAP = 6f;
  private static final float RIGHT_INSET = 96f;
  private static final float GROUP_GAP = 24f;
  private static final float MAX_SLOT_SIZE = 96f;
  private static final int WEAPON_SLOTS = 3;

  private BottomHotbarLayout() {}

  static Table root(String name) {
    Table root =
        new Table() {
          @Override
          protected void sizeChanged() {
            super.sizeChanged();
            invalidateGroups();
          }

          @Override
          public void setOrigin(int alignment) {
            // UiScale normally infers a separate edge for each panel. These two groups form one
            // row, so they must always use the same bottom-left coordinate system.
            setOrigin(0f, 0f);
          }

          @Override
          public void setScale(float scale) {
            boolean changed = getScaleX() != scale;
            super.setScale(scale);
            if (changed) {
              invalidate();
              invalidateGroups();
            }
          }

          private void invalidateGroups() {
            // Nested tables cache their preferred sizes even when viewport-based Values change.
            for (Actor child : getChildren()) {
              if (child instanceof Table group) group.invalidate();
            }
          }
        };
    root.setName(name);
    root.setFillParent(true);
    root.bottom().left().padBottom(BOTTOM_INSET);
    root.setTouchable(Touchable.disabled);
    return root;
  }

  static Value slotSize(Table root) {
    return new Value() {
      @Override
      public float get(Actor context) {
        return slotSize(availableWidth(root));
      }
    };
  }

  private static float slotSize(float width) {
    int totalSlots = WEAPON_SLOTS + InventoryComponent.CONSUMABLE_SLOT_COUNT;
    float reservedWidth =
        leftInset(width)
            + Math.min(RIGHT_INSET, width * 0.1f)
            + GROUP_GAP
            + (totalSlots - 2) * SLOT_GAP;
    return Math.clamp((width - reservedWidth) / totalSlots, 1f, MAX_SLOT_SIZE);
  }

  static float availableWidth(Table root) {
    return root.getStage().getWidth() / root.getScaleX();
  }

  static float leftInset(float width) {
    return Math.min(LEFT_INSET, width * 0.45f);
  }

  static float consumableInset(float width) {
    return leftInset(width)
        + WEAPON_SLOTS * slotSize(width)
        + (WEAPON_SLOTS - 1) * SLOT_GAP
        + GROUP_GAP;
  }
}
