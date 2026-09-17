package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class StatusEffectCommandTest {
  private StatusEffectCommand command;
  private StatusEffectsControllerComponent controller;

  @BeforeEach
  void setUp() {
    controller = new StatusEffectsControllerComponent();
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10, 4f, 1f))
            .addComponent(controller);
    player.create();
    command = new StatusEffectCommand(player);
  }

  @Test
  void shouldApplyNamedEffects() {
    assertTrue(command.action(new ArrayList<>(List.of("burn"))));
    assertTrue(command.action(new ArrayList<>(List.of("regeneration"))));
    assertTrue(command.action(new ArrayList<>(List.of("slow"))));
    assertTrue(command.action(new ArrayList<>(List.of("speed"))));
    assertTrue(command.action(new ArrayList<>(List.of("vulnerable"))));
    assertTrue(command.action(new ArrayList<>(List.of("freeze"))));
  }

  @Test
  void shouldRejectUnknownOrEmptyArgs() {
    assertFalse(command.action(new ArrayList<>()));
    assertFalse(command.action(new ArrayList<>(List.of("poison"))));
    assertFalse(command.action(new ArrayList<>(List.of("burn", "extra"))));
  }
}
