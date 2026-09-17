package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
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
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossStatueEvadeTest {
  private static final float EPSILON = 0.001f;
  private static final float STEP = 0.01f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity player;
  private GameTime time;
  private World world;
  private RenderService renderer;
  private FinalBossMovementComponent bossMovement;
  private float arenaWidth = 16f;
  private float arenaHeight = 8f;

  @BeforeEach
  void setup() {
    MathUtils.random.setSeed(2026L);
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(STEP);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
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
    bossMovement = mock(FinalBossMovementComponent.class);
    when(bossMovement.getCamera()).thenReturn(new OrthographicCamera(arenaWidth, arenaHeight));
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    Entity boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(bossMovement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setPosition(4f, 0f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) {
      statue.slamCooldown = 100f;
      statue.pauseRemaining = 100f;
    }
    stage.bursts.clear();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void everyThirdDistinctHitEvadesInAShortArenaAndTheTenthStillDefeatsTheStatue() {
    stage.statues.stream().skip(1).forEach(peer -> peer.pauseRemaining = 0f);
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    for (int group = 0; group < 3; group++) {
      Vector2 before = statue.entity.getCenterPosition();
      putPlayerBeside(statue);
      stage.bursts.clear();
      hit(statue);
      hit(statue);
      tick(STEP);
      assertPositionEquals(before, statue.entity.getCenterPosition());
      assertTrue(stage.bursts.isEmpty());
      hit(statue);
      assertPositionEquals(before, statue.entity.getCenterPosition());
      tick(STEP);
      assertEvaded(statue, before);
      assertEquals(10 - (group + 1) * 3, statue.hitsRemaining);
      assertEquals(statue.hitsRemaining, health(statue).getHealth());
    }
    Vector2 beforeDeath = statue.entity.getCenterPosition();
    stage.bursts.clear();
    hit(statue);
    tick(STEP);
    assertTrue(statue.broken);
    assertEquals(0, health(statue).getHealth());
    assertEquals(4, stage.getRemainingStatues());
    assertPositionEquals(beforeDeath, statue.entity.getCenterPosition());
    assertEquals(1, stage.bursts.size());
    assertTrue(stage.bursts.getFirst().grey);
  }

  @Test
  void repeatedWeaponContactsAndNonPositiveDamageDoNotAdvanceTheThreeHitCounter() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    Vector2 before = statue.entity.getCenterPosition();
    Entity firstWeapon = new Entity();
    Entity initiallyHarmlessWeapon = new Entity();
    for (int i = 0; i < 5; i++) health(statue).takeDamage(1, firstWeapon);
    health(statue).takeDamage(0, initiallyHarmlessWeapon);
    health(statue).takeDamage(-1, new Entity());
    health(statue).takeDamage(1, null);
    tick(STEP);
    assertEquals(9, statue.hitsRemaining);
    assertPositionEquals(before, statue.entity.getCenterPosition());
    health(statue).takeDamage(1, initiallyHarmlessWeapon);
    tick(STEP);
    assertEquals(8, statue.hitsRemaining);
    assertPositionEquals(before, statue.entity.getCenterPosition());
    hit(statue);
    tick(STEP);
    assertEquals(7, statue.hitsRemaining);
    assertEvaded(statue, before);
  }

  @Test
  void evadingDuringAWarningCancelsTheSlamAndRestartsTheFullCooldown() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    statue.slamCooldown = 0f;
    tick(config.statueSlamInitialDelay);
    assertTrue(statue.warningRemaining > 0f);
    Vector2 before = statue.entity.getCenterPosition();
    hitThreeTimes(statue);
    tick(STEP);
    assertEvaded(statue, before);
    assertSlamCancelled(statue);
    tick(13.5f - STEP);
    assertEquals(0f, statue.warningRemaining);
    tick(2f * STEP);
    assertEquals(config.statueSlamWarning, statue.warningRemaining, EPSILON);
    assertTrue(stage.shockwaves.isEmpty());
  }

  @Test
  void evadingWhileAirborneReturnsToTheGroundWithoutProducingARing() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    statue.slamCooldown = 0f;
    tick(config.statueSlamInitialDelay);
    tick(config.statueSlamWarning);
    tick(config.statueJumpDuration / 2f);
    assertTrue(statue.airborne);
    assertTrue(statue.jumpHeight > 0f);
    Vector2 before = statue.entity.getCenterPosition();
    Vector2 departure = before.cpy().add(0f, statue.jumpHeight);
    hitThreeTimes(statue);
    tick(STEP);
    assertEvaded(statue, before, departure);
    assertSlamCancelled(statue);
    tick(config.statueJumpDuration);
    assertFalse(statue.airborne);
    assertTrue(stage.shockwaves.isEmpty());
    verify(renderer, never()).shake(any(), anyFloat(), anyFloat());
  }

  @Test
  void teleportDestinationAvoidsWallsOtherStatuesAndTheirReservedRoutes() {
    arenaWidth = 24f;
    arenaHeight = 16f;
    when(bossMovement.getCamera()).thenReturn(new OrthographicCamera(arenaWidth, arenaHeight));
    Vector2[] centres = {
      new Vector2(-2f, 0f),
      new Vector2(3f, -3f),
      new Vector2(-9f, -5f),
      new Vector2(9f, -5f),
      new Vector2(0f, 5f)
    };
    for (int i = 0; i < centres.length; i++) setCentre(stage.statues.get(i).entity, centres[i]);
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    FinalBossStageThreeComponent.Statue walkingPeer = stage.statues.get(1);
    Vector2 routeStart = walkingPeer.entity.getCenterPosition();
    Vector2 routeEnd = new Vector2(3f, 3f);
    walkingPeer.destination = routeEnd.cpy();
    walkingPeer.pauseRemaining = 0f;
    walkingPeer.moveRemaining = 20f;
    Rectangle obstacle = new Rectangle(-8f, -2f, 2f, 4f);
    addWall(obstacle);
    putPlayerBeside(statue);
    Vector2 before = statue.entity.getCenterPosition();
    hitThreeTimes(statue);
    tick(STEP);
    assertEvaded(statue, before);
    Vector2 destination = statue.entity.getCenterPosition();
    Vector2 position = statue.entity.getPosition();
    Vector2 size = statue.entity.getScale();
    assertFalse(obstacle.overlaps(new Rectangle(position.x, position.y, size.x, size.y)));
    assertTrue(
        Intersector.distanceSegmentPoint(routeStart, routeEnd, destination)
            >= config.statueMinSpacing + 0.15f - EPSILON,
        "A teleport must not occupy a neighbour's reserved walking route");
  }

  @Test
  void thirdRealWeaponContactQueuesTeleportUntilThePhysicsWorldUnlocks() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    hit(statue);
    hit(statue);
    Vector2 before = statue.entity.getCenterPosition();
    Body body = statue.entity.getComponent(PhysicsComponent.class).getBody();
    Vector2 bodyBefore = body.getPosition().cpy();
    Entity weapon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(1, 1))
            .addComponent(new TouchAttackComponent(PhysicsLayer.NPC));
    weapon.setPosition(statue.entity.getPosition());
    weapon.create();
    AtomicInteger contacts = new AtomicInteger();
    statue
        .entity
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity attacker) -> {
              assertSame(weapon, attacker);
              assertTrue(world.isLocked(), "This hit must occur inside a real contact callback");
              assertEquals(7, statue.hitsRemaining);
              assertPositionEquals(before, statue.entity.getCenterPosition());
              assertPositionEquals(bodyBefore, body.getPosition());
              assertTrue(stage.bursts.isEmpty());
              contacts.incrementAndGet();
            });
    world.step(PhysicsEngine.PHYSICS_TIMESTEP, 6, 2);
    assertEquals(1, contacts.get());
    assertFalse(world.isLocked());
    assertPositionEquals(before, statue.entity.getCenterPosition());
    tick(STEP);
    assertEvaded(statue, before);
    assertFalse(body.getPosition().epsilonEquals(bodyBefore, EPSILON));
  }

  @Test
  void aStatueBrokenBeforeItsQueuedEvadeNeverTeleports() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    Vector2 before = statue.entity.getCenterPosition();
    for (int i = 0; i < 10; i++) hit(statue);
    assertTrue(statue.broken);
    tick(STEP);
    assertPositionEquals(before, statue.entity.getCenterPosition());
    assertEquals(0, health(statue).getHealth());
    assertEquals(1, stage.bursts.size());
    assertTrue(stage.bursts.getFirst().grey);
  }

  @Test
  void noClearDestinationDoesNotForceATeleportIntoAnObstacle() {
    FinalBossStageThreeComponent.Statue statue = stage.statues.getFirst();
    putPlayerBeside(statue);
    Vector2 before = statue.entity.getCenterPosition();
    addWall(new Rectangle(-12f, -8f, 24f, 16f));
    hitThreeTimes(statue);
    tick(STEP);
    assertPositionEquals(before, statue.entity.getCenterPosition());
    assertEquals(7, statue.hitsRemaining);
    assertFalse(statue.broken);
    assertTrue(stage.bursts.isEmpty());
  }

  private void assertSlamCancelled(FinalBossStageThreeComponent.Statue statue) {
    assertEquals(0f, statue.warningRemaining);
    assertFalse(statue.airborne);
    assertEquals(0f, statue.jumpHeight);
    assertEquals(13.5f, statue.slamCooldown, EPSILON);
    assertNull(statue.destination);
    assertFalse(statue.entity.getComponent(PhysicsMovementComponent.class).getMoving());
    assertTrue(
        statue.entity.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().isZero());
    assertTrue(stage.shockwaves.isEmpty());
    verify(renderer, never()).shake(any(), anyFloat(), anyFloat());
  }

  private void assertEvaded(FinalBossStageThreeComponent.Statue statue, Vector2 before) {
    assertEvaded(statue, before, before);
  }

  private void assertEvaded(
      FinalBossStageThreeComponent.Statue statue, Vector2 before, Vector2 departure) {
    Vector2 after = statue.entity.getCenterPosition();
    assertTrue(after.dst(before) >= 2.5f, "Evade must noticeably move the statue away");
    assertTrue(after.dst(player.getCenterPosition()) > before.dst(player.getCenterPosition()));
    for (FinalBossStageThreeComponent.Statue other : stage.statues) {
      if (other != statue && !other.broken) {
        assertTrue(
            after.dst(other.entity.getCenterPosition())
                >= config.statueMinSpacing + 0.15f - EPSILON);
      }
    }
    Vector2 position = statue.entity.getPosition();
    Vector2 size = statue.entity.getScale();
    assertTrue(position.x >= -arenaWidth / 2f && position.x + size.x <= arenaWidth / 2f);
    assertTrue(position.y >= -arenaHeight / 2f);
    assertTrue(position.y + size.y + config.statueJumpHeight + 0.32f <= arenaHeight / 2f + EPSILON);
    assertEquals(2, stage.bursts.size());
    assertTrue(stage.bursts.stream().noneMatch(burst -> burst.grey || burst.ice));
    assertPositionEquals(departure, stage.bursts.get(0).position);
    assertPositionEquals(after, stage.bursts.get(1).position);
  }

  private void assertPositionEquals(Vector2 expected, Vector2 actual) {
    assertTrue(expected.epsilonEquals(actual, EPSILON), () -> expected + " != " + actual);
  }

  private void putPlayerBeside(FinalBossStageThreeComponent.Statue statue) {
    setCentre(player, statue.entity.getCenterPosition().add(-1f, 0f));
  }

  private void setCentre(Entity actor, Vector2 centre) {
    actor.setPosition(centre.cpy().sub(actor.getScale().scl(0.5f)));
  }

  private void addWall(Rectangle bounds) {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent());
    wall.setPosition(bounds.x, bounds.y);
    wall.setScale(bounds.width, bounds.height);
    wall.create();
  }

  private CombatStatsComponent health(FinalBossStageThreeComponent.Statue statue) {
    return statue.entity.getComponent(CombatStatsComponent.class);
  }

  private void hit(FinalBossStageThreeComponent.Statue statue) {
    health(statue).takeDamage(1, new Entity());
  }

  private void hitThreeTimes(FinalBossStageThreeComponent.Statue statue) {
    for (int i = 0; i < 3; i++) hit(statue);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }
}
