package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** Checks visual contact feedback against the real, unchanged collision damage path. */
@ExtendWith(GameExtension.class)
class CyclopsAnimationPlaybackTest {
  private GameTime time;
  private SpriteBatch batch;
  private ResourceService resources;
  private EntityService entities;
  private Entity player;
  private Entity cyclops;
  private AnimationRenderComponent animator;
  private EnemyAnimationController controller;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    OrthographicCamera camera = new OrthographicCamera(20f, 16f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);
    RenderService render = new RenderService();
    render.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(render);
    resources = new ResourceService();
    resources.loadTextureAtlases(
        new String[] {"images/giant-enemy/cyclops.atlas", "images/giant-enemy/mummy.atlas"});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(3f, 0f);
    entities.register(player);
    cyclops = giant("images/giant-enemy/cyclops.atlas");
    animator = cyclops.getComponent(AnimationRenderComponent.class);
    controller = cyclops.getComponent(EnemyAnimationController.class);
    cyclops.getEvents().trigger("chaseStart");
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
  void contactPlaysOnceWhileDamageAndKnockbackKeepTheUpstreamRepeatInterval() {
    contact(cyclops);
    assertEquals(97, player.getComponent(CombatStatsComponent.class).getHealth());
    var body = player.getComponent(PhysicsComponent.class).getBody();
    assertEquals(1.5f, body.getLinearVelocity().len() * body.getMass(), 0.0001f);
    assertEquals("attack", animator.getCurrentAnimation());
    renderFor(0.25f);
    assertEquals(3, frameIndex());
    renderFor(0.08f);
    controller.update();
    assertEquals("chase", animator.getCurrentAnimation());
    when(time.getDeltaTime()).thenReturn(0.99f);
    cyclops.getComponent(TouchAttackComponent.class).update();
    assertEquals(97, player.getComponent(CombatStatsComponent.class).getHealth());
    when(time.getDeltaTime()).thenReturn(0.02f);
    cyclops.getComponent(TouchAttackComponent.class).update();
    assertEquals(94, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(3f, body.getLinearVelocity().len() * body.getMass(), 0.0001f);
  }

  @Test
  void movementAndFurtherContactsCannotRestartThePlayingVisual() {
    contact(cyclops);
    renderFor(0.17f);
    TextureRegion frame = animator.getCurrentFrame();
    cyclops.getEvents().trigger("wanderStart");
    cyclops.getEvents().trigger("moving", new Vector2(-1, 0));
    cyclops.getEvents().trigger("contactAttack", player);
    assertEquals("attack", animator.getCurrentAnimation());
    assertSame(frame, animator.getCurrentFrame());
    assertEquals(
        97,
        player.getComponent(CombatStatsComponent.class).getHealth(),
        "A visual event must not apply extra collision damage");
    renderFor(0.16f);
    controller.update();
    assertEquals("move", animator.getCurrentAnimation());
  }

  @Test
  void completedContactReturnsToIdleWhenTheExistingMovementStops() {
    contact(cyclops);
    cyclops.getComponent(PhysicsMovementComponent.class).setMoving(false);
    renderFor(0.33f);
    controller.update();
    assertEquals("default", animator.getCurrentAnimation());
  }

  @Test
  void contactFacesThePlayerAndHoldsFacingUntilTheClipFinishes() {
    player.setPosition(-3f, 0f);
    contact(cyclops);
    assertTrue(animator.isFlipX());
    cyclops.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(0.5f, 0);
    renderFor(0.17f);
    controller.update();
    assertTrue(animator.isFlipX());
    renderFor(0.16f);
    controller.update();
    assertFalse(animator.isFlipX());
  }

  @Test
  void frozenContactKeepsItsFrameAndConcealedPlayerDoesNotTriggerFeedback() {
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(StatusEffectsFactory.createInvisibility(time, 5000));
    contact(cyclops);
    assertEquals("chase", animator.getCurrentAnimation());
    assertEquals(100, player.getComponent(CombatStatsComponent.class).getHealth());
    player.getComponent(StatusEffectsControllerComponent.class).clearStatusEffects();
    when(time.getDeltaTime()).thenReturn(1f);
    cyclops.getComponent(TouchAttackComponent.class).update();
    renderFor(0.17f);
    cyclops
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000));
    renderFor(1f);
    controller.update();
    assertEquals("attack", animator.getCurrentAnimation());
    assertEquals(2, frameIndex());
  }

  @Test
  void deathInterruptsContactReachesTheLyingFrameAndDisposesWithinOneSecond() {
    contact(cyclops);
    cyclops.getComponent(CombatStatsComponent.class).setHealth(0);
    renderFor(0.65f);
    assertEquals("dieAnimation", animator.getCurrentAnimation());
    assertEquals(8, frameIndex());
    TextureAtlas.AtlasRegion frame = (TextureAtlas.AtlasRegion) animator.getCurrentFrame();
    assertEquals(512, frame.getRegionX());
    assertEquals(384, frame.getRegionY());
    cyclops.getEvents().trigger("contactAttack", player);
    cyclops.getEvents().trigger("chaseStart");
    cyclops.getEvents().trigger("moving", new Vector2(1, 0));
    controller.update();
    assertSame(frame, animator.getCurrentFrame());
    assertFalse(animator.isFinished());
    renderFor(0.08f);
    controller.update();
    entities.update();
    assertFalse(entities.getEntities().contains(cyclops, true));
  }

  @Test
  void otherGiantSkinsRetainTheirContactPlaybackAndExistingDeathTiming() {
    Entity mummy = giant("images/giant-enemy/mummy.atlas");
    AnimationRenderComponent animation = mummy.getComponent(AnimationRenderComponent.class);
    mummy.getEvents().trigger("chaseStart");
    contact(mummy);
    assertEquals("chase", animation.getCurrentAnimation());
    assertEquals(97, player.getComponent(CombatStatsComponent.class).getHealth());
    mummy.getComponent(CombatStatsComponent.class).setHealth(0);
    when(time.getDeltaTime()).thenReturn(0.49f);
    animation.render(batch);
    mummy.getComponent(EnemyAnimationController.class).update();
    assertFalse(animation.isFinished());
    assertTrue(entities.getEntities().contains(mummy, true));
    when(time.getDeltaTime()).thenReturn(0.02f);
    animation.render(batch);
    assertTrue(animation.isFinished());
  }

  @Test
  void fullCellsKeepTheExistingBodySizeAndGroundWithoutChangingPhysicsScale() {
    cyclops.setPosition(2f, 3f);
    cyclops.setScale(1.5f, 1.5f);
    animator.startAnimation("default");
    renderFor(0f);
    ArgumentCaptor<Float> bounds = ArgumentCaptor.forClass(Float.class);
    verify(batch)
        .draw(
            any(TextureRegion.class),
            bounds.capture(),
            bounds.capture(),
            bounds.capture(),
            bounds.capture());
    List<Float> draw = bounds.getAllValues();
    assertEquals(1.5f, draw.get(2) * 28f / 64f, 0.00001f);
    assertEquals(1.5f, draw.get(3) * 40f / 64f, 0.00001f);
    assertEquals(2f, draw.get(0) + 19f / 64f * draw.get(2), 0.00001f);
    assertEquals(3f + 2f / 40f * 1.5f, draw.get(1), 0.00001f);
    assertEquals(new Vector2(1.5f, 1.5f), cyclops.getScale());
  }

  private Entity giant(String skin) {
    Entity enemy = NPCFactory.createGiantEnemy(player, skin);
    entities.register(enemy);
    return enemy;
  }

  private void contact(Entity enemy) {
    enemy
        .getEvents()
        .trigger(
            "collisionStart",
            enemy.getComponent(HitboxComponent.class).getFixture(),
            player.getComponent(HitboxComponent.class).getFixture());
  }

  private void renderFor(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    animator.render(batch);
  }

  private int frameIndex() {
    return ((TextureAtlas.AtlasRegion) animator.getCurrentFrame()).index;
  }
}
