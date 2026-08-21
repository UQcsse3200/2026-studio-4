package com.csse3200.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class PhysicsLayerTest {
  @Test
  void weaponLayerShouldBeDistinctFromPlayerAndNpc() {
    assertEquals(1 << 4, PhysicsLayer.WEAPON);
    assertFalse(PhysicsLayer.contains(PhysicsLayer.PLAYER, PhysicsLayer.WEAPON));
    assertFalse(PhysicsLayer.contains(PhysicsLayer.NPC, PhysicsLayer.WEAPON));
    assertTrue(PhysicsLayer.contains(PhysicsLayer.ALL, PhysicsLayer.WEAPON));
    assertTrue(PhysicsLayer.contains(PhysicsLayer.WEAPON, PhysicsLayer.WEAPON));
  }

  @Test
  void shouldPreventInstantiation() throws Exception {
    var constructor = PhysicsLayer.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    Exception thrown = assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(thrown.getCause() instanceof IllegalStateException);
  }
}
