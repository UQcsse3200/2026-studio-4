package com.csse3200.game.components.npc;

import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CerberusAnimationControllerTest {

  @Mock private AnimationRenderComponent animator;
  @Mock private EntityService entityService;

  private Entity entity;
  private CerberusAnimationController controller;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(entityService);

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

  //  @Test
  //  void shouldPlayAttackAnimation() {
  //    entity.getEvents().trigger("attackStart");
  //    verify(animator).startAnimation("attack");
  //  }
  //
  //  @Test
  //  void shouldPlayRoarOnEnrage() {
  //    entity.getEvents().trigger("enragePhaseStarted");
  //    verify(animator).startAnimation("roar");
  //  }

  @Test
  void shouldHandleDeathAndDisposal() {
    entity.getEvents().trigger("dieAnimation");
    verify(animator).startAnimation("dieAnimation");

    when(animator.isFinished()).thenReturn(false);
    controller.update();
    verify(entityService, never()).scheduleDisposal(entity);

    when(animator.isFinished()).thenReturn(true);
    controller.update();
    verify(entityService).scheduleDisposal(entity);
  }
}
