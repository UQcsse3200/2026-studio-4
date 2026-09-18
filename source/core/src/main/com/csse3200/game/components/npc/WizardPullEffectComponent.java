package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Show the purple vortex only when wizard is pulling player. */
public class WizardPullEffectComponent extends Component {
  private static final float EFFECT_SIZE = 7.5f;
  private final TextureAtlas atlas;
  private Entity effect;

  public WizardPullEffectComponent(TextureAtlas atlas) {
    this.atlas = atlas;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("wizardPullStart", this::startEffect);
    entity.getEvents().addListener("wizardPullStop", this::stopEffect);
    entity.getEvents().addListener("enemyDeathAnimation", this::stopEffect);
  }

  @Override
  public void update() {
    if (effect != null) {
      putEffectAtWizard();
    }
  }

  @Override
  public void dispose() {
    stopEffect();
  }

  private void startEffect() {
    stopEffect();
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("vortex", 0.12f, Animation.PlayMode.LOOP_PINGPONG);
    effect = new Entity().addComponent(animator);
    effect.setScale(EFFECT_SIZE, EFFECT_SIZE);
    putEffectAtWizard();
    ServiceLocator.getEntityService().register(effect);
    animator.startAnimation("vortex");
  }

  private void stopEffect() {
    if (effect != null) {
      ServiceLocator.getEntityService().scheduleDisposal(effect);
      effect = null;
    }
  }

  private void putEffectAtWizard() {
    Vector2 center = entity.getCenterPosition();
    effect.setPosition(center.x - EFFECT_SIZE / 2f, center.y - EFFECT_SIZE / 2f);
  }
}
