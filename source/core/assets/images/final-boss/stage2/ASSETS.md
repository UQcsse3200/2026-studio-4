# Final Boss Stage 2 art sources

Selected artwork supplied by Eden, reorganized without changing source image bytes.
Steps 2 and 3 integrate the fire transformation, shield, fireballs, impact effects,
ice cover and cover debris through runtime regions; other selections remain
reserved for later steps.
Third-party artwork retains its own license; this document does not relicense it.

| Folder | Source / author | License |
| --- | --- | --- |
| pickup | Pixel Art Gems Pack — karsiori | CC0 |
| fireball, impact | Pixel Magic Effects — lordfitoi, distributed by Foozle | CC0 |
| shield | Fire Circle FX — Matriax | CC0 |
| obstacle | Crystals — Chris Fiedler, Iwan Gabovitch, Jordan Irwin | CC0 |
| ice-projectile | Spell Effects — StarsteelGaming (2018) | CC0 |
| ice-aura | Ice Sparkles Overlay Effect — Jordan Irwin (AntumDeluge) | CC BY 4.0 |
| transform | Pixel Holy Spell Effect Pack 3 — BDragon1727 | Custom: free for non-commercial games; contribution required for commercial games; modification allowed; asset resale/redistribution prohibited |
| shatter | Pixel Traps - Ice Shards Trap — PixLeroy | Custom: personal/commercial project use allowed; no resale or redistribution as-is; credit optional |

## Sources

- https://karsiori.itch.io/free-pixel-art-gem-pack
- https://foozlecc.itch.io/pixel-magic-sprite-effects
- https://opengameart.org/content/fire-circle-fx
- https://opengameart.org/content/crystals-0
- https://opengameart.org/content/spell-effects-by-starsteelgaming
- https://opengameart.org/content/ice-sparkles-overlay-effect
- https://bdragon1727.itch.io/pixel-holy-spell-effect-32x32-pack-3
- https://pixleroy.itch.io/pixel-traps-ice-shards-trap

CC0: https://creativecommons.org/publicdomain/zero/1.0/
CC BY 4.0: https://creativecommons.org/licenses/by/4.0/

## Supplied documents and status

The gems, Foozle and crystals license/readme files are preserved under licenses/.
Other license statements above were checked on the source pages on 2026-10-02.
The transform and shatter assets use custom terms. Public source-repository
distribution of their PNGs has not been confirmed; do not interpret project-use
permission as an explicit grant to publish standalone source assets.

## Import notes

- pickup: blue GEM 1 is the current selection. Spark is a separate feedback effect.
- fireball: 001-005 are the flying loop; 006-010 are retained but not used in the
  loop. All are 64x64. The bright head at (50,32) is the rotation/collision anchor.
- impact: 001-007 are 64x64 frames played once when a fireball hits.
- shield: 50x50 cells in a 400x400 source sheet, eight columns. The 61 nonempty
  frames loop; the last three blank cells are excluded.
- obstacle: crystal-icy.png is a static 160x128 tileset. Runtime region
  (128,64,32,64) selects the lower-right small crystal. It is drawn at its original
  1:2 aspect ratio, anchored at the cover's bottom edge (default 0.9x1.8 world
  units). Damage cracks use a 1x1 bright pixel from the same texture, with tint
  and geometry applied only while drawing. A brief additive pass brightens the
  crystal on a hit; source PNG bytes are unchanged.
- ice-projectile: six 64x32 frames, retained in their original names.
- transform: 01.png is a 704x576 sheet with 64x64 cells and 11 columns. The first
  nine cells of the sixth row (start index 55) form the orange/yellow burst.
  Ice-cover spawning uses the nine nonempty cyan-swirl cells of the seventh row
  (start index 66, y=384, x=0 through 512). The final two empty cells are excluded.
  Both effects reuse the same loaded texture; no additional image or resource
  path is introduced, and the original PNG is unchanged.
- ice-aura: the source sheet is 240x64 with five 48x64 cells.
- shatter: the supplied copy is 2048x341 and has been resampled; it is not an
  original 256x256-cell sheet. Runtime regions select six visibly separated
  debris cells from its third row, skipping the preceding intact trap images.
  Their x positions are 228,341,455,569,683,796; widths are 113,114,114,114,113,114;
  all use y=227 and height=114. Frames play once for the configured shatter
  duration (default 0.55 seconds), then fade out. This uses the current copy's
  measured regions without claiming to recover original pixels or modifying it.
- Damage cracks are drawn progressively after the first three fireball hits;
  they are not additional image files. Destroyed cover no longer draws a body.
- The wizard body already exists in the project. Mana bars will be drawn in code.
