package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class RendererTest {
  @Spy OrthographicCamera camera;
  @Mock SpriteBatch spriteBatch;
  @Mock Stage stage;
  @Mock Graphics graphics;
  @Mock RenderService renderService;
  @Mock DebugRenderer debugRenderer;
  PhysicsService physicsService;

  @BeforeEach
  void beforeEach() {
    Gdx.graphics = graphics;
    physicsService = new PhysicsService(mock(PhysicsEngine.class));
  }

  @Test
  void shouldResizeCamera() {
    CameraComponent cameraComponent = makeCameraEntity(camera);

    when(stage.getViewport()).thenReturn(mock(Viewport.class));
    when(graphics.getWidth()).thenReturn(100);
    when(graphics.getHeight()).thenReturn(200);
    Renderer renderer =
        new Renderer(cameraComponent, 10, spriteBatch, stage, renderService, debugRenderer);

    assertEquals(Vector3.Zero, camera.position);
    assertEquals(10, camera.viewportWidth);
    assertEquals(20, camera.viewportHeight);

    renderer.resize(200, 100);
    assertEquals(10, camera.viewportWidth);
    assertEquals(5, camera.viewportHeight);
  }

  @Test
  void shouldResizeViewPort() {
    CameraComponent cameraComponent = makeCameraEntity(camera);
    ScreenViewport screenViewport = spy(ScreenViewport.class);
    Stage stage = new Stage(screenViewport, spriteBatch);

    Renderer renderer =
        new Renderer(cameraComponent, 10, spriteBatch, stage, renderService, debugRenderer);

    assertEquals(0, stage.getViewport().getScreenWidth());
    assertEquals(0, stage.getViewport().getScreenHeight());

    renderer.resize(200, 100);
    verify(screenViewport).update(200, 100, true);
    assertEquals(200, stage.getViewport().getScreenWidth());
    assertEquals(100, stage.getViewport().getScreenHeight());
  }

  @Test
  void shouldRender() {
    CameraComponent cameraComponent = makeCameraEntity(camera);
    Renderer renderer =
        new Renderer(cameraComponent, 10, spriteBatch, stage, renderService, debugRenderer);
    renderer.render();
    verify(renderService).render(spriteBatch);
  }

  @Test
  void shouldShakeWorldAndDebugWhileKeepingCameraAndUiSteady() {
    when(graphics.getWidth()).thenReturn(100);
    when(graphics.getHeight()).thenReturn(100);
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    RenderService worldService = spy(new RenderService());
    CameraComponent cameraComponent = makeCameraEntity(camera);
    Renderer renderer =
        new Renderer(cameraComponent, 10, spriteBatch, stage, worldService, debugRenderer);
    Matrix4 originalProjection = new Matrix4(camera.combined);
    Vector3 originalPosition = new Vector3(camera.position);
    worldService.shake(this, 0.3f, 0.1f);

    renderer.render();

    ArgumentCaptor<Matrix4> worldProjection = ArgumentCaptor.forClass(Matrix4.class);
    InOrder order = inOrder(spriteBatch, worldService, debugRenderer, stage);
    order.verify(spriteBatch).setProjectionMatrix(worldProjection.capture());
    order.verify(spriteBatch).begin();
    order.verify(worldService).render(spriteBatch);
    order.verify(spriteBatch).end();
    order.verify(debugRenderer).render(same(worldProjection.getValue()));
    order.verify(spriteBatch).setProjectionMatrix(same(camera.combined));
    order.verify(stage).act();
    order.verify(stage).draw();
    assertNotSame(camera.combined, worldProjection.getValue());
    assertTrue(
        Math.abs(worldProjection.getValue().val[Matrix4.M03] - camera.combined.val[Matrix4.M03])
                + Math.abs(
                    worldProjection.getValue().val[Matrix4.M13] - camera.combined.val[Matrix4.M13])
            > 0f);
    assertArrayEquals(originalProjection.val, camera.combined.val);
    assertEquals(originalPosition, camera.position);
  }

  @Test
  void whiteFlashCoversUiAfterStageDraw() {
    when(graphics.getWidth()).thenReturn(100);
    when(graphics.getHeight()).thenReturn(60);
    when(stage.getCamera()).thenReturn(new OrthographicCamera());
    when(stage.getWidth()).thenReturn(100f);
    when(stage.getHeight()).thenReturn(60f);
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(time);
    RenderService service = new RenderService();
    Renderer renderer =
        new Renderer(makeCameraEntity(camera), 10, spriteBatch, stage, service, debugRenderer);
    service.startWhiteFlash();

    try (MockedConstruction<Pixmap> pixmaps = mockConstruction(Pixmap.class);
        MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      renderer.render();

      Texture white = textures.constructed().get(0);
      InOrder order = inOrder(stage, spriteBatch);
      order.verify(stage).draw();
      order.verify(spriteBatch).begin();
      order.verify(spriteBatch).setColor(1f, 1f, 1f, 1f);
      order.verify(spriteBatch).draw(white, 0f, 0f, 100f, 60f);
      order.verify(spriteBatch).end();
      renderer.dispose();
      verify(white).dispose();
    }
  }

  private static CameraComponent makeCameraEntity(Camera camera) {
    Entity camEntity = new Entity().addComponent(new CameraComponent(camera));
    return camEntity.getComponent(CameraComponent.class);
  }
}
