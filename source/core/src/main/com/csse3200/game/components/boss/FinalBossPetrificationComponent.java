package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageOneConfig;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Controls the Stage 1 Wave 2 petrification punishment.
 *
 * <p>The Enemy Team owns targeting, warning timing and hit detection only. Applying and removing
 * the movement-speed status effect is delegated through player events so that the Status Effects
 * team can implement the actual effect independently.
 */
public class FinalBossPetrificationComponent extends Component {
  private final Entity target;
  private final FinalBossStageOneConfig config;

  private FinalBossPhaseControllerComponent phaseController;
  private FinalBossStageOneComponent stageOneController;
  private FinalBossPetrificationWarningRenderComponent warningRenderer;

  private final Vector2 markedPosition = new Vector2();

  private float warningRemaining;
  private float cooldownRemaining;

  private boolean warningActive;
  private boolean waveTwoActive;
  private boolean disposed;

  /** Creates the Wave 2 petrification controller. */
  public FinalBossPetrificationComponent(Entity target, FinalBossStageOneConfig config) {
    if (target == null || config == null) {
      throw new IllegalArgumentException("Petrification target and config must not be null");
    }

    config.validate();

    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    phaseController = requireComponent(FinalBossPhaseControllerComponent.class);
    stageOneController = requireComponent(FinalBossStageOneComponent.class);
    warningRenderer = requireComponent(FinalBossPetrificationWarningRenderComponent.class);

    /*
     * Listen for Stage 1 state changes so the warning is removed immediately when Wave 2 ends,
     * rather than waiting until the next update frame.
     */
    entity
        .getEvents()
        .addListener(FinalBossEvents.STAGE_ONE_STATE_CHANGED, this::onStageOneStateChanged);
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }

    boolean shouldBeActive =
        phaseController.getCurrentPhase() == FinalBossPhase.STAGE_ONE
            && stageOneController.getState() == FinalBossStageOneState.WAVE_TWO;

    if (!shouldBeActive) {
      if (waveTwoActive) {
        stopWaveTwo();
      }
      return;
    }

    if (!waveTwoActive) {
      startWaveTwo();
    }

    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();

    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      return;
    }

    if (warningActive) {
      warningRemaining = Math.max(0f, warningRemaining - deltaTime);

      if (warningRemaining <= 0f) {
        resolveWarning();
      }

      return;
    }

    cooldownRemaining = Math.max(0f, cooldownRemaining - deltaTime);

    if (cooldownRemaining <= 0f) {
      beginWarning();
    }
  }

  @Override
  public void dispose() {
    disposed = true;

    cancelWarning();

    if (waveTwoActive) {
      requestPetrificationClear();
    }

    waveTwoActive = false;
  }

  /** Returns whether a petrification warning is currently active. */
  public boolean isWarningActive() {
    return warningActive;
  }

  /** Returns the fixed position captured for the current warning. */
  public Vector2 getMarkedPosition() {
    return markedPosition.cpy();
  }

  /** Returns the remaining ability cooldown. */
  public float getCooldownRemaining() {
    return cooldownRemaining;
  }

  private void startWaveTwo() {
    waveTwoActive = true;

    /*
     * Start the first warning immediately after Wave 2 begins.
     * Subsequent casts use the configured cooldown.
     */
    cooldownRemaining = 0f;
  }

  private void stopWaveTwo() {
    cancelWarning();
    requestPetrificationClear();

    cooldownRemaining = 0f;
    waveTwoActive = false;
  }

  private void beginWarning() {
    /*
     * IMPORTANT:
     * Capture the player's position once. The warning does NOT follow the player after this point.
     */
    markedPosition.set(target.getCenterPosition());
    warningRemaining = config.petrificationWarningDuration;
    warningActive = true;

    warningRenderer.showAt(markedPosition, config.petrificationRadius);

    /*
     * This event also gives future VFX code a clean hook without changing the gameplay component.
     */
    entity
        .getEvents()
        .trigger(
            FinalBossEvents.PETRIFICATION_WARNING,
            markedPosition.cpy(),
            config.petrificationRadius,
            config.petrificationWarningDuration);

    if (warningRemaining <= 0f) {
      resolveWarning();
    }
  }

  private void resolveWarning() {
    boolean playerStillInside =
        target.getCenterPosition().dst2(markedPosition)
            <= config.petrificationRadius * config.petrificationRadius;

    warningRenderer.hide();
    warningActive = false;
    warningRemaining = 0f;

    if (playerStillInside) {
      /*
       * Cross-team interface:
       *
       * Enemy Team only reports that petrification hit.
       * Status Effects Team owns the actual movement-speed modification and restoration.
       *
       * Arguments:
       *   1. requested movement multiplier
       *   2. requested duration in seconds
       */
      target
          .getEvents()
          .trigger(
              FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED,
              config.petrificationSlowMultiplier,
              config.petrificationSlowDuration);

      entity.getEvents().trigger(FinalBossEvents.PETRIFICATION_HIT, target);
    } else {
      entity.getEvents().trigger(FinalBossEvents.PETRIFICATION_MISSED, markedPosition.cpy());
    }

    cooldownRemaining = config.petrificationCooldown;
  }

  private void cancelWarning() {
    warningActive = false;
    warningRemaining = 0f;

    if (warningRenderer != null) {
      warningRenderer.hide();
    }
  }

  /**
   * Requests removal of any petrification effect when Stage 1 ends.
   *
   * <p>The Status Effects team can listen for this event and restore the player's movement state.
   */
  private void requestPetrificationClear() {
    target.getEvents().trigger(FinalBossEvents.PETRIFICATION_EFFECT_CLEAR_REQUESTED);
  }

  private void onStageOneStateChanged(FinalBossStageOneState nextState) {
    if (nextState == FinalBossStageOneState.WAVE_TWO) {
      startWaveTwo();
      return;
    }

    if (waveTwoActive) {
      stopWaveTwo();
    }
  }

  private <T extends Component> T requireComponent(Class<T> type) {
    T component = entity.getComponent(type);

    if (component == null) {
      throw new IllegalStateException(
          "FinalBossPetrificationComponent requires " + type.getSimpleName());
    }

    return component;
  }
}
