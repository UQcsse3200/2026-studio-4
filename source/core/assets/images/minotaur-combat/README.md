# Minotaur combat animation assets

`attacks.png` and `attacks.atlas` contain the 16 `atk_1` frames from
chierit's **Boss: Minotaur**, free v1.1, obtained on 2026-10-14.
See [the attribution and license record](../../licenses/MINOTAUR-ATTRIBUTION.md).

The 1152 × 640 RGBA sheet packs the original 288 × 160 frames in a 4 × 4
row-major grid. Each source frame is reflected horizontally to match the
existing game's right-facing Minotaur. Frames are neither trimmed nor
resized; every RGBA pixel, including transparent pixels, is preserved by
the reflection. Texture sampling is nearest-neighbor.

Yuezhou Wang selected these sequences on 2026-10-14:

- `normalAttack`, indices 0–6: source frames A01–A07.
- `chargeAttack`, indices 0–8: source frames A08–A16, the attack after charging.
- Charging movement continues to use the existing walk animation W01–W12.

## Alignment with existing walk assets

The existing walk regions are 148 × 130. Their alpha channels match the
original walk frames reflected horizontally and cropped to
`(left=70, top=30, right=218, bottom=160)` across all 12 frames. Ten frames
also match all visible RGBA pixels exactly; W06 and W12 each differ at five
shadow pixels only: existing RGB `(14,14,14)` versus source RGB `(12,12,12)`,
with the same alpha 54. Fully transparent RGB values also differ. These
small color differences do not affect the alignment.

For an entity of width `w` and height `h`, full attack frames are rendered
at width `288/148 * w`, height `160/130 * h`, horizontal offset
`-70/148 * w`, and zero vertical offset. The source crop ends at the bottom
of the full frame, so the feet remain aligned. Entity dimensions and the
physics collider are independent of this larger visual rectangle.

The existing `minotaur.png` and `minotaur.atlas` are retained separately.
