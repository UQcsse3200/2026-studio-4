# Dungeon 2 Snake miniboss

## Implemented: first-phase burrow cycle

The Snake enters the encounter with a green dust burst, then repeats this cycle:

| State | Default duration | Behaviour |
| --- | --- | --- |
| Burrowing | 0.45 seconds | Hide the Snake and health bar; disable its physics body and block incoming damage. |
| Underground | 2.4 seconds | Move towards orbiting positions near the player, leaving brief green trails. |
| Warning | 0.9 seconds | Lock a clear position near the player's current centre and show a fixed ground circle. |
| Exposed | 1.5 seconds | Emerge at that position, strike once, then remain above ground and vulnerable. |

The emergence attack deals 2 direct damage when the player's centre is inside
the warning radius at the instant the Snake emerges. It has no poison-over-time
effect. Leaving the circle avoids the strike; entering it after the strike does
not cause damage. The Snake no longer has automatic contact damage.

When this strike actually lowers a surviving player's health, a green impact
plays around their current centre and follows their movement for 0.56 seconds.
It uses the supplied effect sheet's second row from the top: seven 64 x 64
frames, at 80 milliseconds per frame, excluding the four empty cells. The
player's existing 0.6-second red damage flash plays at the same time. The green
art retains its own colour instead of inheriting the red tint. A later hit
restarts one animation rather than stacking effects.

Dodging, invulnerability, fully absorbed damage and a local zero-damage
multiplier do not start these hurt effects. Player or Snake death ends the
impact, and disposal unregisters its renderer. The room preloads and owns the
texture. This feedback does not add poison damage over time.

The warning radius is 0.9 world units. The same centre and radius are used for
drawing and damage. The warning does not follow the player after it appears.
The Snake is 1.5 world units wide and tall, with its damage-detection hitbox and
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

## Planned: second phase at 50% health

This incremental implementation repeats the first-phase cycle at all health
levels. The old poison-pool attack is disconnected. The next step will add:

- Four fan volleys of four poison projectiles, with a small angle change per
  volley. Projectiles do not home and deal low direct damage.
- A spit, burrow, emergence-strike and 1.5-second recovery cycle. The Snake can
  be damaged while spitting and during recovery, but not underground.
- Random green gem pickups that refill one shield-durability bar.
- A translucent green shield that consumes durability only when blocking a
  Snake poison projectile. It does not block the emergence attack.

## Verification

Automated tests cover real damage immunity and recovery, a locked warning,
dodging and single-hit damage, long-frame transitions, freeze and concealment,
death cancellation, physics activation, bounded emergence selection, obstacle
avoidance, hit feedback and sprite selection, effect lifetime and cleanup, and
factory wiring.

Run from `source`:

```sh
./gradlew core:test --tests 'com.csse3200.game.components.miniboss.snake.*' --tests 'com.csse3200.game.entities.factories.NPCFactoryTest'
./gradlew formatCheck
```

For playtesting in Dungeon 2's Snake room:

1. Watch the entry dust and underground trail; the Snake and its bar should hide.
2. Move out of the warning circle before emergence; health should not decrease.
3. Stay inside once; expect one 2-damage strike, a red player flash and the green
   impact following the player briefly, with no subsequent poison ticks.
4. Attack while the Snake is exposed, then again underground. Only exposed
   attacks should lower its health, and the exposed window should last 1.5 seconds.
5. Stand near the outer and internal walls. Warnings must stay in clear room
   space and the Snake must emerge at the marked position.
6. Defeat it and verify that all dust/warnings stop and the room clears normally.

If local invulnerability is enabled, it will intentionally mask the player
health checks above. Keep local test-only edits outside the feature commit.
