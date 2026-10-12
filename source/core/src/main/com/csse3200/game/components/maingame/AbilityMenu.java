package com.csse3200.game.components.maingame;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.friendlynpc.NpcInteractionEvents;
import com.csse3200.game.components.player.AbilityAttunementComponent;
import com.csse3200.game.components.player.PlayerAbilitiesComponent;
import com.csse3200.game.components.player.PlayerAbility;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * What the player is choosing between while Hecate's menu is open, and nothing about how it looks.
 *
 * <p>Keeping the state here rather than in the display means the whole flow can be tested without a
 * GL context: the display only draws what this says, and the menu's input component only calls into
 * it.
 *
 * <p>It opens on {@link NpcInteractionEvents#INTERACTION_FINISHED} rather than on the dialogue
 * starting, so Hecate says her piece first and the menu never shares the screen with a dialogue
 * box. While it is open it holds a control lock of its own, which is separate from the lock the
 * interaction itself took and so outlives it.
 */
public class AbilityMenu extends Component {
  /** The friendly NPC whose interaction opens this menu. */
  public static final String NPC_ID = "hecate";

  /** Triggered with no arguments when the menu opens. */
  public static final String MENU_OPENED = "abilityMenuOpened";

  /** Triggered with no arguments when the menu closes, however it closed. */
  public static final String MENU_CLOSED = "abilityMenuClosed";

  private final Entity player;
  private final List<PlayerAbility> options = new ArrayList<>();
  private boolean open;
  private int selected;

  /**
   * Builds a menu for one player.
   *
   * @param player the entity the abilities and the NPC interaction events live on
   */
  public AbilityMenu(Entity player) {
    this.player = Objects.requireNonNull(player);
  }

  @Override
  public void create() {
    player
        .getEvents()
        .addListener(
            NpcInteractionEvents.INTERACTION_FINISHED,
            (String npcId, Entity npc) -> {
              if (NPC_ID.equals(npcId)) {
                open();
              }
            });
    // Dying with the menu open would otherwise strand the control lock.
    player.getEvents().addListener("entityDied", this::close);
  }

  /**
   * Opens the menu on the abilities the player has, starting on whichever is already attuned.
   *
   * @return false if it was already open, or there is nothing to choose between
   */
  public boolean open() {
    PlayerAbilitiesComponent abilities = player.getComponent(PlayerAbilitiesComponent.class);
    if (open || abilities == null || abilities.getRegisteredAbilities().isEmpty()) {
      return false;
    }
    options.clear();
    options.addAll(abilities.getRegisteredAbilities());
    selected = Math.max(0, indexOfAttuned());
    open = true;
    setControlsLocked(true);
    stopPlayerWalking();
    entity.getEvents().trigger(MENU_OPENED);
    return true;
  }

  /** Closes the menu and gives the player back their controls. Closing a shut menu does nothing. */
  public void close() {
    if (!open) {
      return;
    }
    open = false;
    setControlsLocked(false);
    entity.getEvents().trigger(MENU_CLOSED);
  }

  /**
   * Moves the highlight, wrapping at both ends so holding a direction never dead-ends.
   *
   * @param delta rows to move by, usually -1 or 1
   */
  public void moveSelection(int delta) {
    if (open && !options.isEmpty()) {
      selected = Math.floorMod(selected + delta, options.size());
    }
  }

  /**
   * Attunes the highlighted ability and closes. The menu closes either way, so a refusal cannot
   * leave the player stuck behind it.
   *
   * @return whether the ability was actually attuned
   */
  public boolean confirm() {
    if (!open) {
      return false;
    }
    AbilityAttunementComponent attunement = player.getComponent(AbilityAttunementComponent.class);
    PlayerAbility choice = getSelected();
    boolean attuned = attunement != null && choice != null && attunement.attune(choice.getClass());
    close();
    return attuned;
  }

  /**
   * @return whether the menu is on screen and taking input
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Returns the rows on offer, snapshotted when the menu opened.
   *
   * @return what the player is choosing between, in the order abilities were registered
   */
  public List<PlayerAbility> getOptions() {
    return Collections.unmodifiableList(options);
  }

  /**
   * @return the highlighted row
   */
  public int getSelectedIndex() {
    return selected;
  }

  /**
   * Returns what confirming would attune.
   *
   * @return the highlighted ability, or null when the menu is empty
   */
  public PlayerAbility getSelected() {
    return selected >= 0 && selected < options.size() ? options.get(selected) : null;
  }

  /**
   * Marks the row the player already carries, so a visit shows what they have.
   *
   * @param ability the ability on the row
   * @return whether it is the one the player currently carries
   */
  public boolean isAttuned(PlayerAbility ability) {
    AbilityAttunementComponent attunement = player.getComponent(AbilityAttunementComponent.class);
    return ability != null && attunement != null && attunement.isAttuned(ability.getClass());
  }

  @Override
  public void dispose() {
    close();
    super.dispose();
  }

  private int indexOfAttuned() {
    for (int i = 0; i < options.size(); i++) {
      if (isAttuned(options.get(i))) {
        return i;
      }
    }
    return 0;
  }

  /**
   * Drops any walk that was in progress when the menu opened. The menu swallows key releases while
   * it is open, so a movement key let go behind it never reaches the player's input, and without
   * this the player would carry on walking once the menu closed (#266).
   */
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
