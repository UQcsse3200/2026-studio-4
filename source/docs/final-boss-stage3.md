# Final Boss Stage 3 — Ice, Jumping Statues and Shockwaves

Base: Boss commit `ae7bacc82e2f4b12a4e43b9ad3d4445d008a2db4`.
Owner: Eden. Stage 2 remains Chananyu's stage.

## Encounter

Stage 1 spawns 8 summons in Wave 1 and 16 in Wave 2, then finishes at 80% of maximum health. Stage 2 ends at 60% of maximum health. Stage 3 starts after the shared three-second transition. A damage floor prevents a powerful Stage 2 hit from passing the 60% threshold; the transition shield preserves that health until Stage 3 starts.

Wave 1 uses the purple wizard. The boss retreats while casting and circles when it has enough distance. Each volley fires three ice projectiles at once: one aimed at the player's current position and two angled 30 degrees to either side (a 60-degree fan). Bolts travel at five world units per second, with 1.6 seconds between the first and second volleys. The second volley begins a three-second recovery: the boss stops moving, teleports once after 0.25 seconds to a clear position near six world units from the player, and remains stationary without firing until the recovery ends. The next cycle's first volley fires as the three-second recovery ends, with no additional 1.6-second delay. Projectiles already in flight continue moving and can hit the player during recovery. The boss stays vulnerable and flashes when its health actually decreases; ice projectiles stop at static walls.

An ice hit locks movement, dash and attack controls for 0.35 seconds. Controls then return with movement speed reduced to 65%, shown by a blue tint. This uses an independent status effect and preserves other movement-speed buffs. Freeze cancels the current volley/recovery cycle and takes priority: the boss roams for one second, teleports beside the player, waits 0.45 seconds and strikes once. On contact, it deals damage through the normal combat rules, releases its freeze with a thaw effect, and immediately teleports away. A fresh volley cycle starts after the normal 1.6-second interval. A missed strike cannot repeat; the remaining slow expires after a maximum of three seconds from the initial hit instead. Cleanup, the Wave 2 charge transition and player defeat release the Boss-owned freeze immediately, without releasing unrelated control locks.

At 20% maximum health, incoming damage is blocked and the shield appears for three seconds. Outstanding projectiles and freeze are cleared. The boss then disappears (including its physics body and health bar).

Wave 2 spawns five copies of the selected angel statue and replaces SPACE dash with a jump. A visible SPACE hint explains how to avoid shockwaves. The player can still move and attack in the air; jumping lifts the sprite without moving its ground collider or changing movement-speed buffs. Each jump lasts 0.85 seconds, reaches 0.9 world units, and has a 0.35-second landing cooldown. It protects against the shockwave edge while high enough, rather than granting general invulnerability.

Statues move and pause between attacks. Once the first statue breaks, survivors gradually walk toward a tightening formation near the arena centre. Their required centre spacing decreases from 3.35 units with five alive to 2.35 with one, including clearance margin; walking speed rises from 1.1 to at most 1.5, and pauses shorten so even the final statue can reposition. Walls and reserved routes remain enforced. Spawn selection searches for separated clear positions when a preferred position is obstructed. Walking checks both nearby statues and their reserved routes; a blocked statue changes direction or pauses instead of walking into the group. A shared round-robin scheduler starts the first warning after three seconds. With five living statues, subsequent warnings start three seconds apart; as the number falls to four, three, two and one, the encounter-wide interval decreases to 2.7, 2.4, 2.1 and 1.8 seconds respectively. Each warning lasts 0.6 seconds, followed by a 0.9-second jump; landing creates a room-wide decaying camera shake and a hollow black ring that expands outward. Each statue also has a recovery after landing, calculated from the living count and the shared interval, with a minimum of 0.3 seconds when only one remains. Breaking another statue speeds up waiting timers while leaving any active warning or jump unchanged. Each ring damages the grounded player once on contact, with hit feedback. Jumping over the moving edge consumes that ring's contact, so landing inside a ring that has already passed does not cause damage. Collision tests include the player's path and the ring's expansion between updates.

Every statue displays a world-space healthbar that follows its jump. Ten distinct positive weapon hits defeat it, independent of weapon damage. Repeated contacts from the same weapon hitbox and source-less damage do not count as additional attacks. After every three accepted hits (hits 3, 6 and 9), the statue requests a sideways teleport, aiming for 3.5 world units of separation from its previous position. The move runs after the physics callback, with blue effects at departure and arrival. It stays clear of walls, at least 2.5 units from the player and at least the configured spacing from other statues and their reserved paths. If there is no valid position, it keeps moving normally and retries when space becomes available. A successful dodge cancels its pending jump/slam and starts a fresh recovery using the current living-statue count. The shared attack interval still prevents simultaneous warnings. Health and the ten-hit defeat counter persist through teleports. At the tenth hit the statue stops immediately with a grey break effect. Its collider is disabled when safe, and entity disposal is queued until after the physics step; no physics body is removed from a locked collision callback. Each of the first four defeated statues leaves one animated tornado, capped at four. Tornadoes wander slowly and chase the player when nearby; they stop chasing after sufficient distance is gained. The final statue spawns no tornado: all existing tornadoes stop and dissolve through the same grey burst effect. See `final-boss-tornado-finale.md` for movement and lifecycle details. Tornado contact now deals 6 damage with a shared one-second interval across all tornadoes. Collision uses the narrow ground footprint and swept player movement. Spawning and dissolving tornadoes are harmless; concealment and normal damage protection are respected. Jumping over shockwaves does not prevent tornado contact damage.

Defeating all five statues clears all shockwaves and camera shake immediately and restores SPACE dash. A 1.2-second transformation plays at the boss's position, revealing peaceful Grandpa halfway through. Grandpa remains alive and invulnerable. After the transformation, the encounter-completion event clears the room and the dialogue begins. Its three current lines are draft text. Leaving the encounter or player defeat also releases the stage's jump mode and freeze lock.

## Tuning

`FinalBossStageThreeConfig` contains the adjustable values. Initial prototype defaults (not final balance):

| Setting | Default |
| --- | --- |
| Preferred boss distance / movement speed | 5 / 2.4 world units |
| Ice volley interval / projectile speed | 1.6 seconds / 5 units per second |
| Projectiles per volley / side angle | 3 / 30 degrees (60 degrees total) |
| Volleys before reposition / delay / desired distance | 2 / 0.25 seconds / 6 world units |
| Recovery after each two-volley cycle | 3 seconds, including repositioning |
| Control lock / remaining movement speed | 0.35 seconds / 65% |
| Maximum combined freeze and slow duration | 3 seconds; the strike clears it earlier on contact |
| Tornado damage / shared hit interval | 6 HP / 1 second |
| Roaming before teleport / delay before strike | 1 / 0.45 seconds |
| Strike damage | 10 |
| Wave 2 threshold / charge | 20% maximum HP / 3 seconds |
| Statues / hits per statue | 5 / 10 |
| Hits before evasive teleport / desired travel | 3 / 3.5 world units |
| Required spacing between statue centres, 5 to 1 living | 3.35 down to 2.35 world units |
| Statue movement speed / step / normal pause | 1.1–1.5 / 2 units / 2–0.2 seconds as count falls |
| First slam delay | 3 seconds |
| Encounter-wide slam interval, 5 / 4 / 3 / 2 / 1 living statues | 3.0 / 2.7 / 2.4 / 2.1 / 1.8 seconds |
| Slam warning / jump / minimum recovery after landing | 0.6 / 0.9 / 0.3 seconds |
| Statue jump height | 1.6 world units |
| Shockwave speed / width / damage | 4.5 units per second / 0.24 units / 6 HP |
| Player jump duration / height / landing cooldown | 0.85 seconds / 0.9 units / 0.35 seconds |
| Camera shake duration / amplitude | 0.3 seconds / 0.1 world units |
| Grandpa return transformation | 1.2 seconds |
| Disappearance duration | 0.6 seconds |

The default strike lands roughly 1.45 seconds after the initial ice hit and clears the remaining slow on contact. Controls return after 0.35 seconds, giving the player time to dodge. Teleport and statue positions stay within the camera bounds and attempt to avoid static obstacle fixtures. This uses the current project's camera-based arena convention; test with the final room layout. Stone sprites slide rather than using a walking animation. Stone destruction uses the agreed magic burst rather than a bespoke fracture animation.

## Selected artwork

Source PNG bytes are preserved; sprites are sliced at runtime with nearest-neighbour filtering. Stage 3 loads only the artwork listed below. The old unused tornado pack remains removed. The new user-supplied GIF is decoded into two compact sheets with lossless pixel-grid reduction and exact background keying at load time; provenance is recorded in `images/final-boss/stage3/TORNADO-ASSET.md`.

| Purpose | Uploaded source |
| --- | --- |
| Wizard idle / run / cast / hit | wizard.zip |
| Ice flight, hit; freeze start, loop, ending | Ice Effect 01.rar, individual animation sheets |
| Blue teleport, grey break/disappearance | 79.png, rows 3 and 6 (one-based) |
| Statue | PixelAngelStatueLite.zip, 15.png |
| Wandering tornado | clima_tornado_epico_20260915154616.gif, 60 frames |
| Black shockwave, shadow and statue healthbar | Rendered geometry; one shared white pixel texture |
| Shared shield, impact and peaceful Grandpa | Existing Stage 1 assets |

The wizard, ice, statue and burst source packs should retain their original download-page license records. No license is inferred from a filename containing “Free”.

## Validation and playtest

The current regression tests simulate repeated landings at every living-statue count, including round-robin fairness and the transition to faster recovery. Evade checks cover hits 3/6/9, spacing, wall clearance, duplicate contacts, a real locked Box2D callback, warning/airborne interruption, tenth-hit defeat and blocked destinations. Tests use the existing JUnit API and do not add dependencies. See the feedback-tuning delivery for current compilation and test results. Full Gradle build and graphical gameplay still need checking on the local machine; the final room layout and encounter balance need a playtest.

Run from `source`:

```bash
./gradlew spotlessApply
./gradlew build
```

Play through Stage 1 and Stage 2 to check the handover, then verify:

- Three ice projectiles form a visible fan; each is dodgable and blocked by walls.
- Boss retreats while casting and repositions after every two volleys, with blue departure/arrival effects.
- After the second volley, Boss stops for three seconds, including its reposition, before the next cycle starts. It remains vulnerable throughout this pause.
- Repositioning preserves projectiles in flight. An ice hit during recovery interrupts it and starts the freeze/strike combo.
- Freeze stops controls for only 0.35 seconds, then allows movement, dash and attacks while slowed. Boss moves for the first second, teleports in and strikes once; the strike releases the freeze immediately and Boss teleports away. A missed strike releases the freeze at its three-second timeout.
- Boss damage is visible during Wave 1; the 20% shield cannot be damaged.
- Wave 2 has five separated statues with individual healthbars and no boss body/healthbar.
- SPACE jumps in Wave 2; walking and attacks still work in the air, and dash returns on exit.
- Statues take turns warning and jumping, with the total attack cadence gradually increasing as statues break; each landing shakes the room and creates an expanding hollow black ring.
- A grounded ring contact causes damage and feedback once; a well-timed jump avoids it. Landing inside a passed ring is safe.
- Statues remain separated while walking, including when their original routes would converge or cross.
- Weak and strong weapons each require ten hits per statue. Hits 3, 6 and 9 trigger a blue evasive teleport with no health reset; the tenth hit defeats the statue.
- An evasive teleport keeps clear of walls, the player and other statues, interrupts its own slam and restarts its attack cooldown.
- The first four statue defeats each leave one slowly wandering/chasing tornado. The fifth statue leaves no fifth tornado: all four stop and fade with grey transformation bursts. Remaining rings and shake clear immediately.
- Peaceful Grandpa appears through the 1.2-second transformation before dialogue starts.
- Grandpa and dialogue return; Continue/Enter closes dialogue and restores controls.
- Leaving the room or restarting during freeze/jump cleans up control locks, visual offsets, shake and spawned actors.

## Floating demon reinforcements

Stage 3 Wave 2 now spawns two floating demons alongside the five statues. These
use the existing floatingDemon atlas, health and ranged attacks. They move at
1.2 world units per second, start following a visible player within 9 units and
abandon a chase beyond 12 units or when sight is blocked. Within 3-6 units they circle the player. Below 3 units they retreat for at most
0.6 seconds per attack cycle, then circle. They aim at a fixed player position
for 0.4 seconds before firing, then move during a 2.4-second cooldown. The two
summons circle in opposite directions and stagger their first shots by 1.2 seconds. Outside pursuit they patrol their spawn
area; concealed players are not chased or targeted.

The final statue still determines victory. Ending the encounter, player defeat,
leaving Stage 3 or disposing the boss removes the reinforcements and their
projectiles. Deferred shots cannot spawn after combat ends. Counts, speed and
chase distances are configured in FinalBossStageThreeConfig; a count of zero
disables reinforcements for isolated mechanic tests.
