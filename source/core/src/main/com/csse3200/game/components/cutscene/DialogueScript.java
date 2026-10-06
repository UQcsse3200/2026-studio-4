package com.csse3200.game.components.cutscene;

import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A conversation: who is speaking and what they say, line by line. Loaded from {@code
 * configs/dialogues/<id>.json}, or built in code with the fluent methods below.
 *
 * <pre>{@code
 * {
 *   "speakers": [
 *     { "id": "sage", "name": "Old Sage", "atlas": "images/ghost.atlas", "animation": "float" }
 *   ],
 *   "lines": [
 *     { "speaker": "sage", "text": "Welcome, traveller." },
 *     { "speaker": "sage", "text": "Mind the dungeon.", "cue": true }
 *   ]
 * }
 * }</pre>
 *
 * Each speaker is shown in the portrait box on the right of the dialogue panel. A speaker uses an
 * {@code atlas} + {@code animation} for an animated portrait, or a plain {@code texture}.
 */
public class DialogueScript {
  static final String DIRECTORY = "configs/dialogues/";

  /** Someone who can speak, and how their portrait looks. */
  public static class Speaker {
    /** Key that lines use to say who is speaking. */
    public String id;

    /** Name shown under the portrait. */
    public String name;

    /** Texture atlas for an animated portrait. Takes priority over texture. */
    public String atlas;

    /** Looping animation (atlas region name) to play from the atlas. */
    public String animation;

    /** Seconds per frame of the portrait animation. */
    public float frameSeconds = 0.1f;

    /** Static portrait image, for speakers without an atlas. */
    public String texture;

    public Speaker() {}

    public Speaker(String id, String name, String atlas, String animation) {
      this.id = id;
      this.name = name;
      this.atlas = atlas;
      this.animation = animation;
    }
  }

  /** One line of dialogue. */
  public static class Line {
    /** Id of the {@link Speaker} saying this. */
    public String speaker;

    /** What they say. */
    public String text;

    /**
     * If true, {@link CutsceneEvents#DIALOGUE_CUTSCENE_CUE} fires when this line appears, which
     * starts an NPC's DURING_DIALOGUE cutscene.
     */
    public boolean cue;

    public Line() {}

    public Line(String speaker, String text) {
      this.speaker = speaker;
      this.text = text;
    }
  }

  /** Set from the file name when loaded. */
  public String id;

  /** If true, the dialogue plays once per session unless forced. */
  public boolean once;

  public Speaker[] speakers = new Speaker[0];
  public Line[] lines = new Line[0];

  public DialogueScript() {}

  public DialogueScript(String id) {
    this.id = id;
  }

  /** Adds a speaker (fluent). */
  public DialogueScript speaker(String id, String name, String atlas, String animation) {
    List<Speaker> list = new ArrayList<>(List.of(speakers));
    list.add(new Speaker(id, name, atlas, animation));
    speakers = list.toArray(new Speaker[0]);
    return this;
  }

  /** Adds a line (fluent). */
  public DialogueScript line(String speakerId, String text) {
    List<Line> list = new ArrayList<>(List.of(lines));
    list.add(new Line(speakerId, text));
    lines = list.toArray(new Line[0]);
    return this;
  }

  /**
   * @param speakerId a speaker id used by a line
   * @return that speaker, or null if the script does not define it
   */
  public Speaker findSpeaker(String speakerId) {
    for (Speaker speaker : speakers) {
      if (speaker != null && speaker.id != null && speaker.id.equals(speakerId)) {
        return speaker;
      }
    }
    return null;
  }

  /**
   * @return true if the script has something to show
   */
  public boolean hasLines() {
    return lines != null && lines.length > 0;
  }

  /**
   * Loads {@code configs/dialogues/<name>.json}.
   *
   * @param name dialogue id; a trailing ".json" is accepted
   * @return the script, or null if the file is missing or invalid
   */
  public static DialogueScript load(String name) {
    String id = ScriptNames.stripExtension(name);
    if (id == null || !ScriptNames.isSafe(id) || !ScriptNames.exists(DIRECTORY + id + ".json")) {
      return null;
    }
    DialogueScript script = FileLoader.readClass(DialogueScript.class, DIRECTORY + id + ".json");
    if (script != null) {
      script.id = id;
    }
    return script;
  }
}
