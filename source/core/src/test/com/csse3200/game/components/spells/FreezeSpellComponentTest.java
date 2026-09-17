package com.csse3200.game.components.spells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.statuseffects.Stat;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link FreezeSpellComponent}: everything it catches stops dead and turns blue. */
@ExtendWith(GameExtension.class)
class FreezeSpellComponentTest {
  private static final long FREEZE_MILLIS = 5000L;

  private GameTime time;
  private Entity caster;

  private record FixedTargets(Entity... entities) implements EnemyTargetingStrategy {
    @Override
    public Array<Entity> selectTargets(Entity caster) {
      Array<Entity> targets = new Array<>();
      for (Entity entity : entities) {
        targets.add(entity);
      }
      return targets;
    }

    @Override
    public float getRadius() {
      return 3f;
    }
  }

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    caster = new Entity();
  }

  private static Entity enemy() {
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent());
    enemy.create();
    return enemy;
  }

  private FreezeSpellComponent freezeAt(Entity... targets) {
    FreezeSpellComponent spell =
        new FreezeSpellComponent(5f, FREEZE_MILLIS, new FixedTargets(targets));
    caster.addComponent(spell);
    caster.create();
    return spell;
  }

  @Test
  void pinsEveryEnemyItCatches() {
    Entity first = enemy();
    Entity second = enemy();

    freezeAt(first, second).cast();

    assertTrue(StatusEffectsControllerComponent.isImmobilised(first));
    assertTrue(StatusEffectsControllerComponent.isImmobilised(second));
  }

  @Test
  void turnsThemLightBlueAndGlowingSoTheFreezeShowsOnAnySprite() {
    Entity frozen = enemy();

    freezeAt(frozen).cast();

    Color tint = StatusEffectsControllerComponent.getTint(frozen);
    Color glow = StatusEffectsControllerComponent.getGlow(frozen);
    assertNotNull(tint);
    assertNotNull(glow, "a tint alone is invisible on a near-black enemy");
    assertTrue(tint.b > tint.r, "ice: blue over red");
    assertTrue(glow.b > glow.r);
  }

  @Test
  void aFrozenEnemyCannotHurtAnythingOnContactEither() {
    Entity frozen = enemy();

    freezeAt(frozen).cast();

    CombatStatsComponent stats = frozen.getComponent(CombatStatsComponent.class);
    assertEquals(0, stats.getEffectiveBaseAttack());
    assertEquals(10, stats.getBaseAttack(), "its real stats are untouched underneath");
  }

  @Test
  void thawsOnceTheFreezeRunsOutAndIsWholeAgain() {
    Entity frozen = enemy();
    freezeAt(frozen).cast();

    when(time.getTime()).thenReturn(FREEZE_MILLIS);

    assertFalse(StatusEffectsControllerComponent.isImmobilised(frozen));
    assertEquals(10, frozen.getComponent(CombatStatsComponent.class).getEffectiveBaseAttack());
    assertEquals(
        1f,
        frozen
            .getComponent(StatusEffectsControllerComponent.class)
            .getStatMultiplier(Stat.MOVEMENT_SPEED));
  }

  @Test
  void doesNoDamageOfItsOwn() {
    Entity frozen = enemy();

    freezeAt(frozen).cast();

    assertEquals(100, frozen.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void skipsATargetThatCannotCarryAnEffect() {
    Entity plain = new Entity();

    assertTrue(freezeAt(plain).cast());

    assertFalse(StatusEffectsControllerComponent.isImmobilised(plain));
  }

  @Test
  void doesNotFreezeAgainWhileOnCooldown() {
    Entity frozen = enemy();
    FreezeSpellComponent spell = freezeAt(frozen);

    assertTrue(spell.cast());
    assertFalse(spell.cast());
  }

  @Test
  void isCastByItsOwnEvent() {
    Entity target = enemy();
    freezeAt(target);

    caster.getEvents().trigger(FreezeSpellComponent.CAST_EVENT);

    assertTrue(StatusEffectsControllerComponent.isImmobilised(target));
  }

  @Test
  void lightningAndFreezeListenOnDifferentEventsSoOneCastIsNotBoth() {
    Entity target = enemy();
    caster
        .addComponent(new FreezeSpellComponent(5f, FREEZE_MILLIS, new FixedTargets(target)))
        .addComponent(new LightningSpellComponent(5f, 25, 400L, new FixedTargets(target)));
    caster.create();

    caster.getEvents().trigger(FreezeSpellComponent.CAST_EVENT);

    assertTrue(StatusEffectsControllerComponent.isImmobilised(target));
    assertEquals(
        100,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "casting freeze must not also fire lightning");
  }

  @Test
  void rejectsNonsensicalArguments() {
    FixedTargets strategy = new FixedTargets();

    assertThrows(IllegalArgumentException.class, () -> new FreezeSpellComponent(5f, -1L, strategy));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FreezeSpellComponent(-1f, FREEZE_MILLIS, strategy));
    assertThrows(
        IllegalArgumentException.class, () -> new FreezeSpellComponent(5f, FREEZE_MILLIS, null));
  }
}
