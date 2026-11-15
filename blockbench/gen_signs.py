"""
Signs: the neon heart sign (a calm board with a glowing heart tube) and the A-frame chalkboard sign (two boards
leaning together, wooden edges, MENU on both outer faces).
Run: python3 blockbench/gen_signs.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import put, canvas, hx, TEX

A = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")


def heart(x, y):
    """A clear pixel heart: two round lobes and a point. < 0 inside; x, y about -1.1 .. 1.1 (y up)."""
    lobes = min(math.hypot(x - 0.52, y - 0.38), math.hypot(x + 0.52, y - 0.38)) - 0.54
    point = max(abs(x) * 0.95 + (0.25 - y) * 0.72 - 0.98, y - 0.38)
    return min(lobes, point)


def neon_back():
    """A smooth plum board with a soft lighter frame and darker corners: no speckle."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = "#4a3448"
            if x in (0, 15) or y in (0, 15):
                c = "#6a4a66"
            elif x in (1, 14) or y in (1, 14):
                c = "#3e2c3e"
            put(img, x, y, hx(c))
    return img


def neon_heart():
    """The tube: a 2px pink heart with a pale core and a faint glow halo just outside it."""
    img = canvas()
    inside = {}
    for y in range(16):
        for x in range(16):
            hx_, hy = (x + 0.5 - 8) / 6.0, -(y + 0.5 - 7.4) / 6.0
            inside[(x, y)] = heart(hx_, hy) < 0
    def border(x, y, d):
        if not inside.get((x, y)):
            return False
        return any(not inside.get((x + dx, y + dy), False) for dx in range(-d, d + 1) for dy in range(-d, d + 1))
    for y in range(16):
        for x in range(16):
            if border(x, y, 1):
                outer = any(not inside.get((x + dx, y + dy), False) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                put(img, x, y, hx("#ff7ab4") if outer else hx("#ffd2e8"))
            elif not inside[(x, y)] and any(inside.get((x + dx, y + dy), False) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(img, x, y, hx("#ff9ccb"), 110)
    return img


def chalkboard_model():
    t = {"board": "bakery:block/chalkboard", "wood": "bakery:block/wood_pink", "particle": "bakery:block/chalkboard"}
    edge = lambda u1, v1, u2, v2: {"texture": "#wood", "uv": [u1, v1, u2, v2]}
    def board(z1, z2, front, angle):
        faces = {"north": {"texture": "#board" if front else "#wood", "uv": [0, 0, 16, 16]},
                 "south": {"texture": "#wood" if front else "#board", "uv": [0, 0, 16, 16]},
                 "east": edge(0, 0, 1, 15), "west": edge(0, 0, 1, 15), "up": edge(0, 0, 12, 1), "down": edge(0, 0, 12, 1)}
        zc = (z1 + z2) / 2
        return {"from": [2, 0, z1], "to": [14, 15, z2], "rotation": {"angle": angle, "axis": "x", "origin": [8, 0, zc]}, "faces": faces}
    # the front board leans back (top toward +z), the back board leans forward: their tops just touch (no crossing)
    els = [board(1.6, 2.6, True, 22.5), board(13.4, 14.4, False, -22.5),
           {"from": [7, 12.4, 7.4], "to": [9, 13.2, 8.6], "faces": {f: edge(0, 0, 2, 1) for f in ("north", "south", "east", "west", "up", "down")}}]
    return {"parent": "block/block", "ambientocclusion": False, "textures": t, "elements": els}


def build():
    neon_back().save(os.path.join(TEX, "neon_back.png"))
    neon_heart().save(os.path.join(TEX, "neon_heart.png"))
    with open(os.path.join(A, "models/block/chalkboard_sign.json"), "w") as f:
        json.dump(chalkboard_model(), f, indent="\t")
        f.write("\n")
    return 3


if __name__ == "__main__":
    print(build(), "sign pieces written")
