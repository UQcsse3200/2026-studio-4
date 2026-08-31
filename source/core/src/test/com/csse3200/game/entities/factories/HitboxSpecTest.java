package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HitboxSpecTest {
  @Test
  void shouldDefaultLayerAndOffset() {
    HitboxSpec spec = new HitboxSpec();
    assertEquals(PhysicsLayer.WEAPON, spec.getLayer());
    assertEquals(PhysicsLayer.NPC, spec.getTargetLayer());
    assertEquals(new Vector2(), spec.getLocalOffset());
    assertNull(spec.getPosition());
    assertNull(spec.getSize());
    assertNull(spec.getOwner());
    assertEquals(0f, spec.getLifetime());
    assertEquals(0, spec.getDamage());
    assertEquals(0f, spec.getKnockback());
  }

  @Test
  void shouldCopyVectorsOnSetAndGet() {
    Vector2 position = new Vector2(1f, 2f);
    Vector2 size = new Vector2(0.4f, 0.8f);
    Vector2 offset = new Vector2(0.5f, 0f);
    HitboxSpec spec = new HitboxSpec().position(position).size(size).localOffset(offset);

    position.set(0f, 0f);
    size.set(0f, 0f);
    offset.set(0f, 0f);

    assertEquals(new Vector2(1f, 2f), spec.getPosition());
    assertEquals(new Vector2(0.4f, 0.8f), spec.getSize());
    assertEquals(new Vector2(0.5f, 0f), spec.getLocalOffset());

    spec.getPosition().set(9f, 9f);
    spec.getSize().set(9f, 9f);
    spec.getLocalOffset().set(9f, 9f);
    assertEquals(new Vector2(1f, 2f), spec.getPosition());
    assertEquals(new Vector2(0.4f, 0.8f), spec.getSize());
    assertEquals(new Vector2(0.5f, 0f), spec.getLocalOffset());
  }

  @Test
  void shouldAllowNullPositionSizeAndOffset() {
    HitboxSpec spec =
        new HitboxSpec()
            .position(new Vector2(1f, 1f))
            .size(new Vector2(1f, 1f))
            .localOffset(new Vector2(1f, 0f));
    spec.position(null).size(null).localOffset(null);
    assertNull(spec.getPosition());
    assertNull(spec.getSize());
    assertEquals(new Vector2(), spec.getLocalOffset());
  }

  @Test
  void shouldStoreOwnerLifetimeDamageAndKnockback() {
    Entity owner = new Entity();
    HitboxSpec spec =
        new HitboxSpec()
            .lifetime(0.2f)
            .layer(PhysicsLayer.OBSTACLE)
            .targetLayer(PhysicsLayer.PLAYER)
            .damage(12)
            .knockback(3f)
            .owner(owner);
    assertEquals(0.2f, spec.getLifetime());
    assertEquals(PhysicsLayer.OBSTACLE, spec.getLayer());
    assertEquals(PhysicsLayer.PLAYER, spec.getTargetLayer());
    assertEquals(12, spec.getDamage());
    assertEquals(3f, spec.getKnockback());
    assertSame(owner, spec.getOwner());

    spec.owner(null);
    assertNull(spec.getOwner());
  }
}
