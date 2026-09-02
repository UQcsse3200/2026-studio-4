package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerActionsTest {
  private AnimationRenderComponent animator;
  private Entity player;

  @BeforeEach
  void setUp() {
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    when(body.getMass()).thenReturn(1f);
    when(body.getWorldCenter()).thenReturn(new Vector2());

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("sounds/Impact4.ogg", Sound.class)).thenReturn(mock(Sound.class));
    ServiceLocator.registerResourceService(resources);

    animator = mock(AnimationRenderComponent.class);
    player =
        new Entity()
            .addComponent(physics)
            .addComponent(new CombatStatsComponent(100, 10, 3f, 1f))
            .addComponent(new PlayerActions())
            .addComponent(animator)
            .addComponent(new PlayerAnimationController());
    player.create();
  }

  @Test
  void shouldWalkInEachDirection() {
    walk(Vector2Utils.DOWN);
    verify(animator).startAnimation("walk_down");

    walk(Vector2Utils.UP);
    verify(animator).startAnimation("walk_up");

    walk(Vector2Utils.LEFT);
    verify(animator).startAnimation("walk_left");

    walk(Vector2Utils.RIGHT);
    verify(animator).startAnimation("walk_right");
  }

  @Test
  void shouldIdleFacingLastWalkDirectionWhenStopped() {
    walkAndStop(Vector2Utils.DOWN);
    verify(animator).startAnimation("idle_down");

    walkAndStop(Vector2Utils.UP);
    verify(animator).startAnimation("idle_up");

    walkAndStop(Vector2Utils.LEFT);
    verify(animator).startAnimation("idle_left");

    walkAndStop(Vector2Utils.RIGHT);
    verify(animator).startAnimation("idle_right");
  }

  @Test
  void shouldPreferVerticalWalkOnDiagonals() {
    walk(new Vector2(1f, -1f));
    verify(animator).startAnimation("walk_down");
    verify(animator, never()).startAnimation("walk_right");
  }

  @Test
  void shouldNotRestartWalkWhenAlreadyWalkingThatWay() {
    walk(Vector2Utils.DOWN);
    walk(Vector2Utils.DOWN);
    verify(animator, times(1)).startAnimation("walk_down");
  }

  @Test
  void shouldAttackDownByDefault() {
    Vector2[] facing = {null};
    player.getEvents().addListener("weaponAttack", (Vector2 direction) -> facing[0] = direction);

    player.getEvents().trigger("attack");

    assertEquals(0f, facing[0].x, 0.001f);
    assertEquals(-1f, facing[0].y, 0.001f);
    verify(animator).startAnimation("attack_down");
  }

  @Test
  void shouldAttackInLastWalkFacing() {
    walk(Vector2Utils.LEFT);
    player.getEvents().trigger("walkStop");
    player.update();

    player.getEvents().trigger("attack");

    verify(animator).startAnimation("attack_left");
  }

  private void walk(Vector2 direction) {
    player.getEvents().trigger("walk", direction.cpy());
    player.update();
  }

  private void walkAndStop(Vector2 direction) {
    walk(direction);
    player.getEvents().trigger("walkStop");
    player.update();
  }
}
