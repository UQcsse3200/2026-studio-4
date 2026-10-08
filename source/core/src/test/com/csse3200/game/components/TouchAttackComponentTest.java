package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.statuseffects.StatusEffectsFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TouchAttackComponentTest {
  private GameTime time;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
  }

  @Test
  void repeatsOnlyAfterIntervalAndStopsOnExit() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    contact(attacker, target, "collisionStart");
    assertEquals(90, health(target));
    tick(attacker, 0.5f);
    assertEquals(90, health(target));
    tick(attacker, 0.5f);
    assertEquals(80, health(target));
    contact(attacker, target, "collisionEnd");
    tick(attacker, 1f);
    assertEquals(80, health(target));
    contact(attacker, target, "collisionStart");
    assertEquals(70, health(target));
  }

  @Test
  void existingConstructorsRemainEntryOnly() {
    for (TouchAttackComponent attack :
        new TouchAttackComponent[] {
          new TouchAttackComponent(PhysicsLayer.PLAYER),
          new TouchAttackComponent(PhysicsLayer.PLAYER, 0f)
        }) {
      Entity attacker = attackerWith(attack);
      Entity target = durableTarget();
      contact(attacker, target, "collisionStart");
      tick(attacker, 2f);
      assertEquals(90, health(target));
    }
  }

  @Test
  void multipleFixturesShareOneCooldownUntilLastExit() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    Fixture me = attacker.getComponent(HitboxComponent.class).getFixture();
    Fixture extra = target.getComponent(ColliderComponent.class).getFixture();
    contact(attacker, target, "collisionStart");
    contact(attacker, target, "collisionStart");
    attacker.getEvents().trigger("collisionStart", me, extra);
    assertEquals(90, health(target));
    contact(attacker, target, "collisionEnd");
    tick(attacker, 1f);
    assertEquals(80, health(target));
    attacker.getEvents().trigger("collisionEnd", me, extra);
    tick(attacker, 1f);
    assertEquals(80, health(target));
  }

  @Test
  void targetsHaveIndependentCooldownsAndLongFramesDoNotBurst() {
    Entity attacker = repeatingAttacker();
    Entity first = durableTarget();
    Entity second = durableTarget();
    contact(attacker, first, "collisionStart");
    tick(attacker, 0.5f);
    contact(attacker, second, "collisionStart");
    tick(attacker, 0.5f);
    assertEquals(80, health(first));
    assertEquals(90, health(second));
    tick(attacker, 10f);
    assertEquals(70, health(first));
    assertEquals(80, health(second));
  }

  @Test
  void frozenTargetStillTakesRepeatedDamage() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    target
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000));
    contact(attacker, target, "collisionStart");
    tick(attacker, 1f);
    assertEquals(80, health(target));
  }

  @Test
  void frozenAttackerResumesAfterThawingWithoutNewContact() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    StatusEffectsControllerComponent effects =
        attacker.getComponent(StatusEffectsControllerComponent.class);
    effects.addStatusEffect(StatusEffectsFactory.createFrozen(time, 5000));
    contact(attacker, target, "collisionStart");
    tick(attacker, 1f);
    assertEquals(100, health(target));
    effects.clearStatusEffects();
    tick(attacker, 1f);
    assertEquals(90, health(target));
  }

  @Test
  void concealedTargetCanBeHitAfterRevealWithoutReentering() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    StatusEffectsControllerComponent effects =
        target.getComponent(StatusEffectsControllerComponent.class);
    effects.addStatusEffect(StatusEffectsFactory.createInvisibility(time, 5000));
    contact(attacker, target, "collisionStart");
    tick(attacker, 1f);
    assertEquals(100, health(target));
    effects.clearStatusEffects();
    tick(attacker, 1f);
    assertEquals(90, health(target));
  }

  @Test
  void deadOrDisposedAttackerStopsRepeating() {
    Entity attacker = repeatingAttacker();
    Entity target = durableTarget();
    contact(attacker, target, "collisionStart");
    attacker.getComponent(CombatStatsComponent.class).setHealth(0);
    tick(attacker, 1f);
    assertEquals(90, health(target));
    attacker.getComponent(CombatStatsComponent.class).setHealth(100);
    attacker.getComponent(TouchAttackComponent.class).dispose();
    tick(attacker, 1f);
    contact(attacker, target, "collisionStart");
    assertEquals(90, health(target));
  }

  @Test
  void rejectsInvalidRepeatIntervals() {
    for (float interval : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TouchAttackComponent(PhysicsLayer.PLAYER, 0f, interval));
    }
  }

  private Entity repeatingAttacker() {
    return attackerWith(new TouchAttackComponent(PhysicsLayer.PLAYER, 0f, 1f));
  }

  private Entity attackerWith(TouchAttackComponent attack) {
    Entity attacker =
        new Entity()
            .addComponent(attack)
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    attacker.create();
    return attacker;
  }

  private Entity durableTarget() {
    Entity target =
        new Entity()
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    target.create();
    return target;
  }

  private int health(Entity target) {
    return target.getComponent(CombatStatsComponent.class).getHealth();
  }

  private void contact(Entity attacker, Entity target, String event) {
    attacker
        .getEvents()
        .trigger(
            event,
            attacker.getComponent(HitboxComponent.class).getFixture(),
            target.getComponent(HitboxComponent.class).getFixture());
  }

  private void tick(Entity attacker, float delta) {
    when(time.getDeltaTime()).thenReturn(delta);
    attacker.getComponent(TouchAttackComponent.class).update();
  }

  @Test
  void shouldAttack() {
    short targetLayer = (1 << 3);
    Entity entity = createAttacker(targetLayer);
    Entity target = createTarget(targetLayer);

    Fixture entityFixture = entity.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    entity.getEvents().trigger("collisionStart", entityFixture, targetFixture);

    assertEquals(0, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotAttackOtherLayer() {
    short targetLayer = (1 << 3);
    short attackLayer = (1 << 4);
    Entity entity = createAttacker(attackLayer);
    Entity target = createTarget(targetLayer);

    Fixture entityFixture = entity.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    entity.getEvents().trigger("collisionStart", entityFixture, targetFixture);

    assertEquals(10, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotAttackWithoutCombatComponent() {
    short targetLayer = (1 << 3);
    Entity entity = createAttacker(targetLayer);
    // Target does not have a combat component
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(targetLayer));
    target.create();

    Fixture entityFixture = entity.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();

    // This should not cause an exception, but the attack should be ignored
    entity.getEvents().trigger("collisionStart", entityFixture, targetFixture);
  }

  Entity createAttacker(short targetLayer) {
    Entity entity =
        new Entity()
            .addComponent(new TouchAttackComponent(targetLayer))
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    entity.create();
    return entity;
  }

  Entity createTarget(short layer) {
    return createTarget(layer, 10);
  }

  Entity createTarget(short layer, int health) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    target.create();
    return target;
  }
}
