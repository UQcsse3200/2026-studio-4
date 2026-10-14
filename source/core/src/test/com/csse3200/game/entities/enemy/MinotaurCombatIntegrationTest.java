package com.csse3200.game.entities.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.ai.tasks.TaskRunner;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.EnemyAnimationController;
import com.csse3200.game.components.npc.MinotaurAnimationController;
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

/** Exercises visual attack wiring through the existing contact and lunge combat paths. */
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
    ServiceLocator.registerEntityService(mock(EntityService.class));
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
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(1f, 0f);
    player.create();
    minotaur = NPCFactory.createChaseEnemy(player, false, "images/minotaur.atlas");
    // EnemyManagerComponent uses this square world size for the minotaur room spawn.
    minotaur.setScale(1.5f, 1.5f);
    minotaur.create();
  }

  @Test
  void contactAnimationPreservesImmediateDamageRepeatIntervalAndExit() {
    contact("collisionStart");
    assertEquals(18, health());
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    tickContact(0.5f);
    assertEquals(18, health());
    tickContact(0.5f);
    assertEquals(16, health());
    contact("collisionEnd");
    tickContact(2f);
    assertEquals(16, health());
    contact("collisionStart");
    assertEquals(14, health());
  }

  @Test
  void contactNotificationObservesOriginalDamageAndKnockbackAlreadyApplied() {
    int[] notifications = {0};
    minotaur
        .getEvents()
        .addListener(
            "touchAttack",
            (Entity victim) -> {
              notifications[0]++;
              assertSame(player, victim);
              assertEquals(18, health());
              assertTrue(
                  player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x > 0f);
            });

    contact("collisionStart");

    assertEquals(1, notifications[0]);
    assertEquals(18, health());
  }

  @Test
  void realDashContactSelectsChargeWithoutAddingDamageAtPhaseTransitions() {
    contact("collisionStart");
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    contact("collisionEnd");
    LungeAttackTask lunge = createLunge();
    lunge.start();
    assertEquals(18, health());
    when(time.getTime()).thenReturn(500L);
    lunge.update();
    assertEquals(18, health());

    contact("collisionStart");
    assertEquals("chargeAttack", attackRenderer().getCurrentAnimation());
    assertEquals(16, health());
    tickContact(0.5f);
    assertEquals(16, health());
    when(time.getTime()).thenReturn(900L);
    lunge.update();
    assertEquals(16, health());
    tickContact(0.5f);
    assertEquals("normalAttack", attackRenderer().getCurrentAnimation());
    assertEquals(14, health());
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
    contact("collisionStart");
    assertEquals("chargeAttack", attackRenderer().getCurrentAnimation());
    assertPhysicsShape(hitbox, collider);
  }

  @Test
  void otherChaseSkinsRetainSharedAnimationControllerWithoutMinotaurAttackLayer() {
    Entity crab = NPCFactory.createChaseEnemy(player, false, "images/crab.atlas");
    crab.create();
    assertNull(minotaur.getComponent(EnemyAnimationController.class));
    assertTrue(
        minotaur.getComponent(MinotaurAnimationController.class)
            instanceof EnemyAnimationController);
    assertNotNull(crab.getComponent(EnemyAnimationController.class));
    assertNull(crab.getComponent(MinotaurAnimationController.class));
    assertNull(crab.getComponent(MinotaurAttackRenderComponent.class));
    AnimationRenderComponent crabAnimations = crab.getComponent(AnimationRenderComponent.class);
    crab.getEvents().trigger("chaseStart");
    assertEquals("chase", crabAnimations.getCurrentAnimation());
    assertFalse(crabAnimations.hasAnimation("normalAttack"));
    assertFalse(crabAnimations.hasAnimation("chargeAttack"));
  }

  private void assertPhysicsShape(Fixture hitbox, Fixture collider) {
    assertEquals(new Vector2(1.5f, 1.5f), minotaur.getScale());
    assertSame(hitbox, minotaur.getComponent(HitboxComponent.class).getFixture());
    assertSame(collider, minotaur.getComponent(ColliderComponent.class).getFixture());
    assertTrue(hitbox.isSensor());
    assertEquals(PhysicsLayer.NPC, hitbox.getFilterData().categoryBits);
    assertBounds(hitbox, 0f, 0f, 1.5f, 1.5f);
    // Base NPC's 0.9 x 0.4 foot collider is created before the room changes visual scale.
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

  private void tickContact(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    minotaur.getComponent(TouchAttackComponent.class).update();
  }

  private int health() {
    return player.getComponent(CombatStatsComponent.class).getHealth();
  }

  private MinotaurAttackRenderComponent attackRenderer() {
    return minotaur.getComponent(MinotaurAttackRenderComponent.class);
  }

  private AnimationRenderComponent locomotion() {
    return minotaur.getComponent(AnimationRenderComponent.class);
  }
}
