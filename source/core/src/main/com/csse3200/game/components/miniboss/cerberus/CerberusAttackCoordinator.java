package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Coordinates sequential attacks in phase one and concurrent attacks in phase two. */
public class CerberusAttackCoordinator {
  private final Entity[] heads;
  private final CerberusPhaseComponent phase;
  private final Map<Entity, BooleanSupplier> readiness = new IdentityHashMap<>();
  private final Set<Entity> attacking = Collections.newSetFromMap(new IdentityHashMap<>());

  private Entity activeHead;
  private int nextIndex;

  public CerberusAttackCoordinator(
      Entity leftHead, Entity middleHead, Entity rightHead, CerberusPhaseComponent phase) {
    heads =
        new Entity[] {
          Objects.requireNonNull(leftHead),
          Objects.requireNonNull(middleHead),
          Objects.requireNonNull(rightHead)
        };
    this.phase = Objects.requireNonNull(phase);
  }

  /** Registers a check for cooldown, target range and other skill conditions. */
  public void register(Entity head, BooleanSupplier canAttack) {
    if (indexOf(head) < 0) {
      throw new IllegalArgumentException("Head does not belong to this Cerberus");
    }
    readiness.put(head, Objects.requireNonNull(canAttack));
  }

  /** Returns true when this head is allowed to begin an attack. */
  public boolean tryStart(Entity head) {
    if (!isReady(head)) {
      return false;
    }
    if (phase.getCurrentPhase() == 2) {
      attacking.add(head);
      return true;
    }

    if (activeHead != null) {
      if (isAlive(activeHead)) {
        return false;
      }
      finish(activeHead);
    }

    Entity nextHead = findNextReadyHead();
    if (nextHead != head) {
      return false;
    }

    activeHead = head;
    attacking.add(head);
    return true;
  }

  /** Releases an attack when it ends or is cancelled. */
  public void finish(Entity head) {
    attacking.remove(head);

    if (activeHead == head && activeHead != null) {
      nextIndex = (indexOf(head) + 1) % heads.length;
      activeHead = null;
    }
  }

  private Entity findNextReadyHead() {
    for (int offset = 0; offset < heads.length; offset++) {
      Entity candidate = heads[(nextIndex + offset) % heads.length];
      if (isReady(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private boolean isReady(Entity head) {
    BooleanSupplier check = readiness.get(head);
    return check != null && !attacking.contains(head) && isAlive(head) && check.getAsBoolean();
  }

  private boolean isAlive(Entity head) {
    CombatStatsComponent stats = head.getComponent(CombatStatsComponent.class);
    return stats != null && !stats.isDead();
  }

  private int indexOf(Entity head) {
    for (int index = 0; index < heads.length; index++) {
      if (heads[index] == head) {
        return index;
      }
    }
    return -1;
  }
}
