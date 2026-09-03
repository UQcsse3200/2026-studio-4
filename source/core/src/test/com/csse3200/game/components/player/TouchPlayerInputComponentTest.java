package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TouchPlayerInputComponentTest {
  private Entity player;
  private TouchPlayerInputComponent input;
  private int walkCount;
  private int walkStopCount;
  private int attackCount;
  private Vector2 lastWalkDirection;

  @BeforeEach
  void setUp() {
    player = new Entity();
    input = new TouchPlayerInputComponent();
    player.addComponent(input);
    player
        .getEvents()
        .addListener(
            "walk",
            (Vector2 direction) -> {
              walkCount++;
              lastWalkDirection = direction.cpy();
            });
    player.getEvents().addListener("walkStop", () -> walkStopCount++);
    player.getEvents().addListener("attack", () -> attackCount++);
  }

  @Test
  void shouldWalkOnArrowKeysAndStopWhenReleased() {
    assertTrue(input.keyDown(Input.Keys.RIGHT));
    assertEquals(1, walkCount);
    assertEquals(1f, lastWalkDirection.x, 0.001f);
    assertEquals(0f, lastWalkDirection.y, 0.001f);

    assertTrue(input.keyUp(Input.Keys.RIGHT));
    assertEquals(1, walkStopCount);
  }

  @Test
  void shouldWalkUpOnUpArrow() {
    assertTrue(input.keyDown(Input.Keys.UP));
    assertEquals(0f, lastWalkDirection.x, 0.001f);
    assertEquals(1f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldWalkLeftOnLeftArrow() {
    assertTrue(input.keyDown(Input.Keys.LEFT));
    assertEquals(-1f, lastWalkDirection.x, 0.001f);
  }

  @Test
  void shouldWalkDownOnDownArrow() {
    assertTrue(input.keyDown(Input.Keys.DOWN));
    assertEquals(-1f, lastWalkDirection.y, 0.001f);
  }

  @Test
  void shouldAttackOnTouchDown() {
    assertTrue(input.touchDown(10, 20, 0, 0));
    assertEquals(1, attackCount);
  }

  @Test
  void shouldIgnoreUnboundKeys() {
    assertFalse(input.keyDown(Input.Keys.W));
    assertEquals(0, walkCount);
  }
}
