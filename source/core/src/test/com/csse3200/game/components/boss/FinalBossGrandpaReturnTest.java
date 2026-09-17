package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FinalBossGrandpaReturnTest {
  private static final float EPSILON = 0.001f;
  private static final float TRANSFORM_SIZE = 2.4f;
  private FinalBossStageThreeComponent stage;
  private FinalBossStageThreeConfig config;
  private Entity boss;
  private Entity player;
  private GameTime time;
  private World world;
  private OrthographicCamera camera;

  @BeforeEach
  void setup() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    ServiceLocator.registerEntityService(mock(EntityService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(0.1f);
    player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions());
    player.create();
    setCentre(player, new Vector2());
    camera = new OrthographicCamera(16f, 8f);
    FinalBossMovementComponent movement = mock(FinalBossMovementComponent.class);
    when(movement.getCamera()).thenReturn(camera);
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
    config = new FinalBossStageThreeConfig();
    config.floatingDemonCount = 0; // Isolate statue/ice mechanics from rendered summons.
    stage = new FinalBossStageThreeComponent(player, Entity::create, config);
    boss =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(phases)
            .addComponent(movement)
            .addComponent(new FinalBossDamageControllerComponent())
            .addComponent(stage);
    boss.setScale(2f, 2f);
    boss.setPosition(30f, 30f);
    boss.create();
    boss.getComponent(CombatStatsComponent.class).setHealth(40);
    stage.update();
  }

  @AfterEach
  void disposeWorld() {
    world.dispose();
  }

  @Test
  void offscreenBossReturnsBesidePlayerFromTheFirstEndingUpdateThroughCompletion() {
    startEnding();
    tick(0.02f);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertVisibleAndNearPlayer();
    Vector2 firstPosition = boss.getCenterPosition();
    tick(config.returnTransformDuration - 0.03f);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
    assertPositionEquals(firstPosition, boss.getCenterPosition());
    tick(0.02f);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertVisibleAndNearPlayer();
    assertPositionEquals(firstPosition, boss.getCenterPosition());
    assertTrue(boss.getComponent(PhysicsComponent.class).getBody().isActive());
  }

  @Test
  void eachCameraCornerLeavesTheReturningSpriteAndTransformationFullyVisible() {
    startEnding();
    for (Vector2 corner :
        new Vector2[] {
          new Vector2(-7.25f, -3.25f),
          new Vector2(7.25f, -3.25f),
          new Vector2(-7.25f, 3.25f),
          new Vector2(7.25f, 3.25f)
        }) {
      setCentre(player, corner);
      tick(0.1f);
      assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
      assertVisibleAndNearPlayer();
    }
    tick(config.returnTransformDuration);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertVisibleAndNearPlayer();
  }

  @Test
  void movingAndZoomingTheCameraDuringTheReturnUsesThePlayersLatestLocation() {
    startEnding();
    tick(0.1f);
    Vector2 oldReturn = boss.getCenterPosition();
    camera.position.set(20f, 30f, 0f);
    camera.zoom = 0.75f;
    setCentre(player, new Vector2(20f, 30f));
    tick(0.3f);
    assertVisibleAndNearPlayer();
    assertTrue(oldReturn.dst(boss.getCenterPosition()) > 10f);
    tick(config.returnTransformDuration);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertVisibleAndNearPlayer();
  }

  @Test
  void aStaticWallBesideThePlayerUsesAnotherClearNearbyReturnPosition() {
    startEnding();
    Rectangle obstacle = new Rectangle(0.7f, -2.5f, 5f, 5f);
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent());
    wall.setPosition(obstacle.x, obstacle.y);
    wall.setScale(obstacle.width, obstacle.height);
    wall.create();
    tick(0.1f);
    assertVisibleAndNearPlayer();
    assertFalse(obstacle.overlaps(spriteBounds()));
    tick(config.returnTransformDuration);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertVisibleAndNearPlayer();
    assertFalse(obstacle.overlaps(spriteBounds()));
  }

  @Test
  void peacefulGrandpaStaysWhereHeReturnedWhenThePlayerWalksAway() {
    startEnding();
    tick(config.returnTransformDuration);
    assertEquals(FinalBossStageThreeState.PEACEFUL, stage.getState());
    assertVisibleAndNearPlayer();
    Vector2 returnedPosition = boss.getCenterPosition();
    setCentre(player, new Vector2(20f, 20f));
    camera.position.set(20f, 20f, 0f);
    tick(1f);
    tick(1f);
    assertPositionEquals(returnedPosition, boss.getCenterPosition());
  }

  @Test
  void theLastRealWeaponContactDefersRepositioningUntilPhysicsIsUnlocked() {
    startStatues();
    for (int index = 0; index < config.statueCount - 1; index++) {
      breakStatue(stage.statues.get(index));
    }
    FinalBossStageThreeComponent.Statue last = stage.statues.getLast();
    for (int hit = 1; hit < config.statueHits; hit++) stage.hitStatue(last);
    Vector2 oldBossPosition = boss.getCenterPosition();
    Vector2 oldBodyPosition =
        boss.getComponent(PhysicsComponent.class).getBody().getPosition().cpy();
    Entity weapon =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(1, 1))
            .addComponent(new TouchAttackComponent(PhysicsLayer.NPC));
    weapon.setPosition(last.entity.getPosition());
    weapon.create();
    AtomicInteger contacts = new AtomicInteger();
    last.entity
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity attacker) -> {
              assertSame(weapon, attacker);
              assertTrue(world.isLocked());
              assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
              assertPositionEquals(oldBossPosition, boss.getCenterPosition());
              assertPositionEquals(
                  oldBodyPosition,
                  boss.getComponent(PhysicsComponent.class).getBody().getPosition());
              contacts.incrementAndGet();
            });
    world.step(1f / 60f, 6, 2);
    assertEquals(1, contacts.get());
    assertFalse(world.isLocked());
    tick(0.02f);
    assertVisibleAndNearPlayer();
  }

  private void startEnding() {
    startStatues();
    for (FinalBossStageThreeComponent.Statue statue : stage.statues) breakStatue(statue);
    assertEquals(FinalBossStageThreeState.ENDING, stage.getState());
  }

  private void startStatues() {
    boss.getComponent(CombatStatsComponent.class).takeDamage(30, player);
    tick(config.chargeDuration);
    assertEquals(FinalBossStageThreeState.WAVE_TWO, stage.getState());
  }

  private void breakStatue(FinalBossStageThreeComponent.Statue statue) {
    for (int hit = 0; hit < config.statueHits; hit++) stage.hitStatue(statue);
  }

  private void assertVisibleAndNearPlayer() {
    Vector2 centre = boss.getCenterPosition();
    assertTrue(
        centre.dst(player.getCenterPosition()) <= 3.2f,
        "Grandpa should return beside the current player");
    float halfWidth = Math.max(TRANSFORM_SIZE, boss.getScale().x) / 2f;
    float halfHeight = Math.max(TRANSFORM_SIZE, boss.getScale().y) / 2f;
    float left = camera.position.x - camera.viewportWidth * camera.zoom / 2f;
    float bottom = camera.position.y - camera.viewportHeight * camera.zoom / 2f;
    assertTrue(centre.x - halfWidth >= left - EPSILON);
    assertTrue(centre.x + halfWidth <= left + camera.viewportWidth * camera.zoom + EPSILON);
    assertTrue(centre.y - halfHeight >= bottom - EPSILON);
    assertTrue(centre.y + halfHeight <= bottom + camera.viewportHeight * camera.zoom + EPSILON);
  }

  private Rectangle spriteBounds() {
    Vector2 position = boss.getPosition();
    Vector2 size = boss.getScale();
    return new Rectangle(position.x, position.y, size.x, size.y);
  }

  private void setCentre(Entity actor, Vector2 centre) {
    actor.setPosition(centre.cpy().sub(actor.getScale().scl(0.5f)));
  }

  private void assertPositionEquals(Vector2 expected, Vector2 actual) {
    assertTrue(expected.epsilonEquals(actual, EPSILON), () -> expected + " != " + actual);
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    stage.update();
  }
}
