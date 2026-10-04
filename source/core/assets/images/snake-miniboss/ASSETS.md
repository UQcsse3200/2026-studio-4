# Snake miniboss art assets

These files support the Dungeon 2 Snake miniboss. The first-phase burrow cycle
uses the existing Snake atlas, code-drawn dust and a green player-hit animation.
The second-phase attack uses the three poison textures, rotating green gem
pickups and the player's translucent green poison shield.

## Animation layout

All PNG files retain their original bytes. Read animation cells left to right,
then top to bottom. Exclude the trailing empty cells listed below.

| File | Image size | Cell size | Layout | Frames to play | Intended use |
| --- | --- | --- | --- | --- | --- |
| `hit/green-hit-sheet.png` | 896 x 576 | 64 x 64 | 14 columns, 9 rows | Fourth row from top, all 14 cells | Green impact on a damaged player |
| `pickup/GEM 1 - LIGHT GREEN - Spritesheet.png` | 180 x 30 | 18 x 30 | 10 columns, 1 row | 10 | Green shield-refill pickup |
| `pickup/green-gem-spawn-sheet.png` | 704 x 576 | 64 x 64 | 11 columns, 9 rows | Second row from top, first 7 cells | Green gem appearance |
| `shield/shieldGreen_Edit.png` | 556 x 556 | Whole image | Static | 1 | Translucent shield around the player |
| `poison/PoisonProjectile_forming_spritesheet.png` | 256 x 192 | 64 x 64 | 4 columns, 3 rows | 10; skip last 2 cells | Poison shot forming |
| `poison/PoisonProjectile_flying_spritesheet.png` | 128 x 128 | 64 x 64 | 2 columns, 2 rows | 4 | Poison shot in flight |
| `poison/PoisonProjectile_fading_spritesheet.png` | 192 x 192 | 64 x 64 | 3 columns, 3 rows | 8; skip last cell | Poison shot impact or disappearance |

The gem archive also has 11 individual frame files, but its last individual
frame duplicates the previous one. The included spritesheet contains 10 frames.
Animation timing is a game setting, not metadata supplied by this document.

The shield already contains transparency. Do not treat it as a spritesheet.
The renderer controls its translucent appearance and brief block flash.

## Sources and attribution

### Green player-hit effect

- Input: the user-supplied `16.png`, renamed without changing its bytes;
  it replaces the earlier `01(3).png` player-hit sheet.
- Selected animation: row index 3, columns 0 through 13. All 14 cells contain
  visible pixels, including the small particles in the last two frames.
- Author, source URL and licence were not supplied with this replacement.
  Its provenance could not be confirmed from the available asset archives;
  the earlier sheet's attribution and terms must not be assumed to apply.
  Record the original source and applicable permission before redistribution.
- Changes to the artwork: none. The game selects frames at runtime.

### Green gem appearance effect

- Work: Pixel Holy Spell Effect 32x32 Pack 3 (the actual cells are 64 x 64).
- Author: BDragon1727.
- Source: https://bdragon1727.itch.io/pixel-holy-spell-effect-32x32-pack-3
- Input: user-supplied `01(4).png`, copied without changing its bytes. It is
  identical to the earlier `01(3).png` recorded in commit `8fad26ff`.
- Selected animation: row index 1, columns 0 through 6; the last four cells
  of that row are empty. The game plays these seven frames once in 0.35 seconds.
- Terms retained from the existing source record: non-commercial game use is
  free; commercial game use requires a contribution of any amount. Modification
  is allowed; resale and redistribution of the asset are prohibited. This is
  a custom licence, not CC0 or Creative Commons. Permission to redistribute
  the raw spritesheet in a public repository was not established by that record.
- Changes to the artwork: none; frame selection and scaling happen at runtime.

### Green gem

- Work: Free Pixel Art Gems Pack - Animated.
- Author: karsiori.
- Source: https://karsiori.itch.io/free-pixel-art-gem-pack
- Licence: CC0 1.0 Universal, https://creativecommons.org/publicdomain/zero/1.0/
- The author's readme is retained in `licenses/gems-readme.txt`, with line
  endings and trailing whitespace normalized.
- Changes: none; only the selected original spritesheet is included.

### Green shield

- Work: Shield Aura Effect, green version.
- Author of this version: sholev; based on Shield effect by Bonsaiheldin.
- Source: https://opengameart.org/content/shield-aura-effect
- Original work: https://opengameart.org/content/shield-effect
- Original attribution notice: Bonsaiheldin | http://bonsaiheld.org
- Licence: Creative Commons Attribution 3.0 Unported (CC BY 3.0),
  https://creativecommons.org/licenses/by/3.0/
- Changes by this project: none; the user-selected original PNG is included.
- Keep this attribution and the licence link when distributing the game.

### Poison projectiles

- Work: Pixel VFX: 3 Poison Attacks mini-pack.
- Author: Sentient Dream Studio.
- Source: https://sentient-dream-studio.itch.io/pixel-vfx-3-poison-attacks-mini-pack
- Terms on the author's page: use in commercial projects is permitted;
  attribution is optional, and resale of the asset is prohibited. No standard
  Creative Commons licence is claimed for this pack.
- Changes: none; only the three original projectile spritesheets are included
  as assets for this game.

Source pages checked on 2026-10-04.

## First-phase visuals

- The enemy reuses `images/snake.atlas` and its existing character art.
- `SnakeBurrowVisualComponent` draws green dust, short underground trails and a
  fixed orange-red ground warning. It creates its small textures at runtime and
  does not use third-party dust sprites.
- Smoke N Dust 03 was considered for burrow visuals, but is not included because
  its source page prohibits sprite redistribution.
- The second phase adds one green shield-durability bar beside the player.

## Second-phase poison visuals

- The existing `chase` region in `images/snake.atlas` supplies the horizontal
  tongue-out pose during spitting. It is a single 32 x 32 frame at (96, 0) in
  `images/snake.png`, not a new multi-frame animation. No character art is edited.
- `SnakePoisonAssets` selects 10 forming, 4 flying and 8 fading frames, excluding
  the sheets' trailing transparent cells. No artwork has been changed.
- The forming effect telegraphs the spit; individual shots form for 0.2 seconds,
  fly with a looping four-frame animation and fade for 0.28 seconds on impact or
  expiry. The flying artwork points right and is rotated to each shot's fixed
  direction.
- `RoomAssets` owns texture loading and unloading. The Snake owns the active
  projectiles and clears them when the encounter ends.

## Second-phase gem and shield visuals

- `SnakeShieldPickupComponent` animates all ten 18 x 30 cells of the original
  light-green GEM 1 sheet at a height of 0.65 world units. The gem fades in at
  its fixed position during a 0.35-second green appearance effect, then becomes
  collectible. The effect uses the second row's seven non-empty 64 x 64 cells
  from the supplied `01(4).png`, once at 0.05 seconds per frame.
  The sheet is byte-identical to GEM 1 in the newly supplied
  `Pixel Art Gem Pack - Animated (1).zip`.
- `SnakeShieldComponent` draws the original green shield with reduced alpha
  and a brief brighter flash when blocking poison. One code-drawn green bar
  shows remaining blocks; refilling never adds a second bar.
- All three original images are preloaded and unloaded by `RoomAssets`. The small
  white texture used to draw the bar is owned and disposed by the component.
