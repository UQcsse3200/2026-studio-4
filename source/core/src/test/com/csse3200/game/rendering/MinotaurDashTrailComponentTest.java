package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FlashEffect;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MinotaurDashTrailComponentTest {
  private GameTime time;
  private Entity entity;
  private AnimationRenderComponent body;
  private MinotaurAttackRenderComponent attack;
  private MinotaurDashTrailComponent trail;
  private StatusEffectsControllerComponent effects;
  private Pixmap pixels;
  private Texture source;
  private TextureAtlas atlas;
  private MinotaurDashTrailComponent secondTrail;
  private PhysicsComponent ownedPhysics;
  private float elapsed;

  @BeforeEach
  void setUp() {
    // GameExtension shares its GL mock across this class; texture handles repeat per fixture.
    clearInvocations(Gdx.gl);
    AtomicInteger textureIds = new AtomicInteger(100);
    when(Gdx.gl.glGenTexture()).thenAnswer(invocation -> textureIds.getAndIncrement());
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerEntityService(new EntityService());
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    pixels = new Pixmap(8, 4, Pixmap.Format.RGBA8888);
    pixels.drawPixel(0, 0, 0x325faaff);
    pixels.drawPixel(4, 1, 0xffca35ff);
    source = new Texture(pixels);
    atlas = new TextureAtlas();
    atlas.addRegion("move", source, 0, 0, 4, 4).index = 0;
    atlas.addRegion("move", source, 4, 0, 4, 4).index = 1;
    for (int i = 0; i < 7; i++) atlas.addRegion("normalAttack", source, 0, 0, 4, 4).index = i;
    for (int i = 0; i < 9; i++) atlas.addRegion("chargeAttack", source, 0, 0, 4, 4).index = i;
    body = spy(new AnimationRenderComponent(atlas));
    body.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    body.startAnimation("move");
    attack = new MinotaurAttackRenderComponent(atlas);
    effects = new StatusEffectsControllerComponent();
    trail = new MinotaurDashTrailComponent();
    entity =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 2))
            .addComponent(effects)
            .addComponent(body)
            .addComponent(attack)
            .addComponent(trail);
    entity.setScale(1.5f, 1.5f);
    ServiceLocator.getEntityService().register(entity);
  }

  @AfterEach
  void disposeTextures() {
    trail.dispose();
    if (secondTrail != null) secondTrail.dispose();
    if (ownedPhysics != null) ownedPhysics.dispose();
    source.dispose();
    pixels.dispose();
  }

  @Test
  void movingOutsideDashNeverDrawsTrail() {
    move(0.3f, 0f, 0.06f);
    move(0.6f, 0f, 0.06f);
    RecordingBatch drawn = renderTrail();
    assertTrue(drawn.ghosts.isEmpty());
    assertTrue(drawn.quads.isEmpty());
  }

  @Test
  void silhouettesAndSpeedGeometryFollowActualRightLeftAndDiagonalMovement() {
    for (Vector2 step :
        new Vector2[] {new Vector2(0.3f, 0f), new Vector2(-0.3f, 0f), new Vector2(0.2f, 0.2f)}) {
      entity.setPosition(0f, 0f);
      entity.getEvents().trigger("lungeDashStart");
      sampleMotion(step, 6);
      RecordingBatch drawn = renderTrail();
      assertFalse(drawn.ghosts.isEmpty());
      assertFalse(drawn.quads.isEmpty());
      Vector2 direction = step.cpy().nor();
      Vector2 current = entity.getPosition();
      assertTrue(
          drawn.ghosts.stream()
              .anyMatch(
                  ghost -> new Vector2(ghost.x, ghost.y).sub(current).dot(direction) < -0.01f));
      for (Ghost ghost : drawn.ghosts) {
        assertTrue(new Vector2(ghost.x, ghost.y).sub(current).dot(direction) <= 0.0001f);
        assertTrue(ghost.color.r > ghost.color.g + ghost.color.b);
        assertTrue(ghost.color.a > 0f && ghost.color.a <= 0.54f);
      }
      Vector2 centre = entity.getCenterPosition();
      for (float[] quad : drawn.quads) {
        Vector2 average = new Vector2();
        for (int i = 0; i < 20; i += 5) average.add(quad[i], quad[i + 1]);
        average.scl(0.25f);
        assertTrue(average.sub(centre).dot(direction) < 0f);
      }
      entity.getEvents().trigger("lungeDashEnd");
    }
  }

  @Test
  void ghostHistoryIsBoundedAndOlderSnapshotsFadeWhileDashContinues() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 12);
    RecordingBatch drawn = renderTrail();
    assertTrue(drawn.ghosts.size() > 0 && drawn.ghosts.size() <= 4);
    float minAlpha =
        drawn.ghosts.stream().map(ghost -> ghost.color.a).min(Float::compare).orElseThrow();
    float maxAlpha =
        drawn.ghosts.stream().map(ghost -> ghost.color.a).max(Float::compare).orElseThrow();
    assertTrue(minAlpha < maxAlpha);
    entity.getEvents().trigger("lungeDashEnd");
    assertTrue(renderTrail().ghosts.isEmpty());
  }

  @Test
  void historicalFramesFacingAndScaleStayLocalWithoutRecoloringBodyTexture() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    when(time.getDeltaTime()).thenReturn(0.1f);
    body.draw(mock(SpriteBatch.class));
    body.setFlipX(true);
    entity.setScale(2f, 1f);
    sampleMotion(new Vector2(0.3f, 0f), 2);
    RecordingBatch drawn = renderTrail();
    assertTrue(
        drawn.ghosts.stream()
            .anyMatch(ghost -> ghost.region.getU() == 0f && !ghost.region.isFlipX()));
    assertTrue(drawn.ghosts.stream().anyMatch(ghost -> ghost.region.isFlipX()));
    assertTrue(
        drawn.ghosts.stream().anyMatch(ghost -> ghost.width == 1.5f && ghost.height == 1.5f));
    assertTrue(drawn.ghosts.stream().anyMatch(ghost -> ghost.width == 2f && ghost.height == 1f));
    assertFalse(body.getCurrentFrame().isFlipX());
    assertEquals(source, body.getCurrentFrame().getTexture());
    assertEquals(0x325faaff, pixels.getPixel(0, 0));
    assertEquals(0xffca35ff, pixels.getPixel(4, 1));
  }

  @Test
  void stationaryFreezeTeleportAndActiveSwingClearTrail() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    assertFalse(renderTrail().ghosts.isEmpty());
    move(entity.getPosition().x, entity.getPosition().y, 0.06f);
    assertTrue(renderTrail().ghosts.isEmpty());
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    effects.addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000L));
    move(entity.getPosition().x + 0.3f, 0f, 0.06f);
    assertTrue(renderTrail().ghosts.isEmpty());
    effects.clearStatusEffects();
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    move(30f, 30f, 0.06f);
    assertTrue(renderTrail().ghosts.isEmpty());
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    attack.startAnimation("chargeAttack", false);
    move(entity.getPosition().x + 0.3f, entity.getPosition().y, 0.06f);
    assertTrue(renderTrail().ghosts.isEmpty());
    assertTrue(renderTrail().quads.isEmpty());
  }

  @Test
  void missingCurrentFrameClearsTrailAndResumesFromNewPosition() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    assertFalse(renderTrail().ghosts.isEmpty());

    // A frame is a nullable renderer result; the effect must not retain stale silhouettes.
    doReturn(null).when(body).getCurrentFrame();
    move(1.5f, 0f, 0.06f);
    assertTrue(renderTrail().ghosts.isEmpty());
    assertTrue(renderTrail().quads.isEmpty());

    doCallRealMethod().when(body).getCurrentFrame();
    move(1.8f, 0f, 0.06f);
    RecordingBatch resumed = renderTrail();
    assertEquals(1, resumed.ghosts.size());
    assertEquals(1.5f, resumed.ghosts.get(0).x, 0.0001f);
  }

  @Test
  void phaseChangesAndDeathRemoveHistoricalTrailImmediately() {
    for (String event :
        new String[] {
          "lungeTelegraphStart", "wanderStart", "chaseStart", "default", "enemyDeathAnimation"
        }) {
      entity.getEvents().trigger("lungeDashStart");
      sampleMotion(new Vector2(0.3f, 0f), 4);
      assertFalse(renderTrail().ghosts.isEmpty());
      entity.getEvents().trigger(event);
      assertTrue(renderTrail().ghosts.isEmpty());
      assertTrue(renderTrail().quads.isEmpty());
    }
  }

  @Test
  void drawingPreservesBatchStateAndDoesNotAdvanceOrDuplicateSnapshotsForGlow() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 6);
    RecordingBatch first = renderTrail();
    assertFalse(first.ghosts.isEmpty());
    effects.addStatusEffect(new FlashEffect(time, 5000L, null, new Color(1f, 0.2f, 0.1f, 0.5f)));
    for (int pass = 0; pass < 3; pass++) {
      RecordingBatch repeated = renderTrail();
      assertEquals(first.ghosts.size(), repeated.ghosts.size());
      assertEquals(first.quads.size(), repeated.quads.size());
      for (int i = 0; i < first.ghosts.size(); i++) {
        assertEquals(first.ghosts.get(i).x, repeated.ghosts.get(i).x);
        assertEquals(first.ghosts.get(i).color.a, repeated.ghosts.get(i).color.a);
      }
      assertEquals(repeated.originalColor, repeated.color);
      assertEquals(GL20.GL_SRC_ALPHA, repeated.blendSrc);
      assertEquals(GL20.GL_ONE_MINUS_SRC_ALPHA, repeated.blendDst);
      assertEquals(repeated.originalShader, repeated.shader);
    }
  }

  @Test
  void disposalReleasesOnlyOwnedTrailTexturesAndStopsDrawing() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    RecordingBatch drawn = renderTrail();
    assertFalse(drawn.ghosts.isEmpty());
    Texture whiteCopy = drawn.ghosts.get(0).region.getTexture();
    assertNotEquals(source, whiteCopy);
    int whiteHandle = whiteCopy.getTextureObjectHandle();
    int sourceHandle = source.getTextureObjectHandle();
    trail.dispose();
    verify(Gdx.gl).glDeleteTexture(whiteHandle);
    verify(Gdx.gl, never()).glDeleteTexture(sourceHandle);
    assertTrue(renderTrail().ghosts.isEmpty());
    assertTrue(renderTrail().quads.isEmpty());
  }

  @Test
  void sharedSilhouetteLivesUntilLastDashEntityReleasesItsReference() {
    AnimationRenderComponent secondBody = new AnimationRenderComponent(atlas);
    secondBody.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    secondBody.startAnimation("move");
    secondTrail = new MinotaurDashTrailComponent();
    Entity second =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 2))
            .addComponent(secondBody)
            .addComponent(secondTrail);
    second.setScale(1.5f, 1.5f);
    ServiceLocator.getEntityService().register(second);
    entity.getEvents().trigger("lungeDashStart");
    second.getEvents().trigger("lungeDashStart");
    for (int i = 1; i <= 4; i++) {
      second.setPosition(0.3f * i, 2f);
      move(0.3f * i, 0f, 0.06f);
    }
    RecordingBatch firstDrawing = renderTrail();
    RecordingBatch secondDrawing = renderTrail(secondTrail);
    assertFalse(firstDrawing.ghosts.isEmpty());
    assertFalse(secondDrawing.ghosts.isEmpty());
    Texture sharedCopy = firstDrawing.ghosts.get(0).region.getTexture();
    assertSame(sharedCopy, secondDrawing.ghosts.get(0).region.getTexture());
    int sharedHandle = sharedCopy.getTextureObjectHandle();
    int sourceHandle = source.getTextureObjectHandle();

    trail.dispose();
    verify(Gdx.gl, never()).glDeleteTexture(sharedHandle);
    assertFalse(renderTrail(secondTrail).ghosts.isEmpty());
    secondTrail.dispose();
    verify(Gdx.gl).glDeleteTexture(sharedHandle);
    verify(Gdx.gl, never()).glDeleteTexture(sourceHandle);
  }

  @Test
  void fixedPhysicsIntermediateUpdateAgesTrailWithoutClearingOrInventingSnapshots() {
    PhysicsComponent physics = movingPhysicsScene();
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    RecordingBatch before = renderTrail();
    assertFalse(before.ghosts.isEmpty());

    Vector2 unchanged = entity.getPosition();
    move(unchanged.x, unchanged.y, 0.01f);
    RecordingBatch after = renderTrail();
    assertEquals(before.ghosts.size(), after.ghosts.size());
    for (int i = 0; i < before.ghosts.size(); i++) {
      assertEquals(before.ghosts.get(i).x, after.ghosts.get(i).x);
      assertEquals(before.ghosts.get(i).y, after.ghosts.get(i).y);
      assertTrue(after.ghosts.get(i).color.a < before.ghosts.get(i).color.a);
    }
    physics.getBody().setLinearVelocity(0f, 0f);
    move(unchanged.x, unchanged.y, 0.01f);
    assertTrue(renderTrail().ghosts.isEmpty());
  }

  @Test
  void fixedStepDashSamplesEverySixtyMillisecondsAcrossUnchangedRenderUpdates() {
    movingPhysicsScene();
    entity.getEvents().trigger("lungeDashStart");
    for (int step = 1; step <= 15; step++) {
      Vector2 unchanged = entity.getPosition();
      move(unchanged.x, unchanged.y, 0.008f);
      move(step * 0.096f, 0f, 0.008f);
    }
    RecordingBatch drawn = renderTrail();
    assertTrue(
        drawn.ghosts.size() >= 3 && drawn.ghosts.size() <= 4,
        "60ms sampling across 240ms must retain three or four ghosts, including unchanged physics updates");
  }

  @Test
  void blockedDashWithDesiredVelocityClearsTrailAfterPhysicsGrace() {
    movingPhysicsScene();
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    assertFalse(renderTrail().ghosts.isEmpty());
    Vector2 blocked = entity.getPosition();
    move(blocked.x, blocked.y, 0.06f);
    RecordingBatch drawn = renderTrail();
    assertTrue(drawn.ghosts.isEmpty());
    assertTrue(drawn.quads.isEmpty());
  }

  @Test
  void statusGlowDoesNotChangeSeparateAlphaBlendStateOfRedTrailPass() {
    entity.getEvents().trigger("lungeDashStart");
    sampleMotion(new Vector2(0.3f, 0f), 4);
    effects.addStatusEffect(new FlashEffect(time, 5000L, null, new Color(1f, 0.2f, 0.1f, 0.5f)));
    RecordingBatch drawn = new RecordingBatch();
    drawn.blendSrcAlpha = GL20.GL_ONE;
    drawn.blendDstAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

    trail.render(drawn.batch);

    assertFalse(drawn.ghosts.isEmpty());
    assertEquals(GL20.GL_SRC_ALPHA, drawn.blendSrc);
    assertEquals(GL20.GL_ONE_MINUS_SRC_ALPHA, drawn.blendDst);
    assertEquals(GL20.GL_ONE, drawn.blendSrcAlpha);
    assertEquals(GL20.GL_ONE_MINUS_SRC_ALPHA, drawn.blendDstAlpha);
  }

  private PhysicsComponent movingPhysicsScene() {
    ServiceLocator.getEntityService().unregister(entity);
    trail.dispose();
    ServiceLocator.registerPhysicsService(new PhysicsService());
    PhysicsComponent physics = new PhysicsComponent();
    ownedPhysics = physics;
    PhysicsMovementComponent movement = new PhysicsMovementComponent();
    body = new AnimationRenderComponent(atlas);
    body.addAnimation("move", 0.1f, Animation.PlayMode.LOOP);
    body.startAnimation("move");
    trail = new MinotaurDashTrailComponent();
    entity =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 2))
            .addComponent(physics)
            .addComponent(movement)
            .addComponent(body)
            .addComponent(trail);
    entity.setScale(1.5f, 1.5f);
    ServiceLocator.getEntityService().register(entity);
    physics.getBody().setLinearVelocity(6f, 0f);
    return physics;
  }

  private void sampleMotion(Vector2 step, int count) {
    for (int i = 0; i < count; i++) {
      Vector2 next = entity.getPosition().add(step);
      move(next.x, next.y, 0.06f);
    }
  }

  private void move(float x, float y, float delta) {
    elapsed += delta;
    when(time.getTime()).thenReturn((long) (elapsed * 1000));
    when(time.getDeltaTime()).thenReturn(delta);
    entity.setPosition(x, y);
    ServiceLocator.getEntityService().update();
  }

  private RecordingBatch renderTrail() {
    return renderTrail(trail);
  }

  private RecordingBatch renderTrail(MinotaurDashTrailComponent component) {
    RecordingBatch recording = new RecordingBatch();
    component.render(recording.batch);
    return recording;
  }

  private record Ghost(
      TextureRegion region, float x, float y, float width, float height, Color color) {}

  private static class RecordingBatch {
    final SpriteBatch batch = mock(SpriteBatch.class);
    final List<Ghost> ghosts = new ArrayList<>();
    final List<float[]> quads = new ArrayList<>();
    final Color originalColor = new Color(0.7f, 0.8f, 0.9f, 0.6f);
    final Color color = new Color(originalColor);
    final ShaderProgram originalShader = mock(ShaderProgram.class);
    ShaderProgram shader = originalShader;
    int blendSrc = GL20.GL_SRC_ALPHA;
    int blendDst = GL20.GL_ONE_MINUS_SRC_ALPHA;
    int blendSrcAlpha = GL20.GL_SRC_ALPHA;
    int blendDstAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

    RecordingBatch() {
      when(batch.getColor()).thenReturn(color);
      when(batch.getBlendSrcFunc()).thenAnswer(invocation -> blendSrc);
      when(batch.getBlendDstFunc()).thenAnswer(invocation -> blendDst);
      when(batch.getBlendSrcFuncAlpha()).thenAnswer(invocation -> blendSrcAlpha);
      when(batch.getBlendDstFuncAlpha()).thenAnswer(invocation -> blendDstAlpha);
      when(batch.getShader()).thenAnswer(invocation -> shader);
      doAnswer(
              invocation -> {
                color.set(
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3));
                return null;
              })
          .when(batch)
          .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
      doAnswer(
              invocation -> {
                color.set((Color) invocation.getArgument(0));
                return null;
              })
          .when(batch)
          .setColor(any(Color.class));
      doAnswer(
              invocation -> {
                blendSrc = invocation.getArgument(0);
                blendDst = invocation.getArgument(1);
                blendSrcAlpha = blendSrc;
                blendDstAlpha = blendDst;
                return null;
              })
          .when(batch)
          .setBlendFunction(anyInt(), anyInt());
      doAnswer(
              invocation -> {
                blendSrc = invocation.getArgument(0);
                blendDst = invocation.getArgument(1);
                blendSrcAlpha = invocation.getArgument(2);
                blendDstAlpha = invocation.getArgument(3);
                return null;
              })
          .when(batch)
          .setBlendFunctionSeparate(anyInt(), anyInt(), anyInt(), anyInt());
      doAnswer(
              invocation -> {
                shader = invocation.getArgument(0);
                return null;
              })
          .when(batch)
          .setShader(any());
      doAnswer(
              invocation -> {
                ghosts.add(
                    new Ghost(
                        new TextureRegion((TextureRegion) invocation.getArgument(0)),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        new Color(color)));
                return null;
              })
          .when(batch)
          .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      doAnswer(
              invocation -> {
                float[] vertices = invocation.getArgument(1);
                int offset = invocation.getArgument(2);
                int count = invocation.getArgument(3);
                quads.add(Arrays.copyOfRange(vertices, offset, offset + count));
                return null;
              })
          .when(batch)
          .draw(any(Texture.class), any(float[].class), anyInt(), anyInt());
    }
  }
}
