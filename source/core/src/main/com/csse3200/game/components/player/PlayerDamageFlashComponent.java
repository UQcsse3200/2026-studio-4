package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.RedFlashEffect;
import com.csse3200.game.services.ServiceLocator;

/** Shows a short red blink whenever the player actually loses health. */
public class PlayerDamageFlashComponent extends Component {
  private static final long FLASH_DURATION_MILLIS = 600L;
  private StatusEffectsControllerComponent effects;
  private RedFlashEffect flash;
  private int previousHealth;
  private boolean disposed;

  @Override
  public void create() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    effects = entity.getComponent(StatusEffectsControllerComponent.class);
    if (stats == null || effects == null) {
      throw new IllegalStateException(
          "PlayerDamageFlashComponent requires combat stats and a status-effect controller");
    }
    previousHealth = stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthChanged);
    entity.getEvents().addListener("entityDied", this::clear);
  }

  private void onHealthChanged(Integer health) {
    boolean lostHealth = health < previousHealth;
    previousHealth = health;
    if (disposed || effects.isDisposed()) {
      return;
    }
    if (health <= 0) {
      clear();
    } else if (lostHealth) {
      // Health updates occur after mitigation. Blocked hits and healing never start a blink.
      clear();
      flash = new RedFlashEffect(ServiceLocator.getTimeSource(), FLASH_DURATION_MILLIS);
      effects.addStatusEffect(flash);
    }
  }

  private void clear() {
    if (effects != null) {
      effects.removeStatusEffect(flash);
    }
    flash = null;
  }

  @Override
  public void dispose() {
    disposed = true;
    clear();
  }
}
