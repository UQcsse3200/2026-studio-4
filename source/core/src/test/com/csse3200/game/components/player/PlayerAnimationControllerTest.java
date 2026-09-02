package com.csse3200.game.components.player;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.utils.math.Vector2Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerAnimationControllerTest {
  private AnimationRenderComponent animator;
  private PlayerAnimationController controller;
  private Entity player;

  @BeforeEach
  void setUp() {
    animator = mock(AnimationRenderComponent.class);
    controller = new PlayerAnimationController();
    player = new Entity().addComponent(animator).addComponent(controller);
    player.create();
  }

  @Test
  void shouldPlayAttackDown() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.DOWN);
    verify(animator).startAnimation("attack_down");
  }

  @Test
  void shouldPlayAttackUp() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.UP);
    verify(animator).startAnimation("attack_up");
  }

  @Test
  void shouldPlayAttackLeft() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.LEFT);
    verify(animator).startAnimation("attack_left");
  }

  @Test
  void shouldPlayAttackRight() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.RIGHT);
    verify(animator).startAnimation("attack_right");
  }

  @Test
  void shouldPreferVerticalFacingForDiagonalAttacks() {
    player.getEvents().trigger("weaponAttack", new Vector2(1f, -1f));
    verify(animator).startAnimation("attack_down");
  }

  @Test
  void shouldIgnoreIdleAndWalkEventsWhileAttacking() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.DOWN);
    when(animator.isFinished()).thenReturn(false);

    player.getEvents().trigger("idleLeft");
    player.getEvents().trigger("walkRight");

    verify(animator, never()).startAnimation("idle_left");
    verify(animator, never()).startAnimation("walk_right");
  }

  @Test
  void shouldNotRestartAttackWhileAlreadyAttacking() {
    player.getEvents().trigger("weaponAttack", Vector2Utils.RIGHT);
    player.getEvents().trigger("weaponAttack", Vector2Utils.UP);
    verify(animator, times(1)).startAnimation("attack_right");
    verify(animator, never()).startAnimation("attack_up");
  }

  @Test
  void shouldReturnToWalkWhenAttackFinishes() {
    player.getEvents().trigger("walkLeft");
    player.getEvents().trigger("weaponAttack", Vector2Utils.LEFT);
    when(animator.isFinished()).thenReturn(true);

    controller.update();

    verify(animator, times(2)).startAnimation("walk_left");
  }

  @Test
  void shouldReturnToIdleWhenAttackFinishes() {
    player.getEvents().trigger("idleUp");
    player.getEvents().trigger("weaponAttack", Vector2Utils.UP);
    when(animator.isFinished()).thenReturn(true);

    controller.update();

    verify(animator, times(2)).startAnimation("idle_up");
  }

  @Test
  void shouldPlayAttackDownWhenDirectionIsZero() {
    player.getEvents().trigger("weaponAttack", Vector2.Zero);
    verify(animator).startAnimation("attack_down");
  }
}
