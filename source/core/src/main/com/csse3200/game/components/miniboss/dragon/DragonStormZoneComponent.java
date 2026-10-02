package com.csse3200.game.components.miniboss.dragon;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/** Fixed storm warnings followed by one damage attempt per zone. */
public class DragonStormZoneComponent extends Component {
  public static final String ATTACK_STARTED = "dragonStormZoneStarted";
  public static final float RADIUS = 1f;
  private static final float WARNING_TIME = 1f;
  private static final float SECOND_ZONE_DELAY = 0.35f;
  private static final float FLASH_TIME = 0.2f;
  private static final float COOLDOWN = 2.5f;
  private static final int DAMAGE = 10;

  /** Immutable data for a renderer; progress is warning completion from zero to one. */
  public record ZoneView(float x, float y, float progress, boolean struck) {}

  private static class Zone {
    private final Vector2 centre;
    private float remaining = WARNING_TIME;
    private boolean struck;

    private Zone(Vector2 centre) {
      this.centre = centre;
    }
  }

  private final Entity target;
  private final List<Zone> zones = new ArrayList<>();
  private CombatStatsComponent stats;
  private CombatStatsComponent targetStats;
  private DragonPhaseComponent phase;
  private boolean secondPending;
  private boolean stopped;
  private float secondRemaining;
  private float cooldownReamining;

  public DragonStormZoneComponent(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Target is required");
    }
    this.target = target;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    targetStats = target.getComponent(CombatStatsComponent.class);
    phase = entity.getComponent(DragonPhaseComponent.class);
    if (stats == null || targetStats == null || phase == null) {
      throw new IllegalStateException("Storm requires owner/target stats and dragon phase");
    }
    entity.getEvents().addListener("entityDied", this::stop);
  }

  /** Phase one creates one zone; phase two schedules a second separately warned zone. */
  public boolean tryAttack() {
    if (!canAct() || isBusy() || cooldownReamining > 0f) {
      return false;
    }
    zones.add(new Zone(target.getCenterPosition().cpy()));
    secondPending = phase.isEnraged();
    secondRemaining = SECOND_ZONE_DELAY;
    cooldownReamining = COOLDOWN;
    entity.getEvents().trigger(ATTACK_STARTED);
    return true;
  }

  private boolean canAct() {
    return !stopped
        && stats != null
        && !stats.isDead()
        && targetStats != null
        && !targetStats.isDead();
  }

  public boolean isBusy() {
    return secondPending || !zones.isEmpty();
  }

  public List<ZoneView> getZones() {
    List<ZoneView> result = new ArrayList<>();
    for (Zone zone : zones) {
      float progress = zone.struck ? 1f : 1f - zone.remaining / WARNING_TIME;
      result.add(new ZoneView(zone.centre.x, zone.centre.y, progress, zone.struck));
    }
    return List.copyOf(result);
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  public void update(float delta) {
    if (!canAct()) {
      stop();
      return;
    }
    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }
    cooldownReamining = Math.max(0f, cooldownReamining - delta);
    for (Zone zone : new ArrayList<>(zones)) {
      if (!canAct()) {
        stop();
        return;
      }
      advanceZone(zone, delta);
    }
    if (!canAct()) {
      stop();
      return;
    }
    updateSecondZone(delta);
  }

  private void advanceZone(Zone zone, float delta) {
    zone.remaining = Math.max(0f, zone.remaining - delta);
    if (zone.remaining > 0f) {
      return;
    }
    if (zone.struck) {
      zones.remove(zone);
      return;
    }
    zone.struck = true;
    zone.remaining = FLASH_TIME;
    if (overlapsTarget(zone.centre) && !StatusEffectsControllerComponent.isConcealed(target)) {
      targetStats.takeDamage(DAMAGE, entity);
    }
  }

  private void updateSecondZone(float delta) {
    if (!secondPending) {
      return;
    }
    secondRemaining -= delta;
    if (secondRemaining <= 0f) {
      secondPending = false;
      zones.add(new Zone(target.getCenterPosition().cpy()));
    }
  }

  private boolean overlapsTarget(Vector2 centre) {
    Vector2 position = target.getPosition();
    Vector2 size = target.getScale();
    float nearestX = MathUtils.clamp(centre.x, position.x, position.x + size.x);
    float nearestY = MathUtils.clamp(centre.y, position.y, position.y + size.y);
    return centre.dst2(nearestX, nearestY) <= RADIUS * RADIUS;
  }

  public void stop() {
    stopped = true;
    secondPending = false;
    zones.clear();
  }

  @Override
  public void dispose() {
    stop();
    super.dispose();
  }
}
