package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Performs a telegraphed coil attack toward a target. The entity freezes briefly (the telegraph),
 * moves quickly toward the target and coils around them, then applies a poison effect that deals
 * damage over time, then enters a cooldown period before it can coil again.
 */
public class CoilAttackTask extends DefaultTask implements PriorityTask {
    private static final int PRIORITY = 20;
    private static final float TRIGGER_RANGE = 3f;
    private static final float TELEGRAPH_DURATION = 0.6f;
    private static final float COIL_SPEED = 5f;
    private static final float COIL_DISTANCE = 4f;
    private static final float COIL_DURATION = 0.5f;
    private static final float COOLDOWN_DURATION = 3f;

    private static final int POISON_DAMAGE_PER_TICK = 2;
    private static final long POISON_TICK_INTERVAL_MS = 1000;
    private static final long POISON_TOTAL_DURATION_MS = 4000;

    private enum Phase {
        TELEGRAPH,
        COIL,
        POISONING,
        DONE
    }

    private final Entity target;
    private final float restoreSpeed;
    private final GameTime gameTime;

    private PhysicsMovementComponent movementComponent;
    private Phase phase;
    private long phaseStartTime;
    private long cooldownEndTime = 0;
    private Vector2 coilTargetPoint;
    private PoisonEffect poisonEffect;

    /**
     * @param target Entity to coil toward (usually the player).
     * @param restoreSpeed Normal movement speed to return to after the coil ends.
     */
    public CoilAttackTask(Entity target, float restoreSpeed) {
        this.target = target;
        this.restoreSpeed = restoreSpeed;
        this.gameTime = ServiceLocator.getTimeSource();
    }

    @Override
    public void start() {
        super.start();
        movementComponent = owner.getEntity().getComponent(PhysicsMovementComponent.class);
        movementComponent.setMoving(false);
        phase = Phase.TELEGRAPH;
        phaseStartTime = gameTime.getTime();
        owner.getEntity().getEvents().trigger("coilTelegraphStart");
    }

    @Override
    public void update() {
        long now = gameTime.getTime();
        switch (phase) {
            case TELEGRAPH:
                if (now - phaseStartTime >= TELEGRAPH_DURATION * 1000) {
                    beginCoil(now);
                }
                break;
            case COIL:
                if (now - phaseStartTime >= COIL_DURATION * 1000 || reachedCoilTarget()) {
                    beginPoisoning(now);
                }
                break;
            case POISONING:
                if (poisonEffect.update()) {
                    endPoisoning(now);
                }
                break;
            case DONE:
                // Waiting for the AI component to switch to another task.
                break;
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (movementComponent != null) {
            movementComponent.setMoving(false);
            movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));
        }
    }

    @Override
    public int getPriority() {
        if (status == Status.ACTIVE) {
            return phase == Phase.DONE ? -1 : PRIORITY;
        }

        long now = gameTime.getTime();
        if (now < cooldownEndTime) {
            return -1;
        }
        if (getDistanceToTarget() <= TRIGGER_RANGE) {
            return PRIORITY;
        }
        return -1;
    }

    @Override
    public void setPriority(int status) {
        // Intentional empty method: this task's priority is fixed and computed internally.
    }

    private void beginCoil(long now) {
        Vector2 direction = target.getPosition().cpy().sub(owner.getEntity().getPosition()).nor();
        coilTargetPoint = owner.getEntity().getPosition().cpy().add(direction.scl(COIL_DISTANCE));

        movementComponent.setMaxSpeed(new Vector2(COIL_SPEED, COIL_SPEED));
        movementComponent.setTarget(coilTargetPoint);
        movementComponent.setMoving(true);

        phase = Phase.COIL;
        phaseStartTime = now;
        owner.getEntity().getEvents().trigger("coilStart");
    }

    private void beginPoisoning(long now) {
        movementComponent.setMoving(false);
        movementComponent.setMaxSpeed(new Vector2(restoreSpeed, restoreSpeed));

        CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
        poisonEffect =
                new PoisonEffect(
                        targetStats, POISON_DAMAGE_PER_TICK, POISON_TICK_INTERVAL_MS, POISON_TOTAL_DURATION_MS);

        phase = Phase.POISONING;
        phaseStartTime = now;
        owner.getEntity().getEvents().trigger("coilPoisonStart");
    }

    private void endPoisoning(long now) {
        poisonEffect = null;
        cooldownEndTime = now + (long) (COOLDOWN_DURATION * 1000);
        phase = Phase.DONE;
        owner.getEntity().getEvents().trigger("coilEnd");
    }

    private boolean reachedCoilTarget() {
        return owner.getEntity().getPosition().dst(coilTargetPoint) <= 0.2f;
    }

    private float getDistanceToTarget() {
        return owner.getEntity().getPosition().dst(target.getPosition());
    }
}
