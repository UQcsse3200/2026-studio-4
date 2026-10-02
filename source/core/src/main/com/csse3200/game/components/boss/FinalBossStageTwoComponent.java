package com.csse3200.game.components.boss;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;

/** Controls Stage 2's roaming, fire volleys, ice cover/pickups and Stage 3 health floor. */
public class FinalBossStageTwoComponent extends Component {
  private static final float ARRIVAL_DISTANCE = 0.15f;
  private static final int DESTINATION_ATTEMPTS = 4;

  private final FinalBossStageTwoConfig stageTwoConfig;
  private final Entity target;
  private FinalBossPhaseControllerComponent phaseController;
  private CombatStatsComponent bossStats;
  private FinalBossMovementComponent movementComponent;
  private PhysicsMovementComponent physicsMovement;
  private FinalBossStageTwoArenaComponent arena;
  private boolean transitionSent;
  private boolean encounterStarted;
  private boolean disposed;
  private boolean isAttacking;
  private double cycleTimer;
  private float retargetRemaining;
  private Vector2 roamDestination;
  private Vector2 previousPlayerCentre;
  private FinalBossStageTwoFireController fire;
  private FinalBossStageTwoIceController ice;
  private FinalBossStageTwoPickupController pickups;
  private final FinalBossStageTwoFireController.WallQuery walls =
      new FinalBossStageTwoFireController.WallQuery() {
        @Override
        public float firstHitFraction(Vector2 from, Vector2 to) {
          return findWall(from, to).fraction();
        }

        @Override
        public void onHit(Vector2 from, Vector2 to) {
          WallHit hit = findWall(from, to);
          if (ice != null && hit.fixture() != null) ice.hitByFire(hit.fixture());
        }
      };

  public FinalBossStageTwoComponent(FinalBossStageTwoConfig stageTwoConfig) {
    this(null, stageTwoConfig);
  }

  public FinalBossStageTwoComponent(Entity target, FinalBossStageTwoConfig stageTwoConfig) {
    if (stageTwoConfig == null) {
      throw new IllegalArgumentException("Configs must not be null");
    }
    stageTwoConfig.validate();
    this.stageTwoConfig = stageTwoConfig;
    this.target = target;
  }

  @Override
  public void create() {
    phaseController = entity.getComponent(FinalBossPhaseControllerComponent.class);
    bossStats = entity.getComponent(CombatStatsComponent.class);
    movementComponent = entity.getComponent(FinalBossMovementComponent.class);
    physicsMovement = entity.getComponent(PhysicsMovementComponent.class);
    arena = entity.getComponent(FinalBossStageTwoArenaComponent.class);
    if (phaseController == null || bossStats == null || movementComponent == null) {
      throw new IllegalStateException(
          "FinalBossStageTwoComponent requires FinalBossPhaseControllerComponent, "
              + "CombatStatsComponent, and FinalBossMovementComponent");
    }
    entity.getEvents().addListener("updateHealth", this::onBossHealthChanged);
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::onPhaseChanged);
    entity.getEvents().addListener("entityDied", this::clearEffects);
    if (target != null) {
      fire = new FinalBossStageTwoFireController(stageTwoConfig, new Random());
      pickups = new FinalBossStageTwoPickupController(stageTwoConfig, new Random());
      ice =
          new FinalBossStageTwoIceController(
              entity, target, stageTwoConfig, new Random(), pickups::isClearOfPickups);
    }
  }

  /** Begins one continuous encounter only after the phase-transition protection ends. */
  public void startEncounter() {
    if (disposed || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    applyStageThreeHealthFloor();
    encounterStarted = true;
    transitionSent = false;
    isAttacking = true;
    cycleTimer = 0f;
    retargetRemaining = 0f;
    roamDestination = null;
    clearEffects();
    previousPlayerCentre = target == null ? null : target.getCenterPosition();
    movementComponent.disableChargeAttacks();
    movementComponent.setMode(FinalBossMovementComponent.Mode.STOPPED);
  }

  @Override
  public void update() {
    if (disposed || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    if (!encounterStarted
        || phaseController.isTransitioning()
        || bossStats.isDead()
        || isTargetDead()) {
      clearEffects();
      stopRoaming();
      return;
    }
    GameTime time = ServiceLocator.getTimeSource();
    float deltaTime = time == null ? 0f : time.getDeltaTime();
    if (!Float.isFinite(deltaTime) || deltaTime <= 0f) {
      stopRoaming();
      return;
    }
    updateRoaming(deltaTime);
    updateFire(deltaTime);
    // Retain overshoot so the three-second pause does not depend on frame boundaries.
    double cycleDuration = (double) stageTwoConfig.attackDuration + stageTwoConfig.pauseDuration;
    cycleTimer = (cycleTimer + deltaTime) % cycleDuration;
    isAttacking = cycleTimer < stageTwoConfig.attackDuration;
  }

  private boolean isTargetDead() {
    CombatStatsComponent stats =
        target == null ? null : target.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  /**
   * Advances through attack/pause boundaries without moving the pause's shots into another
   * interval.
   */
  private void updateFire(float delta) {
    if (fire == null) return;
    Rectangle bounds = arena == null ? null : arena.getBounds();
    if (bounds == null) {
      clearEffects();
      return;
    }
    PhysicsComponent playerPhysics = target.getComponent(PhysicsComponent.class);
    if (playerPhysics != null) playerPhysics.earlyUpdate();
    if (ice != null) ice.update(delta, bounds);
    Vector2 playerNow = target.getCenterPosition();
    Vector2 playerBefore = previousPlayerCentre == null ? playerNow : previousPlayerCentre;
    Vector2 origin = entity.getCenterPosition();
    float radius = Math.max(0.2f, Math.min(target.getScale().x, target.getScale().y) * 0.3f);
    double duration = (double) stageTwoConfig.attackDuration + stageTwoConfig.pauseDuration;
    double cursor = cycleTimer;
    double consumed = 0;
    for (int part = 0; consumed < delta && part < 8; part++) {
      boolean firing = cursor < stageTwoConfig.attackDuration;
      double boundary = firing ? stageTwoConfig.attackDuration : duration;
      double chunk = Math.min(delta - consumed, boundary - cursor);
      if (chunk <= 0) break;
      Vector2 from = playerBefore.cpy().lerp(playerNow, (float) (consumed / delta));
      Vector2 to = playerBefore.cpy().lerp(playerNow, (float) ((consumed + chunk) / delta));
      fire.update((float) chunk, firing, origin, from, to, radius, bounds, walls, this::hitPlayer);
      if (!canContinueFire()) {
        clearEffects();
        stopRoaming();
        return;
      }
      consumed += chunk;
      cursor += chunk;
      if (cursor >= duration) cursor = 0;
    }
    // A long stall may cross many cycles. Expire/move leftovers without a burst of catch-up shots.
    if (consumed < delta) {
      fire.update(
          (float) (delta - consumed),
          false,
          origin,
          playerBefore.cpy().lerp(playerNow, (float) (consumed / delta)),
          playerNow,
          radius,
          bounds,
          walls,
          this::hitPlayer);
      if (!canContinueFire()) clearEffects();
    }
    if (pickups != null && canContinueFire()) {
      pickups.update(
          delta,
          bounds,
          actorBounds(entity),
          actorBounds(target),
          playerBefore,
          this::isPickupSpaceClear);
    }
    previousPlayerCentre = playerNow;
  }

  private static Rectangle actorBounds(Entity actor) {
    Vector2 position = actor.getPosition();
    Vector2 scale = actor.getScale();
    return new Rectangle(position.x, position.y, scale.x, scale.y);
  }

  /** Pickups have no physics bodies, but leave enough space around solid static geometry. */
  private boolean isPickupSpaceClear(Rectangle clearance) {
    PhysicsService physics = ServiceLocator.getPhysicsService();
    if (physics == null || physics.getPhysics() == null) return false;
    World world = physics.getPhysics().getWorld();
    if (world == null) return false;
    boolean[] blocked = {false};
    world.QueryAABB(
        fixture -> {
          if (fixture.getBody().isActive()
              && fixture.getBody().getType() == BodyType.StaticBody
              && !fixture.isSensor()) {
            blocked[0] = true;
            return false;
          }
          return true;
        },
        clearance.x,
        clearance.y,
        clearance.x + clearance.width,
        clearance.y + clearance.height);
    return !blocked[0];
  }

  private boolean canContinueFire() {
    return !disposed
        && !bossStats.isDead()
        && !isTargetDead()
        && phaseController.getCurrentPhase() == FinalBossPhase.STAGE_TWO;
  }

  private void hitPlayer() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats == null || !canContinueFire()) return;
    stats.takeDamage(stageTwoConfig.fireballDamage, entity);
    if (!canContinueFire()) clearEffects();
  }

  /** Pure query; cover durability changes only after the fire controller consumes a real hit. */
  private WallHit findWall(Vector2 from, Vector2 to) {
    if (ServiceLocator.getPhysicsService() == null)
      return new WallHit(null, Float.POSITIVE_INFINITY);
    World world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    Fixture[] nearestFixture = {null};
    world.QueryAABB(
        fixture -> {
          if (!fixture.isSensor()
              && fixture.getBody().getType() == BodyType.StaticBody
              && fixture.testPoint(from)) {
            nearestFixture[0] = fixture;
            return false;
          }
          return true;
        },
        from.x - 0.001f,
        from.y - 0.001f,
        from.x + 0.001f,
        from.y + 0.001f);
    if (nearestFixture[0] != null) return new WallHit(nearestFixture[0], 0f);
    if (from.epsilonEquals(to, 0.0001f)) return new WallHit(null, Float.POSITIVE_INFINITY);
    float[] nearest = {Float.POSITIVE_INFINITY};
    world.rayCast(
        (fixture, point, normal, fraction) -> {
          if (fixture.isSensor() || fixture.getBody().getType() != BodyType.StaticBody) return -1f;
          if (fraction < nearest[0]) {
            nearest[0] = fraction;
            nearestFixture[0] = fixture;
          }
          return fraction;
        },
        from,
        to);
    return new WallHit(nearestFixture[0], nearest[0]);
  }

  private record WallHit(Fixture fixture, float fraction) {}

  FinalBossStageTwoIceController getIceController() {
    return ice;
  }

  FinalBossStageTwoFireController getFireController() {
    return fire;
  }

  FinalBossStageTwoPickupController getPickupController() {
    return pickups;
  }

  private void clearEffects() {
    if (fire != null) fire.clear();
    if (ice != null) ice.clear();
    if (pickups != null) pickups.clear();
    previousPlayerCentre = null;
  }

  private void updateRoaming(float deltaTime) {
    Rectangle allowed = arena == null ? null : arena.getMovementBounds(entity);
    if (physicsMovement == null || allowed == null) {
      stopRoaming();
      return;
    }
    PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
    if (physics != null) {
      physics.earlyUpdate();
    }
    Vector2 position = entity.getPosition();
    retargetRemaining -= deltaTime;
    if (roamDestination != null) {
      roamDestination.set(
          MathUtils.clamp(roamDestination.x, allowed.x, allowed.x + allowed.width),
          MathUtils.clamp(roamDestination.y, allowed.y, allowed.y + allowed.height));
    }
    if (roamDestination == null
        || retargetRemaining <= 0f
        || position.dst2(roamDestination) <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
      roamDestination = chooseDestination(allowed, position);
      retargetRemaining = stageTwoConfig.bossRoamRetargetInterval;
    }
    float distance = position.dst(roamDestination);
    if (distance <= ARRIVAL_DISTANCE) {
      stopRoaming();
      return;
    }
    // PhysicsMovementComponent normalises direction; reduce speed close to a destination.
    float speed = Math.min(stageTwoConfig.bossMoveSpeed, distance / deltaTime);
    physicsMovement.setMaxSpeed(new Vector2(speed, speed));
    physicsMovement.setTarget(roamDestination.cpy());
    physicsMovement.setMoving(true);
  }

  private Vector2 chooseDestination(Rectangle allowed, Vector2 position) {
    Vector2 candidate = new Vector2();
    for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
      candidate.set(
          MathUtils.random(allowed.x, allowed.x + allowed.width),
          MathUtils.random(allowed.y, allowed.y + allowed.height));
      if (position.dst2(candidate) > ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
        return candidate;
      }
    }
    // A tiny viewport can leave no useful random choice; try the opposite corner safely.
    return candidate.set(
        position.x < allowed.x + allowed.width / 2f ? allowed.x + allowed.width : allowed.x,
        position.y < allowed.y + allowed.height / 2f ? allowed.y + allowed.height : allowed.y);
  }

  private void stopRoaming() {
    if (physicsMovement != null) {
      physicsMovement.setMoving(false);
    }
  }

  private void onPhaseChanged(FinalBossPhase phase) {
    clearEffects();
    encounterStarted = false;
    isAttacking = false;
    cycleTimer = 0f;
    roamDestination = null;
    retargetRemaining = 0f;
    if (phase == FinalBossPhase.STAGE_TWO) {
      transitionSent = false;
    }
  }

  /** Prevents a powerful hit from skipping the Stage 3 encounter entirely. */
  public void applyStageThreeHealthFloor() {
    bossStats.setMinimumHealth(
        Math.max(
            1, Math.round(bossStats.getMaxHealth() * stageTwoConfig.stageThreeHealthThreshold)));
  }

  /** Returns whether the active encounter is in its firing interval rather than its pause. */
  public boolean isAttacking() {
    return encounterStarted
        && !disposed
        && phaseController.getCurrentPhase() == FinalBossPhase.STAGE_TWO
        && !phaseController.isTransitioning()
        && !bossStats.isDead()
        && isAttacking;
  }

  private void onBossHealthChanged(Integer health) {
    if (transitionSent
        || health <= 0
        || phaseController.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      return;
    }
    int healthThreshold =
        Math.round(bossStats.getMaxHealth() * stageTwoConfig.stageThreeHealthThreshold);
    if (health <= healthThreshold) {
      transitionSent = true;
      movementComponent.disableChargeAttacks();
      entity.getEvents().trigger(FinalBossEvents.STAGE_COMPLETED, FinalBossPhase.STAGE_TWO);
    }
  }

  @Override
  public void dispose() {
    disposed = true;
    clearEffects();
    if (ice != null) ice.dispose();
    if (pickups != null) pickups.dispose();
    encounterStarted = false;
    roamDestination = null;
    // Entity disposal may already have destroyed its physics body; do not steer it here.
  }
}
