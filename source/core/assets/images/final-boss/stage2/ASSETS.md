# Final Boss Stage 2 art sources

Selected artwork supplied by Eden, reorganized without changing source image bytes.
Frame selection and game integration will be implemented separately.
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
- fireball: the Foozle fireball selection is retained.
- shield: 50x50 cells in a 400x400 source sheet. Loop suitability needs testing.
- obstacle: crystal-icy.png is a static 160x128 tileset; choose a region later.
- ice-projectile: six 64x32 frames, retained in their original names.
- transform: 01.png contains multiple effects; the sixth row is a candidate
  orange/yellow burst. It has not been extracted or integrated.
- ice-aura: the source sheet is 240x64 with five 48x64 cells.
- shatter: the supplied copy is 2048x341. This is suitable for visual review,
  but not a verified original 256x256-cell sprite sheet. Obtain the originally
  downloaded PNG before exact slicing; enlarging this copy will not restore it.
- Damage cracks for the ice obstacle have not been created.
- The wizard body already exists in the project. Mana bars will be drawn in code.
