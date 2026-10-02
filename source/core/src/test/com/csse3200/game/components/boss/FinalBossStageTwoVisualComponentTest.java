package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageTwoConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderable;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoVisualComponentTest {
  private static final String ROOT = "images/final-boss/stage2/";
  private static final float BATCH_COLOUR = Color.toFloatBits(0.2f, 0.3f, 0.4f, 0.8f);
  private ResourceService resources;
  private RenderService renderer;
  private SpriteBatch batch;
  private FinalBossPhaseControllerComponent phases;
  private FinalBossStageTwoComponent stage;
  private FinalBossStageTwoFireController fire;
  private FinalBossStageTwoVisualComponent visual;
  private Renderable overlay;
  private CombatStatsComponent bossStats;
  private CombatStatsComponent playerStats;
  private String[] paths;
  private final Map<String, Texture> sharedTextures = new HashMap<>();

  @BeforeEach
  void createVisualWithRealArtworkAndMockRenderer() {
    resources = spy(new ResourceService());
    renderer = mock(RenderService.class);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(renderer);
    paths =
        Stream.of(FinalBossStageTwoAssets.paths(), FinalBossVisualAssets.paths())
            .flatMap(Stream::of)
            .toArray(String[]::new);
    resources.loadTextures(paths);
    resources.loadAll();
    for (String path : paths) {
      // Keep real loaded image data while observing who attempts to dispose shared textures.
      Texture texture = spy(resources.getAsset(path, Texture.class));
      sharedTextures.put(path, texture);
      doReturn(texture).when(resources).getAsset(path, Texture.class);
    }

    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(BATCH_COLOUR);
    phases = mock(FinalBossPhaseControllerComponent.class);
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    stage = mock(FinalBossStageTwoComponent.class);
    fire = new FinalBossStageTwoFireController(new FinalBossStageTwoConfig(), new Random(1));
    when(stage.getFireController()).thenReturn(fire);
    playerStats = new CombatStatsComponent(100, 1);
    Entity player = new Entity().addComponent(playerStats);
    player.setPosition(8f, 2f);
    bossStats = new CombatStatsComponent(100, 1);
    visual = new FinalBossStageTwoVisualComponent(player);
    Entity boss =
        new Entity()
            .addComponent(bossStats)
            .addComponent(phases)
            .addComponent(stage)
            .addComponent(visual);
    boss.setPosition(2f, 3f);
    boss.setScale(2f, 2f);
    visual.create();

    ArgumentCaptor<Renderable> registered = ArgumentCaptor.forClass(Renderable.class);
    verify(renderer, times(2)).register(registered.capture());
    assertSame(visual, registered.getAllValues().get(0));
    overlay = registered.getAllValues().get(1);
    assertEquals(Float.MAX_VALUE, overlay.getZIndex());
  }

  @AfterEach
  void releaseResources() {
    if (visual != null) visual.dispose();
    if (resources != null && paths != null) resources.unloadAssets(paths);
  }

  @Test
  void transformationChangesWizardAtItsMidpointAndKeepsTheBurstOverBothBodies() {
    when(phases.isTransitioning()).thenReturn(true);
    when(phases.getTransitionProgress()).thenReturn(0.25f);

    visual.render(batch);

    List<TextureRegion> firstHalf = simpleDraws(3);
    assertSame(sharedTextures.get("images/final-boss/wizard.png"), firstHalf.get(0).getTexture());
    assertSame(
        sharedTextures.get(ROOT + "shield/fire_circles_50x50px.png"),
        firstHalf.get(1).getTexture());
    assertSame(sharedTextures.get(ROOT + "transform/01.png"), firstHalf.get(2).getTexture());
    assertEquals(320, firstHalf.get(2).getRegionY());
    verify(batch).setPackedColor(BATCH_COLOUR);
    clearInvocations(batch);
    when(phases.getTransitionProgress()).thenReturn(0.75f);

    visual.render(batch);

    List<TextureRegion> secondHalf = simpleDraws(3);
    assertSame(
        sharedTextures.get("images/final-boss/wizard-stage-2.png"), secondHalf.get(0).getTexture());
    assertSame(sharedTextures.get(ROOT + "transform/01.png"), secondHalf.get(2).getTexture());
    assertTrue(secondHalf.get(2).getRegionX() > firstHalf.get(2).getRegionX());
    verify(batch).setPackedColor(BATCH_COLOUR);
  }

  @Test
  void firingPauseKeepsTheFireShieldVisibleWithoutReplayingTransformation() {
    when(stage.isAttacking()).thenReturn(false);
    when(phases.isTransitioning()).thenReturn(false);

    visual.render(batch);

    List<TextureRegion> draws = simpleDraws(2);
    assertSame(
        sharedTextures.get("images/final-boss/wizard-stage-2.png"), draws.get(0).getTexture());
    assertSame(
        sharedTextures.get(ROOT + "shield/fire_circles_50x50px.png"), draws.get(1).getTexture());
  }

  @Test
  void stageChangeOrEitherDeathHidesBothTheBossAndProjectileOverlay() {
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(1L, new Vector2(4f, 5f), new Vector2(1f, 0f)));
    for (FinalBossPhase phase :
        new FinalBossPhase[] {FinalBossPhase.STAGE_THREE, FinalBossPhase.DEFEATED}) {
      when(phases.getCurrentPhase()).thenReturn(phase);
      visual.render(batch);
      overlay.render(batch);
    }
    when(phases.getCurrentPhase()).thenReturn(FinalBossPhase.STAGE_TWO);
    bossStats.setHealth(0);
    visual.render(batch);
    overlay.render(batch);
    bossStats.setHealth(100);
    playerStats.setHealth(0);
    visual.render(batch);
    overlay.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void disposalUnregistersBothLayersOnceWithoutDisposingRoomTextures() {
    visual.dispose();
    visual.dispose();

    verify(renderer, times(1)).unregister(visual);
    verify(renderer, times(1)).unregister(overlay);
    verify(renderer, times(2)).unregister(any(Renderable.class));
    for (Map.Entry<String, Texture> entry : sharedTextures.entrySet()) {
      verify(entry.getValue(), never()).dispose();
      assertTrue(resources.containsAsset(entry.getKey(), Texture.class));
    }
    visual.render(batch);
    overlay.render(batch);
    verifyNoInteractions(batch);
  }

  @Test
  void bodyRenderingRestoresBatchColourEvenWhenDrawingTheShieldFails() {
    Texture shield = sharedTextures.get(ROOT + "shield/fire_circles_50x50px.png");
    doThrow(new IllegalStateException("simulated draw failure"))
        .when(batch)
        .draw(
            argThat((TextureRegion frame) -> frame.getTexture() == shield),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());

    assertThrows(IllegalStateException.class, () -> visual.render(batch));

    verify(batch).setPackedColor(BATCH_COLOUR);
  }

  @Test
  void fireballRotationUsesItsBrightHeadAsAnchorAndOverlayRestoresBatchColour() {
    Vector2 velocity = new Vector2(0f, 4f);
    fire.fireballs.add(
        new FinalBossStageTwoFireController.Fireball(1L, new Vector2(5f, 6f), velocity));
    fire.impacts.add(new FinalBossStageTwoFireController.Impact(new Vector2(7f, 8f)));

    overlay.render(batch);

    float anchorX = 1.15f * 50f / 64f;
    float anchorY = 1.15f * 0.5f;
    ArgumentCaptor<TextureRegion> projectile = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch)
        .draw(
            projectile.capture(),
            eq(5f - anchorX),
            eq(6f - anchorY),
            eq(anchorX),
            eq(anchorY),
            eq(1.15f),
            eq(1.15f),
            eq(1f),
            eq(1f),
            eq(velocity.angleDeg()));
    assertSame(sharedTextures.get(ROOT + "fireball/001.png"), projectile.getValue().getTexture());
    List<TextureRegion> impacts = simpleDraws(1);
    assertSame(sharedTextures.get(ROOT + "impact/001.png"), impacts.get(0).getTexture());
    verify(batch).setPackedColor(BATCH_COLOUR);
  }

  private List<TextureRegion> simpleDraws(int count) {
    ArgumentCaptor<TextureRegion> draws = ArgumentCaptor.forClass(TextureRegion.class);
    verify(batch, times(count))
        .draw(draws.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    return draws.getAllValues();
  }
}
