package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.GameTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbilityCommandTest {
  private GameTime time;
  private CombatStatsComponent stats;
  private PlayerAbilitiesComponent abilities;
  private AbilityCommand command;
  private Entity hostile;
  private final List<String> used = new ArrayList<>();

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(1_000L);
    stats = new CombatStatsComponent(100, 10);
    abilities = new PlayerAbilitiesComponent(time);
    Entity player = new Entity().addComponent(stats).addComponent(abilities);
    player.create();
    player.getEvents().addListener("abilityUsed", (String ability) -> used.add(ability));
    command = new AbilityCommand(player);
    hostile = new Entity().addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER));
  }

  @Test
  void shouldRejectNullEmptyExtraAndUnknownArgumentsWithoutActivatingAbilities() {
    assertFalse(command.action(null));
    assertFalse(command.action(args()));
    assertFalse(command.action(args("invisibility", "laststand")));
    assertFalse(command.action(args("laststand", "extra")));
    for (String invalid :
        List.of("", "unknown", "Invisibility", "LASTSTAND", " invisibility", "laststand ")) {
      assertFalse(command.action(args(invalid)), invalid);
    }
    assertFalse(abilities.isInvisible());
    assertEquals(0, abilities.getInvisibilityCooldownRemainingMs());
    stats.takeDamage(81, hostile);
    assertFalse(abilities.isLastStandActive());
    assertEquals(0, abilities.getLastStandCooldownRemainingMs());
    assertTrue(used.isEmpty());
  }

  @Test
  void shouldRejectMissingPlayerOrAbilitiesForBothCommands() {
    for (AbilityCommand unavailable :
        List.of(new AbilityCommand(null), new AbilityCommand(new Entity()))) {
      assertFalse(unavailable.action(args("invisibility")));
      assertFalse(unavailable.action(args("laststand")));
    }
  }

  @Test
  void shouldCastInvisibilityWithoutBypassingOrResettingCooldown() {
    assertTrue(command.action(args("invisibility")));
    assertTrue(abilities.isInvisible());
    assertFalse(command.action(args("invisibility")));
    when(time.getTime()).thenReturn(6_000L);
    assertFalse(command.action(args("invisibility")));
    assertEquals(10_000, abilities.getInvisibilityRemainingMs());
    assertEquals(40_000, abilities.getInvisibilityCooldownRemainingMs());
    when(time.getTime()).thenReturn(16_000L);
    assertFalse(command.action(args("invisibility")));
    assertFalse(abilities.isInvisible());
    when(time.getTime()).thenReturn(45_999L);
    assertFalse(command.action(args("invisibility")));
    assertEquals(1, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(List.of("invisibility"), used);
    when(time.getTime()).thenReturn(46_000L);
    assertTrue(command.action(args("invisibility")));
    assertEquals(15_000, abilities.getInvisibilityRemainingMs());
    assertEquals(45_000, abilities.getInvisibilityCooldownRemainingMs());
    assertEquals(List.of("invisibility", "invisibility"), used);
  }

  @Test
  void shouldEnableLastStandIdempotentlyWithoutDirectActivationOrCooldownBypass() {
    stats.setHealth(19);
    assertTrue(command.action(args("laststand")));
    assertTrue(command.action(args("laststand")));
    assertFalse(abilities.isLastStandActive());
    assertEquals(0, abilities.getLastStandCooldownRemainingMs());
    assertTrue(used.isEmpty());
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    when(time.getTime()).thenReturn(6_000L);
    assertTrue(command.action(args("laststand")));
    stats.takeDamage(1, hostile);
    assertEquals(5_000, abilities.getLastStandRemainingMs());
    assertEquals(55_000, abilities.getLastStandCooldownRemainingMs());
    when(time.getTime()).thenReturn(60_999L);
    assertTrue(command.action(args("laststand")));
    stats.takeDamage(1, hostile);
    assertFalse(abilities.isLastStandActive());
    assertEquals(1, abilities.getLastStandCooldownRemainingMs());
    when(time.getTime()).thenReturn(61_000L);
    assertTrue(command.action(args("laststand")));
    assertFalse(abilities.isLastStandActive());
    stats.takeDamage(1, hostile);
    assertTrue(abilities.isLastStandActive());
    assertEquals(60_000, abilities.getLastStandCooldownRemainingMs());
    assertEquals(List.of("laststand", "laststand"), used);
  }

  @Test
  void shouldRejectDeadPlayerCastAndNotEnablePassiveWhileDead() {
    stats.setHealth(0);
    assertFalse(command.action(args("invisibility")));
    // Last Stand reports a recognized command, not whether the player can enable the passive.
    assertTrue(command.action(args("laststand")));
    stats.setHealth(19);
    stats.takeDamage(1, hostile);
    assertFalse(abilities.isLastStandActive());
    assertTrue(used.isEmpty());
  }

  @Test
  void shouldNotActivateDisposedOrUncreatedAbilitiesOrAbilitiesWithoutStats() {
    abilities.dispose();
    assertFalse(command.action(args("invisibility")));
    assertTrue(command.action(args("laststand")));
    stats.takeDamage(81, hostile);
    assertFalse(abilities.isLastStandActive());
    assertTrue(used.isEmpty());

    PlayerAbilitiesComponent incomplete = new PlayerAbilitiesComponent(time);
    Entity target = new Entity().addComponent(incomplete);
    AbilityCommand incompleteCommand = new AbilityCommand(target);
    assertFalse(incompleteCommand.action(args("invisibility")));
    assertTrue(incompleteCommand.action(args("laststand")));
    target.create();
    assertFalse(incompleteCommand.action(args("invisibility")));
    assertTrue(incompleteCommand.action(args("laststand")));
    assertFalse(incomplete.isLastStandActive());
  }

  private ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
