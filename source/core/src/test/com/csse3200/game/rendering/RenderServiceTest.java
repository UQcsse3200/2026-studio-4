package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class RenderServiceTest {
  @Test
  void shouldRender() {
    RenderService renderService = new RenderService();
    Renderable renderable = mock(Renderable.class);
    SpriteBatch spriteBatch = mock(SpriteBatch.class);
    renderService.register(renderable);
    renderService.render(spriteBatch);
    verify(renderable).render(spriteBatch);

    renderService.unregister(renderable);
  }

  @Test
  void shouldNotRenderAfterUnregister() {
    RenderService renderService = new RenderService();
    Renderable renderable = mock(Renderable.class);
    SpriteBatch spriteBatch = mock(SpriteBatch.class);
    renderService.register(renderable);
    renderService.unregister(renderable);
    renderService.render(spriteBatch);
    verify(renderable, times(0)).render(any());
  }

  @Test
  void shouldRenderInZIndexOrder() {
    RenderService renderService = new RenderService();
    SpriteBatch spriteBatch = mock(SpriteBatch.class);
    RenderComponent renderable1 = mock(RenderComponent.class);
    RenderComponent renderable2 = mock(RenderComponent.class);

    // Same layer, renderable2 is in front
    when(renderable1.getLayer()).thenReturn(1);
    when(renderable2.getLayer()).thenReturn(1);
    when(renderable1.compareTo(any())).thenReturn(1);
    when(renderable2.compareTo(any())).thenReturn(-1);

    renderService.register(renderable1);
    renderService.register(renderable2);

    InOrder inOrder = Mockito.inOrder(renderable1, renderable2);
    renderService.render(spriteBatch);
    inOrder.verify(renderable2).render(any());
    inOrder.verify(renderable1).render(any());
  }

  @Test
  void shouldRenderInLayerOrder() {
    RenderService renderService = new RenderService();
    SpriteBatch spriteBatch = mock(SpriteBatch.class);
    Renderable renderable1 = mock(Renderable.class);
    Renderable renderable2 = mock(Renderable.class);

    when(renderable1.getLayer()).thenReturn(1);
    when(renderable2.getLayer()).thenReturn(2);
    when(renderable1.compareTo(any())).thenReturn(1);
    when(renderable2.compareTo(any())).thenReturn(-1);

    renderService.register(renderable1);
    renderService.register(renderable2);

    InOrder inOrder = Mockito.inOrder(renderable1, renderable2);
    renderService.render(spriteBatch);
    inOrder.verify(renderable1).render(any());
    inOrder.verify(renderable2).render(any());
  }

  @Test
  void shouldDecayWorldShakeAndRestoreProjection() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(0.25f);
    RenderService service = new RenderService();
    Matrix4 base = new Matrix4();
    service.shake(this, 1f, 0.8f);

    assertEquals(0.6f, displacement(service.getWorldProjection(base)), 0.001f);
    assertEquals(0.4f, displacement(service.getWorldProjection(base)), 0.001f);
    assertEquals(0.2f, displacement(service.getWorldProjection(base)), 0.001f);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldReuseProjectionWithoutMutatingCameraOrAccumulatingOffset() {
    RenderService service = new RenderService();
    Matrix4 base = new Matrix4().setToOrtho2D(3f, 7f, 20f, 12f);
    Matrix4 original = new Matrix4(base);
    service.shake(this, 1f, 0.1f);

    Matrix4 world = service.getWorldProjection(base);
    Matrix4 firstFrame = new Matrix4(world);
    assertNotSame(base, world);
    for (int i = 0; i < 10; i++) {
      assertSame(world, service.getWorldProjection(base));
      assertArrayEquals(firstFrame.val, world.val);
      assertArrayEquals(original.val, base.val);
    }

    service.clearShake(this);
    base.setToOrtho2D(11f, 17f, 30f, 15f);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldOnlyClearShakeForItsExactOwner() {
    RenderService service = new RenderService();
    Object owner = new String("owner");
    Matrix4 base = new Matrix4();
    service.shake(owner, 1f, 0.1f);

    service.clearShake(new String("owner"));
    assertTrue(displacement(service.getWorldProjection(base)) > 0f);
    service.clearShake(null);
    assertTrue(displacement(service.getWorldProjection(base)) > 0f);
    service.clearShake(owner);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldNotLetOldOwnerClearReplacementShake() {
    RenderService service = new RenderService();
    Object replacementOwner = new Object();
    Matrix4 base = new Matrix4();
    service.shake(this, 1f, 0.1f);
    service.shake(replacementOwner, 0.5f, 0.2f);

    service.clearShake(this);
    assertEquals(0.2f, displacement(service.getWorldProjection(base)), 0.001f);
    service.clearShake(replacementOwner);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldIgnoreInvalidShakeRequestsWithoutReplacingActivePulse() {
    RenderService service = new RenderService();
    Matrix4 base = new Matrix4();
    service.shake(this, 1f, 0.1f);
    Object invalidOwner = new Object();
    service.shake(null, 1f, 0.2f);
    for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      service.shake(invalidOwner, invalid, 0.2f);
      service.shake(invalidOwner, 1f, invalid);
    }

    assertEquals(0.1f, displacement(service.getWorldProjection(base)), 0.001f);
    service.clearShake(this);
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldIgnoreInvalidDeltaAndResumeWhenTimeIsValid() {
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    when(time.getDeltaTime()).thenReturn(Float.NaN, -0.1f, Float.POSITIVE_INFINITY, 0f, 1f);
    RenderService service = new RenderService();
    Matrix4 base = new Matrix4();
    service.shake(this, 1f, 0.1f);

    for (int i = 0; i < 4; i++) {
      assertEquals(0.1f, displacement(service.getWorldProjection(base)), 0.001f);
    }
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  @Test
  void shouldClearShakeOnDispose() {
    RenderService service = new RenderService();
    Matrix4 base = new Matrix4();
    service.shake(this, 1f, 0.1f);
    assertTrue(displacement(service.getWorldProjection(base)) > 0f);

    service.dispose();
    assertArrayEquals(base.val, service.getWorldProjection(base).val);
  }

  private static float displacement(Matrix4 projection) {
    return (float) Math.hypot(projection.val[Matrix4.M03], projection.val[Matrix4.M13]);
  }
}
