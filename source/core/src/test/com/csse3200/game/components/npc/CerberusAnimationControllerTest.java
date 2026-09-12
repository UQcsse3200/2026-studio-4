package com.csse3200.game.components.npc;

import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CerberusAnimationControllerTest {

  @Mock private AnimationRenderComponent animator;

  private Entity entity;
  private CerberusAnimationController controller;

  @BeforeEach
  void setUp() {
    entity = new Entity();
    controller = new CerberusAnimationController();

    entity.addComponent(animator);
    entity.addComponent(controller);

    entity.create();
  }

  @Test
  void shouldPlayMoveStart() {
    verify(animator).startAnimation("move");
  }

  @Test
  void shouldPlayAttackAnimation() {
    entity.getEvents().trigger("attackStart");

    verify(animator).startAnimation("lunge");
  }

  @Test
  void shouldPlayIdleOnEnrage() {
    entity.getEvents().trigger("enragePhaseStarted");

    verify(animator).startAnimation("idle");
  }

  @Test
  void shouldReturnToMoveAfterLunge() {
    when(animator.isFinished()).thenReturn(true);
    when(animator.getCurrentAnimation()).thenReturn("lunge");

    controller.update();

    verify(animator, times(2)).startAnimation("move");
  }

  @Test
  void shouldReturnToMoveAfterIdle() {
    when(animator.isFinished()).thenReturn(true);
    when(animator.getCurrentAnimation()).thenReturn("idle");

    controller.update();

    verify(animator, times(2)).startAnimation("move");
  }
}
