package com.csse3200.game.components.items;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.KeyboardPlayerInputComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pulls a snapshot of the current room's dropped items after a completed E hold. */
public class RoomMagneticPickupComponent extends Component {
  public static final String RESET_EVENT = "roomPickupReset";
  private static final float PULL_SPEED = 8f;
  private static final float COLLECT_DISTANCE = 0.3f;
  private final Set<Entity> targets = new LinkedHashSet<>();

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(KeyboardPlayerInputComponent.ROOM_MAGNETIC_PICKUP, this::capture);
    entity.getEvents().addListener(RESET_EVENT, targets::clear);
  }

  /**
   * Captures only the drops registered when the hold completes. Later drops require another hold;
   * releasing E does not cancel this batch. Another completed hold can add to an unfinished batch.
   */
  private void capture() {
    EntityService entities = ServiceLocator.getEntityService();
    if (entities == null || isPlayerDead()) {
      return;
    }
    // RoomSelector keeps only the current room loaded and resets this batch before changing rooms.
    // EntityService is already iterating this libGDX Array when the input completes its hold.
    for (int i = 0; i < entities.getEntities().size; i++) {
      Entity candidate = entities.getEntities().get(i);
      ItemComponent item = candidate.getComponent(ItemComponent.class);
      if (candidate != entity && item != null && !item.isCollected()) {
        targets.add(candidate);
      }
    }
  }

  @Override
  public void update() {
    if (isPlayerDead()) {
      targets.clear();
      return;
    }
    EntityService entities = ServiceLocator.getEntityService();
    if (entities == null || entities.isFrozen()) {
      return;
    }
    float step = PULL_SPEED * ServiceLocator.getTimeSource().getDeltaTime();
    if (step <= 0f) {
      return;
    }
    pullTargets(entities, step);
  }

  private void pullTargets(EntityService entities, float step) {
    Vector2 destination = entity.getCenterPosition();
    Iterator<Entity> iterator = targets.iterator();
    while (iterator.hasNext()) {
      Entity target = iterator.next();
      ItemComponent item = target.getComponent(ItemComponent.class);
      if (!entities.getEntities().contains(target, true) || item == null || item.isCollected()) {
        iterator.remove();
        continue;
      }
      Vector2 offset = destination.cpy().sub(target.getCenterPosition());
      float distance = offset.len();
      if (distance <= COLLECT_DISTANCE || distance <= step) {
        if (item.tryPickUp(entity)) {
          entities.scheduleDisposal(target);
        }
        iterator.remove();
      } else {
        PhysicsComponent physics = target.getComponent(PhysicsComponent.class);
        if (physics != null) {
          physics.getBody().setLinearVelocity(Vector2.Zero);
        }
        target.setPosition(target.getPosition().add(offset.scl(step / distance)));
      }
    }
  }

  private boolean isPlayerDead() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    return stats != null && stats.getHealth() <= 0;
  }

  @Override
  public void dispose() {
    targets.clear();
  }
}
