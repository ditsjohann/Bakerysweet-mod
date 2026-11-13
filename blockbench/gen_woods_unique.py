"""
A unique look for every wood, on top of the soft 16x16 painterly base (gen_woods16.py).

Inspired by the vibe of hand-made wood sets (framed mahogany planks, carved ornamental woods, pale woods with nail
dots, concentric-square log ends, plated bark) - not copied: each of our woods gets its own design for bark, log end,
planks, door, trapdoor and sapling, all in the same soft, low-contrast pixel style.

    cherry blossom  soft horizontal bark   spiral end      narrow boards       heart-window door, flower trapdoor
    citrus          pale, dark dash marks  square rings    boards + nail dots  slatted door with window, dotted slats
    orangewood      soft rounded plates    square rings    framed H-joints     long inset panel + knob, framed square
    peachwood       soft vertical          spiral          herringbone         two panels + porthole, round window
    mangowood       mossy                  square rings    wide grained boards lattice door, lattice trapdoor
    plumwood        dark violet bands      spiral          carved ornament     carved ornate door, ornament trapdoor
    palm            ringed trunk           fibrous dotted  woven strips        bamboo slat door, woven trapdoor

Run after gen_woods16.py: python3 blockbench/gen_woods_unique.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods16 import WOODS, ramp, noise, quantize, hx, TEX, S
from PIL import Image

STYLE = {
    "blossom": dict(bark="h", top="spiral", planks="vertical", door="heart", trap="flower", sapling="blossom"),
    "citrus": dict(bark="eyes", top="squares", planks="nails", door="slats", trap="dots", sapling="lemon"),
    "orangewood": dict(bark="plates", top="squares", planks="framed", door="panel", trap="framed", sapling="orange"),
    "peachwood": dict(bark="v", top="spiral", planks="herring", door="porthole", trap="round", sapling="peach"),
    "mangowood": dict(bark="mossy", top="squares", planks="wide", door="lattice", trap="lattice", sapling="mango"),
    "plumwood": dict(bark="h", top="spiral", planks="diagonal", door="ornate", trap="ornament", sapling="plum"),
    "palm": dict(bark="rings", top="fibre", planks="bamboo", door="bamboo", trap="woven", sapling="palm"),
}


# ---------------------------------------------------------------- helpers

def canvas(w=S, h=S):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (a,))


def fill_noise(img, pal, lo, hi, seed, cw=2, ch=5, region=None):
    """Paints a soft painterly fill using tones lo..hi of the palette (grain stretched cw x ch)."""
    w, h = img.width, img.height
    rnd_field = [[0.0] * w for _ in range(h)]
    for oy in range(0, h, S):
        f = noise(seed + oy, cw, ch, ((1.0, 1.0), (0.5, 0.55)))
        for y in range(min(S, h - oy)):
            for x in range(w):
                rnd_field[oy + y][x] = f[y][x % S]
    q = quantize(rnd_field, hi - lo + 1)
    for y in range(h):
        for x in range(w):
            if region is None or region(x, y):
                put(img, x, y, pal[lo + q[y][x]])


def rect(img, x1, y1, x2, y2, c):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            put(img, x, y, c)


def frame(img, x1, y1, x2, y2, dark, light):
    """A soft 1px bevel: light on top/left, dark on bottom/right (never an outline)."""
    for x in range(x1, x2 + 1):
        put(img, x, y1, light)
        put(img, x, y2, dark)
    for y in range(y1, y2 + 1):
        put(img, x1, y, light)
        put(img, x2, y, dark)


# ---------------------------------------------------------------- bark

def bark(name, w):
    bd, bl, wd, wl, _ = w
    pal = ramp(bd, bl, 6)
    style = STYLE[name]["bark"]
    img = canvas()
    seed = sum(map(ord, name))
    if style == "eyes":
        # Pale bark (wood-light tones) with soft dark horizontal dash marks, birch-like.
        pale = ramp(wd, wl, 6)
        fill_noise(img, pale, 3, 5, seed, 2, 5)
        for (x, y, l) in ((2, 2, 5), (10, 5, 4), (5, 9, 6), (12, 12, 4), (0, 14, 3), (14, 1, 3)):
            for t in range(l):
                xx = (x + t) % S
                put(img, xx, y, pal[1] if 0 < t < l - 1 else pal[2])
                put(img, xx, (y + 1) % S, pale[2])
        return img
    if style == "plates":
        # Soft rounded bark plates: tall cells with gently darker crevices (two steps, never black on light).
        fill_noise(img, pal, 2, 4, seed, 2, 5)
        cells = [(2, 3), (7, 10), (12, 2), (4, 13), (10, 15), (15, 9), (0, 8)]
        for y in range(S):
            for x in range(S):
                ds = sorted(math.hypot(((x - cx + 8) % S) - 8, (((y - cy + 8) % S) - 8) * 0.45) for (cx, cy) in cells)
                gap = ds[1] - ds[0]
                if gap < 0.7:
                    put(img, x, y, pal[1])
                elif gap < 1.4:
                    put(img, x, y, pal[2])
        return img
    if style == "mossy":
        fill_noise(img, pal, 0, 5, seed, 2, 5)
        moss = ramp("#4a6a2a", "#9ac45a", 4)
        f = noise(seed + 3, 2, 3)
        q = quantize(f, 12)
        for y in range(S):
            for x in range(S):
                if q[y][x] >= 10:
                    put(img, x, y, moss[1 if q[y][x] == 10 else 2])
        return img
    if style == "rings":
        f = noise(seed, 6, 2)
        for y in range(S):
            for x in range(S):
                f[y][x] = f[y][x] * 0.55 + (1 - (y % 4) / 3) * 0.9
        q = quantize(f, 6)
        for y in range(S):
            for x in range(S):
                put(img, x, y, pal[q[y][x]])
        # little leaf-scar diamonds on the rings
        for (x, y) in ((3, 1), (11, 5), (6, 9), (14, 13)):
            put(img, x, y, pal[1]); put(img, x + 1, y, pal[2])
        return img
    cw, ch = (2, 5) if style == "v" else (5, 2)
    fill_noise(img, pal, 0, 5, seed, cw, ch)
    return img


# ---------------------------------------------------------------- log ends

def log_top(name, w, stripped=False):
    bd, bl, wd, wl, _ = w
    wood, barkp = ramp(wd, wl, 6), ramp(bd, bl, 6)
    style = STYLE[name]["top"]
    img = canvas()
    rnd = random.Random(sum(map(ord, name)) + (7 if stripped else 0))
    for y in range(S):
        for x in range(S):
            dx, dy = x - 7.5, y - 7.5
            edge = min(x, y, S - 1 - x, S - 1 - y)
            if not stripped and edge <= 1:
                put(img, x, y, barkp[1 if edge == 0 else (3 if dx + dy < 0 else 2)])
                continue
            glow = 4.3 - (dx + dy) * 0.1 + rnd.uniform(-0.2, 0.2)
            if style == "squares":
                d = max(abs(dx), abs(dy))
                line = int(d) % 3 == 0
                v = glow - d * 0.28 - (1.4 if line else 0)
            elif style == "fibre":
                d = math.hypot(dx, dy)
                v = glow - d * 0.25
                if (x * 3 + y * 5) % 7 == 0:
                    v -= 1.3  # palm fibre dots
            else:
                d = (abs(dx) ** 3.2 + abs(dy) ** 3.2) ** (1 / 3.2)
                ang = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
                line = ((d + ang * 2.5) % 2.5) < 0.95
                v = glow - d * 0.32 - (1.4 if line else 0)
            if stripped and edge == 0:
                v -= 1.2
            put(img, x, y, wood[max(0, min(5, int(round(v))))])
    if style == "squares":
        put(img, 7, 7, wood[1]); put(img, 8, 8, wood[2])
    return img


# ---------------------------------------------------------------- planks

def planks(name, w):
    _, _, wd, wl, _ = w
    pal = ramp(wd, wl, 6)
    style = STYLE[name]["planks"]
    img = canvas()
    seed = sum(map(ord, name)) + 11
    if style == "vertical":
        # Tongue-and-groove boards standing upright, staggered ends, tiny flower-shaped nail heads.
        fill_noise(img, pal, 2, 4, seed, 1, 6)
        ends = [3, 11, 7, 14]
        for b in range(4):
            x0 = b * 4
            for y in range(S):
                put(img, x0, y, pal[4])
                put(img, x0 + 3, y, pal[1])
            ey = ends[b]
            for x in range(x0, x0 + 4):
                put(img, x, ey, pal[1]); put(img, x, (ey + 1) % S, pal[4])
            # a little flower nail head beside the board end
            fx, fy = x0 + 1, (ey + 3) % S
            put(img, fx + 1, fy, pal[5]); put(img, fx, fy + 1, pal[5]); put(img, fx + 2, fy + 1, pal[5]); put(img, fx + 1, fy + 2, pal[5])
            put(img, fx + 1, fy + 1, pal[1])
        return img
    if style == "diagonal":
        # Slanted boards running corner to corner, each with a light leading edge and a soft seam.
        fill_noise(img, pal, 2, 4, seed, 3, 3)
        for y in range(S):
            for x in range(S):
                u = (x + y) % 5
                if u == 0:
                    put(img, x, y, pal[1])
                elif u == 1:
                    put(img, x, y, pal[4])
        for (x, y) in ((3, 9), (12, 4), (8, 14)):
            put(img, x, y, pal[1]); put(img, (x + 1) % S, (y + 1) % S, pal[5])
        return img
    if style == "bamboo":
        # Upright bamboo stalks: rounded (light middle, darker sides) with a node ring every few pixels.
        for b in range(4):
            x0 = b * 4
            off = [0, 5, 2, 7][b]
            for y in range(S):
                node = (y + off) % 8 == 0
                for i, k in enumerate((2, 4, 3, 1)):
                    put(img, x0 + i, y, pal[1 if node and i > 0 else k] if not node else pal[2 if i in (0, 3) else 1])
                if (y + off) % 8 == 1:
                    put(img, x0 + 1, y, pal[5]); put(img, x0 + 2, y, pal[4])
        return img
    if style == "framed":
        # Mahogany-style: boards framed by darker rails, with H-shaped joints.
        fill_noise(img, pal, 2, 4, seed, 6, 1)
        for y in (0, 7, 8, 15):
            for x in range(S):
                put(img, x, y, pal[1] if y in (7, 15) else pal[4])
        for (x, y1, y2) in ((4, 1, 6), (11, 9, 14)):
            for y in range(y1, y2 + 1):
                put(img, x, y, pal[1]); put(img, x + 1, y, pal[4])
            put(img, x - 1, (y1 + y2) // 2, pal[1]); put(img, x + 2, (y1 + y2) // 2, pal[1])
        return img
    if style == "herring":
        fill_noise(img, pal, 2, 4, seed, 2, 2)
        for y in range(S):
            for x in range(S):
                u = (x + (y if (x // 4) % 2 == 0 else -y)) % 4
                if u == 0:
                    put(img, x, y, pal[1])
                elif u == 1:
                    put(img, x, y, pal[4])
        return img
    if style == "woven":
        for y in range(S):
            for x in range(S):
                cell = ((x // 4) + (y // 4)) % 2
                t = (y % 4) if cell == 0 else (x % 4)
                put(img, x, y, pal[[2, 4, 3, 1][t]])
        return img
    if style == "carved":
        # Plum: a carved ornament (a ring with a cross of petals) inset in each square panel.
        fill_noise(img, pal, 2, 3, seed, 3, 3)
        for (ox, oy) in ((0, 0), (8, 8), (8, 0), (0, 8)):
            frame(img, ox, oy, ox + 7, oy + 7, pal[1], pal[4])
            cx, cy = ox + 3.5, oy + 3.5
            for y in range(oy + 1, oy + 7):
                for x in range(ox + 1, ox + 7):
                    d = math.hypot(x - cx, y - cy)
                    if 1.6 < d < 2.6:
                        put(img, x, y, pal[5] if (x - cx) + (y - cy) < 0 else pal[1])
            put(img, ox + 3, oy + 3, pal[1]); put(img, ox + 4, oy + 4, pal[5])
        return img
    # board layouts: narrow (3px boards), nails (4px + dots), wide (8px + grain lines), default staggered
    bh = {"narrow": 3, "wide": 8}.get(style, 4)
    fill_noise(img, pal, 2, 4, seed, 6, 1)
    rnd = random.Random(seed)
    y = 0
    b = 0
    while y < S:
        for x in range(S):
            put(img, x, y, pal[4])
            if y + bh - 1 < S:
                put(img, x, y + bh - 1, pal[1])
        joint = (3 + b * 7) % S
        for yy in range(y, min(S, y + bh - 1)):
            put(img, joint, yy, pal[1]); put(img, (joint + 1) % S, yy, pal[4])
        if style == "nails":
            for nx in ((joint - 2) % S, (joint + 3) % S):
                put(img, nx, y + 1, pal[0])
        if style == "wide":
            for gx in (rnd.randrange(2, 7), rnd.randrange(9, 14)):
                for yy in range(y + 2, y + bh - 2):
                    put(img, gx, yy, pal[2])
        y += bh
        b += 1
    return img


# ---------------------------------------------------------------- doors and trapdoors

def door(name, w):
    """Returns (top, bottom). Drawn as one 16x32 door; transparent pixels are cut-outs/glass gaps."""
    _, _, wd, wl, _ = w
    pal = ramp(wd, wl, 6)
    style = STYLE[name]["door"]
    img = canvas(16, 32)
    seed = sum(map(ord, name)) + 23
    fill_noise(img, pal, 2, 4, seed, 2, 6)
    frame(img, 0, 0, 15, 31, pal[1], pal[4])
    frame(img, 1, 1, 14, 30, pal[2], pal[4])
    glass = (196, 228, 238)
    if style == "panel":
        frame(img, 3, 3, 12, 28, pal[4], pal[1])
        put(img, 13, 17, (232, 184, 74)); put(img, 13, 18, (190, 140, 50))
    elif style == "heart":
        heart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
        for j, row in enumerate(heart):
            for i, ch in enumerate(row):
                if ch == "1":
                    put(img, 4 + i, 4 + j, glass, 150)
        frame(img, 3, 13, 12, 28, pal[4], pal[1])
        for (x, y) in ((7, 19), (6, 20), (8, 20), (7, 21), (7, 20)):
            put(img, x, y, pal[5] if (x, y) == (7, 20) else pal[1])
    elif style == "slats":
        rect(img, 4, 4, 11, 9, glass)
        frame(img, 3, 3, 12, 10, pal[1], pal[4])
        for y in range(13, 29, 3):
            for x in range(3, 13):
                put(img, x, y, pal[1]); put(img, x, y + 1, pal[4])
    elif style == "porthole":
        for y in range(3, 12):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, y - 7)
                if d < 3.6:
                    put(img, x, y, glass, 160)
                elif d < 4.6:
                    put(img, x, y, pal[1] if y > 7 else pal[4])
        frame(img, 3, 15, 12, 21, pal[4], pal[1])
        frame(img, 3, 23, 12, 28, pal[4], pal[1])
    elif style == "lattice":
        for y in range(3, 29):
            for x in range(3, 13):
                if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                    put(img, x, y, pal[1])
                elif (x + y) % 4 == 2 and (x - y) % 4 == 2:
                    put(img, x, y, (0, 0, 0), 0)
    elif style == "ornate":
        for (cy) in (8, 23):
            for y in range(cy - 5, cy + 6):
                for x in range(3, 13):
                    d = math.hypot(x - 7.5, y - cy)
                    if 2.5 < d < 3.7:
                        put(img, x, y, pal[5] if y < cy else pal[1])
                    elif d < 1.2:
                        put(img, x, y, pal[1])
            for (dx, dy) in ((0, -4), (0, 4), (-4, 0), (4, 0)):
                put(img, int(7.5 + dx), cy + dy, pal[0])
        for y in range(13, 18):
            put(img, 7, y, pal[1]); put(img, 8, y, pal[5])
    elif style == "bamboo":
        for x in range(2, 14):
            k = [1, 4, 3, 3][x % 4]
            for y in range(2, 30):
                if y % 7 == 0:
                    put(img, x, y, pal[1])
                else:
                    put(img, x, y, pal[k])
        put(img, 12, 16, (232, 184, 74))
    return img.crop((0, 0, 16, 16)), img.crop((0, 16, 16, 32))


def trapdoor(name, w):
    _, _, wd, wl, _ = w
    pal = ramp(wd, wl, 6)
    style = STYLE[name]["trap"]
    img = canvas()
    fill_noise(img, pal, 2, 4, sum(map(ord, name)) + 31, 2, 6)
    frame(img, 0, 0, 15, 15, pal[1], pal[4])
    hole = (0, 0, 0)
    if style == "flower":
        for (dx, dy) in ((0, -2), (-2, 0), (2, 0), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1), (0, -3), (-3, 0), (3, 0), (0, 3)):
            put(img, 7 + dx, 7 + dy, hole, 0)
        put(img, 7, 7, (246, 214, 90))
    elif style == "dots":
        for y in range(2, 14, 3):
            for x in range(2, 14):
                put(img, x, y, pal[1]); put(img, x, y + 1, pal[4])
        for (x, y) in ((2, 3), (13, 3), (2, 9), (13, 9)):
            put(img, x, y, pal[0])
        rect(img, 6, 6, 9, 9, hole)
        for y in range(6, 10):
            for x in range(6, 10):
                put(img, x, y, hole, 0)
    elif style == "framed":
        for k in (2, 4):
            frame(img, k, k, 15 - k, 15 - k, pal[4] if k == 2 else pal[1], pal[1] if k == 2 else pal[4])
        put(img, 7, 7, pal[1]); put(img, 8, 8, pal[5])
    elif style == "round":
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3:
                    put(img, x, y, hole, 0)
                elif d < 4:
                    put(img, x, y, pal[1] if y > 7 else pal[5])
    elif style == "lattice":
        for y in range(2, 14):
            for x in range(2, 14):
                if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                    put(img, x, y, pal[1])
                elif (x + y) % 4 == 2 and (x - y) % 4 == 2:
                    put(img, x, y, hole, 0)
    elif style == "ornament":
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 7.5)
                if 3 < d < 4.3:
                    put(img, x, y, pal[5] if y < 8 else pal[1])
                elif d < 1.5:
                    put(img, x, y, hole, 0)
        for (dx, dy) in ((0, -5), (0, 5), (-5, 0), (5, 0)):
            put(img, int(7.5 + dx), int(7.5 + dy), pal[0])
    elif style == "woven":
        for y in range(1, 15):
            for x in range(1, 15):
                cell = ((x // 4) + (y // 4)) % 2
                t = (y % 4) if cell == 0 else (x % 4)
                put(img, x, y, pal[[2, 4, 3, 1][t]])
    return img


# ---------------------------------------------------------------- leaves

LEAF = {
    # leaf dark, leaf light, style, see-through share
    "blossom": ("#a84e7c", "#ffd0e6", "blossoms", 0.16),
    "citrus": ("#244e22", "#7ec24e", "glossy", 0.10),
    "orangewood": ("#1e4220", "#5c9e3e", "dense", 0.06),
    "peachwood": ("#3a6a2c", "#a6d46a", "narrow", 0.24),
    "mangowood": ("#1f4a26", "#64a84a", "drooping", 0.14),
    "plumwood": ("#3e2e56", "#b49ad6", "round", 0.15),
    "palm": ("#245e24", "#8ccc5a", "fronds", 0.62),
}


def leaf_shape(img, x, y, angle, length, width, pal, wrap=True):
    """One pointed leaf at 16px: lit upper side, shaded lower side, a pale midrib. Wraps so the block tiles."""
    ca, sa = math.cos(angle), math.sin(angle)
    for t in range(length + 1):
        half = math.sin((t + 0.6) / (length + 1.2) * math.pi) * width
        sx = -half
        while sx <= half + 0.01:
            px, py = x + ca * t - sa * sx, y + sa * t + ca * sx
            k = 3 if sx < 0 else 1
            if abs(sx) < 0.4 and 0 < t < length:
                k = 4
            if t == length or abs(sx) > half - 0.3:
                k = 2 if sx < 0 else 0
            put(img, int(math.floor(px)) % S, int(math.floor(py)) % S, pal[k])
            sx += 0.5


def leaves(name):
    dark, light, style, gap = LEAF[name]
    pal = ramp(dark, light, 5)
    rnd = random.Random(sum(map(ord, name)) * 7)
    img = canvas()
    # A dark leafy underlayer in clumps; where it's missing (and no leaf lands) you can see through.
    f = noise(sum(map(ord, name)), 3, 3, ((1.0, 1.0), (0.5, 0.6)))
    flat = sorted(v for row in f for v in row)
    cut = flat[int(len(flat) * (gap + 0.14))]
    under = pal[1] if style in ("blossoms", "round") else pal[0]
    for yy in range(S):
        for xx in range(S):
            if f[yy][xx] > cut:
                put(img, xx, yy, under)
    if style == "blossoms":
        for _ in range(16):
            leaf_shape(img, rnd.uniform(0, S), rnd.uniform(0, S), rnd.uniform(0, 6.28), 2, 0.9, ramp("#2e5a2a", "#78b04c", 5))
        petal = ramp("#c8608e", "#fff0f6", 5)
        for (cx, cy) in ((3, 3), (11, 2), (7, 8), (14, 10), (2, 12), (10, 14), (6, 0), (14, 6), (1, 7), (6, 13)):
            for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                put(img, (cx + dx) % S, (cy + dy) % S, petal[4 if dy < 0 or dx < 0 else 3])
            for (dx, dy) in ((-1, -1), (1, 1)):
                put(img, (cx + dx) % S, (cy + dy) % S, petal[2])
            put(img, cx, cy, hx("#f6c84a"))
    elif style == "glossy":
        for _ in range(14):
            leaf_shape(img, rnd.uniform(0, S), rnd.uniform(0, S), rnd.uniform(0, 6.28), 3, 1.1, pal)
        for (x, y) in ((4, 5), (12, 11)):
            for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                put(img, x + dx, y + dy, (255, 252, 240))
            put(img, x, y, hx("#f6d65a"))
        put(img, 9, 3, hx("#f6dc4a")); put(img, 10, 3, hx("#e8c43a")); put(img, 9, 4, hx("#e8c43a"))
    elif style == "dense":
        for _ in range(22):
            leaf_shape(img, rnd.uniform(0, S), rnd.uniform(0, S), rnd.uniform(0, 6.28), 3, 1.0, pal)
        for (x, y) in ((5, 10), (12, 4)):
            put(img, x, y, hx("#ffb04a")); put(img, x + 1, y, hx("#f2902a")); put(img, x, y + 1, hx("#e2801e")); put(img, x + 1, y + 1, hx("#c86a16"))
    elif style == "narrow":
        for _ in range(12):
            leaf_shape(img, rnd.uniform(0, S), rnd.uniform(0, S), math.pi / 2 + rnd.uniform(-0.6, 0.6), 5, 0.6, pal)
        for (x, y) in ((6, 4), (13, 12)):
            put(img, x, y, hx("#ffc0d0")); put(img, x + 1, y, hx("#ff9ab4")); put(img, x, y + 1, hx("#ff9ab4"))
    elif style == "drooping":
        young = ramp("#7a2a2a", "#e88a6a", 5)
        for i in range(12):
            p = young if i in (3, 10) else pal
            leaf_shape(img, rnd.uniform(0, S), rnd.uniform(0, S), math.pi / 2 + rnd.uniform(-0.35, 0.35), 6, 0.9, p)
    elif style == "round":
        for _ in range(48):
            cx, cy = rnd.randrange(S), rnd.randrange(S)
            put(img, cx, cy, pal[3]); put(img, (cx + 1) % S, cy, pal[2]); put(img, cx, (cy + 1) % S, pal[1]); put(img, (cx + 1) % S, (cy + 1) % S, pal[1])
            put(img, cx, cy, pal[4] if rnd.random() < 0.3 else pal[3])
        for (x, y) in ((4, 12), (11, 6)):
            put(img, x, y, hx("#9a5ab4")); put(img, x + 1, y, hx("#7a3e96")); put(img, x, y + 1, hx("#7a3e96"))
    elif style == "fronds":
        # Three feathery fronds sweeping diagonally: a midrib with slanted leaflets on both sides, lots of air.
        for (ox, oy, slope) in ((0, 2, 0.45), (5, 9, 0.35), (10, 13, 0.55)):
            for t in range(16):
                mx, my = (ox + t) % S, oy + t * slope
                put(img, mx, int(my) % S, pal[2])
                if t % 2 == 0:
                    for k in range(1, 4):
                        put(img, (mx - k // 2) % S, int(my - k) % S, pal[4 if k == 1 else 3])
                        put(img, (mx - k // 2) % S, int(my + k) % S, pal[2 if k == 1 else 1])
    return img


# ---------------------------------------------------------------- saplings

def sapling(name, w):
    bd, bl, _, _, _ = w
    trunk = ramp(bd, bl, 4)
    img = canvas()
    style = STYLE[name]["sapling"]
    leaf_cols = {"blossom": ("#c8608e", "#ffc6e2"), "plum": ("#5a4478", "#c4a8e2")}.get(style, ("#2e6a2a", "#86c858"))
    lf = ramp(leaf_cols[0], leaf_cols[1], 4)
    for y in range(9, 16):
        put(img, 7, y, trunk[2]); put(img, 8, y, trunk[1])
    if style == "palm":
        for y in range(6, 16):
            put(img, 7 + (1 if y < 9 else 0), y, trunk[2])
        for (dx, sgn) in ((1, 1), (1, -1), (0, 1), (0, -1)):
            for t in range(1, 6):
                put(img, 8 + sgn * t, 6 - dx + (t * t) // 6, lf[3 if t < 3 else 2])
                put(img, 8 + sgn * t, 7 - dx + (t * t) // 6, lf[1])
        return img
    if style == "mango":
        for y in range(5, 16):
            put(img, 7, y, trunk[2])
    # leafy crown: soft clusters lit top-left
    rnd = random.Random(sum(map(ord, name)))
    blobs = {"lemon": [(7, 6, 3.2)], "orange": [(7, 6, 3.4)], "peach": [(5, 6, 2.5), (10, 6, 2.5), (7, 4, 2.4)],
             "mango": [(7, 4, 3.0), (4, 7, 2.0), (11, 7, 2.0)], "blossom": [(5, 5, 2.4), (10, 5, 2.4), (7, 3, 2.2), (7, 7, 2.2)],
             "plum": [(4, 5, 2.0), (11, 4, 2.0), (7, 3, 2.2), (8, 7, 2.0)]}[style]
    for (cx, cy, r) in blobs:
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r:
                    lit = (x - cx) + (y - cy)
                    put(img, x, y, lf[3] if lit < -1 else lf[2] if lit < 1.5 else lf[1])
    fruit = {"lemon": "#f6dc4a", "orange": "#f29a2a", "peach": "#ffb6c8", "mango": "#f2b03a"}.get(style)
    if fruit:
        for (x, y) in ((5, 8), (10, 7)):
            put(img, x, y, hx(fruit))
    return img


def build():
    n = 0
    for name, w in WOODS.items():
        bark(name, w).save(os.path.join(TEX, f"{name}_log.png"))
        log_top(name, w).save(os.path.join(TEX, f"{name}_log_top.png"))
        log_top(name, w, stripped=True).save(os.path.join(TEX, f"stripped_{name}_log_top.png"))
        planks(name, w).save(os.path.join(TEX, f"{name}_planks.png"))
        top, bottom = door(name, w)
        top.save(os.path.join(TEX, f"{name}_door_top.png"))
        bottom.save(os.path.join(TEX, f"{name}_door_bottom.png"))
        trapdoor(name, w).save(os.path.join(TEX, f"{name}_trapdoor.png"))
        sapling(name, w).save(os.path.join(TEX, f"{name}_sapling.png"))
        leaves(name).save(os.path.join(TEX, f"{name}_leaves.png"))
        n += 8
    return n


if __name__ == "__main__":
    print(build(), "unique wood textures written")
