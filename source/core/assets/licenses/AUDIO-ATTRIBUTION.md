# Music attribution

Provenance for every music track the game plays. Sound effects are not covered here.

Which track plays is decided in
`components/rooms/RoomAssets.java` by the room the player is standing in:

| Situation | Track |
|---|---|
| The hub, and any dungeon room with nothing left alive in it | `sounds/lobby_music.mp3` |
| A dungeon room that still has enemies in it | `sounds/fight_music.mp3` |
| The final dungeon, while the boss is alive | `sounds/boss_music.mp3` |

---

## Original music — Faraj Abbasi

`sounds/lobby_music.mp3` and `sounds/fight_music.mp3` were written for this project by Faraj
Abbasi (`@farajgrow`) and are not third-party assets. No external licence applies.

| File | Length | Bitrate |
|---|---|---|
| `sounds/lobby_music.mp3` | 2:45 | 192 kbps |
| `sounds/fight_music.mp3` | 2:31 | 192 kbps |

---

## Boss theme — Marllon Silva (xDeviruchi)

> Original music by Marllon Silva (xDeviruchi)
>
> https://www.youtube.com/@xDeviruchi

- Pack: *16-Bit Fantasy & Adventure Music* (2025), 22 SNES-style tracks
- Track used: **17 — "Decisive Battle 2 - The Calamity"**, shipped here as
  `sounds/boss_music.mp3` (4:32, 320 kbps, 44.1 kHz / 16-bit stereo)
- Author: **Marllon Silva**, who releases music as **xDeviruchi**
- Licence: **non-exclusive use licence**, granted with the pack. The full text is kept beside
  this file as `xdeviruchi-16bit-fantasy-license.pdf`.

### What the licence allows and requires

Permitted:

- Use in commercial and non-commercial projects, including video games.
- Modification, remixing and adaptation to suit the project.

Required:

- **Attribution.** The licence asks for the exact credit *"Original music by Marllon Silva
  (xDeviruchi)"*, and a link to the creator's YouTube channel where possible. That credit is
  reproduced verbatim at the top of this section, and belongs anywhere the game lists its
  assets, such as a credits screen.

Not permitted:

- Redistributing the original material, meaning republishing or reselling the pack itself.
  Shipping one track as part of this game is use, not redistribution.
- Claiming authorship of the music.

The file in this repository is the pack's unmodified `.mp3` export, renamed from
`17 - Decisive Battle 2 - The Calamity.mp3` to `boss_music.mp3` so the asset path matches the
project's naming. Renaming a file is not a modification of the work.

### Loop points, and why the game does not use them

The pack documents loop points for each track, and ships a separate "Loopable" folder of `.wav`
and `.ogg` files carrying `LOOPSTART` / `LOOPLENGTH` metadata tags. For track 17:

| | Seconds | Samples |
|---|---|---|
| Loop start | 13.846 | 610,622 |
| Loop length | 82.308 | 3,629,774 |

In other words the track opens with a 13.8 second intro that is **not** part of the intended
loop, and the loop proper is the 82 seconds that follow.

libGDX's `Music.setLooping(true)`, which `RoomAssets` uses, restarts a track from the very
beginning. It has no concept of a loop point and does not read those metadata tags. So the
intro replays on every repeat rather than being heard once.

For a boss fight that lasts less than the 4:32 runtime this never comes up, because the track
does not reach its end. It would only matter in a fight long enough to loop. Two ways to fix it
if that ever becomes audible:

- Trim the first 13.846 seconds so the file *is* the loop, losing the intro entirely; or
- Play the intro and the loop as two assets, starting the second when the first finishes.

Neither is worth doing before someone actually hears the problem.
