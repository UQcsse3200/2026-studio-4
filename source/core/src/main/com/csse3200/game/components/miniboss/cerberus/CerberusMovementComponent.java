package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.services.ServiceLocator;

public class CerberusMovementComponent extends Component {
  private static final float SPEED = 2f;
  private static final float STOP_DISTANCE = 0.2f;
  private static final int CIRCLE_SEGMENTS = 48;

  private final Entity target;
  private final Vector2 anchor;
  private final float radius;

  private PhysicsMovementComponent physicsMovementComponent;
  private CombatStatsComponent combatStatsComponent;

  public CerberusMovementComponent(Entity target, Vector2 anchor, float radius) {
    this.target = target;
    this.anchor = anchor.cpy();
    this.radius = radius;
  }

  @Override
  public void create() {
    physicsMovementComponent = entity.getComponent(PhysicsMovementComponent.class);
    combatStatsComponent = entity.getComponent(CombatStatsComponent.class);
    physicsMovementComponent.setMaxSpeed(new Vector2(SPEED, SPEED));
  }

  @Override
  public void earlyUpdate() {
    if (combatStatsComponent.isDead() || target == null) {
      physicsMovementComponent.setMoving(false);
      return;
    }

    CerberusBiteComponent bite = entity.getComponent(CerberusBiteComponent.class);
    if (bite != null && bite.controlMovement(physicsMovementComponent)) {
      return;
    }

    physicsMovementComponent.setMaxSpeed(new Vector2(SPEED, SPEED));

    Vector2 destination =
        target.getPosition().sub(anchor).limit(Math.max(0f, radius - 0.5f)).add(anchor);
    physicsMovementComponent.setTarget(destination);
    physicsMovementComponent.setMoving(
        entity.getPosition().dst2(destination) > STOP_DISTANCE * STOP_DISTANCE);
  }

  @Override
  public void update() {
    drawDebugRange();
  }

  private void drawDebugRange() {
    if (ServiceLocator.getRenderService() == null) {
      return;
    }

    DebugRenderer debug = ServiceLocator.getRenderService().getDebug();
    if (debug == null || !debug.getActive()) {
      return;
    }

    debug.drawLine(anchor.cpy(), entity.getPosition(), Color.YELLOW, 2f);

    for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
      float firstAngle = 360f * i / CIRCLE_SEGMENTS;
      float secondAngle = 360f * (i + 1) / CIRCLE_SEGMENTS;

      Vector2 from = new Vector2(radius, 0f).rotateDeg(firstAngle).add(anchor);
      Vector2 to = new Vector2(radius, 0f).rotateDeg(secondAngle).add(anchor);

      debug.drawLine(from, to, Color.CYAN, 1f);
    }
  }
}
