package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.components.statuseffects.StatusEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The two questions the renderer, movement and AI ask the controller about the effects an entity is
 * carrying: can it act, and what should it look like.
 */
@ExtendWith(GameExtension.class)
class StatusEffectsControllerImmobiliseAndGlowTest {
  private GameTime time;
  private Entity entity;
  private StatusEffectsControllerComponent effects;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    effects = new StatusEffectsControllerComponent();
    entity = new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(effects);
    entity.create();
  }

  /** An effect that only glows, so glow combining can be tested apart from anything else. */
  private static StatusEffect glowing(Color glow, long remaining) {
    return new StatusEffect() {
      @Override
      public boolean update() {
        return false;
      }

      @Override
      public long getRemainingDuration() {
        return remaining;
      }

      @Override
      public Color getGlow() {
        return glow;
      }
    };
  }

  @Test
  void anEntityIsFreeToActUntilSomethingSaysOtherwise() {
    assertFalse(effects.isImmobilised());
    assertFalse(StatusEffectsControllerComponent.isImmobilised(entity));
  }

  @Test
  void anyEffectThatImmobilisesStopsTheWholeEntity() {
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    assertTrue(effects.isImmobilised());
    assertTrue(StatusEffectsControllerComponent.isImmobilised(entity));
  }

  @Test
  void anExpiredFreezeReleasesTheEntityImmediatelyWithoutWaitingForAnUpdate() {
    effects.addStatusEffect(new FrozenEffect(time, 5000L));

    when(time.getTime()).thenReturn(5000L);

    assertFalse(effects.isImmobilised(), "queries must skip an expired effect the instant it ends");
  }

  @Test
  void anEntityWithNoControllerAtAllIsNeverImmobilised() {
    assertFalse(StatusEffectsControllerComponent.isImmobilised(new Entity()));
  }

  @Test
  void aMissingEntityIsTreatedAsFreeToActRatherThanCrashing() {
    // Damage can arrive unattributed, e.g. from a spell with no caster entity behind it.
    assertFalse(StatusEffectsControllerComponent.isImmobilised(null));
    assertNull(StatusEffectsControllerComponent.getGlow(null));
  }

  @Test
  void nothingGlowsUntilAnEffectSaysItShould() {
    assertNull(effects.getGlow());
    assertNull(StatusEffectsControllerComponent.getGlow(entity));
  }

  @Test
  void reportsTheGlowOfASingleEffect() {
    effects.addStatusEffect(glowing(new Color(0.2f, 0.4f, 0.6f, 0.5f), 1000L));

    assertEquals(new Color(0.2f, 0.4f, 0.6f, 0.5f), effects.getGlow());
  }

  @Test
  void addsGlowsTogetherSoTwoAtOnceAreBrighterThanEither() {
    effects.addStatusEffect(glowing(new Color(0.2f, 0.1f, 0.0f, 0.3f), 1000L));
    effects.addStatusEffect(glowing(new Color(0.1f, 0.2f, 0.3f, 0.2f), 1000L));

    assertEquals(new Color(0.3f, 0.3f, 0.3f, 0.5f), effects.getGlow());
  }

  @Test
  void clampsACombinedGlowRatherThanOverflowingPastFullBrightness() {
    effects.addStatusEffect(glowing(new Color(0.9f, 0.9f, 0.9f, 0.9f), 1000L));
    effects.addStatusEffect(glowing(new Color(0.9f, 0.9f, 0.9f, 0.9f), 1000L));

    assertEquals(new Color(1f, 1f, 1f, 1f), effects.getGlow());
  }

  @Test
  void ignoresTheGlowOfAnExpiredEffect() {
    effects.addStatusEffect(glowing(new Color(1f, 0f, 0f, 1f), 0L));
    effects.addStatusEffect(glowing(new Color(0f, 0f, 1f, 0.5f), 1000L));

    assertEquals(new Color(0f, 0f, 1f, 0.5f), effects.getGlow());
  }

  @Test
  void handsBackACopySoARendererCannotCorruptTheEffectsOwnColour() {
    FrozenEffect frozen = new FrozenEffect(time, 5000L);
    effects.addStatusEffect(frozen);

    Color reported = effects.getGlow();
    reported.set(0f, 0f, 0f, 0f);

    assertEquals(frozen.getGlow(), effects.getGlow());
  }
}
