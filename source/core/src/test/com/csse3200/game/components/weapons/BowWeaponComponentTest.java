package com.csse3200.game.components.weapons;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BowWeaponComponentTest {
  private EntityService entityService;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    entityService = spy(new EntityService());
    ServiceLocator.registerEntityService(entityService);
  }

  @Test
  void shouldSpawnTravellingHitboxOnAttack() {
    BowWeaponComponent bow = new BowWeaponComponent();
    Entity wielder =
        new Entity().addComponent(new WeaponStatsComponent(0.5f, 10, 0f)).addComponent(bow);
    wielder.create();

    assertTrue(bow.attack(new Vector2(0f, 0f), new Vector2(1f, 0f)));

    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    Entity arrow = captor.getValue();

    assertNotNull(arrow.getComponent(HitboxComponent.class));
    assertNotNull(arrow.getComponent(ProjectileComponent.class));
    assertNull(arrow.getComponent(FollowComponent.class));
  }
}
