package com.csse3200.game.components.weapons;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.HitboxFactory;
import com.csse3200.game.entities.factories.HitboxSpec;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * Fast, short-range stab. Spawns a small hitbox just in front of the wielder that follows them for
 * its brief lifetime.
 *
 * <p>Once upgraded (see {@link WeaponUpgradeComponent}) the knife also has a heavy attack: a flurry
 * of two quick slashes that swing across the front of the wielder in opposite directions, then a
 * finishing stab. Only the finisher knocks enemies back, so they stay in reach of every hit.
 */
public class KnifeWeaponComponent extends WeaponComponent {
  /** Sprite drawn for the knife's stab. Loaded by {@link WeaponAssetsComponent}. */
  public static final String TEXTURE = "images/weapons/knife.png";

  /**
   * Fancier sprite drawn for every strike (stab, slash, finisher) once the knife is upgraded. Cut
   * from the same sheet and drawn at the same angle as {@link #TEXTURE}. Loaded by {@link
   * WeaponAssetsComponent}.
   */
  public static final String UPGRADED_TEXTURE = "images/weapons/knife_upgraded.png";

  private static final float BLADE_LENGTH = 1.0f;
  private static final float BLADE_WIDTH = 0.5f;
  // Must stay below the attack cooldown (0.5s at attackSpeed 1.0). A longer-lived stab is still
  // alive when the next one spawns, and an enemy entering the zone takes a hit from every live
  // hitbox at once -- an overlap that grows with attack-speed buffs.
  private static final float LIFETIME = 0.2f;
  private static final float GAP = 0.05f;
  private static final float SPRITE_SIZE = 0.8f;
  // Anchor the grip toward the wielder so the blade reads as a thrust rather than a floating edge.
  private static final float SPRITE_PULL_IN = -0.35f;
  // The pack draws every weapon as an icon pointing up and to the LEFT: hilt at the bottom-right,
  // tip at the top-left, a measured 135 degrees. Correcting it here keeps the pixel art crisp,
  // where rotating the PNG off-axis would resample and soften it.
  private static final float SPRITE_ANGLE_OFFSET = -135f;

  // Heavy flurry: slash, slash, finishing stab.
  private static final float SLASH_ARC_DEGREES = 70f; // how far each slash swings
  private static final float SLASH_LIFETIME = 0.12f; // how long each slash takes
  // Time between strikes. Must stay above SLASH_LIFETIME so one strike's hitbox is gone before the
  // next spawns, otherwise an enemy could be hit by two strikes at once.
  private static final float STRIKE_INTERVAL = 0.15f;
  private static final float FINISHER_LIFETIME = 0.15f;
  private static final float FINISHER_DAMAGE_RATIO = 2f; // finisher damage relative to a slash
  private static final float FINISHER_KNOCKBACK_SCALE = 1.5f; // relative to the weapon knockback
  private static final float FLURRY_DURATION = 2f * STRIKE_INTERVAL + FINISHER_LIFETIME;

  /** Flurry strikes waiting to spawn, each with the seconds left before it does. */
  private final List<PendingStrike> pendingStrikes = new ArrayList<>();

  /**
   * Spawn this weapon's hitbox. Melee implementations should pass the wielder as hitbox owner;
   * projectile splash should omit owner.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   * @require origin != null &amp;&amp; direction != null
   */
  @Override
  protected void createAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
    spawnStab(origin, direction, resolveHitboxDamage(), stats.getKnockback(), LIFETIME);
  }

  @Override
  protected boolean hasHeavyAttack() {
    return true;
  }

  @Override
  protected float getHeavyAttackDuration() {
    return FLURRY_DURATION;
  }

  /**
   * Start the flurry: the first slash spawns now and the rest are queued. The aim is locked to the
   * direction at the moment of the attack, and damage is resolved up front so the combo cannot
   * change halfway through.
   *
   * @param origin world position of the attack
   * @param direction facing or aim direction
   */
  @Override
  protected void createHeavyAttack(Vector2 origin, Vector2 direction) {
    WeaponStatsComponent stats = entity.getComponent(WeaponStatsComponent.class);
    Vector2 aim = snapToAxis(direction);
    float baseAngle = aim.angleDeg();
    float halfArc = SLASH_ARC_DEGREES / 2f;
    int slashDamage = resolveHeavyHitboxDamage();
    int finisherDamage = resolveHeavyHitboxDamage(FINISHER_DAMAGE_RATIO);
    float finisherKnockback = stats.getKnockback() * FINISHER_KNOCKBACK_SCALE;

    // Replace any leftovers rather than stacking a second combo on top.
    pendingStrikes.clear();
    spawnSlash(origin, aim, baseAngle + halfArc, baseAngle - halfArc, slashDamage);
    pendingStrikes.add(
        new PendingStrike(
            STRIKE_INTERVAL,
            () ->
                spawnSlash(
                    entity.getCenterPosition(),
                    aim,
                    baseAngle - halfArc,
                    baseAngle + halfArc,
                    slashDamage)));
    pendingStrikes.add(
        new PendingStrike(
            2f * STRIKE_INTERVAL,
            () ->
                spawnStab(
                    entity.getCenterPosition(),
                    aim,
                    finisherDamage,
                    finisherKnockback,
                    FINISHER_LIFETIME)));
  }

  /** Counts down queued flurry strikes and spawns each one when its time comes. */
  @Override
  public void update() {
    if (pendingStrikes.isEmpty()) {
      return;
    }
    float dt = Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime());
    List<Runnable> due = new ArrayList<>();
    pendingStrikes.removeIf(
        strike -> {
          strike.delay -= dt;
          if (strike.delay <= 0f) {
            due.add(strike.spawn);
            return true;
          }
          return false;
        });
    // Spawn once the entity loop finishes, so a new hitbox is never registered mid-iteration.
    for (Runnable spawn : due) {
      ServiceLocator.getEntityService().runAfterUpdate(spawn);
    }
  }

  /**
   * Disabling the knife (e.g. switching weapons) cancels any flurry in progress. Disabled
   * components stop updating, so without this the queued strikes would fire when the knife is
   * re-equipped.
   *
   * @param enabled whether the knife is equipped
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      pendingStrikes.clear();
    }
  }

  /**
   * Spawn a stab: a hitbox straight out from the wielder along the aim axis.
   *
   * @param origin world position of the attack
   * @param direction aim direction; snapped to the nearest axis
   * @param damage damage dealt to each enemy hit
   * @param knockback knockback impulse applied to each enemy hit
   * @param lifetime seconds the stab stays active
   */
  private void spawnStab(
      Vector2 origin, Vector2 direction, int damage, float knockback, float lifetime) {
    Vector2 cardinalDir = snapToAxis(direction);
    float reach = reachFor(cardinalDir);
    Vector2 offset = cardinalDir.cpy().scl(reach);

    HitboxSpec spec =
        baseSpec(origin, cardinalDir, damage, knockback, lifetime)
            .localOffset(offset)
            .rotation(cardinalDir.angleDeg());
    ServiceLocator.getEntityService().register(HitboxFactory.createHitbox(spec));
  }

  /**
   * Spawn a slash: the blade swept through an arc in front of the wielder, without knockback so the
   * enemy stays in reach of the rest of the flurry.
   *
   * @param origin world position of the attack
   * @param aim aim direction, already snapped to an axis
   * @param fromAngle angle the slash starts at, in degrees
   * @param toAngle angle the slash ends at, in degrees
   * @param damage damage dealt to each enemy hit
   */
  private void spawnSlash(Vector2 origin, Vector2 aim, float fromAngle, float toAngle, int damage) {
    float reach = reachFor(aim);
    Vector2 offset = new Vector2(reach, 0f).setAngleDeg(fromAngle);

    HitboxSpec spec =
        baseSpec(origin, aim, damage, 0f, SLASH_LIFETIME).localOffset(offset).rotation(fromAngle);
    Entity hitbox = HitboxFactory.createHitbox(spec);
    hitbox.addComponent(new SweepComponent(SLASH_LIFETIME, fromAngle, toAngle, reach));
    ServiceLocator.getEntityService().register(hitbox);
  }

  /** Hitbox settings shared by every knife strike: blade size, layers, owner and sprite. */
  private HitboxSpec baseSpec(
      Vector2 origin, Vector2 aim, int damage, float knockback, float lifetime) {
    return new HitboxSpec()
        .position(origin)
        .size(bladeSize(aim))
        .lifetime(lifetime)
        .layer(PhysicsLayer.WEAPON)
        .targetLayer(PhysicsLayer.NPC)
        .damage(damage)
        .knockback(knockback)
        .owner(entity)
        .texture(resolveTexture())
        .visualSource(entity)
        .visualScale(new Vector2(SPRITE_SIZE, SPRITE_SIZE))
        .visualOffset(new Vector2(SPRITE_PULL_IN, 0f))
        .rotationOffset(SPRITE_ANGLE_OFFSET);
  }

  /**
   * @return {@link #UPGRADED_TEXTURE} once upgraded, otherwise {@link #TEXTURE}
   */
  String resolveTexture() {
    return isUpgraded() ? UPGRADED_TEXTURE : TEXTURE;
  }

  /** Snap a direction to the nearest axis: right, left, up or down. */
  private static Vector2 snapToAxis(Vector2 direction) {
    Vector2 dir = direction.cpy().nor();
    return isHorizontal(dir)
        ? new Vector2(Math.signum(dir.x), 0f)
        : new Vector2(0f, Math.signum(dir.y));
  }

  private static boolean isHorizontal(Vector2 dir) {
    return Math.abs(dir.x) >= Math.abs(dir.y);
  }

  /** Blade hitbox, lying along the aim axis. */
  private static Vector2 bladeSize(Vector2 aim) {
    return isHorizontal(aim)
        ? new Vector2(BLADE_LENGTH, BLADE_WIDTH)
        : new Vector2(BLADE_WIDTH, BLADE_LENGTH);
  }

  /** Distance from the wielder's centre to the blade's centre, so the blade clears the wielder. */
  private float reachFor(Vector2 aim) {
    boolean horizontal = isHorizontal(aim);
    Vector2 wielderHalfSize = entity.getScale().cpy().scl(0.5f);
    float wielderHalfExtent = horizontal ? wielderHalfSize.x : wielderHalfSize.y;
    Vector2 size = bladeSize(aim);
    float hitboxHalfExtent = horizontal ? size.x / 2f : size.y / 2f;
    return wielderHalfExtent + hitboxHalfExtent + GAP;
  }

  /** A queued flurry strike. */
  private static class PendingStrike {
    private float delay;
    private final Runnable spawn;

    PendingStrike(float delay, Runnable spawn) {
      this.delay = delay;
      this.spawn = spawn;
    }
  }
}
