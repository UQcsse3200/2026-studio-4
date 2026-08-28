package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.weapons.FollowComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HitboxCommandTest {
  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  @Test
  void shouldSpawnFollowHitboxOnPlayer() {
    Entity player =
        new Entity()
            .addComponent(new PlayerActions())
            .addComponent(new CombatStatsComponent(100, 10));
    player.setPosition(2f, 3f);
    ServiceLocator.getEntityService().register(player);

    assertTrue(new HitboxCommand().action(new ArrayList<>()));

    Entity hitbox = ServiceLocator.getEntityService().findByComponent(FollowComponent.class);
    assertNotNull(hitbox);
    assertEquals(PhysicsLayer.WEAPON, hitbox.getComponent(HitboxComponent.class).getLayer());
    assertEquals(8, hitbox.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldFailWithoutPlayer() {
    assertFalse(new HitboxCommand().action(new ArrayList<>()));
  }
}
