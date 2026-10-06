package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionIndicatorComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

public class FriendlyNpcFactory {
  private static final float ANIMATION_FRAME_DURATION = 0.1f;

  public static Entity createFriendlyNpc(InteractableNpcConfig config) {
    Entity npc = new Entity();
    float aspectRatio;
    if (config.atlas != null) {
      TextureAtlas atlas =
          ServiceLocator.getResourceService().getAsset(config.atlas, TextureAtlas.class);
      AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
      animator.addAnimation(config.animation, ANIMATION_FRAME_DURATION, Animation.PlayMode.LOOP);
      npc.addComponent(animator);
      TextureAtlas.AtlasRegion frame = atlas.findRegion(config.animation);
      aspectRatio = frame == null ? 1f : (float) frame.getRegionHeight() / frame.getRegionWidth();
    } else {
      Texture texture = ServiceLocator.getResourceService().getAsset(config.texture, Texture.class);
      npc.addComponent(new TextureRenderComponent(texture));
      aspectRatio = (float) texture.getHeight() / texture.getWidth();
    }

    npc.addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
        .addComponent(new ColliderComponent().setLayer(PhysicsLayer.FRIENDLY_NPC))
        .addComponent(new NpcInteractableComponent(config))
        .addComponent(new NpcInteractionIndicatorComponent());

    npc.setScale(config.width, config.width * aspectRatio);

    PhysicsUtils.setScaledCollider(npc, 0.6f, 0.25f);

    AnimationRenderComponent animator = npc.getComponent(AnimationRenderComponent.class);
    if (animator != null) {
      animator.startAnimation(config.animation);
    }

    return npc;
  }

  private FriendlyNpcFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
