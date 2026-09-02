package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.items.Charm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CharmEffectComponentTest {

  @Test
  void shouldApplyStrengthBuffOnCharmAdded() {
    Entity player = createPlayer();

    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));

    assertEquals(20, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldRemoveStrengthBuffOnCharmRemoved() {
    Entity player = createPlayer();

    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));
    player.getEvents().trigger("charmRemoved", new Charm("Strength Charm"));

    assertEquals(10, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldIgnoreUnrelatedCharms() {
    Entity player = createPlayer();

    player.getEvents().trigger("charmAdded", new Charm("Health Charm"));

    assertEquals(10, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldNotStackBuffWhenDuplicateStrengthCharmsAdded() {
    Entity player = createPlayer();

    // Two separate Strength Charm instances, e.g. picked up twice
    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));
    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));

    // Buff should only apply once, not stack to +20
    assertEquals(20, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldKeepBuffActiveUntilLastStrengthCharmRemoved() {
    Entity player = createPlayer();

    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));
    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));
    player.getEvents().trigger("charmRemoved", new Charm("Strength Charm"));

    // One Strength Charm still held, so the buff should remain active
    assertEquals(20, player.getComponent(CombatStatsComponent.class).getBaseAttack());

    player.getEvents().trigger("charmRemoved", new Charm("Strength Charm"));

    // Last Strength Charm removed, buff should now be gone
    assertEquals(10, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void shouldIncreaseDamageWhileStrengthCharmHeld() {
    Entity player = createPlayer();
    CombatStatsComponent playerStats = player.getComponent(CombatStatsComponent.class);
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 0));
    target.create();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    player.getEvents().trigger("charmAdded", new Charm("Strength Charm"));
    targetStats.hit(playerStats);

    assertEquals(80, targetStats.getHealth());
  }

  private Entity createPlayer() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new CharmEffectComponent());
    player.create();
    return player;
  }
}
