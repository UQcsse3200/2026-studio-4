package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.MinotaurAttackRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Starts a minotaur swing on contact and resolves it on the visible axe impact frame. Animation and
 * damage share one update clock. The shared controller retains the original death lifecycle; lunge
 * movement and physics shapes are unchanged.
 */
public class MinotaurAnimationController extends EnemyAnimationController {
  private static final float ATTACK_INTERVAL = 1f;
  private static final float KNOCKBACK = 1.5f;
  private static final float TIME_EPSILON = 0.000001f;

  private enum Phase {
    LOCOMOTION,
    TELEGRAPH,
    DASH
  }

  private final Entity target;
  private final Map<Entity, Set<Fixture>> contacts = new HashMap<>();
  private AnimationRenderComponent locomotion;
  private MinotaurAttackRenderComponent attacks;
  private HitboxComponent hitbox;
  private CombatStatsComponent stats;
  private Phase phase = Phase.LOCOMOTION;
  private String resumeAnimation = "default";
  private Entity swingTarget;
  private float cooldown;
  private boolean impactResolved;
  private boolean dying;
  private boolean disposed;

  public MinotaurAnimationController(Entity target) {
    this.target = target;
  }

  @Override
  public void create() {
    // Register shared listeners first within one component: Entity's component order is undefined.
    super.create();
    locomotion = entity.getComponent(AnimationRenderComponent.class);
    attacks = entity.getComponent(MinotaurAttackRenderComponent.class);
    hitbox = entity.getComponent(HitboxComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
    entity.getEvents().addListener("lungeTelegraphStart", () -> startApproach(Phase.TELEGRAPH));
    entity.getEvents().addListener("lungeDashStart", () -> startApproach(Phase.DASH));
    entity.getEvents().addListener("lungeDashEnd", this::endDash);
    entity.getEvents().addListener("wanderStart", () -> resumeLocomotion("move"));
    entity.getEvents().addListener("chaseStart", () -> resumeLocomotion("chase"));
    entity.getEvents().addListener("patrolStart", () -> resumeLocomotion("move"));
    entity.getEvents().addListener("default", () -> resumeLocomotion("default"));
    entity.getEvents().addListener("dieAnimation", this::stopForDeath);
    entity.getEvents().addListener("enemyDeathAnimation", this::stopForDeath);
    entity.getEvents().addListener("entityDied", this::stopForDeath);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (disposed
        || dying
        || hitbox.getFixture() != me
        || !PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }
    Entity victim = ((BodyUserData) other.getBody().getUserData()).entity;
    contacts.computeIfAbsent(victim, ignored -> new HashSet<>()).add(other);
    tryStartAttack();
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (disposed || hitbox.getFixture() != me) {
      return;
    }
    // Bodies can be being disposed here, so do not inspect native fixture data.
    contacts
        .values()
        .removeIf(
            fixtures -> {
              fixtures.remove(other);
              return fixtures.isEmpty();
            });
  }

  private void startApproach(Phase nextPhase) {
    if (dying || disposed) {
      return;
    }
    phase = nextPhase;
    if (attacks.getCurrentAnimation() != null && !impactResolved) {
      // A cancelled wind-up did not strike; it must not prevent the ensuing charge strike.
      cooldown = 0f;
    }
    cancelSwing();
    resumeAnimation = "move";
    locomotion.startAnimation(resumeAnimation);
    locomotion.setFlipX(target.getCenterPosition().x < entity.getCenterPosition().x);
    tryStartAttack();
  }

  private void endDash() {
    phase = Phase.LOCOMOTION;
    // A contact strike can finish after movement stops. A missed dash has no strike.
  }

  private boolean canAct() {
    return !disposed
        && !dying
        && !stats.isDead()
        && !StatusEffectsControllerComponent.isImmobilised(entity);
  }

  private boolean canHit(Entity victim) {
    if (victim == null
        || !contacts.containsKey(victim)
        || StatusEffectsControllerComponent.isConcealed(victim)) {
      return false;
    }
    CombatStatsComponent victimStats = victim.getComponent(CombatStatsComponent.class);
    return victimStats != null && !victimStats.isDead();
  }

  private void tryStartAttack() {
    if (!canAct()
        || phase == Phase.TELEGRAPH
        || cooldown > TIME_EPSILON
        || attacks.getCurrentAnimation() != null) {
      return;
    }
    for (Entity victim : contacts.keySet()) {
      if (canHit(victim)) {
        startAttack(victim);
        return;
      }
    }
  }

  private void startAttack(Entity victim) {
    String animation =
        phase == Phase.DASH
            ? MinotaurAttackRenderComponent.CHARGE_ATTACK
            : MinotaurAttackRenderComponent.NORMAL_ATTACK;
    boolean facingLeft = victim.getCenterPosition().x < entity.getCenterPosition().x;
    swingTarget = victim;
    impactResolved = false;
    cooldown = ATTACK_INTERVAL;
    attacks.startAnimation(animation, facingLeft);
    locomotion.setFlipX(facingLeft);
    locomotion.stopAnimation();
  }

  private void resolveImpact() {
    if (impactResolved || !attacks.hasReachedImpactFrame()) {
      return;
    }
    // Consume the impact even on a miss. Re-entering later cannot cause a delayed hit.
    impactResolved = true;
    Entity victim = swingTarget;
    if (!canHit(victim)) {
      return;
    }
    victim.getComponent(CombatStatsComponent.class).hit(stats);
    PhysicsComponent physics = victim.getComponent(PhysicsComponent.class);
    if (physics != null) {
      Body body = physics.getBody();
      Vector2 impulse =
          victim.getCenterPosition().sub(entity.getCenterPosition()).setLength(KNOCKBACK);
      body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }
  }

  private void resumeLocomotion(String animation) {
    if (dying || disposed) {
      return;
    }
    // A new movement task also cancels a telegraph/dash interrupted without a dash-end event.
    phase = Phase.LOCOMOTION;
    resumeAnimation = animation;
    if (attacks.getCurrentAnimation() != null) {
      locomotion.stopAnimation();
    }
  }

  private void cancelSwing() {
    attacks.stopAnimation();
    swingTarget = null;
    impactResolved = true;
  }

  private void stopForDeath() {
    dying = true;
    cancelSwing();
    contacts.clear();
  }

  @Override
  public void update() {
    if (disposed) {
      return;
    }
    super.update();
    // AI may change phase later in this same update. Settle those transitions before choosing
    // the frame and resolving its hit, so the subsequent render shows the frame that did damage.
    ServiceLocator.getEntityService().runAfterUpdate(this::updateAttack);
  }

  private void updateAttack() {
    if (!canAct()) {
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    cooldown = Math.max(0f, cooldown - delta);
    if (attacks.getCurrentAnimation() != null) {
      attacks.advance(delta);
      resolveImpact();
      if (attacks.isFinished()) {
        cancelSwing();
        locomotion.startAnimation(resumeAnimation);
      }
    }
    tryStartAttack();
  }

  @Override
  public void dispose() {
    disposed = true;
    cancelSwing();
    contacts.clear();
    super.dispose();
  }
}
