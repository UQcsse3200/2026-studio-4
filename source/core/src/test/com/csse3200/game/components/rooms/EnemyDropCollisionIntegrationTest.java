package com.csse3200.game.components.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.items.ItemComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** Verifies the full lethal-hit-to-room-drop flow against a real Box2D world. */
@ExtendWith(GameExtension.class)
class EnemyDropCollisionIntegrationTest {
  private static final Vector2 DEATH_POSITION = new Vector2(5f, 5f);
  private static final int ENEMY_HEALTH = 10;

  private PhysicsEngine physicsEngine;
  private EntityService entityService;
  private EnemyManagerComponent enemyManager;

  @BeforeEach
  void beforeEach() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(gameTime);

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
    ServiceLocator.registerRenderService(mock(RenderService.class));

    ResourceService resourceService = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resourceService.getAsset("images/strength_charm_pixel.png", Texture.class))
        .thenReturn(texture);
    when(texture.getWidth()).thenReturn(1270);
    when(texture.getHeight()).thenReturn(1239);
    ServiceLocator.registerResourceService(resourceService);

    enemyManager = new EnemyManagerComponent();
    entityService.register(new Entity().addComponent(enemyManager));
  }

  @Test
  void lethalWeaponCollisionShouldCreateDropAfterPhysicsUnlocks() {
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(ENEMY_HEALTH, 0))
            .addComponent(new EnemyDeathComponent());
    enemy.setPosition(DEATH_POSITION);
    enemyManager.track(enemy);
    entityService.register(enemy);

    Entity weaponHitbox =
        HitboxFactory.createHitbox(
            new HitboxSpec()
                .position(DEATH_POSITION)
                .size(new Vector2(1f, 1f))
                .lifetime(0.5f)
                .layer(PhysicsLayer.WEAPON)
                .targetLayer(PhysicsLayer.NPC)
                .damage(ENEMY_HEALTH));
    entityService.register(weaponHitbox);

    physicsEngine.update();
    assertEquals(0, enemy.getComponent(CombatStatsComponent.class).getHealth());

    entityService.update();
    physicsEngine.update();

    ArgumentCaptor<Entity> registeredEntities = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, atLeastOnce()).register(registeredEntities.capture());
    List<Entity> drops =
        registeredEntities.getAllValues().stream()
            .filter(entity -> entity.getComponent(ItemComponent.class) != null)
            .toList();
    assertEquals(1, drops.size());
    assertEquals(DEATH_POSITION, drops.get(0).getPosition());
    assertEquals(
        "Strength Charm", drops.get(0).getComponent(ItemComponent.class).getCharm().getName());
  }
}
