package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InvisibilityPotionComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class AbilityCommandTest {
  private final AtomicLong nowMs = new AtomicLong();
  private AbilityCommand command;
  private InvisibilityPotionComponent invisibility;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenAnswer(invocation -> nowMs.get());
    ServiceLocator.registerTimeSource(time);
    nowMs.set(0L);

    invisibility = new InvisibilityPotionComponent();
    Entity player =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(invisibility);
    player.create();
    command = new AbilityCommand(player);
  }

  @Test
  void shouldApplyInvisibilityForQa() {
    assertTrue(command.action(new ArrayList<>(List.of("invisibility"))));
    assertTrue(invisibility.isInvisible());
  }

  @Test
  void shouldRejectUnknownAbility() {
    assertFalse(command.action(new ArrayList<>(List.of("dash"))));
    assertFalse(invisibility.isInvisible());
  }

  @Test
  void shouldRejectMissingArgs() {
    assertFalse(command.action(new ArrayList<>()));
    assertFalse(invisibility.isInvisible());
  }

  @Test
  void shouldFailWhenPlayerHasNoInvisibilityComponent() {
    AbilityCommand missing = new AbilityCommand(new Entity());
    assertFalse(missing.action(new ArrayList<>(List.of("invisibility"))));
  }

  @Test
  void qaCommandShouldBypassCooldown() {
    assertTrue(invisibility.tryUse());
    nowMs.set(20_000L);
    invisibility.update();
    assertFalse(invisibility.tryUse());

    assertTrue(command.action(new ArrayList<>(List.of("invisibility"))));
    assertTrue(invisibility.isInvisible());
    assertEquals("Invisibility: 15s", invisibility.getDurationHudText());
  }
}
