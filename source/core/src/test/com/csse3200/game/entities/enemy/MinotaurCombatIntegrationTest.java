package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.MinotaurAnimationController;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.components.tasks.LungeAttackTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.NPCFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.MinotaurAttackRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises frame-timed minotaur damage through real collision fixtures and lunge transitions. */
@ExtendWith(GameExtension.class)
class MinotaurCombatIntegrationTest {
  private GameTime time;
  private Entity player;
  private Entity minotaur;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerPhysicsService(new PhysicsService());
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(time);
    ResourceService resources = new ResourceService();
    resources.loadTextureAtlases(
        new String[] {
          "images/minotaur.atlas", "images/minotaur-combat/attacks.atlas", "images/crab.atlas"
        });
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 0))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(1f, 0f);
    player.create();
    minotaur = NPCFactory.createChaseEnemy(player, false, "images/minotaur.atlas");
    // EnemyManagerComponent uses this square world size for the minotaur room spawn.
    minotaur.setScale(1.5f, 1.5f);
    ServiceLocator.getEntityService().register(minotaur);
  }

  @Test
  void normalContactDamagesAndKnocksBackOnlyWhenAxeReachesFifthFrame() {
    int[] hits = observeImpactFrame("normalAttack", 4);
    contact("collisionStart");
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    assertEquals(20, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());

    tick(0.399f);
    assertEquals(20, health());
    assertEquals(0, hits[0]);
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
    tick(0.001f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
    assertEquals(1.5f, playerBody().getLinearVelocity().len(), 0.0001f);
    tick(0.25f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
  }

  @Test
  void longUpdatePresentsImpactFrameWhenDamageLandsInsteadOfSkippingToRecovery() {
    int[] hits = observeImpactFrame("normalAttack", 4);
    contact("collisionStart");
    tick(1f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
    assertFrame("normalAttack", 4);
    tick(0.1f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
    assertFrame("normalAttack", 5);
  }

  @Test
  void dashContactDamagesOnlyWhenChargeAxeReachesFourthFrame() {
    int[] hits = observeImpactFrame("chargeAttack", 3);
    LungeAttackTask lunge = createLunge();
    lunge.start();
    when(time.getTime()).thenReturn(500L);
    lunge.update();
    contact("collisionStart");
    assertEquals("chargeAttack", attackRenderer().getCurrentAnimation());
    assertEquals(20, health());
    tick(0.299f);
    assertEquals(20, health());
    assertEquals(0, hits[0]);
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
    tick(0.001f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
    assertEquals(1.5f, playerBody().getLinearVelocity().len(), 0.0001f);
    tick(0.25f);
    assertEquals(18, health());
    assertEquals(1, hits[0]);
  }

  @Test
  void continuingContactStartsNextSwingOnlyOneSecondAfterPreviousStart() {
    contact("collisionStart");
    tick(0.4f);
    assertEquals(18, health());
    tick(0.3f);
    tick(0.299f);
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals(18, health());
    tick(0.001f);
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    assertEquals(18, health());
    assertFrame("normalAttack", 0);
    tick(0.399f);
    assertEquals(18, health());
    tick(0.001f);
    assertEquals(16, health());
  }

  @Test
  void leavingAtImpactWhiffsAndLaterReentryCannotHitLateOrBypassCooldown() {
    contact("collisionStart");
    tick(0.2f);
    contact("collisionEnd");
    tick(0.2f);
    assertEquals(20, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
    contact("collisionStart");
    tick(0.3f);
    tick(0.299f);
    assertEquals(20, health());
    assertNull(attackRenderer().getCurrentAnimation());
    tick(0.001f);
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    tick(0.4f);
    assertEquals(18, health());
    contact("collisionEnd");
    tick(2f);
    assertEquals(18, health());
    assertNull(attackRenderer().getCurrentAnimation());
  }

  @Test
  void reentryBeforeImpactKeepsOriginalSwingClockAndCanStillBeHit() {
    contact("collisionStart");
    tick(0.2f);
    contact("collisionEnd");
    contact("collisionStart");
    assertFrame("normalAttack", 2);
    tick(0.199f);
    assertEquals(20, health());
    tick(0.001f);
    assertEquals(18, health());
    assertFrame("normalAttack", 4);
  }

  @Test
  void concealedVictimAtImpactTakesNeitherDamageNorKnockback() {
    contact("collisionStart");
    tick(0.399f);
    player
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(StatusEffectsFactory.createInvisibility(time, 5000L));
    tick(0.001f);
    assertEquals(20, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
  }

  @Test
  void deadVictimAtImpactIsNotKnockedBack() {
    contact("collisionStart");
    tick(0.399f);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(0.001f);
    assertEquals(0, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
  }

  @Test
  void frozenAttackerPausesBothAnimationAndDamageClockUntilThaw() {
    contact("collisionStart");
    tick(0.2f);
    StatusEffectsControllerComponent effects =
        minotaur.getComponent(StatusEffectsControllerComponent.class);
    effects.addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000L));
    tick(2f);
    assertEquals(20, health());
    assertFrame("normalAttack", 2);
    effects.clearStatusEffects();
    tick(0.199f);
    assertEquals(20, health());
    tick(0.001f);
    assertEquals(18, health());
  }

  @Test
  void lungeRequestCancelsPendingNormalAndTelegraphContactCannotHit() {
    contact("collisionStart");
    tick(0.3f);
    LungeAttackTask lunge = createLunge();
    lunge.start();
    assertNull(attackRenderer().getCurrentAnimation());
    tick(0.5f);
    assertEquals(20, health());
    contact("collisionEnd");
    contact("collisionStart");
    tick(0.5f);
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals(20, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
  }

  @Test
  void laterEntityLungeTransitionCancelsImpactBeforeDeferredDamageSettles() {
    minotaur.getComponent(AITaskComponent.class).setEnabled(false);
    contact("collisionStart");
    tick(0.3f);
    assertEquals(20, health());
    LungeAttackTask lunge = createLunge();
    // This entity runs after the minotaur has queued its impact update. Using another entity
    // makes that order deterministic regardless of the minotaur's component iteration order.
    Entity laterAiDriver =
        new Entity()
            .addComponent(
                new Component() {
                  @Override
                  public void update() {
                    lunge.start();
                  }
                });
    ServiceLocator.getEntityService().register(laterAiDriver);
    when(time.getDeltaTime()).thenReturn(0.1f);
    when(time.getTime()).thenReturn(400L);

    ServiceLocator.getEntityService().update();

    assertEquals(20, health());
    assertEquals(Vector2.Zero, playerBody().getLinearVelocity());
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals("move", locomotion().getCurrentAnimation());
    assertFalse(minotaur.getComponent(PhysicsMovementComponent.class).getMoving());
  }

  @Test
  void missedLungeKeepsWalkVisualAndOriginalTelegraphDashAndCooldownBoundaries() {
    LungeAttackTask lunge = createLunge();
    PhysicsMovementComponent movement = minotaur.getComponent(PhysicsMovementComponent.class);
    Body body = minotaur.getComponent(PhysicsComponent.class).getBody();
    lunge.start();
    assertFalse(movement.getMoving());
    assertEquals(Vector2.Zero, body.getLinearVelocity());
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals("move", locomotion().getCurrentAnimation());
    when(time.getTime()).thenReturn(499L);
    lunge.update();
    assertFalse(movement.getMoving());
    assertEquals(20, lunge.getPriority());
    when(time.getTime()).thenReturn(500L);
    lunge.update();
    assertTrue(movement.getMoving());
    movement.update();
    assertEquals(6f, body.getLinearVelocity().x, 0.0001f);
    assertEquals(0f, body.getLinearVelocity().y, 0.0001f);
    assertEquals("move", locomotion().getCurrentAnimation());
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals(20, health());
    when(time.getTime()).thenReturn(899L);
    lunge.update();
    assertTrue(movement.getMoving());
    when(time.getTime()).thenReturn(900L);
    lunge.update();
    assertFalse(movement.getMoving());
    assertEquals(Vector2.Zero, body.getLinearVelocity());
    assertEquals(-1, lunge.getPriority());
    lunge.stop();
    when(time.getTime()).thenReturn(2899L);
    assertEquals(-1, lunge.getPriority());
    when(time.getTime()).thenReturn(2900L);
    assertEquals(20, lunge.getPriority());
    assertNull(attackRenderer().getCurrentAnimation());
    assertEquals(20, health());
  }

  @Test
  void fullCanvasAttacksLeaveSquareWorldSizeAndExistingPhysicsFixturesUnchanged() {
    Fixture hitbox = minotaur.getComponent(HitboxComponent.class).getFixture();
    Fixture collider = minotaur.getComponent(ColliderComponent.class).getFixture();
    assertPhysicsShape(hitbox, collider);
    contact("collisionStart");
    assertNotNull(attackRenderer().getCurrentFrame());
    assertEquals(288, attackRenderer().getCurrentFrame().getRegionWidth());
    assertEquals(160, attackRenderer().getCurrentFrame().getRegionHeight());
    assertPhysicsShape(hitbox, collider);
    contact("collisionEnd");
    LungeAttackTask lunge = createLunge();
    lunge.start();
    when(time.getTime()).thenReturn(500L);
    lunge.update();
    tick(1f);
    contact("collisionStart");
    assertEquals("chargeAttack", attackRenderer().getCurrentAnimation());
    assertPhysicsShape(hitbox, collider);
  }

  @Test
  void otherChaseSkinsRetainImmediateSharedTouchAttackWithoutMinotaurAttackLayer() {
    Entity crab = NPCFactory.createChaseEnemy(player, false, "images/crab.atlas");
    crab.create();
    assertNull(minotaur.getComponent(EnemyAnimationController.class));
    assertTrue(
        minotaur.getComponent(MinotaurAnimationController.class)
            instanceof EnemyAnimationController);
    assertNull(minotaur.getComponent(TouchAttackComponent.class));
    assertNotNull(crab.getComponent(TouchAttackComponent.class));
    assertNotNull(crab.getComponent(EnemyAnimationController.class));
    assertNull(crab.getComponent(MinotaurAnimationController.class));
    assertNull(crab.getComponent(MinotaurAttackRenderComponent.class));
    crab.getEvents()
        .trigger(
            "collisionStart",
            crab.getComponent(HitboxComponent.class).getFixture(),
            player.getComponent(HitboxComponent.class).getFixture());
    assertEquals(18, health());
    AnimationRenderComponent crabAnimations = crab.getComponent(AnimationRenderComponent.class);
    crab.getEvents().trigger("chaseStart");
    assertEquals("chase", crabAnimations.getCurrentAnimation());
    assertFalse(crabAnimations.hasAnimation("normalAttack"));
    assertFalse(crabAnimations.hasAnimation("chargeAttack"));
  }

  private int[] observeImpactFrame(String animation, int index) {
    int[] hits = {0};
    player
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer lost, Integer remaining) -> {
              hits[0]++;
              assertSame(minotaur, attacker);
              assertEquals(2, lost);
              assertEquals(18, remaining);
              assertEquals(animation, attackRenderer().getCurrentAnimation());
              assertFrame(animation, index);
            });
    return hits;
  }

  private void assertFrame(String animation, int index) {
    TextureRegion expected =
        ServiceLocator.getResourceService()
            .getAsset("images/minotaur-combat/attacks.atlas", TextureAtlas.class)
            .findRegions(animation)
            .get(index);
    TextureRegion actual = attackRenderer().getCurrentFrame();
    assertNotNull(actual);
    assertEquals(expected.getU(), actual.getU());
    assertEquals(expected.getV(), actual.getV());
    assertEquals(expected.getU2(), actual.getU2());
    assertEquals(expected.getV2(), actual.getV2());
  }

  private void assertPhysicsShape(Fixture hitbox, Fixture collider) {
    assertEquals(new Vector2(1.5f, 1.5f), minotaur.getScale());
    assertSame(hitbox, minotaur.getComponent(HitboxComponent.class).getFixture());
    assertSame(collider, minotaur.getComponent(ColliderComponent.class).getFixture());
    assertTrue(hitbox.isSensor());
    assertEquals(PhysicsLayer.NPC, hitbox.getFilterData().categoryBits);
    assertBounds(hitbox, 0f, 0f, 1.5f, 1.5f);
    assertBounds(collider, 0.05f, 0f, 0.95f, 0.4f);
  }

  private void assertBounds(Fixture fixture, float left, float bottom, float right, float top) {
    PolygonShape shape = (PolygonShape) fixture.getShape();
    Vector2 vertex = new Vector2();
    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, vertex);
      minX = Math.min(minX, vertex.x);
      minY = Math.min(minY, vertex.y);
      maxX = Math.max(maxX, vertex.x);
      maxY = Math.max(maxY, vertex.y);
    }
    assertEquals(left, minX, 0.0001f);
    assertEquals(bottom, minY, 0.0001f);
    assertEquals(right, maxX, 0.0001f);
    assertEquals(top, maxY, 0.0001f);
  }

  private LungeAttackTask createLunge() {
    TaskRunner runner = mock(TaskRunner.class);
    when(runner.getEntity()).thenReturn(minotaur);
    LungeAttackTask lunge = new LungeAttackTask(player, 20, 2.5f, minotaur);
    lunge.create(runner);
    return lunge;
  }

  private void contact(String event) {
    minotaur
        .getEvents()
        .trigger(
            event,
            minotaur.getComponent(HitboxComponent.class).getFixture(),
            player.getComponent(HitboxComponent.class).getFixture());
  }

  private void tick(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    minotaur.getComponent(MinotaurAnimationController.class).update();
  }

  private int health() {
    return player.getComponent(CombatStatsComponent.class).getHealth();
  }

  private Body playerBody() {
    return player.getComponent(PhysicsComponent.class).getBody();
  }

  private MinotaurAttackRenderComponent attackRenderer() {
    return minotaur.getComponent(MinotaurAttackRenderComponent.class);
  }

  private AnimationRenderComponent locomotion() {
    return minotaur.getComponent(AnimationRenderComponent.class);
  }
}
