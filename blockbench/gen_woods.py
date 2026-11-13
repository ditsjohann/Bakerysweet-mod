"""
Hand-drawn 32x32 textures for every wood set: planks, bark, log ends, stripped logs, leaves, sapling, door, trapdoor.

Every texture of a set is painted from that wood's own small palettes (plank, bark, leaf, blossom), with soft
same-hue shading - no harsh seams or random colour noise. Run: python3 blockbench/gen_woods.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb

S = 32

WOODS = {
    # planks, bark, leaf, blossom (or None), bark style, leaf style
    "palm": dict(planks="#e8cf9c", bark="#8a6a4a", leaf="#58a84e", bloom=None, bark_style="rings", leaf_style="fronds"),
    "citrus": dict(planks="#f2dc8c", bark="#8c7a62", leaf="#3f8f45", bloom="#fffaf0", bark_style="smooth", leaf_style="glossy"),
    "blossom": dict(planks="#f2b8c8", bark="#6a4a4e", leaf="#f2a9c6", bloom="#fff3f8", bark_style="lenticels", leaf_style="blossom"),
    "orangewood": dict(planks="#eaa86a", bark="#7c6a5c", leaf="#3c8a3e", bloom="#ffffff", bark_style="ridged", leaf_style="glossy"),
    "peachwood": dict(planks="#f4bfa0", bark="#7a5444", leaf="#5aa651", bloom="#ffb6c8", bark_style="ridged", leaf_style="dotted"),
    "mangowood": dict(planks="#e8bf62", bark="#5e4a36", leaf="#3d9447", bloom=None, bark_style="mossy", leaf_style="lush"),
    "plumwood": dict(planks="#c7a8dc", bark="#5e4a5e", leaf="#6f8f52", bloom="#c48ae0", bark_style="ridged", leaf_style="dotted"),
}


def ramp(c, n=5, spread=0.26):
    # Same-hue shading: tiny hue shift only, so seams stay a darker version of the wood, not a new colour.
    return shades(c, n, spread, 0.012)


# ---------------------------------------------------------------- planks

def planks(w, seed):
    p = ramp(w["planks"], 6, 0.24)
    rnd = random.Random(seed)
    cv = Canvas(S)
    board_h = 8
    for b in range(4):
        y0 = b * board_h
        tone = rnd.choice((2, 3, 3))
        joint = (b * 11 + rnd.randint(3, 9)) % S
        phase = rnd.uniform(0, 6.28)
        for y in range(y0, y0 + board_h):
            for x in range(S):
                k = tone
                ly = y - y0
                if ly == 0:
                    k = 0  # seam
                elif ly == 1:
                    k = min(4, tone + 1)  # top edge catches the light
                elif ly == board_h - 1:
                    k = max(1, tone - 1)
                else:
                    # soft grain lines that wander along the board
                    g = math.sin(x * 0.22 + phase + ly * 0.9) + math.sin(x * 0.07 + phase * 2)
                    if abs(g) < 0.18:
                        k = max(1, tone - 1)
                    elif g > 1.7:
                        k = min(4, tone + 1)
                if x == joint and ly > 0:
                    k = 0
                elif x == (joint + 1) % S and ly > 0:
                    k = min(4, tone + 1)
                cv.set(x, y, p[k])
        # nail heads beside the joint
        for nx in ((joint - 2) % S, (joint + 3) % S):
            cv.set(nx, y0 + 3, p[0])
            cv.set(nx, y0 + 5, p[0])
    return cv


# ---------------------------------------------------------------- bark

def soft(c, amount=0.18):
    """Pushes a colour toward a pastel: a little lighter and gentler, for the soft chibi look."""
    rr, gg, bb = rgb(c)
    return "#%02x%02x%02x" % tuple(int(v + (255 - v) * amount) for v in (rr, gg, bb))


def bark_color(w):
    """The bark, warmed toward the wood's own pastel and lightened: friendly instead of realistic."""
    br, bg, bb = rgb(w["bark"])
    pr, pg, pb = rgb(w["planks"])
    mixed = "#%02x%02x%02x" % (int(br * 0.7 + pr * 0.3), int(bg * 0.7 + pg * 0.3), int(bb * 0.7 + pb * 0.3))
    return soft(mixed, 0.3)


HEART_KNOT = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]


def knot(cv, x, y, b, heart=False):
    """A cute little knot: a soft ring with a lighter middle (or a heart)."""
    if heart:
        for j, row in enumerate(HEART_KNOT):
            for i, ch in enumerate(row):
                if ch == "1":
                    cv.set(x + i - 3, y + j - 3, b[1] if (i in (0, 6) or j == 5) else b[4] if j < 2 else b[3])
        return
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            d = abs(dx) + abs(dy)
            if d <= 3:
                cv.set(x + dx, y + dy, b[1] if d == 3 else b[4] if d == 0 else b[2])


def deep(c, n=6):
    """A ramp with real depth: dark, slightly cool shadows up to warm, light highlights."""
    return shades(c, n, 0.46, 0.04)


def bark(w, seed):
    """Chibi bark with volume: chunky rounded plates, each lit from the upper left and shading into its edges,
    with dark crevices between. Pastel colours, but real light and shadow."""
    b = deep(bark_color(w), 6)
    rnd = random.Random(seed)
    cv = Canvas(S)
    style = w["bark_style"]
    if style == "rings":
        # Palm: plump stacked rings, each a rounded roll lit from above.
        for y in range(S):
            t = (y % 8) / 7
            k = 5 if t < 0.15 else 4 if t < 0.35 else 3 if t < 0.6 else 2 if t < 0.85 else 0
            for x in range(S):
                kk = k
                if (x + (y // 8) * 5) % 16 in (0, 1) and k > 1:
                    kk -= 1  # little notches so it isn't too regular
                cv.set(x, y, b[kk])
        return cv
    # Plate centres on a staggered grid (wrapping), stretched tall.
    cells = []
    for row in range(2):
        for col in range(4):
            cx = col * 8 + (row % 2) * 4 + rnd.uniform(-0.8, 0.8)
            cy = row * 16 + rnd.uniform(-2, 2)
            cells.append((cx, cy))
    def metric(x, y, c):
        dx = (x - c[0] + S / 2) % S - S / 2
        dy = (y - c[1] + S / 2) % S - S / 2
        return math.hypot(dx, dy * 0.18), dx, dy
    for y in range(S):
        for x in range(S):
            ds = sorted((metric(x + 0.5, y + 0.5, c) for c in cells), key=lambda t: t[0])
            d1, dx, dy = ds[0]
            gap = ds[1][0] - d1
            if gap < 0.9:
                k = 0  # crevice
            else:
                lit = -(dx * 0.9 + dy * 0.18 * 0.5) / 3.8
                k = 3 + (1 if lit > 0.35 else 0) + (1 if lit > 0.8 else 0) - (1 if lit < -0.45 else 0)
                if gap < 1.8:
                    k = min(k, 2)  # plates darken into their edges
                k = max(1, min(5, k))
            cv.set(x, y, b[k])
    if style == "lenticels":
        for _ in range(10):
            x, y = rnd.randrange(S), rnd.randrange(S)
            if cv.get(x, y)[:3] != b[0]:
                cv.set(x, y, b[5]); cv.set(x + 1, y, b[4])
    if style == "mossy":
        moss = deep(soft("#7aa84a", 0.15), 5)
        for (mx, my) in ((6, 24), (22, 8)):
            for dy in range(-2, 3):
                for dx in range(-3, 4):
                    if dx * dx / 9 + dy * dy / 4 <= 1:
                        cv.set(mx + dx, my + dy, moss[3 if dy < 0 else 2 if dy == 0 else 1])
    if seed % 2 == 0:
        knot(cv, 16, 16, b, heart=True)
    return cv


def log_top(w, seed, stripped=False):
    """Log end with depth: a bark rim of rounded plates, rings that dip and rise, a shadow under the rim."""
    p = deep(soft(w["planks"], 0.1), 6)
    b = deep(bark_color(w), 6)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            edge = min(x, y, S - 1 - x, S - 1 - y)
            corner = min(x, S - 1 - x) + min(y, S - 1 - y)
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            ring = (d % 5.5) / 5.5
            k = 4 if ring < 0.45 else 3 if ring < 0.8 else 2
            if (x + y) < 26 and ring < 0.3:
                k = 5
            c = p[k]
            if not stripped:
                if edge < 3 or corner < 5:
                    lit = (x + y) < S
                    c = b[4 if lit else 2] if edge >= 1 and corner >= 3 else b[1]
                elif edge == 3:
                    c = p[1]  # shadow just inside the bark
            else:
                if edge == 0:
                    c = p[1]
                elif edge == 1:
                    c = p[2] if (x + y) > S else p[5]
            cv.set(x, y, c)
    for j, row in enumerate(HEART_KNOT):
        for i, ch in enumerate(row):
            if ch == "1":
                cv.set(13 + i, 13 + j, p[1] if j > 3 or i in (0, 6) else p[2])
    return cv


def stripped(w, seed):
    """Stripped log: smooth pastel wood in rounded vertical bands, each lit on its left side."""
    p = deep(soft(w["planks"], 0.1), 6)
    cv = Canvas(S)
    for x in range(S):
        u = x % 8
        k = [2, 4, 4, 3, 3, 3, 3, 2][u]
        for y in range(S):
            kk = k
            if (y * 5 + x * 3) % 37 == 0 and k > 2:
                kk = k - 1
            cv.set(x, y, p[kk])
    return cv


def leaf_blob(cv, x, y, angle, length, ramp_, rnd):
    """One small leaf: an oval along `angle`, darker on its lower side, with a pale midrib."""
    ca, sa = math.cos(angle), math.sin(angle)
    for t in range(-1, length + 1):
        half = max(0.6, math.sin((t + 1) / (length + 2) * math.pi) * length * 0.32)
        for s in (-2, -1, 0, 1, 2):
            if abs(s) > half:
                continue
            px, py = x + ca * t - sa * s, y + sa * t + ca * s
            k = 2 if s > 0 else 3
            if s == 0 and 0 < t < length - 1:
                k = 4
            cv.set(px, py, ramp_[k])


def leaves(w, seed):
    """Chibi leaves with volume: chunky round puffs with a dark outline, a lit upper left, a shaded lower right
    and a highlight; deep shadow between puffs and a few cute flowers. Wraps so the block tiles."""
    lr = deep(soft(w["leaf"], 0.08), 6)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=lr[0])

    def put(x, y, c):
        cv.set(int(math.floor(x)) % S, int(math.floor(y)) % S, c)

    puffs = [(x + rnd.uniform(-1.5, 1.5), y + rnd.uniform(-1.5, 1.5)) for y in range(0, S, 8) for x in range((y // 8) % 2 * 4, S, 8)]
    rnd.shuffle(puffs)
    for (cx, cy) in puffs:
        rad = rnd.uniform(4.6, 5.6)
        for dy in range(-7, 8):
            for dx in range(-7, 8):
                d = math.hypot(dx + 0.5, dy + 0.5)
                if d > rad:
                    continue
                if d > rad - 1:
                    put(cx + dx, cy + dy, lr[1])  # outline
                    continue
                lit = -((dx + 0.5) + (dy + 0.5)) / (rad * 1.3)
                k = 5 if lit > 0.8 else 4 if lit > 0.2 else 3 if lit > -0.35 else 2
                put(cx + dx, cy + dy, lr[k])
    if w["leaf_style"] == "fronds":
        for f in range(4):
            y0 = f * 8 + 3
            for x in range(S):
                put(x, y0 + math.sin(x * 0.3 + f) * 1.5, lr[5] if x % 3 else lr[4])
    for _ in range(8):
        x, y = rnd.randrange(S), rnd.randrange(S)
        if cv.get(x, y)[:3] == lr[0]:
            cv.set(x, y, (0, 0, 0), 0)
    if w["bloom"]:
        br = deep(w["bloom"], 5)
        count = {"blossom": 14, "dotted": 6, "glossy": 6}.get(w["leaf_style"], 5)
        for _ in range(count):
            x, y = rnd.randrange(S), rnd.randrange(S)
            for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                put(x + dx, y + dy, br[4] if dy < 0 or dx < 0 else br[2])
            put(x, y, rgb("#f6c84a"))
    return cv


def sapling(w, seed):
    lr = ramp(w["leaf"], 5, 0.34)
    br = ramp(w["bark"], 4, 0.3)
    rnd = random.Random(seed)
    cv = Canvas(S)
    # A little trunk with a curve
    for y in range(14, 32):
        x = 16 + int(math.sin(y * 0.25) * 1.2)
        cv.set(x, y, br[2])
        cv.set(x + 1, y, br[1])
    if w["leaf_style"] == "fronds":
        for a in range(-3, 4):
            ang = -math.pi / 2 + a * 0.45
            for t in range(0, 13):
                x, y = 16.5 + math.cos(ang) * t, 13 + math.sin(ang) * t + (t * t) * 0.05
                cv.set(x, y, lr[2])
                if t % 2 == 0:
                    cv.set(x, y + 1, lr[3])
    else:
        for (cx, cy, r) in ((16, 10, 7), (10, 14, 5), (22, 14, 5)):
            for _ in range(int(r * 4)):
                ang = rnd.uniform(0, 6.28)
                d = rnd.uniform(0, r - 1.5)
                leaf_blob(cv, cx + math.cos(ang) * d, cy + math.sin(ang) * d, rnd.uniform(0, 6.28), rnd.randint(2, 4), lr, rnd)
        if w["bloom"]:
            bl = ramp(w["bloom"], 4, 0.25)
            for _ in range(6):
                cv.set(rnd.randint(9, 23), rnd.randint(5, 17), bl[3])
    cv.outline(tuple(int(c * 0.55) for c in lr[0]))
    return cv


# ---------------------------------------------------------------- door and trapdoor

def door(w, top):
    p = ramp(w["planks"], 6, 0.26)
    cv = Canvas(S)
    for x in range(S):
        for y in range(S):
            k = 3
            if x in (0, S - 1) or (top and y == 0) or (not top and y == S - 1):
                k = 0
            elif x in (1, 2, S - 3, S - 2) or (top and y in (1, 2)) or (not top and y in (S - 3, S - 2)):
                k = 2 if x not in (1,) else 4
            elif x % 8 == 3:
                k = 2  # vertical board seams
            cv.set(x, y, p[k])
    if top:
        # A window with softly rounded corners, four panes and a little shine.
        glass = ramp("#bfe6f2", 4, 0.22)
        x1, y1, x2, y2 = 7, 6, 24, 25
        for y in range(y1 - 1, y2 + 2):
            for x in range(x1 - 1, x2 + 2):
                corner = (x in (x1 - 1, x2 + 1) and y in (y1 - 1, y2 + 1)) or (x in (x1, x2) and y in (y1, y2))
                if corner:
                    continue
                frame = x in (x1 - 1, x2 + 1) or y in (y1 - 1, y2 + 1)
                bar = x in (15, 16) or y == 15
                c = p[1] if frame else p[2] if bar else glass[2]
                cv.set(x, y, c)
        for i in range(4):
            cv.set(x1 + 2 + i, y1 + 5 - i, glass[3])
            cv.set(x1 + 11 + i, y1 + 14 - i, glass[3])
    else:
        # Two raised panels and a heart cut-out, plus a brass knob.
        for (x1, y1, x2, y2) in ((6, 3, 25, 12), (6, 16, 25, 27)):
            for y in range(y1, y2 + 1):
                for x in range(x1, x2 + 1):
                    k = 3
                    if y == y1 or x == x1:
                        k = 4
                    if y == y2 or x == x2:
                        k = 1
                    cv.set(x, y, p[k])
        heart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
        for j, row in enumerate(heart):
            for i, ch in enumerate(row):
                if ch == "1":
                    cv.set(13 + i, 5 + j, (0, 0, 0), 0)
        gold = ramp("#e9b949", 4, 0.3)
        cv.set(23, 14, gold[3]); cv.set(24, 14, gold[2]); cv.set(23, 15, gold[1]); cv.set(24, 15, gold[0])
    return cv


def trapdoor(w):
    p = ramp(w["planks"], 6, 0.26)
    cv = Canvas(S)
    for x in range(S):
        for y in range(S):
            edge = min(x, y, S - 1 - x, S - 1 - y)
            k = 3 if (x // 8) % 2 == 0 else 2
            if edge == 0:
                k = 0
            elif edge <= 2:
                k = 4 if edge == 1 else 2
            cv.set(x, y, p[k])
    # A cross brace and four heart-shaped holes
    for i in range(3, S - 3):
        for d in (0, 1):
            cv.set(i, i + d, p[1])
            cv.set(S - 1 - i, i + d, p[1])
    hole = ["0110110", "1111111", "0111110", "0011100", "0001000"]
    for (hx, hy) in ((5, 6), (20, 6), (5, 21), (20, 21)):
        for j, row in enumerate(hole):
            for i, ch in enumerate(row):
                if ch == "1":
                    cv.set(hx + i, hy + j, (0, 0, 0), 0)
    return cv


def build():
    n = 0
    for i, (name, w) in enumerate(WOODS.items()):
        seed = 100 + i * 17
        planks(w, seed).save(f"block/{name}_planks")
        bark(w, seed).save(f"block/{name}_log")
        log_top(w, seed).save(f"block/{name}_log_top")
        stripped(w, seed).save(f"block/stripped_{name}_log")
        log_top(w, seed, stripped=True).save(f"block/stripped_{name}_log_top")
        leaves(w, seed).save(f"block/{name}_leaves")
        sapling(w, seed).save(f"block/{name}_sapling")
        door(w, True).save(f"block/{name}_door_top")
        door(w, False).save(f"block/{name}_door_bottom")
        trapdoor(w).save(f"block/{name}_trapdoor")
        n += 10
    return n


if __name__ == "__main__":
    print(build(), "wood textures written")
