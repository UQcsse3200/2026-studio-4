package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NPCFactoryTest {

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = new ResourceService();
    resourceService.loadTextureAtlases(
        new String[] {
          "images/beetle.atlas",
          "images/crab.atlas",
          "images/cyclops.atlas",
          "images/floatingDemon.atlas",
          "images/golem.atlas",
          "images/medusa.atlas",
          "images/mummy.atlas",
          "images/snake.atlas",
          "images/witch.atlas"
        });
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void giantEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createGiantEnemy(new Entity(), "images/mummy.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void bombEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createBombEnemy(new Entity(), "images/beetle.atlas", 2f);
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void chaseEnemyHasHealthBar() {
    Entity enemy = NPCFactory.createChaseEnemy(new Entity(), true, "images/crab.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void snakeMiniBossHasHealthBar() {
    Entity enemy = NPCFactory.createSnakeMiniBoss(new Entity());
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void floatingDemonHasHealthBar() {
    Entity enemy =
        NPCFactory.createFloatingDemon(
            new Entity(),
            new Vector2(0f, 0f),
            new Vector2(1f, 1f),
            new Vector2(2f, 0f),
            "images/floatingDemon.atlas");
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void witchHasHealthBar() {
    Entity enemy = NPCFactory.createWitch(new Entity(), projectile -> {});
    assertNotNull(enemy.getComponent(EnemyStatDisplay.class));
  }

  @Test
  void baseNpcDoesNotGainHealthBar() {
    assertNull(NPCFactory.createBaseNPC().getComponent(EnemyStatDisplay.class));
  }
}
