package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.components.items.RoomMagneticPickupComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.Objects;

/** Keeps magnetic pickup guidance visible until the room's accumulated drops are collected. */
public class MagneticPickupHintDisplay extends UIComponent {
  private static final String HINT = "Hold E for 1 second to collect all room drops.";
  private final Entity player;
  private Table root;
  private boolean awaitingCollection;
  private boolean playerDead;
  private boolean disposed;

  /**
   * @param player player whose death and room transitions clear the hint
   */
  public MagneticPickupHintDisplay(Entity player) {
    this.player = Objects.requireNonNull(player);
  }

  @Override
  public void create() {
    super.create();
    root =
        new Table() {
          @Override
          public void setScale(float scale) {
            super.setScale(scale);
            // Recalculate the wrapping width when the player changes the UI scale.
            invalidateHierarchy();
          }
        };
    root.setName("magnetic-pickup-hint");
    root.setFillParent(true);
    root.top().padTop(24f);
    root.setTouchable(Touchable.disabled);

    Label.LabelStyle style = new Label.LabelStyle(skin.get("statDisplay", Label.LabelStyle.class));
    Label hint = new Label(HINT, style);
    hint.setName("magnetic-pickup-hint-text");
    hint.setAlignment(Align.center);
    hint.setWrap(true);
    hint.setFontScale(0.8f);

    Table panel = new Table();
    panel.setBackground(skin.getDrawable("tooltip"));
    panel.pad(12f, 16f, 12f, 16f);
    panel.add(hint).growX();
    root.add(panel)
        .width(
            new Value() {
              @Override
              public float get(Actor context) {
                return Math.clamp(stage.getWidth() / root.getScaleX() - 32f, 1f, 640f);
              }
            });
    root.setVisible(false);
    stage.addActor(root);

    player.getEvents().addListener(RoomMagneticPickupComponent.RESET_EVENT, this::clearHint);
    player.getEvents().addListener("entityDied", this::onPlayerDied);
  }

  @Override
  public void update() {
    if (disposed || root == null || playerDead) {
      return;
    }
    if (!enabled || gameplayPaused()) {
      root.setVisible(false);
      return;
    }
    int drops = countRoomDrops();
    if (drops == 0) {
      awaitingCollection = false;
    } else if (drops >= 2) {
      awaitingCollection = true;
    }
    root.setVisible(awaitingCollection);
  }

  private int countRoomDrops() {
    var entities = ServiceLocator.getEntityService().getEntities();
    int count = 0;
    // RoomSelector keeps only the current room loaded. Use indices because EntityService is
    // already iterating this array while updating the HUD.
    for (int i = 0; i < entities.size; i++) {
      Entity candidate = entities.get(i);
      ItemComponent item = candidate.getComponent(ItemComponent.class);
      if (candidate != player && item != null && !item.isCollected()) {
        count++;
      }
    }
    return count;
  }

  private boolean gameplayPaused() {
    return ServiceLocator.getEntityService().isFrozen()
        || (ServiceLocator.getRunTimer() != null && ServiceLocator.getRunTimer().isPaused());
  }

  private void clearHint() {
    awaitingCollection = false;
    if (root != null) {
      root.setVisible(false);
    }
  }

  private void onPlayerDied() {
    playerDead = true;
    clearHint();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The shared Scene2D stage draws this non-modal hint.
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    clearHint();
    if (root != null) {
      root.remove();
    }
    super.dispose();
  }
}
