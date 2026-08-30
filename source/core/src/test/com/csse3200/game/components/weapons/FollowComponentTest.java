package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    Vector2 offset = new Vector2(0.5f, -0.25f);
    Entity follower = new Entity().addComponent(new FollowComponent(owner, offset));
    follower.create();
    follower.update();

    assertEquals(1.5f, follower.getPosition().x, 1e-4f);
    assertEquals(1.75f, follower.getPosition().y, 1e-4f);

    owner.setPosition(3f, 4f);
    follower.update();
    assertEquals(3.5f, follower.getPosition().x, 1e-4f);
    assertEquals(3.75f, follower.getPosition().y, 1e-4f);
  }

  @Test
  void shouldHoldOffsetWhenOwnerDoesNotMove() {
    Entity owner = new Entity();
    owner.setPosition(2f, 2f);

    Entity follower = new Entity().addComponent(new FollowComponent(owner, new Vector2(1f, 0f)));
    follower.create();
    follower.update();
    follower.update();

    assertEquals(3f, follower.getPosition().x, 1e-4f);
    assertEquals(2f, follower.getPosition().y, 1e-4f);
  }

  @Test
  void shouldAllowOffsetMutationForSweeps() {
    Entity owner = new Entity();
    owner.setPosition(0f, 0f);

    FollowComponent follow = new FollowComponent(owner, new Vector2(1f, 0f));
    Entity follower = new Entity().addComponent(follow);
    follower.create();
    follower.update();
    assertEquals(1f, follower.getPosition().x, 1e-4f);

    follow.setLocalOffset(new Vector2(0f, 1f));
    follower.update();
    assertEquals(0f, follower.getPosition().x, 1e-4f);
    assertEquals(1f, follower.getPosition().y, 1e-4f);
  }

  @Test
  void shouldCopyOffsetSoCallerMutationDoesNotMoveFollower() {
    Entity owner = new Entity();
    owner.setPosition(0f, 0f);
    Vector2 offset = new Vector2(1f, 0f);
    FollowComponent follow = new FollowComponent(owner, offset);
    Entity follower = new Entity().addComponent(follow);
    follower.create();

    offset.set(99f, 99f);
    follower.update();
    assertEquals(1f, follower.getPosition().x, 1e-4f);
    assertEquals(0f, follower.getPosition().y, 1e-4f);
  }

  @Test
  void shouldExposeOwnerAndOffsetCopies() {
    Entity owner = new Entity();
    FollowComponent follow = new FollowComponent(owner, new Vector2(2f, 3f));
    assertSame(owner, follow.getOwner());
    Vector2 copy = follow.getLocalOffset();
    copy.set(0f, 0f);
    assertEquals(2f, follow.getLocalOffset().x, 1e-4f);
    assertEquals(3f, follow.getLocalOffset().y, 1e-4f);
  }

  @Test
  void shouldRejectNullOwner() {
    Vector2 offset = new Vector2(1f, 0f);
    assertThrows(IllegalArgumentException.class, () -> new FollowComponent(null, offset));
  }

  @Test
  void shouldRejectNullOffset() {
    Entity owner = new Entity();
    assertThrows(IllegalArgumentException.class, () -> new FollowComponent(owner, null));
  }

  @Test
  void shouldRejectNullSetLocalOffset() {
    FollowComponent follow = new FollowComponent(new Entity(), new Vector2(1f, 0f));
    assertThrows(IllegalArgumentException.class, () -> follow.setLocalOffset(null));
  }
}
