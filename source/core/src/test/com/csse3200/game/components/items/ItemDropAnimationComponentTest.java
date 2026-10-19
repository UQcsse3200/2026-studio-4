package com.csse3200.game.components.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.charms.StrengthCharm;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemDropAnimationComponentTest {
  private Texture texture;
  private GameTime clock;
  private SpriteBatch batch;
  private final List<float[]> quads = new ArrayList<>();
  private final List<float[]> buffers = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ServiceLocator.registerRenderService(new RenderService());
    texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(80);
    when(texture.getHeight()).thenReturn(80);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(StrengthCharm.TEXTURE, Texture.class)).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    clock = mock(GameTime.class);
    ServiceLocator.registerTimeSource(clock);
    batch = mock(SpriteBatch.class);
    Color color = new Color(0.8f, 0.7f, 0.6f, 0.5f);
    when(batch.getColor()).thenReturn(color);
    when(batch.getPackedColor()).thenAnswer(call -> color.toFloatBits());
    doAnswer(
            call -> {
              color.set(
                  call.getArgument(0),
                  call.getArgument(1),
                  call.getArgument(2),
                  call.getArgument(3));
              return null;
            })
        .when(batch)
        .setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    when(batch.getBlendSrcFunc()).thenReturn(GL20.GL_SRC_ALPHA);
    when(batch.getBlendDstFunc()).thenReturn(GL20.GL_ONE_MINUS_SRC_ALPHA);
    when(batch.getBlendSrcFuncAlpha()).thenReturn(GL20.GL_ONE);
    when(batch.getBlendDstFuncAlpha()).thenReturn(GL20.GL_ZERO);
    doAnswer(
            call -> {
              float[] vertices = call.getArgument(1);
              buffers.add(vertices);
              quads.add(vertices.clone());
              return null;
            })
        .when(batch)
        .draw(eq(texture), any(float[].class), eq(0), eq(20));
  }

  private Entity item(ItemDropAnimationComponent animation, Vector2 size, Vector2 position) {
    Entity item =
        new Entity().addComponent(new ItemComponent(new StrengthCharm())).addComponent(animation);
    item.setScale(size);
    item.setPosition(position);
    item.create();
    return item;
  }

  private List<float[]> frame(ItemDropAnimationComponent animation) {
    quads.clear();
    animation.render(batch);
    return new ArrayList<>(quads.subList(0, 8));
  }

  @Test
  void rocksRigidlyAboutBottomCentreBothWaysAndSettles() {
    when(clock.getDeltaTime()).thenReturn(0.24f, 0.24f, 0.15f, 0.3f, 0.6f, 3f, 0.24f);
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble(0.5, 0.7)).thenReturn(0.6);
    var animation = new ItemDropAnimationComponent(random);
    Entity item = item(animation, new Vector2(1f, 1f), new Vector2(3f, 5f));
    List<List<float[]>> frames = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      animation.update();
      frames.add(frame(animation));
    }
    assertEquals(5.35f, frames.get(0).get(7)[1], 0.00001f);
    assertEquals(5f, frames.get(1).get(7)[1], 0.00001f);
    float right = topCentreX(frames.get(2)) - 3.5f;
    float left = topCentreX(frames.get(3)) - 3.5f;
    assertTrue(right > 0f);
    assertTrue(left < 0f);
    assertTrue(Math.abs(topCentreX(frames.get(4)) - 3.5f) < Math.abs(left));
    for (int i = 2; i < 7; i++) {
      float[] bottom = frames.get(i).get(7);
      assertEquals(3.5f, (bottom[0] + bottom[15]) / 2f, 0.00001f);
      assertEquals(5f, (bottom[1] + bottom[16]) / 2f, 0.00001f);
      float[] top = frames.get(i).get(0);
      // Rigid rocking preserves width, height and the right angle of the whole icon.
      Vector2 edge = new Vector2(top[10] - top[5], top[11] - top[6]);
      Vector2 side = new Vector2(top[5] - bottom[0], top[6] - bottom[1]);
      assertEquals(1f, edge.len(), 0.00001f);
      assertEquals(1f, side.len(), 0.00001f);
      assertEquals(0f, edge.dot(side), 0.00001f);
    }
    assertEquals(3f, frames.get(5).get(0)[5], 0.00001f);
    assertArrayEquals(frames.get(5).get(0), frames.get(6).get(0));
    assertEquals(new Vector2(3f, 5f), item.getPosition());
  }

  private float topCentreX(List<float[]> frame) {
    return (frame.get(0)[5] + frame.get(0)[10]) / 2f;
  }

  @Test
  void individualPeriodsKeepDropsFromRockingInSyncWithoutChangingTheirPivots() {
    RandomGenerator random = mock(RandomGenerator.class);
    when(random.nextDouble(0.5, 0.7)).thenReturn(0.51, 0.69);
    var fast = new ItemDropAnimationComponent(random);
    var slow = new ItemDropAnimationComponent(random);
    item(fast, new Vector2(1f, 1f), new Vector2(3f, 5f));
    item(slow, new Vector2(1f, 1f), new Vector2(3f, 5f));
    when(clock.getDeltaTime()).thenReturn(0.78f);
    fast.update();
    slow.update();
    assertTrue(topCentreX(frame(fast)) < 3.5f);
    assertTrue(topCentreX(frame(slow)) > 3.5f);
    for (var animation : List.of(fast, slow)) {
      var pose = frame(animation);
      float[] bottom = pose.get(7);
      assertEquals(3.5f, (bottom[0] + bottom[15]) / 2f, 0.00001f);
      assertEquals(5f, (bottom[1] + bottom[16]) / 2f, 0.00001f);
      var repeat = frame(animation);
      for (int row = 0; row < 8; row++) assertArrayEquals(pose.get(row), repeat.get(row));
      when(clock.getDeltaTime()).thenReturn(3.2f);
      animation.update();
      assertEquals(3.5f, topCentreX(frame(animation)), 0.00001f);
    }
    verify(random, times(2)).nextDouble(0.5, 0.7);
    verifyNoMoreInteractions(random);
  }

  @Test
  void differentSizedDropsStartAtOneVisualCentreAndRiseBeforeLanding() {
    Vector2 origin = new Vector2(3f, 5f);
    for (Vector2 size : List.of(new Vector2(1f, 2f), new Vector2(2f, 0.5f))) {
      var animation = new ItemDropAnimationComponent();
      animation.launchFrom(origin);
      Entity item = item(animation, size, origin);
      List<float[]> initial = frame(animation);
      assertEquals(origin.x, (initial.get(0)[5] + initial.get(0)[10]) / 2f, 0.00001f);
      assertEquals(origin.y, (initial.get(0)[6] + initial.get(7)[1]) / 2f, 0.00001f);
      when(clock.getDeltaTime()).thenReturn(0.35f);
      animation.update();
      List<float[]> apex = frame(animation);
      assertTrue((apex.get(0)[6] + apex.get(7)[1]) / 2f > origin.y + 0.9f);
      when(clock.getDeltaTime()).thenReturn(0.35f);
      animation.update();
      assertEquals(origin.y, frame(animation).get(7)[1], 0.00001f);
      assertEquals(origin, item.getPosition());
    }
  }

  @Test
  void invalidFrameTimesCannotCorruptTheMesh() {
    var animation = new ItemDropAnimationComponent();
    item(animation, new Vector2(1f, 1f), new Vector2(3f, 5f));
    var initial = frame(animation);
    when(clock.getDeltaTime()).thenReturn(Float.NaN, -1f, 0f, Float.POSITIVE_INFINITY, 4f);
    for (int i = 0; i < 5; i++) {
      animation.update();
      var current = frame(animation);
      for (int row = 0; row < 8; row++) assertArrayEquals(initial.get(row), current.get(row));
    }
  }

  @Test
  void reflectionFollowsBottomPivotRotationReusesBufferAndRestoresBatchAfterFailure() {
    var animation = new ItemDropAnimationComponent();
    item(animation, new Vector2(1f, 1f), new Vector2(3f, 5f));
    when(clock.getDeltaTime()).thenReturn(0.63f);
    animation.update();
    animation.render(batch);
    int count = quads.size();
    assertTrue(count > 8);
    float[] bottom = quads.get(7);
    float cosine = bottom[15] - bottom[0];
    float sine = bottom[1] - bottom[16];
    for (float[] quad : quads) {
      for (int v = 0; v < 20; v += 5) {
        float height = 1f - quad[v + 4];
        float x = quad[v + 3] - 0.5f;
        assertEquals(3.5f + x * cosine + height * sine, quad[v], 0.00001f);
        assertEquals(5f - x * sine + height * cosine, quad[v + 1], 0.00001f);
      }
    }
    animation.render(batch);
    assertEquals(count * 2, quads.size());
    for (int i = 0; i < count; i++) assertArrayEquals(quads.get(i), quads.get(i + count));
    for (float[] buffer : buffers) assertSame(buffers.get(0), buffer);
    var alpha = org.mockito.ArgumentCaptor.forClass(Float.class);
    verify(batch, atLeastOnce()).setColor(eq(0.8f), eq(0.7f), eq(0.6f), alpha.capture());
    assertTrue(alpha.getAllValues().stream().anyMatch(value -> value > 0.275f && value < 0.5f));
    // Fail during the reflection, after the eight base strips have been drawn.
    int[] draws = {0};
    doAnswer(
            call -> {
              if (++draws[0] > 8) throw new IllegalStateException("draw failed");
              return null;
            })
        .when(batch)
        .draw(eq(texture), any(float[].class), eq(0), eq(20));
    assertThrows(IllegalStateException.class, () -> animation.render(batch));
    assertEquals(new Color(0.8f, 0.7f, 0.6f, 0.5f), batch.getColor());
    verify(batch, times(3))
        .setBlendFunctionSeparate(
            GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ZERO);
  }
}
