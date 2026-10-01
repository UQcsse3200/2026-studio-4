package com.csse3200.game.entities.factories;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.traps.BurnTrapEffectComponent;
import com.csse3200.game.components.traps.FireTrapRenderComponent;
import com.csse3200.game.components.traps.FreezeTrapEffectComponent;
import com.csse3200.game.components.traps.IceTrapRenderComponent;
import com.csse3200.game.components.traps.TrapEffectComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderComponent;

/** Creates the concrete trap entities that room configuration can place. */
public class TrapFactory {

  private TrapFactory() {
    throw new IllegalStateException("Instantiating static utility class");
  }

  /** Creates a walkable one-shot trap that freezes the player. */
  public static Entity createFreezeTrap() {
    return createTrap(new FreezeTrapEffectComponent(), new IceTrapRenderComponent());
  }

  /** Creates a walkable one-shot trap that burns the player. */
  public static Entity createBurnTrap() {
    return createTrap(new BurnTrapEffectComponent(), new FireTrapRenderComponent());
  }

  private static Entity createTrap(TrapEffectComponent effect, RenderComponent visual) {
    Entity trap =
        new Entity()
            .addComponent(visual)
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.TRAP))
            .addComponent(effect);
    trap.setScale(0.5f, 0.5f);
    return trap;
  }
}
