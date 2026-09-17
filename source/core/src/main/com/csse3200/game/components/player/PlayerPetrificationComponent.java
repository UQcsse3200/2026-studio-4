package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.boss.FinalBossEvents;
import com.csse3200.game.components.statuseffects.PetrificationEffect;
import com.csse3200.game.services.ServiceLocator;

/** Connects the Boss petrification events to the player's shared status-effect controller. */
public class PlayerPetrificationComponent extends Component {
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private PetrificationEffect petrification;
  private boolean disposed;

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    effects = entity.getComponent(StatusEffectsControllerComponent.class);
    if (stats == null || effects == null) {
      throw new IllegalStateException(
          "PlayerPetrificationComponent requires combat stats and a status-effect controller");
    }
    entity.getEvents().addListener(FinalBossEvents.PETRIFICATION_EFFECT_REQUESTED, this::apply);
    entity
        .getEvents()
        .addListener(FinalBossEvents.PETRIFICATION_EFFECT_CLEAR_REQUESTED, this::clear);
  }

  /** Returns whether the movement penalty is still active, including between controller ticks. */
  public boolean isPetrified() {
    return !disposed && effects != null && effects.hasStatusEffect(petrification);
  }

  /** HUD copy. Empty when the player is not petrified. */
  public String getHudText() {
    return isPetrified() ? "Petrified" : "";
  }

  private void apply(Float multiplier, Float durationSeconds) {
    if (disposed || effects.isDisposed() || stats.isDead()) {
      return;
    }
    validateRequest(multiplier, durationSeconds);

    // A second hit refreshes the penalty instead of halving speed again.
    clear();
    if (durationSeconds == 0f || multiplier == 1f) {
      return;
    }
    long durationMillis = Math.max(1L, Math.round(durationSeconds.doubleValue() * 1000d));
    PetrificationEffect next =
        new PetrificationEffect(ServiceLocator.getTimeSource(), durationMillis, multiplier);
    next.setOnEnded(
        () -> {
          if (petrification == next) {
            petrification = null;
          }
          stats.notifyEffectiveStatsChanged();
        });
    petrification = next;
    effects.addStatusEffect(next);
    stats.notifyEffectiveStatsChanged();
  }

  private static void validateRequest(Float multiplier, Float durationSeconds) {
    if (multiplier == null
        || !Float.isFinite(multiplier)
        || multiplier < 0f
        || multiplier > 1f
        || durationSeconds == null
        || !Float.isFinite(durationSeconds)
        || durationSeconds < 0f) {
      throw new IllegalArgumentException(
          "Petrification requires a fraction from 0 to 1 and a duration");
    }
  }

  private void clear() {
    PetrificationEffect previous = petrification;
    petrification = null;
    if (effects != null) {
      // Preserve unrelated effects such as Last Stand, slow stacks and invisibility.
      effects.removeStatusEffect(previous);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    clear();
  }
}
