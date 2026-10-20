#!/usr/bin/env python3
"""Draw a room-config overview; this is a schematic, not an in-game screenshot."""

import argparse
import json
from collections import defaultdict
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    raise SystemExit("Install Pillow: python -m pip install -r tools/requirements.txt")

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = REPO_ROOT / "source/core/assets/configs/rooms.json"
DEFAULT_OUTPUT = REPO_ROOT / "source/core/build/reports/tactical-layouts.png"
CELL_WIDTH, CELL_HEIGHT, HEADER_HEIGHT = 360, 345, 94
BACKGROUND = "#101724"
FLOORS = {
    "1": "#65543c", "2": "#a38e64", "3": "#65734e", "4": "#655e52",
    "5": "#455269", "6": "#7d6777", "7": "#758c9e", "8": "#4f627a",
}
LEGEND = [
    ("Walls", "#b5c1ce"), ("Solid props", "#e5ac58"), ("Enemies", "#ff6e89"),
    ("Burn traps", "#ff912f"), ("Freeze traps", "#58daff"),
    ("Doors / return shelves", "#53efb5"), ("Arrivals", "#ffffff"),
]
DOOR_OFFSETS = {"LEFT": (3, 0), "RIGHT": (-3, 0), "TOP": (0, -3), "BOTTOM": (0, 3)}


def font(size):
    """Use DejaVu when available, or Pillow's bundled font on other systems."""
    try:
        return ImageFont.truetype("DejaVuSans.ttf", size)
    except OSError:
        return ImageFont.load_default(size=size)


def arrival_positions(rooms):
    """Mirror RoomLoader/RoomSelector offsets and PlayerFactory's collider centre."""
    arrivals = defaultdict(set)
    for room in rooms.values():
        for exit_config in room.get("exits", []):
            if not exit_config.get("available", True):
                continue
            target = rooms[exit_config["destinationRoomId"]]
            if exit_config.get("destinationExitId"):
                door = next(e for e in target["exits"]
                            if e["id"] == exit_config["destinationExitId"])
                dx, dy = DOOR_OFFSETS[door["side"]]
                x, y = door["x"] + dx, door["y"] + dy
            else:
                entry = next(e for e in target["entryPoints"]
                             if e["id"] == exit_config["destinationEntryPointId"])
                x, y = entry["x"], entry["y"]
            # Selector adds (2,-2); collider centre adds (1,0.3) in tile units.
            arrivals[target["id"]].add((x + 3, y - 1.7))
    return arrivals


def draw_room(draw, room, arrivals, column, row):
    x0 = column * CELL_WIDTH + 16
    y0 = HEADER_HEIGHT + row * CELL_HEIGHT + 55
    width, height = room["mapWidth"], room["mapHeight"]
    grid = room["obstacles"]["spawns"]
    props = sum(line.count("B") + line.count("S") for line in grid)
    enemies, traps = room.get("enemySpawns", []), room.get("trapSpawns", [])
    draw.text((x0, y0 - 49), room["title"].split(": ")[-1], font=font(14), fill="white")
    description = (f"{width} x {height} | {len(enemies)} enemies | "
                   f"{props} props | {len(traps)} traps")
    draw.text((x0, y0 - 29), description, font=font(11), fill="#a7b5cb")
    scale = min(4, 320 / width, 280 / height)

    def point(x, y):
        # JSON/game coordinates grow upwards; image coordinates grow downwards.
        return x0 + x * scale, y0 + (height - y) * scale

    for y, line in enumerate(grid):
        for x, tile in enumerate(line):
            a, b = point(x, y + 1)
            color = FLOORS.get(tile, BACKGROUND)
            outline = None
            if tile in "#%":
                color = "#b5c1ce"
            elif tile in "BS":
                color, outline = "#e5ac58", "#573714"
            draw.rectangle((a, b, a + scale - 1, b + scale - 1),
                           fill=color, outline=outline)

    for trap in traps:
        a, b = point(trap["x"] + 0.5, trap["y"] + 0.5)
        color = "#58daff" if trap["type"] == "FREEZE" else "#ff912f"
        draw.polygon([(a, b - 3), (a + 3, b), (a, b + 3), (a - 3, b)], fill=color)
    for enemy in enemies:
        a, b = point(enemy["x"] + 0.5, enemy["y"] + 0.5)
        draw.ellipse((a - 2, b - 2, a + 2, b + 2), fill="#ff6e89")
    for exit_config in room.get("exits", []):
        a, b = point(exit_config["x"], exit_config["y"])
        draw.rectangle((a - 2, b - 2, a + 3, b + 3), fill="#53efb5")
    for x, y in sorted(arrivals):
        a, b = point(x, y)
        draw.line((a - 2, b, a + 2, b), fill="white")
        draw.line((a, b - 2, a, b + 2), fill="white")
    divider_y = HEADER_HEIGHT + row * CELL_HEIGHT - 8
    draw.line((x0 - 6, divider_y, x0 + CELL_WIDTH - 22, divider_y), fill="#354052")


def render(config, output):
    with config.open(encoding="utf-8") as stream:
        world = json.load(stream)
    rooms = {room["id"]: room for room in world["rooms"]}
    groups = defaultdict(list)
    for room in world["rooms"]:
        dungeon = room.get("dungeonId")
        if dungeon and dungeon != "finalDungeon":
            groups[dungeon].append(room)
    if not groups:
        raise ValueError("No regular dungeon rooms found in the configuration")

    columns = max(len(group) for group in groups.values())
    image = Image.new("RGB", (max(5, columns) * CELL_WIDTH,
                             len(groups) * CELL_HEIGHT + HEADER_HEIGHT), BACKGROUND)
    draw = ImageDraw.Draw(image)
    draw.text((16, 12), "Dungeon layouts: tactical props, hazards and safe approaches",
              font=font(23), fill="white")
    x = 16
    for label, color in LEGEND:
        draw.rectangle((x, 52, x + 11, 63), fill=color)
        draw.text((x + 18, 49), label, font=font(14), fill="#dce3ef")
        x += draw.textbbox((0, 0), label, font=font(14))[2] + 47

    arrivals = arrival_positions(rooms)
    for row, group in enumerate(groups.values()):
        for column, room in enumerate(group):
            draw_room(draw, room, arrivals[room["id"]], column, row)
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, format="PNG")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG,
                        help="room JSON (default: repository's rooms.json)")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT,
                        help="PNG destination (default: core/build/reports/tactical-layouts.png)")
    args = parser.parse_args()
    try:
        render(args.config, args.output)
    except (OSError, ValueError, KeyError, StopIteration) as error:
        parser.exit(1, f"Could not render dungeon layouts: {error}\n")
    print(args.output.resolve())


if __name__ == "__main__":
    main()
