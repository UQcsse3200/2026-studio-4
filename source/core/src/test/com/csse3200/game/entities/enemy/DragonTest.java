package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyDeathComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.miniboss.dragon.*;
import com.csse3200.game.components.npc.EnemyStatDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.DragonConfig;
import com.csse3200.game.entities.factories.DragonFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonTest {
  private ResourceService resources;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerTimeSource(new GameTime());

    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextureAtlases(new String[] {DragonFactory.ATLAS_PATH});
    resources.loadAll();
  }

  @AfterEach
  void tearDown() {
    resources.unloadAssets(new String[] {DragonFactory.ATLAS_PATH});
  }

  @Test
  void shouldCreateDragonWithDefaultCombatStats() {
    Entity dragon = DragonFactory.createDragon();
    CombatStatsComponent stats = dragon.getComponent(CombatStatsComponent.class);

    assertEquals(500, stats.getHealth());
    assertEquals(500, stats.getMaxHealth());
    assertEquals(20, stats.getBaseAttack());
  }

  @Test
  void shouldApplyCustomSettings() {
    DragonConfig config = new DragonConfig();
    config.health = 600;
    config.baseAttack = 25;
    config.width = 3f;
    config.height = 2f;

    Entity dragon = DragonFactory.createDragon(config);
    CombatStatsComponent stats = dragon.getComponent(CombatStatsComponent.class);

    assertEquals(600, stats.getHealth());
    assertEquals(600, stats.getMaxHealth());
    assertEquals(25, stats.getBaseAttack());
    assertEquals(3f, dragon.getScale().x, 0.001f);
    assertEquals(2f, dragon.getScale().y, 0.001f);
  }

  @Test
  void shouldAttachHealthBarPhaseAndDeathHandling() {
    Entity dragon = DragonFactory.createDragon();

    assertNotNull(dragon.getComponent(EnemyStatDisplay.class));
    assertNotNull(dragon.getComponent(DragonPhaseComponent.class));
    assertNotNull(dragon.getComponent(EnemyDeathComponent.class));
  }

  @Test
  void shouldSupportEnemyTargetingMovementAndStatusEffects() {
    Entity dragon = DragonFactory.createDragon();
    HitboxComponent hitbox = dragon.getComponent(HitboxComponent.class);

    assertNotNull(hitbox);
    assertTrue(PhysicsLayer.contains(hitbox.getLayer(), PhysicsLayer.NPC));
    assertNotNull(dragon.getComponent(PhysicsMovementComponent.class));
    assertNotNull(dragon.getComponent(StatusEffectsControllerComponent.class));
  }

  @Test
  void shouldLoadAnimationsAndStartIdle() {
    Entity dragon = DragonFactory.createDragon();
    AnimationRenderComponent animator = dragon.getComponent(AnimationRenderComponent.class);

    assertNotNull(animator);
    assertTrue(animator.hasAnimation("idle"));
    assertTrue(animator.hasAnimation("moveRight"));
    assertTrue(animator.hasAnimation("moveLeft"));
    assertTrue(animator.hasAnimation("wave"));
    assertTrue(animator.hasAnimation("jump"));
    assertTrue(animator.hasAnimation("collapse"));
    assertEquals("idle", animator.getCurrentAnimation());
  }

  @Test
  void shouldConnectPhaseToTheDragonsHealthPool() {
    Entity dragon = DragonFactory.createDragon();
    DragonPhaseComponent phase = dragon.getComponent(DragonPhaseComponent.class);

    // Initialise only phase logic; UI rendering is outside this test.
    phase.create();

    dragon.getComponent(CombatStatsComponent.class).setHealth(250);

    assertEquals(2, phase.getCurrentPhase());
  }

  @Test
  void shouldAttachThunderOrbSkillWithoutFiringDuringConstruction() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    List<Entity> spawnedProjectiles = new ArrayList<>();

    Entity dragon = DragonFactory.createDragon(target, spawnedProjectiles::add);

    assertNotNull(dragon.getComponent(DragonThunderOrbComponent.class));
    assertTrue(spawnedProjectiles.isEmpty());
  }

  @Test
  void shouldAttachAllCloudDashComponents() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    List<Entity> spawnedProjectiles = new ArrayList<>();

    Entity dragon = DragonFactory.createDragon(target, spawnedProjectiles::add);

    assertNotNull(dragon.getComponent(DragonCloudDashComponent.class));
    assertNotNull(dragon.getComponent(DragonCloudDashMovementComponent.class));
    assertNotNull(dragon.getComponent(DragonCloudDashDamageComponent.class));
    assertNotNull(dragon.getComponent(DragonCloudDashVisualComponent.class));
    assertTrue(spawnedProjectiles.isEmpty());
  }
}
