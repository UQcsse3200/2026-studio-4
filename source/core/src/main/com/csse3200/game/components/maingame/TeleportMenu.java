package com.csse3200.game.components.maingame;

import com.badlogic.gdx.utils.TimeUtils;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.friendlynpc.NpcInteractableComponent;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import com.csse3200.game.entities.configs.InteractableNpcConfig.TeleportOffer;
import com.csse3200.game.rooms.RoomSelector;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * What the player is being asked when an NPC offers to send them to another room, and nothing about
 * how it looks. The display only draws what this says and the input component only calls into it,
 * so the whole flow can be tested without a GL context.
 *
 * <p>An NPC makes the offer by having a {@link TeleportOffer} in its config. The menu opens on
 * {@link NpcInteractionEvents#INTERACTION_FINISHED} rather than when the dialogue starts, so the
 * NPC says their piece first and the question never shares the screen with the dialogue box. While
 * it is open it holds a control lock of its own, which outlives the one the interaction took.
 *
 * <p>Accepting asks {@link RoomSelector} to move the player, using {@link
 * RoomSelector#TELEPORT_REQUEST}.
 */
public class TeleportMenu extends Component implements ModalMenu {
  /** The two rows on offer: going ahead, then staying put. */
  public static final int ACCEPT_ROW = 0;

  public static final int DECLINE_ROW = 1;

  /** Triggered with no arguments when the menu opens. */
  public static final String MENU_OPENED = "teleportMenuOpened";

  /** Triggered with no arguments when the menu closes, however it closed. */
  public static final String MENU_CLOSED = "teleportMenuClosed";

  /**
   * How long after opening the menu ignores confirming. Players tend to mash through a dialogue,
   * and a press meant for its last line must not also accept the offer.
   */
  static final long CONFIRM_GRACE_MS = 400;

  private final Entity player;
  private final LongSupplier clockMillis;
  private TeleportOffer offer;
  private int selected;
  private long openedAtMillis;

  /**
   * @param player the entity the NPC interaction events and room requests live on
   */
  public TeleportMenu(Entity player) {
    this(player, TimeUtils::millis);
  }

  /** Package-private so tests can move the clock instead of sleeping through the grace period. */
  TeleportMenu(Entity player, LongSupplier clockMillis) {
    this.player = Objects.requireNonNull(player);
    this.clockMillis = clockMillis;
  }

  @Override
  public void create() {
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_FINISHED,
            (String npcId, Entity npc) -> offerFrom(npc));
    // Dying with the menu open would otherwise strand the control lock.
    player.getEvents().addListener("entityDied", this::close);
  }

  private void offerFrom(Entity npc) {
    NpcInteractableComponent interactable =
        npc == null ? null : npc.getComponent(NpcInteractableComponent.class);
    InteractableNpcConfig config = interactable == null ? null : interactable.getConfig();
    if (config != null && config.teleportOffer != null) {
      open(config.teleportOffer);
    }
  }

  /**
   * Opens the menu on an offer, with going ahead highlighted.
   *
   * @return false if it was already open
   */
  public boolean open(TeleportOffer offer) {
    if (this.offer != null) {
      return false;
    }
    this.offer = Objects.requireNonNull(offer);
    selected = ACCEPT_ROW;
    openedAtMillis = clockMillis.getAsLong();
    setControlsLocked(true);
    stopPlayerWalking();
    entity.getEvents().trigger(MENU_OPENED);
    return true;
  }

  /** Closes the menu and gives the player back their controls. Closing a shut menu does nothing. */
  @Override
  public void close() {
    if (offer == null) {
      return;
    }
    offer = null;
    setControlsLocked(false);
    entity.getEvents().trigger(MENU_CLOSED);
  }

  /**
   * Moves the highlight, wrapping at both ends so holding a direction never dead-ends.
   *
   * @param delta rows to move by, usually -1 or 1
   */
  @Override
  public void moveSelection(int delta) {
    if (offer != null) {
      selected = Math.floorMod(selected + delta, 2);
    }
  }

  /**
   * Acts on the highlighted row and closes. Going ahead asks for the teleport once the menu has
   * released its control lock, so the player arrives free to move. Ignored for a moment after the
   * menu opens, see {@link #CONFIRM_GRACE_MS}.
   *
   * @return whether the player is being teleported
   */
  @Override
  public boolean confirm() {
    if (offer == null || clockMillis.getAsLong() - openedAtMillis < CONFIRM_GRACE_MS) {
      return false;
    }
    TeleportOffer chosen = offer;
    boolean teleport = selected == ACCEPT_ROW;
    close();
    if (teleport) {
      player.getEvents().trigger(RoomSelector.TELEPORT_REQUEST, chosen.roomId, chosen.entryPointId);
    }
    return teleport;
  }

  /**
   * @return whether the menu is on screen and taking input
   */
  @Override
  public boolean isOpen() {
    return offer != null;
  }

  /**
   * @return the offer being shown, or null when the menu is shut
   */
  public TeleportOffer getOffer() {
    return offer;
  }

  /**
   * @return the highlighted row, {@link #ACCEPT_ROW} or {@link #DECLINE_ROW}
   */
  @Override
  public int getSelectedIndex() {
    return selected;
  }

  @Override
  public void dispose() {
    close();
    super.dispose();
  }

  /** Drops any walk in progress, since the menu swallows the key releases that would end it. */
  private void stopPlayerWalking() {
    player.getEvents().trigger("walkStop");
    player.getEvents().trigger("resetMovementInput");
  }

  private void setControlsLocked(boolean locked) {
    PlayerActions actions = player.getComponent(PlayerActions.class);
    if (actions != null) {
      actions.setControlsLocked(this, locked);
    }
  }
}
