package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks actual Harpy sprite visibility and playback through its factory and AI events. */
@ExtendWith(GameExtension.class)
class HarpyAnimationPlaybackTest {
  private static final String HARPY_ATLAS = "images/flying-enemy/harpy.atlas";
  private GameTime time;
  private SpriteBatch batch;
  private ResourceService resources;
  private EntityService entities;
  private Entity harpy;
  private AnimationRenderComponent animator;
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
    resources.loadTextureAtlases(new String[] {HARPY_ATLAS});
    resources.loadAll();
    ServiceLocator.registerResourceService(resources);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 5));
    player.setPosition(2f, 0f);
    entities.register(player);
    harpy = NPCFactory.createHarpy(player, new Vector2());
    harpy
        .getEvents()
        .addListener(
            NPCFactory.PROJECTILE_SPAWNED,
            (Entity projectile) -> {
              projectiles.add(projectile);
              entities.register(projectile);
            });
    entities.register(harpy);
    animator = harpy.getComponent(AnimationRenderComponent.class);
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
  @ValueSource(strings = {"move", "chase", "attack", "dieAnimation", "projectile", "projectileHit"})
  void everyLoadedFrameKeepsTheEnemyOrItsProjectileVisible(String clip) throws Exception {
    TextureAtlas atlas = resources.getAsset(HARPY_ATLAS, TextureAtlas.class);
    assertTrue(atlas.findRegions(clip).notEmpty(), clip + " must retain its upstream artwork");
    for (TextureAtlas.AtlasRegion frame : atlas.findRegions(clip)) {
      BufferedImage sheet =
          ImageIO.read(
              ((FileTextureData) frame.getTexture().getTextureData()).getFileHandle().file());
      boolean visible = false;
      for (int y = frame.getRegionY(); y < frame.getRegionY() + frame.getRegionHeight(); y++) {
        for (int x = frame.getRegionX(); x < frame.getRegionX() + frame.getRegionWidth(); x++) {
          visible |= (sheet.getRGB(x, y) >>> 24) != 0;
        }
      }
      assertTrue(visible, clip + " frame " + frame.index + " must not disappear during playback");
    }
  }

  @Test
  void actualRangedAttackCompletesItsVisibleClipAndStillFiresThreeProjectilesPerCooldown() {
    AITaskComponent ai = harpy.getComponent(AITaskComponent.class);
    when(time.getDeltaTime()).thenReturn(0f);
    ai.update();
    assertEquals(3, projectiles.size());
    assertEquals("attack", animator.getCurrentAnimation());
    renderFor(0.19f);
    TextureRegion attackFrame = animator.getCurrentFrame();
    harpy.getEvents().trigger("moving", new Vector2(-1, 1));
    harpy.getEvents().trigger("patrolStart");
    harpy.getComponent(EnemyAnimationController.class).update();
    assertEquals("attack", animator.getCurrentAnimation());
    assertSame(
        attackFrame, animator.getCurrentFrame(), "Patrol must not restart or interrupt attack");
    renderFor(0.02f);
    harpy.getComponent(EnemyAnimationController.class).update();
    assertEquals("move", animator.getCurrentAnimation());
    TextureRegion resumedFlight = animator.getCurrentFrame();
    renderFor(0.07f);
    assertNotSame(resumedFlight, animator.getCurrentFrame(), "Flight must continue after shooting");

    when(time.getDeltaTime()).thenReturn(1.79f);
    ai.update();
    assertEquals(3, projectiles.size());
    when(time.getDeltaTime()).thenReturn(0.02f);
    ai.update();
    assertEquals(6, projectiles.size());
    assertEquals("attack", animator.getCurrentAnimation());
  }

  @ParameterizedTest
  @CsvSource({"-1,-1", "-1,0", "-1,1", "1,-1", "1,0", "1,1", "0,1", "0,-1"})
  void patrolTurnsFaceTravelWithoutRestartingFlight(float x, float y) {
    animator.setFlipX(true);
    renderFor(0.35f);
    TextureRegion frame = animator.getCurrentFrame();
    harpy.getEvents().trigger("moving", new Vector2(x, y));
    assertSame(frame, animator.getCurrentFrame());
    assertEquals(
        x <= 0, animator.isFlipX(), "Vertical movement retains the last horizontal facing");
    renderFor(0.11f);
    assertNotSame(frame, animator.getCurrentFrame(), "The flight loop continues advancing");
  }

  @ParameterizedTest
  @ValueSource(strings = {"move", "chase"})
  void flightCompletesItsFullWingCycleAtTheFasterTempo(String clip) {
    animator.startAnimation(clip);
    renderFor(1.4f);
    assertEquals(23, ((TextureAtlas.AtlasRegion) animator.getCurrentFrame()).index);
    renderFor(0.05f);
    assertEquals(0, ((TextureAtlas.AtlasRegion) animator.getCurrentFrame()).index);
  }

  @Test
  void deathFinishesWithinOneSecondWithoutBeingCutOffOrOverridden() {
    harpy.getEvents().trigger("entityDied");
    renderFor(0.79f);
    assertEquals(7, ((TextureAtlas.AtlasRegion) animator.getCurrentFrame()).index);
    TextureRegion lastFrame = animator.getCurrentFrame();
    harpy.getEvents().trigger("patrolStart");
    harpy.getEvents().trigger("moving", new Vector2(-1, 1));
    harpy.getEvents().trigger("rangedAttack");
    assertSame(lastFrame, animator.getCurrentFrame());
    harpy.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertTrue(entities.getEntities().contains(harpy, true));
    renderFor(0.02f);
    harpy.getComponent(EnemyAnimationController.class).update();
    entities.update();
    assertFalse(entities.getEntities().contains(harpy, true));
  }

  private void renderFor(float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    animator.render(batch);
  }
}
