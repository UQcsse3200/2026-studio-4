package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/** in phase two * */
public class StampedeTask extends DefaultTask implements PriorityTask {

  private Entity target;

  public StampedeTask(Entity target, Entity entity) {
    this.target = target;
    entity.getEvents().addListener("enragePhaseStarted", this::stampede);
  }

  private void stampede() {
    // run around player in a circle causing small damage attacks
    target.getComponent(CombatStatsComponent.class).takeDamage(1);
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public void setPriority(int status) {}
}
