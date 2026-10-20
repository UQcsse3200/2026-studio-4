# Developer tools

These tools are generated using LLMs (GPT 6.1 Sol)

## Dungeon connection graphs

`render_dungeon_graphs.py` draws a named graph for each regular dungeon from
`rooms.json`, using the same Pillow dependency as the layout renderer:

```sh
python -m pip install -r tools/requirements.txt
python tools/render_dungeon_graphs.py
```

The default output is `source/core/build/reports/dungeon-graphs.png`. Nodes show
the configured room title and ID. Green nodes mark entrances and red nodes mark
boss rooms (IDs ending in `End`). Lines represent reciprocal connections;
arrows represent one-way connections. Disabled exits, the selection hub,
external return bookshelves, and the separate final dungeon are omitted.
Positions follow door distance from the entrance, rather than physical geography.
Dungeon headings are taken from room-title prefixes.

Use `--config path/to/rooms.json` and `--output graph.png` to override paths.
Defaults work from any directory; explicit paths are relative to the current
directory. The script reads configuration only and does not change game data.

## Dungeon layout overview

`render_dungeon_layouts.py` draws a PNG overview directly from
`source/core/assets/configs/rooms.json`. Each regular dungeon occupies one row;
the hub and separate final boss arena are excluded. The number of columns adapts
to the room count.

From the repository root, with Python 3.8 or newer:

```sh
python -m pip install -r tools/requirements.txt
python tools/render_dungeon_layouts.py
```

The default output is `source/core/build/reports/tactical-layouts.png`, which is
already ignored by Git. Default paths are resolved relative to the script, so it
also works when invoked from another directory.

To use a different configuration or output location:

```sh
python tools/render_dungeon_layouts.py --config path/to/rooms.json --output overview.png
```

Explicit paths are relative to your current working directory. Pillow is the
only dependency; DejaVu Sans is used when available, with a bundled-font fallback.

The legend identifies walls, solid props, enemies, burn/freeze traps, doors and
return bookshelves, and incoming player arrival positions. Arrival markers mirror
the current offsets in `RoomLoader`, `RoomSelector`, and `PlayerFactory`; update
that calculation if the game's spawn logic changes.

This is a schematic of configuration data. It does not draw game sprites, model
collider shapes, or verify that routes are playable. Preserved-room labels are
omitted because the configuration does not record editing history.
