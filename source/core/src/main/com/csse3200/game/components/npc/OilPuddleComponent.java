package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.OilPuddleFactory;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RadialTextureFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** A harmless oil patch that refreshes Oiled and explodes once when touched by fire. */
public class OilPuddleComponent extends RenderComponent {
  private static final int TEXTURE_SIZE = 64;

  private final Consumer<Entity> spawner;
  private HitboxComponent hitbox;
  private Texture texture;
  private boolean ignited;

  public OilPuddleComponent(Consumer<Entity> spawner) {
    this.spawner = spawner;
  }

  @Override
  public void create() {
    hitbox = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    super.create();
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (ignited || hitbox.getFixture() != me) {
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    Entity player = ((BodyUserData) other.getBody().getUserData()).entity;
    StatusEffectsControllerComponent effects =
        player.getComponent(StatusEffectsControllerComponent.class);
    if (effects != null) {
      effects.applyOiled(OilChaserComponent.OILED_DURATION);
    }
  }

  /** Creates one damaging fire burst at this puddle, then removes the puddle safely. */
  public void ignite() {
    if (ignited) {
      return;
    }
    ignited = true;
    var centre = entity.getCenterPosition().cpy();
    ServiceLocator.getEntityService()
        .schedule(() -> spawner.accept(OilPuddleFactory.createExplosion(centre)));
    ServiceLocator.getEntityService().scheduleDisposal(entity);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null) {
      texture = RadialTextureFactory.create(TEXTURE_SIZE, distance -> 0.8f * (1f - distance));
    }
    float oldColour = batch.getPackedColor();
    try {
      batch.setColor(0.12f, 0.08f, 0.03f, 0.82f);
      batch.draw(
          texture,
          entity.getPosition().x,
          entity.getPosition().y,
          entity.getScale().x,
          entity.getScale().y);
    } finally {
      batch.setPackedColor(oldColour);
    }
  }

  @Override
  public float getZIndex() {
    return super.getZIndex() - 0.02f;
  }

  @Override
  public void dispose() {
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    super.dispose();
  }
}
