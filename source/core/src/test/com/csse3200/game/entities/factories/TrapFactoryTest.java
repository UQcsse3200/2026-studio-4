package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.traps.BurnTrapEffectComponent;
import com.csse3200.game.components.traps.FireTrapRenderComponent;
import com.csse3200.game.components.traps.FreezeTrapEffectComponent;
import com.csse3200.game.components.traps.IceTrapRenderComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TrapFactoryTest {
  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());

    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resources.getAsset(IceTrapRenderComponent.TEXTURE, Texture.class)).thenReturn(texture);
    when(texture.getWidth()).thenReturn(160);
    when(texture.getHeight()).thenReturn(128);
    Texture fire = mock(Texture.class);
    when(fire.getWidth()).thenReturn(72);
    when(fire.getHeight()).thenReturn(59);
    when(resources.getAsset(FireTrapRenderComponent.START_TEXTURE, Texture.class)).thenReturn(fire);
    when(resources.getAsset(FireTrapRenderComponent.LOOP_TEXTURE, Texture.class)).thenReturn(fire);
    when(resources.getAsset(FireTrapRenderComponent.END_TEXTURE, Texture.class)).thenReturn(fire);
    ServiceLocator.registerResourceService(resources);
  }

  @Test
  void createsAConcreteFreezeTrap() {
    Entity trap = TrapFactory.createFreezeTrap();

    assertNotNull(trap.getComponent(FreezeTrapEffectComponent.class));
    assertTrapPhysics(trap);
  }

  @Test
  void createsAConcreteBurnTrap() {
    Entity trap = TrapFactory.createBurnTrap();

    assertNotNull(trap.getComponent(BurnTrapEffectComponent.class));
    assertTrapPhysics(trap);
  }

  private static void assertTrapPhysics(Entity trap) {
    assertEquals(
        BodyType.StaticBody, trap.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(PhysicsLayer.TRAP, trap.getComponent(HitboxComponent.class).getLayer());
  }
}
