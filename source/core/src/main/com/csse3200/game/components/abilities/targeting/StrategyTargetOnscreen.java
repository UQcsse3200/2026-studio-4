package com.csse3200.game.components.abilities.targeting;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;

/**
 * Selects enemies whose centre currently falls within the camera's viewport. Uses {@link
 * StrategyTargetAll} to get the room's enemy pool, then filters by an axis-aligned bounds check
 * against the camera.
 */
public class StrategyTargetOnscreen implements EnemyTargetingStrategy {
  private final CameraComponent cameraComponent;
  private final EnemyTargetingStrategy candidateSource = new StrategyTargetAll();

  /**
   * @param cameraComponent the active camera to test enemy visibility against
   * @require cameraComponent != null
   * @throws IllegalArgumentException if cameraComponent is null
   */
  public StrategyTargetOnscreen(CameraComponent cameraComponent) {
    if (cameraComponent == null) {
      throw new IllegalArgumentException("cameraComponent must not be null");
    }
    this.cameraComponent = cameraComponent;
  }

  @Override
  public Array<Entity> selectTargets(Entity caster) {
    Camera camera = cameraComponent.getCamera();
    Vector2 cameraCenter = new Vector2(camera.position.x, camera.position.y);
    Vector2 halfViewport = cameraComponent.getCameraSize().scl(0.5f);

    Array<Entity> onScreen = new Array<>();
    for (Entity candidate : candidateSource.selectTargets(caster)) {
      Vector2 offset = candidate.getCenterPosition().sub(cameraCenter);
      if (Math.abs(offset.x) <= halfViewport.x && Math.abs(offset.y) <= halfViewport.y) {
        onScreen.add(candidate);
      }
    }
    return onScreen;
  }
}
