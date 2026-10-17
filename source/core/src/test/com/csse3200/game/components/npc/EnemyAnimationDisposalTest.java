package com.csse3200.game.components.npc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
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
    EnemyAnimationController controller = new EnemyAnimationController();
    Entity demon =
        new Entity()
            .addComponent(animator)
            .addComponent(mock(AITaskComponent.class))
            .addComponent(mock(PhysicsMovementComponent.class))
            .addComponent(controller);
    demon.create();

    demon.getEvents().trigger("enemyDeathAnimation");
    controller.update();

    verify(entityService).scheduleDisposal(demon);
  }

  @Test
  void shouldFaceHorizontalMovementDuringChase() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f), new Vector2(1f, 0f));

    Entity enemy =
        new Entity()
            .addComponent(animator)
            .addComponent(physics)
            .addComponent(new EnemyAnimationController());
    enemy.create();

    enemy.getEvents().trigger("chaseStart");
    enemy.getComponent(EnemyAnimationController.class).update();
    enemy.getComponent(EnemyAnimationController.class).update();

    verify(animator).setFlipX(true);
    verify(animator).setFlipX(false);
  }

  @Test
  void shouldRotateDeathFramesByThirtyDegreesUpToNinety() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.getCurrentFrameIndex()).thenReturn(0, 1, 2, 3, 4);
    EnemyAnimationController controller = new EnemyAnimationController();
    controller.setDeathRotation(30f, 90f);
    Entity enemy = new Entity().addComponent(animator).addComponent(controller);
    enemy.create();

    enemy.getEvents().trigger("dieAnimation");
    for (int frame = 0; frame < 5; frame++) {
      controller.update();
    }

    verify(animator).setRotation(0f);
    verify(animator).setRotation(30f);
    verify(animator).setRotation(60f);
    verify(animator, times(2)).setRotation(90f);
  }

  private static AnimationRenderComponent finishedAnimator() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.isFinished()).thenReturn(true);
    return animator;
  }
}
