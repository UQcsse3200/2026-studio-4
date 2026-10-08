package com.csse3200.game.components.miniboss.cerberus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class CerberusChainComponentTest {

  @Test
  void shouldFollowBodyWhileKeepingWallAnchorFixed() {
    CerberusChainComponent chain = new CerberusChainComponent(new Vector2(10f, 10f));

    Entity body = new Entity().addComponent(chain);
    body.setScale(2f, 2f);
    body.setPosition(5f, 7f);

    assertEquals(6f, chain.getBodyAttachment().x, 0.001f);
    assertEquals(7.8f, chain.getBodyAttachment().y, 0.001f);

    body.setPosition(6f, 8f);

    assertEquals(7f, chain.getBodyAttachment().x, 0.001f);
    assertEquals(8.8f, chain.getBodyAttachment().y, 0.001f);
    assertEquals(new Vector2(10f, 10f), chain.getWallAnchor());
  }

  @Test
  void shouldProtectAnchorFromExternalChanges() {
    Vector2 suppliedAnchor = new Vector2(10f, 10f);
    CerberusChainComponent chain = new CerberusChainComponent(suppliedAnchor);

    suppliedAnchor.setZero();
    chain.getWallAnchor().setZero();

    assertEquals(new Vector2(10f, 10f), chain.getWallAnchor());
  }

  @Test
  void shouldMeasureDistanceBetweenEndpoints() {
    CerberusChainComponent chain = new CerberusChainComponent(new Vector2(1f, 3.8f));

    Entity body = new Entity().addComponent(chain);
    body.setScale(2f, 2f);
    body.setPosition(0f, 0f);

    assertEquals(3f, chain.getEndpointDistance(), 0.001f);
  }

  @Test
  void shouldRejectMissingAnchor() {
    assertThrows(IllegalArgumentException.class, () -> new CerberusChainComponent(null));
  }
}
