package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.SplitComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.InvisibilityEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

/** Exercises the actual factory, AI events, atlas and animation clock together. */
@ExtendWith(GameExtension.class)
class MedusaAnimationPlaybackTest {
  private GameTime time;
  private SpriteBatch batch;
  private ResourceService resources;
  private EntityService entities;
  private Entity player;
  private Entity medusa;
  private AnimationRenderComponent animator;
  private EnemyAnimationController controller;
  private PhysicsMovementComponent movement;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    OrthographicCamera camera = new OrthographicCamera(20f, 16f);
    camera.position.set(0f, 0f, 0f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    resources = new ResourceService();
    resources.loadTextureAtlases(new String[] {"images/medusa.atlas", "images/crab.atlas"});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5))
            .addComponent(new StatusEffectsControllerComponent());
    player.setPosition(2f, 0f);
    entities.register(player);
    medusa = NPCFactory.createChaseEnemy(player, false, "images/medusa.atlas");
    entities.register(medusa);
    animator = medusa.getComponent(AnimationRenderComponent.class);
    controller = medusa.getComponent(EnemyAnimationController.class);
    movement = medusa.getComponent(PhysicsMovementComponent.class);
    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
  }

  @AfterEach
  void tearDown() {
    entities.dispose();
    resources.dispose();
    ServiceLocator.getPhysicsService().getPhysics().dispose();
  }

  @Test
  void existingAiDrivesTelegraphDashAndCooldownWithoutChangingTheirTiming() {
    aiAt(0L);
    assertEquals("lungeTelegraph", animator.getCurrentAnimation());
    renderFor(0.42f);
    controller.update();
    assertEquals(6, frameIndex());
    aiAt(499L);
    assertEquals("lungeTelegraph", animator.getCurrentAnimation());
    aiAt(500L);
    assertEquals("lungeDash", animator.getCurrentAnimation());
    assertEquals(1, frameIndex());
    movement.update();
    assertEquals(
        6f, medusa.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x, 0.001f);
    renderFor(0.31f);
    controller.update();
    assertEquals(4, frameIndex());
    aiAt(899L);
    assertEquals("lungeDash", animator.getCurrentAnimation());
    aiAt(900L);
    assertEquals("default", animator.getCurrentAnimation());
    aiAt(901L);
    assertEquals("chase", animator.getCurrentAnimation());
    aiAt(2899L);
    assertEquals("chase", animator.getCurrentAnimation());
    aiAt(2900L);
    assertEquals("lungeTelegraph", animator.getCurrentAnimation());
  }

  @Test
  void wanderWaitUsesIdleAndResumesWalkingWithoutRestartingEveryUpdate() {
    medusa.getEvents().trigger("wanderStart");
    renderFor(0.21f);
    controller.update();
    controller.update();
    assertEquals("move", animator.getCurrentAnimation());
    assertEquals(3, frameIndex());
    movement.setMoving(false);
    controller.update();
    assertEquals("default", animator.getCurrentAnimation());
    movement.setMoving(true);
    controller.update();
    assertEquals("move", animator.getCurrentAnimation());
  }

  @Test
  void concealedTargetCancelsTelegraphAndTheNextAiTaskRestoresLocomotion() {
    aiAt(0L);
    assertEquals("lungeTelegraph", animator.getCurrentAnimation());
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new InvisibilityEffect(time, 5000L));
    aiAt(100L);
    aiAt(101L);
    assertEquals("move", animator.getCurrentAnimation());
    renderFor(0.2f);
    controller.update();
    assertEquals(3, frameIndex());
  }

  @Test
  void nonlethalHitPlaysHurtAndReturnsToCurrentMovement() {
    medusa.getEvents().trigger("chaseStart");
    medusa
        .getComponent(CombatStatsComponent.class)
        .hit(player.getComponent(CombatStatsComponent.class));
    assertEquals(35, medusa.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals("hurt", animator.getCurrentAnimation());
    renderFor(0.2f);
    controller.update();
    assertEquals(3, frameIndex());
    movement.setMoving(false);
    renderFor(0.11f);
    controller.update();
    assertEquals("default", animator.getCurrentAnimation());
    movement.setMoving(true);
    controller.update();
    assertEquals("chase", animator.getCurrentAnimation());
  }

  @Test
  void hitReactionCannotHideTelegraphOrDash() {
    aiAt(0L);
    medusa.getEvents().trigger("hitReaction", player);
    assertEquals("lungeTelegraph", animator.getCurrentAnimation());
    aiAt(500L);
    medusa.getEvents().trigger("hitReaction", player);
    assertEquals("lungeDash", animator.getCurrentAnimation());
  }

  @Test
  void chaseAndDashFaceTheirActualHorizontalMovement() {
    medusa.getEvents().trigger("chaseStart");
    medusa.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(-2f, 0f);
    controller.update();
    assertTrue(animator.isFlipX());
    aiAt(0L);
    controller.update();
    assertTrue(animator.isFlipX(), "telegraph retains the last facing while stopped");
    aiAt(500L);
    movement.update();
    controller.update();
    assertFalse(animator.isFlipX());
  }

  @Test
  void frozenAnimationKeepsItsFrameAndResumesAfterThaw() {
    medusa.getEvents().trigger("wanderStart");
    renderFor(0.21f);
    medusa
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new FrozenEffect(time, 5000L));
    movement.setMoving(false);
    controller.update();
    renderFor(1f);
    assertEquals("move", animator.getCurrentAnimation());
    assertEquals(3, frameIndex());
    when(time.getTime()).thenReturn(5000L);
    movement.setMoving(true);
    controller.update();
    renderFor(0.1f);
    assertEquals(4, frameIndex());
  }

  @Test
  void lethalHitKeepsAllThreeDeathFramesAndCannotBeOverridden() {
    medusa.getComponent(CombatStatsComponent.class).setHealth(5);
    medusa
        .getComponent(CombatStatsComponent.class)
        .hit(player.getComponent(CombatStatsComponent.class));
    assertEquals("dieAnimation", animator.getCurrentAnimation());
    renderFor(0.7f);
    for (String event :
        List.of(
            "wanderStart",
            "chaseStart",
            "default",
            "patrolStart",
            "lungeTelegraphStart",
            "lungeDashStart",
            "lungeDashEnd",
            "dieAnimation")) {
      medusa.getEvents().trigger(event);
    }
    medusa.getEvents().trigger("hitReaction", player);
    controller.update();
    assertEquals("dieAnimation", animator.getCurrentAnimation());
    assertEquals(3, frameIndex());
    assertFalse(animator.isFinished());
    renderFor(0.31f);
    entities.update();
    assertFalse(entities.getEntities().contains(medusa, true));
  }

  @Test
  void splittingStillCreatesTwoWeakerChildrenWithTheSameAnimationWiring() {
    Entity parent = NPCFactory.createChaseEnemy(player, true, "images/medusa.atlas");
    entities.register(parent);
    List<Entity> children = new ArrayList<>();
    parent
        .getEvents()
        .addListener(
            "spawnChildren",
            (Entity child) -> {
              children.add(child);
              entities.register(child);
            });
    parent.getComponent(CombatStatsComponent.class).setHealth(5);
    parent
        .getComponent(CombatStatsComponent.class)
        .hit(player.getComponent(CombatStatsComponent.class));
    entities.update();
    assertFalse(entities.getEntities().contains(parent, true));
    assertEquals(2, children.size());
    for (Entity child : children) {
      assertNull(child.getComponent(SplitComponent.class));
      assertEquals(30, child.getComponent(CombatStatsComponent.class).getHealth());
      assertEquals(1, child.getComponent(CombatStatsComponent.class).getBaseAttack());
      child.getEvents().trigger("lungeTelegraphStart");
      assertEquals(
          "lungeTelegraph",
          child.getComponent(AnimationRenderComponent.class).getCurrentAnimation());
    }
  }

  @Test
  void otherChaseSkinsKeepTheirExistingPlayback() {
    Entity crab = NPCFactory.createChaseEnemy(player, false, "images/crab.atlas");
    entities.register(crab);
    crab.getEvents().trigger("chaseStart");
    crab.getEvents().trigger("lungeTelegraphStart");
    crab.getEvents().trigger("hitReaction", player);
    crab.getComponent(PhysicsMovementComponent.class).setMoving(false);
    crab.getComponent(EnemyAnimationController.class).update();
    assertEquals("chase", crab.getComponent(AnimationRenderComponent.class).getCurrentAnimation());
  }

  @ParameterizedTest
  @CsvSource({"1, false", "1, true", "0.8333333, false", "0.8333333, true"})
  void paddedFramesPreserveBodyHeightAndGroundAnchorWithoutScalingTheEntity(
      float size, boolean flip) {
    medusa.setPosition(2f, 3f);
    medusa.setScale(size, size);
    animator.setFlipX(flip);
    renderFor(0f);
    ArgumentCaptor<Float> bounds = ArgumentCaptor.forClass(Float.class);
    verify(batch)
        .draw(
            org.mockito.ArgumentMatchers.any(TextureRegion.class),
            bounds.capture(),
            bounds.capture(),
            bounds.capture(),
            bounds.capture());
    List<Float> draw = bounds.getAllValues();
    float pixelSize = size / 85f;
    assertEquals(128 * pixelSize, draw.get(2), 0.00001f);
    assertEquals(draw.get(2), draw.get(3), "original square cells keep their proportions");
    // The former 65x85 crop included eight transparent pixels below the 77px body.
    assertEquals(3f + 8 * pixelSize, draw.get(1), 0.00001f);
    float bodyCentreInFrame = flip ? 63.5f : 64.5f;
    assertEquals(2f + size / 2f, draw.get(0) + bodyCentreInFrame * pixelSize, 0.00001f);
    assertEquals(size, medusa.getScale().x);
    assertEquals(size, medusa.getScale().y);
  }

  private void aiAt(long millis) {
    when(time.getTime()).thenReturn(millis);
    medusa.getComponent(AITaskComponent.class).update();
    controller.update();
  }

  private void renderFor(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    animator.render(batch);
  }

  private int frameIndex() {
    return ((TextureAtlas.AtlasRegion) animator.getCurrentFrame()).index + 1;
  }
}
