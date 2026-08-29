package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileMovementComponentTest {
  @Test
  void shouldMoveInGivenDirection() {
    ProjectileMovementComponent movement = new ProjectileMovementComponent(new Vector2(1f, 0f), 5f);
    Entity projectile = new Entity().addComponent(movement);
    projectile.setPosition(2f, 3f);

    movement.update(0.5f);

    assertEquals(4.5f, projectile.getPosition().x);
    assertEquals(3f, projectile.getPosition().y);
  }
}
