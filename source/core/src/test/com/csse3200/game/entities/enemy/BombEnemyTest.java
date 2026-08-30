package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.ExplodeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BombEnemyTest {

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);

    ServiceLocator.registerPhysicsService(new PhysicsService());
    EntityService entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(new String[] {"images/bombEnemy.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void testBombEnemyExplodesOnPlayerCollision() {
    Entity player =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player);
    bombEnemy.create();

    ExplodeComponent explodeComponent = bombEnemy.getComponent(ExplodeComponent.class);

    assertNotNull(explodeComponent);

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();

    Fixture playerFixture = player.getComponent(HitboxComponent.class).getFixture();

    EventListener0 dieAnimationListener = mock(EventListener0.class);

    bombEnemy.getEvents().addListener("dieAnimation", dieAnimationListener);

    bombEnemy.getEvents().trigger("collisionStart", bombFixture, playerFixture);

    verify(dieAnimationListener, times(1)).handle();
  }

  @Test
  void testBombEnemyDoesNotExplodeOnNonPlayerCollision() {
    Entity player =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    player.create();

    Entity bombEnemy = NPCFactory.createBombEnemy(player);
    bombEnemy.create();

    ExplodeComponent explodeComponent = bombEnemy.getComponent(ExplodeComponent.class);

    assertNotNull(explodeComponent);

    Fixture bombFixture = bombEnemy.getComponent(HitboxComponent.class).getFixture();

    Entity otherEntity =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new HitboxComponent());
    otherEntity.create();
    Fixture otherFixture = otherEntity.getComponent(HitboxComponent.class).getFixture();

    EventListener0 dieAnimationListener = mock(EventListener0.class);

    bombEnemy.getEvents().addListener("dieAnimation", dieAnimationListener);

    bombEnemy.getEvents().trigger("collisionStart", bombFixture, otherFixture);

    verify(dieAnimationListener, times(0)).handle();
  }
}
