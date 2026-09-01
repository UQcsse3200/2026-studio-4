package com.csse3200.game.entities;

import com.badlogic.gdx.utils.Array;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a global access point for entities to register themselves. This allows for iterating
 * over entities to perform updates each loop. All game entities should be registered here.
 *
 * <p>Avoid adding additional state here! Global access is often the easy but incorrect answer to
 * sharing data.
 */
public class EntityService {
  private static final Logger logger = LoggerFactory.getLogger(EntityService.class);
  private static final int INITIAL_CAPACITY = 16;

  private final Array<Entity> entities = new Array<>(false, INITIAL_CAPACITY);
  private final Array<Runnable> afterUpdateActions = new Array<>();
  private boolean updating;
  private final Array<Entity> pendingDisposal = new Array<>(false, INITIAL_CAPACITY);
  private final Array<Runnable> pendingTasks = new Array<>(false, INITIAL_CAPACITY);

  /**
   * Register a new entity with the entity service. The entity will be created and start updating.
   *
   * @param entity new entity.
   */
  public void register(Entity entity) {
    logger.debug("Registering {} in entity service", entity);
    entities.add(entity);
    entity.create();
  }

  /**
   * Unregister an entity with the entity service. The entity will be removed and stop updating.
   *
   * @param entity entity to be removed.
   */
  public void unregister(Entity entity) {
    logger.debug("Unregistering {} in entity service", entity);
    entities.removeValue(entity, true);
  }

  /**
   * Queue an entity for disposal once the current update has finished. Collision events are fired
   * while the physics world is locked, and a locked world cannot destroy the bodies and fixtures
   * that {@link Entity#dispose()} removes. Use this instead of {@link Entity#dispose()} whenever an
   * entity is removed in response to a collision.
   *
   * <p>The entity is disabled immediately, so it stops updating before it is disposed.
   *
   * @param entity entity to dispose, ignored if null or already queued.
   */
  public void scheduleDisposal(Entity entity) {
    if (entity == null || pendingDisposal.contains(entity, false)) {
      return;
    }
    logger.debug("Scheduling {} for disposal in entity service", entity);
    entity.setEnabled(false);
    pendingDisposal.add(entity);
  }

  /**
   * Queue a task to run once the current update has finished. Collision events are fired while the
   * physics world is locked, and a locked world cannot create or destroy bodies, fixtures, or
   * joints. Use this to defer such work (e.g. spawning a new entity) from a collision event until
   * it is safe.
   *
   * @param task task to run after the current update, ignored if null.
   */
  public void schedule(Runnable task) {
    if (task == null) {
      return;
    }
    logger.debug("Scheduling deferred task in entity service");
    pendingTasks.add(task);
  }

  /** Update all registered entities. Should only be called from the main game loop. */
  public void update() {
    updating = true;
    for (Entity entity : entities) {
      entity.earlyUpdate();
      entity.update();
    }
    updating = false;

    for (Runnable action : afterUpdateActions) {
      action.run();
    }
    afterUpdateActions.clear();
  }

  /** Runs an action after the current entity update has finished. */
  public void runAfterUpdate(Runnable action) {
    if (updating) {
      afterUpdateActions.add(action);
    } else {
      action.run();
      drainQueues();
    }
  }

  /**
   * Run every task queued by {@link #schedule(Runnable)}, then dispose every entity queued by
   * {@link #scheduleDisposal(Entity)}. Tasks run before disposals so that queued work can still
   * read entities that are about to be removed. Tasks and disposals may queue further work, so both
   * queues are drained until empty.
   */
  private void drainQueues() {
    while (pendingTasks.notEmpty() || pendingDisposal.notEmpty()) {
      while (pendingTasks.notEmpty()) {
        pendingTasks.removeIndex(0).run();
      }
      while (pendingDisposal.notEmpty()) {
        pendingDisposal.pop().dispose();
      }
    }
  }

  /** Dispose all entities. */
  public void dispose() {
    for (Entity entity : entities) {
      entity.dispose();
    }
  }
}
