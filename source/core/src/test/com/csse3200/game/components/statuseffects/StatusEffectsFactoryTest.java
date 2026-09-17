package com.csse3200.game.components.statuseffects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class StatusEffectsFactoryTest {
  @Test
  void shouldCreateEachConfiguredEffect() {
    CombatStatsComponent stats = new CombatStatsComponent(100, 10, 4f, 1f);
    GameTime time = Mockito.mock(GameTime.class);
    Mockito.when(time.getTime()).thenReturn(0L);

    assertInstanceOf(Burning.class, StatusEffectsFactory.createBurn(stats));
    assertInstanceOf(Regeneration.class, StatusEffectsFactory.createRegeneration(stats));
    assertInstanceOf(Slow.class, StatusEffectsFactory.createSlow(stats));
    assertInstanceOf(Speed.class, StatusEffectsFactory.createSpeed(stats));
    assertInstanceOf(Vulnerable.class, StatusEffectsFactory.createVulnerable());
    assertInstanceOf(Slow.class, StatusEffectsFactory.createFreeze(stats));
    assertInstanceOf(Shield.class, StatusEffectsFactory.createShield());
    assertInstanceOf(InvisibilityEffect.class, StatusEffectsFactory.createInvisibility(time, 100));
    assertInstanceOf(LastStandEffect.class, StatusEffectsFactory.createLastStand(time, 100));
  }

  @Test
  void shouldExposeAllScalableStats() {
    assertEquals(3, Stat.values().length);
    assertEquals(Stat.ATTACK, Stat.valueOf("ATTACK"));
    assertEquals(Stat.MOVEMENT_SPEED, Stat.valueOf("MOVEMENT_SPEED"));
    assertEquals(Stat.ATTACK_SPEED, Stat.valueOf("ATTACK_SPEED"));
  }

  @Test
  void factoryClassShouldNotBeConstructable() throws Exception {
    Constructor<StatusEffectsFactory> constructor =
        StatusEffectsFactory.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    Exception thrown = assertThrows(Exception.class, constructor::newInstance);
    assertTrue(thrown.getCause() instanceof IllegalStateException);
  }
}
