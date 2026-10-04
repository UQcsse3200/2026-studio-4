# Dungeon 2 Snake miniboss

## Implemented: first-phase burrow cycle

The Snake enters the encounter with a green dust burst, then repeats this cycle:

| State | Default duration | Behaviour |
| --- | --- | --- |
| Burrowing | 0.45 seconds | Hide the Snake and health bar; disable its physics body and block incoming damage. |
| Underground | 2.4 seconds | Move towards orbiting positions near the player, leaving brief green trails. |
| Warning | 0.9 seconds | Lock a clear position near the player's current centre and show a fixed ground circle. |
| Exposed | 1.5 seconds | Emerge at that position, strike once, then remain above ground and vulnerable. |

The emergence attack deals 5 direct damage when the player's centre is inside
the warning radius at the instant the Snake emerges. It has no poison-over-time
effect. Leaving the circle avoids the strike; entering it after the strike does
not cause damage. The Snake no longer has automatic contact damage.

When this strike actually lowers a surviving player's health, a green impact
plays around their current centre and follows their movement for 0.56 seconds.
It uses the supplied `16.png` sheet's fourth row from the top: fourteen 64 x 64
frames, at 40 milliseconds per frame, including the final fading particles. The
player's existing 0.6-second red damage flash plays at the same time. The green
art retains its own colour instead of inheriting the red tint. A later hit
restarts one animation rather than stacking effects.

Dodging, invulnerability, fully absorbed damage and a local zero-damage
multiplier do not start these hurt effects. Player or Snake death ends the
impact, and disposal unregisters its renderer. The room preloads and owns the
texture. This feedback does not add poison damage over time.

The warning radius is 0.9 world units. The same centre and radius are used for
drawing and damage. The warning does not follow the player after it appears.
The Snake is 2 world units wide and tall, with its damage-detection hitbox and
ground collider scaled to match. The green dust bursts and trails use larger
particles and a wider spread to remain visible around the larger character.
The room supplies world bounds; emergence positions also check static solid
physics fixtures so the Snake does not emerge inside the room's stone walls.
If no clear position can be found nearby, it repeats its underground approach.

The enemy remains in the room's enemy list while underground. Killing it keeps
the existing death animation, room-clear tracking and item-drop behaviour.
Freeze pauses the cycle. A concealed player is not tracked or attacked; a dead
player cancels the current attack. Damage goes through CombatStatsComponent,
including normal mitigation and a local testing damage multiplier.

Settings are fields on `SnakeMiniBossConfig`; `configs/NPCs.json` can override
them without altering other enemy configurations. The default underground speed
is 3.5 world units per second, with an orbit radius of 1.8 world units.

## Implemented: second-phase poison and burrow cycle

At or below 50% of maximum health (75 of the default 150), the Snake permanently
enters its second phase. It completes the current burrow/warning and the full
1.5-second exposed window, then adds a poison attack before burrowing again.
Healing above half health does not revert the phase.

The second-phase loop is: poison windup, five volleys, brief recovery, burrow,
underground movement, ground warning, one emergence strike, then 1.5 seconds
exposed before the next poison attack. The Snake is vulnerable throughout its
poison attack and recovery, and invulnerable underground.

During the spit, the Snake uses the existing `chase` atlas region: the horizontal
tongue-out pose at (96, 0) in `images/snake.png`. This source pose has one frame;
it is held throughout the windup, volleys and short spit recovery. The pose turns
towards the player at windup and immediately before each of the five volleys.
The ordinary emergence window restores `default`, and burrowing hides the
character again.

The windup effect and every new projectile originate at this pose's mouth,
at (25, 9) from the top-left of the 32 x 32 frame. This anchor scales and moves
with the Snake and rotates with the spit pose around the sprite's centre.
Aiming starts at the transformed mouth rather than the body's centre. Rotation
is visual only; it does not rotate the collider or health bar, and resets when
the spit ends. Already-fired shots move independently.

| Setting | Default |
| --- | --- |
| Spit windup | 0.6 seconds, with a green forming effect |
| Volleys per attack | 5 |
| Projectiles per volley | 4; 20 in total |
| Time between volleys | 0.4 seconds |
| Fan width | 54 degrees |
| Direction offset per volley | -12, -6, 0, +6, +12 degrees relative to that volley's aim |
| Recovery after the last volley | 0.35 seconds |
| Poison speed | 3 world units per second |
| Poison direct damage | 1 per projectile |
| Projectile forming / fading time | 0.2 / 0.28 seconds |
| Maximum flight time | 5 seconds |
| Projectile visual width / collision radius | 1.2 / 0.12 world units |

Aim is captured again from the mouth to the player's current position before
each volley. Every projectile keeps its own direction after being emitted;
moving the player does not steer shots already in flight. A slow frame emits
at most one volley and preserves the next interval instead of
releasing several volleys at once. Emergence damage remains 5 and poison does
not apply damage over time. Actual poison damage also triggers the existing
green player-hit animation and red damage flash.

The attack uses the previously supplied forming, flying and fading poison
spritesheets. The room preloads these textures. Projectiles stop on solid
static room obstacles and room boundaries, use swept collision checks, and
can hit the player only once. Already emitted projectiles remain after the
Snake burrows. Freezing the Snake pauses its attacks and poison projectiles;
concealment pauses new volleys and prevents poison damage to the player.
Snake death, player death or room disposal clears the poison effects.

All listed gameplay settings are on `SnakeMiniBossConfig`, so later playtest
tuning remains separate from the attack logic. The old poison-pool task stays
disconnected.

## Next step: green gems and shield

Random green gem pickups will refill one shield-durability bar and show a
translucent green shield. It will consume durability only when blocking a
Snake poison projectile, and will not block the emergence attack. Gems and
the shield are not enabled in this step.

## Verification

Automated tests cover real damage immunity and recovery, a locked warning,
dodging and single-hit damage, long-frame transitions, freeze and concealment,
death cancellation, physics activation, bounded emergence selection, obstacle
avoidance, hit feedback and sprite selection, the half-health transition,
ordered poison volleys, non-homing flight, obstacle collision and single-hit
damage, effect lifetime and cleanup, and factory wiring.

Run from `source`:

```sh
./gradlew core:test --tests 'com.csse3200.game.components.miniboss.snake.*' --tests 'com.csse3200.game.entities.factories.NPCFactoryTest'
./gradlew formatCheck
```

For playtesting in Dungeon 2's Snake room:

1. Watch the entry dust and underground trail; the Snake and its bar should hide.
2. Move out of the warning circle before emergence; health should not decrease.
3. Stay inside once; expect one 5-damage strike, a red player flash and the green
   impact following the player briefly, with no subsequent poison ticks.
4. Attack while the Snake is exposed, then again underground. Only exposed
   attacks should lower its health, and the exposed window should last 1.5 seconds.
5. Stand near the outer and internal walls. Warnings must stay in clear room
   space and the Snake must emerge at the marked position.
6. Lower its health to half. After the current exposed window, watch for the
   horizontal tongue-out pose, windup and five waves of four larger green shots.
   Move between sides: the head must turn towards you before each volley, with
   the windup and shots remaining aligned to the rotated mouth.
   Move sideways; the shots must continue straight instead of turning to follow
   you. After burrowing, the next emergence should restore the curled pose.
7. Attack while it spits, then again underground. Only ground attacks should
   lower its health. Check that it returns to the marked burrow strike and
   repeats the poison cycle after the next 1.5-second exposed window.
8. With no shield or damage modifier active, let one poison projectile hit:
   expect 1 damage, green impact and red flash, with no poison ticks. The
   existing burrow strike should still deal 5 damage.
9. Put a stone wall between yourself and the shots; they must fade at the wall.
10. Defeat the Snake and verify that dust, warnings and poison all stop and the
    room clears normally. Leaving the room must not leave damaging projectiles.

If local invulnerability is enabled, it will intentionally mask the player
health checks above. Keep local test-only edits outside the feature commit.
