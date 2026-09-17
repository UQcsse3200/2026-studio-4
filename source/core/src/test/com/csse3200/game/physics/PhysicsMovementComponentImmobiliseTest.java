package com.csse3200.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Every steered entity in the game, from a wandering enemy to a boss closing on the player, moves
 * through this component, so freezing it here is what actually pins them all.
 */
@ExtendWith(GameExtension.class)
class PhysicsMovementComponentImmobiliseTest {
  private GameTime time;
  private StatusEffectsControllerComponent effects;
  private PhysicsMovementComponent movement;
  private PhysicsComponent physics;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    effects = new StatusEffectsControllerComponent();
    movement = new PhysicsMovementComponent();
    physics = new PhysicsComponent();
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(effects)
            .addComponent(physics)
            .addComponent(movement);
    entity.create();
    movement.setMaxSpeed(new Vector2(3f, 3f));
    movement.setTarget(new Vector2(100f, 0f));
  }

  private float speed() {
    return physics.getBody().getLinearVelocity().len();
  }

  @Test
  void movesTowardsItsTargetNormally() {
    movement.update();

    assertTrue(speed() > 0f);
  }

  @Test
  void aFrozenEntityIsBroughtToAStandstill() {
    movement.update();
    assertTrue(speed() > 0f, "moving before the freeze lands");

    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    movement.update();

    assertEquals(0f, speed(), 1e-4f);
  }

  @Test
  void aFrozenEntityIsHeldStillEveryFrameRatherThanDriftingOn() {
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    for (int frame = 0; frame < 5; frame++) {
      movement.update();
      assertEquals(0f, speed(), 1e-4f, "frame " + frame);
    }
  }

  @Test
  void carriesOnTowardsTheSameTargetOnceItThaws() {
    // The target is left alone through the freeze, so a chasing enemy resumes its approach rather
    // than standing there waiting to be given a new one.
    effects.addStatusEffect(new FrozenEffect(time, 5000L));
    movement.update();

    when(time.getTime()).thenReturn(5000L);
    movement.update();

    assertEquals(new Vector2(100f, 0f), movement.getTarget());
    assertTrue(speed() > 0f);
  }

  @Test
  void anEntityWithNoStatusEffectsAtAllStillMoves() {
    Entity plain = new Entity();
    PhysicsMovementComponent plainMovement = new PhysicsMovementComponent();
    PhysicsComponent plainPhysics = new PhysicsComponent();
    plain.addComponent(plainPhysics).addComponent(plainMovement);
    plain.create();
    plainMovement.setMaxSpeed(new Vector2(3f, 3f));
    plainMovement.setTarget(new Vector2(100f, 0f));

    plainMovement.update();

    assertTrue(plainPhysics.getBody().getLinearVelocity().len() > 0f);
  }
}
