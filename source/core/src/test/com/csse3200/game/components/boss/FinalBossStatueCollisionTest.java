package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStatueCollisionTest {
  private World world;

  @AfterEach
  void disposeWorld() {
    if (world != null) world.dispose();
  }

  @Test
  void tenthWeaponContactDefersPhysicsRemovalUntilTheWorldUnlocks() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    PhysicsEngine physics = spy(new PhysicsEngine());
    world = physics.getWorld();
    ServiceLocator.registerPhysicsService(new PhysicsService(physics));
    EntityService entities = spy(new EntityService());
    ServiceLocator.registerEntityService(entities);
    doAnswer(
            invocation -> {
              assertFalse(world.isLocked(), "Bodies must only be destroyed after the step");
              return invocation.callRealMethod();
            })
        .when(physics)
        .destroyBody(any(Body.class));

    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    entities.register(player);
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    FinalBossStageThreeConfig config = new FinalBossStageThreeConfig();
    config.floatingDemonCount = 0; // Isolate statue/ice mechanics from rendered summons.
    FinalBossStageThreeComponent stage =
        new FinalBossStageThreeComponent(player, entities::register, config);
    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(mock(FinalBossMovementComponent.class))
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    entities.register(boss);
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    when(time.getDeltaTime()).thenReturn(config.chargeDuration + 0.01f);
    stage.update();
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());

    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    CombatStatsComponent health = statue.entity.getComponent(CombatStatsComponent.class);
    for (int i = 0; i < 9; i++) health.takeDamage(1, new Entity());
    assertEquals(1, statue.hitsRemaining);

    // A real sensor overlap follows PhysicsContactListener -> TouchAttackComponent -> damage.
    Entity weapon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(1, 1))
            .addComponent(new TouchAttackComponent(PhysicsLayer.NPC));
    weapon.setPosition(statue.entity.getPosition());
    entities.register(weapon);
    Body statueBody = statue.entity.getComponent(PhysicsComponent.class).getBody();
    int bodyCount = world.getBodyCount();
    AtomicInteger contacts = new AtomicInteger();
    statue
        .entity
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity attacker) -> {
              assertSame(weapon, attacker);
              assertTrue(world.isLocked(), "The tenth hit must come from a real physics step");
              assertTrue(statue.broken);
              assertTrue(
                  statueBody.isActive(), "Do not deactivate a body inside its contact callback");
              assertEquals(bodyCount, world.getBodyCount());
              contacts.incrementAndGet();
            });

    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);

    assertEquals(1, contacts.get());
    assertEquals(0, health.getHealth());
    assertEquals(config.statueCount - 1, stage.getRemainingStatues());
    assertFalse(world.isLocked());
    assertEquals(bodyCount, world.getBodyCount());
    verify(entities).scheduleDisposal(statue.entity);
    verify(physics, never()).destroyBody(statueBody);

    when(time.getDeltaTime()).thenReturn(0.01f);
    entities.update();

    verify(physics).destroyBody(statueBody);
    assertEquals(bodyCount - 1, world.getBodyCount());
  }
}
