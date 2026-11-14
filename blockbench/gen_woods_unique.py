"""
A unique look for every wood, on top of the soft 16x16 painterly base (gen_woods16.py).

Inspired by the vibe of hand-made wood sets (framed mahogany planks, carved ornamental woods, pale woods with nail
dots, concentric-square log ends, plated bark) - not copied: each of our woods gets its own design for bark, log end,
planks, door, trapdoor and sapling, all in the same soft, low-contrast pixel style.

    cherry blossom  soft horizontal bark   spiral end      calm long boards    heart-window door, flower trapdoor
    citrus          pale, dark dash marks  square rings    calm upright boards slatted door with window, dotted slats
    orangewood      soft rounded plates    square rings    framed H-joints     4-pane window door, 4-panel trapdoor
    peachwood       soft vertical          spiral          wide rounded boards two panels + porthole, round window
    mangowood       furrowed, mossy        square rings    wide grained boards centred lattice door and trapdoor
    plumwood        smooth with lenticels  spiral          carved ornament     carved ornate door, rosette trapdoor
    palm            ringed trunk           fibrous dotted  woven strips        louvred shutter door, woven trapdoor

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
    "blossom": dict(bark="h", top="spiral", planks="soft_h", door="heart", trap="flower", sapling="blossom"),
    "citrus": dict(bark="eyes", top="squares", planks="soft_v", door="slats", trap="dots", sapling="lemon"),
    "orangewood": dict(bark="plates", top="squares", planks="framed", door="classic", trap="panels", sapling="orange"),
    "peachwood": dict(bark="v", top="spiral", planks="classic", door="porthole", trap="round", sapling="peach"),
    "mangowood": dict(bark="mossy", top="squares", planks="wide", door="lattice", trap="lattice", sapling="mango"),
    "plumwood": dict(bark="lenticel", top="spiral", planks="diagonal", door="ornate", trap="rosette", sapling="plum"),
    "palm": dict(bark="rings", top="fibre", planks="bamboo", door="louver", trap="woven", sapling="palm"),
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
        # Mango: upright furrowed bark plates (every column gets the same share of furrows, so no edge looks cut)
        # with a few soft moss patches spread evenly over the tile.
        f = noise(seed, 2, 6, ((1.0, 1.0), (0.5, 0.4)))
        q = quantize(f, 3)
        wob = noise(seed + 9, 16, 4, ((1.0, 1.0),))
        for y in range(S):
            for x in range(S):
                put(img, x, y, pal[2 + q[y][x]])
        for y in range(S):
            shift = 1 if wob[y][0] > 0.5 else 0
            for fx in (1, 5, 9, 13):                 # four furrows, 4px apart, gently wandering
                x = (fx + shift + (1 if (y // 5 + fx) % 3 == 0 else 0)) % S
                put(img, x, y, pal[0])
                put(img, (x + 1) % S, y, pal[1])
                put(img, (x - 1) % S, y, pal[4] if (x + y) % 3 else pal[3])
        moss = ramp("#4a6a2a", "#9ac45a", 4)
        mf = noise(seed + 3, 4, 4, ((1.0, 1.0), (0.5, 0.5)))
        mq = quantize(mf, 10)
        for y in range(S):
            for x in range(S):
                if mq[y][x] >= 9:                      # the top tenth: soft, irregular moss patches
                    lit = mq[(y - 1) % S][x] < 9 or mq[y][(x - 1) % S] < 9
                    put(img, x, y, moss[2] if lit else moss[1])
        return img
    if style == "lenticel":
        # Plum: smooth dusky bark in calm, even tones, crossed by short pale horizontal lenticels (like real plum
        # and cherry bark), spread evenly so no corner of the block is darker than another.
        f = noise(seed, 6, 2, ((1.0, 1.0), (0.5, 0.35)))
        q = quantize(f, 3)
        for y in range(S):
            for x in range(S):
                put(img, x, y, pal[2 + q[y][x]])
        for (x, y, l) in ((1, 1, 3), (9, 2, 4), (5, 5, 3), (13, 6, 3), (0, 9, 4), (8, 9, 3), (4, 13, 4), (12, 13, 3)):
            for t in range(l):
                put(img, (x + t) % S, y, pal[5])
                put(img, (x + t) % S, y + 1, pal[1])
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
    if style == "classic":
        # Peach: the familiar four-board plank layout in a brown-pink, each board with a lit top row, a soft seam,
        # one butt joint and a couple of quiet grain streaks; the joints step across like a stair (its own touch).
        pal = ramp("#6e4034", "#d6a08c", 6)
        rnd = random.Random(seed)
        joints = [2, 6, 10, 14]
        for b in range(4):
            y0 = b * 4
            for r in range(4):
                for x in range(S):
                    k = 3
                    if r == 0:
                        k = 4
                    elif r == 3:
                        k = 1
                    put(img, x, y0 + r, pal[k])
            j = joints[b]
            for r in range(3):
                put(img, j, y0 + r, pal[1])
            put(img, (j + 1) % S, y0 + 1, pal[4])
            for _ in range(2):
                gx, gy = rnd.randrange(S), y0 + rnd.choice((1, 2))
                for k in range(rnd.choice((3, 4))):
                    if (gx + k) % S != j:
                        put(img, gx + k, gy, pal[2])
        return img
    if style in ("soft_h", "soft_v", "brick"):
        # Calm boards: two close tones inside each board, every board a shade apart, one soft seam, no details.
        rnd = random.Random(seed)
        fill = noise(seed, 6, 1, ((1.0, 1.0),)) if style != "soft_v" else noise(seed, 1, 6, ((1.0, 1.0),))
        if style == "soft_h":      # cherry: 4 long boards, ends staggered
            boards = [(0, y, S, 4, j) for y, j in zip(range(0, S, 4), (4, 12, 8, 0))]   # ends spread evenly
        elif style == "soft_v":    # citrus: 4 upright boards, ends staggered
            boards = [(x, 0, 4, S, (5 + 7 * i) % S) for i, x in enumerate(range(0, S, 4))]
        else:                      # peach: three wide rounded boards (5, 6, 5 px), lit along the top edge
            boards = [(0, 0, S, 5, 6), (0, 5, S, 6, 13), (0, 11, S, 5, 2)]
        tone = [2, 3, 2, 3, 3, 2, 3, 2]
        gold = ramp("#a8784a", "#f6d290", 6)
        for i, (bx, by, bw, bh, joint) in enumerate(boards):
            base = tone[i % len(tone)]
            for yy in range(bh):
                for xx in range(bw):
                    x, y = (bx + xx) % S, (by + yy) % S
                    k = min(3, base + (1 if fill[y][x] > 0.66 else 0))
                    seam = (yy == bh - 1) if style != "soft_v" else (xx == bw - 1)
                    if seam:
                        k = 1
                    elif style == "brick" and yy == 0:
                        k = 4
                    elif joint is not None and ((style in ("soft_h", "brick") and x == joint) or (style == "soft_v" and y == joint)):
                        k = 1
                    c = pal[min(5, k)]
                    if name == "blossom" and k == 3 and fill[y][x] > 0.66:
                        c = tuple((a * 2 + b) // 3 for a, b in zip(gold[4], c))   # cherry: soft gold grain in the pink
                    put(img, x, y, c)
        return img
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
        # The board width (8) divides the tile, so the slant runs on unbroken from block to block.
        fill_noise(img, pal, 2, 3, seed, 3, 3)
        for y in range(S):
            for x in range(S):
                u = (x + y) % 8
                if u == 0:
                    put(img, x, y, pal[1])
                elif u == 1:
                    put(img, x, y, pal[4])
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

# ---------------------------------------------------------------- symmetric cut-outs (v2)
# Everything here is mirror-symmetric about the tile's true centre (7.5, 7.5), so nothing looks pushed aside.

def cutout(img, holes, pal):
    """Punches the holes and gives each a carved rim: the wood above/left of a hole is in shadow (you look at the
    inner wall), below/right it catches the light."""
    holes = set(holes)
    for (x, y) in holes:
        put(img, x, y, (0, 0, 0), 0)
    for (x, y) in holes:
        for (dx, dy, k) in ((0, -1, 1), (-1, 0, 1), (0, 1, 4), (1, 0, 4)):
            q = (x + dx, y + dy)
            if q not in holes and 0 <= q[0] < img.width and 0 <= q[1] < img.height:
                put(img, q[0], q[1], pal[k])


def stagger_holes(y0, y1):
    """2x2 holes in a staggered (diamond) mesh: rows of three and two alternate, centred on x = 7.5."""
    out = []
    for i, y in enumerate(range(y0, y1 + 1, 4)):
        for x in ((3, 7, 11) if i % 2 == 0 else (5, 9)):
            out += [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)]
    return out


def recessed(img, x1, y1, x2, y2, pal, inner=3):
    """A sunken panel: shadowed top/left inner edge, lit bottom/right edge, a calm face."""
    rect(img, x1, y1, x2, y2, pal[inner])
    for x in range(x1, x2 + 1):
        put(img, x, y1, pal[1]); put(img, x, y2, pal[5])
    for y in range(y1, y2 + 1):
        put(img, x1, y, pal[1]); put(img, x2, y, pal[5])
    put(img, x2, y1, pal[3]); put(img, x1, y2, pal[3])


def knob(img, x, y):
    put(img, x, y, (246, 210, 110)); put(img, x, y + 1, (200, 148, 56)); put(img, x - 1, y + 1, (150, 100, 40))


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
    # Window glass is see-through: a faint blue tint you can look through, and a brighter glint in one corner.
    glass, glass_a = (214, 238, 248), 64
    shine, shine_a = (255, 255, 255), 150

    def pane(x1, y1, x2, y2):
        for yy in range(y1, y2 + 1):
            for xx in range(x1, x2 + 1):
                put(img, xx, yy, glass, glass_a)

    if style == "classic":
        # Orangewood: a proper cottage door - four-pane window on top, two sunken panels below, a brass knob.
        fill_noise(img, pal, 3, 4, seed, 2, 8, region=lambda x, y: 2 <= x <= 13 and 2 <= y <= 29)
        frame(img, 0, 0, 15, 31, pal[1], pal[4])
        frame(img, 1, 1, 14, 30, pal[2], pal[5])
        for (x1, y1) in ((3, 3), (9, 3), (3, 8), (9, 8)):
            pane(x1, y1, x1 + 3, y1 + 3)
            put(img, x1, y1, shine, shine_a); put(img, x1 + 1, y1, shine, shine_a); put(img, x1, y1 + 1, shine, shine_a)
        for y in range(3, 12):
            put(img, 7, y, pal[4]); put(img, 8, y, pal[2])
        for x in range(3, 13):
            put(img, x, 7, pal[4])
        put(img, 7, 7, pal[5]); put(img, 8, 7, pal[3])
        frame(img, 2, 2, 13, 12, pal[4], pal[1])
        recessed(img, 3, 15, 6, 28, pal)
        recessed(img, 9, 15, 12, 28, pal)
        knob(img, 13, 16)
    elif style == "heart":
        heart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
        for j, row in enumerate(heart):
            for i, ch in enumerate(row):
                if ch == "1":
                    put(img, 4 + i, 4 + j, shine if (i, j) in ((1, 1), (2, 1), (1, 2)) else glass, shine_a if (i, j) in ((1, 1), (2, 1), (1, 2)) else glass_a)
        frame(img, 3, 13, 12, 28, pal[4], pal[1])
        for (x, y) in ((7, 19), (6, 20), (8, 20), (7, 21), (7, 20)):
            put(img, x, y, pal[5] if (x, y) == (7, 20) else pal[1])
    elif style == "slats":
        pane(4, 4, 11, 9)
        put(img, 4, 4, shine, shine_a); put(img, 5, 4, shine, shine_a); put(img, 4, 5, shine, shine_a)
        frame(img, 3, 3, 12, 10, pal[1], pal[4])
        for y in range(13, 29, 3):
            for x in range(3, 13):
                put(img, x, y, pal[1]); put(img, x, y + 1, pal[4])
    elif style == "porthole":
        for y in range(3, 12):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, y - 7)
                if d < 3.6:
                    glint = (x - 7.5) + (y - 7) < -2.5 and d > 1.8
                    put(img, x, y, shine if glint else glass, shine_a if glint else glass_a)
                elif d < 4.6:
                    put(img, x, y, pal[1] if y > 7 else pal[4])
        frame(img, 3, 15, 12, 21, pal[4], pal[1])
        frame(img, 3, 23, 12, 28, pal[4], pal[1])
    elif style == "lattice":
        cutout(img, stagger_holes(3, 27), pal)
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
    elif style == "louver":
        # Palm: a smooth tropical shutter door - angled louvres on top, one calm sunken panel below, brass knob.
        rect(img, 2, 2, 13, 29, pal[4])
        for y in range(2, 30):
            for x in range(2, 14):
                if (x * 7 + y * 3) % 11 == 0:
                    put(img, x, y, pal[3])          # the faintest grain, nothing noisy
        frame(img, 0, 0, 15, 31, pal[2], pal[5])
        frame(img, 1, 1, 14, 30, pal[3], pal[5])
        for y in range(3, 15):
            for x in range(3, 13):
                put(img, x, y, (pal[5], pal[4], pal[2])[(y - 3) % 3])
        for y in range(3, 15):
            put(img, 3, y, pal[2]); put(img, 12, y, pal[3])
        frame(img, 2, 2, 13, 15, pal[5], pal[2])
        recessed(img, 3, 18, 12, 28, pal, inner=4)
        knob(img, 13, 16)
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
        # Cherry: a four-petal blossom cut right through the middle, a golden heart, and a calm inset border.
        frame(img, 2, 2, 13, 13, pal[4], pal[1])
        petals = [(x, y) for y in range(S) for x in range(S)
                  if any(math.hypot(x - px, y - py) <= 1.75 for (px, py) in ((7.5, 4.5), (4.5, 7.5), (10.5, 7.5), (7.5, 10.5)))
                  and not (7 <= x <= 8 and 7 <= y <= 8)]
        cutout(img, petals, pal)
        for (x, y, c) in ((7, 7, (255, 226, 120)), (8, 7, (246, 204, 90)), (7, 8, (246, 204, 90)), (8, 8, (214, 164, 64))):
            put(img, x, y, c)
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
    elif style == "panels":
        # Orangewood: a sturdy frame with a cross rail and four sunken panels, a brass boss where the rails meet.
        rect(img, 0, 0, 15, 15, pal[4])
        for (x, y) in ((4, 0), (11, 1), (1, 7), (14, 9), (7, 14), (5, 15)):
            put(img, x, y, pal[3])
        frame(img, 0, 0, 15, 15, pal[2], pal[5])
        for (x1, y1) in ((2, 2), (9, 2), (2, 9), (9, 9)):
            recessed(img, x1, y1, x1 + 4, y1 + 4, pal)
            put(img, x1 + 1, y1 + 1, pal[2])
        for (x, y, c) in ((7, 7, (246, 210, 110)), (8, 7, (224, 176, 80)), (7, 8, (224, 176, 80)), (8, 8, (170, 120, 46))):
            put(img, x, y, c)
    elif style == "round":
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3:
                    put(img, x, y, hole, 0)
                elif d < 4:
                    put(img, x, y, pal[1] if y > 7 else pal[5])
    elif style == "lattice":
        frame(img, 1, 1, 14, 14, pal[2], pal[5])
        cutout(img, stagger_holes(3, 11), pal)
    elif style == "rosette":
        # Plum: a round carved window split by a cross into four panes, a raised ring, and a plum flower stud in
        # each corner - all centred on the middle of the block.
        rect(img, 0, 0, 15, 15, pal[3])
        frame(img, 0, 0, 15, 15, pal[1], pal[4])
        frame(img, 1, 1, 14, 14, pal[2], pal[4])
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 7.5)
                diag = (x - 7.5) + (y - 7.5)
                if 3.0 <= d < 4.2:
                    put(img, x, y, pal[5] if diag < -1 else pal[1] if diag > 1 else pal[4])
                elif 4.2 <= d < 4.8:
                    put(img, x, y, pal[2] if diag < 0 else pal[4])
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.0:
                    if x in (7, 8) or y in (7, 8):
                        put(img, x, y, pal[4] if (x == 7 or y == 7) else pal[2])
                    else:
                        put(img, x, y, hole, 0)
        put(img, 7, 7, pal[5]); put(img, 8, 8, pal[1])
        for (cx, cy) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                put(img, cx + dx, cy + dy, pal[5] if dx + dy < 0 else pal[4])
            put(img, cx, cy, pal[1])
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
    if style == "peach":
        # A slim young peach tree: a forked stem with long, slender leaves sprouting in pairs and one pink blossom.
        from gen_leaves16 import clump, LEAF as LEAF16
        lp = [hx(c) for c in LEAF16["peachwood"][0]]
        img = canvas()
        for y in range(10, 16):
            put(img, 7, y, trunk[2])
        put(img, 8, 15, trunk[1]); put(img, 8, 14, trunk[1])
        for (x, y) in ((6, 9), (5, 8), (8, 9), (9, 8), (7, 9), (7, 8), (7, 7)):
            put(img, x, y, trunk[2])
        for (x, y, a) in ((3.6, 6.4, 0.5), (4.6, 4.6, -1.0), (10.8, 6.6, 2.6), (10.0, 4.4, -2.1), (7.4, 4.2, -1.55),
                          (5.2, 10.2, 2.7), (9.8, 10.4, 0.45)):
            clump(img, x, y, 2.4, 0.8, a, lp)
        for (x, y, c) in ((8, 6, "#ffd2dc"), (9, 6, "#f6a8bc"), (8, 7, "#e88aa4")):
            put(img, x, y, hx(c))
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


# ---------------------------------------------------------------- saplings with depth (v2)
# A young tree is built from a few layers: darker leaves at the back, the stem, then lit leaves in front, each leaf
# casting a 1px shadow down-right. That front/back layering is what keeps a cross-model sapling from looking flat.

SAP_LEAF = {
    "lemon": ("#1f4a22", "#2f6a2c", "#4c8c3a", "#6eaa4c", "#94c86a"),
    "orange": ("#1c4220", "#2a6028", "#427e36", "#5e9c46", "#84bc5e"),
    "mango": ("#1a3e24", "#285a30", "#3c7a3e", "#5a9a4e", "#7eb862"),
    "plum": ("#3c2c56", "#5a4680", "#7862a2", "#9a84c2", "#bca6dc"),
    "palm": ("#1c4a1e", "#2c6a2a", "#468c36", "#66ac48", "#8ccc66"),
    "blossom": ("#2c4a24", "#44702e", "#5e8c3e", "#7caa52", "#9cc46c"),
}


def sap_leaf(img, cx, cy, length, width, ang, pal, back=False, cast=True):
    """An oval leaf lit from the upper left; back leaves sit one tone darker. Its 1px shadow only falls on leaves
    already drawn (never onto the air), so the crown keeps a clean silhouette."""
    ca, sa = math.cos(ang), math.sin(ang)
    rx, ry = length / 2, width / 2
    r = int(rx) + 3
    body, shadow = [], []
    for oy in range(-r, r + 1):
        for ox in range(-r, r + 1):
            px, py = ox + 0.5 - (cx % 1), oy + 0.5 - (cy % 1)
            u = (px * ca + py * sa) / rx
            v = (-px * sa + py * ca) / ry
            d = u * u + v * v
            X, Y = math.floor(cx) + ox, math.floor(cy) + oy
            if d <= 1.0:
                side = -(px + py) / (rx + ry)
                k = 3 if side > 0.15 else 2
                if d > 0.45 and side < -0.1:
                    k = 1
                if side > 0.45 and d < 0.6:
                    k = 4
                body.append((X, Y, k - 1 if back else k))
            elif cast and d <= 1.8 and px + py > 0.4:
                shadow.append((X, Y))
    for (X, Y) in shadow:
        if 0 <= X < img.width and 0 <= Y < img.height and img.getpixel((X, Y))[3]:
            put(img, X, Y, pal[0])
    for (X, Y, k) in body:
        put(img, X, Y, pal[k])


def blade(img, x0, y0, x1, y1, pal, back=False, leaflets=False):
    """A long pointed leaf from (x0, y0) to its tip (x1, y1): a lit upper edge, a shaded underside, a 1px tip.
    With leaflets it becomes a palm frond: a rib with little leaflets hanging under every other pixel."""
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    pts = []
    for i in range(n + 1):
        p = (round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n))
        if p not in pts:
            pts.append(p)
    steep = abs(y1 - y0) > abs(x1 - x0)
    off = ((1 if x1 >= x0 else -1), 0) if steep else (0, 1)
    lit, body, dark = (2, 1, 0) if back else (3, 2, 1)
    for i, (x, y) in enumerate(pts):
        tip = i == len(pts) - 1
        if leaflets:
            put(img, x, y, pal[lit + (1 if i == 1 and not back else 0)])
            if not tip:
                put(img, x, y + 1, pal[body])
                if i % 2 == 0:
                    put(img, x, y + 2, pal[dark])
            continue
        put(img, x, y, pal[lit + (1 if i == len(pts) // 3 and not back else 0)])
        if not tip and 0 < i:
            put(img, x + off[0], y + off[1], pal[dark if i > len(pts) // 2 else body])


def stem(img, pts, c_light, c_dark):
    """A 1px stem along a polyline, with a darker pixel tucked on the right where it bends (reads as round)."""
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            x, y = round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)
            put(img, x, y, c_light)
    for (x, y) in pts[1:-1]:
        put(img, x + 1, y, c_dark)


def trunk(img, x, y_top, y_bot, tr):
    for y in range(y_top, y_bot + 1):
        put(img, x, y, tr[2]); put(img, x + 1, y, tr[1])
    put(img, x, y_bot, tr[1]); put(img, x + 1, y_bot, tr[0])


def fruit(img, x, y, base, size=2):
    """A little hanging fruit: a lit upper-left pixel, a shaded lower-right one, a dark stalk pixel above."""
    c = hx(base)
    lite = tuple(min(255, v + 40) for v in c)
    dark = tuple(int(v * 0.72) for v in c)
    for dy in range(size):
        for dx in range(size):
            put(img, x + dx, y + dy, c)
    put(img, x, y, lite)
    put(img, x + size - 1, y + size - 1, dark)
    if size == 3:
        put(img, x + 1, y + 1, c); put(img, x + 2, y, c); put(img, x, y + 2, c)


def tiny_flower(img, x, y, petal, heart):
    for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
        put(img, x + dx, y + dy, petal)
    put(img, x, y, heart)


def sapling_v2(name, w):
    bd, bl, _, _, _ = w
    tr = ramp(bd, bl, 4)
    style = STYLE[name]["sapling"]
    lp = [hx(c) for c in SAP_LEAF[style]]
    img = canvas()
    if style == "palm":
        # A coconut sprouting a short ringed stem and a fan of arching fronds (the back ones darker).
        stalk = ramp("#5a4026", "#b08c5c", 4)
        for y in range(8, 13):
            put(img, 7, y, stalk[3] if y % 2 else stalk[2]); put(img, 8, y, stalk[1])
        for (x0, y0, x1, y1) in ((7, 6, 3, 2), (8, 6, 12, 2)):
            blade(img, x0, y0, x1, y1, lp, back=True, leaflets=True)
        for pts in (((7, 7), (4, 5), (1, 7), (0, 10)), ((8, 7), (11, 5), (14, 7), (15, 10)),
                    ((7, 7), (5, 6), (3, 8), (2, 11)), ((8, 7), (10, 6), (12, 8), (13, 11))):
            for (a, b) in zip(pts, pts[1:]):
                blade(img, a[0], a[1], b[0], b[1], lp, leaflets=True)
        for (x, y, k) in ((7, 6, 4), (8, 6, 3), (7, 7, 3), (8, 7, 2), (7, 5, 3), (8, 5, 2), (8, 4, 3), (7, 4, 4), (8, 3, 4)):
            put(img, x, y, lp[k])
        coco = [hx(c) for c in ("#3e2614", "#5c3a1e", "#7e5430", "#a07040", "#c08a52")]
        for (x, y, k) in ((6, 12, 3), (9, 12, 1), (5, 13, 3), (6, 13, 4), (7, 13, 3), (8, 13, 2), (9, 13, 2), (10, 13, 1),
                          (5, 14, 2), (6, 14, 3), (7, 14, 2), (8, 14, 2), (9, 14, 1), (10, 14, 0),
                          (6, 15, 1), (7, 15, 1), (8, 15, 1), (9, 15, 0)):
            put(img, x, y, coco[k])
        return img
    if style == "blossom":
        # A young cherry: a forked dark stem, a few green leaves at the back and soft clusters of blossom in front.
        from gen_leaves16 import FLOWER_BIG, FLOWER_SMALL, stamp
        trunk(img, 7, 10, 15, tr)
        stem(img, [(7, 10), (5, 8), (4, 6)], tr[2], tr[1])
        stem(img, [(8, 10), (10, 8), (11, 6)], tr[2], tr[1])
        stem(img, [(7, 9), (7, 5)], tr[2], tr[1])
        for (x, y, a) in ((2.5, 8.5, 2.6), (13.0, 8.5, 0.5), (5.0, 3.0, 2.2), (10.5, 3.0, 0.9)):
            sap_leaf(img, x, y, 4.2, 2.0, a, lp, back=True)
        back = [hx("#5a2a44")] + [hx(c) for c in ("#b45a88", "#cc76a2", "#e094ba", "#eeb0cc")]
        front = [hx("#7a3a5c")] + [hx(c) for c in ("#d985ab", "#f0a6c8", "#fcc4dc", "#ffe2ee")]
        for (x, y) in ((7, 0), (10, 3), (1, 4)):
            stamp(img, FLOWER_BIG, x, y, back, hx("#8a3a62"))
        for (x, y, big) in ((3, 1, 1), (8, 3, 1), (4, 6, 1), (11, 6, 0), (1, 7, 0), (11, 1, 0)):
            stamp(img, FLOWER_BIG if big else FLOWER_SMALL, x, y, front, hx("#b04a78"))
        return img
    if style == "plum":
        # A purple-leaf plum: a slim stem with dusky violet leaves in two layers and a few pale blossoms.
        trunk(img, 7, 10, 15, tr)
        stem(img, [(7, 10), (5, 8), (3, 6)], tr[2], tr[1])
        stem(img, [(8, 10), (10, 8), (12, 6)], tr[2], tr[1])
        stem(img, [(7, 9), (7, 4)], tr[2], tr[1])
        for (x, y, a) in ((3.2, 3.6, 2.4), (12.4, 3.4, 0.7), (7.6, 1.8, 1.57), (2.0, 7.4, 2.9), (13.6, 7.2, 0.2)):
            sap_leaf(img, x, y, 4.0, 2.6, a, lp, back=True)
        for (x, y, a) in ((4.4, 5.8, 2.5), (11.0, 5.4, 0.6), (5.8, 3.0, 2.0), (9.8, 2.8, 1.1), (5.0, 9.2, 2.8),
                          (10.6, 8.8, 0.35), (7.8, 5.8, 1.57)):
            sap_leaf(img, x, y, 3.8, 2.5, a, lp)
        for (x, y) in ((3, 3), (12, 3), (8, 1), (2, 8)):
            tiny_flower(img, x, y, hx("#fbeef6"), hx("#d0709c"))
        return img
    if style == "mango":
        # Mango leaves are long and hang in rosettes from the branch tips; the newest ones come out bronze.
        trunk(img, 7, 10, 15, tr)
        stem(img, [(7, 10), (7, 4)], tr[2], tr[1])
        stem(img, [(7, 9), (6, 8), (5, 7), (4, 6)], tr[2], tr[1])
        stem(img, [(8, 9), (9, 8), (10, 7), (11, 6)], tr[2], tr[1])
        for (x0, y0, x1, y1) in ((7, 4, 3, 3), (8, 4, 12, 3), (4, 6, 0, 6), (11, 6, 15, 6)):
            blade(img, x0, y0, x1, y1, lp, back=True)
        for (x0, y0, x1, y1) in ((4, 6, 1, 10), (4, 7, 3, 12), (5, 7, 6, 11), (11, 6, 14, 10), (11, 7, 12, 12),
                                 (10, 7, 9, 11), (7, 4, 4, 7), (8, 4, 11, 7)):
            blade(img, x0, y0, x1, y1, lp)
        bronze = [hx(c) for c in ("#4a2418", "#7a3a26", "#a65634", "#d08a52", "#e8a86e")]
        for (x, y, k) in ((7, 0, 3), (7, 1, 4), (8, 1, 2), (6, 2, 3), (7, 2, 3), (8, 2, 1), (9, 2, 2), (7, 3, 2), (8, 3, 1)):
            put(img, x, y, bronze[k])      # a limp bronze young shoot on top
        mango = [hx(c) for c in ("#b8642a", "#e8962e", "#f6b83e", "#ffd66a", "#e05a3a")]
        for (x, y, k) in ((7, 11, 2), (8, 11, 1), (6, 12, 3), (7, 12, 2), (8, 12, 1), (6, 13, 4), (7, 13, 2),
                          (8, 13, 0), (7, 14, 0)):
            put(img, x + 3, y - 1, mango[k])
        put(img, 10, 9, tr[1])
        return img
    # lemon and orange: a stem with two crowns of leaves (back and front) and hanging fruit
    crowns = {
        "lemon": dict(stem=[(7, 9), (7, 4)], arms=[[(7, 10), (5, 8), (4, 7)], [(8, 9), (10, 7), (11, 6)]],
                      back=[(4.0, 4.5, 2.5), (11.2, 4.2, 0.6), (7.5, 2.4, 1.57), (2.4, 8.0, 2.9), (13.2, 7.6, 0.25)],
                      front=[(5.0, 6.6, 2.3), (10.0, 6.0, 0.8), (6.0, 3.6, 2.0), (9.4, 3.2, 1.0), (7.6, 8.0, 1.7)],
                      size=(4.4, 2.6), fruit=("#f6dc4a", [(3, 9), (11, 8)], 2), flower=(5, 2)),
        "orange": dict(stem=[(7, 9), (7, 4)], arms=[[(7, 10), (5, 8), (3, 7)], [(8, 9), (10, 7), (12, 6)]],
                       back=[(3.4, 4.6, 2.4), (11.8, 4.4, 0.7), (7.5, 2.0, 1.57), (2.2, 7.6, 3.0), (13.4, 7.4, 0.15)],
                       front=[(4.6, 6.4, 2.6), (10.4, 6.2, 0.5), (5.8, 3.4, 2.1), (9.6, 3.0, 1.0), (7.6, 7.4, 1.6)],
                       size=(4.0, 2.7), fruit=("#f2922a", [(2, 9), (11, 8), (6, 4)], 2), flower=(10, 2)),
    }[style]
    trunk(img, 7, 10, 15, tr)
    stem(img, crowns["stem"], tr[2], tr[1])
    for arm in crowns["arms"]:
        stem(img, arm, tr[2], tr[1])
    ln, wd = crowns["size"]
    for (x, y, a) in crowns["back"]:
        sap_leaf(img, x, y, ln, wd, a, lp, back=True)
    for (x, y, a) in crowns.get("young", ()):
        sap_leaf(img, x, y, ln - 1, wd, a, [hx(c) for c in ("#3e2214", "#6a3a22", "#9a5a32", "#c07c46", "#d89a5e")])
    for (x, y, a) in crowns["front"]:
        sap_leaf(img, x, y, ln, wd, a, lp)
    col, spots, size = crowns["fruit"]
    for (x, y) in spots:
        put(img, x + size // 2, y - 1, tr[1])
        fruit(img, x, y, col, size)
    if "flower" in crowns:
        tiny_flower(img, *crowns["flower"], hx("#fffaf0"), hx("#f2d060"))
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
        (sapling(name, w) if name == "peachwood" else sapling_v2(name, w)).save(os.path.join(TEX, f"{name}_sapling.png"))
        n += 7
    return n


if __name__ == "__main__":
    print(build(), "unique wood textures written")
