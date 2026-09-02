# Player Animations

## Scope

This page describes how the player shows idle, walk, and melee attack clips on
`core-player-features`.

The player sprite is a four-direction character. Movement already exists through
`PlayerActions`. This work plays the matching atlas animations so the sprite
faces the same way the player moves and swings.

Intended flow:

1. The game loads `images/idle_down.atlas` (texture `idle_down.png`, page size 768×1280).
2. `PlayerFactory` registers looping `idle_*` / `walk_*` clips and one-shot `attack_*` clips.
3. The player starts in `idle_down`.
4. WASD movement makes `PlayerActions` emit `walkDown` / `walkLeft` / `walkRight` / `walkUp`.
5. Releasing movement emits `idle*` for the last walk direction.
6. Pressing J emits `attack`, then `weaponAttack` with the last facing vector (default down).
7. `PlayerAnimationController` plays `attack_*` once, ignores idle/walk until the clip ends,
   then returns to the last idle or walk animation.

Diagonal facing matches walk: vertical wins over horizontal.

## Controls

| Input | Event | Animation |
| --- | --- | --- |
| W / A / S / D | `walk` then `walkUp` / `walkLeft` / `walkDown` / `walkRight` | `walk_*` (loop) |
| Release WASD | `walkStop` then `idle*` | `idle_*` (loop) |
| J | `attack` → `weaponAttack` | `attack_*` (play once) |
| Space | `dash` | no dedicated clip yet (still idle/walk) |
| K | `specialAttack` | sound only; no clip yet |

Click the game window after **Start**, and use an English keyboard layout so WASD/J reach the game.

## Component contract

`PlayerAnimationController` listens on the player entity. It does not spawn hitboxes.

- Locomotion events: `idleDown`, `idleLeft`, `idleRight`, `idleUp`, `walkDown`, `walkLeft`, `walkRight`, `walkUp`.
- Attack: `weaponAttack` with a `Vector2` facing (same event the sword uses).
- While `attacking` is true, idle/walk events only store the next locomotion name; they do not call `startAnimation`.
- When `AnimationRenderComponent.isFinished()` is true, locomotion resumes.

`PlayerFactory` must `addAnimation` for every atlas name before `startAnimation("idle_down")`.

Attack clips use `PlayMode.NORMAL` at 0.06s per frame (8 frames ≈ 0.48s), close to the sword cooldown of 0.5s.

## Classes

- `com.csse3200.game.components.player.PlayerAnimationController`
- `com.csse3200.game.components.player.PlayerActions`
- `com.csse3200.game.components.player.KeyboardPlayerInputComponent`
- `com.csse3200.game.components.player.TouchPlayerInputComponent`
- `com.csse3200.game.entities.factories.PlayerFactory`
- Atlas: `source/core/assets/images/idle_down.atlas`

## Automated verification

From the `source` directory with JDK 21:

```bash
./gradlew :core:test --tests com.csse3200.game.components.player.*
```

- `PlayerAnimationControllerTest`: four-way idle, walk, and attack; diagonal facing (down and up); no locomotion during attack; restore idle/walk when the attack finishes.
- `PlayerActionsTest`: walk/idle events; default attack down; attack uses last walk facing; special attack does not fire `weaponAttack`; dash is 5× walk speed; a second dash and `walkStop` are ignored while dashing.
- `KeyboardPlayerInputComponentTest`: WASD (including A/S and W+D), J attack, K special attack, Space dash, E interact + itemPickup.
- `TouchPlayerInputComponentTest`: arrow-key walk/stop and touch-down attack.

## Manual check

1. Run `./gradlew desktop:run` from `source`.
2. Click **Start**, then click the game window.
3. Walk with WASD; confirm the walk clip matches the direction.
4. Stop; confirm idle matches the last walk direction.
5. Press J in each facing; confirm a one-shot slash, then idle or walk resumes.
6. Walk down-right; confirm down walk (vertical wins). Attack on that facing should be `attack_down`.

## Out of scope

- Special attack (K) animation
- Dash animation
- Player hurt / death clips
- Changing sword damage or hitbox timing (weapons team)

Those can be follow-up tasks on the same atlas (unused rows remain on the sheet).
