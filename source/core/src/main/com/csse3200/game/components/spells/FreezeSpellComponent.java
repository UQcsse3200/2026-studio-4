package com.csse3200.game.components.spells;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Freeze spell: on cast, freezes every enemy its targeting strategy selects, leaving each one
 * tinted light blue and unable to move or attack until it thaws.
 */
public class FreezeSpellComponent extends SpellComponent {
  /** Event on the caster that casts this spell. */
  public static final String CAST_EVENT = "castFreeze";

  /** Light blue, drawn over the frozen area. */
  private static final Color AOE_COLOUR = new Color(0.5f, 0.8f, 1f, 0.85f);

  private final long freezeDuration;

  /**
   * @param cooldown seconds between casts
   * @param freezeDuration how long each caught enemy stays frozen, in milliseconds
   * @param targetingStrategy how targets are selected on cast
   * @throws IllegalArgumentException if a numeric argument is negative or targetingStrategy is null
   */
  public FreezeSpellComponent(
      float cooldown, long freezeDuration, EnemyTargetingStrategy targetingStrategy) {
    super(cooldown, CAST_EVENT, AOE_COLOUR, targetingStrategy);
    if (freezeDuration < 0L) {
      throw new IllegalArgumentException("freezeDuration must be >= 0");
    }
    this.freezeDuration = freezeDuration;
  }

  @Override
  protected void applyTo(Entity target) {
    addEffect(target, new FrozenEffect(ServiceLocator.getTimeSource(), freezeDuration));
  }
}
