# OpenGameArt asset attribution

Assets in this project sourced from OpenGameArt.org.

## 16x16 Weapon RPG Icons — Shade

- Source: *16x16 Weapon RPG Icons*, https://opengameart.org/content/16x16-weapon-rpg-icons
- Author: **Shade** (OpenGameArt username `shade-1`)
- Submitted: 9 September 2022
- Licence: **CC0 1.0 Universal (Public Domain Dedication)** —
  https://creativecommons.org/publicdomain/zero/1.0/
- The author states: *"No need to give me credit"*, *"Feel free to use this for your game
  (commercially or not)"*, and *"Feel free to creatively modify these sprites however you
  like."* Attribution is not required under CC0; it is recorded here for provenance.

The submission ships four material variants (`iron`, `steel`, `bronze`, `gold`). This
project uses the **steel** sheet, whose cool blue-grey blades match the existing player and
dungeon art.

`images/weapons/steel-weapons.png` is the unmodified 384x320 source sheet: a 24x20 grid of
16x16 sprites on a 16px pitch, already supplied with an alpha channel.

Sprites cut from that sheet, unaltered apart from being cropped out of the grid:

| File | Grid cell | Used for |
|---|---|---|
| `images/weapons/sword.png` | row 2, column 7 | sword attack hitbox |
| `images/weapons/knife.png` | row 0, column 3 | knife attack hitbox |
| `images/weapons/throwing_knife.png` | row 13, column 6 | bow projectile |

No pixel data inside the sprites was modified, and no recolouring or rotation was applied.

**Orientation note for developers:** every sprite in this pack is drawn as an inventory icon
pointing up and to the right, at roughly 45 degrees. Do not rotate the PNGs to correct this
— rotating pixel art by 45 degrees resamples it and destroys the clean edges. Apply a
constant -45 degree offset in the render component instead, where the rotation happens on
the GPU at draw time.
