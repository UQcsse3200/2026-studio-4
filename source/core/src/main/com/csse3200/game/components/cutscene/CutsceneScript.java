package com.csse3200.game.components.cutscene;

import com.csse3200.game.files.FileLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A scripted animation scene: an mp4 played full screen. Loaded from {@code
 * configs/cutscenes/<id>.json}, or built in code.
 *
 * <pre>{@code
 * { "video": "videos/boss_intro.mp4", "skippable": true,
 *   "events": [ { "at": 2.5, "event": "openGate" } ] }
 * }</pre>
 */
public class CutsceneScript {
  static final String DIRECTORY = "configs/cutscenes/";

  /** An event fired on the player's event handler part-way through the cutscene. */
  public static class TimedEvent {
    /** Seconds after the cutscene starts. */
    public float at;

    /** Event name triggered on the player's event handler. */
    public String event;

    /** Optional string argument; if null the event is triggered with no arguments. */
    public String arg;

    public TimedEvent() {}

    public TimedEvent(float at, String event, String arg) {
      this.at = at;
      this.event = event;
      this.arg = arg;
    }
  }

  /** Set from the file name when loaded. */
  public String id;

  /** If true, the cutscene plays once per session unless forced. */
  public boolean once;

  /** Whether the player can skip with SPACE / ENTER / ESC. */
  public boolean skippable = true;

  /** Path of the mp4 inside the assets folder, e.g. "videos/intro.mp4". */
  public String video;

  /** Events to fire at set times. */
  public TimedEvent[] events = new TimedEvent[0];

  public CutsceneScript() {}

  public CutsceneScript(String id) {
    this.id = id;
  }

  /** A video cutscene (fluent factory). */
  public static CutsceneScript ofVideo(String id, String videoPath) {
    CutsceneScript script = new CutsceneScript(id);
    script.video = videoPath;
    return script;
  }

  /** Adds a timed event (fluent). */
  public CutsceneScript at(float seconds, String event, String arg) {
    List<TimedEvent> list = new ArrayList<>(List.of(events));
    list.add(new TimedEvent(seconds, event, arg));
    events = list.toArray(new TimedEvent[0]);
    return this;
  }

  /**
   * Loads {@code configs/cutscenes/<name>.json}.
   *
   * @param name cutscene id; a trailing ".json" is accepted
   * @return the script, or null if the file is missing or invalid
   */
  public static CutsceneScript load(String name) {
    String id = ScriptNames.stripExtension(name);
    if (id == null || !ScriptNames.isSafe(id) || !ScriptNames.exists(DIRECTORY + id + ".json")) {
      return null;
    }
    CutsceneScript script = FileLoader.readClass(CutsceneScript.class, DIRECTORY + id + ".json");
    if (script != null) {
      script.id = id;
    }
    return script;
  }
}
