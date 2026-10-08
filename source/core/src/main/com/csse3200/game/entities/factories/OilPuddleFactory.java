package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.npc.FireBurstRenderComponent;
import com.csse3200.game.components.npc.FireHitEffectComponent;
import com.csse3200.game.components.npc.OilPuddleComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import java.util.function.Consumer;

/** Creates oil puddles and the short-lived blast made when one ignites. */
public final class OilPuddleFactory {
  private static final Vector2 PUDDLE_SIZE = new Vector2(1.8f, 1.2f);
  private static final Vector2 EXPLOSION_SIZE = new Vector2(2.8f, 2.8f);

  public static Entity createPuddle(Vector2 centre, Consumer<Entity> spawner) {
    HitboxSpec spec =
        new HitboxSpec()
            .position(bottomLeft(centre, PUDDLE_SIZE))
            .size(PUDDLE_SIZE)
            .lifetime(9f)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer(PhysicsLayer.PLAYER)
            .damage(0);
    return HitboxFactory.createHitbox(spec).addComponent(new OilPuddleComponent(spawner));
  }

  public static Entity createExplosion(Vector2 centre) {
    HitboxSpec spec =
        new HitboxSpec()
            .position(bottomLeft(centre, EXPLOSION_SIZE))
            .size(EXPLOSION_SIZE)
            .lifetime(0.18f)
            .layer(PhysicsLayer.WEAPON)
            .targetLayer((short) (PhysicsLayer.PLAYER | PhysicsLayer.NPC))
            .damage(5);
    return HitboxFactory.createHitbox(spec)
        .addComponent(new FireHitEffectComponent())
        .addComponent(new FireBurstRenderComponent());
  }

  private static Vector2 bottomLeft(Vector2 centre, Vector2 size) {
    return centre.cpy().mulAdd(size, -0.5f);
  }

  private OilPuddleFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
