package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStageThreeVolleyTest {
  private static final float EPSILON = 0.001f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity boss;
  private Entity player;
  private GameTime time;

  @BeforeEach
  void setup() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    player.create();
    config = new FinalBossStageThreeConfig();
    config.floatingDemonCount = 0; // Isolate statue/ice mechanics from rendered summons.
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    OrthographicCamera camera = new OrthographicCamera(24f, 16f);
    when(movement.getCamera()).thenReturn(camera);
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
    boss.getComponent(CombatStatsComponent.class).setHealth(60);
    stage.startWaveOne();
  }

  @Test
  void volleyHasThreeIndependentBoltsAimedInASymmetricFan() {
    Vector2 origin = boss.getCenterPosition();
    Vector2 aim = player.getCenterPosition().sub(origin).nor();
    tick(1.59f);
    assertTrue(stage.bolts.isEmpty());
    tick(0.02f);
    assertEquals(3, stage.bolts.size());
    List<Double> angles = new ArrayList<>();
    for (FinalBossStageThreeComponent.Bolt bolt : stage.bolts) {
      assertTrue(bolt.position.epsilonEquals(origin, EPSILON));
      assertEquals(5f, bolt.velocity.len(), EPSILON);
      angles.add(Math.toDegrees(Math.atan2(aim.crs(bolt.velocity), aim.dot(bolt.velocity))));
    }
    angles.sort(Double::compare);
    assertEquals(-30d, angles.get(0), 0.001d);
    assertEquals(0d, angles.get(1), 0.001d);
    assertEquals(30d, angles.get(2), 0.001d);
    assertNotSame(stage.bolts.get(0).position, stage.bolts.get(1).position);
    assertNotSame(stage.bolts.get(0).velocity, stage.bolts.get(1).velocity);
    tick(0.02f);
    for (FinalBossStageThreeComponent.Bolt bolt : stage.bolts) {
      assertEquals(0.1f, bolt.position.dst(origin), EPSILON);
    }
    assertFalse(stage.bolts.get(0).position.epsilonEquals(stage.bolts.get(1).position, EPSILON));
  }

  @Test
  void everySecondVolleyStartsAThreeSecondStationaryRecoveryThenTheNextCycle() {
    assertEquals(3f, config.volleyRecoveryDuration);
    fireMissedVolley();
    for (int cycle = 0; cycle < 2; cycle++) {
      stage.bursts.clear();
      stepBossPhysics();
      assertFalse(boss.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().isZero());
      Vector2 start = boss.getCenterPosition();
      tick(config.boltInterval - 0.01f);
      assertTrue(stage.bolts.isEmpty());
      assertTrue(boss.getComponent(PhysicsMovementComponent.class).getMoving());
      tick(0.02f);
      assertEquals(3, stage.bolts.size());
      stage.bolts.clear();
      assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
      stepBossPhysics();
      assertTrue(boss.getCenterPosition().epsilonEquals(start, EPSILON));

      tick(config.volleyTeleportDelay - 0.01f);
      assertTrue(boss.getCenterPosition().epsilonEquals(start, EPSILON));
      assertTrue(stage.bursts.isEmpty());
      tick(0.02f);
      Vector2 landing = boss.getCenterPosition();
      assertTrue(landing.dst(start) >= 2f - EPSILON);
      assertEquals(2, stage.bursts.size());
      assertTrue(stage.bursts.stream().allMatch(burst -> !burst.grey && !burst.ice));
      assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
      stepBossPhysics();
      assertTrue(boss.getCenterPosition().epsilonEquals(landing, EPSILON));

      tick(config.volleyRecoveryDuration - config.volleyTeleportDelay - 0.02f);
      assertTrue(stage.bolts.isEmpty());
      assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
      assertTrue(boss.getCenterPosition().epsilonEquals(landing, EPSILON));
      tick(0.02f);
      // The next first volley starts at the end of recovery, without another 1.6-second delay.
      assertEquals(3, stage.bolts.size());
      assertTrue(boss.getCenterPosition().epsilonEquals(landing, EPSILON));
      assertTrue(boss.getComponent(PhysicsMovementComponent.class).getMoving());
      stage.bolts.clear();
    }
  }

  @Test
  void secondVolleyStartsBeforeTeleportAndItsProjectilesSurviveTheBlink() {
    tick(config.boltInterval);
    // Dodge the first fan instead of deleting it, so both fans remain in flight.
    player.setPosition(0f, 6f);
    Vector2 origin = boss.getCenterPosition();
    tick(config.boltInterval);
    assertEquals(6, stage.bolts.size());
    List<FinalBossStageThreeComponent.Bolt> secondFan = new ArrayList<>(stage.bolts.subList(3, 6));
    for (FinalBossStageThreeComponent.Bolt bolt : secondFan) {
      assertTrue(bolt.position.epsilonEquals(origin, EPSILON));
    }
    tick(config.volleyTeleportDelay + 0.001f);
    assertFalse(boss.getCenterPosition().epsilonEquals(origin, EPSILON));
    assertEquals(6, stage.bolts.size());
    for (FinalBossStageThreeComponent.Bolt bolt : secondFan) {
      assertTrue(stage.bolts.contains(bolt));
      Vector2 expected = origin.cpy().mulAdd(bolt.velocity, config.volleyTeleportDelay + 0.001f);
      assertTrue(bolt.position.epsilonEquals(expected, EPSILON));
    }
    assertFalse(stage.isFrozen());
    Vector2 landing = boss.getCenterPosition();
    tick(0.5f);
    assertTrue(boss.getCenterPosition().epsilonEquals(landing, EPSILON));
    assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
    assertEquals(6, stage.bolts.size());
    for (FinalBossStageThreeComponent.Bolt bolt : secondFan) {
      Vector2 expected = origin.cpy().mulAdd(bolt.velocity, config.volleyTeleportDelay + 0.501f);
      assertTrue(bolt.position.epsilonEquals(expected, EPSILON));
    }
    assertFalse(stage.isFrozen());
  }

  @Test
  void freezeDuringRecoveryCancelsTheScheduledBlinkAndRestartsTheCycleAfterStrikeThaw() {
    fireMissedVolley();
    fireMissedVolley();
    Vector2 beforeFreeze = boss.getCenterPosition();
    stage.bolts.add(
        new FinalBossStageThreeComponent.Bolt(player.getCenterPosition(), new Vector2()));
    tick(0.01f);
    assertTrue(stage.isFrozen());
    tick(config.volleyTeleportDelay + 0.01f);
    assertTrue(boss.getCenterPosition().epsilonEquals(beforeFreeze, EPSILON));
    assertTrue(stage.bursts.stream().noneMatch(burst -> !burst.ice && !burst.grey));
    tick(config.teleportDelay - config.volleyTeleportDelay);
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) <= 1.1f);
    tick(config.strikeDelay + 0.01f);
    assertEquals(
        100 - config.strikeDamage, player.getComponent(CombatStatsComponent.class).getHealth());
    assertFalse(stage.isFrozen());
    assertFalse(player.getComponent(PlayerActions.class).areControlsLocked());
    assertTrue(stage.thawRemaining > 0f);
    assertTrue(stage.bolts.isEmpty());
    stage.bursts.clear();
    Vector2 afterCombo = boss.getCenterPosition();
    assertTrue(afterCombo.dst(player.getCenterPosition()) > 2f);
    tick(config.boltInterval - 0.01f);
    assertTrue(stage.bolts.isEmpty());
    tick(0.02f);
    assertEquals(3, stage.bolts.size());
    stage.bolts.clear();
    tick(config.volleyTeleportDelay + 0.01f);
    assertTrue(boss.getCenterPosition().epsilonEquals(afterCombo, EPSILON));
    assertTrue(stage.bursts.isEmpty());
  }

  @Test
  void bossPhysicallyRetreatsWhileTheCastingAnimationIsActive() {
    boss.setPosition(3f, 0f);
    Vector2 before = boss.getCenterPosition();
    float distanceBefore = before.dst(player.getCenterPosition());
    tick(config.boltInterval);
    assertTrue(stage.castRemaining > 0f);
    boss.getComponent(PhysicsMovementComponent.class).update();
    ServiceLocator.getPhysicsService().getPhysics().getWorld().step(0.016f, 6, 2);
    boss.getComponent(PhysicsComponent.class).earlyUpdate();
    assertTrue(boss.getCenterPosition().dst(player.getCenterPosition()) > distanceBefore);
    assertTrue(boss.getCenterPosition().x > before.x);
  }

  @Test
  void teleportLandsAtUsefulDistanceInsideTheArena() {
    fireMissedVolley();
    fireMissedVolley();
    tick(config.volleyTeleportDelay + 0.001f);
    Vector2 landing = boss.getCenterPosition();
    Vector2 size = boss.getScale();
    assertEquals(config.repositionDistance, landing.dst(player.getCenterPosition()), 0.05f);
    assertTrue(landing.x - size.x / 2f >= -12f + 0.19f);
    assertTrue(landing.x + size.x / 2f <= 12f - 0.19f);
    assertTrue(landing.y - size.y / 2f >= -8f + 0.19f);
    assertTrue(landing.y + size.y / 2f <= 8f - 0.19f);
  }

  @Test
  void healthThresholdCancelsPendingVolleyTeleportAndClearsProjectiles() {
    fireMissedVolley();
    tick(config.boltInterval);
    assertEquals(3, stage.bolts.size());
    Vector2 before = boss.getCenterPosition();
    boss.getComponent(CombatStatsComponent.class).takeDamage(1000, player);
    assertEquals(FinalBossStageThreeState.CHARGING, stage.getState());
    tick(config.volleyTeleportDelay + 0.01f);
    assertTrue(stage.bolts.isEmpty());
    assertTrue(stage.bursts.isEmpty());
    assertTrue(boss.getCenterPosition().epsilonEquals(before, EPSILON));
    assertFalse(boss.getComponent(PhysicsMovementComponent.class).getMoving());
  }

  private void stepBossPhysics() {
    boss.getComponent(PhysicsMovementComponent.class).update();
    ServiceLocator.getPhysicsService().getPhysics().getWorld().step(0.016f, 6, 2);
    boss.getComponent(PhysicsComponent.class).earlyUpdate();
  }

  private void fireMissedVolley() {
    tick(config.boltInterval);
    assertEquals(3, stage.bolts.size());
    // Simulate shots missing the player to isolate the firing/teleport cadence.
    stage.bolts.clear();
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }
}
