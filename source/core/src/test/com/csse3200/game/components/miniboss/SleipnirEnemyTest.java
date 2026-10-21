package com.csse3200.game.components.miniboss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.BossPhaseComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class SleipnirEnemyTest {
  private final Vector2 spawnPoints = new Vector2(10, 10);

  @BeforeEach
  void setUp() {
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
    resourceService.loadTextureAtlases(new String[] {"images/horse.atlas"});
    resourceService.loadTextures(new String[] {"images/dust.png"});
    resourceService.loadAll();
    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldCreateSleipnirWithDefaultCombatStats() {
    Entity sleipnir = NPCFactory.createSleipnir(new Entity(), spawnPoints);
    sleipnir.create();
    CombatStatsComponent stats = sleipnir.getComponent(CombatStatsComponent.class);
    assertEquals(250, stats.getHealth());
    assertEquals(250, stats.getMaxHealth());
    assertEquals(20, stats.getBaseAttack());
  }

  @Test
  void shouldHaveAITasks() {
    Entity sleipnir = NPCFactory.createSleipnir(new Entity(), spawnPoints);
    sleipnir.create();
    AITaskComponent aiTaskComponent = sleipnir.getComponent(AITaskComponent.class);

    Assertions.assertNotNull(aiTaskComponent);
    Assertions.assertEquals(sleipnir, aiTaskComponent.getEntity());
  }

  @Test
  void shouldCreateSleipnir() {
    Entity target = new Entity();

    Entity sleipnir = NPCFactory.createSleipnir(target, spawnPoints);

    Assertions.assertNotNull(sleipnir);
  }

  @Test
  void shouldHaveRequiredComponents() {
    Entity target = new Entity();

    Entity sleipnir = NPCFactory.createSleipnir(target, spawnPoints);

    Assertions.assertNotNull(sleipnir.getComponent(PhysicsMovementComponent.class));
    Assertions.assertNotNull(sleipnir.getComponent(AITaskComponent.class));
    Assertions.assertNotNull(sleipnir.getComponent(EnemyDeathComponent.class));
    Assertions.assertNotNull(sleipnir.getComponent(EnemyAnimationController.class));
    Assertions.assertNotNull(sleipnir.getComponent(EnemyStatDisplay.class));
    Assertions.assertNotNull(sleipnir.getComponent(BossPhaseComponent.class));
  }

  @Test
  void shouldSetMaximumMovementSpeed() {
    Entity target = new Entity();

    Entity sleipnir = NPCFactory.createSleipnir(target, spawnPoints);

    PhysicsMovementComponent movementComponent =
        sleipnir.getComponent(PhysicsMovementComponent.class);

    Assertions.assertNotNull(movementComponent);
    Assertions.assertTrue(movementComponent.getMoving());
  }

  @Test
  void shouldEnterPhaseTwoOnlyBelowHalfOfScaledHealth() {
    Entity horse = NPCFactory.createSleipnir(new Entity(), spawnPoints);
    horse.create();

    try {
      CombatStatsComponent stats = horse.getComponent(CombatStatsComponent.class);
      BossPhaseComponent phase = horse.getComponent(BossPhaseComponent.class);
      int[] enrageEvents = {0};

      horse.getEvents().addListener("enragePhaseStarted", () -> enrageEvents[0]++);

      // Three completed dungeons: 250 -> 625.
      stats.scale(3);

      assertEquals(625, stats.getMaxHealth());
      assertEquals(625, stats.getHealth());
      assertEquals(1, phase.getCurrentPhase());
      assertEquals(0, enrageEvents[0]);

      stats.takeDamage(312);

      assertEquals(313, stats.getHealth());
      assertEquals(1, phase.getCurrentPhase());

      stats.takeDamage(1);

      assertEquals(312, stats.getHealth());
      assertEquals(2, phase.getCurrentPhase());
      assertEquals(1, enrageEvents[0]);

      stats.takeDamage(1);
      assertEquals(1, enrageEvents[0]);
    } finally {
      horse.dispose();
    }
  }
}
