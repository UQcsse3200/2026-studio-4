# Snake miniboss art assets

These files support the planned second phase of the Dungeon 2 Snake miniboss.
The first-phase burrow cycle uses the existing Snake atlas and code-drawn effects;
the gem, shield and poison textures will be registered with the second phase.

## Animation layout

All PNG files retain their original bytes. Read animation cells left to right,
then top to bottom. Exclude the trailing empty cells listed below.

| File | Image size | Cell size | Layout | Frames to play | Intended use |
| --- | --- | --- | --- | --- | --- |
| `pickup/GEM 1 - LIGHT GREEN - Spritesheet.png` | 180 x 30 | 18 x 30 | 10 columns, 1 row | 10 | Green shield-refill pickup |
| `shield/shieldGreen_Edit.png` | 556 x 556 | Whole image | Static | 1 | Translucent shield around the player |
| `poison/PoisonProjectile_forming_spritesheet.png` | 256 x 192 | 64 x 64 | 4 columns, 3 rows | 10; skip last 2 cells | Poison shot forming |
| `poison/PoisonProjectile_flying_spritesheet.png` | 128 x 128 | 64 x 64 | 2 columns, 2 rows | 4 | Poison shot in flight |
| `poison/PoisonProjectile_fading_spritesheet.png` | 192 x 192 | 64 x 64 | 3 columns, 3 rows | 8; skip last cell | Poison shot impact or disappearance |

The gem archive also has 11 individual frame files, but its last individual
frame duplicates the previous one. The included spritesheet contains 10 frames.
Animation timing is a game setting, not metadata supplied by this document.

The shield already contains transparency. Do not treat it as a spritesheet.
Its appearance, hit flash and fade can be controlled by the renderer later.

## Sources and attribution

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
- The single green shield-durability bar remains part of the second-phase work.
