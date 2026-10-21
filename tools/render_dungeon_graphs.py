#!/usr/bin/env python3
"""Draw named room-connection graphs from rooms.json using Pillow."""

import argparse
import json
import math
import textwrap
from collections import defaultdict, deque
from pathlib import Path

from render_dungeon_layouts import DEFAULT_CONFIG, REPO_ROOT, font
from PIL import Image, ImageDraw

DEFAULT_OUTPUT = REPO_ROOT / "source/core/build/reports/dungeon-graphs.png"
PANEL_W, PANEL_H = 1240, 560
BOX_W, BOX_H = 240, 96
BACKGROUND = "#101724"


def connections(rooms):
    """Keep enabled internal exits; collapse reciprocal doors to one edge."""
    ids = {room["id"] for room in rooms}
    directed = {
        (room["id"], exit_config["destinationRoomId"])
        for room in rooms for exit_config in room.get("exits", [])
        if exit_config.get("available", True)
        and exit_config["destinationRoomId"] in ids
    }
    return directed


def positions(rooms, edges, entrance):
    """Lay rooms out in columns by minimum door distance from the entrance."""
    neighbours = defaultdict(set)
    for a, b in edges:
        neighbours[a].add(b)
        neighbours[b].add(a)
    distance = {entrance: 0}
    queue = deque([entrance])
    while queue:
        current = queue.popleft()
        for neighbour in sorted(neighbours[current]):
            if neighbour not in distance:
                distance[neighbour] = distance[current] + 1
                queue.append(neighbour)
    disconnected_column = max(distance.values()) + 1
    layers = defaultdict(list)
    for room in rooms:
        layers[distance.get(room["id"], disconnected_column)].append(room["id"])
    last_column = max(layers)
    return {
        room_id: (
            150 + column * (PANEL_W - 300) / max(1, last_column),
            140 + (index + 0.5) * 350 / len(layer),
        )
        for column, layer in layers.items()
        for index, room_id in enumerate(layer)
    }


def boundary(start, end):
    dx, dy = end[0] - start[0], end[1] - start[1]
    scale = 1 / max(abs(dx) / (BOX_W / 2), abs(dy) / (BOX_H / 2))
    return start[0] + dx * scale, start[1] + dy * scale


def draw_group(draw, rooms, all_rooms, offset):
    ox, oy = offset
    ids = {room["id"] for room in rooms}
    incoming = [
        e["destinationRoomId"] for room in all_rooms if room["id"] not in ids
        for e in room.get("exits", [])
        if e.get("available", True) and e["destinationRoomId"] in ids
    ]
    entrance = next(iter(incoming), rooms[0]["id"])
    edges = connections(rooms)
    points = positions(rooms, edges, entrance)
    points = {key: (x + ox, y + oy) for key, (x, y) in points.items()}
    prefixes = [room["title"].split(": ")[0] for room in rooms]
    title = next((p for p in prefixes if not p.startswith("Dungeon ")), prefixes[0])
    draw.text((ox + 30, oy + 22), title, font=font(26), fill="white")
    draw.text((ox + 30, oy + 59), rooms[0]["dungeonId"], font=font(15), fill="#a7b5cb")

    for a, b in sorted(edges):
        reciprocal = (b, a) in edges
        if reciprocal and a > b:
            continue
        if a == b:
            raise ValueError(f"Self-connection cannot be drawn: {a}")
        start, end = boundary(points[a], points[b]), boundary(points[b], points[a])
        draw.line((start, end), fill="#a7b5cb", width=3)
        if not reciprocal:
            angle = math.atan2(end[1] - start[1], end[0] - start[0])
            draw.polygon([end] + [
                (end[0] - 13 * math.cos(angle + turn),
                 end[1] - 13 * math.sin(angle + turn)) for turn in (-0.5, 0.5)
            ], fill="#a7b5cb")

    for room in rooms:
        x, y = points[room["id"]]
        boss = room["id"].endswith("End")
        color = "#593443" if boss else "#234d49" if room["id"] == entrance else "#26364f"
        draw.rounded_rectangle((x - BOX_W / 2, y - BOX_H / 2,
                                x + BOX_W / 2, y + BOX_H / 2),
                               radius=12, fill=color, outline="#a7b5cb", width=2)
        name = room["title"].split(": ", 1)[-1]
        lines = textwrap.wrap(name, width=22)
        for index, line in enumerate(lines):
            draw.text((x, y - 28 + index * 22), line, font=font(18),
                      fill="white", anchor="mm")
        draw.text((x, y + 29), room["id"], font=font(12), fill="#c6d2e3", anchor="mm")


def render(config, output):
    world = json.loads(config.read_text(encoding="utf-8"))
    groups = defaultdict(list)
    for room in world["rooms"]:
        if room.get("dungeonId") and room["dungeonId"] != "finalDungeon":
            groups[room["dungeonId"]].append(room)
    if not groups:
        raise ValueError("No regular dungeon rooms found")
    columns = min(2, len(groups))
    rows = math.ceil(len(groups) / columns)
    image = Image.new("RGB", (columns * PANEL_W, rows * PANEL_H + 80), BACKGROUND)
    draw = ImageDraw.Draw(image)
    for index, rooms in enumerate(groups.values()):
        draw_group(draw, rooms, world["rooms"],
                   (index % columns * PANEL_W, index // columns * PANEL_H))
    draw.text((30, rows * PANEL_H + 6),
              "Line: two-way doors | Arrow: one-way exit | Green: entrance | Red: boss room",
              font=font(19), fill="#c6d2e3")
    draw.text((30, rows * PANEL_H + 36),
              "Internal connections only; selection hub and final dungeon omitted. Positions show connectivity, not geography.",
              font=font(17), fill="#a7b5cb")
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, format="PNG")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    try:
        render(args.config, args.output)
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"Could not render dungeon graphs: {error}\n")
    print(args.output.resolve())


if __name__ == "__main__":
    main()
