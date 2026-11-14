"""
Decorations redone like the saplings and crops: 16x16, composed by hand, with depth.

- Heart balloons are real 3D now: a puffy heart built from layers (a full heart in the middle, smaller ones in front
  and behind), a glossy highlight, a knot and a string - no more flat crossed pictures.
- Tulips (pot and vase), the flower box, the hanging basket: leaves in two layers with shadows, flowers with a lit
  and a shaded side.
- Fairy lights, star lights and bunting: a sagging cord with glowing bulbs, little stars, and soft pastel flags.

Run after gen_deco/gen_kitchen: python3 blockbench/gen_deco16.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import sap_leaf, blade, stem, tiny_flower, put, canvas, hx, TEX
from round_model import Model

A = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
LEAF = [hx(c) for c in ("#1e4a22", "#2e6a2c", "#4a8c3a", "#6aaa4c", "#90c86a")]
STALK = [hx(c) for c in ("#2a4a1c", "#3e6a28", "#5a8a36")]


def bloom(img, x, y, colors, kind="round"):
    """A small flower head, lit on its upper left: tulip cup, round bloom or daisy."""
    c = [hx(v) for v in colors]  # dark, mid, light, highlight
    if kind == "tulip":
        for (dx, dy, k) in ((-1, -1, 2), (1, -1, 1), (-1, 0, 2), (0, 0, 2), (1, 0, 1), (-1, 1, 1), (0, 1, 1), (1, 1, 0), (0, -1, 3)):
            put(img, x + dx, y + dy, c[k])
        put(img, x, y - 2, c[2])
    elif kind == "daisy":
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            put(img, x + dx, y + dy, c[3])
        put(img, x, y, hx("#f6d65a"))
    else:
        for (dx, dy, k) in ((0, -1, 3), (-1, 0, 2), (0, 0, 2), (1, 0, 1), (0, 1, 0), (-1, -1, 2), (1, -1, 1), (-1, 1, 1), (1, 1, 0)):
            put(img, x + dx, y + dy, c[k])
        put(img, x, y, hx("#f6d65a"))


PINK = ("#a82858", "#e04a80", "#f67aa6", "#ffc2d8")
RED = ("#8a1428", "#c8243e", "#ee4a5c", "#ff9aa6")
ORANGE = ("#b05a14", "#e88a24", "#f8b046", "#ffd690")
LILAC = ("#6a4aa0", "#9a72d0", "#c4a2f0", "#ecdcff")
WHITE = ("#b8b0b8", "#e6e0e6", "#ffffff", "#ffffff")
YELLOW = ("#c09018", "#f0c030", "#ffe060", "#fff4b0")


def tulips():
    """Three tulips on tall stems with long blade leaves, back leaves darker."""
    img = canvas()
    for (x0, y0, x1, y1) in ((7, 15, 2, 8), (8, 15, 13, 8)):
        blade(img, x0, y0, x1, y1, LEAF, back=True)
    for (x, top) in ((4, 4), (8, 2), (12, 5)):
        stem(img, [(7 if x < 8 else 8, 15), (x, top + 3), (x, top + 1)], STALK[2], STALK[1])
    for (x0, y0, x1, y1) in ((7, 15, 4, 9), (8, 15, 11, 9), (7, 15, 6, 10)):
        blade(img, x0, y0, x1, y1, LEAF)
    for (x, top, col) in ((4, 4, PINK), (8, 2, RED), (12, 5, ORANGE)):
        bloom(img, x, top, col, "tulip")
    return img


def box_flowers():
    """A planter's worth of flowers: a row of leaves along the soil and blooms nodding above (rows 4-14 are used)."""
    img = canvas()
    for i, x in enumerate(range(1, 16, 3)):
        sap_leaf(img, x + 0.5, 12.6, 3.6, 2.4, 2.4 if i % 2 else 0.7, LEAF, back=True)
    for i, (x, top) in enumerate(((2, 7), (5, 5), (8, 6), (11, 5), (14, 7))):
        stem(img, [(x, 14), (x, top + 1)], STALK[2], STALK[1])
    for i, x in enumerate(range(0, 16, 3)):
        sap_leaf(img, x + 1.5, 11.6, 3.4, 2.2, 0.6 if i % 2 else 2.5, LEAF)
    for (x, top, col, kind) in ((2, 7, PINK, "round"), (5, 5, LILAC, "round"), (8, 6, WHITE, "daisy"), (11, 5, RED, "round"), (14, 7, YELLOW, "round")):
        bloom(img, x, top, col, kind)
    return img


def trailing_flowers():
    """A hanging basket's spill: a cushion of leaves and blooms at the top, vines trailing down with little flowers."""
    img = canvas()
    for (x0, y0, x1, y1) in ((4, 4, 2, 13), (11, 4, 13, 14), (7, 5, 6, 15), (9, 5, 10, 12)):
        pts = [(x0, y0), ((x0 + x1) // 2 + (1 if x1 > x0 else -1), (y0 + y1) // 2), (x1, y1)]
        stem(img, pts, LEAF[2], LEAF[1])
        for (x, y) in pts[1:]:
            sap_leaf(img, x + 1.2, y, 2.6, 1.8, 0.4, LEAF, back=True)
            sap_leaf(img, x - 1.2, y + 1, 2.6, 1.8, 2.7, LEAF)
    for (x, y, a, b) in ((3, 2.5, 2.6, True), (12.5, 2.5, 0.5, True), (7.5, 1.6, 1.57, True), (5, 3.6, 2.2, False), (10, 3.4, 0.9, False)):
        sap_leaf(img, x, y, 3.8, 2.6, a, LEAF, back=b)
    for (x, y, col) in ((4, 2, PINK), (8, 3, WHITE), (12, 2, LILAC), (2, 9, PINK), (13, 10, LILAC), (6, 12, WHITE)):
        bloom(img, x, y, col, "daisy" if col is WHITE else "round")
    return img


def cord_y(x, y0=2, sag=3.5):
    return y0 + sag * math.sin(x / 15 * math.pi)


def lights(kind):
    """A cord sagging across the wall with bulbs hanging from it: round warm bulbs, or little stars."""
    img = canvas()
    cord = [hx(c) for c in ("#6a7a5a", "#8a9a76")] if kind == "fairy" else [hx(c) for c in ("#8a7058", "#ad9076")]
    for x in range(16):
        y = cord_y(x)
        put(img, x, round(y), cord[1])
        put(img, x, round(y) + 1, cord[0]) if x % 4 == 0 else None
    colors = (("#ffd66a", "#fff4c0"), ("#ff8db0", "#ffd0de"), ("#8ad8f0", "#d8f6ff"), ("#c6a8f0", "#f0e4ff"))
    for i, x in enumerate(range(1, 16, 3) if kind == "fairy" else range(2, 16, 4)):
        y = round(cord_y(x)) + 1
        base, glow = (hx(c) for c in (colors[i % 4] if kind == "fairy" else ("#ffd040", "#fff6c0")))
        dark = tuple(int(v * 0.75) for v in base)
        put(img, x, y, cord[0])
        if kind == "fairy":
            put(img, x, y + 1, glow); put(img, x + 1, y + 1, base)
            put(img, x, y + 2, base); put(img, x + 1, y + 2, dark)
        else:
            put(img, x, y + 1, glow)
            for (dx, dy) in ((-1, 2), (1, 2), (0, 2), (0, 3)):
                put(img, x + dx, y + dy, base)
            put(img, x, y + 2, glow); put(img, x - 1, y + 4, dark); put(img, x + 1, y + 4, dark)
    return img


def bunting():
    """Pastel pennants on a cord (rows 0-10 are used), each lit on its left half and with a stitched hem."""
    img = canvas()
    for x in range(16):
        put(img, x, round(cord_y(x, 1, 2)), hx("#b89a86"))
    cols = ("#f7a8c6", "#ffe08a", "#a8e6cf", "#cdb4f0", "#a8d8f7")
    for i, x0 in enumerate(range(0, 16, 4)):
        c = hx(cols[i % len(cols)])
        lite = tuple(min(255, v + 26) for v in c)
        dark = tuple(int(v * 0.82) for v in c)
        top = round(cord_y(x0 + 1.5, 1, 2)) + 1
        for row in range(6):
            half = 1.6 - row * 0.28
            for x in range(16):
                dx = x - (x0 + 1.5)
                if abs(dx) <= half:
                    put(img, x, top + row, lite if dx < 0 else c if row < 4 else dark)
        put(img, x0 + 1, top, hx("#ffffff")); put(img, x0 + 2, top, hx("#ffffff"))
    return img


# ------------------------------------------------------------------ wrapped pottery (columns go round, rows go down)

def vase_wrap():
    """The tulip vase, unrolled: a gold lip, a pink neck, a white scalloped shoulder, a belly of white polka dots
    in staggered rows, a fine white line and a deeper pink foot. Rows run from the lip (top) to the foot."""
    pink = [hx(c) for c in ("#c8607e", "#e07c98", "#f29ab2", "#fbc0d0")]
    white, gold, gold_d = hx("#fff8f6"), hx("#f2c94c"), hx("#c89a30")
    img = canvas()
    for y in range(16):
        for x in range(16):
            k = 2 if y < 11 else 1
            if y in (4, 13, 14, 15):
                k = 1
            put(img, x, y, pink[k])
    for x in range(16):
        put(img, x, 0, gold); put(img, x, 1, gold_d)          # lip
        put(img, x, 5, white if x % 2 == 0 else pink[3])     # scalloped shoulder
        put(img, x, 6, white if x % 4 == 1 else pink[2])
        put(img, x, 12, white)                                # a fine line above the foot
    for y, off in ((8, 0), (10, 2)):
        for x in range(off, 16, 4):
            put(img, x, y, white); put(img, x + 1, y, white)  # polka dots
            put(img, x, y + 1, pink[3])
    for x in range(1, 16, 4):
        put(img, x, 3, pink[3])                               # a glint on the neck
    return img


def pot_wrap():
    """The tulip pot, unrolled: a lighter rolled rim with a shadow under it, a painted band of little white hearts,
    then the terracotta body with soft throwing rings."""
    t = [hx(c) for c in ("#a8583a", "#c87250", "#e08e66", "#f2aa82")]
    white, rose = hx("#fff4ec"), hx("#f28aa6")
    img = canvas()
    for y in range(16):
        for x in range(16):
            k = 3 if y < 2 else 2 if y < 4 else 1 if y == 4 else 2 if y < 12 else 1
            if y in (10, 14) and x % 5 != 2:
                k = 1                                         # throwing rings
            put(img, x, y, t[k])
    heart = ((0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, 2))
    for x0 in range(0, 16, 4):
        for (dx, dy) in heart:
            put(img, x0 + dx, 6 + dy, white if (x0 // 4) % 2 == 0 else rose)
    return img


# ------------------------------------------------------------------ 3D heart balloons

HEART = ["0110110", "1111111", "1111111", "1111111", "0111110", "0011100", "0001000"]
SMALL = ["0100010", "0111110", "0111110", "0011100", "0001000"]


def balloon_texture(base):
    """Rubber skin: the colour, lighter toward the top, a soft sheen band."""
    p = [hx(c) for c in base]
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, p[3] if y < 4 else p[2] if y < 10 else p[1])
    for x in range(16):
        put(img, x, 6, p[3])
    return img


def balloon_model(name):
    m = Model(textures={"skin": f"bakery:block/{name}", "shine": "bakery:block/plate", "string": "bakery:block/plate",
                        "particle": f"bakery:block/{name}"}, ambient=False)
    px = 1.3  # pixels per heart cell
    x0, y_top = 8 - 3.5 * px, 15.6

    def layer(rows, z1, z2, inset_x=0.0, inset_y=0.0, tag="mid"):
        for r, row in enumerate(rows):
            start = None
            for i, ch in enumerate(row + "0"):
                if ch == "1" and start is None:
                    start = i
                if ch != "1" and start is not None:
                    xa, xb = x0 + start * px + inset_x, x0 + i * px - inset_x
                    ya, yb = y_top - (r + 1) * px - inset_y, y_top - r * px - inset_y
                    m.box(f"{tag}_{r}_{start}", (round(xa, 3), round(ya, 3), z1), (round(xb, 3), round(yb, 3), z2), "skin")
                    start = None

    layer(HEART, 6.2, 9.8)
    layer(SMALL, 5.4, 6.2, inset_x=0.0, inset_y=1.0, tag="front")
    layer(SMALL, 9.8, 10.6, inset_x=0.0, inset_y=1.0, tag="back")
    # a glossy highlight on the upper left lobe, the knot, and the string down to the ground
    m.box("shine", (x0 + 1.4, y_top - 3.0, 5.25), (x0 + 2.8, y_top - 1.8, 5.4), "shine", faces=["north"])
    m.box("knot", (7.4, y_top - 7 * px - 0.8, 7.4), (8.6, y_top - 7 * px + 0.1, 8.6), "skin")
    m.box("string", (7.9, 0, 7.9), (8.1, y_top - 7 * px - 0.8, 8.1), "string", faces=["north", "south", "east", "west"])
    return m


def build():
    textures = {"tulip_vase_wrap": vase_wrap(), "tulip_pot_wrap": pot_wrap(), "tulips": tulips(), "box_flowers": box_flowers(), "trailing_flowers": trailing_flowers(),
                "fairy_lights": lights("fairy"), "star_lights": lights("star"), "bunting": bunting()}
    balloons = {"pink_heart_balloon": ("#b82a64", "#e84c88", "#f674a6", "#ffa8c8"),
                "lilac_heart_balloon": ("#6a3a9a", "#9256c6", "#b07ae0", "#d4aef6"),
                "mint_heart_balloon": ("#1e7a6a", "#34a08c", "#5cc2ac", "#9ce2d0")}
    for name, cols in balloons.items():
        textures[name] = balloon_texture(cols)
        balloon_model(name).save(os.path.join(A, "models/block", name + ".json"))
    for name, img in textures.items():
        img.save(os.path.join(TEX, name + ".png"))
    return len(textures)


if __name__ == "__main__":
    print(build(), "16x16 deco textures written (balloons are 3D now)")
