# Final Boss Stage 2

Branch: `Final-boss`. Step 1 starts from the art-assets commit `34bf7dc3`.
Step 2 builds on `9c3d2f2`, including the separate Step 1 format fix.
Step 3 builds on `63ff13c`, including the tuned fireball size, speed and spacing.
Step 4 builds on `dd6883c`, including blue cover arrival, two-second replenishment
and the seven-second cover lifetime.
Step 5 builds on `90a3810`, including pickups and hiding empty energy bars.
The current version includes held-J homing ice fire, ice-only Boss damage,
near-player cover and the longer-encounter balance adjustment described below.
Earlier steps below describe their intermediate playable versions.

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

## Step 4: ice pickups and two energy reserves

After the Stage 2 transition, one small animated blue gem can appear at a safe
random position. Thereafter, every four seconds the encounter attempts to add
one gem, up to two on the ground. Holding two energy reserves pauses new spawns;
after a reserve is exhausted, spawning resumes on the next scheduled attempt.
A long frame makes at most one attempt. Ground
gems are separate from solid ice cover; destroying or expiring cover does not
drop them. These provisional availability values are exposed in
`FinalBossStageTwoConfig` for playtesting.

Each uncollected gem expires seven seconds after it appears. Walking or dashing
within pickup range automatically collects it without an interaction key. The
swept player path catches a gem crossed between frames, but cannot collect it
at or after its expiry time. Gems and cover avoid each other when spawning;
new gems also leave clearance around both actors, walls and the arena edges.
They have no collider, enemy stats, room enemy count or inventory entry.

Each gem adds one complete ice-energy reserve, up to two. Picking up a new gem
does not refill a partially used reserve. The two bars retain their own slots;
spending one reserve does not move or refill the other bar. New energy fills an
empty slot and is consumed after any older remaining reserve. With both reserve slots occupied,
the gem stays on the ground until it expires or a slot becomes available. Stored
energy has no time-based expiry; the seven-second timeout belongs to the ground
gem. Only occupied reserves draw blue bars beside the player: one bar after
collecting one gem, and two after collecting the second. Empty slots draw no
outline or background. The bars move to the other side or
clamp within the arena near an edge. Blue-white sparkles loop above the player
while at least one reserve remains; collecting a gem plays a short spark burst.

This increment supplies the pickup and energy state. Held-J ice firing is the
next increment, so playing this version can fill the bars but does not drain
them yet. The controller's sequential-consumption API is tested for that next
step. Normal player attacks still damage the Boss, allowing Stage 3 progression
without the unimplemented ice weapon.

Spawn and expiry continue during firing pauses. Leaving Stage 2, either
combatant dying, loss of the arena or Boss disposal clears all gems, pickup
effects and stored energy. Re-entering the encounter starts empty. The Boss
owns this temporary state and its visuals; no persistent player listeners,
player-factory changes or changes to the shared player HUD are required.

## Step 5: held-J homing ice fire and the ice-only shield

While at least one reserve remains, holding J replaces the normal J attack
with repeated ice shots aimed at the Boss. Each shot continues to turn towards
the moving Boss rather than only aiming once on release. Releasing J stops new
shots without consuming energy; existing shots keep flying. Holding J before
walking over a pickup also starts ice fire when energy becomes available.
Moving, dashing and other keys retain their existing controls.

Current playtest values in `FinalBossStageTwoConfig` are:

| Setting | Value |
| --- | --- |
| Shots per full reserve / held reserves | 8 / 2 |
| Shot interval | 0.18 seconds |
| Ice shot speed | 6 world units/second |
| Maximum homing turn rate | 240 degrees/second |
| Damage per ice hit | 0.25, accumulated into whole health points |
| Ice shot hit radius | 0.12 world units |
| Maximum lifetime / simultaneous ice shots | 4 seconds / 64 |
| Impact / energy-ending effect duration | 0.35 / 0.5 seconds |

Only an emitted shot spends energy. Capacity limits or an origin inside a
solid wall cannot silently drain the bars. Cooldown keeps advancing while J
is released, so tapping faster cannot bypass the firing interval. A long frame
emits at most one new shot instead of catching up a backlog.

Energy uses the oldest reserve first. At the first reserve's end, its bar hides,
the next reserve continues without interrupting a held key, and the player aura
remains. New pickup generation resumes with a free reserve slot. A later pickup
fills the empty slot without changing the older reserve's remaining energy.
Only exhausting both reserves removes the aura and plays a short blue shrinking
ring above the player; subsequent J presses use the ordinary attack again.
Collecting a new reserve cancels any still-visible ending ring. Unspent energy
has no timer, and using a non-binary shot cost cannot leave a rounding-only slot.

Ice shots stop at the first solid static wall or ice cover, arena edge or expiry.
Hitting cover consumes the shot without taking away any of its four fire-hit
durability points. Swept collision compares the moving Boss with the shot's
travel path. A wall/arena/expiry tie takes priority over a Boss hit. Ice shots
already in flight keep moving during both the Boss's three-second firing pause
and a player action lock; new shots require the player to be alive, unlocked
and free of immobilising status effects.

Once the transition into Stage 2 finishes, its persistent fire shield blocks
ordinary melee, arrows, other damage sources and unattributed damage. Only
landed player ice shots pass through the Boss's normal damage pipeline. The
60% health floor still causes Stage 3 rather than allowing an oversized hit to
skip it. A synchronous phase change from an ice hit immediately clears the
remaining ice shots and preserves Stage 3's newly installed shield or damage
window. The private ice damage source is not a room entity or a physics body.

The J override belongs to the encounter: it runs after UI input and before
ordinary player keyboard input, passes J through when there is no energy, and
unregisters on disposal. Key release, a missed physical release, terminal/text
entry and encounter cleanup clear its held state. No player-factory debug
changes or persistent player listeners are added. The local zero-damage testing
multiplier still protects the player without changing Boss ice damage.

The six original 64x32 ice-spear textures loop during flight. Their bright head
at source (50,15), rather than their whole-image centre, is aligned to the
collision position. The energy-ending effect reuses nine blue/purple frames
from row five of `transform/01.png`; no PNG bytes are changed.

### Follow-up: longer encounters and nearby cover

Playtesting showed that two pickups could end Stage 2 too quickly. Each reserve
now supplies eight shots instead of sixteen, and each landed ice shot contributes
0.25 damage instead of one. Boss health remains integer-valued: four successful
hits remove one health point through the existing protected damage pipeline.
Fractional damage carries across pickups and firing pauses, but clears when
leaving the Stage 2 protection state. Misses, wall/cover impacts and blocked
ordinary attacks do not contribute. No shared enemy/player health API is changed.

For the default 100 maximum health, Stage 2 still spans 80 to 60 health. All
shots landing now requires 80 ice hits and ten fully used pickups, compared
with 20 hits and two pickups before this adjustment. This is an ideal lower
bound, not a promised fight duration: travel to pickups, aiming around cover,
and missed or expired shots increase the time and number of pickups required.
The 0.18-second shot cadence, two persistent reserve slots and ground pickup
timings remain the same.

Cover initially attempts up to four crystals, and replenishes at most one every
3.5 seconds, replacing the six-crystal cap and two-per-two-seconds replenishment.
Its seven-second lifetime and four-fire-hit durability are unchanged. For each
new crystal, placement first tries centres 2.5 to 4.5 world units from the
player towards the Boss (a 100-degree cone, 24 attempts), then anywhere in that
nearby ring (16 attempts), then elsewhere in the arena (8 attempts). These are
preferences, not guarantees: every candidate still needs the existing wall,
actor, cover, pickup and arena-edge clearance. If no safe candidate is found,
that crystal is skipped. Replenishment uses the player's current position;
existing crystals remain stationary.

Playtest this adjustment by moving between opposite sides of the arena and
checking that new cover favours the new position, including near walls and
corners. Confirm that one pickup emits eight shots, four landed shots remove
one health point, and a successful full eight-shot reserve removes two. Check
that cover does not spawn on the player or pickups, and that leaving cover to
find a firing angle still matters. The separate local invincibility and
skip-Stage-1 edits are not part of this change and must remain unstaged.

## Subsequent increments

1. One-to-one ice/fire cancellation, its effects, and final balance.

The confirmed full design is: while any ice energy remains, holding J
temporarily replaces the normal attack and continuously fires ice projectiles
that turn to track the Boss. Energy decreases with shots, not with time held.
When the first reserve empties, firing continues from the second without
interrupting held J or removing the player aura. Only exhausting both reserves
plays the ending effect and restores normal attacks. Releasing J does not spend
energy. A player ice shot hitting solid cover disappears without damaging the
cover; only Boss fire consumes its four-hit durability. Ice/fire collisions
consume one projectile from each side and play a cancellation effect. Ordinary
attacks become ineffective against the Stage 2 Boss when this ice weapon is
enabled, making pickups necessary for progression. Step 5 implements this except
ice/fire cancellation, which remains the next increment. Shot cost, speed,
cadence and per-reserve capacity remain tunable playtest parameters.

Each increment gets its own local test, commit and push before work proceeds.
The firing-pattern timings and pickup/charge balance remain provisional.

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

Step 4 validation: Gradle 8.5/JDK 21 passed all 1,463 core tests across 206
classes, including 147 Stage 2/configuration tests, with zero failures, errors
or skips. An independent full `formatCheck` passed. Added coverage includes
swept pickup before expiry, exact-expiry precedence, full-capacity spawn pause
and resumption, fixed energy slots with oldest-first consumption, actual wall
and cover exclusion in both directions, transition/death/disposal cleanup,
asset frame bounds and visible pixels, and two independently filled vertical
bars staying inside the arena. Original PNGs and PlayerFactory are unchanged;
final visual scale and gameplay feel still require the local graphical playtest.

The follow-up bar-visibility adjustment passed all 12 pickup/Stage 2 visual
tests and an independent full `formatCheck`. Empty reserve slots now draw
neither a bar nor its background/outline; occupied slots keep their own position.

Step 5 validation: JDK 21/Gradle 8.5 compiled the main and test sources and ran
1,530 core cases. All 1,518 cases outside the new player-ice integration class
passed; after correcting that class's assertion to retain the second reserve
in its fixed bar slot, a focused rerun passed all 12 of its cases. An independent
full `formatCheck` passed. Coverage includes held/released J and UI capture,
16/32-shot reserves, non-binary shot-cost rounding, oldest-first consumption,
homing limits, moving-target sweeps, wall/cover/expiry ordering, ordinary-hit
rejection, private-source authorisation, synchronous Stage 3 protection, ending
effects, texture anchors and cleanup. No local player invincibility edit is
included. Graphical feel and balance still require the playtest below.

The nearby-cover/longer-encounter adjustment passed 405 focused Final Boss and
Stage 2 configuration tests across 39 classes with no failures, errors or
skips, plus `formatCheck` under JDK 21/Gradle 8.5. Added coverage includes
near-player/Boss-side preference, following a moved player on replenishment,
wall/edge/pickup fallback, fractional damage across volleys, protection reset,
invalid/recursive damage, exception recovery, eight-shot energy exhaustion,
and the actual eightieth-hit Stage 3 handover.

With the local zero-damage multiplier retained, the relevant checks can be run
without the normal-damage PlayerFactory test:

```bash
./gradlew :core:test --tests 'com.csse3200.game.components.boss.FinalBoss*' --tests 'com.csse3200.game.entities.configs.FinalBossStageTwoConfigTest' formatCheck
```

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

Step 4 playtest, retaining the local player damage multiplier at zero:

1. Finish the Stage 2 transformation and find a small floating blue gem. It
   should be visibly different from the larger solid cover. Leave it alone and
   check that it disappears after seven seconds.
2. Walk or dash across another gem. It should disappear with a spark effect,
   and exactly one blue energy bar plus the player sparkles should appear;
   there should be no empty second bar or outline.
3. Collect a second gem. Both bars should be full. Try a third gem: it should
   remain on the ground without resetting or adding more bars.
4. Move against every arena edge. The bars should stay visible and follow the
   player. Check that gems are not hidden inside ice cover or walls.
5. Use ordinary attacks to enter Stage 3, or restart/leave the encounter. Gems,
   bars and player sparkles should all disappear. The next encounter must begin
   with no stored energy. Held-J firing and energy drain are not enabled yet.

Step 5 playtest (supersedes the ordinary-attack Stage 3 handover in earlier checks):

1. Enter Stage 2 and try ordinary attacks after its transition. The fire shield
   should remain visible and Boss health should not fall.
2. Pick up one gem and hold J. Ice spears should turn towards the moving Boss,
   consume the single visible blue bar over eight shots, and remove one Boss
   health point for every four landed shots. Release J; the bar must stop
   draining immediately.
3. Collect two reserves and keep J held. The first bar should disappear when
   empty, the second should then drain, and head sparkles should remain without
   an ending burst between them. A new pickup must not refill the older bar.
4. Fire from behind solid cover. Ice shots must disappear at the cover without
   cracking it; move out to find a firing angle. Boss fire still damages cover.
5. Exhaust both reserves. Both bars and the looping aura should disappear with
   a single brief blue transformation. Collecting another gem restores ice fire.
6. Use ice hits to reach 60% Boss health. Stage 3 should begin with its transition
   protection intact, and ice shots/energy/effects should all clear. Also check
   restart/leave and player death. The local invincibility edit can remain for
   the other checks, but prevents a genuine player-death playtest.
7. While holding J, open the F1 terminal. Ice fire should stop, and typing should
   not spend energy. Close it and make a fresh J press to resume.

Ice/fire projectiles do not cancel each other in this version; that collision
rule and its effect are intentionally the next small increment.
