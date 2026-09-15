package com.csse3200.game.components.abilities;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.abilities.targeting.EnemyTargetingStrategy;
import com.csse3200.game.entities.Entity;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightning spell: on cast, deals a fixed amount of damage and applies a stun to every enemy
 * selected by its {@link EnemyTargetingStrategy}. Which enemies get hit (all of them, just the
 * closest, on-screen only, ...) is fully decoupled from this class &mdash; swap the strategy to
 * change targeting behaviour without touching the spell's effect logic.
 *
 * <p>Deliberately does not reuse {@link com.csse3200.game.components.weapons.WeaponComponent}: a
 * wielder can only have one {@code WeaponStatsComponent} (component lookup in this engine is by
 * exact class), and that slot is already used by whatever melee weapon is equipped. This component
 * tracks its own cooldown instead, so it can coexist with a weapon on the same entity.
 */
public class LightningSpellComponent extends Component {
    private static final Logger logger = LoggerFactory.getLogger(LightningSpellComponent.class);

    private final float cooldown;
    private final int damage;
    private final float stunDuration;
    private EnemyTargetingStrategy targetingStrategy;
    private float remainingCooldown;

    /**
     * @param cooldown seconds between casts
     * @param damage damage dealt to each struck enemy
     * @param stunDuration seconds each struck enemy is stunned for
     * @param targetingStrategy how targets are selected on cast
     * @require cooldown &gt;= 0 &amp;&amp; damage &gt;= 0 &amp;&amp; stunDuration &gt;= 0 &amp;&amp;
     *     targetingStrategy != null
     * @throws IllegalArgumentException if a numeric argument is negative or targetingStrategy is
     *     null
     */
    public LightningSpellComponent(
            float cooldown, int damage, float stunDuration, EnemyTargetingStrategy targetingStrategy) {
        if (cooldown < 0f || damage < 0 || stunDuration < 0f) {
            logger.error(
                    "Invalid LightningSpellComponent args: cooldown={}, damage={}, stunDuration={}",
                    cooldown,
                    damage,
                    stunDuration);
            throw new IllegalArgumentException("cooldown, damage and stunDuration must be >= 0");
        }
        if (targetingStrategy == null) {
            throw new IllegalArgumentException("targetingStrategy must not be null");
        }
        this.cooldown = cooldown;
        this.damage = damage;
        this.stunDuration = stunDuration;
        this.targetingStrategy = targetingStrategy;
    }

    /**
     * Swap targeting behaviour at runtime, e.g. if the player picks a different spell variant.
     *
     * @param targetingStrategy new targeting strategy
     * @require targetingStrategy != null
     * @throws IllegalArgumentException if targetingStrategy is null
     */
    public void setTargetingStrategy(EnemyTargetingStrategy targetingStrategy) {
        if (targetingStrategy == null) {
            throw new IllegalArgumentException("targetingStrategy must not be null");
        }
        this.targetingStrategy = targetingStrategy;
    }

    @Override
    public void create() {
        entity.getEvents().addListener("specialAttack", this::cast);
    }

    @Override
    public void update() {
        if (remainingCooldown <= 0f) {
            return;
        }
        float dt = ServiceLocator.getTimeSource().getDeltaTime();
        remainingCooldown = Math.max(0f, remainingCooldown - Math.max(0f, dt));
    }

    /**
     * @return true if the cooldown has elapsed and the spell can be cast
     */
    public boolean canCast() {
        return remainingCooldown <= 0f;
    }

    private void cast() {
        if (!canCast()) {
            return;
        }
        remainingCooldown = cooldown;
        strike(targetingStrategy.selectTargets(entity));
    }

    private void strike(Array<Entity> targets) {
        // Proxy "attacker" stats representing the spell itself, same trick HitboxFactory uses to
        // apply damage via CombatStatsComponent#hit without a real attacking entity.
        CombatStatsComponent spellStats = new CombatStatsComponent(0, damage);

        for (Entity target : targets) {
            CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
            if (targetStats != null) {
                targetStats.hit(spellStats);
            }

            // TODO: hook up the existing stun component here once its class/method are confirmed, e.g.:

        }
    }
}