package com.csse3200.game.components.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TrainingDummyComponentTest {
  private static final int MAX_HEALTH = 9999;
  private final List<String> hits = new ArrayList<>();
  private CombatStatsComponent stats;
  private TrainingDummyComponent dummy;
  private Entity entity;

  @BeforeEach
  void setUp() {
    stats = new CombatStatsComponent(MAX_HEALTH, 0);
    dummy = new TrainingDummyComponent("Training Dummy");
    entity = new Entity().addComponent(stats).addComponent(dummy);
    entity.create();
    entity
        .getEvents()
        .<String, Integer, Integer>addListener(
            TrainingDummyComponent.HIT,
            (label, damage, total) -> hits.add(label + ":" + damage + ":" + total));
  }

  @Test
  void shouldBeToppedUpAfterEveryHit() {
    stats.takeDamage(25);

    assertEquals(MAX_HEALTH, stats.getHealth());
    assertFalse(stats.isDead());
  }

  @Test
  void shouldReportEachHitWithItsDamageAndRunningTotal() {
    stats.takeDamage(25);
    stats.takeDamage(40);

    assertEquals(List.of("Training Dummy:25:1", "Training Dummy:40:2"), hits);
    assertEquals(2, dummy.getHits());
  }

  @Test
  void shouldNeverDieNoMatterHowHardItIsHit() {
    boolean[] died = {false};
    entity.getEvents().addListener("entityDied", () -> died[0] = true);

    for (int i = 0; i < 5; i++) {
      stats.takeDamage(MAX_HEALTH - 1);
    }

    assertFalse(died[0]);
    assertEquals(MAX_HEALTH, stats.getHealth());
  }

  @Test
  void shouldNotCountZeroDamage() {
    stats.takeDamage(0);

    assertEquals(0, dummy.getHits());
    assertEquals(List.of(), hits);
  }

  @Test
  void shouldBeHeldStillEveryFrame() {
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);
    Entity held =
        new Entity()
            .addComponent(physics)
            .addComponent(new CombatStatsComponent(MAX_HEALTH, 0))
            .addComponent(new TrainingDummyComponent("Test Dummy"));
    held.create();

    held.update();

    // A dummy is a dynamic body so weapons can hit it, which means knockback would slide it.
    verify(body).setLinearVelocity(0f, 0f);
  }
}
