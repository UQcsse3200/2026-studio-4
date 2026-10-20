package com.csse3200.game.components.tutorial;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.entities.Entity;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Makes a prop something the player can walk up to and use with E: read a sign, take a potion.
 *
 * <p>The prop-side counterpart of NpcInteractableComponent, deliberately much smaller: props have
 * no dialogue sequence or availability rules, just something to do when used. {@link
 * TutorialPropManagerComponent} finds the nearest one in range, shows its indicator and prompt, and
 * routes E to it.
 */
public class PropInteractableComponent extends Component {
  private static final float DEFAULT_RANGE = 1.3f;

  private final String prompt;
  private final float range;
  private final Consumer<Entity> action;
  private boolean indicatorVisible;

  /**
   * @param prompt HUD text while the player is in range, e.g. "Press E — Read Notice Board"
   * @param action what using the prop does; it is given the player
   */
  public PropInteractableComponent(String prompt, Consumer<Entity> action) {
    this(prompt, DEFAULT_RANGE, action);
  }

  /**
   * @param range distance, in world units between centres, at which the player can use the prop
   */
  public PropInteractableComponent(String prompt, float range, Consumer<Entity> action) {
    this.prompt = Objects.requireNonNull(prompt);
    this.range = range;
    this.action = Objects.requireNonNull(action);
  }

  /**
   * @param player the player entity
   * @return distance between the player's centre and this prop's centre
   */
  public float distanceTo(Entity player) {
    return entity.getCenterPosition().dst(player.getCenterPosition());
  }

  /**
   * @param player the player entity
   * @return true if the player is close enough to use the prop
   */
  public boolean isInRange(Entity player) {
    return distanceTo(player) <= range;
  }

  public String getPrompt() {
    return prompt;
  }

  /**
   * Uses the prop.
   *
   * @param player the player entity
   */
  public void interact(Entity player) {
    action.accept(player);
  }

  /**
   * Shows or hides the interaction indicator above the prop.
   *
   * @param visible whether the indicator should be visible
   */
  public void setIndicatorVisible(boolean visible) {
    if (visible == indicatorVisible) {
      return;
    }
    indicatorVisible = visible;
    entity
        .getEvents()
        .trigger(
            visible ? NpcInteractionEvents.INDICATOR_SHOWN : NpcInteractionEvents.INDICATOR_HIDDEN);
  }
}
