package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DragonCloudDashMovementComponentTest {
  private PhysicsService physicsService;
  private Entity dragon;
  private Entity target;
  private CombatStatsComponent targetStats;
  private CombatStatsComponent stats;
  private DragonCloudDashComponent dash;
  private DragonCloudDashMovementComponent movement;
  private Body body;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerTimeSource(new GameTime());
    physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);

    targetStats = new CombatStatsComponent(100, 10);
    PhysicsComponent targetPhysics = new PhysicsComponent();
    HitboxComponent targetHitbox = new HitboxComponent();
    targetHitbox.setLayer(PhysicsLayer.PLAYER);

    target =
        new Entity()
            .addComponent(targetStats)
            .addComponent(targetPhysics)
            .addComponent(targetHitbox);
    target.setScale(1f, 1f);
    target.setPosition(10f, 0f);

    targetPhysics.create();
    targetHitbox.create();

    stats = new CombatStatsComponent(500, 20);
    DragonPhaseComponent phase = new DragonPhaseComponent();
    PhysicsComponent physics = new PhysicsComponent();
    ColliderComponent collider = new ColliderComponent();
    PhysicsMovementComponent normalMovement = new PhysicsMovementComponent();

    dash = new DragonCloudDashComponent(target);
    movement = new DragonCloudDashMovementComponent();

    dragon =
        new Entity()
            .addComponent(stats)
            .addComponent(phase)
            .addComponent(physics)
            .addComponent(collider)
            .addComponent(normalMovement)
            .addComponent(dash)
            .addComponent(movement);
    dragon.setScale(1f, 1f);

    physics.create();
    collider.create();
    normalMovement.create();
    phase.create();
    dash.create();
    movement.create();

    body = physics.getBody();
    DragonCloudDashDamageComponent damage = new DragonCloudDashDamageComponent(target);
    dragon.addComponent(damage);
    damage.create();
  }

  @AfterEach
  void tearDown() {
    physicsService.getPhysics().dispose();
  }

  private void startDash() {
    assertTrue(dash.tryAttack());
    dash.update(0.8f);
  }

  private void addWall(float x) {
    BodyDef definition = new BodyDef();
    definition.type = BodyDef.BodyType.StaticBody;
    definition.position.set(x, 0.5f);
    Body wall = physicsService.getPhysics().createBody(definition);

    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.05f, 3f);

    FixtureDef fixture = new FixtureDef();
    fixture.shape = shape;
    fixture.filter.categoryBits = PhysicsLayer.OBSTACLE;
    wall.createFixture(fixture);

    shape.dispose();
  }

  @Test
  void shouldStayStillDuringWarning() {
    assertTrue(dash.tryAttack());
    body.setLinearVelocity(4f, 0f);

    movement.update(0.2f);

    assertEquals(new Vector2(), dragon.getPosition());
    assertEquals(new Vector2(), body.getLinearVelocity());
  }

  @Test
  void shouldMoveAlongLockedDirection() {
    startDash();
    target.setPosition(0f, 10f);

    movement.update(0.1f);

    assertEquals(0.6f, dragon.getPosition().x, 0.001f);
    assertEquals(0f, dragon.getPosition().y, 0.001f);
  }

  @Test
  void shouldLimitDistanceEvenWithLargeDelta() {
    startDash();

    movement.update(10f);

    assertEquals(3f, dragon.getPosition().x, 0.001f);
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());
  }

  @Test
  void shouldStopBeforeBodyCrossesWall() {
    addWall(2f);
    startDash();

    movement.update(1f);

    assertTrue(dragon.getPosition().x > 0f);
    assertTrue(dragon.getPosition().x + 1f <= 1.95f);
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());
  }

  @Test
  void shouldResetDistanceForNextAttack() {
    startDash();
    movement.update(1f);

    movement.update(0.1f);
    dash.update(1f);

    startDash();
    movement.update(1f);

    assertEquals(6f, dragon.getPosition().x, 0.001f);
  }

  @Test
  void shouldNotMoveAfterDeath() {
    startDash();
    stats.setHealth(0);

    movement.update(1f);

    assertEquals(new Vector2(), dragon.getPosition());
    assertEquals(DragonCloudDashComponent.State.STOPPED, dash.getState());
  }

  @Test
  void shouldIgnoreInvalidDelta() {
    startDash();

    movement.update(Float.NaN);
    movement.update(Float.POSITIVE_INFINITY);
    movement.update(-1f);

    assertEquals(new Vector2(), dragon.getPosition());
    assertEquals(DragonCloudDashComponent.State.DASHING, dash.getState());
  }

  @Test
  void shouldDamageTargetAlongPathOnlyOnce() {
    target.setPosition(2f, 0f);
    startDash();

    movement.update(1f);

    assertEquals(80, targetStats.getHealth());
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());
  }

  @Test
  void shouldNotDamageDuringWarning() {
    target.setPosition(0.5f, 0f);
    assertTrue(dash.tryAttack());

    movement.update(0.2f);

    assertEquals(100, targetStats.getHealth());
  }

  @Test
  void shouldNotDamageTargetBehindWall() {
    target.setPosition(2f, 0f);
    addWall(1.5f);
    startDash();

    movement.update(1f);

    assertEquals(100, targetStats.getHealth());
    assertEquals(DragonCloudDashComponent.State.RECOVERING, dash.getState());
  }

  @Test
  void shouldMissTargetThatDodgesLockedPath() {
    target.setPosition(2f, 0f);
    startDash();

    target.setPosition(2f, 3f);
    movement.update(1f);

    assertEquals(100, targetStats.getHealth());
  }

  @Test
  void shouldAllowOneNewHitOnNextDash() {
    target.setPosition(2f, 0f);
    startDash();
    movement.update(1f);
    assertEquals(80, targetStats.getHealth());

    movement.update(0.1f);
    dash.update(1f);
    target.setPosition(5f, 0f);

    startDash();
    movement.update(1f);

    assertEquals(60, targetStats.getHealth());
  }
}
