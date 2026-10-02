package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Fire-wizard transformation, persistent fire shield and the encounter's projectile overlay. */
public class FinalBossStageTwoVisualComponent extends RenderComponent {
  private final Entity target;
  private FinalBossStageTwoComponent stage;
  private FinalBossPhaseControllerComponent phases;
  private CombatStatsComponent stats;
  private TextureRegion[] previousWizard;
  private TextureRegion[] fireWizard;
  private TextureRegion[] transform;
  private TextureRegion[] shield;
  private TextureRegion[] fireball;
  private TextureRegion[] impact;
  private FinalBossStageTwoIceVisuals iceVisuals;
  private FinalBossStageTwoPickupVisuals pickupVisuals;
  private RenderComponent overlay;
  private float elapsed;
  private float hitRemaining;
  private int previousHealth;
  private boolean disposed;

  public FinalBossStageTwoVisualComponent(Entity target) {
    this.target = target;
  }

  @Override
  public void create() {
    stage = entity.getComponent(FinalBossStageTwoComponent.class);
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    previousHealth = stats.getHealth();
    previousWizard = FinalBossVisualAssets.WIZARD.loadFrames();
    fireWizard = FinalBossVisualAssets.WIZARD_STAGE_TWO.loadFrames();
    transform = FinalBossStageTwoAssets.transformFrames();
    shield = FinalBossStageTwoAssets.shieldFrames();
    fireball = FinalBossStageTwoAssets.fireballFrames();
    impact = FinalBossStageTwoAssets.impactFrames();
    iceVisuals = new FinalBossStageTwoIceVisuals();
    pickupVisuals = new FinalBossStageTwoPickupVisuals();
    entity.getEvents().addListener("updateHealth", this::healthChanged);
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::phaseChanged);
    super.create();
    overlay =
        new RenderComponent() {
          @Override
          public float getZIndex() {
            return Float.MAX_VALUE;
          }

          @Override
          protected void draw(SpriteBatch batch) {
            drawProjectiles(batch);
            if (isVisible()) {
              pickupVisuals.drawPlayerBuff(batch, stage.getPickupController(), target, elapsed);
            }
          }
        };
    overlay.setEntity(entity);
    overlay.create();
  }

  private void healthChanged(Integer health) {
    if (phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO && health < previousHealth) {
      hitRemaining = 0.2f;
    }
    previousHealth = health;
  }

  private void phaseChanged(FinalBossPhase phase) {
    elapsed = 0f;
    hitRemaining = 0f;
  }

  @Override
  public void update() {
    if (disposed || ServiceLocator.getTimeSource() == null) return;
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;
    if (phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO) elapsed += delta;
    hitRemaining = Math.max(0f, hitRemaining - delta);
  }

  private boolean isVisible() {
    if (disposed || phases.getCurrentPhase() != FinalBossPhase.STAGE_TWO || stats.isDead())
      return false;
    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    return playerStats == null || !playerStats.isDead();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (!isVisible()) return;
    float colour = batch.getPackedColor();
    try {
      iceVisuals.draw(batch, stage.getIceController());
      pickupVisuals.drawGround(batch, stage.getPickupController());
      Vector2 centre = entity.getCenterPosition();
      Vector2 size = entity.getScale();
      boolean transforming = phases.isTransitioning();
      float progress = phases.getTransitionProgress();
      TextureRegion[] wizard = transforming && progress < 0.5f ? previousWizard : fireWizard;
      FinalBossStageTwoFireController fire = stage.getFireController();
      int frame =
          fire != null && fire.getCastRemaining() > 0f
              ? 8 + (int) (elapsed / 0.09f) % 4
              : (int) (elapsed / 0.25f) % 2;
      float width = size.x * 0.8f;
      float height = size.y * 0.8f;
      float hover = 0.035f * MathUtils.sin(elapsed * 4f);
      boolean left = target.getCenterPosition().x < centre.x;
      if (hitRemaining > 0f) batch.setColor(1f, 0.55f, 0.35f, 1f);
      batch.draw(
          wizard[frame],
          left ? centre.x + width / 2f : centre.x - width / 2f,
          centre.y - height / 2f + hover,
          left ? -width : width,
          height);

      // This visual persists during firing pauses. Ice-only damage is a later gameplay step.
      batch.setColor(1f, 1f, 1f, 0.78f + 0.12f * MathUtils.sin(elapsed * 5f));
      centred(
          batch,
          FinalBossStageTwoAssets.frame(shield, elapsed, 2.4f, true),
          centre,
          size.x * 1.25f,
          size.y * 1.25f);
      if (transforming) {
        batch.setColor(1f, 1f, 1f, 1f);
        centred(
            batch,
            FinalBossStageTwoAssets.frame(transform, progress, 1f, false),
            centre,
            size.x * 1.5f,
            size.y * 1.5f);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private void drawProjectiles(SpriteBatch batch) {
    if (!isVisible()) return;
    FinalBossStageTwoFireController fire = stage.getFireController();
    if (fire == null) return;
    float colour = batch.getPackedColor();
    try {
      batch.setColor(1f, 1f, 1f, 1f);
      for (FinalBossStageTwoFireController.Fireball shot : fire.fireballs) {
        TextureRegion frame = FinalBossStageTwoAssets.frame(fireball, shot.elapsed, 0.35f, true);
        // A larger visible flame keeps the smaller damage core easy to read while dodging.
        float size = 1.15f;
        // The source faces right; its bright head is at (50,32), not the centre of the 64px cell.
        float anchorX = size * 50f / 64f;
        float anchorY = size * 0.5f;
        batch.draw(
            frame,
            shot.position.x - anchorX,
            shot.position.y - anchorY,
            anchorX,
            anchorY,
            size,
            size,
            1f,
            1f,
            shot.velocity.angleDeg());
      }
      for (FinalBossStageTwoFireController.Impact burst : fire.impacts) {
        centred(
            batch,
            FinalBossStageTwoAssets.frame(impact, burst.elapsed, fire.getImpactDuration(), false),
            burst.position,
            1.2f,
            1.2f);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private static void centred(
      SpriteBatch batch, TextureRegion frame, Vector2 centre, float width, float height) {
    batch.draw(frame, centre.x - width / 2f, centre.y - height / 2f, width, height);
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    if (overlay != null) overlay.dispose();
    super.dispose();
    // Shared textures remain owned by RoomAssets/ResourceService.
  }
}
