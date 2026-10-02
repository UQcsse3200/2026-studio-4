package com.csse3200.game.components.friendlynpc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InteractionPrompt;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * NPC-side half of the friendly NPC system: makes an entity something the player can walk up to and
 * interact with.
 *
 * <p>Every NPC uses this same component what differs between NPCs (name, range, dialogue, cutscene,
 * conditions) comes from its InteractableNpcConfig. The player-side work of running the interaction
 * is done by NpcInteractorComponent.
 */
public class NpcInteractableComponent extends Component {
  /** Whether the player can interact right now, and if not, why. */
  public enum Availability {
    AVAILABLE,
    /** Another NPC interaction is already running, or the player cannot interact at all. */
    BUSY,
    /** A once-only interaction that has already been completed. */
    ALREADY_COMPLETED,
    /** The room is not cleared, or a prerequisite NPC has not been talked to yet. */
    CONDITIONS_UNMET
  }

  private final InteractableNpcConfig config;
  private BooleanSupplier roomCleared = () -> true;
  private boolean indicatorVisible;

  /**
   * @param config definition for this NPC
   */
  public NpcInteractableComponent(InteractableNpcConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  /**
   * @param roomCleared reports whether the NPC's room has no enemies left
   */
  public void setRoomClearedSupplier(BooleanSupplier roomCleared) {
    this.roomCleared = Objects.requireNonNull(roomCleared);
  }

  public InteractableNpcConfig getConfig() {
    return config;
  }

  /**
   * @param player the player entity
   * @return distance between the player's centre and this NPC's centre
   */
  public float distanceTo(Entity player) {
    return entity.getCenterPosition().dst(player.getCenterPosition());
  }

  /**
   * @param player the player entity
   * @return true if the player is close enough to interact
   */
  public boolean isInRange(Entity player) {
    return distanceTo(player) <= config.interactionRange;
  }

  /**
   * @param player the player entity
   * @return whether the player can start this NPC's interaction now
   */
  public Availability getAvailability(Entity player) {
    NpcInteractorComponent interactor = player.getComponent(NpcInteractorComponent.class);
    if (interactor == null || interactor.isInteracting()) {
      return Availability.BUSY;
    }
    if (config.once && interactor.hasCompleted(config.id)) {
      return Availability.ALREADY_COMPLETED;
    }
    if (config.requiresRoomCleared && !roomCleared.getAsBoolean()) {
      return Availability.CONDITIONS_UNMET;
    }
    for (String required : config.requiresCompleted) {
      if (!interactor.hasCompleted(required)) {
        return Availability.CONDITIONS_UNMET;
      }
    }
    return Availability.AVAILABLE;
  }

  /**
   * HUD text while the player is in range. Range is checked by the caller
   *
   * @param player the player entity
   * @return prompt text, or null if nothing should be shown
   */
  public String getPrompt(Entity player) {
    return switch (getAvailability(player)) {
      case AVAILABLE -> InteractionPrompt.forNpc(config.promptVerb, config.name);
      case ALREADY_COMPLETED -> blankToNull(config.completedMessage);
      case CONDITIONS_UNMET -> blankToNull(config.unavailableMessage);
      case BUSY -> null;
    };
  }

  /**
   * Starts this NPC's interaction if it is available. Range is checked by the caller
   *
   * @param player the player entity
   * @return true if the interaction started
   */
  public boolean interact(Entity player) {
    if (getAvailability(player) != Availability.AVAILABLE) {
      return false;
    }
    setIndicatorVisible(false);
    return player.getComponent(NpcInteractorComponent.class).beginInteraction(entity, config);
  }

  /**
   * Shows or hides the interaction indicator
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

  public boolean isIndicatorVisible() {
    return indicatorVisible;
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text;
  }
}
