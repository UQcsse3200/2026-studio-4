package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.weapons.FollowComponent;
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
  void shouldSpawnSensorAndDamageNpc() {
    Entity hitbox = HitboxFactory.createHitbox(meleeSpec());
    hitbox.create();

    assertTrue(hitbox.getComponent(HitboxComponent.class).getFixture().isSensor());
    assertEquals(PhysicsLayer.WEAPON, hitbox.getComponent(HitboxComponent.class).getLayer());
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());

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
  void shouldFollowOnlyWhenOwnerSet() {
    Entity withOwner = HitboxFactory.createHitbox(meleeSpec());
    assertNull(withOwner.getComponent(FollowComponent.class));

    HitboxSpec spec = meleeSpec();
    spec.owner = new Entity();
    assertNotNull(HitboxFactory.createHitbox(spec).getComponent(FollowComponent.class));
  }

  @Test
  void stubWeaponShouldSpawnScaledDamage() {
    WeaponStatsComponent stats = new WeaponStatsComponent(0.2f, 0.8f, 0f);
    FactoryWeapon weapon = new FactoryWeapon();
    Entity wielder =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(stats)
            .addComponent(weapon);
    wielder.create();

    assertTrue(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
    assertEquals(8, weapon.spawned.getComponent(CombatStatsComponent.class).getBaseAttack());
    assertFalse(weapon.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));
  }

  private static HitboxSpec meleeSpec() {
    HitboxSpec spec = new HitboxSpec();
    spec.position = new Vector2(1f, 2f);
    spec.size = new Vector2(0.4f, 0.8f);
    spec.lifetime = 0.15f;
    spec.damage = 8;
    spec.knockback = 1.5f;
    return spec;
  }

  private static class FactoryWeapon extends WeaponComponent {
    Entity spawned;

    @Override
    protected void createAttack(Vector2 origin, Vector2 direction) {
      HitboxSpec spec = new HitboxSpec();
      spec.position = origin;
      spec.size = new Vector2(0.4f, 0.8f);
      spec.lifetime = 0.15f;
      spec.damage = resolveHitboxDamage();
      spec.knockback = entity.getComponent(WeaponStatsComponent.class).getKnockback();
      spec.owner = entity;
      spec.localOffset = direction.cpy().nor().scl(0.5f);
      spawned = HitboxFactory.createHitbox(spec);
    }
  }
}
