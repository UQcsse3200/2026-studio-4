package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.rooms.FollowingCameraComponent;
import com.csse3200.game.components.rooms.WallComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoArenaComponentTest {
  private Entity player;
  private Entity boss;
  private Entity cameraEntity;
  private OrthographicCamera camera;
  private FollowingCameraComponent following;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossStageTwoArenaComponent arena;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10));
    player.setPosition(4f, 7f);
    player.create();
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_ONE);
    arena = new FinalBossStageTwoArenaComponent(player, new FinalBossStageTwoConfig());
    boss =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(arena);
    boss.setScale(2f, 2f);
    boss.setPosition(8f, 8f);
    boss.create();
    camera = new OrthographicCamera(20f, 10f);
    CameraComponent cameraComponent = new CameraComponent(camera);
    cameraEntity = new Entity().addComponent(cameraComponent);
    cameraEntity.setPosition(10f, 10f);
    cameraEntity.create();
    cameraComponent.update();
    following = new FollowingCameraComponent();
    following.setCamera(cameraComponent);
    following.setTarget(player);
    WallComponent walls = mock(WallComponent.class);
    when(walls.getWallBounds()).thenReturn(new Vector2(100f, 100f));
    new Entity().addComponent(walls).addComponent(following).create();
    arena.setCamera(camera, following);
  }

  @Test
  void activatesInStageTwoTransitionAndReleasesImmediatelyOnPhaseExit() {
    arena.update();
    assertNull(arena.getBounds());
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    when(phases.isTransitioning()).thenReturn(true);
    player.setPosition(30f, 10f);
    boss.getEvents().trigger(FinalBossEvents.PHASE_CHANGED, FinalBossPhase.STAGE_TWO);
    assertEquals(30f, player.getPosition().x);
    assertNull(arena.getBounds());
    arena.update();
    assertEquals(new Rectangle(0f, 5f, 20f, 10f), arena.getBounds());
    assertEquals(18.75f, player.getPosition().x);
    following.update();
    assertEquals(10f, camera.position.x);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    boss.getEvents().trigger(FinalBossEvents.PHASE_CHANGED, FinalBossPhase.STAGE_THREE);
    assertNull(arena.getBounds());
    following.update();
    assertTrue(cameraEntityPosition().x > 10f);
  }

  @Test
  void usesZoomAndOnlyShrinksTheOriginalArenaWhenViewportChanges() {
    camera.zoom = 2f;
    activate();
    assertEquals(new Rectangle(-10f, 0f, 40f, 20f), arena.getBounds());
    Rectangle copy = arena.getBounds();
    copy.width = 999f;
    assertEquals(40f, arena.getBounds().width);
    camera.viewportWidth = 5f;
    camera.viewportHeight = 4f;
    arena.update();
    assertEquals(new Rectangle(5f, 6f, 10f, 8f), arena.getBounds());
    camera.viewportWidth = 50f;
    camera.viewportHeight = 40f;
    arena.update();
    assertEquals(new Rectangle(-10f, 0f, 40f, 20f), arena.getBounds());
  }

  @Test
  void clampsFreshPhysicsPositionsAndRemovesOnlyOutwardVelocity() {
    activate();
    PhysicsComponent playerPhysics = player.getComponent(PhysicsComponent.class);
    playerPhysics.getBody().setTransform(-3f, 11f, 0f);
    playerPhysics.getBody().setLinearVelocity(-6f, 2f);
    PhysicsComponent bossPhysics = boss.getComponent(PhysicsComponent.class);
    bossPhysics.getBody().setTransform(22f, 20f, 0f);
    bossPhysics.getBody().setLinearVelocity(6f, 7f);
    arena.update();
    assertEquals(new Vector2(0.25f, 11f), player.getPosition());
    assertEquals(new Vector2(0f, 2f), playerPhysics.getBody().getLinearVelocity());
    assertEquals(new Vector2(17.75f, 12.75f), boss.getPosition());
    assertEquals(Vector2.Zero, bossPhysics.getBody().getLinearVelocity());
    playerPhysics.getBody().setTransform(-3f, 11f, 0f);
    playerPhysics.getBody().setLinearVelocity(4f, 2f);
    arena.update();
    assertEquals(new Vector2(4f, 2f), playerPhysics.getBody().getLinearVelocity());
  }

  @Test
  void phaseExitDuringBossContainmentDoesNotClampThePlayerWithClearedBounds() {
    activate();
    boss.setPosition(22f, 20f);
    player.setPosition(-4f, -4f);
    boss.getEvents()
        .addListener(
            "setPosition",
            (Vector2 position) -> {
              when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
              boss.getEvents().trigger(FinalBossEvents.PHASE_CHANGED, FinalBossPhase.STAGE_THREE);
            });

    assertDoesNotThrow(arena::update);

    assertNull(arena.getBounds());
    assertEquals(new Vector2(-4f, -4f), player.getPosition());
  }

  @Test
  void movementBoundsIncludeWholeActorAndRemainValidInTinyView() {
    activate();
    player.setScale(1.5f, 2.5f);
    assertEquals(new Rectangle(0.25f, 5.25f, 18f, 7f), arena.getMovementBounds(player));
    camera.viewportWidth = 1f;
    camera.viewportHeight = 1f;
    arena.update();
    assertEquals(new Rectangle(9.25f, 8.75f, 0f, 0f), arena.getMovementBounds(player));
    assertEquals(new Vector2(10f, 10f), player.getCenterPosition());
  }

  @Test
  void finishesContainmentAfterLaterPlayerMovementAndIgnoresQueuedWorkAfterExit() {
    EntityService entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    activate();
    ArgumentCaptor<Runnable> afterUpdates = ArgumentCaptor.forClass(Runnable.class);
    verify(entities).runAfterUpdate(afterUpdates.capture());
    PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
    player.setPosition(0.25f, 10f);
    physics.getBody().setLinearVelocity(-8f, 2f);
    afterUpdates.getValue().run();
    assertEquals(new Vector2(0f, 2f), physics.getBody().getLinearVelocity());
    arena.dispose();
    player.setPosition(-4f, 10f);
    physics.getBody().setLinearVelocity(-8f, 2f);
    afterUpdates.getValue().run();
    assertEquals(-4f, player.getPosition().x);
    assertEquals(new Vector2(-8f, 2f), physics.getBody().getLinearVelocity());
  }

  @Test
  void playerDeathReleasesTheCameraAndDoesNotReactivate() {
    activate();
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    arena.update();
    assertNull(arena.getBounds());
    player.setPosition(15f, 10f);
    following.update();
    assertTrue(cameraEntityPosition().x > 10f);
  }

  @Test
  void bossDeathReleasesOnlyThisArenasLock() {
    Object otherOwner = new Object();
    following.lockPosition(otherOwner);
    activate();
    boss.getComponent(CombatStatsComponent.class).setHealth(0);
    assertNull(arena.getBounds());
    player.setPosition(15f, 10f);
    following.update();
    assertEquals(new Vector2(10f, 10f), cameraEntityPosition());
    following.unlockPosition(otherOwner);
    following.update();
    assertTrue(cameraEntityPosition().x > 10f);
  }

  @Test
  void disposalReleasesActiveCameraWithoutReadingDisposedBodies() {
    activate();
    player.setPosition(15f, 10f);
    player.getComponent(PhysicsComponent.class).dispose();
    boss.getComponent(PhysicsComponent.class).dispose();
    assertDoesNotThrow(arena::dispose);
    assertNull(arena.getBounds());
    following.update();
    assertTrue(cameraEntityPosition().x > 10f);
  }

  @Test
  void missingOrInvalidCameraDoesNotConfineActors() {
    arena.setCamera(null, null);
    activate();
    assertNull(arena.getBounds());
    arena.setCamera(camera, following);
    camera.viewportWidth = 0f;
    player.setPosition(-4f, -4f);
    arena.update();
    assertNull(arena.getBounds());
    assertEquals(new Vector2(-4f, -4f), player.getPosition());
  }

  private void activate() {
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    arena.update();
  }

  private Vector2 cameraEntityPosition() {
    return cameraEntity.getPosition();
  }
}
