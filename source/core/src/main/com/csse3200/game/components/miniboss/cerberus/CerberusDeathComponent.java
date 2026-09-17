package com.csse3200.game.components.miniboss.cerberus;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

public class CerberusDeathComponent extends Component {
  private final Entity leftHead;
  private final Entity rightHead;

  private boolean middleDead;
  private boolean leftDead;
  private boolean rightDead;
  private boolean disposalScheduled;

  private static final String ENTITY_DIED = "entityDied";

  public CerberusDeathComponent(Entity leftHead, Entity rightHead) {
    this.leftHead = leftHead;
    this.rightHead = rightHead;
  }

  @Override
  public void create() {
    entity.getEvents().addListener(ENTITY_DIED, this::onMiddleHeadDied);
    leftHead.getEvents().addListener(ENTITY_DIED, this::onLeftHeadDied);
    rightHead.getEvents().addListener(ENTITY_DIED, this::onRightHeadDied);
    leftDead = leftHead.getComponent(CombatStatsComponent.class).isDead();
    rightDead = rightHead.getComponent(CombatStatsComponent.class).isDead();

    if (entity.getComponent(CombatStatsComponent.class).isDead()) {
      onMiddleHeadDied();
    } else {
      disposeBodyIfAllHeadsDead();
    }
  }

  private void onMiddleHeadDied() {
    if (middleDead) {
      return;
    }
    middleDead = true;

    entity.getComponent(CombatStatsComponent.class).setBaseAttack(0);

    ServiceLocator.getEntityService()
        .schedule(
            () -> {
              if (!disposalScheduled) {
                PhysicsMovementComponent movement =
                    entity.getComponent(PhysicsMovementComponent.class);
                if (movement != null) {
                  movement.setMoving(false);
                }
              }
            });

    disposeBodyIfAllHeadsDead();
  }

  private void onLeftHeadDied() {
    leftDead = true;
    disposeBodyIfAllHeadsDead();
  }

  private void onRightHeadDied() {
    rightDead = true;
    disposeBodyIfAllHeadsDead();
  }

  private void disposeBodyIfAllHeadsDead() {
    if (disposalScheduled || !middleDead || !leftDead || !rightDead) {
      return;
    }

    disposalScheduled = true;
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }
}
