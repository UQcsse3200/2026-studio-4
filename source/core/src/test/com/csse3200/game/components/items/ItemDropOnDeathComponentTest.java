package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.ItemType;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** Tests developed with assistance from OpenAI Codex and reviewed by Yuezhou Wang. */
@ExtendWith(GameExtension.class)
class ItemDropOnDeathComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldCreateConfiguredFactoryDrop() {
    Vector2 deathPosition = new Vector2(2f, 5f);
    Entity owner = new Entity().addComponent(new ItemDropOnDeathComponent(ItemType.STRENGTH_CHARM));
    owner.setPosition(deathPosition);
    owner.create();

    owner.getEvents().trigger("entityDied");

    ArgumentCaptor<Entity> dropCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(dropCaptor.capture());
    Entity drop = dropCaptor.getValue();
    assertEquals(deathPosition, drop.getPosition());
    assertEquals("Strength Charm", drop.getComponent(ItemComponent.class).getCharm().getName());
  }

  @Test
  void shouldCreateAndRegisterDropAtDeathPosition() {
    Vector2 deathPosition = new Vector2(3f, 7f);
    Entity expectedDrop = new Entity();
    List<Vector2> requestedPositions = new ArrayList<>();
    List<Entity> registeredDrops = new ArrayList<>();
    ItemDropOnDeathComponent component =
        new ItemDropOnDeathComponent(
            position -> {
              requestedPositions.add(position);
              return expectedDrop;
            },
            registeredDrops::add);
    Entity owner = new Entity().addComponent(component);
    owner.setPosition(deathPosition);
    owner.create();

    owner.getEvents().trigger("entityDied");

    assertEquals(List.of(deathPosition), requestedPositions);
    assertEquals(1, registeredDrops.size());
    assertSame(expectedDrop, registeredDrops.get(0));
  }

  @Test
  void shouldNotCreateDropBeforeDeath() {
    AtomicInteger factoryCalls = new AtomicInteger();
    List<Entity> registeredDrops = new ArrayList<>();
    ItemDropOnDeathComponent component =
        new ItemDropOnDeathComponent(
            position -> {
              factoryCalls.incrementAndGet();
              return new Entity();
            },
            registeredDrops::add);
    Entity owner = new Entity().addComponent(component);
    owner.create();

    assertEquals(0, factoryCalls.get());
    assertEquals(0, registeredDrops.size());
  }

  @Test
  void shouldDropOnlyOnceForRepeatedDeathEvents() {
    AtomicInteger factoryCalls = new AtomicInteger();
    List<Entity> registeredDrops = new ArrayList<>();
    ItemDropOnDeathComponent component =
        new ItemDropOnDeathComponent(
            position -> {
              factoryCalls.incrementAndGet();
              return new Entity();
            },
            registeredDrops::add);
    Entity owner = new Entity().addComponent(component);
    owner.create();

    owner.getEvents().trigger("entityDied");
    owner.getEvents().trigger("entityDied");

    assertEquals(1, factoryCalls.get());
    assertEquals(1, registeredDrops.size());
  }

  @Test
  void shouldIgnoreDeathOfAnotherEntity() {
    AtomicInteger factoryCalls = new AtomicInteger();
    ItemDropOnDeathComponent component =
        new ItemDropOnDeathComponent(
            position -> {
              factoryCalls.incrementAndGet();
              return new Entity();
            },
            drop -> {});
    Entity owner = new Entity().addComponent(component);
    Entity other = new Entity();
    owner.create();
    other.create();

    other.getEvents().trigger("entityDied");

    assertEquals(0, factoryCalls.get());
  }
}
