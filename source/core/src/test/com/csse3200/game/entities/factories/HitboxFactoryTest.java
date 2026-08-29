package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.weapons.FollowComponent;
import com.csse3200.game.components.weapons.LifetimeComponent;
import com.csse3200.game.components.weapons.WeaponComponent;
import com.csse3200.game.components.weapons.WeaponStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HitboxFactoryTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldSpawnSensorOnWeaponLayer() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    hitbox.create();

    HitboxComponent hitboxComponent = hitbox.getComponent(HitboxComponent.class);
    assertNotNull(hitboxComponent);
    assertTrue(hitboxComponent.getFixture().isSensor());
    assertEquals(PhysicsLayer.WEAPON, hitboxComponent.getLayer());
    assertEquals(
        BodyType.KinematicBody, hitbox.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(new Vector2(1f, 2f), hitbox.getPosition());
    assertEquals(new Vector2(0.4f, 0.8f), hitbox.getScale());
    assertNotNull(hitbox.getComponent(LifetimeComponent.class));
    assertNotNull(hitbox.getComponent(TouchAttackComponent.class));
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldFollowWhenOwnerProvided() {
    Entity owner = new Entity();
    HitboxSpec spec = meleeSpec().owner(owner).localOffset(new Vector2(0.5f, 0f));
    Entity hitbox = HitboxFactory.createHitbox(spec);
    assertNotNull(hitbox.getComponent(FollowComponent.class));
  }

  @Test
  void shouldNotFollowWhenOwnerOmitted() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    assertNull(hitbox.getComponent(FollowComponent.class));
  }

  @Test
  void shouldDamageTargetOnCollision() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    hitbox.create();

    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC));
    target.create();

    Fixture hitboxFixture = hitbox.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    hitbox.getEvents().trigger("collisionStart", hitboxFixture, targetFixture);

    assertEquals(2, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldRejectNullSpec() {
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(null));
  }

  @Test
  void shouldRejectMissingPosition() {
    HitboxSpec spec = new HitboxSpec().size(new Vector2(0.4f, 0.8f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectMissingSize() {
    HitboxSpec spec = new HitboxSpec().position(new Vector2(1f, 2f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNonPositiveSize() {
    HitboxSpec zeroWidth = meleeSpec().size(new Vector2(0f, 1f));
    HitboxSpec zeroHeight = meleeSpec().size(new Vector2(1f, 0f));
    HitboxSpec negativeSize = meleeSpec().size(new Vector2(-0.1f, 1f));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(zeroWidth));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(zeroHeight));
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(negativeSize));
  }

  @Test
  void shouldRejectNegativeLifetime() {
    HitboxSpec spec = meleeSpec().lifetime(-0.1f);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNegativeDamage() {
    HitboxSpec spec = meleeSpec().damage(-1);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void shouldRejectNegativeKnockback() {
    HitboxSpec spec = meleeSpec().knockback(-0.5f);
    assertThrows(IllegalArgumentException.class, () -> HitboxFactory.createHitbox(spec));
  }

  @Test
  void stubWeaponShouldSpawnHitboxThroughFactory() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.2f, 8, 0f);
    FactoryWeapon weapon = new FactoryWeapon();
    Entity wielder = new Entity().addComponent(stats).addComponent(weapon);
    wielder.setPosition(0f, 0f);
    wielder.create();

    assertTrue(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertNotNull(weapon.spawned);
    assertNotNull(weapon.spawned.getComponent(HitboxComponent.class));
    assertFalse(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
  }

  @Test
  void shouldPreventInstantiation() throws Exception {
    var constructor = HitboxFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    Exception thrown =
        assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
    assertTrue(thrown.getCause() instanceof IllegalStateException);
  }

  private static HitboxSpec meleeSpec() {
    return new HitboxSpec()
        .position(new Vector2(1f, 2f))
        .size(new Vector2(0.4f, 0.8f))
        .lifetime(0.15f)
        .layer(PhysicsLayer.WEAPON)
        .targetLayer(PhysicsLayer.NPC)
        .damage(8)
        .knockback(1.5f);
  }

  private static class FactoryWeapon extends WeaponComponent {
    Entity spawned;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
      HitboxSpec spec =
          new HitboxSpec()
              .position(origin)
              .size(new Vector2(0.4f, 0.8f))
              .lifetime(0.15f)
              .layer(PhysicsLayer.WEAPON)
              .targetLayer(PhysicsLayer.NPC)
              .damage(stats.getDamage())
              .knockback(stats.getKnockback())
              .owner(entity)
              .localOffset(direction.cpy().nor().scl(0.5f));
      spawned = HitboxFactory.createHitbox(spec);
    }
  }
}
