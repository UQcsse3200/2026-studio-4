package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** in Phase two of norse miniboss, between Stampedes, attacks within range */
public class EarthquakeAttackTask extends DefaultTask implements PriorityTask {

  private float WAIT = 3f;
  private Entity target;
  private float range;
  private float coolDownTimer = 0f;
  private boolean phaseTwoActivated = false;
  private boolean cooldown = true;

  public EarthquakeAttackTask(Entity target, float range, Entity entity) {
    this.target = target;
    this.range = range;
    entity.getEvents().addListener("enragePhaseStarted", this::activate);
  }

  public void start() {
    super.start();
  }

  protected void activate() {
    phaseTwoActivated = true;
  }

  public void update() {
    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();
    coolDownTimer += deltaTime;
    if (!cooldown && coolDownTimer >= WAIT) {
      coolDownTimer = 0;
      cooldown = true; // cooldown over
    }
    if (!phaseTwoActivated || !cooldown) {
      return;
    }
    CombatStatsComponent playerStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent entityStats = owner.getEntity().getComponent(CombatStatsComponent.class);

    // trigger animation

    if (target.getPosition().dst(owner.getEntity().getPosition()) <= range) {
      playerStats.takeDamage(entityStats.getBaseAttack(), owner.getEntity());
      cooldown = false;
    }
  }

  @Override
  public int getPriority() {
    if (!phaseTwoActivated || !cooldown) {
      return -1;
    } else {
      return 15;
    }
  }

  @Override
  public void setPriority(int status) {}
}
