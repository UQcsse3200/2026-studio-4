package com.csse3200.game.components.miniboss.cerberus;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffectsControllerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class CerberusLayeredRenderComponent extends RenderComponent {

  public enum Part {
    BODY("body"),
    LEFT("left"),
    MIDDLE("middle"),
    RIGHT("right");

    private final String prefix;

    Part(String prefix) {
      this.prefix = prefix;
    }
  }

  public enum Action {
    IDLE("idle", true),
    MOVE("move", true),
    LUNGE("lunge", false),
    CAST("cast", false);

    private final String suffix;
    private final boolean looping;

    Action(String suffix, boolean looping) {
      this.suffix = suffix;
      this.looping = looping;
    }
  }

  private static final int FRAME_COUNT = 4;
  private static final float FRAME_DURATION = 0.15f;
  private static final float ANIMATION_DURATION = FRAME_COUNT * FRAME_DURATION;

  private final EnumMap<Part, CombatStatsComponent> headStats = new EnumMap<>(Part.class);

  private final EnumMap<Part, EnumMap<Action, TextureRegion[]>> animations =
      new EnumMap<>(Part.class);

  private final EnumMap<Part, Action> currentActions = new EnumMap<>(Part.class);

  private final EnumMap<Part, Float> elapsedTimes = new EnumMap<>(Part.class);

  private final EnumSet<Part> visibleParts = EnumSet.allOf(Part.class);

  public CerberusLayeredRenderComponent(TextureAtlas atlas) {
    for (Part part : Part.values()) {
      EnumMap<Action, TextureRegion[]> partAnimations = new EnumMap<>(Action.class);

      for (Action action : Action.values()) {
        String name = part.prefix + "_" + action.suffix;
        TextureRegion[] actionFrames = new TextureRegion[FRAME_COUNT];

        for (int index = 0; index < FRAME_COUNT; index++) {
          TextureRegion frame = atlas.findRegion(name, index);
          if (frame == null) {
            throw new IllegalArgumentException(
                "Missing Cerberus atlas region: " + name + " index " + index);
          }
          actionFrames[index] = frame;
        }

        partAnimations.put(action, actionFrames);
      }

      animations.put(part, partAnimations);
      currentActions.put(part, Action.IDLE);
      elapsedTimes.put(part, 0f);
    }
  }

  /** Starts an animation without changing any other layer. */
  public void play(Part part, Action action) {
    if (!isPartVisible(part)) {
      return;
    }

    currentActions.put(part, action);
    elapsedTimes.put(part, 0f);
  }

  public Action getAction(Part part) {
    return currentActions.get(part);
  }

  public TextureRegion getCurrentFrame(Part part) {
    int index = Math.min((int) (elapsedTimes.get(part) / FRAME_DURATION), FRAME_COUNT - 1);

    return animations.get(part).get(currentActions.get(part))[index];
  }

  public void setPartVisible(Part part, boolean visible) {
    if (visible) {
      visibleParts.add(part);
    } else {
      visibleParts.remove(part);
    }
  }

  public boolean isPartVisible(Part part) {
    return visibleParts.contains(part);
  }

  /**
   * Connects visual head layers to their existing independent health pools. The body remains
   * visible until the main entity is disposed.
   */
  public void bindHeads(Entity leftHead, Entity middleHead, Entity rightHead) {
    headStats.put(Part.LEFT, requireStats(leftHead));
    headStats.put(Part.MIDDLE, requireStats(middleHead));
    headStats.put(Part.RIGHT, requireStats(rightHead));
    refreshHeadVisibility();
  }

  private CombatStatsComponent requireStats(Entity head) {
    CombatStatsComponent stats = head.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      throw new IllegalArgumentException("Each Cerberus head requires CombatStatsComponent");
    }
    return stats;
  }

  @Override
  public void update() {
    update(ServiceLocator.getTimeSource().getDeltaTime());
  }

  /** Advances animation independently of rendering, including glow passes. */
  public void update(float delta) {
    refreshHeadVisibility();

    if (!Float.isFinite(delta) || delta <= 0f) {
      return;
    }

    if (StatusEffectsControllerComponent.isImmobilised(entity)) {
      return;
    }

    for (Part part : Part.values()) {
      if (isPartVisible(part)) {
        advanceAnimation(part, delta);
      }
    }
  }

  private void advanceAnimation(Part part, float delta) {
    Action action = currentActions.get(part);
    float elapsed = elapsedTimes.get(part);

    if (action.looping) {
      elapsedTimes.put(part, (elapsed + delta % ANIMATION_DURATION) % ANIMATION_DURATION);
      return;
    }

    float remaining = ANIMATION_DURATION - elapsed;
    if (delta >= remaining) {
      currentActions.put(part, Action.IDLE);
      elapsedTimes.put(part, (delta - remaining) % ANIMATION_DURATION);
    } else {
      elapsedTimes.put(part, elapsed + delta);
    }
  }

  private void refreshHeadVisibility() {
    for (Map.Entry<Part, CombatStatsComponent> entry : headStats.entrySet()) {
      if (entry.getValue().isDead()) {
        setPartVisible(entry.getKey(), false);
      }
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();

    // Layer offsets are already included in each transparent frame.
    for (Part part : Part.values()) {
      if (visibleParts.contains(part)) {
        batch.draw(getCurrentFrame(part), position.x, position.y, scale.x, scale.y);
      }
    }
  }
}
