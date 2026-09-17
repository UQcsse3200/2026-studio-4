package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GiantEnemyTest {

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
    resourceService.loadTextureAtlases(new String[] {"images/cyclops.atlas", "images/mummy.atlas"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldHaveGiantAnimations() {
    Entity cyclops = NPCFactory.createGiantEnemy(new Entity(), "images/cyclops.atlas");
    Entity mummy = NPCFactory.createGiantEnemy(new Entity(), "images/mummy.atlas");
    AnimationRenderComponent cyclopsAnimator = cyclops.getComponent(AnimationRenderComponent.class);
    AnimationRenderComponent mummyAnimator = mummy.getComponent(AnimationRenderComponent.class);

    assertTrue(cyclopsAnimator.hasAnimation("move"));
    assertTrue(cyclopsAnimator.hasAnimation("chase"));
    assertTrue(cyclopsAnimator.hasAnimation("dieAnimation"));
    assertTrue(mummyAnimator.hasAnimation("move"));
    assertTrue(mummyAnimator.hasAnimation("chase"));
    assertTrue(mummyAnimator.hasAnimation("dieAnimation"));
  }

  @Test
  void getGiantEnemySize() {
    Entity giantEnemy = NPCFactory.createGiantEnemy(new Entity(), "images/cyclops.atlas");
    assertEquals(giantEnemy.getScale(), new Vector2(3f, 3f));
  }

  @Test
  void shouldUseConfiguredBaseCombatStats() {
    Entity giant = NPCFactory.createGiantEnemy(new Entity(), "images/cyclops.atlas");
    CombatStatsComponent stats = giant.getComponent(CombatStatsComponent.class);

    assertEquals(30, stats.getHealth());
    assertEquals(3, stats.getBaseAttack());
  }
}
