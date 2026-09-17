package com.csse3200.game.components.spells;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/**
 * What every spell on the caster has in common: a cooldown, a targeting strategy deciding who is
 * caught, and the disc shown over the area it covered. Subclasses say only what landing on an enemy
 * does.
 *
 * <p>Which enemies get hit (all of them, the closest, everything within a radius, ...) is fully
 * decoupled from the effect. Swap the strategy to change targeting without touching a spell.
 */
public abstract class SpellComponent extends Component {
  private final float cooldown;
  private final String castEvent;
  private final Color aoeColour;
  private EnemyTargetingStrategy targetingStrategy;
  private float remainingCooldown;

  /**
   * @param cooldown seconds between casts
   * @param castEvent event on the caster that casts this spell, so each spell is cast on its own
   * @param aoeColour colour of the disc drawn over the area covered
   * @param targetingStrategy how targets are selected on cast
   * @throws IllegalArgumentException if cooldown is negative or a reference argument is null
   */
  protected SpellComponent(
      float cooldown, String castEvent, Color aoeColour, EnemyTargetingStrategy targetingStrategy) {
    if (cooldown < 0f) {
      throw new IllegalArgumentException("cooldown must be >= 0");
    }
    if (castEvent == null || aoeColour == null || targetingStrategy == null) {
      throw new IllegalArgumentException("castEvent, aoeColour and targetingStrategy are required");
    }
    this.cooldown = cooldown;
    this.castEvent = castEvent;
    this.aoeColour = aoeColour;
    this.targetingStrategy = targetingStrategy;
  }

  /**
   * Swap targeting behaviour at runtime, e.g. if the player picks a different spell variant.
   *
   * @param targetingStrategy new targeting strategy
   * @throws IllegalArgumentException if targetingStrategy is null
   */
  public void setTargetingStrategy(EnemyTargetingStrategy targetingStrategy) {
    if (targetingStrategy == null) {
      throw new IllegalArgumentException("targetingStrategy must not be null");
    }
    this.targetingStrategy = targetingStrategy;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(castEvent, this::cast);
  }

  @Override
  public void update() {
    if (remainingCooldown <= 0f) {
      return;
    }
    float dt = ServiceLocator.getTimeSource().getDeltaTime();
    remainingCooldown = Math.max(0f, remainingCooldown - Math.max(0f, dt));
  }

  /**
   * @return true if the cooldown has elapsed and the spell can be cast
   */
  public boolean canCast() {
    return remainingCooldown <= 0f;
  }

  /**
   * Casts the spell, showing the area covered and applying its effect to everything caught.
   *
   * @return true if it was cast, false if it is still on cooldown
   */
  public boolean cast() {
    if (!canCast()) {
      return false;
    }
    remainingCooldown = cooldown;

    Array<Entity> targets = targetingStrategy.selectTargets(entity);
    SpellAoeVisualComponent visual = entity.getComponent(SpellAoeVisualComponent.class);
    if (visual != null) {
      visual.show(aoeColour, targetingStrategy.getRadius());
    }
    for (Entity target : targets) {
      applyTo(target);
    }
    return true;
  }

  /** Applies this spell's effect to one enemy it caught. */
  protected abstract void applyTo(Entity target);

  /**
   * Puts an effect on a target that may not be able to carry one, which is the only thing every
   * spell's side effect needs to agree on.
   */
  protected static void addEffect(Entity target, StatusEffect effect) {
    StatusEffectsControllerComponent effects =
        target.getComponent(StatusEffectsControllerComponent.class);
    if (effects != null && !effects.isDisposed()) {
      effects.addStatusEffect(effect);
    }
  }
}
