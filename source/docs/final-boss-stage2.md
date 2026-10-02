# Final Boss Stage 2

Branch: `Final-boss`. Step 1 starts from the art-assets commit `34bf7dc3`.
Step 2 builds on `9c3d2f2`, including the separate Step 1 format fix.
Step 3 builds on `63ff13c`, including the tuned fireball size, speed and spacing.

## Step 1: fixed arena and slow movement

Stage 2 is one continuous encounter between 80% and 60% of maximum Boss health.
The existing three-second phase transition remains. During this transition the
camera locks at its current view and both combatants are contained, but the Boss
does not begin roaming or advance its attack timer until the transition ends.

The Boss then roams at 1.2 world units per second, choosing another destination
every three seconds or when it reaches its current destination. The entire
player and Boss sprites stay within the fixed visible area with a 0.25-unit
margin. Walking, dashing and knockback cannot carry either actor outside it.
The arena accounts for camera zoom; reducing the visible area shrinks the usable
bounds. Enlarging the window does not enlarge the original arena.

The old Stage 2 charge movement and its contact-damage component are no longer
enabled on the Final Boss. The future firing cycle currently retains the old
20-second active duration and uses a three-second pause. Slow movement continues
during that pause. This step does not spawn fire bullets yet.

At 60% health, Stage 2 hands over to Stage 3. A health floor prevents large hits
from skipping Stage 3. Leaving Stage 2, either combatant dying, or disposing the
Boss releases this encounter's camera lock. Normal camera following resumes;
other systems' camera locks, if any, remain owned by those systems.

Ordinary player attacks still damage the Boss in this incremental version. The
ice-only damage rule will be enabled together with a working player ice attack,
so the intermediate versions remain playable.

## Step 2: fire-wizard effects and outward fireballs

The shared three-second Stage 2 transition now plays the orange transformation
burst. The body changes from the previous wizard to the fire wizard halfway
through the transition. A fire-circle shield loops throughout Stage 2, including
firing pauses. It is a visual shield at this step: ordinary player damage is
still accepted after the transition. The previous Stage 1 renderer yields to
the Stage 2 renderer so two Boss bodies/shields are not drawn together.

After the transition the Boss waits 0.6 seconds before its first volley. While
roaming, it randomly chooses a six-way ring, four-shot fan, or five-way spread
with small angular variation. Each volley has a random outward orientation;
fireballs move in straight lines rather than seeking the player. Current
provisional values in `FinalBossStageTwoConfig` are:

| Setting | Value |
| --- | --- |
| Firing interval / pause | 20 seconds / 3 seconds |
| Delay between volleys | 0.3 seconds |
| Fireball speed | 2.4 world units/second |
| Damage per fireball | 8 |
| Fireball hit radius | 0.16 world units |
| Maximum lifetime / simultaneous fireballs | 6 seconds / 128 |
| Impact animation duration | 0.35 seconds |

The first density adjustment reduces speed from 4 to 2.4 world units/second
and shortens the volley interval from 0.9 to 0.3 seconds. Slower travel and
three times the firing frequency allow more waves to overlap. The fireball
capacity increases from 64 to 128 to accommodate the denser pattern.

A second readability adjustment enlarges the fireball sprite from 0.9 to 1.15
world units per side, while retaining the 0.16-unit damage radius. The visible
flame is easier to follow without making its collision core harder to dodge.
Ring/fan/spread volleys drop from 8/5/6 to 6/4/5 projectiles, averaging about 21%
fewer shots. The ring remains evenly spaced, and the fan retains its 72-degree
total arc with wider gaps. Slow flight and the 0.3-second cadence remain.

Pausing stops only new volleys; fireballs already in flight continue moving and
can hit the player. Swept relative-motion collision checks catch fast bullets
and players crossing a bullet path between frames. The nearest player, solid
static wall or arena exit wins. Each hit consumes only its own fireball, even
if invulnerability or the local test damage multiplier blocks the damage.
Large frame delays do not accumulate an unlimited backlog of volleys.

Fireballs and impact effects are encounter-owned data, not room entities. The
controller clears them on phase exit, either combatant's death, or disposal.
Damage uses the existing `CombatStatsComponent.takeDamage` route, so the user's
uncommitted local zero-damage multiplier still works. No player debug setting
is included in this step.

Textures load/unload with `RoomAssets`; renderers share them. Original PNG bytes
remain unchanged. Runtime slicing uses nine orange transformation frames, 61
nonempty fire-circle frames, the first five flying-fireball images and seven
impact images. The fireball's visual head, at source pixel (50,32), is aligned
with its collision centre when rotating the sprite.

## Step 3: destructible ice cover

Once the Stage 2 transition ends, the encounter places up to six ice crystals
at random safe positions in the fixed arena. Each crystal is 0.9 by 1.8 world
units and has a solid static obstacle collider. A 1.25-unit clearance keeps new
cover away from the player, Boss, walls, other cover and the visible edges.
Placement uses bounded attempts; cramped arenas may contain fewer crystals.
While below the limit, the encounter attempts to replenish up to two crystals
every two seconds, including during firing pauses. A long frame makes at most
one replenishment attempt, and a partial batch cannot exceed the six-cover cap.

Each crystal lasts at most seven seconds from creation, including its arrival
animation. Its visual and collider disappear together when it expires, without
starting a shatter effect. Fire hits do not reset this timer, and newly replenished
crystals have their own full lifetime. Expiry also continues during firing pauses.

Every new crystal plays a blue/cyan arrival swirl once for 0.6 seconds. The
crystal is visible and solid from the start, so the animation does not delay
usable protection. It uses nine 64x64 cells from row seven of the same supplied
`transform/01.png` sheet (start index 66). This reuses the existing texture;
the newly supplied `01.png` is byte-identical. The swirl ages with its crystal
and disappears if that crystal breaks or the encounter clears.

This adjusts the original three-cover, one-per-six-seconds version after
playtesting showed that it left too little cover available. The later lifetime
adjustment reduces replenishment frequency from every 1.5 seconds to every two
seconds, keeping the six-cover cap and two-cover batch.

Players and the Boss collide with the cover. Each actual fireball hit consumes
that bullet and removes one of four durability points. The first three hits
add cracks and a brief hit flash. The fourth removes the collider immediately
and plays six ice-shard frames over 0.55 seconds. A later shot in the same update
can pass through the newly opened gap. Collision queries and shots that hit the
player or a nearer wall do not reduce a crystal's durability.

Cover has no enemy combat stats or enemy hitbox, so ordinary player attacks do
not damage it or affect the room's enemy count. Future Stage 2 ice projectiles
will be consumed by the static obstacle query without calling the fire-damage
callback; the player ice attack is still a later increment.

The encounter owns these stationary collision entities and renders them with
its ice layer. They are not registered as updating room enemies. Broken pieces
are visual data only. Leaving Stage 2, either combatant dying, losing the arena,
or disposing the Boss clears cover and effects. Physics changes from a locked
world callback are deferred safely, and invalidated spawns cannot reappear after
cleanup. Cover outside a resized arena is removed.

The crystal uses region (128,64,32,64) from the existing 160x128 tileset. The
supplied 2048x341 shatter image is sliced using its actual 18-column/3-row layout,
starting with six breaking frames in row three; it is not treated as an original
256-pixel-cell sheet. No source PNG bytes are changed.

## Subsequent increments

1. Ice pickups, seven-second expiry, two charges, and two separate blue bars.
2. Held-J homing ice fire, sequential charge consumption, and buff-end effects;
   enable ice-only Boss damage when this attack is functional.
3. One-to-one ice/fire cancellation, remaining effects, cleanup and balance.

Each increment gets its own local test, commit and push before work proceeds.
The firing-pattern timings and pickup/charge balance will be tuned in those
increments rather than treated as final values here.

## Validation

From `source`, run the core tests with JDK 21:

```bash
./gradlew :core:test
./gradlew formatCheck
```

Tests cover phase-entry timing, slow movement during pauses, the 60% health
handover, camera-lock ownership, resize and zoom, actual Box2D containment,
outward-velocity cancellation after player updates, death and safe disposal.

Step 1 validation: Gradle 8.5 with JDK 21 compiled the core main/test sources and
passed all 1,339 core tests across 196 test classes, with zero failures, errors
or skips. A subsequent standalone format fix, `9c3d2f2`, corrected three files
and passed the full `formatCheck` used by CI.

Step 2 regression checks additionally cover actual asset frame bounds and alpha,
render lifecycle and rotation anchor, attack/pause handover, wall-before-player
and player-before-wall ordering, moving-target collision, single-hit consumption,
lifetime/cap limits, and cleanup during synchronous damage callbacks.

Step 2 validation: Gradle 8.5/JDK 21 compiled and passed all 1,384 core tests with
zero failures, errors or skips. The full `formatCheck` also passed. The patch
is based on `9c3d2f2` and includes no `PlayerFactory.java` debug changes.

After the density and readability adjustments, the Stage 2 and configuration tests and
the full `formatCheck` passed again. For focused checks with the local player
zero-damage multiplier still enabled, run:

```bash
./gradlew :core:test --tests 'com.csse3200.game.components.boss.FinalBossStageTwo*' --tests 'com.csse3200.game.entities.configs.FinalBossStageTwoConfigTest' formatCheck
```

The full suite expects normal damage in `PlayerFactoryTest`; the local test
multiplier prevents its low-health ability assertion from passing. Keep that
debug edit out of the commit and use the normal player configuration for the
full suite.

Step 3 validation: Gradle 8.5/JDK 21 passed all 1,418 core tests across 203
classes, including 102 Stage 2/configuration tests, with zero failures, errors
or skips. The full `formatCheck` passed. Added checks cover actual static-body
blocking at dash speed, safe random placement, nearest-hit ordering, four-hit
durability and same-update follow-through, pause-time hits, locked-world
disposal, invalidated queued spawns, resize cleanup, cracks and shatter frames.
The patch starts at `63ff13c` and does not include the local player debug edit.

The subsequent spawn-effects/availability adjustment passed 104 focused Stage 2
and configuration tests plus an independent full `formatCheck`. These include
batch replenishment without exceeding capacity, no catch-up burst on long
frames, solid cover during its arrival animation, and the blue animation ending
without replay. The source texture bytes and local player debug edit are unchanged.

The seven-second lifetime adjustment passed 107 focused Stage 2/configuration
tests and an independent full `formatCheck`. New coverage checks expiry at seven
seconds including arrival, fire hits
not extending lifetime, independent timers for replenished crystals, and expiry
removing collision during a firing pause.

For the local graphical playtest:

1. Enter the Final Boss encounter and finish Stage 1. At 80% health, check that
   the camera stays fixed throughout the transition and Stage 2.
2. During the transition, check the orange burst and fire-wizard body change.
   Afterwards, watch for at least 25 seconds to cover an active interval and a
   three-second pause. The Boss should keep moving and its fire shield should
   remain visible. Existing fireballs should keep moving during the pause.
3. Walk and dash against all four visible edges. Neither combatant should leave
   the visible arena. Resize the window and check containment again.
4. Check fireball heads align with their travel/hit position, and that individual
   bullets disappear with an impact when hitting a wall or the player. With the
   local zero-damage multiplier active, collisions still consume the bullet but
   should not lower player health.
5. Use normal attacks to reach 60%. Stage 3 should start, fireballs/shield should
   disappear, and camera following should resume. Also check player death or
   restarting/leaving the encounter does not leave effects in the next room.

Headless tests cannot confirm the final room's visual framing or the feel of the
movement. Those remain local playtest checks before committing this increment.

Step 3 playtest, with the local player damage multiplier still at zero:

1. Enter Stage 2 and wait for the transformation to finish. Check that up to
   six small ice crystals appear with space around characters and walls. Each
   should arrive with a short blue swirl while already providing protection.
2. Walk and dash into a crystal from each side; it should block movement. Watch
   the Boss also collide with it as it roams.
3. Hide behind one crystal. Fireballs that hit it should disappear; the first
   three hits add cracks/flash and the fourth opens the gap with a brief icy
   shatter. Your local invulnerability does not protect the crystal.
4. Watch the refill: up to two new crystals appear per two-second attempt,
   up to six intact crystals, and they should never appear on either character.
   Active fireballs must still damage crystals during the three-second pause.
   Each unbroken crystal should disappear within seven seconds of appearing,
   including during a firing pause, and leave no invisible collision behind.
5. Shrink the window and check no cover remains beyond the visible battle area
   or traps a character moved inward by the arena clamp.
6. Reach 60% Boss health, or restart/leave the encounter. All ice collision and
   fragments should disappear with the fire effects. Normal player attacks are
   still usable against the Boss until the later player-ice step is implemented.
