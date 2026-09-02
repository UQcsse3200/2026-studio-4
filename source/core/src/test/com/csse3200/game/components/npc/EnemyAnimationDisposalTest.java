package com.csse3200.game.components.npc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyAnimationDisposalTest {
  private EntityService entityService;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
  }

  @Test
  void shouldScheduleNormalEnemyDisposalAndDisposeAnimatorOnlyOnce() {
    AnimationRenderComponent animator = finishedAnimator();
    EnemyAnimationController controller = new EnemyAnimationController();
    Entity enemy = new Entity().addComponent(animator).addComponent(controller);
    enemy.create();

    enemy.getEvents().trigger("dieAnimation");
    controller.update();

    verify(entityService).scheduleDisposal(enemy);
    verify(animator, never()).dispose();

    enemy.dispose();
    verify(animator, times(1)).dispose();
  }

  @Test
  void shouldScheduleFloatingDemonDisposal() {
    AnimationRenderComponent animator = finishedAnimator();
    FloatingDemonAnimationController controller = new FloatingDemonAnimationController();
    Entity demon =
        new Entity()
            .addComponent(animator)
            .addComponent(mock(AITaskComponent.class))
            .addComponent(mock(PhysicsMovementComponent.class))
            .addComponent(controller);
    demon.create();

    demon.getEvents().trigger("dieAnimation");
    controller.update();

    verify(entityService).scheduleDisposal(demon);
  }

  private static AnimationRenderComponent finishedAnimator() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.isFinished()).thenReturn(true);
    return animator;
  }
}
