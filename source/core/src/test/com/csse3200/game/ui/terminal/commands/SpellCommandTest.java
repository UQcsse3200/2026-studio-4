package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.components.spells.FreezeSpellComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.spells.targeting.StrategyWithinRadius;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The {@code spell lightning|freeze} terminal command. */
@ExtendWith(GameExtension.class)
class SpellCommandTest {
  private GameTime time;
  private Entity player;
  private SpellCommand command;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    EntityService entities = mock(EntityService.class);
    when(entities.getEntities()).thenReturn(new Array<>());
    ServiceLocator.registerEntityService(entities);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new StatusEffectsControllerComponent())
            .addComponent(new LightningSpellComponent(5f, 25, 400L, new StrategyWithinRadius(3f)))
            .addComponent(new FreezeSpellComponent(5f, 5000L, new StrategyWithinRadius(3f)));
    player.create();
    command = new SpellCommand(player);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  @Test
  void castsLightning() {
    assertTrue(command.action(args("lightning")));
  }

  @Test
  void castsFreeze() {
    assertTrue(command.action(args("freeze")));
  }

  @Test
  void castingOneSpellLeavesTheOtherReady() {
    assertTrue(command.action(args("lightning")));

    assertTrue(command.action(args("freeze")), "the two spells have separate cooldowns");
  }

  @Test
  void reportsFailureWhileTheSpellIsStillOnCooldown() {
    // Casting goes through the spell rather than around it, so the terminal shows the refusal
    // instead of silently firing anyway.
    assertTrue(command.action(args("lightning")));

    assertFalse(command.action(args("lightning")));
  }

  @Test
  void succeedsAgainOnceTheCooldownHasRunDown() {
    command.action(args("freeze"));
    when(time.getDeltaTime()).thenReturn(5f);
    player.getComponent(FreezeSpellComponent.class).update();

    assertTrue(command.action(args("freeze")));
  }

  @Test
  void rejectsASpellNameItDoesNotKnow() {
    assertFalse(command.action(args("fireball")));
    assertFalse(command.action(args("")));
    assertFalse(command.action(args("Lightning")), "names are matched exactly");
  }

  @Test
  void rejectsTheWrongNumberOfArguments() {
    assertFalse(command.action(args()));
    assertFalse(command.action(args("lightning", "freeze")));
  }

  @Test
  void survivesBeingCalledWithNothingToWorkWith() {
    assertFalse(command.action(null));
    assertFalse(new SpellCommand(null).action(args("lightning")));
  }

  @Test
  void reportsFailureWhenThePlayerSimplyDoesNotHaveThatSpell() {
    Entity spellless = new Entity().addComponent(new CombatStatsComponent(100, 10));
    spellless.create();

    assertFalse(new SpellCommand(spellless).action(args("lightning")));
    assertFalse(new SpellCommand(spellless).action(args("freeze")));
  }
}
