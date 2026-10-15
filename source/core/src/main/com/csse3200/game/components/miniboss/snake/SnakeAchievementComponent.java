package com.csse3200.game.components.miniboss.snake;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Reports Snake encounter challenges using the configured achievement IDs. */
public class SnakeAchievementComponent extends Component {
  public static final String NO_SHIELD_PICKUP = "snakeDefeatedWithoutShieldPickup";
  public static final String GREEN_BUBBLE = "snakeStageTwoShieldPickup";
  public static final String WHACK_A_MOLE = "snakeExposedHit";
  private static final String ENTITY_DIED_EVENT = "entityDied";
  private final Entity target;
  private final Attempt attempt = new Attempt();
  private final Set<String> reportedAchievements = new HashSet<>();
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private SnakeShieldPickupComponent pickups;
  private SnakeBurrowComponent burrow;
  private Entity lightningCaster;

  public SnakeAchievementComponent(Entity target) {
    this.target = Objects.requireNonNull(target);
  }

  @Override
  public void create() {
    bossStats = entity.getComponent(CombatStatsComponent.class);
    playerStats = target.getComponent(CombatStatsComponent.class);
    pickups = entity.getComponent(SnakeShieldPickupComponent.class);
    burrow = entity.getComponent(SnakeBurrowComponent.class);
    if (bossStats == null || pickups == null) {
      throw new IllegalStateException(
          "SnakeAchievementComponent requires combat stats and shield pickups");
    }
    if (bossStats.isDead() || playerStats == null || playerStats.isDead()) {
      attempt.cancel();
    }
    entity.getEvents().addListener(ENTITY_DIED_EVENT, this::onBossDied);
    entity
        .getEvents()
        .addListener(
            SnakeShieldPickupComponent.SHIELD_GEM_COLLECTED_EVENT, this::onShieldGemCollected);
    entity.getEvents().addListener("damageAttempted", this::onDamageAttempted);
    entity.getEvents().addListener("damageBlocked", attempt::clearHit);
    entity.getEvents().addListener("damageTaken", this::onDamageTaken);
    entity
        .getEvents()
        .addListener(
            LightningSpellComponent.DAMAGE_SOURCE_EVENT,
            (Entity caster) -> lightningCaster = caster);
    // The player outlives this encounter; retain only the small attempt state in its listener.
    target.getEvents().addListener(ENTITY_DIED_EVENT, attempt::cancel);
  }

  private void onDamageAttempted(int damage, Entity source) {
    attempt.clearHit();
    if (damage > 0
        && !attempt.finished
        && !bossStats.isDead()
        && playerStats != null
        && !playerStats.isDead()
        && burrow != null
        && burrow.getState() == SnakeBurrowComponent.State.EXPOSED
        && (isPlayerAttack(source) || (source == null && lightningCaster == target))) {
      // A lethal hit changes the burrow state and may dispose Snake before damageTaken arrives.
      attempt.pendingExposedHit = true;
      attempt.pendingSource = source;
    }
  }

  private boolean isPlayerAttack(Entity source) {
    if (source == target) {
      return true;
    }
    if (source == null) {
      return false;
    }
    HitboxComponent hitbox = source.getComponent(HitboxComponent.class);
    TouchAttackComponent attack = source.getComponent(TouchAttackComponent.class);
    return hitbox != null
        && PhysicsLayer.contains(hitbox.getLayer(), PhysicsLayer.WEAPON)
        && attack != null
        && PhysicsLayer.contains(attack.getTargetLayer(), PhysicsLayer.NPC)
        && !PhysicsLayer.contains(attack.getTargetLayer(), PhysicsLayer.PLAYER);
  }

  private void onDamageTaken(Entity source, int healthLost, int remainingHealth) {
    boolean qualifyingHit =
        attempt.pendingExposedHit
            && attempt.pendingSource == source
            && healthLost > 0
            && !attempt.cancelled
            && playerStats != null
            && !playerStats.isDead();
    attempt.clearHit();
    if (qualifyingHit) {
      reportAchievement(WHACK_A_MOLE);
    }
  }

  private void onShieldGemCollected() {
    if (!attempt.finished
        && !bossStats.isDead()
        && playerStats != null
        && !playerStats.isDead()
        && burrow != null
        && burrow.isStageTwo()
        && pickups.hasCollectedShieldGem()) {
      reportAchievement(GREEN_BUBBLE);
    }
  }

  private void onBossDied() {
    if (bossStats.isDead()) {
      finishEncounter();
    }
  }

  private void finishEncounter() {
    if (attempt.finished) {
      return;
    }
    // Consume before notifying achievement listeners, which may clean up the encounter.
    attempt.finish();
    if (bossStats == null
        || !bossStats.isDead()
        || playerStats == null
        || playerStats.isDead()
        || pickups == null
        || pickups.hasCollectedShieldGem()) {
      return;
    }
    reportAchievement(NO_SHIELD_PICKUP);
  }

  private void reportAchievement(String achievementId) {
    if (!reportedAchievements.add(achievementId)) {
      return;
    }
    AchievementService achievements = ServiceLocator.getAchievementService();
    if (achievements != null) {
      AchievementContext context = new AchievementContext();
      context.bossAchievementId = achievementId;
      achievements.update(context);
    }
  }

  @Override
  public void dispose() {
    // Room death listeners can dispose the Snake before its own death listener runs.
    // A living Snake disposed during a room change instead cancels this attempt.
    if (bossStats == null || !bossStats.isDead()) {
      attempt.cancel();
    } else {
      finishEncounter();
    }
  }

  private static class Attempt {
    private boolean finished;
    private boolean cancelled;
    private boolean pendingExposedHit;
    private Entity pendingSource;

    private void finish() {
      finished = true;
    }

    private void clearHit() {
      pendingExposedHit = false;
      pendingSource = null;
    }

    private void cancel() {
      finish();
      cancelled = true;
      clearHit();
    }
  }
}
