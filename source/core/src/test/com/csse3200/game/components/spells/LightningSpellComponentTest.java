package com.csse3200.game.components.spells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.targeting.EnemyTargetingStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** {@link LightningSpellComponent}: damage and a purple flash on everything it catches. */
@ExtendWith(GameExtension.class)
class LightningSpellComponentTest {
  private static final int DAMAGE = 25;
  private static final long FLASH_MILLIS = 400L;

  private GameTime time;
  private Entity caster;

  /** A strategy handing back a fixed list, keeping these tests about the effect, not targeting. */
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

  private static Entity enemyWith(int health) {
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 10))
            .addComponent(new StatusEffectsControllerComponent());
    enemy.create();
    return enemy;
  }

  private LightningSpellComponent lightningAt(Entity... targets) {
    return lightningAt(DAMAGE, targets);
  }

  private LightningSpellComponent lightningAt(int damage, Entity... targets) {
    LightningSpellComponent spell =
        new LightningSpellComponent(5f, damage, FLASH_MILLIS, new FixedTargets(targets));
    caster.addComponent(spell);
    caster.create();
    return spell;
  }

  @Test
  void damagesEveryEnemyItCatches() {
    Entity first = enemyWith(100);
    Entity second = enemyWith(40);

    lightningAt(first, second).cast();

    assertEquals(100 - DAMAGE, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(40 - DAMAGE, second.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void exposesCasterDuringDamageWithoutChangingItsHitReactionSource() {
    Entity enemy = enemyWith(100);
    AtomicReference<Entity> source = new AtomicReference<>();
    List<String> events = new ArrayList<>();
    enemy
        .getEvents()
        .addListener(
            LightningSpellComponent.DAMAGE_SOURCE_EVENT,
            (Entity currentSource) -> {
              source.set(currentSource);
              events.add(currentSource == null ? "cleared" : "caster");
            });
    enemy
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity attacker) -> {
              assertSame(caster, source.get());
              assertNull(attacker);
              events.add("attempted");
            });
    enemy
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity attacker, Integer healthLost, Integer remainingHealth) -> {
              assertSame(caster, source.get());
              assertNull(attacker);
              assertEquals(DAMAGE, healthLost);
              events.add("taken");
            });
    enemy
        .getEvents()
        .addListener(
            "hitReaction",
            (Entity attacker) -> {
              assertSame(caster, source.get());
              assertNull(attacker);
              events.add("reaction");
            });

    lightningAt(enemy).cast();

    assertEquals(List.of("caster", "attempted", "taken", "reaction", "cleared"), events);
    assertNull(source.get());
  }

  @Test
  void clearsCasterForZeroDamage() {
    assertCasterClearedWithoutDamage(0, false);
  }

  @Test
  void clearsCasterForBlockedDamage() {
    assertCasterClearedWithoutDamage(DAMAGE, true);
  }

  private void assertCasterClearedWithoutDamage(int damage, boolean invulnerable) {
    Entity enemy = enemyWith(100);
    enemy.getComponent(CombatStatsComponent.class).setInvulnerable(invulnerable);
    List<Entity> sources = new ArrayList<>();
    enemy
        .getEvents()
        .addListener(
            LightningSpellComponent.DAMAGE_SOURCE_EVENT, (Entity source) -> sources.add(source));

    lightningAt(damage, enemy).cast();

    assertEquals(100, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(2, sources.size());
    assertSame(caster, sources.get(0));
    assertNull(sources.get(1));
  }

  @Test
  void clearsCasterWhenDamageThrows() {
    Entity enemy = enemyWith(100);
    List<Entity> sources = new ArrayList<>();
    enemy
        .getEvents()
        .addListener(
            LightningSpellComponent.DAMAGE_SOURCE_EVENT, (Entity source) -> sources.add(source));
    enemy
        .getEvents()
        .addListener(
            "damageAttempted",
            (Integer damage, Entity attacker) -> {
              throw new IllegalStateException("damage interrupted");
            });
    LightningSpellComponent spell = lightningAt(enemy);

    assertThrows(IllegalStateException.class, spell::cast);

    assertEquals(2, sources.size());
    assertSame(caster, sources.get(0));
    assertNull(sources.get(1));
  }

  @Test
  void canFinishOffAnEnemyOutright() {
    Entity dying = enemyWith(10);

    lightningAt(dying).cast();

    assertEquals(0, dying.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(dying.getComponent(CombatStatsComponent.class).isDead());
  }

  @Test
  void flashesEachStruckEnemyPurple() {
    Entity struck = enemyWith(100);

    lightningAt(struck).cast();

    Color tint = StatusEffectsControllerComponent.getTint(struck);
    assertNotNull(tint, "a struck enemy should be tinted");
    assertTrue(tint.r > tint.g, "purple: red and blue over green");
    assertTrue(tint.b > tint.g);
  }

  @Test
  void alsoGlowsSoTheFlashShowsOnANearBlackEnemy() {
    Entity struck = enemyWith(100);

    lightningAt(struck).cast();

    Color glow = StatusEffectsControllerComponent.getGlow(struck);
    assertNotNull(glow, "a tint alone leaves a dark sprite looking unhit");
    assertTrue(glow.b > glow.g, "and it should read as the same purple");
  }

  @Test
  void theFlashIsBriefAndLeavesTheEnemyLookingNormalAgain() {
    Entity struck = enemyWith(100);
    lightningAt(struck).cast();

    when(time.getTime()).thenReturn(FLASH_MILLIS);

    assertNull(StatusEffectsControllerComponent.getTint(struck));
    assertNull(StatusEffectsControllerComponent.getGlow(struck));
  }

  @Test
  void leavesTheStruckEnemyFreeToKeepFighting() {
    // Lightning hurts; only freeze pins an enemy in place.
    Entity struck = enemyWith(100);

    lightningAt(struck).cast();

    assertFalse(StatusEffectsControllerComponent.isImmobilised(struck));
  }

  @Test
  void skipsSomethingWithNoHealthToTakeRatherThanThrowing() {
    Entity noStats = new Entity().addComponent(new StatusEffectsControllerComponent());

    assertTrue(lightningAt(noStats).cast());

    assertNull(StatusEffectsControllerComponent.getTint(noStats), "nothing was struck");
  }

  @Test
  void doesNotStrikeAgainWhileOnCooldown() {
    Entity enemy = enemyWith(100);
    LightningSpellComponent spell = lightningAt(enemy);

    assertTrue(spell.cast());
    assertFalse(spell.cast());

    assertEquals(100 - DAMAGE, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void isCastByItsOwnEvent() {
    Entity enemy = enemyWith(100);
    lightningAt(enemy);

    caster.getEvents().trigger(LightningSpellComponent.CAST_EVENT);

    assertEquals(100 - DAMAGE, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void rejectsNonsensicalDamageOrFlashLengths() {
    FixedTargets strategy = new FixedTargets();

    assertThrows(
        IllegalArgumentException.class,
        () -> new LightningSpellComponent(5f, -1, FLASH_MILLIS, strategy));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LightningSpellComponent(5f, DAMAGE, -1L, strategy));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LightningSpellComponent(-1f, DAMAGE, FLASH_MILLIS, strategy));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LightningSpellComponent(5f, DAMAGE, FLASH_MILLIS, null));
  }

  @Test
  void aZeroDamageBoltStillFlashesTheEnemy() {
    Entity enemy = enemyWith(100);
    LightningSpellComponent spell =
        new LightningSpellComponent(5f, 0, FLASH_MILLIS, new FixedTargets(enemy));
    caster.addComponent(spell);
    caster.create();

    spell.cast();

    assertEquals(100, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertNotNull(StatusEffectsControllerComponent.getTint(enemy));
  }
}
