package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private Handler animationLogs;
  private final Logger animationLogger = Logger.getLogger(AnimationRenderComponent.class.getName());
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
          "images/floatingDemon.atlas"
        });
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    player = new Entity().addComponent(new CombatStatsComponent(100, 5));
    player.setPosition(2f, 0f);
    entities.register(player);
    batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));
    animationLogs = mock(Handler.class);
    animationLogger.addHandler(animationLogs);
  }

  @AfterEach
  void tearDown() {
    animationLogger.removeHandler(animationLogs);
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
    assertNoMissingAnimationRequests();
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
    assertNoMissingAnimationRequests();
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
    renderFor(animation, 1.1f);
    TextureRegion secondFrame = animation.getCurrentFrame();
    assertNotSame(firstFrame, secondFrame);

    horse.getEvents().trigger("moving", direction);
    assertSame(secondFrame, animation.getCurrentFrame());
    renderFor(animation, 1f);
    assertSame(firstFrame, animation.getCurrentFrame(), "The existing two-frame loop must wrap");

    horse.getEvents().trigger("moving", direction.cpy().scl(-1));
    assertNotEquals(clip, animation.getCurrentAnimation(), "A new direction still selects new art");
    assertEquals(0, ((TextureAtlas.AtlasRegion) animation.getCurrentFrame()).index);
    assertNoMissingAnimationRequests();
  }

  @ParameterizedTest
  @ValueSource(strings = {"knight", "zombie"})
  void missingFuseKeepsTheCurrentAnimationWithoutRequestingAnAbsentClip(String skin) {
    Entity enemy = bombEnemy(skin);
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("chaseStart");
    renderFor(animation, 0.11f);
    TextureRegion frame = animation.getCurrentFrame();

    enemy.getEvents().trigger("fuseStarted");
    assertEquals("chase", animation.getCurrentAnimation());
    assertSame(frame, animation.getCurrentFrame());
    assertNoMissingAnimationRequests();
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
    assertNoMissingAnimationRequests();
  }

  @Test
  void availableAttackClipCompletesBeforeFlightFallbackResumes() {
    // Horse supplies a real attack strip to exercise this shared factory's optional-clip path.
    Entity enemy = flyingEnemy("horse");
    AnimationRenderComponent animation = enemy.getComponent(AnimationRenderComponent.class);
    enemy.getEvents().trigger("rangedAttack");
    renderFor(animation, 0.15f);
    TextureRegion frame = animation.getCurrentFrame();
    enemy.getEvents().trigger("moving", new Vector2(-1, 1));
    assertEquals("attack", animation.getCurrentAnimation());
    assertSame(frame, animation.getCurrentFrame());

    renderFor(animation, 0.36f);
    enemy.getComponent(EnemyAnimationController.class).update();
    assertEquals("move", animation.getCurrentAnimation());
    assertNoMissingAnimationRequests();
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

  private Entity flyingEnemy(String skin) {
    Entity enemy =
        NPCFactory.createFloatingDemon(
            player,
            new Vector2(-2, 0),
            new Vector2(0, 2),
            new Vector2(2, 0),
            projectile -> {
              projectiles.add(projectile);
              entities.register(projectile);
            },
            "images/" + skin + ".atlas");
    entities.register(enemy);
    return enemy;
  }

  private Entity bombEnemy(String skin) {
    Entity enemy = NPCFactory.createBombEnemy(player, "images/" + skin + ".atlas", 2f);
    entities.register(enemy);
    return enemy;
  }

  private Entity sleipnir() {
    Entity enemy =
        NPCFactory.createSleipnir(player, new Vector2[] {new Vector2(-5, -5), new Vector2(5, 5)});
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

  private void assertNoMissingAnimationRequests() {
    verify(animationLogs, never())
        .publish(argThat(record -> record.getLevel().intValue() >= Level.WARNING.intValue()));
  }
}
