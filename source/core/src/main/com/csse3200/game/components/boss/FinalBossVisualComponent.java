package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Renders Grandpa's entrance, airborne wizard, charge, cast and shield animations. */
public class FinalBossVisualComponent extends RenderComponent {
  private static final float SHIELD_HIT_DURATION = 0.48f;
  private static final float DAMAGE_HIT_DURATION = 0.24f;

  private final Entity target;
  private final FinalBossStageOneConfig config;

  private TextureRegion[] grandpa;
  private TextureRegion[] wizard;
  private TextureRegion[] wizardStageTwo;
  private TextureRegion[] wizardTired;
  private TextureRegion[] transform;
  private TextureRegion[] shield;
  private TextureRegion[] impact;
  private TextureRegion[] attackAlert;

  private FinalBossStageOneComponent stageOne;
  private FinalBossStageTwoComponent stageTwo;
  private FinalBossMovementComponent movement;
  private FinalBossDamageControllerComponent protection;
  private FinalBossPhaseControllerComponent phaseController;

  private int previousHealth;

  private float elapsed;
  private float hitRemaining;
  private float damageHitRemaining;
  private float castRemaining;
  private float castDuration;
  private float deathElapsed;

  private boolean dying;
  private boolean disposalScheduled;

  public FinalBossVisualComponent(Entity target, FinalBossStageOneConfig config) {
    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    grandpa = FinalBossVisualAssets.GRANDPA.loadFrames();
    wizard = FinalBossVisualAssets.WIZARD.loadFrames();
    wizardStageTwo = FinalBossVisualAssets.WIZARD_STAGE_TWO.loadFrames();
    wizardTired = FinalBossVisualAssets.WIZARD_TIRED.loadFrames();
    transform = FinalBossVisualAssets.TRANSFORM.loadFrames();
    shield = FinalBossVisualAssets.SHIELD.loadFrames();
    impact = FinalBossVisualAssets.SHIELD_HIT.loadFrames();
    attackAlert = FinalBossVisualAssets.ATTACK_ALERT.loadFrames();

    // Remove transparent margins while preserving the same centre in every frame.
    for (int i = 0; i < impact.length; i++) {
      impact[i] = new TextureRegion(impact[i], 20, 20, 32, 32);
    }

    stageOne = entity.getComponent(FinalBossStageOneComponent.class);
    stageTwo = entity.getComponent(FinalBossStageTwoComponent.class);
    movement = entity.getComponent(FinalBossMovementComponent.class);
    protection = entity.getComponent(FinalBossDamageControllerComponent.class);
    phaseController = entity.getComponent(FinalBossPhaseControllerComponent.class);

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    previousHealth = stats.getHealth();

    // Successful damage feedback during the break window.
    entity.getEvents().addListener("updateHealth", this::onHealthChanged);

    // Shielded attacks still need their own impact feedback.
    entity
        .getEvents()
        .addListener(FinalBossEvents.SHIELD_HIT, () -> hitRemaining = SHIELD_HIT_DURATION);

    // When the shield comes back, any previous orange damage feedback should stop immediately.
    entity.getEvents().addListener(FinalBossEvents.SHIELD_ACTIVATED, () -> damageHitRemaining = 0f);

    entity.getEvents().addListener(FinalBossEvents.PETRIFICATION_WARNING, this::onCast);
    entity.getEvents().addListener("dieAnimation", () -> dying = true);

    super.create();
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    elapsed += delta;

    hitRemaining = Math.max(0f, hitRemaining - delta);
    damageHitRemaining = Math.max(0f, damageHitRemaining - delta);
    castRemaining = Math.max(0f, castRemaining - delta);

    if (dying) {
      deathElapsed += delta;

      if (deathElapsed >= 0.5f && !disposalScheduled) {
        disposalScheduled = true;
        ServiceLocator.getEntityService().scheduleDisposal(entity);
      }
    }
  }

  private void onCast(Vector2 position, Float radius, Float duration) {
    castDuration = Math.max(0.1f, duration);
    castRemaining = castDuration;
  }

  /**
   * Shows orange damage feedback only when the Boss actually loses health during the break window.
   *
   * <p>This means attacks after reaching the health floor do not show successful-damage feedback.
   */
  private void onHealthChanged(Integer health) {
    if (health < previousHealth
        && health > 0
        && stageOne.getState() == FinalBossStageOneState.BREAK_WINDOW
        && !protection.isShielded()) {
      damageHitRemaining = DAMAGE_HIT_DURATION;
    }

    previousHealth = health;
  }

  /** Snapshot of per-frame layout values needed to draw the boss body and its effects. */
  private record BodyLayout(
      boolean ordinary,
      float hover,
      float bodyWidth,
      float bodyHeight,
      float x,
      float y,
      TextureRegion body,
      boolean damageHit,
      boolean transitioning) {}

  @Override
  protected void draw(SpriteBatch batch) {
    FinalBossStageOneState state = stageOne.getState();
    float stateTime = stageOne.getStateElapsed();
    Vector2 pos = entity.getPosition();
    Vector2 size = entity.getScale();

    BodyLayout layout = computeBodyLayout(state, stateTime, pos, size);
    float colour = batch.getPackedColor();

    try {
      drawBody(batch, layout, pos, size, colour);
      drawDamageHitEffect(batch, layout, colour);

      if (state == FinalBossStageOneState.TRANSFORMING) {
        int frame = FinalBossVisualAssets.once(stateTime, config.bossTransformDuration, 12);
        batch.draw(transform[frame], pos.x, pos.y, size.x, size.y);
      }

      drawChargeWarning(batch, pos, size);
      drawShield(batch, pos, size, layout.ordinary());
    } finally {
      batch.setPackedColor(colour);
    }
  }

  private boolean isOrdinaryPose(FinalBossStageOneState state, float stateTime) {
    return state == FinalBossStageOneState.INTRO
        || (state == FinalBossStageOneState.TRANSFORMING
            && stateTime < config.bossTransformDuration * 0.5f);
  }

  private boolean isAirborne(FinalBossStageOneState state, boolean ordinary) {
    return !ordinary
        && state != FinalBossStageOneState.BREAK_WINDOW
        && state != FinalBossStageOneState.COMPLETE
        && !dying;
  }

  private BodyLayout computeBodyLayout(
      FinalBossStageOneState state, float stateTime, Vector2 pos, Vector2 size) {
    boolean ordinary = isOrdinaryPose(state, stateTime);
    boolean airborne = isAirborne(state, ordinary);

    // Reserve headroom inside the physical rectangle, so hovering cannot leave camera bounds.
    float hover = airborne ? 0.12f + 0.035f * MathUtils.sin(elapsed * 4f) : 0f;

    float bodyHeight = size.y * 0.8f;
    float bodyWidth = size.x * 0.8f;

    float x = pos.x + (size.x - bodyWidth) * 0.5f;
    float y = pos.y + hover * size.y;

    TextureRegion[] activeWizardFrames = getActiveWizardFrames();
    TextureRegion body =
        ordinary
            ? grandpa[1 + (int) (elapsed / 0.3f) % 2]
            : activeWizardFrames[wizardFrame(state, stateTime)];

    boolean damageHit =
        damageHitRemaining > 0f
            && state == FinalBossStageOneState.BREAK_WINDOW
            && !protection.isShielded()
            && !dying;

    boolean transitioning = phaseController != null && phaseController.isTransitioning();

    return new BodyLayout(ordinary, hover, bodyWidth, bodyHeight, x, y, body, damageHit, transitioning);
  }

  /**
   * Draws the boss body. During a successful hit in the break window the body briefly receives a
   * warm tint.
   */
  private void drawBody(SpriteBatch batch, BodyLayout layout, Vector2 pos, Vector2 size, float colour) {
    applyBodyTint(batch, layout);

    boolean faceLeft =
        !layout.ordinary() && target.getCenterPosition().x < entity.getCenterPosition().x;

    batch.draw(
        layout.body(),
        faceLeft ? layout.x() + layout.bodyWidth() : layout.x(),
        layout.y(),
        faceLeft ? -layout.bodyWidth() : layout.bodyWidth(),
        layout.bodyHeight());

    // Restore colour so later effects are not unintentionally tinted.
    batch.setPackedColor(colour);
  }

  private void applyBodyTint(SpriteBatch batch, BodyLayout layout) {
    if (dying) {
      batch.setColor(1f, 1f, 1f, Math.max(0f, 1f - deathElapsed / 0.5f));
    } else if (layout.transitioning()) {
      // Blink red to warn that the boss is about to change stage.
      float blink = MathUtils.sin(elapsed * 12f) > 0f ? 1f : 0.35f;
      batch.setColor(1f, blink, blink, 1f);
    } else if (layout.damageHit()) {
      batch.setColor(1f, 0.5f, 0.5f, 1f);
    }
  }

  /** Orange successful-hit effect, only drawn when HP really decreased during BREAK_WINDOW. */
  private void drawDamageHitEffect(SpriteBatch batch, BodyLayout layout, float colour) {
    if (!layout.damageHit()) {
      return;
    }

    int frame =
        FinalBossVisualAssets.once(
            DAMAGE_HIT_DURATION - damageHitRemaining, DAMAGE_HIT_DURATION, impact.length);

    float effectWidth = layout.bodyWidth() * 0.8f;
    float effectHeight = layout.bodyHeight() * 0.8f;

    batch.setColor(1f, 0.65f, 0.45f, 1f);

    batch.draw(
        impact[frame],
        layout.x() + (layout.bodyWidth() - effectWidth) * 0.5f,
        layout.y() + (layout.bodyHeight() - effectHeight) * 0.5f,
        effectWidth,
        effectHeight);

    batch.setPackedColor(colour);
  }

  /** Draws the existing Stage 2 charge warning. */
  private void drawChargeWarning(SpriteBatch batch, Vector2 pos, Vector2 size) {
    if (dying || movement == null || !movement.isChargeWarningActive()) {
      return;
    }

    int frame = (int) (elapsed / 0.12f) % attackAlert.length;
    float alertWidth = size.x * 1.35f;
    float alertHeight = size.y * 1.35f;
    batch.draw(
        attackAlert[frame],
        pos.x + (size.x - alertWidth) * 0.5f,
        pos.y + (size.y - alertHeight) * 0.5f,
        alertWidth,
        alertHeight);
  }

  private void drawShield(SpriteBatch batch, Vector2 pos, Vector2 size, boolean ordinary) {
    if (ordinary || dying || !protection.isShielded()) {
      return;
    }

    boolean shieldHit = hitRemaining > 0f;
    float alpha = shieldHit ? 1f : 0.65f + 0.2f * MathUtils.sin(elapsed * 5f);

    // Briefly expand the intact shield when it is struck.
    float pulse = shieldHit ? 1f + 0.08f * hitRemaining / SHIELD_HIT_DURATION : 1f;
    float shieldWidth = size.x * pulse;
    float shieldHeight = size.y * pulse;
    float colour = batch.getPackedColor();

    try {
      batch.setColor(1f, 1f, 1f, alpha);
      batch.draw(
          shield[(int) (elapsed / 0.12f) % shield.length],
          pos.x + (size.x - shieldWidth) * 0.5f,
          pos.y + (size.y - shieldHeight) * 0.5f,
          shieldWidth,
          shieldHeight);

      if (shieldHit) {
        batch.setColor(1f, 1f, 1f, 1f);
        int frame =
            FinalBossVisualAssets.once(
                SHIELD_HIT_DURATION - hitRemaining, SHIELD_HIT_DURATION, impact.length);
        batch.draw(impact[frame], pos.x, pos.y, size.x, size.y);
      }
    } finally {
      batch.setPackedColor(colour);
    }
  }

  /** Selects the appropriate wizard texture based on current phase and attack state. */
  private TextureRegion[] getActiveWizardFrames() {
    FinalBossPhaseControllerComponent newPhaseController =
        entity.getComponent(FinalBossPhaseControllerComponent.class);
    if (newPhaseController == null || newPhaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return wizard;
    }

    if (stageTwo != null && !stageTwo.isAttacking()) {
      return wizardTired;
    }

    return wizardStageTwo;
  }

  private int wizardFrame(FinalBossStageOneState state, float stateTime) {
    if (state == FinalBossStageOneState.BREAK_WINDOW) {
      return 2 + (int) (stateTime / 0.25f) % 4;
    }

    if (state == FinalBossStageOneState.SUMMONING_ONE
        || state == FinalBossStageOneState.SUMMONING_TWO) {
      return 8 + FinalBossVisualAssets.once(stateTime, config.bossSummonCastDuration, 4);
    }

    if (state == FinalBossStageOneState.WAVE_TWO && castRemaining > 0f) {
      return 8 + FinalBossVisualAssets.once(castDuration - castRemaining, castDuration, 4);
    }

    return (int) (elapsed / 0.25f) % 2;
  }
}
