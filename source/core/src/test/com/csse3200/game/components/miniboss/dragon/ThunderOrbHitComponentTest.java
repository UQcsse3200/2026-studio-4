package com.csse3200.game.components.miniboss.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThunderOrbHitComponentTest {
  private Entity orb;
  private CombatStatsComponent playerStats;
  private ThunderOrbMovementComponent movement;
  private ThunderOrbHitComponent hit;
  private AnimationRenderComponent animator;
  private EntityService entities;
  private Fixture orbFixture;
  private Fixture playerFixture;

  @BeforeEach
  void setUp() {
    entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);

    playerStats = new CombatStatsComponent(100, 0);
    Entity player = new Entity().addComponent(playerStats);
    player.setPosition(10f, 0f);

    orbFixture = mock(Fixture.class);
    playerFixture = createFixture(player, PhysicsLayer.PLAYER);

    HitboxComponent hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(orbFixture);
    when(hitbox.getLayer()).thenReturn(PhysicsLayer.NPC);

    animator = mock(AnimationRenderComponent.class);
    when(animator.hasAnimation(ThunderOrbHitComponent.IMPACT_ANIMATION)).thenReturn(true);

    movement = new ThunderOrbMovementComponent(player, 2f, 90f, 3f);
    hit = new ThunderOrbHitComponent(10);

    orb =
        new Entity()
            .addComponent(hitbox)
            .addComponent(movement)
            .addComponent(animator)
            .addComponent(hit);
    movement.create();
    hit.create();
  }

  private Fixture createFixture(Entity entity, short layer) {
    Fixture fixture = mock(Fixture.class);
    Body body = mock(Body.class);
    Filter filter = new Filter();
    filter.categoryBits = layer;

    BodyUserData data = new BodyUserData();
    data.entity = entity;

    when(fixture.getFilterData()).thenReturn(filter);
    when(fixture.getBody()).thenReturn(body);
    when(body.getUserData()).thenReturn(data);
    return fixture;
  }

  private void collide() {
    orb.getEvents().trigger("collisionStart", orbFixture, playerFixture);
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  @Test
  void shouldDamageOnlyOnceAndStopMovement() {
    collide();
    collide();

    assertEquals(90, playerStats.getHealth());
    assertTrue(movement.isStopped());
    verify(animator, times(1)).startAnimation(ThunderOrbHitComponent.IMPACT_ANIMATION);
    verify(entities, never()).scheduleDisposal(orb);
  }

  @Test
  void shouldRemoveAfterImpactPlaybackOnlyOnce() {
    collide();

    hit.update(0.25f);
    verify(entities, never()).scheduleDisposal(orb);

    hit.update(0.27f);
    hit.update(1f);

    verify(entities, times(1)).scheduleDisposal(orb);
  }

  @Test
  void shouldRemoveOnExpiryWithoutDamage() {
    movement.update(3f);
    collide();
    hit.cancel();

    assertEquals(100, playerStats.getHealth());
    verify(entities, times(1)).scheduleDisposal(orb);
    verify(animator, never()).startAnimation(anyString());
  }

  @Test
  void shouldIgnoreOtherLayersAndOtherOwnFixtures() {
    Fixture enemyFixture = createFixture(new Entity(), PhysicsLayer.NPC);

    orb.getEvents().trigger("collisionStart", orbFixture, enemyFixture);
    orb.getEvents().trigger("collisionStart", mock(Fixture.class), playerFixture);

    assertEquals(100, playerStats.getHealth());
    verifyNoInteractions(entities);
    collide();
    assertEquals(90, playerStats.getHealth());
  }

  @Test
  void shouldCancelWithoutDamageOrRepeatedRemoval() {
    hit.cancel();
    hit.cancel();
    collide();

    assertTrue(movement.isStopped());
    assertEquals(100, playerStats.getHealth());
    verify(entities, times(1)).scheduleDisposal(orb);
  }

  @Test
  void shouldConsumeOrbEvenWhenPlayerIsInvulnerable() {
    playerStats.setInvulnerable(true);

    collide();
    playerStats.setInvulnerable(false);
    collide();

    assertEquals(100, playerStats.getHealth());
    assertTrue(movement.isStopped());
    verify(animator, times(1)).startAnimation(ThunderOrbHitComponent.IMPACT_ANIMATION);
  }

  @Test
  void shouldRemoveImmediatelyWhenImpactAnimationIsUnavailable() {
    when(animator.hasAnimation(ThunderOrbHitComponent.IMPACT_ANIMATION)).thenReturn(false);

    collide();

    assertEquals(90, playerStats.getHealth());
    verify(entities, times(1)).scheduleDisposal(orb);
  }

  @Test
  void shouldNotInterruptImpactWhenExpiryEventArrives() {
    collide();
    orb.getEvents().trigger(ThunderOrbMovementComponent.EXPIRED);

    verify(entities, never()).scheduleDisposal(orb);

    hit.update(0.6f);
    verify(entities, times(1)).scheduleDisposal(orb);
  }
}
