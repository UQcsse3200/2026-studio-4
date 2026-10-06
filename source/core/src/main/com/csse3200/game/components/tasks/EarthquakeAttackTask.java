package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** in Phase two of norse miniboss, between Stampedes, attacks within range */
public class EarthquakeAttackTask extends DefaultTask implements PriorityTask {

  private static final float waitFor = 1f;
  private final Entity target;
  private final float range;
  private float coolDownTimer = 0f;
  private boolean phaseTwoActivated = false;
  private int maxAttack = 0;

  public EarthquakeAttackTask(Entity target, float range, Entity entity) {
    this.target = target;
    this.range = range;
    entity.getEvents().addListener("enragePhaseStarted", this::activate);
  }

  protected void activate() {
    phaseTwoActivated = true;
  }

  @Override
  public void update() {
    if (!phaseTwoActivated) {
      return;
    }
    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();
    coolDownTimer += deltaTime;

    if (coolDownTimer < waitFor) {
      return;
    }
    coolDownTimer = 0;

    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent entityStats = owner.getEntity().getComponent(CombatStatsComponent.class);

    owner.getEntity().getEvents().trigger("rangedAttack");

    if (target.getPosition().dst(owner.getEntity().getPosition()) <= range) {
      playerStats.takeDamage(entityStats.getBaseAttack(), owner.getEntity());
    }
    maxAttack += 1;
  }

  @Override
  public int getPriority() {
    if (!phaseTwoActivated || maxAttack > 2) {
      return -1;
    } else {
      return 20;
    }
  }
}
