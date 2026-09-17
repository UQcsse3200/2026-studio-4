package com.csse3200.game.components.spells;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.statuseffects.FlashEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * Lightning spell: on cast, strikes every enemy its targeting strategy selects for a fixed amount
 * of damage, flashing each one purple to show it was caught.
 */
public class LightningSpellComponent extends SpellComponent {
  /** Event on the caster that casts this spell. */
  public static final String CAST_EVENT = "castLightning";

  /** Dim purple, drawn over the struck area. */
  private static final Color AOE_COLOUR = new Color(0.55f, 0.25f, 0.85f, 0.85f);

  /** Brighter purple, so a struck enemy still reads as purple against the area beneath it. */
  private static final Color STRUCK_TINT = new Color(0.8f, 0.4f, 1f, 1f);

  /** The same purple laid on top, so near-black enemies flash too rather than looking unhit. */
  private static final Color STRUCK_GLOW = new Color(0.6f, 0.25f, 0.95f, 0.65f);

  private final int damage;
  private final long flashDuration;

  /**
   * @param cooldown seconds between casts
   * @param damage damage dealt to each struck enemy
   * @param flashDuration how long a struck enemy flashes purple, in milliseconds
   * @param targetingStrategy how targets are selected on cast
   * @throws IllegalArgumentException if a numeric argument is negative or targetingStrategy is null
   */
  public LightningSpellComponent(
      float cooldown, int damage, long flashDuration, EnemyTargetingStrategy targetingStrategy) {
    super(cooldown, CAST_EVENT, AOE_COLOUR, targetingStrategy);
    if (damage < 0 || flashDuration < 0L) {
      throw new IllegalArgumentException("damage and flashDuration must be >= 0");
    }
    this.damage = damage;
    this.flashDuration = flashDuration;
  }

  @Override
  protected void applyTo(Entity target) {
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null) {
      return;
    }
    // Unattributed: the bolt has no entity behind it, and naming the caster would make a struck
    // enemy treat this as a hit from whoever cast it.
    targetStats.takeDamage(damage);
    addEffect(
        target,
        new FlashEffect(ServiceLocator.getTimeSource(), flashDuration, STRUCK_TINT, STRUCK_GLOW));
  }
}
