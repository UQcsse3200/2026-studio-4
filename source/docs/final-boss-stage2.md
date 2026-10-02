# Final Boss Stage 2

Branch: `Final-boss`. Step 1 starts from the art-assets commit `34bf7dc3`.

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
fire shield and ice-only damage rule will be enabled together with a working
player ice attack, so this intermediate version remains playable. The imported
Stage 2 artwork stays available for the later visual steps.

## Subsequent increments

1. Fire-wizard transformation and shield visuals; outward fire-bullet patterns.
2. Ice cover, its four-fireball durability, and break effects.
3. Ice pickups, seven-second expiry, two charges, and two separate blue bars.
4. Held-J homing ice fire, sequential charge consumption, and buff-end effects;
   enable ice-only Boss damage when this attack is functional.
5. One-to-one ice/fire cancellation, remaining effects, cleanup and balance.

Each increment gets its own local test, commit and push before work proceeds.
The firing-pattern timings and pickup/charge balance will be tuned in those
increments rather than treated as final values here.

## Validation

From `source`, run the core tests with JDK 21:

```bash
./gradlew :core:test
```

Tests cover phase-entry timing, slow movement during pauses, the 60% health
handover, camera-lock ownership, resize and zoom, actual Box2D containment,
outward-velocity cancellation after player updates, death and safe disposal.

Step 1 validation: Gradle 8.5 with JDK 21 compiled the core main/test sources and
passed all 1,339 core tests across 196 test classes, with zero failures, errors
or skips. All 13 changed Java files were formatted with the project's Spotless
configuration. The patch is checked against the clean `34bf7dc3` base.

For the local graphical playtest:

1. Enter the Final Boss encounter and finish Stage 1. At 80% health, check that
   the camera stays fixed throughout the transition and Stage 2.
2. After the transition, watch the Boss roam slowly instead of charging. Watch
   for at least 25 seconds to cover an active interval and a three-second pause.
   The Boss should keep moving during the pause.
3. Walk and dash against all four visible edges. Neither combatant should leave
   the visible arena. Resize the window and check containment again.
4. Use normal attacks to reach 60%. Stage 3 should start and camera following
   should resume. Also check player death/restarting or leaving the encounter
   does not leave the next room's camera locked.

Headless tests cannot confirm the final room's visual framing or the feel of the
movement. Those remain local playtest checks before committing this increment.
