# Final Boss Stage 2 art sources

Selected artwork supplied by Eden, reorganized without changing source image bytes.
Steps 2 through 5 integrate the fire transformation, shield, fireballs, impact
effects, ice cover, cover debris, ice-magic pickups, player charge indicators,
homing ice projectiles and the final-charge ending effect through runtime regions.
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

- pickup: blue GEM 1 uses ten 18x30 frames in its 180x30 sheet, drawn at 0.36x0.60
  world units with a small visual hover. Its fixed pickup position does not move
  while drawing. The final 1.5 seconds use a visibility pulse. Spark feedback
  uses the first ten 20x19 cells of its 220x19 sheet; the last blank cell is
  excluded. Both effects retain their source aspect ratio and are clamped to
  the encounter's camera bounds.
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
- ice-projectile: six transparent 64x32 frames, explicitly ordered Icespear.png,
  Icespear2.png through Icespear6.png. They face right and loop over 0.3 seconds.
  The visible sprite is 1.05x0.525 world units, independent of its damage radius.
  Source head (50,15) is measured from the top left, so its SpriteBatch rotation
  anchor is (width*50/64,height*17/32). It follows the shot's velocity without
  rotating the tail around the image centre. Impacts reuse the ten visible Spark
  frames with a blue tint and the controller's impact lifetime; this is not yet
  an ice/fire projectile cancellation effect.
- transform: 01.png is a 704x576 sheet with 64x64 cells and 11 columns. The first
  nine cells of the sixth row (start index 55) form the orange/yellow burst.
  Ice-cover spawning uses the nine nonempty cyan-swirl cells of the seventh row
  (start index 66, y=384, x=0 through 512). The final two empty cells are excluded.
  Both effects reuse the same loaded texture; no additional image or resource
  path is introduced, and the original PNG is unchanged.
  When the final ice reserve is consumed, the nine blue contracting-ring frames
  in the fifth row (start index 44, y=256, x=0 through 512) play above the player.
  This one-shot effect lasts the stage's ending duration (default 0.5 seconds),
  fades near its endpoint and stays inside the camera bounds. It reuses the same
  transform texture; the sixth and seventh rows retain their previous uses.
- ice-aura: the source sheet is 240x64 with five 48x64 cells. Its sparse blue-white
  sparkles loop above the player while at least one charge remains, drawn at
  0.60x0.80 world units and kept inside the camera bounds.
- charge indicators: two separate vertical bars read the two stored charge
  fractions. Only occupied slots are drawn, including their outline and background:
  one held reserve shows one bar, and two show two bars. Empty slots are hidden.
  Bars switch to the player's left at the right arena
  edge and are clamped to the camera bounds. Their geometry reuses the opaque
  pale-blue source pixel at (155,94) in crystal-icy.png (RGB 193,232,248); blue
  tints account for this colour instead of assuming a pure-white pixel. No new
  texture is created, and rendering never consumes energy or advances timers.
- shatter: the supplied copy is 2048x341 and has been resampled; it is not an
  original 256x256-cell sheet. Runtime regions select six visibly separated
  debris cells from its third row, skipping the preceding intact trap images.
  Their x positions are 228,341,455,569,683,796; widths are 113,114,114,114,113,114;
  all use y=227 and height=114. Frames play once for the configured shatter
  duration (default 0.55 seconds), then fade out. This uses the current copy's
  measured regions without claiming to recover original pixels or modifying it.
- Damage cracks are drawn progressively after the first three fireball hits;
  they are not additional image files. Destroyed cover no longer draws a body.
- The wizard body already exists in the project. Mana bars are drawn in code.
