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
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class FinalBossStageTwoPickupVisualsTest {
  private static final String ROOT = "images/final-boss/stage2/";
  private static final String[] PATHS = {
    ROOT + "pickup/GEM 1 - BLUE - Spritesheet.png",
    ROOT + "pickup/Spark - Spritesheet.png",
    ROOT + "ice-aura/ice_sparkles.png",
    ROOT + "obstacle/crystal-icy.png",
    ROOT + "transform/01.png"
  };
  private static final float COLOUR = Color.toFloatBits(0.2f, 0.4f, 0.6f, 0.8f);
  private ResourceService resources;
  private SpriteBatch batch;
  private FinalBossStageTwoPickupController pickups;
  private FinalBossStageTwoPickupVisuals visuals;
  private Entity player;
  private Rectangle arena;

  @BeforeEach
  void setup() {
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(PATHS);
    resources.loadAll();
    visuals = new FinalBossStageTwoPickupVisuals();
    pickups =
        spy(new FinalBossStageTwoPickupController(new FinalBossStageTwoConfig(), new Random(1)));
    arena = new Rectangle(0f, 0f, 10f, 8f);
    doAnswer(invocation -> new Rectangle(arena)).when(pickups).getArenaBounds();
    batch = mock(SpriteBatch.class);
    when(batch.getPackedColor()).thenReturn(COLOUR);
    player = new Entity().addComponent(new CombatStatsComponent(100, 1));
    player.setScale(1f, 1.5f);
    player.setPosition(4f, 3f);
  }

  @AfterEach
  void cleanup() {
    resources.unloadAssets(PATHS);
  }

  @Test
  void groundEffectsUseRealAnimationFramesWithoutChangingPickupTimersOrPositions() {
    FinalBossStageTwoPickupController.Pickup pickup =
        new FinalBossStageTwoPickupController.Pickup(new Vector2(3f, 4f));
    pickup.elapsed = 0.55;
    pickups.pickups.add(pickup);
    FinalBossStageTwoPickupController.Burst burst =
        new FinalBossStageTwoPickupController.Burst(new Vector2(5f, 4f));
    burst.elapsed = pickups.getEffectDuration() * 0.95f;
    pickups.bursts.add(burst);
    // Reaching two charges must not make an already-spawned ground item invisible.
    doReturn(2).when(pickups).getChargeCount();

    visuals.drawGround(batch, pickups);

    List<Draw> draws = draws();
    assertEquals(2, draws.size());
    assertSame(resources.getAsset(PATHS[0], Texture.class), draws.get(0).frame().getTexture());
    assertEquals(0.36f, draws.get(0).width(), 0.0001f);
    assertEquals(0.60f, draws.get(0).height(), 0.0001f);
    assertSame(resources.getAsset(PATHS[1], Texture.class), draws.get(1).frame().getTexture());
    assertEquals(180, draws.get(1).frame().getRegionX());
    assertEquals(0.55, pickup.elapsed);
    assertEquals(new Vector2(3f, 4f), pickup.position);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    pickup.elapsed = pickups.getPickupLifetime();
    burst.elapsed = pickups.getEffectDuration();

    visuals.drawGround(batch, pickups);

    assertTrue(draws().isEmpty());
  }

  @Test
  void expiredGemPlaysTheFifthRowOnceAtItsGroundPositionThenStops() {
    FinalBossStageTwoPickupController.Burst disappearance =
        new FinalBossStageTwoPickupController.Burst(new Vector2(3f, 4f));
    pickups.disappearances.add(disappearance);

    visuals.drawGround(batch, pickups);

    assertEquals(1, draws().size());
    Draw first = draws().get(0);
    assertSame(resources.getAsset(PATHS[4], Texture.class), first.frame().getTexture());
    assertEquals(0, first.frame().getRegionX());
    assertEquals(256, first.frame().getRegionY());
    assertEquals(2.55f, first.x(), 0.0001f);
    assertEquals(3.55f, first.y(), 0.0001f);
    assertEquals(0.9f, first.width(), 0.0001f);
    assertEquals(0.9f, first.height(), 0.0001f);
    assertEquals(0f, disappearance.elapsed);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    float elapsed = pickups.getDisappearDuration() * 0.95f;
    disappearance.elapsed = elapsed;

    visuals.drawGround(batch, pickups);

    assertEquals(1, draws().size());
    Draw last = draws().get(0);
    assertEquals(512, last.frame().getRegionX());
    assertEquals(256, last.frame().getRegionY());
    assertEquals(first.x(), last.x());
    assertEquals(first.y(), last.y());
    ArgumentCaptor<Float> alpha = ArgumentCaptor.forClass(Float.class);
    verify(batch).setColor(eq(1f), eq(1f), eq(1f), alpha.capture());
    assertTrue(alpha.getValue() > 0f && alpha.getValue() < 1f);
    assertEquals(elapsed, disappearance.elapsed);
    assertEquals(new Vector2(3f, 4f), disappearance.position);
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    disappearance.elapsed = pickups.getDisappearDuration();

    visuals.drawGround(batch, pickups);

    assertTrue(draws().isEmpty());
    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void disappearanceStaysInsideArenaAndRestoresColourEvenOnDrawingFailure() {
    FinalBossStageTwoPickupController.Burst disappearance =
        new FinalBossStageTwoPickupController.Burst(new Vector2(9.9f, 7.9f));
    pickups.disappearances.add(disappearance);

    visuals.drawGround(batch, pickups);

    assertEquals(1, draws().size());
    Draw effect = draws().get(0);
    assertTrue(effect.x() >= arena.x && effect.y() >= arena.y);
    assertTrue(effect.x() + effect.width() <= arena.x + arena.width + 0.0001f);
    assertTrue(effect.y() + effect.height() <= arena.y + arena.height + 0.0001f);
    assertEquals(new Vector2(9.9f, 7.9f), disappearance.position);
    assertEquals(1f, effect.r());
    assertEquals(1f, effect.g());
    assertEquals(1f, effect.b());
    verify(batch).setPackedColor(COLOUR);
    clearInvocations(batch);
    doThrow(new IllegalStateException("simulated disappearance draw failure"))
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    assertThrows(IllegalStateException.class, () -> visuals.drawGround(batch, pickups));

    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void eachBlueBarUsesItsOwnEnergyFractionAndRenderingDoesNotConsumeEnergy() {
    setCharges(2, 0.25f, 1f);

    visuals.drawPlayerBuff(batch, pickups, player, 0.3f);

    List<Draw> fills = drawsWithTint(0.20f, 0.65f, 1f);
    assertEquals(2, fills.size());
    assertEquals(2, drawsWithTint(0.15f, 0.23f, 0.35f).size());
    assertEquals(2, drawsWithTint(0.04f, 0.08f, 0.14f).size());
    assertEquals(fills.get(1).height() * 0.25f, fills.get(0).height(), 0.0001f);
    assertEquals(fills.get(0).y(), fills.get(1).y(), 0.0001f);
    assertTrue(fills.get(0).x() + fills.get(0).width() < fills.get(1).x());
    assertTrue(
        draws().stream()
            .anyMatch(
                draw -> draw.frame().getTexture() == resources.getAsset(PATHS[2], Texture.class)));
    verify(pickups, never()).consumeEnergy(anyFloat());
    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void oneChargeOnlyDrawsTheOccupiedBarIncludingItsOutlineAndBackground() {
    setCharges(1, 0.6f, 0f);

    visuals.drawPlayerBuff(batch, pickups, player, 0f);

    assertEquals(1, drawsWithTint(0.15f, 0.23f, 0.35f).size());
    assertEquals(1, drawsWithTint(0.04f, 0.08f, 0.14f).size());
    assertEquals(1, drawsWithTint(0.20f, 0.65f, 1f).size());
    Draw firstOutline = drawsWithTint(0.15f, 0.23f, 0.35f).get(0);
    clearInvocations(batch);
    setCharges(1, 0f, 0.6f);

    visuals.drawPlayerBuff(batch, pickups, player, 0f);

    List<Draw> outlines = drawsWithTint(0.15f, 0.23f, 0.35f);
    List<Draw> fills = drawsWithTint(0.20f, 0.65f, 1f);
    assertEquals(1, outlines.size());
    assertEquals(1, drawsWithTint(0.04f, 0.08f, 0.14f).size());
    assertEquals(1, fills.size());
    assertTrue(
        outlines.get(0).x() > firstOutline.x() + firstOutline.width(),
        "An occupied second slot must stay in the right bar when the first slot is empty");
  }

  @Test
  void emptyOrDeadPlayerAndMissingControllerHideTheBuffWithoutTouchingBatchState() {
    setCharges(0, 0f, 0f);
    visuals.drawPlayerBuff(batch, pickups, player, 0f);
    setCharges(2, 1f, 1f);
    player.getComponent(CombatStatsComponent.class).setHealth(0);
    visuals.drawPlayerBuff(batch, pickups, player, 0f);
    visuals.drawPlayerBuff(batch, null, player, 0f);
    visuals.drawGround(batch, null);

    verifyNoInteractions(batch);
  }

  @Test
  void auraAndBothBarsStayInsideArenaAndBarsMoveLeftAtTheRightEdge() {
    setCharges(2, 1f, 1f);
    player.setPosition(9f, 6.5f);

    visuals.drawPlayerBuff(batch, pickups, player, 0f);

    for (Draw draw : draws()) {
      assertTrue(draw.x() >= arena.x - 0.0001f);
      assertTrue(draw.y() >= arena.y - 0.0001f);
      assertTrue(draw.x() + draw.width() <= arena.x + arena.width + 0.0001f);
      assertTrue(draw.y() + draw.height() <= arena.y + arena.height + 0.0001f);
    }
    for (Draw bar : drawsWithTint(0.15f, 0.23f, 0.35f)) {
      assertTrue(bar.x() + bar.width() < player.getPosition().x);
    }
    assertEquals(new Rectangle(0f, 0f, 10f, 8f), arena);
    verify(batch).setPackedColor(COLOUR);
  }

  @Test
  void restoresColourWhenDrawingThePlayerEffectFails() {
    setCharges(1, 1f, 0f);
    doThrow(new IllegalStateException("simulated sprite failure"))
        .when(batch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    assertThrows(
        IllegalStateException.class, () -> visuals.drawPlayerBuff(batch, pickups, player, 0f));

    verify(batch).setPackedColor(COLOUR);
  }

  private void setCharges(int count, float first, float second) {
    doReturn(count).when(pickups).getChargeCount();
    doReturn(first).when(pickups).getChargeFraction(0);
    doReturn(second).when(pickups).getChargeFraction(1);
  }

  private List<Draw> drawsWithTint(float r, float g, float b) {
    return draws().stream()
        .filter(draw -> draw.r() == r && draw.g() == g && draw.b() == b)
        .toList();
  }

  private List<Draw> draws() {
    List<Draw> result = new ArrayList<>();
    float r = 1f;
    float g = 1f;
    float b = 1f;
    for (var call : mockingDetails(batch).getInvocations()) {
      Object[] args = call.getArguments();
      if (call.getMethod().getName().equals("setColor") && args.length == 4) {
        r = (float) args[0];
        g = (float) args[1];
        b = (float) args[2];
      } else if (call.getMethod().getName().equals("draw") && args.length == 5) {
        result.add(
            new Draw(
                (TextureRegion) args[0],
                (float) args[1],
                (float) args[2],
                (float) args[3],
                (float) args[4],
                r,
                g,
                b));
      }
    }
    return result;
  }

  private record Draw(
      TextureRegion frame,
      float x,
      float y,
      float width,
      float height,
      float r,
      float g,
      float b) {}
}
