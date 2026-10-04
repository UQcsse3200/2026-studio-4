package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderable;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoPlayerIceVisualsTest {
  private static final String ROOT = "images/final-boss/stage2/";
  private static final float COLOUR = Color.toFloatBits(0.2f, 0.4f, 0.6f, 0.8f);
  private ResourceService resources;
  private SpriteBatch batch;
  private FinalBossStageTwoPlayerIceController ice;
  private FinalBossStageTwoPlayerIceVisuals visuals;
  private Entity player;

  @BeforeEach
  void setup() {
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(FinalBossStageTwoAssets.paths());
    resources.loadAll();
    visuals = new FinalBossStageTwoPlayerIceVisuals();
    ice = new FinalBossStageTwoPlayerIceController(new FinalBossStageTwoConfig());
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(COLOUR);
    player = new Entity().addComponent(new CombatStatsComponent(100, 1));
    player.setPosition(4f, 3f);
    player.setScale(1f, 1.5f);
  }

  @AfterEach
  void cleanup() {
    resources.unloadAssets(FinalBossStageTwoAssets.paths());
  }

  @Test
  void projectileTurnsAroundItsBrightHeadAndRenderingDoesNotAdvanceFlight() {
    FinalBossStageTwoPlayerIceController.Shot shot =
        new FinalBossStageTwoPlayerIceController.Shot(1L, new Vector2(5f, 6f), new Vector2(0f, 4f));
    ice.shots.add(shot);

    visuals.drawShots(batch, ice);

    TextureRegion first = verifyProjectileDraw(90f);
    assertSame(
        resources.getAsset(ROOT + "ice-projectile/Icespear.png", Texture.class),
        first.getTexture());
    assertEquals(0f, shot.elapsed);
    assertEquals(new Vector2(5f, 6f), shot.position);
    assertEquals(new Vector2(0f, 4f), shot.velocity);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    shot.velocity.set(-4f, 0f);
    shot.elapsed = 0.15f;

    visuals.drawShots(batch, ice);

    TextureRegion turning = verifyProjectileDraw(180f);
    assertSame(
        resources.getAsset(ROOT + "ice-projectile/Icespear4.png", Texture.class),
        turning.getTexture());
    assertEquals(0.15f, shot.elapsed);
    assertEquals(new Vector2(5f, 6f), shot.position);
    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void impactUsesTheLastVisibleSparkFrameAndStopsAtItsLifetime() {
    FinalBossStageTwoPlayerIceController.Impact impact =
        new FinalBossStageTwoPlayerIceController.Impact(new Vector2(3f, 4f));
    impact.elapsed = ice.getImpactDuration() * 0.95f;
    ice.impacts.add(impact);

    visuals.drawShots(batch, ice);

    TextureRegion frame = onlySimpleDraw();
    assertSame(
        resources.getAsset(ROOT + "pickup/Spark - Spritesheet.png", Texture.class),
        frame.getTexture());
    assertEquals(180, frame.getRegionX());
    ArgumentCaptor<Float> alpha = ArgumentCaptor.forClass(Float.class);
    verify(batch).setColor(eq(0.55f), eq(0.85f), eq(1f), alpha.capture());
    assertTrue(alpha.getValue() > 0f && alpha.getValue() < 1f);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    impact.elapsed = ice.getImpactDuration();

    visuals.drawShots(batch, ice);

    verify(batch, never())
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
  }

  @Test
  void endingContractsAndFadesAboveThePlayerWithoutLeavingTheArena() {
    Rectangle arena = new Rectangle(0f, 0f, 10f, 8f);
    player.setPosition(9f, 6.5f);

    visuals.drawEnding(batch, player, arena, 0.5f, 0.5f);

    TextureRegion first = onlySimpleDraw();
    assertEquals(0, first.getRegionX());
    assertEquals(256, first.getRegionY());
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);

    visuals.drawEnding(batch, player, arena, 0.05f, 0.5f);

    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    ArgumentCaptor<Float> x = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> y = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> width = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> height = ArgumentCaptor.forClass(Float.class);
    verify(batch)
        .draw(frame.capture(), x.capture(), y.capture(), width.capture(), height.capture());
    assertEquals(512, frame.getValue().getRegionX());
    assertSame(first.getTexture(), frame.getValue().getTexture());
    assertTrue(x.getValue() >= arena.x && y.getValue() >= arena.y);
    assertTrue(x.getValue() + width.getValue() <= 10f + 0.0001f);
    assertTrue(y.getValue() + height.getValue() <= 8f + 0.0001f);
    ArgumentCaptor<Float> alpha = ArgumentCaptor.forClass(Float.class);
    verify(batch).setColor(eq(1f), eq(1f), eq(1f), alpha.capture());
    assertTrue(alpha.getValue() > 0f && alpha.getValue() < 1f);
    verify(batch).setPackedColor(COLOUR);
    assertEquals(new Rectangle(0f, 0f, 10f, 8f), arena);
  }

  @Test
  void missingControllerExpiredEndingOrDeadPlayerDoesNotTouchBatchState() {
    visuals.drawShots(batch, null);
    visuals.drawEnding(batch, player, null, 0f, 0.5f);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    visuals.drawEnding(batch, player, null, 0.5f, 0.5f);

    verifyNoInteractions(batch);
  }

  @Test
  void restoresColourWhenProjectileOrEndingRenderingThrows() {
    ice.shots.add(
        new FinalBossStageTwoPlayerIceController.Shot(
            1L, new Vector2(5f, 6f), new Vector2(0f, 4f)));
    doThrow(new IllegalStateException("simulated shot draw failure"))
        .when(batch)
        .draw(
            any(TextureRegion.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());

    assertThrows(IllegalStateException.class, () -> visuals.drawShots(batch, ice));

    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    doThrow(new IllegalStateException("simulated ending draw failure"))
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    assertThrows(
        IllegalStateException.class, () -> visuals.drawEnding(batch, player, null, 0.5f, 0.5f));

    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void encounterOverlayHidesIceOnPhaseExitDeathAndDisposal() {
    resources.loadTextures(FinalBossVisualAssets.paths());
    resources.loadAll();
    RenderService renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    FinalBossPhaseControllerComponent phases = mock(FinalBossPhaseControllerComponent.class);
    FinalBossStageTwoComponent stage = mock(FinalBossStageTwoComponent.class);
    when(stage.getPlayerIceController()).thenReturn(ice);
    when(stage.getIceBuffEndRemaining()).thenReturn(0.5f);
    when(stage.getIceBuffEndDuration()).thenReturn(0.5f);
    ice.shots.add(
        new FinalBossStageTwoPlayerIceController.Shot(
            1L, new Vector2(5f, 6f), new Vector2(0f, 4f)));
    FinalBossStageTwoVisualComponent visual = new FinalBossStageTwoVisualComponent(player);
    new Entity()
        .addComponent(new CombatStatsComponent(100, 1))
        .addComponent(phases)
        .addComponent(stage)
        .addComponent(visual);
    visual.create();
    try {
      ArgumentCaptor<Renderable> registrations = ArgumentCaptor.forClass(Renderable.class);
      verify(renderer, times(2)).register(registrations.capture());
      Renderable overlay = registrations.getAllValues().get(1);
      when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_THREE);
      overlay.render(batch);
      when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
      player.getComponent(CombatStatsComponent.class).setHealth(0);
      overlay.render(batch);
      verifyNoInteractions(batch);
      player.getComponent(CombatStatsComponent.class).setHealth(100);

      overlay.render(batch);

      verifyProjectileDraw(90f);
      assertEquals(256, onlySimpleDraw().getRegionY());
      clearInvocations(batch);
      visual.dispose();
      visual.dispose();
      overlay.render(batch);
      verifyNoInteractions(batch);
      verify(renderer).unregister(visual);
      verify(renderer).unregister(overlay);
      assertTrue(resources.containsAsset(ROOT + "ice-projectile/Icespear.png", Texture.class));
    } finally {
      visual.dispose();
      resources.unloadAssets(FinalBossVisualAssets.paths());
    }
  }

  private TextureRegion verifyProjectileDraw(float rotation) {
    float width = 1.05f;
    float height = 0.525f;
    float anchorX = width * 50f / 64f;
    float anchorY = height * 17f / 32f;
    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch)
        .draw(
            frame.capture(),
            eq(5f - anchorX),
            eq(6f - anchorY),
            eq(anchorX),
            eq(anchorY),
            eq(width),
            eq(height),
            eq(1f),
            eq(1f),
            eq(rotation));
    return frame.getValue();
  }

  private TextureRegion onlySimpleDraw() {
    ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch).draw(frame.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    return frame.getValue();
  }
}
