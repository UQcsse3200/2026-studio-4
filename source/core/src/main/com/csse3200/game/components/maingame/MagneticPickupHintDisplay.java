package com.csse3200.game.components.maingame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.items.RoomMagneticPickupComponent;
import com.csse3200.game.components.rooms.EnemyManagerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.Objects;

/** Explains magnetic pickup once, after an enemy drop leaves several items in the room. */
public class MagneticPickupHintDisplay extends UIComponent {
  private static final String HINT = "Hold E for 1 second to collect all room drops.";
  private static final float DISPLAY_SECONDS = 6f;
  private final Entity player;
  private Table root;
  private float remainingSeconds;
  private boolean shown;
  private boolean playerDead;
  private boolean disposed;

  /**
   * @param player player receiving enemy-drop notifications throughout the current run
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

    player.getEvents().addListener(EnemyManagerComponent.ENEMY_DROPS_SPAWNED, this::onEnemyDrops);
    player.getEvents().addListener(RoomMagneticPickupComponent.RESET_EVENT, this::clearHint);
    player.getEvents().addListener("entityDied", this::onPlayerDied);
  }

  private void onEnemyDrops(int count) {
    if (disposed || playerDead || shown || count <= 1) {
      return;
    }
    shown = true;
    remainingSeconds = DISPLAY_SECONDS;
    root.setVisible(enabled && !gameplayPaused());
  }

  @Override
  public void update() {
    if (disposed || root == null || remainingSeconds <= 0f) {
      return;
    }
    if (!enabled || gameplayPaused()) {
      root.setVisible(false);
      return;
    }
    remainingSeconds =
        Math.max(0f, remainingSeconds - ServiceLocator.getTimeSource().getDeltaTime());
    root.setVisible(remainingSeconds > 0f);
  }

  private boolean gameplayPaused() {
    return ServiceLocator.getEntityService().isFrozen()
        || (ServiceLocator.getRunTimer() != null && ServiceLocator.getRunTimer().isPaused());
  }

  private void clearHint() {
    remainingSeconds = 0f;
    if (root != null) {
      root.setVisible(false);
    }
    // Room transitions remove the visible prompt without teaching the same control again.
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
