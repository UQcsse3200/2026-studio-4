package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/** Fixes the Stage 2 view and keeps both combatants inside its visible area. */
public class FinalBossStageTwoArenaComponent extends Component {
  private final Entity target;
  private final FinalBossStageTwoConfig config;
  private FinalBossPhaseControllerComponent phases;
  private Camera camera;
  private FollowingCameraComponent followingCamera;
  private Rectangle originalBounds;
  private Rectangle bounds;
  private boolean defeated;
  private boolean disposed;

  public FinalBossStageTwoArenaComponent(Entity target, FinalBossStageTwoConfig config) {
    if (target == null || config == null)
      throw new IllegalArgumentException("Stage 2 arena target and config must not be null");
    config.validate();
    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    phases = entity.getComponent(FinalBossPhaseControllerComponent.class);
    if (phases == null)
      throw new IllegalStateException("Stage 2 arena requires a phase controller");
    entity.getEvents().addListener(FinalBossEvents.PHASE_CHANGED, this::phaseChanged);
    entity.getEvents().addListener("entityDied", this::combatantDied);
  }

  /** Supplies the room camera; a null follower is supported for an already stationary camera. */
  public void setCamera(Camera camera, FollowingCameraComponent followingCamera) {
    if (this.camera == camera && this.followingCamera == followingCamera) return;
    deactivate();
    this.camera = camera;
    this.followingCamera = followingCamera;
  }

  @Override
  public void update() {
    if (!disposed && (isDead(entity) || isDead(target))) defeated = true;
    if (disposed || defeated || phases.getCurrentPhase() != FinalBossPhase.STAGE_TWO) {
      deactivate();
      return;
    }
    Rectangle visible = visibleBounds();
    if (visible == null) {
      deactivate();
      return;
    }
    if (originalBounds == null) {
      // Phase changes can occur inside collision callbacks. Locking and containment start here,
      // after physics, rather than moving either body from the phase-change event listener.
      if (followingCamera != null && !followingCamera.lockPosition(this)) return;
      visible = visibleBounds();
      originalBounds = visible;
    }
    bounds = intersectVisibleArea(visible);
    clampActor(entity);
    clampActor(target);
    if (ServiceLocator.getEntityService() != null)
      ServiceLocator.getEntityService().runAfterUpdate(this::constrainAfterUpdates);
  }

  private void constrainAfterUpdates() {
    if (disposed || defeated || bounds == null
        || phases.getCurrentPhase() != FinalBossPhase.STAGE_TWO
        || isDead(entity) || isDead(target)) return;
    // PlayerActions may update after the boss and set an outward walk or dash velocity again.
    // Finish containment once all actors have updated, before the next physics step.
    clampActor(entity);
    clampActor(target);
  }

  /** Returns a copy of the fixed visible area, or null outside an active arena. */
  public Rectangle getBounds() {
    return bounds == null ? null : new Rectangle(bounds);
  }

  /** Returns the safe range of this actor's lower-left position, including its entire sprite. */
  public Rectangle getMovementBounds(Entity actor) {
    if (bounds == null) return null;
    Vector2 halfSize = actor.getScale().scl(0.5f);
    float insetX = Math.min(bounds.width * 0.5f, halfSize.x + config.arenaMargin);
    float insetY = Math.min(bounds.height * 0.5f, halfSize.y + config.arenaMargin);
    return new Rectangle(
        bounds.x + insetX - halfSize.x,
        bounds.y + insetY - halfSize.y,
        Math.max(0f, bounds.width - 2f * insetX),
        Math.max(0f, bounds.height - 2f * insetY));
  }

  private Rectangle visibleBounds() {
    if (camera == null) return null;
    float zoom = camera instanceof OrthographicCamera ortho ? ortho.zoom : 1f;
    if (!Float.isFinite(zoom) || zoom <= 0f
        || camera.viewportWidth <= 0f || camera.viewportHeight <= 0f) return null;
    float width = camera.viewportWidth * zoom;
    float height = camera.viewportHeight * zoom;
    if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0f || height <= 0f
        || !Float.isFinite(camera.position.x) || !Float.isFinite(camera.position.y)) return null;
    return new Rectangle(
        camera.position.x - width * 0.5f, camera.position.y - height * 0.5f, width, height);
  }

  private Rectangle intersectVisibleArea(Rectangle visible) {
    float left = Math.max(originalBounds.x, visible.x);
    float bottom = Math.max(originalBounds.y, visible.y);
    float right = Math.min(originalBounds.x + originalBounds.width, visible.x + visible.width);
    float top = Math.min(originalBounds.y + originalBounds.height, visible.y + visible.height);
    // A smaller window shrinks the usable area, but widening it never expands the original arena.
    if (right > left && top > bottom) return new Rectangle(left, bottom, right - left, top - bottom);
    return new Rectangle(originalBounds);
  }

  private void clampActor(Entity actor) {
    PhysicsComponent physics = actor.getComponent(PhysicsComponent.class);
    Body body = physics == null ? null : physics.getBody();
    if (body != null) {
      if (body.getWorld().isLocked()) return;
      // EntityService refreshes physics per entity, so the player may not have updated yet.
      physics.earlyUpdate();
    }
    Rectangle movementBounds = getMovementBounds(actor);
    Vector2 position = actor.getPosition();
    float right = movementBounds.x + movementBounds.width;
    float top = movementBounds.y + movementBounds.height;
    float x = MathUtils.clamp(position.x, movementBounds.x, right);
    float y = MathUtils.clamp(position.y, movementBounds.y, top);
    if (position.x != x || position.y != y) actor.setPosition(x, y);
    if (body == null) return;
    Vector2 velocity = body.getLinearVelocity();
    float vx = velocity.x;
    float vy = velocity.y;
    if ((x <= movementBounds.x && vx < 0f) || (x >= right && vx > 0f)) vx = 0f;
    if ((y <= movementBounds.y && vy < 0f) || (y >= top && vy > 0f)) vy = 0f;
    if (vx != velocity.x || vy != velocity.y) body.setLinearVelocity(vx, vy);
  }

  private static boolean isDead(Entity actor) {
    CombatStatsComponent stats = actor.getComponent(CombatStatsComponent.class);
    return stats != null && stats.isDead();
  }

  private void phaseChanged(FinalBossPhase phase) {
    if (phase != FinalBossPhase.STAGE_TWO) deactivate();
  }

  private void combatantDied() {
    defeated = true;
    deactivate();
  }

  private void deactivate() {
    if (followingCamera != null) followingCamera.unlockPosition(this);
    originalBounds = null;
    bounds = null;
  }

  @Override
  public void dispose() {
    disposed = true;
    // Bodies may already have been disposed earlier in Entity's component disposal order.
    deactivate();
  }
}
