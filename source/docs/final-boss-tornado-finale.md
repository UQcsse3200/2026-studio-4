# Stage 3 Wave 2: gathering statues and wandering tornadoes

This update follows the delivered `final-boss-feedback-tuning.patch` baseline. The earlier player
red-flash feedback, petrification timing and warning-only targeting remain in place.

## Statue escalation

The shared round-robin attack schedule now targets these intervals across the whole encounter:

| Living statues | Attack interval | Required centre spacing | Walking speed |
| --- | --- | --- | --- |
| 5 | 3.0 s | 3.35 | 1.1 |
| 4 | 2.7 s | 3.10 | 1.2 |
| 3 | 2.4 s | 2.85 | 1.3 |
| 2 | 2.1 s | 2.60 | 1.4 |
| 1 | 1.8 s | 2.35 (no peers left) | 1.5 |

Units are world units and world units per second. Warnings still last 0.6 seconds and jumps last
0.9 seconds. The last statue has 0.3 seconds of recovery after landing. Existing waiting timers
accelerate when another statue breaks, while warnings and jumps already in progress keep their
full duration. Long-frame protection prevents overlapping starts.

Once any statue breaks, survivors walk toward a tightening ring around the usable arena centre.
They do not instantly relocate to the centre. Destination selection checks static walls and peer
routes and preserves minimum separation. Normal pauses decrease from 2 to 0.2 seconds; post-landing
pauses are capped at a quarter of the current attack recovery, allowing movement between slams.
Hit-triggered evasive teleports still work and interrupt that statue's current attack.

## Tornado lifecycle

Each of the first four defeated statues leaves one tornado near its ground position. There can be
at most four. Spawn positions stay inside the arena and are checked against walls, live statues,
their reserved walking routes and other tornadoes. If all safe positions are temporarily blocked,
the spawn waits and retries every 0.25 seconds during combat. Visible and waiting tornadoes together
are capped at four. The final statue does not spawn a fifth tornado and cancels waiting spawns.

Tornadoes fade in for 0.6 seconds, then randomly wander at 0.55 units/second. Within 2.5 units of the
player they begin slowly chasing at 0.85 units/second. Beyond 3.5 units they return to wandering;
this difference avoids switching modes every frame at the boundary. Concealed players are not
chased. Movement checks a swept footprint against static walls and remains within visible bounds.
The smaller chase-entry radius makes ordinary wandering the default outside close encounters.

Tornado movement and statue movement use the same floor anchors and keep approximately 2.35 world
units of clearance. Tornadoes check entire reserved statue routes; statues check tornado positions
when choosing ordinary walking routes and evasive teleport destinations. Chasing never overrides
these checks. A blocked actor tries another direction or waits instead of entering the other's
space. An existing overlap can retreat gradually, without forcing either actor farther inward.

The full tornado sprite, including its broad top, stays within the camera rectangle during
wandering, chasing, spawning and fading. Ordinary long frames do not teleport it. If the camera
itself abruptly moves or zooms, the controller explicitly restores in-view placement and discards
its old destination before resuming movement. Separation is best-effort if a viewport is too small
or completely obstructed to satisfy all clearances simultaneously.

This version implements movement and visual pressure only. Tornadoes do not yet apply contact
damage, knockback or extra status effects. The owner explicitly deferred damage design to a later
sprint; this sprint focuses on movement, spacing and presentation.

When the last statue breaks, every tornado immediately stops roaming/chasing, plays the existing
grey-white break/transformation effect at its own location and fades out over 0.6 seconds.

During the final transformation, Grandpa returns beside the player's current position rather
than at the hidden boss's old location. Placement keeps the full transformation effect inside
the current camera view and searches around static obstacles. Camera or player movement during
the transformation can correct the placement; after becoming peaceful, Grandpa stays in place.
Grandpa's ending transformation continues normally. Player defeat, leaving the stage or disposing
the boss clears all remnants. Their renderers are removed when no longer needed.

## Artwork

The supplied GIF contains sixty 640×360 frames at 60 ms each (3.6 seconds per loop). Every frame
consists of exact 8×8 pixel blocks: storing 80×45 frames and scaling with nearest filtering
reconstructs all original RGBA pixels exactly. Two 480×225 sheets hold the sixty frames, shared by
all tornadoes. Only the exact solid background colour becomes transparent at texture load time;
the rest of the artwork is preserved. See the asset provenance note for hash and layout details.

## Playtest

1. Break statues one by one: verify one, two, three and four tornadoes appear.
2. Watch the remaining statues gather gradually while the jump-dodge rhythm becomes faster.
3. Approach a tornado, then move away: it should pursue slowly and later resume random movement.
   Lead it near a statue and verify that it turns or waits, leaving room to attack and dodge.
4. Break the final statue: no fifth tornado appears, all four play grey effects and disappear.
5. Confirm death/restart leaves no leftover tornadoes, attacks or movement locks.

Automated tests cover timing, actual Box2D statue movement, clearance, tornado movement and
lifecycle. Final visual readability and difficulty still require gameplay on the target machine.

Validation for the original tornado-finale patch: all 226 core main sources and 134 test sources
compiled with JDK 21;
the full JUnit core suite passed 800 tests with no failures or skips. The run uses the project's
Byte Buddy experimental option and startup agent. All 11 changed Java files passed Google Java
Format 1.28.0. This direct Java/JUnit run does not replace a full local `./gradlew build`; the
environment still lacks the Sonar Gradle plugin cache needed for its offline Gradle build.

Validation for the follow-up spacing patch: all 226 core main sources and 135 test sources compiled
with JDK 21; all 817 JUnit tests passed with no failures or skips. Coverage includes moving statues
and tornadoes together in Box2D, close pursuit and release, evade destinations, camera changes,
blocked-spawn recovery and terminal cleanup. All four changed Java files passed Google Java Format
1.28.0. Full Gradle verification and visual playtesting remain local checks.
