package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Renders Grandpa's entrance, airborne wizard, charge, cast and shield animations. */
public class FinalBossVisualComponent extends RenderComponent {
  private final Entity target;
  private final FinalBossStageOneConfig config;
  private TextureRegion[] grandpa;
  private TextureRegion[] wizard;
  private TextureRegion[] transform;
  private TextureRegion[] shield;
  private TextureRegion[] impact;
  private FinalBossStageOneComponent stageOne;
  private FinalBossDamageControllerComponent protection;
  private float elapsed;
  private float hitRemaining;
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
    transform = FinalBossVisualAssets.TRANSFORM.loadFrames();
    shield = FinalBossVisualAssets.SHIELD.loadFrames();
    impact = FinalBossVisualAssets.SHIELD_HIT.loadFrames();
    stageOne = entity.getComponent(FinalBossStageOneComponent.class);
    protection = entity.getComponent(FinalBossDamageControllerComponent.class);
    entity.getEvents().addListener(FinalBossEvents.SHIELD_HIT, () -> hitRemaining = 0.32f);
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

  @Override
  protected void draw(SpriteBatch batch) {
    FinalBossStageOneState state = stageOne.getState();
    float stateTime = stageOne.getStateElapsed();
    boolean ordinary =
        state == FinalBossStageOneState.INTRO
            || (state == FinalBossStageOneState.TRANSFORMING
                && stateTime < config.bossTransformDuration * 0.5f);
    Vector2 pos = entity.getPosition();
    Vector2 size = entity.getScale();
    boolean airborne =
        !ordinary
            && state != FinalBossStageOneState.BREAK_WINDOW
            && state != FinalBossStageOneState.COMPLETE
            && !dying;
    // Reserve headroom inside the physical rectangle, so hovering cannot leave camera bounds.
    float hover = airborne ? 0.12f + 0.035f * MathUtils.sin(elapsed * 4f) : 0f;
    float bodyHeight = size.y * 0.8f;
    float bodyWidth = size.x * 0.8f;
    float x = pos.x + (size.x - bodyWidth) * 0.5f;
    float y = pos.y + hover * size.y;
    TextureRegion body =
        ordinary ? grandpa[1 + (int) (elapsed / 0.3f) % 2] : wizard[wizardFrame(state, stateTime)];
    float colour = batch.getPackedColor();
    try {
      if (dying) {
        batch.setColor(1f, 1f, 1f, Math.max(0f, 1f - deathElapsed / 0.5f));
      }
      boolean faceLeft = !ordinary && target.getCenterPosition().x < entity.getCenterPosition().x;
      batch.draw(
          body, faceLeft ? x + bodyWidth : x, y, faceLeft ? -bodyWidth : bodyWidth, bodyHeight);

      if (state == FinalBossStageOneState.TRANSFORMING) {
        int frame = FinalBossVisualAssets.once(stateTime, config.bossTransformDuration, 12);
        batch.draw(transform[frame], pos.x, pos.y, size.x, size.y);
      }
      if (!ordinary && !dying && protection.isShielded()) {
        float alpha = hitRemaining > 0f ? 1f : 0.65f + 0.2f * MathUtils.sin(elapsed * 5f);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(shield[(int) (elapsed / 0.12f) % shield.length], pos.x, pos.y, size.x, size.y);
        if (hitRemaining > 0f) {
          batch.setColor(1f, 1f, 1f, 1f);
          int frame = FinalBossVisualAssets.once(0.32f - hitRemaining, 0.32f, impact.length);
          batch.draw(impact[frame], pos.x, pos.y, size.x, size.y);
        }
      }
    } finally {
      batch.setPackedColor(colour);
    }
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
