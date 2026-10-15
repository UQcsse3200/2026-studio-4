package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.tasks.PatrolTask;

/** Initialises flying patrol from the entity's assigned spawn position. */
public class FlyingEnemyPatrolComponent extends Component {

  @Override
  public void create() {
    super.create();

    Vector2 spawnPosition = entity.getPosition().cpy();
    entity.getComponent(AITaskComponent.class).addTask(new PatrolTask(spawnPosition, true));
  }
}
