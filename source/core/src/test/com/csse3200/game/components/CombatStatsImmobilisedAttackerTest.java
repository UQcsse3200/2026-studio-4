package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.statuseffects.FrozenEffect;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A frozen attacker's blow never lands.
 *
 * <p>Bosses, summons and Cerberus all deal damage by calling the victim's {@code takeDamage}
 * directly rather than through a touch attack, so guarding the victim's one entry point is what
 * stops a frozen boss hurting the player at all.
 */
@ExtendWith(GameExtension.class)
class CombatStatsImmobilisedAttackerTest {
  private GameTime time;
  private Entity victim;
  private CombatStatsComponent victimStats;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);

    victimStats = new CombatStatsComponent(100, 5);
    victim =
        new Entity().addComponent(victimStats).addComponent(new StatusEffectsControllerComponent());
    victim.create();
  }

  /** An attacker that can be frozen, built the way a boss is. */
  private Entity attacker() {
    Entity attacker =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 20))
            .addComponent(new StatusEffectsControllerComponent());
    attacker.create();
    return attacker;
  }

  private static void freeze(Entity entity, GameTime time) {
    entity
        .getComponent(StatusEffectsControllerComponent.class)
        .addStatusEffect(new FrozenEffect(time, 5000L));
  }

  @Test
  void anUnfrozenAttackerHurtsAsNormal() {
    victimStats.takeDamage(30, attacker());

    assertEquals(70, victimStats.getHealth());
  }

  @Test
  void aFrozenAttackerDoesNoDamageAtAll() {
    Entity frozen = attacker();
    freeze(frozen, time);

    victimStats.takeDamage(30, frozen);

    assertEquals(100, victimStats.getHealth());
  }

  @Test
  void aFrozenAttackerHurtsAgainOnceItThaws() {
    Entity frozen = attacker();
    freeze(frozen, time);
    victimStats.takeDamage(30, frozen);

    when(time.getTime()).thenReturn(5000L);
    victimStats.takeDamage(30, frozen);

    assertEquals(70, victimStats.getHealth());
  }

  @Test
  void unattributedDamageStillLands() {
    // The lightning spell has no entity behind its bolt, and must not be swallowed by this guard.
    victimStats.takeDamage(25, null);
    victimStats.takeDamage(25);

    assertEquals(50, victimStats.getHealth());
  }

  @Test
  void aFrozenVictimCanStillBeHurtByAnUnfrozenAttacker() {
    // Only the attacker is checked. Freezing an enemy must not also make it invulnerable.
    freeze(victim, time);

    victimStats.takeDamage(30, attacker());

    assertEquals(70, victimStats.getHealth());
  }

  @Test
  void aFrozenAttackerDealsNothingOnContactEither() {
    // Touch attacks route through hit(), which reads the attacker's effective attack. A freeze
    // zeroes that as well, so contact damage is stopped twice over rather than leaking through.
    Entity frozen = attacker();
    frozen.addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
    freeze(frozen, time);

    victimStats.hit(frozen.getComponent(CombatStatsComponent.class));

    assertEquals(100, victimStats.getHealth());
  }

  @Test
  void anAttackerWithNoStatusEffectsAtAllIsNeverBlocked() {
    Entity plain = new Entity().addComponent(new CombatStatsComponent(100, 20));

    victimStats.takeDamage(30, plain);

    assertEquals(70, victimStats.getHealth());
  }

  @Test
  void aBlockedBlowLeavesNoTraceOnTheVictim() {
    Entity frozen = attacker();
    freeze(frozen, time);
    StringBuilder reactions = new StringBuilder();
    victim.getEvents().addListener("hitReaction", (Entity source) -> reactions.append("hit"));
    victim
        .getEvents()
        .addListener(
            "damageTaken",
            (Entity source, Integer lost, Integer left) -> reactions.append("damage"));

    victimStats.takeDamage(30, frozen);

    assertTrue(reactions.isEmpty(), "no knockback, no flash, nothing");
  }
}
