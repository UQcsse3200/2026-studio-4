package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Owns ice attacks, jumping hit-count statues, shockwaves and the peaceful ending. */
public class FinalBossStageThreeComponent extends Component {
  public static final String STATE_CHANGED = "finalBossStageThreeState";
  private static final float STATUE_SPACING_BUFFER = 0.15f;
  private final Entity target;
  private final Consumer<Entity> spawner;
  final FinalBossStageThreeConfig config;
  final List<Bolt> bolts = new ArrayList<>();
  final List<Statue> statues = new ArrayList<>();
  final List<Shockwave> shockwaves = new ArrayList<>();
  private final Vector2 previousPlayerPosition = new Vector2();
  final List<Burst> bursts = new ArrayList<>();
  private FinalBossPhaseControllerComponent phases;
  private FinalBossDamageControllerComponent protection;
  private CombatStatsComponent stats;
  private FinalBossStageThreeState state = FinalBossStageThreeState.INACTIVE;
  private float stateTime;
  private float shotRemaining;
  private int volleysFired;
  private float repositionRemaining;
  private float recoveryRemaining;
  private int orbitDirection = 1;
  private float freezeRemaining;
  private float comboRemaining;
  private boolean besidePlayer;
  private boolean comboCompleted;
  private boolean disposed;
  private boolean playerDefeated;
  private int healthFloor;
  float castRemaining;
  float hitRemaining;
  float playerHitRemaining;
  float freezeElapsed;
  float thawRemaining;
  float shieldHitRemaining;
  private int previousHealth;

  public FinalBossStageThreeComponent(
      Entity target, Consumer<Entity> spawner, FinalBossStageThreeConfig config) {
    if (target == null || spawner == null || config == null)
      throw new IllegalArgumentException("Stage 3 arguments must not be null");
    config.validate();
    this.target = target;
    this.spawner = spawner;
    this.config = config;
  }

  @Override
  public void create() {
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    protection = entity.getComponent(FinalBossDamageControllerComponent.class);
    stats = entity.getComponent(CombatStatsComponent.class);
    if (phases == null || protection == null || stats == null)
      throw new IllegalStateException("Stage 3 requires phase, protection and combat components");
    previousHealth = stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::healthChanged);
    entity.getEvents().addListener(FinalBossEvents.SHIELD_HIT, () -> shieldHitRemaining = 0.48f);
  }

  public FinalBossStageThreeState getState() {
    return state;
  }

  public float getStateTime() {
    return stateTime;
  }

  public Entity getTarget() {
    return target;
  }

  public boolean isFrozen() {
    return freezeRemaining > 0f;
  }

  public int getRemainingStatues() {
    return (int) statues.stream().filter(s -> !s.broken).count();
  }

  @Override
  public void update() {
    if (disposed
        || phases.getCurrentPhase() == FinalBossPhase.STAGE_ONE
        || phases.getCurrentPhase() == FinalBossPhase.STAGE_TWO) return;
    if (ServiceLocator.getTimeSource() == null) return;
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (!Float.isFinite(delta) || delta <= 0f) return;
    updateEffects(delta);
    if (state == FinalBossStageThreeState.INACTIVE && !phases.isTransitioning()) startWaveOne();
    if (state == FinalBossStageThreeState.INACTIVE) return;
    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    if (playerStats != null && playerStats.isDead()) {
      if (!playerDefeated) {
        playerDefeated = true;
        clearFreeze();
        bolts.clear();
        clearStatueCombat();
        stop(entity);
      }
      return;
    }
    stateTime += delta;
    switch (state) {
      case WAVE_ONE -> updateWaveOne(delta);
      case CHARGING -> {
        if (stateTime >= config.chargeDuration) startWaveTwo();
      }
      case WAVE_TWO -> updateStatues(delta);
      case ENDING -> {
        if (stateTime >= config.returnTransformDuration) finishEncounter();
      }
      default -> {}
    }
  }

  private void updateEffects(float delta) {
    castRemaining = Math.max(0f, castRemaining - delta);
    hitRemaining = Math.max(0f, hitRemaining - delta);
    playerHitRemaining = Math.max(0f, playerHitRemaining - delta);
    shieldHitRemaining = Math.max(0f, shieldHitRemaining - delta);
    thawRemaining = Math.max(0f, thawRemaining - delta);
    for (Burst burst : bursts) burst.elapsed += delta;
    bursts.removeIf(b -> b.elapsed >= config.disappearanceDuration);
    for (Statue statue : statues) statue.hitRemaining = Math.max(0f, statue.hitRemaining - delta);
  }

  /** Called at transition completion so removing the shield and installing the floor are atomic. */
  void startWaveOne() {
    if (disposed || state != FinalBossStageThreeState.INACTIVE) return;
    healthFloor = Math.max(1, Math.round(stats.getMaxHealth() * config.waveTwoHealthThreshold));
    protection.openVulnerabilityWindow(1f, Math.min(healthFloor, stats.getHealth()));
    stop(entity);
    resetVolleyCycle();
    changeState(FinalBossStageThreeState.WAVE_ONE);
    if (stats.getHealth() <= healthFloor) beginCharge();
  }

  private void healthChanged(Integer health) {
    if (state == FinalBossStageThreeState.WAVE_ONE && health < previousHealth && health > 0) {
      hitRemaining = 0.24f;
      if (health <= healthFloor) beginCharge();
    }
    previousHealth = health;
  }

  private void beginCharge() {
    if (state != FinalBossStageThreeState.WAVE_ONE) return;
    protection.enableShield();
    bolts.clear();
    clearFreeze();
    resetVolleyCycle();
    stop(entity);
    hitRemaining = 0f;
    changeState(FinalBossStageThreeState.CHARGING);
  }

  private void updateWaveOne(float delta) {
    if (isFrozen()) {
      updateCombo(delta);
      return;
    }
    // Resolve existing projectiles first: a freeze combo takes priority over repositioning.
    updateBolts(delta);
    if (isFrozen()) return;

    if (recoveryRemaining > 0f && updateVolleyRecovery(delta)) return;

    shotRemaining -= delta;
    if (shotRemaining <= 0f) {
      fireVolley();
    }
    if (recoveryRemaining > 0f) stop(entity);
    else keepDistance(delta);
  }

  /** Returns true while the pause after a pair of volleys is still active. */
  private boolean updateVolleyRecovery(float delta) {
    recoveryRemaining = Math.max(0f, recoveryRemaining - delta);
    if (repositionRemaining > 0f) {
      repositionRemaining = Math.max(0f, repositionRemaining - delta);
      if (repositionRemaining <= 0f) {
        teleport(farPosition());
        orbitDirection = -orbitDirection;
        castRemaining = 0f;
      }
    }
    stop(entity);
    if (recoveryRemaining > 0f) return true;
    resetVolleyCycle();
    // The next cycle starts now; do not add another between-volley delay to the pause.
    shotRemaining = 0f;
    return false;
  }

  private void fireVolley() {
    Vector2 origin = entity.getCenterPosition();
    Vector2 aim = target.getCenterPosition().sub(origin);
    if (aim.isZero()) aim.set(1f, 0f);
    aim.nor().scl(config.boltSpeed);
    for (int side = -1; side <= 1; side++) {
      Vector2 velocity = aim.cpy().rotateDeg(side * config.boltSpreadAngle);
      bolts.add(new Bolt(origin.cpy(), velocity));
    }
    castRemaining = 0.45f;
    shotRemaining = config.boltInterval;
    volleysFired++;
    if (volleysFired >= config.volleysBeforeTeleport) {
      repositionRemaining = config.volleyTeleportDelay;
      recoveryRemaining = config.volleyRecoveryDuration;
    }
  }

  private void resetVolleyCycle() {
    volleysFired = 0;
    repositionRemaining = 0f;
    recoveryRemaining = 0f;
    shotRemaining = config.boltInterval;
  }

  private void updateBolts(float delta) {
    boolean hit = false;
    Vector2 player = target.getCenterPosition();
    float radius =
        Math.max(0.25f, Math.min(target.getScale().x, target.getScale().y) * 0.4f) + 0.12f;
    for (Bolt bolt : bolts) {
      Vector2 before = bolt.position.cpy();
      bolt.position.mulAdd(bolt.velocity, delta);
      bolt.elapsed += delta;
      float wallFraction = wallFraction(before, bolt.position);
      if (wallFraction < 1f) {
        bolt.position.set(before.cpy().lerp(bolt.position, wallFraction));
        bolt.elapsed = config.boltLifetime;
      }
      if (segmentDistanceSquared(before, bolt.position, player) <= radius * radius) {
        Burst iceImpact = new Burst(player.cpy(), false);
        iceImpact.ice = true;
        bursts.add(iceImpact);
        hit = true;
        break;
      }
    }
    if (hit) {
      bolts.clear();
      freezePlayer();
    } else bolts.removeIf(b -> b.elapsed >= config.boltLifetime);
  }

  static float segmentDistanceSquared(Vector2 from, Vector2 to, Vector2 point) {
    Vector2 segment = to.cpy().sub(from);
    float fraction =
        segment.isZero()
            ? 0f
            : MathUtils.clamp(point.cpy().sub(from).dot(segment) / segment.len2(), 0f, 1f);
    return point.dst2(from.cpy().mulAdd(segment, fraction));
  }

  private void freezePlayer() {
    PlayerActions actions = target.getComponent(PlayerActions.class);
    if (actions == null) return;
    resetVolleyCycle();
    actions.setControlsLocked(this, true);
    freezeRemaining = config.freezeDuration;
    freezeElapsed = 0f;
    thawRemaining = 0f;
    comboRemaining = 0f;
    besidePlayer = false;
    comboCompleted = false;
    castRemaining = 0f;
    keepDistance(0f);
  }

  private void updateCombo(float delta) {
    freezeElapsed += delta;
    freezeRemaining = Math.max(0f, config.freezeDuration - freezeElapsed);
    if (!isFrozen()) {
      clearFreeze();
      resetVolleyCycle();
      return;
    }
    // A missed strike cannot repeat; the remaining freeze expires at its maximum duration.
    if (comboCompleted || freezeElapsed < config.teleportDelay) {
      keepDistance(delta);
      return;
    }
    if (!besidePlayer) {
      Vector2 offset = entity.getCenterPosition().sub(target.getCenterPosition());
      if (offset.isZero()) offset.set(1f, 0f);
      teleport(target.getCenterPosition().mulAdd(offset.nor(), 1f));
      besidePlayer = true;
      castRemaining = config.strikeDelay;
      comboRemaining = config.strikeDelay;
      return;
    }
    comboRemaining = Math.max(0f, comboRemaining - delta);
    if (comboRemaining > 0f) return;
    boolean inRange = entity.getCenterPosition().dst2(target.getCenterPosition()) <= 2.25f;
    if (inRange) {
      damagePlayer(config.strikeDamage);
      clearFreeze();
    }
    teleport(farPosition());
    castRemaining = 0f;
    if (inRange) resetVolleyCycle();
    else {
      comboCompleted = true;
      besidePlayer = false;
    }
  }

  private void clearFreeze() {
    PlayerActions actions = target.getComponent(PlayerActions.class);
    if (actions != null) actions.setControlsLocked(this, false);
    if (freezeRemaining > 0f || freezeElapsed > 0f) thawRemaining = 0.55f;
    freezeRemaining = 0f;
    freezeElapsed = 0f;
    besidePlayer = false;
    comboCompleted = false;
    comboRemaining = 0f;
  }

  private void keepDistance(float delta) {
    Vector2 centre = entity.getCenterPosition();
    Vector2 player = target.getCenterPosition();
    Vector2 away = centre.cpy().sub(player);
    float distanceSquared = away.len2();
    if (away.isZero()) away.set(1f, 0f);
    away.nor();
    boolean retreat =
        castRemaining > 0f || distanceSquared < config.preferredDistance * config.preferredDistance;
    float[] angles = retreat ? new float[] {0f, 90f, -90f} : new float[] {90f, -90f, 0f};
    // A longer target prevents overshooting tiny per-frame targets in the physics controller.
    float step = Math.max(1f, config.bossSpeed * delta);
    for (float angle : angles) {
      Vector2 direction = away.cpy().rotateDeg(angle * orbitDirection);
      Vector2 next = clamp(centre.cpy().mulAdd(direction, step), entity.getScale());
      Vector2 movement = next.cpy().sub(centre);
      if (movement.len2() > 0.01f
          && movement.dot(away) >= -0.0001f
          && clearSpace(next, entity.getScale())
          && wallFraction(centre, next) >= 1f) {
        moveTowards(entity, next, config.bossSpeed);
        return;
      }
    }
    stop(entity);
  }

  private Vector2 farPosition() {
    Vector2 player = target.getCenterPosition();
    Vector2 origin = entity.getCenterPosition();
    Vector2 best = origin;
    float bestScore = Float.POSITIVE_INFINITY;
    float startAngle = MathUtils.random(360f);
    float minimumDistance = Math.min(config.preferredDistance, config.repositionDistance) * 0.8f;
    // Search near the desired combat distance instead of always picking a distant corner.
    for (float radiusScale : new float[] {1f, 0.8f, 1.2f}) {
      for (int i = 0; i < 24; i++) {
        Vector2 offset =
            new Vector2(config.repositionDistance * radiusScale, 0f)
                .setAngleDeg(startAngle + i * 15f);
        Vector2 candidate = clamp(player.cpy().add(offset), entity.getScale());
        if (!clearSpace(candidate, entity.getScale())) continue;
        float distance = candidate.dst(player);
        float score = Math.abs(distance - config.repositionDistance);
        if (distance < minimumDistance) score += config.repositionDistance * 2f;
        if (candidate.dst2(origin) < 4f) score += config.repositionDistance;
        if (score < bestScore) {
          bestScore = score;
          best = candidate;
        }
      }
    }
    return best;
  }

  private void teleport(Vector2 centre) {
    burst(entity.getCenterPosition(), false);
    stop(entity);
    Vector2 destination = safePosition(centre, entity.getScale());
    entity.setPosition(destination.sub(entity.getScale().scl(0.5f)));
    burst(entity.getCenterPosition(), false);
  }

  private void startWaveTwo() {
    changeState(FinalBossStageThreeState.WAVE_TWO);
    setJumpEnabled(true);
    previousPlayerPosition.set(groundPosition(target));
    entity.getEvents().trigger("enemyHealthBarVisible", false);
    entity.getComponent(PhysicsComponent.class).getBody().setActive(false);
    burst(entity.getCenterPosition(), false);
    Rectangle area = bounds();
    for (int i = 0; i < config.statueCount; i++) {
      float angle = 360f * i / config.statueCount;
      Vector2 centre =
          new Vector2(
                  MathUtils.cosDeg(angle) * area.width * 0.35f,
                  MathUtils.sinDeg(angle) * area.height * 0.35f)
              .add(area.x + area.width / 2f, area.y + area.height / 2f);
      Entity statueEntity =
          NPCFactory.createBaseNPC().addComponent(new CombatStatsComponent(config.statueHits, 0));
      statueEntity.setScale(1.2f, 1.6f);
      statueEntity.setPosition(
          statueSpawnPosition(centre, statueEntity.getScale())
              .sub(statueEntity.getScale().scl(0.5f)));
      CombatStatsComponent statueStats = statueEntity.getComponent(CombatStatsComponent.class);
      statueStats.setInvulnerable(true);
      Statue statue = new Statue(statueEntity, config.statueHits);
      statue.pauseRemaining = config.statuePause * i / config.statueCount;
      statue.slamCooldown = config.statueSlamInitialDelay + config.statueSlamStagger * i;
      statueEntity
          .getEvents()
          .addListener(
              "damageAttempted",
              (Integer damage, Entity attacker) -> {
                if (damage > 0 && attacker != null && statue.attacks.add(attacker))
                  hitStatue(statue);
              });
      statues.add(statue);
      spawner.accept(statueEntity);
    }
  }

  void hitStatue(Statue statue) {
    if (disposed || playerDefeated || state != FinalBossStageThreeState.WAVE_TWO || statue.broken)
      return;
    statue.hitRemaining = 0.2f;
    statue.hitsRemaining--;
    statue
        .entity
        .getComponent(CombatStatsComponent.class)
        .setHealth(Math.max(0, statue.hitsRemaining));
    if (statue.hitsRemaining > 0) {
      statue.hitsSinceEvade++;
      if (statue.hitsSinceEvade >= config.statueEvadeHits) {
        statue.hitsSinceEvade = 0;
        // Hits may arrive inside Box2D callbacks. Move the body only in the next safe
        // update.
        statue.evadePending = true;
      }
      return;
    }
    statue.evadePending = false;
    statue.broken = true;
    statue.position = statue.entity.getCenterPosition();
    stop(statue.entity);
    // Weapon contacts can arrive during a locked Box2D step. Disposal runs safely after it.
    PhysicsComponent statuePhysics = statue.entity.getComponent(PhysicsComponent.class);
    if (!statuePhysics.getBody().getWorld().isLocked()) statuePhysics.getBody().setActive(false);
    statue.jumpHeight = 0f;
    statue.airborne = false;
    burst(statue.position, true);
    ServiceLocator.getEntityService().scheduleDisposal(statue.entity);
    if (getRemainingStatues() == 0) {
      clearStatueCombat();
      changeState(FinalBossStageThreeState.ENDING);
    }
  }

  private void updateStatues(float delta) {
    updateShockwaves(delta);
    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    if (playerStats != null && playerStats.isDead()) {
      playerDefeated = true;
      clearStatueCombat();
      stop(entity);
      return;
    }
    for (Statue statue : statues) {
      if (statue.broken) continue;
      if (updateStatueEvade(statue, delta)) continue;
      if (!updateStatueSlam(statue, delta)) updateStoneMovement(statue, delta);
    }
  }

  /** A successful dodge interrupts the statue's own slam and gives it a fresh cooldown. */
  private boolean updateStatueEvade(Statue statue, float delta) {
    if (!statue.evadePending) return false;
    if (statue.entity.getComponent(PhysicsComponent.class).getBody().getWorld().isLocked())
      return true;
    statue.evadeRetryRemaining = Math.max(0f, statue.evadeRetryRemaining - delta);
    if (statue.evadeRetryRemaining > 0f) return false;
    Vector2 destination = chooseStatueEvadePosition(statue);
    if (destination == null) {
      // Keep the request until space opens; never place the statue inside a wall or a peer.
      statue.evadeRetryRemaining = 0.25f;
      return false;
    }
    burst(statue.entity.getCenterPosition().add(0f, statue.jumpHeight), false);
    stop(statue.entity);
    statue.entity.setPosition(destination.cpy().sub(statue.entity.getScale().scl(0.5f)));
    statue.destination = null;
    statue.moveRemaining = 0f;
    statue.pauseRemaining = config.statuePause;
    statue.warningRemaining = 0f;
    statue.airborne = false;
    statue.jumpElapsed = 0f;
    statue.jumpHeight = 0f;
    statue.slamCooldown = config.statueSlamCooldown;
    statue.evadePending = false;
    burst(destination, false);
    return true;
  }

  private Vector2 chooseStatueEvadePosition(Statue statue) {
    Vector2 origin = statue.entity.getCenterPosition();
    Vector2 player = target.getCenterPosition();
    Vector2 away = origin.cpy().sub(player);
    if (away.isZero()) away.set(1f, 0f);
    // A sideways retreat breaks the player's line of attack without always choosing a corner.
    float side = (statue.hitsRemaining / config.statueEvadeHits) % 2 == 0 ? 1f : -1f;
    Vector2 preferred =
        origin.cpy().add(away.nor().rotateDeg(side * 60f).scl(config.statueEvadeDistance));
    Vector2 size = statueClearanceSize(statue.entity.getScale());
    Rectangle area = bounds();
    Vector2 min = clampStatue(new Vector2(area.x, area.y), size);
    Vector2 max = clampStatue(new Vector2(area.x + area.width, area.y + area.height), size);
    int columns = Math.max(1, MathUtils.ceil((max.x - min.x) / 0.35f));
    int rows = Math.max(1, MathUtils.ceil((max.y - min.y) / 0.35f));
    float minimumTravel = config.statueEvadeDistance * 0.75f;
    float bestScore = Float.MAX_VALUE;
    Vector2 best = null;
    for (int column = 0; column <= columns; column++) {
      for (int row = 0; row <= rows; row++) {
        Vector2 candidate =
            new Vector2(
                MathUtils.lerp(min.x, max.x, (float) column / columns),
                MathUtils.lerp(min.y, max.y, (float) row / rows));
        if (candidate.dst2(origin) < minimumTravel * minimumTravel
            || candidate.dst2(player) < 2.5f * 2.5f
            || !clearSpace(candidate, size)
            || !clearStatueEvadePosition(statue, candidate)) continue;
        float score = candidate.dst2(preferred);
        if (score < bestScore) {
          bestScore = score;
          best = candidate;
        }
      }
    }
    return best;
  }

  /** The landing point must leave room for each peer's entire reserved walking route. */
  private boolean clearStatueEvadePosition(Statue statue, Vector2 candidate) {
    float spacing = config.statueMinSpacing + STATUE_SPACING_BUFFER;
    for (Statue other : statues) {
      if (other == statue || other.broken) continue;
      Vector2 start = other.entity.getCenterPosition();
      Vector2 end = other.destination == null ? start : other.destination;
      if (segmentDistanceSquared(start, end, candidate) < spacing * spacing) return false;
    }
    return true;
  }

  /** A grounded warning and visible jump precede each landing; cooldown starts on landing. */
  private boolean updateStatueSlam(Statue statue, float delta) {
    if (statue.airborne) {
      stop(statue.entity);
      statue.jumpElapsed += delta;
      float progress = Math.min(1f, statue.jumpElapsed / config.statueJumpDuration);
      statue.jumpHeight = 4f * config.statueJumpHeight * progress * (1f - progress);
      if (progress >= 1f) landStatue(statue);
      return true;
    }
    if (statue.warningRemaining > 0f) {
      stop(statue.entity);
      statue.warningRemaining = Math.max(0f, statue.warningRemaining - delta);
      if (statue.warningRemaining <= 0f) {
        statue.airborne = true;
        statue.jumpElapsed = 0f;
      }
      return true;
    }
    statue.slamCooldown = Math.max(0f, statue.slamCooldown - delta);
    if (statue.slamCooldown > 0f) return false;
    stop(statue.entity);
    statue.destination = null;
    statue.warningRemaining = config.statueSlamWarning;
    return true;
  }

  private void landStatue(Statue statue) {
    statue.airborne = false;
    statue.jumpHeight = 0f;
    statue.slamCooldown = config.statueSlamCooldown;
    statue.pauseRemaining = config.statuePause;
    Vector2 centre = groundPosition(statue.entity);
    Rectangle area = bounds();
    float farX = Math.max(Math.abs(centre.x - area.x), Math.abs(centre.x - area.x - area.width));
    float farY = Math.max(Math.abs(centre.y - area.y), Math.abs(centre.y - area.y - area.height));
    shockwaves.add(new Shockwave(centre, (float) Math.sqrt(farX * farX + farY * farY)));
    if (ServiceLocator.getRenderService() != null)
      ServiceLocator.getRenderService().shake(this, 0.3f, 0.10f);
  }

  private void updateStoneMovement(Statue statue, float delta) {
    if (statue.pauseRemaining > 0f) {
      statue.pauseRemaining -= delta;
      statue.destination = null;
      stop(statue.entity);
      return;
    }
    Vector2 centre = statue.entity.getCenterPosition();
    // Recheck reservations every frame: a peer may have stopped, landed or been pushed.
    if (statue.destination != null && !safeStatueRoute(statue, centre, statue.destination)) {
      statue.destination = null;
      stop(statue.entity);
    }
    if (statue.destination == null) {
      statue.destination = chooseStatueDestination(statue, centre);
      if (statue.destination == null) {
        statue.pauseRemaining = config.statuePause;
        stop(statue.entity);
        return;
      }
      statue.moveRemaining = centre.dst(statue.destination) / config.statueSpeed + 1f;
    }
    statue.moveRemaining -= delta;
    float distance = centre.dst(statue.destination);
    if (statue.moveRemaining <= 0f || distance < 0.2f) {
      statue.destination = null;
      statue.pauseRemaining = config.statuePause;
      stop(statue.entity);
    } else {
      // Box2D may consume an extra accumulated fixed step. Brake before the reserved
      // endpoint.
      float speed =
          Math.min(config.statueSpeed, distance / (delta + PhysicsEngine.PHYSICS_TIMESTEP));
      moveTowards(statue.entity, statue.destination, speed);
      // Apply the new direction now, independent of the entities' component update order.
      statue.entity.getComponent(PhysicsMovementComponent.class).update();
    }
  }

  private Vector2 chooseStatueDestination(Statue statue, Vector2 centre) {
    Vector2 size = statueClearanceSize(statue.entity.getScale());
    Vector2 away = new Vector2();
    float spacing = config.statueMinSpacing + STATUE_SPACING_BUFFER;
    for (Statue other : statues) {
      if (other == statue || other.broken) continue;
      Vector2 separation = centre.cpy().sub(other.entity.getCenterPosition());
      if (separation.len2() < spacing * spacing) {
        if (separation.isZero()) {
          separation.set(1f, 0f).setAngleDeg(360f * statues.indexOf(statue) / statues.size());
        }
        away.add(separation.nor());
      }
    }
    float startAngle = away.isZero() ? MathUtils.random(360f) : away.angleDeg();
    // Try tangents and shorter steps before waiting. Roaming never teleports to a fallback
    // point.
    for (float stepScale : new float[] {1f, 0.65f, 0.35f}) {
      for (int direction = 0; direction < 24; direction++) {
        int offset = (direction + 1) / 2 * (direction % 2 == 0 ? -1 : 1);
        Vector2 step =
            new Vector2(config.statueStepDistance * stepScale, 0f)
                .setAngleDeg(startAngle + offset * 15f);
        Vector2 candidate = clampStatue(centre.cpy().add(step), size);
        if (candidate.dst2(centre) >= 0.0625f && safeStatueRoute(statue, centre, candidate))
          return candidate;
      }
    }
    return null;
  }

  /** Reserves the whole route, including every peer's current position and remaining route. */
  private boolean safeStatueRoute(Statue statue, Vector2 from, Vector2 to) {
    Vector2 size = statueClearanceSize(statue.entity.getScale());
    if (!to.epsilonEquals(clampStatue(to, size), 0.0001f)) return false;
    Vector2 sweptSize = size.add(Math.abs(to.x - from.x), Math.abs(to.y - from.y));
    if (!clearSpace(from.cpy().add(to).scl(0.5f), sweptSize)) return false;
    float spacing = config.statueMinSpacing + STATUE_SPACING_BUFFER;
    float spacingSquared = spacing * spacing;
    for (Statue other : statues) {
      if (other == statue || other.broken) continue;
      Vector2 otherFrom = other.entity.getCenterPosition();
      Vector2 otherTo =
          other.destination != null
                  && other.pauseRemaining <= 0f
                  && other.warningRemaining <= 0f
                  && !other.airborne
              ? other.destination
              : otherFrom;
      float currentDistanceSquared = from.dst2(otherFrom);
      float required = Math.min(spacingSquared, currentDistanceSquared);
      if (statueRoutesDistanceSquared(from, to, otherFrom, otherTo) + 0.0001f < required)
        return false;
      // External pushes can violate spacing. Only allow a gradual escape that never gets
      // closer.
      if (currentDistanceSquared < spacingSquared - 0.0001f
          && (to.dst2(otherFrom) <= currentDistanceSquared + 0.0001f
              || to.dst2(otherTo) <= currentDistanceSquared + 0.0001f)) return false;
    }
    return true;
  }

  private static float statueRoutesDistanceSquared(
      Vector2 from, Vector2 to, Vector2 otherFrom, Vector2 otherTo) {
    if (Intersector.intersectSegments(from, to, otherFrom, otherTo, null)) return 0f;
    return Math.min(
        Math.min(
            segmentDistanceSquared(from, to, otherFrom), segmentDistanceSquared(from, to, otherTo)),
        Math.min(
            segmentDistanceSquared(otherFrom, otherTo, from),
            segmentDistanceSquared(otherFrom, otherTo, to)));
  }

  private void updateShockwaves(float delta) {
    Vector2 player = groundPosition(target);
    PlayerActions actions = target.getComponent(PlayerActions.class);
    for (Shockwave wave : shockwaves) {
      float before = wave.radius;
      wave.radius = Math.min(wave.maxRadius, wave.radius + config.shockwaveSpeed * delta);
      if (!wave.hitPlayer
          && sweptRingContact(
              previousPlayerPosition,
              player,
              wave.position,
              before,
              wave.radius,
              config.shockwaveWidth * 0.5f + 0.18f)) {
        // One contact per ring, including a successful jump over it.
        wave.hitPlayer = true;
        if (actions == null || actions.getJumpHeight() < 0.2f) damagePlayer(config.shockwaveDamage);
      }
    }
    shockwaves.removeIf(wave -> wave.radius >= wave.maxRadius);
    previousPlayerPosition.set(player);
  }

  /** Tests the moving player's path against a moving ring edge, not the enclosed disk. */
  static boolean sweptRingContact(
      Vector2 from, Vector2 to, Vector2 centre, float startRadius, float endRadius, float padding) {
    if (Math.abs(from.dst(centre) - startRadius) <= padding
        || Math.abs(to.dst(centre) - endRadius) <= padding) return true;
    Vector2 relative = from.cpy().sub(centre);
    Vector2 movement = to.cpy().sub(from);
    float growth = endRadius - startRadius;
    return crossesRingBoundary(relative, movement, startRadius + padding, growth)
        || crossesRingBoundary(relative, movement, startRadius - padding, growth);
  }

  private static boolean crossesRingBoundary(
      Vector2 relative, Vector2 movement, float radius, float growth) {
    float a = movement.len2() - growth * growth;
    float b = 2f * (relative.dot(movement) - radius * growth);
    float c = relative.len2() - radius * radius;
    if (Math.abs(a) < 0.000001f) {
      if (Math.abs(b) < 0.000001f) return Math.abs(c) < 0.000001f && radius >= 0f;
      return ringRoot(-c / b, radius, growth);
    }
    float discriminant = b * b - 4f * a * c;
    if (discriminant < 0f) return false;
    float root = (float) Math.sqrt(discriminant);
    return ringRoot((-b - root) / (2f * a), radius, growth)
        || ringRoot((-b + root) / (2f * a), radius, growth);
  }

  private static boolean ringRoot(float time, float radius, float growth) {
    return time >= 0f && time <= 1f && radius + time * growth >= 0f;
  }

  private void setJumpEnabled(boolean enabled) {
    PlayerActions actions = target.getComponent(PlayerActions.class);
    if (actions != null) actions.setJumpEnabled(this, enabled);
  }

  /** The floor point under a sprite, aligned with the player's bottom collider. */
  static Vector2 groundPosition(Entity actor) {
    Vector2 position = actor.getPosition();
    Vector2 size = actor.getScale();
    return position.add(size.x * 0.5f, size.y * 0.15f);
  }

  private void clearStatueCombat() {
    shockwaves.clear();
    setJumpEnabled(false);
    if (ServiceLocator.getRenderService() != null)
      ServiceLocator.getRenderService().clearShake(this);
    for (Statue statue : statues) {
      if (!statue.broken) stop(statue.entity);
      statue.jumpHeight = 0f;
      statue.airborne = false;
      statue.warningRemaining = 0f;
      statue.evadePending = false;
    }
  }

  private void damagePlayer(int damage) {
    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    if (playerStats == null) return;
    int before = playerStats.getHealth();
    playerStats.takeDamage(damage, entity);
    if (playerStats.getHealth() < before) playerHitRemaining = 0.24f;
  }

  private void finishEncounter() {
    entity.getComponent(PhysicsComponent.class).getBody().setActive(true);
    protection.enableShield();
    burst(entity.getCenterPosition(), false);
    changeState(FinalBossStageThreeState.PEACEFUL);
    phases.completeStage(FinalBossPhase.STAGE_THREE);
    // Room completion is independent of health: Grandpa remains alive and invulnerable.
    entity.getEvents().trigger("finalBossEncounterCompleted");
  }

  private void changeState(FinalBossStageThreeState next) {
    state = next;
    stateTime = 0f;
    entity.getEvents().trigger(STATE_CHANGED, next);
  }

  private void burst(Vector2 position, boolean grey) {
    bursts.add(new Burst(position.cpy(), grey));
  }

  private Rectangle bounds() {
    Camera camera = entity.getComponent(FinalBossMovementComponent.class).getCamera();
    if (camera == null)
      return new Rectangle(
          target.getCenterPosition().x - 8f, target.getCenterPosition().y - 4f, 16f, 8f);
    float zoom = camera instanceof OrthographicCamera ortho ? ortho.zoom : 1f;
    float width = camera.viewportWidth * zoom;
    float height = camera.viewportHeight * zoom;
    return new Rectangle(
        camera.position.x - width / 2f, camera.position.y - height / 2f, width, height);
  }

  private Vector2 safePosition(Vector2 requested, Vector2 size) {
    return safePosition(requested, size, 0f);
  }

  private Vector2 statueClearanceSize(Vector2 size) {
    return new Vector2(Math.max(1.6f, size.x), size.y);
  }

  private Vector2 clampStatue(Vector2 requested, Vector2 size) {
    return clamp(requested, size, config.statueJumpHeight + 1.15f);
  }

  private Vector2 statueSpawnPosition(Vector2 requested, Vector2 statueSize) {
    Vector2 size = statueClearanceSize(statueSize);
    Vector2 preferred = clampStatue(requested, size);
    float spacing = config.statueMinSpacing + STATUE_SPACING_BUFFER;
    if (clearSpace(preferred, size) && nearestStatueDistanceSquared(preferred) >= spacing * spacing)
      return preferred;

    Rectangle area = bounds();
    Vector2 min = clampStatue(new Vector2(area.x, area.y), size);
    Vector2 max = clampStatue(new Vector2(area.x + area.width, area.y + area.height), size);
    int columns = Math.max(1, MathUtils.ceil((max.x - min.x) / 0.35f));
    int rows = Math.max(1, MathUtils.ceil((max.y - min.y) / 0.35f));
    Vector2 bestClear = clearSpace(preferred, size) ? preferred : null;
    float bestClearDistance = bestClear == null ? -1f : nearestStatueDistanceSquared(bestClear);
    Vector2 bestFallback = preferred;
    float bestFallbackDistance = nearestStatueDistanceSquared(preferred);
    // Search the entire usable arena, so a crowded ellipse does not collapse onto the boss.
    for (int column = 0; column <= columns; column++) {
      for (int row = 0; row <= rows; row++) {
        Vector2 candidate =
            new Vector2(
                MathUtils.lerp(min.x, max.x, (float) column / columns),
                MathUtils.lerp(min.y, max.y, (float) row / rows));
        float distance = nearestStatueDistanceSquared(candidate);
        if (distance > bestFallbackDistance) {
          bestFallback = candidate;
          bestFallbackDistance = distance;
        }
        if (clearSpace(candidate, size)
            && (distance > bestClearDistance
                || (distance == bestClearDistance
                    && bestClear != null
                    && candidate.dst2(preferred) < bestClear.dst2(preferred)))) {
          bestClear = candidate;
          bestClearDistance = distance;
        }
      }
    }
    // An undersized arena still receives the most separated available positions.
    return bestClear == null ? bestFallback : bestClear;
  }

  private float nearestStatueDistanceSquared(Vector2 point) {
    float nearest = Float.MAX_VALUE;
    for (Statue other : statues) {
      if (!other.broken) nearest = Math.min(nearest, point.dst2(other.entity.getCenterPosition()));
    }
    return nearest;
  }

  private Vector2 safePosition(Vector2 requested, Vector2 size, float topInset) {
    Vector2 candidate = clamp(requested, size, topInset);
    if (clearSpace(candidate, size)) return candidate;
    for (int ring = 1; ring <= 8; ring++) {
      for (int direction = 0; direction < 12; direction++) {
        Vector2 offset = new Vector2(ring * 0.35f, 0f).setAngleDeg(direction * 30f);
        candidate = clamp(requested.cpy().add(offset), size, topInset);
        if (clearSpace(candidate, size)) return candidate;
      }
    }
    return clamp(entity.getCenterPosition(), size, topInset);
  }

  private boolean clearSpace(Vector2 centre, Vector2 size) {
    boolean[] clear = {true};
    ServiceLocator.getPhysicsService()
        .getPhysics()
        .getWorld()
        .QueryAABB(
            fixture -> {
              if (!fixture.isSensor() && fixture.getBody().getType() == BodyType.StaticBody) {
                clear[0] = false;
                return false;
              }
              return true;
            },
            centre.x - size.x / 2f,
            centre.y - size.y / 2f,
            centre.x + size.x / 2f,
            centre.y + size.y / 2f);
    return clear[0];
  }

  private float wallFraction(Vector2 from, Vector2 to) {
    if (from.epsilonEquals(to, 0.0001f)) return 1f;
    float[] closest = {1f};
    ServiceLocator.getPhysicsService()
        .getPhysics()
        .getWorld()
        .rayCast(
            (fixture, point, normal, fraction) -> {
              if (fixture.isSensor() || fixture.getBody().getType() != BodyType.StaticBody)
                return -1f;
              closest[0] = Math.min(closest[0], fraction);
              return fraction;
            },
            from,
            to);
    return closest[0];
  }

  private Vector2 clamp(Vector2 centre, Vector2 size) {
    return clamp(centre, size, 0f);
  }

  private Vector2 clamp(Vector2 centre, Vector2 size, float topInset) {
    Rectangle area = bounds();
    float insetX = Math.min(area.width / 2f, size.x / 2f + 0.2f);
    float insetY = Math.min(area.height / 2f, size.y / 2f + 0.2f);
    float minY = area.y + insetY;
    float maxY = Math.max(minY, area.y + area.height - Math.max(insetY, topInset));
    return new Vector2(
        MathUtils.clamp(centre.x, area.x + insetX, area.x + area.width - insetX),
        MathUtils.clamp(centre.y, minY, maxY));
  }

  private static void moveTowards(Entity actor, Vector2 centre, float speed) {
    PhysicsMovementComponent movement = actor.getComponent(PhysicsMovementComponent.class);
    movement.setMaxSpeed(new Vector2(speed, speed));
    movement.setTarget(centre.cpy().sub(actor.getScale().scl(0.5f)));
    movement.setMoving(true);
  }

  private static void stop(Entity actor) {
    PhysicsMovementComponent movement = actor.getComponent(PhysicsMovementComponent.class);
    if (movement != null) movement.setMoving(false);
  }

  @Override
  public void dispose() {
    disposed = true;
    clearFreeze();
    bolts.clear();
    clearStatueCombat();
    for (Statue statue : statues) {
      if (!statue.broken && ServiceLocator.getEntityService() != null)
        ServiceLocator.getEntityService().scheduleDisposal(statue.entity);
    }
  }

  static final class Bolt {
    final Vector2 position;
    final Vector2 velocity;
    float elapsed;

    Bolt(Vector2 position, Vector2 velocity) {
      this.position = position;
      this.velocity = velocity;
    }
  }

  static final class Statue {
    final Entity entity;
    final Set<Entity> attacks = new HashSet<>();
    int hitsRemaining;
    int hitsSinceEvade;
    boolean evadePending;
    float evadeRetryRemaining;
    boolean broken;
    Vector2 position;
    Vector2 destination;
    float pauseRemaining;
    float moveRemaining;
    float hitRemaining;
    float slamCooldown;
    float warningRemaining;
    float jumpElapsed;
    float jumpHeight;
    boolean airborne;

    Statue(Entity entity, int hits) {
      this.entity = entity;
      hitsRemaining = hits;
    }
  }

  static final class Shockwave {
    final Vector2 position;
    final float maxRadius;
    float radius;
    boolean hitPlayer;

    Shockwave(Vector2 position, float maxRadius) {
      this.position = position.cpy();
      this.maxRadius = maxRadius;
    }
  }

  static final class Burst {
    final Vector2 position;
    final boolean grey;
    boolean ice;
    float elapsed;

    Burst(Vector2 position, boolean grey) {
      this.position = position;
      this.grey = grey;
    }
  }
}
