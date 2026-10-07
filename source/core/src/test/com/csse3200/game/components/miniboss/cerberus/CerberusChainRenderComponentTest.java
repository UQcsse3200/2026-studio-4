package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CerberusChainRenderComponentTest {
  private RenderService renderService;
  private Entity body;
  private CerberusChainComponent chain;
  private CerberusChainRenderComponent renderer;

  @BeforeEach
  void setUp() {
    renderService = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(mock(EntityService.class));

    chain = new CerberusChainComponent(new Vector2(0f, 4f));
    renderer = new CerberusChainRenderComponent(mock(TextureRegion.class), 5f);

    body = new Entity().addComponent(chain).addComponent(renderer);
    body.setScale(2f, 2f);
    body.setPosition(1f, 0f);
    body.create();
  }

  private float lengthOf(Vector2[] points) {
    float length = 0f;
    for (int i = 1; i < points.length; i++) {
      length += points[i - 1].dst(points[i]);
    }
    return length;
  }

  @Test
  void shouldConnectWallAndBody() {
    Vector2[] points = renderer.getChainPoints();

    assertEquals(chain.getWallAnchor(), points[0]);
    assertEquals(chain.getBodyAttachment().x, points[points.length - 1].x, 0.001f);
    assertEquals(chain.getBodyAttachment().y, points[points.length - 1].y, 0.001f);
  }

  @Test
  void shouldKeepApproximateLengthWhenBodyMovesCloser() {
    assertEquals(5f, lengthOf(renderer.getChainPoints()), 0.01f);

    body.setPosition(0f, 1f);

    assertEquals(5f, lengthOf(renderer.getChainPoints()), 0.01f);
    assertEquals(new Vector2(0f, 4f), renderer.getChainPoints()[0]);
  }

  @Test
  void shouldDrawStraightIfEndpointsExceedConfiguredLength() {
    body.setPosition(10f, 0f);

    assertEquals(chain.getEndpointDistance(), lengthOf(renderer.getChainPoints()), 0.01f);
  }

  @Test
  void shouldUnregisterWhenBodyIsDisposed() {
    body.dispose();

    verify(renderService).register(renderer);
    verify(renderService).unregister(renderer);
  }

  @Test
  void shouldRequireChainComponentBeforeRegistering() {
    CerberusChainRenderComponent unbound =
        new CerberusChainRenderComponent(mock(TextureRegion.class), 5f);
    new Entity().addComponent(unbound);

    assertThrows(IllegalStateException.class, unbound::create);
  }
}
