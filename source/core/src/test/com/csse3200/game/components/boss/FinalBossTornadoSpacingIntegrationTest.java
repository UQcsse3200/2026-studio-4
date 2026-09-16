package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
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
class FinalBossTornadoSpacingIntegrationTest {
  private static final float STEP = 1f / 60f;
  private static final float EPSILON = 0.02f;
  private static final float CROSS_CLEARANCE = 2.35f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private FinalBossMovementComponent movement;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;
  private Rectangle arena;

  @BeforeEach
  void setup() {
    MathUtils.random.setSeed(2026L);
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
    movePlayerGroundTo(new Vector2(30f, 30f));
    config = new FinalBossStageThreeConfig();
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    movement = mock(FinalBossMovementComponent.class);
    setArena(24f, 16f);
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
  void twoWalkingStatuesAndThreeWanderingTornadoesKeepClearanceInAShortCamera() {
    setArena(16f, 8f);
    startWithSurvivors(2);
    List<Vector2> initialStatues = statuePositions();
    float tornadoTravel = 0f;
    float statueTravel = 0f;
    for (int frame = 0; frame < 600; frame++) {
      List<Vector2> beforeTornadoes = tornadoPositions();
      tickPhysics();
      tornadoTravel += assertContinuousTornadoMovement(beforeTornadoes);
      for (FinalBossTornadoController.Tornado tornado : stage.tornadoes.items) {
        assertFalse(
            tornado.chasing, "A distant player must not turn roaming into permanent pursuit");
      }
      assertCrossClearanceAndCameraBounds();
      statueTravel = Math.max(statueTravel, statueDisplacement(initialStatues));
    }
    assertTrue(tornadoTravel > 0.5f, "Tornadoes must actually roam around the surviving statues");
    assertTrue(statueTravel > 0.2f, "The statues must also walk while tornadoes are present");
    assertCountsAndPlayerHealth(2, 3);
  }

  @Test
  void fourTornadoesRespectTheWalkingStatueDuringNearbyPursuitAndRelease() {
    startWithSurvivors(1);
    FinalBossTornadoController.Tornado selected = stage.tornadoes.items.getFirst();
    for (int frame = 0; frame < 40; frame++) tickPhysics();
    float pursuedDistance = 0f;
    for (int frame = 0; frame < 120; frame++) {
      movePlayerGroundTo(selected.position.cpy().add(1.5f, 0f));
      Vector2 beforeSelected = selected.position.cpy();
      List<Vector2> beforeTornadoes = tornadoPositions();
      tickPhysics();
      assertTrue(selected.chasing);
      pursuedDistance += beforeSelected.dst(selected.position);
      assertContinuousTornadoMovement(beforeTornadoes);
      assertCrossClearanceAndCameraBounds();
    }
    assertTrue(pursuedDistance > 0.2f, "Nearby pursuit should still move at a controlled speed");

    movePlayerGroundTo(new Vector2(30f, 30f));
    for (int frame = 0; frame < 180; frame++) {
      tickPhysics();
      assertTrue(stage.tornadoes.items.stream().noneMatch(tornado -> tornado.chasing));
      assertCrossClearanceAndCameraBounds();
    }
    assertCountsAndPlayerHealth(1, 4);
  }

  @Test
  void aQueuedThreeHitEvadeLandsClearOfEveryMovingTornado() {
    startWithSurvivors(2);
    for (int frame = 0; frame < 45; frame++) tickPhysics();
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    movePlayerGroundTo(FinalBossStageThreeComponent.groundPosition(statue.entity).add(-1f, 0f));
    Vector2 before = statue.entity.getCenterPosition();
    for (int hit = 0; hit < config.statueEvadeHits; hit++) {
      statue.entity.getComponent(CombatStatsComponent.class).takeDamage(1, new Entity());
    }
    assertTrue(statue.evadePending);
    for (int frame = 0; frame < 120 && statue.evadePending; frame++) {
      tickPhysics();
      assertCrossClearanceAndCameraBounds();
    }
    assertFalse(statue.evadePending, "The arena has room for a safe evade destination");
    assertTrue(before.dst(statue.entity.getCenterPosition()) >= 2.5f);
    assertEquals(config.statueHits - config.statueEvadeHits, statue.hitsRemaining);
    assertCrossClearanceAndCameraBounds();
    assertCountsAndPlayerHealth(2, 3);
  }

  private void startWithSurvivors(int remaining) {
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    when(time.getDeltaTime()).thenReturn(config.chargeDuration);
    stage.update();
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    for (int index = remaining; index < stage.statues.size(); index++) {
      FinalBossStageThreeComponent.Statue statue = stage.statues.get(index);
      for (int hit = 0; hit < config.statueHits; hit++) {
        statue.entity.getComponent(CombatStatsComponent.class).takeDamage(1, new Entity());
      }
    }
    // Isolate simultaneous locomotion from the existing shockwave damage mechanic.
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      if (!statue.broken) statue.slamCooldown = 1000f;
    }
    when(time.getDeltaTime()).thenReturn(STEP);
    assertCountsAndPlayerHealth(remaining, config.statueCount - remaining);
    assertCrossClearanceAndCameraBounds();
  }

  private void tickPhysics() {
    stage.update();
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      if (!statue.broken) statue.entity.getComponent(PhysicsMovementComponent.class).update();
    }
    world.step(STEP, 6, 2);
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      if (!statue.broken) statue.entity.getComponent(PhysicsComponent.class).earlyUpdate();
    }
    player.getComponent(PhysicsComponent.class).earlyUpdate();
  }

  private void assertCrossClearanceAndCameraBounds() {
    for (FinalBossTornadoController.Tornado tornado : stage.tornadoes.items) {
      assertTrue(tornado.position.x - FinalBossTornadoController.WIDTH / 2f >= arena.x - EPSILON);
      assertTrue(
          tornado.position.x + FinalBossTornadoController.WIDTH / 2f
              <= arena.x + arena.width + EPSILON);
      assertTrue(tornado.position.y >= arena.y - EPSILON);
      assertTrue(
          tornado.position.y + FinalBossTornadoController.HEIGHT
              <= arena.y + arena.height + EPSILON);
      assertTornadoClearOfStatues(tornado);
    }
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      if (statue.broken) continue;
      Vector2 position = statue.entity.getPosition();
      Vector2 size = statue.entity.getScale();
      assertTrue(position.x >= arena.x - EPSILON);
      assertTrue(position.x + size.x <= arena.x + arena.width + EPSILON);
      assertTrue(position.y >= arena.y - EPSILON);
      assertTrue(position.y + size.y + statue.jumpHeight <= arena.y + arena.height + EPSILON);
    }
  }

  private void assertTornadoClearOfStatues(FinalBossTornadoController.Tornado tornado) {
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      if (statue.broken) continue;
      float distance =
          tornado.position.dst(FinalBossStageThreeComponent.groundPosition(statue.entity));
      assertTrue(
          distance >= CROSS_CLEARANCE - EPSILON,
          "A tornado and a statue overlap their reserved ground space: " + distance);
    }
  }

  private float assertContinuousTornadoMovement(List<Vector2> previous) {
    float total = 0f;
    for (int index = 0; index < previous.size(); index++) {
      float distance = previous.get(index).dst(stage.tornadoes.items.get(index).position);
      assertTrue(
          distance <= FinalBossTornadoController.CHASE_SPEED * STEP + 0.001f,
          "Ordinary tornado avoidance must move without teleporting");
      total += distance;
    }
    return total;
  }

  private float statueDisplacement(List<Vector2> initial) {
    List<Vector2> current = statuePositions();
    float distance = 0f;
    for (int index = 0; index < current.size(); index++) {
      distance += initial.get(index).dst(current.get(index));
    }
    return distance;
  }

  private List<Vector2> statuePositions() {
    return stage.statues.stream()
        .filter(statue -> !statue.broken)
        .map(statue -> statue.entity.getCenterPosition())
        .toList();
  }

  private List<Vector2> tornadoPositions() {
    return stage.tornadoes.items.stream().map(tornado -> tornado.position.cpy()).toList();
  }

  private void assertCountsAndPlayerHealth(int statues, int tornadoes) {
    assertEquals(statues, stage.getRemainingStatues());
    assertEquals(tornadoes, stage.tornadoes.activeCount());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  private void movePlayerGroundTo(Vector2 position) {
    player.setPosition(position.cpy().sub(player.getScale().x / 2f, player.getScale().y * 0.15f));
  }

  private void setArena(float width, float height) {
    when(movement.getCamera()).thenReturn(new OrthographicCamera(width, height));
    arena = new Rectangle(-width / 2f, -height / 2f, width, height);
  }
}
