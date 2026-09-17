package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.spells.FreezeSpellComponent;
import com.csse3200.game.components.spells.LightningSpellComponent;
import com.csse3200.game.components.spells.SpellComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Map;

/**
 * Terminal command that casts one of the player's spells: {@code spell lightning|freeze}.
 *
 * <p>Casting goes through the spell itself rather than around it, so a spell still on cooldown is
 * reported as a failed command instead of firing anyway.
 */
public class SpellCommand implements Command {
  private static final Map<String, Class<? extends SpellComponent>> SPELLS =
      Map.of(
          "lightning", LightningSpellComponent.class,
          "freeze", FreezeSpellComponent.class);

  private final Entity player;

  /**
   * @param player entity carrying the spell components
   */
  public SpellCommand(Entity player) {
    this.player = player;
  }

  /**
   * Casts the named spell.
   *
   * @param args single argument: {@code lightning} or {@code freeze}
   * @return true if the spell was cast, false if the name is unknown, the player does not have that
   *     spell, or it is still on cooldown
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (player == null || args == null || args.size() != 1) {
      return false;
    }
    Class<? extends SpellComponent> spellType = SPELLS.get(args.getFirst());
    if (spellType == null) {
      return false;
    }
    SpellComponent spell = player.getComponent(spellType);
    return spell != null && spell.cast();
  }
}
