package com.csse3200.game.components.friendlynpc;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TEMPORARY stand-in for the dialogue and cutscene systems, which are being built separately.
 *
 * Answers NpcInteractionEvents START_DIALOGUE and NpcInteractionEvents START_CUTSCENE by logging them and reporting "finished" after a short
 * delay, so NPC interactions can be played and tested end to end today. Halfway through a dialogue
 * it also sends NpcInteractionEvents DIALOGUE_CUTSCENE_CUE, which exercises DURING_DIALOGUE NPCs.
 *
 * Remove this from PlayerFactory once the real dialogue and cutscene systems send the
 * "finished" events themselves, otherwise interactions will be reported finished before the
 * real content has played.
 */
public class PlaceholderNarrativeComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlaceholderNarrativeComponent.class);

  private final float durationSeconds;
  private final List<Playback> playing = new ArrayList<>();

  public PlaceholderNarrativeComponent(float durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_DIALOGUE,
            (String dialogueId, Entity npc) -> {
              logger.info("[placeholder] Dialogue '{}' started", dialogueId);
              playing.add(new Playback(dialogueId, true));
            });
    entity
        .getEvents()
        .addListener(
            NpcInteractionEvents.START_CUTSCENE,
            (String cutsceneId, Entity npc) -> {
              logger.info("[placeholder] Cutscene '{}' started", cutsceneId);
              playing.add(new Playback(cutsceneId, false));
            });
  }

  @Override
  public void update() {
    GameTime time = ServiceLocator.getTimeSource();
    advance(time == null ? 0f : time.getDeltaTime());
  }

  /**
   * Moves every fake playback forward. Package-private so tests can drive time directly
   *
   * @param deltaSeconds time since the last call
   */
  void advance(float deltaSeconds) {
    for (Playback playback : new ArrayList<>(playing)) {
      playback.elapsed += deltaSeconds;
      if (playback.dialogue && !playback.cued && playback.elapsed >= durationSeconds / 2f) {
        playback.cued = true;
        entity.getEvents().trigger(NpcInteractionEvents.DIALOGUE_CUTSCENE_CUE, playback.id);
      }
      if (playback.elapsed >= durationSeconds) {
        playing.remove(playback);
        logger.info(
            "[placeholder] {} '{}' finished",
            playback.dialogue ? "Dialogue" : "Cutscene",
            playback.id);
        entity
            .getEvents()
            .trigger(
                playback.dialogue
                    ? NpcInteractionEvents.DIALOGUE_FINISHED
                    : NpcInteractionEvents.CUTSCENE_FINISHED,
                playback.id);
      }
    }
  }

  private static class Playback {
    private final String id;
    private final boolean dialogue;
    private float elapsed;
    private boolean cued;

    Playback(String id, boolean dialogue) {
      this.id = id;
      this.dialogue = dialogue;
    }
  }
}