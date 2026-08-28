package com.csse3200.game.ui.terminal.commands;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug command: {@code hitbox} spawns a weapon sensor in front of the player (visible with {@code
 * debug on}).
 */
public class HitboxCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(HitboxCommand.class);
  private static final float MULTIPLIER = 0.8f;
  private static final float LIFETIME = 2f;

  @Override
  public boolean action(ArrayList<String> args) {
    Entity player = ServiceLocator.getEntityService().findByComponent(PlayerActions.class);
    if (player == null) {
      logger.debug("hitbox: no player in the world");
      return false;
    }

    CombatStatsComponent combat = player.getComponent(CombatStatsComponent.class);
    int baseAttack = combat == null ? 0 : combat.getBaseAttack();

    HitboxSpec spec = new HitboxSpec();
    spec.position = player.getPosition();
    spec.size = new Vector2(0.5f, 0.8f);
    spec.lifetime = LIFETIME;
    spec.damage = Math.round(baseAttack * MULTIPLIER);
    spec.owner = player;
    spec.localOffset = new Vector2(0.6f, 0f);

    ServiceLocator.getEntityService().register(HitboxFactory.createHitbox(spec));
    if (ServiceLocator.getRenderService() != null) {
      ServiceLocator.getRenderService().getDebug().setActive(true);
    }
    return true;
  }
}
