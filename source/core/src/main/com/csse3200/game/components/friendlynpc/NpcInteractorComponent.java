package com.csse3200.game.components.friendlynpc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.InteractableNpcConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Player-side half of the friendly NPC system.
 *
 * Lives on the player so it survives room changes and runs at most one NpcInteractionSequence at a time,
 * remembers which NPC interactions have been completed (for once-only NPCs and prerequisites),
 * forwards the dialogue/cutscene "finished" events to the running sequence, and
 * locks and unlocks player controls through PlayerActions.
 * 
 * Listeners are registered once here rather than per interaction, because EventHandler cannot remove listeners.
 */
public class NpcInteractorComponent extends Component {
  private final Set<String> completedNpcIds = new HashSet<>();
  private NpcInteractionSequence activeSequence;
  private String activeNpcId;
  private Entity activeNpc;

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(
            NpcInteractionEvents.DIALOGUE_FINISHED,
            (String dialogueId) -> {
              if (activeSequence != null) activeSequence.onDialogueFinished(dialogueId);
            });
    entity
        .getEvents()
        .addListener(
            NpcInteractionEvents.CUTSCENE_FINISHED,
            (String cutsceneId) -> {
              if (activeSequence != null) activeSequence.onCutsceneFinished(cutsceneId);
            });
    entity
        .getEvents()
        .addListener(
            NpcInteractionEvents.DIALOGUE_CUTSCENE_CUE,
            (String dialogueId) -> {
              if (activeSequence != null) activeSequence.onCutsceneCue(dialogueId);
            });
    entity.getEvents().addListener("entityDied", this::cancelInteraction);
  }

  /**
   * Starts an interaction with an NPC.
   *
   * @param npc the NPC entity, passed along to the dialogue and cutscene systems
   * @param config the NPC's definition
   * @return false if another interaction is already running
   */
  public boolean beginInteraction(Entity npc, InteractableNpcConfig config) {
    if (activeSequence != null) {
      return false;
    }
    NpcInteractionSequence sequence = 
      new NpcInteractionSequence(config, new Output(npc), 
      () -> onSequenceFinished(config.id));
    activeSequence = sequence;
    activeNpcId = config.id;
    activeNpc = npc;
    entity.getEvents().trigger(NpcInteractionEvents.INTERACTION_STARTED, config.id, npc);
    sequence.start();
    return true;
  }

  /** Abandons the running interaction, if any, without marking it completed. */
  public void cancelInteraction() {
    if (activeSequence == null) {
      return;
    }
    NpcInteractionSequence sequence = activeSequence;
    String npcId = activeNpcId;
    Entity npc = activeNpc;
    clearActive();
    // releases the movement lock
    sequence.cancel();
    entity.getEvents().trigger(NpcInteractionEvents.INTERACTION_CANCELLED, npcId, npc);
  }

  /**
   * @return true while an NPC interaction is running
   */
  public boolean isInteracting() {
    return activeSequence != null;
  }

  /**
   * @return id of the NPC currently being interacted with, or null
   */
  public String getActiveNpcId() {
    return activeNpcId;
  }

  /**
   * @param npcId
   * @return true if an interaction with that NPC has been completed
   */
  public boolean hasCompleted(String npcId) {
    return completedNpcIds.contains(npcId);
  }

  /**
   * Records an interaction as completed without playing it, e.g. when loading a save
   *
   * @param npcId
   */
  public void markCompleted(String npcId) {
    completedNpcIds.add(npcId);
  }

  /**
   * @return read-only view of every completed NPC id
   */
  public Set<String> getCompletedNpcIds() {
    return Collections.unmodifiableSet(completedNpcIds);
  }

  @Override
  public void dispose() {
    cancelInteraction();
    super.dispose();
  }

  private void onSequenceFinished(String npcId) {
    Entity npc = activeNpc;
    completedNpcIds.add(npcId);
    clearActive();
    entity.getEvents().trigger(NpcInteractionEvents.INTERACTION_FINISHED, npcId, npc);
  }

  private void clearActive() {
    activeSequence = null;
    activeNpcId = null;
    activeNpc = null;
  }

  private void setControlsLocked(boolean locked) {
    PlayerActions actions = entity.getComponent(PlayerActions.class);
    if (actions != null) {
      actions.setControlsLocked(this, locked);
    }
  }

  /** Turns sequence requests into player events and control locks */
  private class Output implements NpcInteractionSequence.Output {
    private final Entity npc;

    Output(Entity npc) {
      this.npc = npc;
    }

    @Override
    public void startDialogue(String dialogueId) {
      entity.getEvents().trigger(NpcInteractionEvents.START_DIALOGUE, dialogueId, npc);
    }

    @Override
    public void startCutscene(String cutsceneId) {
      entity.getEvents().trigger(NpcInteractionEvents.START_CUTSCENE, cutsceneId, npc);
    }

    @Override
    public void setMovementLocked(boolean locked) {
      setControlsLocked(locked);
    }
  }
}