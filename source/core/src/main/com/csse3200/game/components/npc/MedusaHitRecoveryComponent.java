package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.components.tasks.LungeAttackTask;
import com.csse3200.game.components.weapons.WeaponAttackSourceComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.Objects;

/** Briefly interrupts Medusa's movement and attacks after a surviving hit from the player. */
public class MedusaHitRecoveryComponent extends Component {
  private static final long RECOVERY_MILLIS = 350L;
  private final Entity player;
  private final LungeAttackTask lunge;
  private StatusEffectsControllerComponent effects;
  private StatusEffect recovery;
  private Entity lightningCaster;
  private boolean disposed;

  /**
   * Creates the recovery behaviour shared by Medusa and its split children.
   *
   * @param player player whose attacks can interrupt this enemy
   * @param lunge this enemy's lunge task
   */
  public MedusaHitRecoveryComponent(Entity player, LungeAttackTask lunge) {
    this.player = Objects.requireNonNull(player, "player");
    this.lunge = Objects.requireNonNull(lunge, "lunge");
  }

  @Override
  public void create() {
    effects = entity.getComponent(StatusEffectsControllerComponent.class);
    entity.getEvents().addListener("damageTaken", this::onDamageTaken);
    entity
        .getEvents()
        .addListener(
            LightningSpellComponent.DAMAGE_SOURCE_EVENT,
            (Entity caster) -> lightningCaster = caster);
  }

  private void onDamageTaken(Entity source, int healthLost, int remainingHealth) {
    if (disposed || healthLost <= 0 || remainingHealth <= 0 || !isPlayerAttack(source)) {
      return;
    }
    if (recovery != null) {
      effects.removeStatusEffect(recovery);
    }
    recovery =
        new StatusEffect(ServiceLocator.getTimeSource(), RECOVERY_MILLIS) {
          @Override
          public boolean immobilisesOwner() {
            return true;
          }
        };
    effects.addStatusEffect(recovery);
    lunge.interrupt();
  }

  private boolean isPlayerAttack(Entity source) {
    if (source == player) {
      return true;
    }
    if (source == null) {
      return lightningCaster == player;
    }
    WeaponAttackSourceComponent weaponSource =
        source.getComponent(WeaponAttackSourceComponent.class);
    if (weaponSource != null) {
      return weaponSource.getOwner() == player;
    }
    // Arrows do not yet carry a WeaponAttackSourceComponent.
    HitboxComponent hitbox = source.getComponent(HitboxComponent.class);
    TouchAttackComponent attack = source.getComponent(TouchAttackComponent.class);
    return hitbox != null
        && PhysicsLayer.contains(hitbox.getLayer(), PhysicsLayer.WEAPON)
        && attack != null
        && PhysicsLayer.contains(attack.getTargetLayer(), PhysicsLayer.NPC)
        && !PhysicsLayer.contains(attack.getTargetLayer(), PhysicsLayer.PLAYER);
  }

  @Override
  public void dispose() {
    disposed = true;
    lightningCaster = null;
    if (effects != null && recovery != null) {
      effects.removeStatusEffect(recovery);
      recovery = null;
    }
  }
}
