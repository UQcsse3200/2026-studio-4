package com.csse3200.game.components.spells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What every spell shares: a cooldown, a targeting strategy deciding who is caught, and the disc
 * drawn over the area covered. Exercised through a spell that only records who it landed on.
 */
@ExtendWith(GameExtension.class)
class SpellComponentTest {
  private static final Color AOE_COLOUR = new Color(0.5f, 0.25f, 0.9f, 0.8f);

  private GameTime time;
  private Entity caster;

  /** A strategy that hands back exactly the targets a test asked for. */
  private static class FixedTargets implements EnemyTargetingStrategy {
    private final Array<Entity> targets = new Array<>();
    private final float radius;
    private Entity lastCaster;

    FixedTargets(float radius, Entity... entities) {
      this.radius = radius;
      for (Entity entity : entities) {
        targets.add(entity);
      }
    }

    @Override
    public Array<Entity> selectTargets(Entity caster) {
      lastCaster = caster;
      return targets;
    }

    @Override
    public float getRadius() {
      return radius;
    }
  }

  /** A spell whose only effect is to remember who it reached. */
  private static class RecordingSpell extends SpellComponent {
    final List<Entity> hit = new ArrayList<>();

    RecordingSpell(float cooldown, EnemyTargetingStrategy strategy) {
      super(cooldown, "castRecording", AOE_COLOUR, strategy);
    }

    @Override
    protected void applyTo(Entity target) {
      hit.add(target);
    }
  }

  /** A spell built with one deliberately missing argument. */
  private static SpellComponent spellWith(
      float cooldown, String castEvent, Color colour, EnemyTargetingStrategy strategy) {
    return new SpellComponent(cooldown, castEvent, colour, strategy) {
      @Override
      protected void applyTo(Entity target) {
        // Never reached: these spells exist only to be rejected by the constructor.
      }
    };
  }

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    caster = new Entity();
  }

  private static Entity freezableTarget() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new StatusEffectsControllerComponent());
  }

  private RecordingSpell spellOn(float cooldown, EnemyTargetingStrategy strategy) {
    RecordingSpell spell = new RecordingSpell(cooldown, strategy);
    caster.addComponent(spell);
    caster.create();
    return spell;
  }

  @Test
  void rejectsBeingBuiltWithoutTheThingsACastNeeds() {
    FixedTargets strategy = new FixedTargets(3f);

    assertThrows(
        IllegalArgumentException.class,
        () -> spellWith(-0.1f, "cast", AOE_COLOUR, strategy),
        "a negative cooldown");
    assertThrows(
        IllegalArgumentException.class,
        () -> spellWith(1f, null, AOE_COLOUR, strategy),
        "no cast event");
    assertThrows(
        IllegalArgumentException.class, () -> spellWith(1f, "cast", null, strategy), "no colour");
    assertThrows(
        IllegalArgumentException.class,
        () -> spellWith(1f, "cast", AOE_COLOUR, null),
        "no targets");
  }

  @Test
  void aZeroCooldownSpellIsAlwaysReady() {
    RecordingSpell spell = spellOn(0f, new FixedTargets(3f));

    assertTrue(spell.cast());
    assertTrue(spell.cast(), "nothing to wait for");
  }

  @Test
  void landsOnEveryTargetTheStrategyPicked() {
    Entity first = freezableTarget();
    Entity second = freezableTarget();

    RecordingSpell spell = spellOn(5f, new FixedTargets(3f, first, second));

    assertTrue(spell.cast());
    assertEquals(List.of(first, second), spell.hit);
  }

  @Test
  void landingOnNothingIsStillACast() {
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f));

    assertTrue(spell.cast(), "the spell went off, it just caught nobody");
    assertFalse(spell.canCast(), "so it still costs the cooldown");
  }

  @Test
  void asksTheStrategyAboutTheCasterItIsAttachedTo() {
    FixedTargets strategy = new FixedTargets(3f);
    RecordingSpell spell = spellOn(5f, strategy);

    spell.cast();

    assertSame(caster, strategy.lastCaster);
  }

  @Test
  void castingAgainDuringTheCooldownDoesNothingAtAll() {
    Entity target = freezableTarget();
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f, target));

    assertTrue(spell.cast());
    assertFalse(spell.canCast());
    assertFalse(spell.cast(), "a spell on cooldown reports the cast failed");
    assertEquals(1, spell.hit.size(), "and must not land a second time");
  }

  @Test
  void becomesReadyAgainOnceTheCooldownHasBeenTickedThrough() {
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f));
    spell.cast();
    when(time.getDeltaTime()).thenReturn(2f);

    spell.update();
    assertFalse(spell.canCast(), "3 seconds still to run");
    spell.update();
    assertFalse(spell.canCast(), "1 second still to run");
    spell.update();

    assertTrue(spell.canCast());
    assertTrue(spell.cast());
  }

  @Test
  void aBadTimeStepNeitherStallsTheCooldownNorRunsItBackwards() {
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f));
    spell.cast();

    when(time.getDeltaTime()).thenReturn(-100f);
    spell.update();
    assertFalse(spell.canCast(), "a negative frame must not hand the cast back early");

    when(time.getDeltaTime()).thenReturn(99f);
    spell.update();
    assertTrue(spell.canCast(), "and an overshoot must not leave the cooldown stuck below zero");
  }

  @Test
  void anIdleSpellDoesNotKeepAskingForTheTime() {
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f));

    spell.update();

    verify(time, never()).getDeltaTime();
  }

  @Test
  void itsOwnCastEventFiresIt() {
    // Each spell listens on its own event, so a second spell on the same caster is not also cast.
    Entity target = freezableTarget();
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f, target));

    caster.getEvents().trigger("castRecording");

    assertEquals(List.of(target), spell.hit);
  }

  @Test
  void showsTheAreaItCoveredInItsOwnColourAndTheStrategysRadius() {
    SpellAoeVisualComponent visual = new SpellAoeVisualComponent();
    caster.addComponent(visual);
    RecordingSpell spell = spellOn(5f, new FixedTargets(4f));
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getColor()).thenReturn(new Color(Color.WHITE));

    visual.draw(batch);
    verify(batch, never()).draw(any(Texture.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());

    spell.cast();
    visual.draw(batch);

    // Radius 4 around an entity centred half a unit past the origin.
    verify(batch).draw(any(Texture.class), eq(-3.5f), eq(-3.5f), eq(8f), eq(8f));
  }

  @Test
  void castingWithoutAnAreaVisualStillLands() {
    Entity target = freezableTarget();
    RecordingSpell spell = spellOn(5f, new FixedTargets(3f, target));

    assertTrue(spell.cast());

    assertEquals(List.of(target), spell.hit);
  }

  @Test
  void swapsTargetingBehaviourWithoutTouchingTheEffect() {
    Entity original = freezableTarget();
    Entity replacement = freezableTarget();
    RecordingSpell spell = spellOn(0f, new FixedTargets(3f, original));

    spell.cast();
    spell.setTargetingStrategy(new FixedTargets(9f, replacement));
    spell.cast();

    assertEquals(List.of(original, replacement), spell.hit);
  }

  @Test
  void refusesATargetingStrategyOfNull() {
    RecordingSpell spell = new RecordingSpell(5f, new FixedTargets(3f));

    assertThrows(IllegalArgumentException.class, () -> spell.setTargetingStrategy(null));
  }

  @Test
  void putsTheEffectOnATargetThatCanCarryOne() {
    Entity target = freezableTarget();
    target.create();

    SpellComponent.addEffect(target, new FrozenEffect(time, 1000L));

    assertTrue(StatusEffectsControllerComponent.isImmobilised(target));
  }

  @Test
  void skipsATargetThatCannotCarryAnEffectRatherThanThrowing() {
    // Nothing guarantees every entity on the enemy layer carries a status effects controller.
    Entity plain = new Entity();

    SpellComponent.addEffect(plain, new FrozenEffect(time, 1000L));

    assertFalse(StatusEffectsControllerComponent.isImmobilised(plain));
  }

  @Test
  void skipsATargetWhoseControllerHasAlreadyBeenDisposed() {
    // An enemy can die partway through the same cast that is still working down its target list.
    Entity target = freezableTarget();
    target.create();
    target.getComponent(StatusEffectsControllerComponent.class).dispose();

    SpellComponent.addEffect(target, new FrozenEffect(time, 1000L));

    assertFalse(StatusEffectsControllerComponent.isImmobilised(target));
  }
}
