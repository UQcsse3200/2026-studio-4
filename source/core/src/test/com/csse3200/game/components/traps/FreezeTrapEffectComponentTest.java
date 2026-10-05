package com.csse3200.game.components.traps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FreezeTrapEffectComponentTest {
  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerTimeSource(new GameTime());
  }

  @Test
  void freezesThePlayerOnlyOnce() {
    Entity trap =
        new Entity()
            .addComponent(new FreezeTrapEffectComponent())
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.TRAP));
    trap.create();

    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 1))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.create();

    int[] triggers = {0};
    trap.getEvents().addListener("trapTriggered", () -> triggers[0]++);
    Fixture trapFixture = trap.getComponent(HitboxComponent.class).getFixture();
    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    trap.getEvents().trigger("collisionStart", trapFixture, playerFixture);
    trap.getEvents().trigger("collisionStart", trapFixture, playerFixture);

    assertTrue(StatusEffectsControllerComponent.isImmobilised(player));
    assertEquals(1, triggers[0]);
  }
}
