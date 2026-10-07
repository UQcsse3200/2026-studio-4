package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.UIComponent;

/** Explains the ice-magic counterattack while the Stage 2 encounter is active. */
public class FinalBossStageTwoHintDisplay extends UIComponent {
  private static final String HINT =
      "Pick up ice magic on the ground, then hold J to attack the boss.";
  private final Entity player;
  private FinalBossPhaseControllerComponent phases;
  private CombatStatsComponent bossStats;
  private Table root;
  private boolean disposed;

  public FinalBossStageTwoHintDisplay(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    bossStats = entity.getComponent(CombatStatsComponent.class);
    if (stage == null) return;

    Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    style.fontColor = Color.valueOf("123F7A");
    Label hint =
        new Label(HINT, style) {
          @Override
          public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            // Thicken the pixel font without altering the shared font or any other label.
            BitmapFontCache cache = getBitmapFontCache();
            cache.translate(1f, 0f);
            try {
              cache.draw(batch);
            } finally {
              cache.translate(-1f, 0f);
            }
          }
        };
    hint.setAlignment(Align.center);
    hint.setWrap(true);

    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", new Color(0.93f, 0.97f, 1f, 0.92f)));
    panel.pad(10f, 16f, 10f, 16f);
    panel.add(hint).growX();

    root = new Table();
    root.setName("final-boss-stage-two-hint");
    root.setFillParent(true);
    root.setTouchable(Touchable.disabled);
    root.bottom().padBottom(170f);
    root.add(panel)
        .width(
            new Value() {
              @Override
              public float get(Actor context) {
                // Reserve the side HUD space; narrow windows wrap the sentence into more lines.
                float available = Math.max(1f, stage.getWidth() - 48f);
                return Math.min(available, Math.max(200f, Math.min(700f, stage.getWidth() - 600f)));
              }
            });
    root.setVisible(false);
    stage.addActor(root);
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::phaseChanged);
    entity.getEvents().addListener("entityDied", this::updateVisibility);
    updateVisibility();
  }

  private void phaseChanged(FinalBossPhase phase) {
    updateVisibility();
  }

  @Override
  public void update() {
    updateVisibility();
  }

  private void updateVisibility() {
    if (root == null) return;
    CombatStatsComponent playerStats =
        player == null ? null : player.getComponent(CombatStatsComponent.class);
    root.setVisible(
        !disposed
            && enabled
            && phases != null
            && phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO
            && !phases.isTransitioning()
            && bossStats != null
            && !bossStats.isDead()
            && (playerStats == null || !playerStats.isDead()));
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The shared UI stage draws the label in screen space.
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    if (root != null) root.remove();
    super.dispose();
  }
}
