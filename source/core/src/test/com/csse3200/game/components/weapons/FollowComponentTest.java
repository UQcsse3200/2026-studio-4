package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class FollowComponentTest {
  @Test
  void shouldTrackOwnerPlusOffset() {
    Entity owner = new Entity();
    owner.setPosition(1f, 2f);

    Entity follower =
        new Entity().addComponent(new FollowComponent(owner, new Vector2(0.5f, -0.25f)));
    follower.create();
    follower.update();

    assertEquals(1.5f, follower.getPosition().x, 1e-4f);
    assertEquals(1.75f, follower.getPosition().y, 1e-4f);

    owner.setPosition(3f, 4f);
    follower.update();
    assertEquals(3.5f, follower.getPosition().x, 1e-4f);
    assertEquals(3.75f, follower.getPosition().y, 1e-4f);
  }
}
