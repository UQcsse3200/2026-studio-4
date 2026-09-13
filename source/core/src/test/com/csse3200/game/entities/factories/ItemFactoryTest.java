package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Item;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFactoryTest {

  Item item;

  @BeforeEach
  void setup() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ServiceLocator.registerPhysicsService(new PhysicsService());
    // Couldn't make tests work without the resourceService
    ResourceService resourceService = new ResourceService();
    resourceService.loadTextures(new String[] {StrengthCharm.TEXTURE});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);

    item = mock(Item.class);
    when(item.getTexture()).thenReturn(StrengthCharm.TEXTURE);
  }

  @Test
  void shouldCreateRenderableItem() {
    Entity itemEntity = ItemFactory.createItem(item);
    assertNotNull(itemEntity.getComponent(TextureRenderComponent.class));
  }

  @Test
  void shouldCreateWithCorrectPhysics() {
    Entity itemEntity = ItemFactory.createItem(item);
    assertNotNull(itemEntity.getComponent(HitboxComponent.class));
    assertNotNull(itemEntity.getComponent(PhysicsComponent.class));

    assertEquals(PhysicsLayer.ITEM, itemEntity.getComponent(HitboxComponent.class).getLayer());
  }
}
