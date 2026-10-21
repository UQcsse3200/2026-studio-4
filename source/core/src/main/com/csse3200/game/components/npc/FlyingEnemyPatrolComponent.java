package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.tasks.PatrolTask;

/** Initialises flying patrol from the entity's assigned spawn position. */
public class FlyingEnemyPatrolComponent extends Component {
  private Vector2 spawn;

  public FlyingEnemyPatrolComponent(Vector2 spawn) {
    this.spawn = spawn;
  }

  @Override
  public void create() {
    super.create();
    entity.getComponent(AITaskComponent.class).addTask(new PatrolTask(spawn, true));
  }
}
