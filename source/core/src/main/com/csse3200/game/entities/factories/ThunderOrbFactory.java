package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.miniboss.dragon.ThunderOrbHitComponent;
import com.csse3200.game.components.miniboss.dragon.ThunderOrbMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Creates thunder orbs with tracking movement and single-hit damage. */
public class ThunderOrbFactory {
  public static final String ATLAS_PATH = "images/dragon/thunder-orb.atlas";

  private static final float SPEED = 3f;
  private static final float TURN_SPEED = 90f;
  private static final float LIFETIME = 3f;
  private static final int DAMAGE = 10;
  private static final float FRAME_DURATION = 0.085f;
  private static final float VISUAL_SIZE = 2f;
  private static final float HITBOX_SIZE = 0.4f;

  private ThunderOrbFactory() {}

  /**
   * Creates an unregistered thunder orb.
   *
   * @param center spawn position of the orb's centre in world coordinates
   * @param target entity to track
   * @return the orb, ready for registration by the caller
   */
  public static Entity createThunderOrb(Vector2 center, Entity target) {
    if (center == null
        || !Float.isFinite(center.x)
        || !Float.isFinite(center.y)
        || target == null) {
      throw new IllegalArgumentException("A finite centre and target are required");
    }

    TextureAtlas atlas =
        ServiceLocator.getResourceService().getAsset(ATLAS_PATH, TextureAtlas.class);

    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("fly", FRAME_DURATION, Animation.PlayMode.LOOP);
    animator.addAnimation(
        ThunderOrbHitComponent.IMPACT_ANIMATION, FRAME_DURATION, Animation.PlayMode.NORMAL);

    Entity orb =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new ThunderOrbMovementComponent(target, SPEED, TURN_SPEED, LIFETIME))
            .addComponent(new ThunderOrbHitComponent(DAMAGE))
            .addComponent(animator);

    orb.setScale(VISUAL_SIZE, VISUAL_SIZE);
    orb.setPosition(center.x - VISUAL_SIZE / 2f, center.y - VISUAL_SIZE / 2f);
    orb.getComponent(HitboxComponent.class)
        .setAsBox(
            new Vector2(HITBOX_SIZE, HITBOX_SIZE), new Vector2(VISUAL_SIZE / 2f, VISUAL_SIZE / 2f));

    animator.startAnimation("fly");
    return orb;
  }
}
