package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercises the shared controller with the actual Dungeon Two, Three and Four atlases. */
@ExtendWith(GameExtension.class)
class EnemyAnimationPlaybackTest {
  private GameTime time;
  private SpriteBatch batch;
  private ResourceService resources;
  private EntityService entities;
  private Entity player;
  private final List<Entity> projectiles = new ArrayList<>();

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    OrthographicCamera camera = new OrthographicCamera(20f, 16f);
    camera.update();
    ServiceLocator.registerWorldCamera(camera);
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    resources = new ResourceService();
    resources.loadTextureAtlases(
        new String[] {
          "images/wasp.atlas",
          "images/crow.atlas",
          "images/jingwei.atlas",
          "images/horse.atlas",
          "images/knight.atlas",
          "images/zombie.atlas",
          "images/beetle.atlas",
          "images/floatingDemon.atlas",
          "images/longwei.atlas",
          "images/wukong.atlas"
        });
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 5))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    player.setPosition(2f, 0f);
    entities.register(player);
    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
  }

  @AfterEach
  void tearDown() {
    entities.dispose();
    resources.dispose();
    ServiceLocator.getPhysicsService().getPhysics().dispose();
  }

  @ParameterizedTest
  @ValueSource(strings = {"wasp", "crow", "jingwei"})
  void flyingPatrolFallsBackToMoveWithoutRestartingItsWingCycle(String skin) {
    Entity enemy = flyingEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    animation.startAnimation("default");
    enemy.getEvents().trigger("moving", new Vector2(1, 0));
    assertEquals("move", animation.getCurrentAnimation());
    renderFor(animation, 0.11f);
    TextureRegion frame = animation.getCurrentFrame();

    for (Vector2 direction : directions()) {
      enemy.getEvents().trigger("moving", direction);
      assertSame(frame, animation.getCurrentFrame(), "Patrol turns must preserve flight progress");
    }
    renderFor(animation, 0.1f);
    assertNotSame(frame, animation.getCurrentFrame(), "The flight loop must keep advancing");
  }

  @ParameterizedTest
  @ValueSource(strings = {"wasp", "crow", "jingwei"})
  void rangedAttackKeepsFlightAndStillFiresThreeProjectilesEveryCooldown(String skin) {
    Entity enemy = flyingEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    renderFor(animation, 0.11f);
    TextureRegion frame = animation.getCurrentFrame();
    AITaskComponent ai = enemy.getComponent(AITaskComponent.class);

    when(time.getDeltaTime()).thenReturn(0f);
    ai.update();
    assertEquals(3, projectiles.size());
    assertSame(frame, animation.getCurrentFrame());
    when(time.getDeltaTime()).thenReturn(1.79f);
    ai.update();
    assertEquals(3, projectiles.size());
    when(time.getDeltaTime()).thenReturn(0.02f);
    ai.update();
    assertEquals(6, projectiles.size());
    assertSame(frame, animation.getCurrentFrame());
  }

  @ParameterizedTest
  @CsvSource({
    "0,1,move_up", "0,-1,move_down", "1,0,move_right", "-1,0,move_left",
    "1,1,move_NE", "-1,1,move_NW", "1,-1,move_SE", "-1,-1,move_SW"
  })
  void sleipnirRepeatingDirectionEventsDoNotPinTheAnimationToItsFirstFrame(
      float x, float y, String clip) {
    Entity horse = sleipnir();
    AnimationRenderComponent animation = horse.getComponent(AnimationRenderComponent.class);
    Vector2 direction = new Vector2(x, y);
    horse.getEvents().trigger("moving", direction);
    assertEquals(clip, animation.getCurrentAnimation());
    TextureRegion firstFrame = animation.getCurrentFrame();
    renderFor(animation, 0.11f);
    TextureRegion secondFrame = animation.getCurrentFrame();
    assertNotSame(firstFrame, secondFrame);

    horse.getEvents().trigger("moving", direction);
    assertSame(secondFrame, animation.getCurrentFrame());
    renderFor(animation, 0.1f);
    assertSame(firstFrame, animation.getCurrentFrame(), "The existing two-frame loop must wrap");

    horse.getEvents().trigger("moving", direction.cpy().scl(-1));
    assertNotEquals(clip, animation.getCurrentAnimation(), "A new direction still selects new art");
    assertEquals(0, ((TextureAtlas.AtlasRegion) animation.getCurrentFrame()).index);
  }

  @ParameterizedTest
  @ValueSource(strings = {"knight", "zombie"})
  void missingFuseRetainsTheOriginalCurrentAnimation(String skin) {
    Entity enemy = bombEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("chaseStart");
    renderFor(animation, 0.11f);
    TextureRegion frame = animation.getCurrentFrame();

    enemy.getEvents().trigger("fuseStarted");
    assertEquals("chase", animation.getCurrentAnimation());
    assertSame(frame, animation.getCurrentFrame());
  }

  @Test
  void stampedeStillSelectsTheExistingDefaultAnimation() {
    Entity horse = sleipnir();
    AnimationRenderComponent animation = horse.getComponent(AnimationRenderComponent.class);
    horse.getEvents().trigger("moving", new Vector2(1, 0));
    assertEquals("move_right", animation.getCurrentAnimation());

    horse.getEvents().trigger("stampede");

    assertEquals("default", animation.getCurrentAnimation());
  }

  @Test
  void beetleStillPlaysItsExistingFuseAnimation() {
    Entity beetle = bombEnemy("beetle");
    AnimationRenderComponent animation = beetle.getComponent(AnimationRenderComponent.class);
    beetle.getEvents().trigger("fuseStarted");
    assertEquals("fuse", animation.getCurrentAnimation());
    TextureRegion frame = animation.getCurrentFrame();
    renderFor(animation, 0.11f);
    assertNotSame(frame, animation.getCurrentFrame());
  }

  @Test
  void availableAttackClipCompletesBeforeFlightFallbackResumes() {
    // Exercise a one-shot attack using the real horse strip without changing a production factory.
    AnimationRenderComponent animation =
        new AnimationRenderComponent(resources.getAsset("images/horse.atlas", TextureAtlas.class));
    animation.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    animation.addAnimation("attack", 0.1f, Animation.PlayMode.NORMAL);
    Entity enemy =
        NPCFactory.createBaseNPC()
            .addComponent(new CombatStatsComponent(100, 5))
            .addComponent(animation)
            .addComponent(new EnemyAnimationController());
    entities.register(enemy);
    animation.startAnimation("move");
    enemy.getEvents().trigger("rangedAttack");
    renderFor(animation, 0.15f);
    TextureRegion frame = animation.getCurrentFrame();
    enemy.getEvents().trigger("moving", new Vector2(-1, 1));
    assertEquals("attack", animation.getCurrentAnimation());
    assertSame(frame, animation.getCurrentFrame());

    renderFor(animation, 0.36f);
    enemy.getComponent(EnemyAnimationController.class).update();
    assertEquals("move", animation.getCurrentAnimation());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "wanderStart",
        "chaseStart",
        "patrolStart",
        "moving",
        "rangedAttack",
        "fuseStarted",
        "stampede",
        "default",
        "dieAnimation",
        "enemyDeathAnimation",
        "entityDied"
      })
  void lateEventsCannotInterruptOrRestartDeath(String event) {
    // Together these skins provide real directional, attack and fuse clips.
    assertDeathProtected(sleipnir(), 0.5f, event);
    assertDeathProtected(bombEnemy("beetle"), 1.5f, event);
    entities.update();
    assertEquals(1, entities.getEntities().size, "Only the player remains after death completes");
  }

  @ParameterizedTest
  @CsvSource({"wasp,1", "crow,0.5", "jingwei,3"})
  void flyingDeathRetainsItsExistingDurationAndDisposesAfterTheLastFrame(
      String skin, float duration) {
    Entity enemy = flyingEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("entityDied");
    renderFor(animation, duration - 0.01f);
    enemy.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertTrue(entities.getEntities().contains(enemy, true));

    renderFor(animation, 0.02f);
    enemy.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertFalse(entities.getEntities().contains(enemy, true));
  }

  @ParameterizedTest
  @CsvSource({"longwei,0.12,0.24", "wukong,0.08,0.48"})
  void contactHitPlaysExistingAttackThenResumesLatestLocomotion(
      String skin, float frameTime, float duration) {
    Entity enemy = contactEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    EnemyAnimationController controller = enemy.getComponent(EnemyAnimationController.class);
    enemy.getComponent(PhysicsMovementComponent.class).setMoving(true);
    enemy.getEvents().trigger("wanderStart");
    int damage = enemy.getComponent(CombatStatsComponent.class).getBaseAttack();
    enemy
        .getEvents()
        .trigger(
            "collisionStart",
            enemy.getComponent(HitboxComponent.class).getFixture(),
            player.getComponent(HitboxComponent.class).getFixture());
    assertEquals(100 - damage, player.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals("contactAttack", animation.getCurrentAnimation());
    TextureRegion first = animation.getCurrentFrame();
    renderFor(animation, frameTime + 0.01f);
    TextureRegion second = animation.getCurrentFrame();
    assertNotSame(first, second, "The existing attack poses must actually advance");
    enemy.getEvents().trigger("chaseStart");
    enemy.getEvents().trigger("contactAttack", player);
    controller.update();
    assertEquals("contactAttack", animation.getCurrentAnimation());
    assertSame(
        second,
        animation.getCurrentFrame(),
        "AI events and another contact cannot restart the clip");
    renderFor(animation, duration - frameTime);
    controller.update();
    assertEquals("chase", animation.getCurrentAnimation());
    assertEquals(
        100 - damage,
        player.getComponent(CombatStatsComponent.class).getHealth(),
        "Animation completion must not inflict another hit");
  }

  @ParameterizedTest
  @ValueSource(strings = {"longwei", "wukong"})
  void contactAnimationCannotInterruptDeathOrExtendItsOriginalDuration(String skin) {
    Entity enemy = contactEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("entityDied");
    renderFor(animation, 0.49f);
    TextureRegion frame = animation.getCurrentFrame();
    enemy.getEvents().trigger("contactAttack", player);
    assertEquals("dieAnimation", animation.getCurrentAnimation());
    assertSame(frame, animation.getCurrentFrame());
    renderFor(animation, 0.02f);
    enemy.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertFalse(entities.getEntities().contains(enemy, true));
  }

  @Test
  void zombieUsesExistingWalkingPosesWithoutChangingBodySizeOrDeathTime() {
    Entity zombie = bombEnemy("zombie");
    assertEquals(new Vector2(1f, 1f), zombie.getScale());
    AnimationRenderComponent animation = zombie.getComponent(AnimationRenderComponent.class);
    for (String event : new String[] {"wanderStart", "chaseStart"}) {
      zombie.getEvents().trigger(event);
      TextureRegion first = animation.getCurrentFrame();
      renderFor(animation, event.equals("wanderStart") ? 0.71f : 0.11f);
      assertNotSame(first, animation.getCurrentFrame(), "Walking must change the visible pose");
    }
    zombie.getEvents().trigger("entityDied");
    assertEquals(2, animation.getCurrentFrame().getRegionX());
    renderFor(animation, 0.49f);
    assertFalse(animation.isFinished());
    renderFor(animation, 0.02f);
    zombie.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertFalse(entities.getEntities().contains(zombie, true));
  }

  private Entity contactEnemy(String skin) {
    Entity enemy =
        skin.equals("wukong")
            ? NPCFactory.createGiantEnemy(player, "images/wukong.atlas")
            : NPCFactory.createChaseEnemy(player, false, "images/longwei.atlas");
    entities.register(enemy);
    return enemy;
  }

  private Entity flyingEnemy(String skin) {
    Vector2 spawn = new Vector2();
    Entity enemy =
        switch (skin) {
          case "wasp" -> NPCFactory.createWasp(player, spawn);
          case "crow" -> NPCFactory.createCrow(player, spawn);
          case "jingwei" -> NPCFactory.createJingwei(player, spawn);
          default -> throw new IllegalArgumentException("Unsupported flying skin: " + skin);
        };
    enemy
        .getEvents()
        .addListener(
            NPCFactory.PROJECTILE_SPAWNED,
            (Entity projectile) -> {
              projectiles.add(projectile);
              entities.register(projectile);
            });
    entities.register(enemy);
    return enemy;
  }

  private Entity bombEnemy(String skin) {
    Entity enemy = NPCFactory.createBombEnemy(player, "images/" + skin + ".atlas", 2f);
    entities.register(enemy);
    return enemy;
  }

  private Entity sleipnir() {
    Entity enemy = NPCFactory.createSleipnir(player, new Vector2(-5, -5));
    entities.register(enemy);
    return enemy;
  }

  private static Vector2[] directions() {
    return new Vector2[] {
      new Vector2(0, 1), new Vector2(0, -1), new Vector2(1, 0), new Vector2(-1, 0),
      new Vector2(1, 1), new Vector2(-1, 1), new Vector2(1, -1), new Vector2(-1, -1)
    };
  }

  private void renderFor(AnimationRenderComponent animation, float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    animation.render(batch);
  }

  private void assertDeathProtected(Entity enemy, float duration, String event) {
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("entityDied");
    renderFor(animation, duration - 0.01f);
    TextureRegion frame = animation.getCurrentFrame();
    if ("moving".equals(event)) {
      enemy.getEvents().trigger(event, new Vector2(-1, 1));
    } else {
      enemy.getEvents().trigger(event);
    }
    assertEquals("dieAnimation", animation.getCurrentAnimation(), event);
    assertSame(frame, animation.getCurrentFrame(), event);
    renderFor(animation, 0.02f);
    assertTrue(animation.isFinished(), "Repeated death must not reset its playback clock");
    enemy.getComponent(EnemyAnimationController.class).update();
    enemy.getEvents().trigger("wanderStart");
    assertEquals(
        "dieAnimation", animation.getCurrentAnimation(), "Disposal must keep death locked");
  }
}
