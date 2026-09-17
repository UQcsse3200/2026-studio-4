package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStatueClusteringTest {
  private static final float STEP = 1f / 60f;
  private static final float SPACING_TOLERANCE = 0.02f;
  private static final float TWO_STATUE_CLEARANCE = 2.6f;
  private static final float MAX_WALK_SPEED = 1.5f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;

  @BeforeEach
  void setup() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(STEP);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    player.create();
    player.getComponent(CombatStatsComponent.class).setInvulnerable(true);
    config = new FinalBossStageThreeConfig();
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(new OrthographicCamera(24f, 16f));
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void twoSurvivorsWalkTowardTheArenaCentreWithSeedSeven() {
    assertSurvivorsGatherGradually(7L);
  }

  @Test
  void twoSurvivorsWalkTowardTheArenaCentreWithSeedThirtyOne() {
    assertSurvivorsGatherGradually(31L);
  }

  @Test
  void twoSurvivorsWalkTowardTheArenaCentreWithSeedTwentyTwentySix() {
    assertSurvivorsGatherGradually(2026L);
  }

  private void assertSurvivorsGatherGradually(long seed) {
    MathUtils.random.setSeed(seed);
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    when(time.getDeltaTime()).thenReturn(config.chargeDuration);
    stage.update();
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues.subList(2, 5)) {
      for (int hit = 0; hit < config.statueHits; hit++) {
        statue.entity.getComponent(CombatStatsComponent.class).takeDamage(1, new Entity());
      }
    }
    assertEquals(2, stage.getRemainingStatues());
    List<FinalBossStageThreeComponent.Statue> survivors =
        stage.statues.stream().filter(statue -> !statue.broken).toList();
    // Isolate ordinary walking from intentional hit-triggered teleports and slam pauses.
    survivors.forEach(statue -> statue.slamCooldown = 1000f);
    when(time.getDeltaTime()).thenReturn(STEP);
    float initialDistance = totalDistanceFromArenaCentre(survivors);
    for (int frame = 0; frame < 480; frame++) {
      List<Vector2> before =
          survivors.stream().map(statue -> statue.entity.getCenterPosition()).toList();
      tickPhysics(survivors);
      assertSafeContinuousMovement(survivors, before);
    }
    assertTrue(
        totalDistanceFromArenaCentre(survivors) < initialDistance - 2f,
        "Survivors must visibly gather toward the arena centre, seed " + seed);
    assertTrue(stage.shockwaves.isEmpty(), "Measure walking without slam attacks");
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  private void assertSafeContinuousMovement(
      List<FinalBossStageThreeComponent.Statue> survivors, List<Vector2> before) {
    float separation =
        survivors
            .get(0)
            .entity
            .getCenterPosition()
            .dst(survivors.get(1).entity.getCenterPosition());
    assertTrue(
        separation >= TWO_STATUE_CLEARANCE - SPACING_TOLERANCE,
        "Gathering must preserve clearance; actual separation: " + separation);
    for (int i = 0; i < survivors.size(); i++) {
      float travelled = before.get(i).dst(survivors.get(i).entity.getCenterPosition());
      assertTrue(
          travelled <= MAX_WALK_SPEED * STEP + 0.001f,
          "Ordinary gathering must walk without teleporting; tick distance: " + travelled);
    }
  }

  private float totalDistanceFromArenaCentre(List<FinalBossStageThreeComponent.Statue> survivors) {
    float distance = 0f;
    for (FinalBossStageThreeComponent.Statue statue : survivors) {
      distance += statue.entity.getCenterPosition().len();
    }
    return distance;
  }

  private void tickPhysics(List<FinalBossStageThreeComponent.Statue> survivors) {
    stage.update();
    for (FinalBossStageThreeComponent.Statue statue : survivors) {
      statue.entity.getComponent(PhysicsMovementComponent.class).update();
    }
    world.step(STEP, 6, 2);
    for (FinalBossStageThreeComponent.Statue statue : survivors) {
      statue.entity.getComponent(PhysicsComponent.class).earlyUpdate();
    }
    player.getComponent(PhysicsComponent.class).earlyUpdate();
  }
}
