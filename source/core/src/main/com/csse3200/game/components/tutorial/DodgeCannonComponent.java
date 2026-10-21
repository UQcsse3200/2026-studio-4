package com.csse3200.game.components.tutorial;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.TutorialPropFactory;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fires harmless orbs straight down from a cannon so the player can practise dodging and dashing.
 *
 * <p>An orb that reaches the player is a "hit" and an orb that passes close to them without
 * touching is a "dodge"; each is announced with {@link #RESULT}. Orbs are plain render entities
 * moved here by a distance check, so they cost the player nothing and cannot get stuck on walls.
 */
public class DodgeCannonComponent extends Component {
  /** Triggered on the cannon with {@code (Boolean dodged)}: true for a dodge, false for a hit. */
  public static final String RESULT = "dodgeResult";

  private static final float ORB_SPEED = 3f;
  private static final float TILE_SIZE = 0.5f;
  private static final float HIT_RADIUS = 0.3f;
  // An orb only counts as dodged if the player was actually near its path, not just anywhere.
  private static final float NEAR_MISS_RADIUS = 1.1f;

  private final Entity target;
  private final Supplier<Entity> orbFactory;
  private final float interval;
  private final float rangeUnits;
  private final List<Orb> orbs = new ArrayList<>();
  private float untilNextShot;

  /**
   * @param target the entity the orbs are aimed at, normally the player
   * @param interval seconds between shots
   * @param delay seconds before the first shot
   * @param rangeTiles how many tiles an orb travels before it fades
   */
  public DodgeCannonComponent(Entity target, float interval, float delay, float rangeTiles) {
    this(target, interval, delay, rangeTiles, TutorialPropFactory::createOrb);
  }

  /** Package-private so tests can fire orbs that need no textures. */
  DodgeCannonComponent(
      Entity target, float interval, float delay, float rangeTiles, Supplier<Entity> orbFactory) {
    this.target = target;
    this.orbFactory = orbFactory;
    this.interval = interval;
    this.untilNextShot = delay;
    this.rangeUnits = rangeTiles * TILE_SIZE;
  }

  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    float delta = time == null ? 0f : time.getDeltaTime();
    if (delta <= 0f) {
      return;
    }
    untilNextShot -= delta;
    if (untilNextShot <= 0f) {
      untilNextShot += interval;
      fire();
    }
    moveOrbs(delta);
  }

  private void fire() {
    Entity orb = orbFactory.get();
    Vector2 mouth =
        new Vector2(
            entity.getCenterPosition().x - orb.getScale().x / 2f,
            entity.getPosition().y - orb.getScale().y / 2f);
    orb.setPosition(mouth);
    ServiceLocator.getEntityService().register(orb);
    orbs.add(new Orb(orb));
  }

  private void moveOrbs(float delta) {
    Iterator<Orb> it = orbs.iterator();
    while (it.hasNext()) {
      Orb orb = it.next();
      Vector2 position = orb.entity.getPosition();
      position.y -= ORB_SPEED * delta;
      orb.entity.setPosition(position);
      orb.travelled += ORB_SPEED * delta;

      float distance = orb.entity.getCenterPosition().dst(target.getCenterPosition());
      orb.closest = Math.min(orb.closest, distance);
      if (distance <= HIT_RADIUS) {
        finish(orb, false);
        it.remove();
      } else if (orb.travelled >= rangeUnits) {
        if (orb.closest <= NEAR_MISS_RADIUS) {
          finish(orb, true);
        } else {
          ServiceLocator.getEntityService().scheduleDisposal(orb.entity);
        }
        it.remove();
      }
    }
  }

  private void finish(Orb orb, boolean dodged) {
    ServiceLocator.getEntityService().scheduleDisposal(orb.entity);
    entity.getEvents().trigger(RESULT, dodged);
  }

  @Override
  public void dispose() {
    for (Orb orb : orbs) {
      orb.entity.dispose();
    }
    orbs.clear();
    super.dispose();
  }

  private static final class Orb {
    private final Entity entity;
    private float travelled;
    private float closest = Float.MAX_VALUE;

    private Orb(Entity entity) {
      this.entity = entity;
    }
  }
}
