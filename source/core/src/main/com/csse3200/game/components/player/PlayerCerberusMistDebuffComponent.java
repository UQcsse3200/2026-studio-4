package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMistComponent;
import com.csse3200.game.components.miniboss.cerberus.CerberusMistSlowEffect;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;

/** Applies Cerberus mist effects to the player while they remain inside its area. */
public class PlayerCerberusMistDebuffComponent extends Component {
  private CombatStatsComponent stats;
  private StatusEffectsControllerComponent effects;
  private StatusEffect slow;
  private StatusEffect vulnerable;
  private Entity mistSource;
  private boolean disposed;

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    effects = entity.getComponent(StatusEffectsControllerComponent.class);

    if (stats == null || effects == null) {
      throw new IllegalStateException(
          "PlayerCerberusMistDebuffComponent requires combat stats and status effects");
    }

    entity.getEvents().addListener(CerberusMistComponent.ENTERED, this::apply);
    entity.getEvents().addListener(CerberusMistComponent.EXITED, this::clear);
    entity.getEvents().addListener("entityDied", this::clearAll);
  }

  /** Returns whether this player currently has effects from a Cerberus mist area. */
  public boolean isMistDebuffed() {
    return !disposed && slow != null && effects.hasStatusEffect(slow);
  }

  private void apply(Entity source) {
    if (disposed || effects.isDisposed() || stats.isDead() || source == mistSource) {
      return;
    }

    clearAll();

    slow = new CerberusMistSlowEffect();
    vulnerable = StatusEffectsFactory.createVulnerable();
    mistSource = source;

    effects.addStatusEffect(slow);
    effects.addStatusEffect(vulnerable);
    stats.notifyEffectiveStatsChanged();
  }

  private void clear(Entity source) {
    if (source == mistSource) {
      clearAll();
    }
  }

  private void clearAll() {
    boolean hadMistEffects = slow != null || vulnerable != null;

    effects.removeStatusEffect(slow);
    effects.removeStatusEffect(vulnerable);

    slow = null;
    vulnerable = null;
    mistSource = null;

    if (hadMistEffects && stats != null) {
      stats.notifyEffectiveStatsChanged();
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    clearAll();
    super.dispose();
  }
}
