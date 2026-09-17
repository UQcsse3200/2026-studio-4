# Stage 3 tornado animation

Source supplied by Eden: `clima_tornado_epico_20260915154616.gif`.
Original file size: 382,236 bytes. SHA-256:
`3f75ab5b4b663b1beb4945fdb269e457d71600a634c935d6a21adff779399a85`.

The GIF contains 60 composited frames at 640 × 360, each lasting 60 ms (3.6 seconds
per loop). The supplied GIF is unchanged. These two PNG sheets contain its decoded
frames in chronological, row-major order: six columns and five rows per sheet.

| File | Frames | Sheet size | Frame size |
| --- | --- | --- | --- |
| `tornado-01.png` | 0–29 | 480 × 225 | 80 × 45 |
| `tornado-02.png` | 30–59 | 480 × 225 | 80 × 45 |

Every source frame consists of exact 8 × 8 solid-colour blocks. Nearest-neighbour
reduction to 80 × 45, followed by enlargement to 640 × 360, was verified to recover
every original RGBA pixel byte-for-byte across all 60 frames. This removes redundant
pixel storage without redrawing, cropping, changing colours, or dropping frames.
Nearest texture filtering preserves that appearance in the game; two shared RGBA
textures occupy 864,000 bytes (approximately 0.824 MiB), regardless of tornado count.

The sheets retain the original opaque solid background. At load time,
`FinalBossTornadoTextureData` makes only the exact background colour RGB (22, 30, 35)
transparent. Other colours, including dark tornado shading and artwork touching
the image edges, are retained. The existing AssetManager owns both shared textures;
there are no new textures allocated by tornado animation or per-frame drawing.

No separate author credit, source URL, or licence was included with this upload.
This manifest records provenance only and does not assign a licence or claim the
artwork as original project artwork. Retain the supplied source and its licence
records when available.
