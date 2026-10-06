# Cutscenes and dialogue

Reusable dialogue boxes and video cutscenes (issue #190). Content lives in JSON files; code
only ever sends an event. While a dialogue or cutscene plays, the **world is frozen** (no enemy,
projectile or trap updates, no physics, no run timer, sprite animations stop), the **player's
controls are locked**, and both are always restored when it ends, including when skipped.

## Quick start (QA from the terminal, `F1`)

| Command | Plays |
|---|---|
| `dialogue demo` | `assets/configs/dialogues/demo.json` |
| `cutscene demovideo` | `assets/configs/cutscenes/demovideo.json` (plays `assets/videos/demo.mp4`) |

Give the file name with or without `.json`. The terminal stays open if the file is not found.

Controls while playing: **SPACE / ENTER / left click** advance a dialogue line (first press
finishes the typewriter, second goes to the next line) or skip a skippable cutscene; **ESC** skips a
cutscene. `F1` still opens the terminal.

## Triggering from game code

Everything is an event on the **player's** event handler. The helpers in `CutsceneEvents` wrap it:

```java
CutsceneEvents.playDialogue(player, "old_sage");        // configs/dialogues/old_sage.json
CutsceneEvents.playCutscene(player, "boss_defeated");   // configs/cutscenes/boss_defeated.json

// or build one in code, no file needed
CutsceneEvents.playDialogue(player,
    new DialogueScript("tip").speaker("sage", "Old Sage", "images/ghost.atlas", "float")
                             .line("sage", "Watch out for the traps!"));
CutsceneEvents.playCutscene(player,
    CutsceneScript.video("gate_opens", "videos/gate_opens.mp4").at(2f, "openGate", null));

// react to them
player.getEvents().addListener(CutsceneEvents.CUTSCENE_FINISHED, (String id) -> { ... });
```

| Request (send) | Args |
|---|---|
| `START_DIALOGUE` | `(String dialogueId, Entity npcOrNull)` |
| `START_CUTSCENE` | `(String cutsceneId, Entity npcOrNull)` |
| `START_DIALOGUE_SCRIPT` | `(DialogueScript)` |
| `START_CUTSCENE_SCRIPT` | `(CutsceneScript)` |

| Notification (listen) | Args |
|---|---|
| `DIALOGUE_STARTED` / `DIALOGUE_FINISHED` | `(String dialogueId)` |
| `DIALOGUE_LINE_SHOWN` | `(String dialogueId, Integer lineIndex)` |
| `DIALOGUE_CUTSCENE_CUE` | `(String dialogueId)` - a line marked `"cue": true` appeared |
| `CUTSCENE_STARTED` / `CUTSCENE_FINISHED` | `(String cutsceneId)` |
| `CUTSCENE_SKIPPED` | `(String cutsceneId)` - followed by `CUTSCENE_FINISHED` |

`START_DIALOGUE`, `START_CUTSCENE`, `DIALOGUE_FINISHED`, `CUTSCENE_FINISHED` and
`DIALOGUE_CUTSCENE_CUE` are the same events the friendly NPC system uses
(`NpcInteractionEvents`), so an NPC's `dialogueId` / `cutsceneId` in `friendlyNpcs.json` are just
script file names. `EventHandler` requires every listener of an event to take the same number of
arguments, so keep to the signatures above.

If a script file is missing, nothing is shown, a warning is logged and the matching `*_FINISHED`
event is still sent so nothing waits forever. A second dialogue (or cutscene) requested while one is
already open is ignored; a dialogue and a cutscene *can* overlap (e.g. a cutscene started from a
dialogue cue).

## Dialogue file: `configs/dialogues/<id>.json`

```json
{
  "once": false,
  "speakers": [
    { "id": "sage", "name": "Old Sage", "atlas": "images/ghost.atlas", "animation": "float" },
    { "id": "me", "name": "You", "texture": "images/box_boy.png" }
  ],
  "lines": [
    { "speaker": "sage", "text": "Welcome, traveller." },
    { "speaker": "me", "text": "Who are you?", "cue": true }
  ]
}
```

The text is shown on the left and the speaker's framed portrait and name on the right. A portrait is
either an animation (`atlas` + `animation`, optional `frameSeconds`) or a still `texture`. The
speakers' art is loaded when the dialogue opens. `once: true` makes the dialogue play only once per
session.

## Cutscene file: `configs/cutscenes/<id>.json`

A cutscene is an mp4 played full screen:

```json
{
  "video": "videos/boss_intro.mp4",
  "skippable": true,
  "once": false,
  "events": [ { "at": 2.5, "event": "openGate", "arg": "optional string" } ]
}
```

`skippable` (default true) lets the player skip with SPACE / ENTER / click / ESC. `once: true` plays
it only once per session. `events` fire on the player's event handler at those times (seconds from
the start of the video).

Video notes: decoding is pure Java ([JCodec](http://jcodec.org)), so it works the same on every OS
but is best at 720p or lower, H.264 baseline/main profile. There is **no audio**. If a video cannot
be decoded the cutscene ends immediately (error logged) and the game is released.

The cutscenes used by `friendlyNpcs.json` (`merchant_opens_shop`, `spirit_reveals_dungeons`,
`guardian_awakens`) currently all play the demo video as a placeholder; point their `video` at real
footage when it exists.

## How it fits together

- `NarrativeFactory.createNarrative(player)` builds one entity (registered in `MainGameScreen`)
  holding `DialogueDisplay` (UI), `CutscenePlayer` (video), `NarrativeManagerComponent`
  (events, freeze, locks, once-only, timed events) and `NarrativeInputComponent` (input capture).
  It is separate from the player because it must keep updating while the world is frozen.
- `EntityService.setFrozen(owner, bool)` freezes every entity that has not called
  `setUpdatesWhilePaused(true)`; each owner releases its own freeze. `MainGameScreen` also stops
  physics, room logic and the run timer, and sets the game time scale to 0, while frozen. This is
  separate from the inventory's `toggleUpdate()` pause.
- Pure logic is split out for testing: `DialogueRunner` (typewriter / line flow).
