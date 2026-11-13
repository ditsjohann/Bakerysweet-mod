"""
Hand-drawn 32x32 crops: four growth stages for every seed, the tall corn and climbing peas, zucchini, and the fruit
skins the 3D berries on ripe crops are cut from.

Stage 0 is a sprout, stage 1 a young plant, stage 2 a full plant with buds or flowers, stage 3 ripe with its produce
clearly visible. Plants grow from the bottom of the texture. Run: python3 blockbench/gen_crops.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb

S = 32
GREEN = "#5aa64e"


def ramp(c, n=5, spread=0.3):
    return shades(c, n, spread, 0.02)


class Plant(Canvas):
    def __init__(self, seed):
        super().__init__(S)
        self.rnd = random.Random(seed)

    def stem(self, x, y0, height, color=GREEN, curve=0.0, width=1):
        """A stem from the ground (y0, usually 31) up `height` pixels, bending by `curve`. Returns its tip."""
        r = ramp(color, 4, 0.3)
        tip = (x, y0)
        for t in range(height):
            px = x + curve * (t / max(1, height)) ** 2 * height * 0.3
            py = y0 - t
            for w in range(width):
                self.set(px + w, py, r[2 if w == 0 else 1])
            tip = (px, py)
        return tip

    def leaf(self, x, y, angle, length, color=GREEN, width=0.34):
        """A pointed leaf from (x, y) toward `angle` (radians, 0 = right, -pi/2 = up), lit on its upper side."""
        r = ramp(color, 5, 0.32)
        ca, sa = math.cos(angle), math.sin(angle)
        for t in range(0, length + 1):
            half = math.sin((t + 0.5) / (length + 1) * math.pi) * length * width
            s = -half
            while s <= half:
                px, py = x + ca * t - sa * s, y + sa * t + ca * s
                upper = (s * ca) < 0 if abs(sa) < 0.5 else s < 0
                k = 3 if upper else 2
                if abs(s) < 0.5 and 0 < t < length:
                    k = 4 if t < length * 0.7 else 3
                if abs(s) > half - 0.6:
                    k = 1
                self.set(px, py, r[k])
                s += 0.5

    def berry(self, x, y, rad, color):
        self.disc(x, y, rad, ramp(color, 5, 0.34))

    def flower(self, x, y, color, petals=5, size=2.2, eye="#f6d65a"):
        r = ramp(color, 4, 0.25)
        for i in range(petals):
            a = i / petals * math.tau - math.pi / 2
            for d in (1, size):
                self.set(x + math.cos(a) * d, y + math.sin(a) * d, r[3 if d == size else 2])
        self.set(x, y, rgb(eye))

    def bush(self, cx, base, w, h, color=GREEN, leaves=24, leaf_len=4):
        """A rounded leafy bush standing on the ground."""
        for t in range(3):
            self.stem(cx - 2 + t * 2, base, int(h * 0.6), color="#6e8a3e", curve=(t - 1) * 2)
        for _ in range(leaves):
            a = self.rnd.uniform(0, math.tau)
            d = self.rnd.uniform(0, 1) ** 0.6
            x = cx + math.cos(a) * w * 0.5 * d
            y = base - h * 0.45 + math.sin(a) * h * 0.45 * d
            self.leaf(x, y, self.rnd.uniform(-math.pi, 0.2), self.rnd.randint(leaf_len - 1, leaf_len + 1), color)

    def sprout(self, x=16, color=GREEN, h=5):
        tx, ty = self.stem(x, 31, h, color)
        self.leaf(tx, ty, -math.pi * 0.85, 3, color)
        self.leaf(tx, ty, -math.pi * 0.15, 3, color)


# ---------------------------------------------------------------- the crops

def crop_berry_bush(name, berry_color, flower_color, seed, low=False, leaf="#4f9a4a"):
    out = []
    for stage in range(4):
        p = Plant(seed + stage)
        if stage == 0:
            p.sprout(13, leaf); p.sprout(20, leaf, 4)
        else:
            h = (8, 15, 18)[stage - 1] if low else (11, 20, 25)[stage - 1]
            p.bush(16, 31, (14, 22, 26)[stage - 1], h, leaf, leaves=(10, 26, 36)[stage - 1], leaf_len=(3, 4, 4)[stage - 1])
            if stage == 2:
                for _ in range(5):
                    p.flower(p.rnd.randint(8, 24), 31 - p.rnd.randint(4, h - 2), flower_color, 5, 1.6)
            if stage == 3:
                for _ in range(10):
                    x, y = p.rnd.randint(7, 25), 31 - p.rnd.randint(3, h - 3)
                    p.berry(x, y, 1.6 if low else 1.9, berry_color)
        out.append(p)
    return out


def crop_strawberry(seed):
    out = []
    red = ramp("#e8394d", 5, 0.32)
    for stage in range(4):
        p = Plant(seed + stage)
        if stage == 0:
            p.sprout(16, h=4)
        else:
            # Low rosette of three-part leaves on long stalks
            n = (3, 5, 6)[stage - 1]
            for i in range(n):
                a = -math.pi * (0.15 + 0.7 * i / max(1, n - 1))
                ln = (5, 9, 11)[stage - 1]
                x, y = 16 + math.cos(a) * ln, 31 + math.sin(a) * ln
                for t in range(ln):
                    p.set(16 + math.cos(a) * t, 31 + math.sin(a) * t, ramp(GREEN, 4)[1])
                for da in (-0.6, 0, 0.6):
                    p.leaf(x, y, a + da, (3, 4, 5)[stage - 1], "#55a84e", 0.45)
            if stage == 2:
                for x in (9, 23, 16):
                    p.flower(x, 22 + (x % 3), "#ffffff", 5, 1.8)
            if stage == 3:
                for (x, y) in ((8, 24), (24, 25), (13, 28), (20, 27)):
                    # heart-shaped berry with seeds and a green cap
                    for j in range(5):
                        half = (2.2, 2.4, 2.0, 1.4, 0.6)[j]
                        for i in range(-int(half), int(half) + 1):
                            k = 3 if i < 0 else 2
                            if j >= 3:
                                k = 1
                            p.set(x + i, y + j, red[k])
                    for (sx, sy) in ((-1, 1), (1, 2), (0, 3), (-1, 3)):
                        p.set(x + sx, y + sy, rgb("#ffe08a"))
                    p.set(x - 1, y - 1, ramp(GREEN)[3]); p.set(x, y - 1, ramp(GREEN)[2]); p.set(x + 1, y - 1, ramp(GREEN)[3])
                    p.set(x - 1, y, red[4])
        out.append(p)
    return out


def crop_flower(name, color, seed, kind):
    """Flowering crops: a leafy clump at the base, stems rising out of it, and the bloom (or spike, puff...) on top."""
    out = []
    for stage in range(4):
        p = Plant(seed + stage)
        if stage == 0:
            p.sprout(16, h=5)
            out.append(p)
            continue
        leaf = "#4f9a4a" if kind not in ("lavender", "candyfloss") else "#7aa070"
        # base clump
        base_h = (6, 9, 10)[stage - 1]
        p.bush(16, 31, (12, 18, 22)[stage - 1], base_h, leaf, leaves=(8, 16, 20)[stage - 1], leaf_len=4 if kind != "lavender" else 5)
        if kind == "vanilla":
            for y in range(4, 32):
                p.set(24, y, ramp("#a07a52", 4)[2]); p.set(25, y, ramp("#a07a52", 4)[1])
            h = (12, 22, 27)[stage - 1]
            for y in range(31, 31 - h, -1):
                x = 24 + math.sin(y * 0.5) * 2.5
                p.set(x, y, ramp("#5a9a3e", 4)[2])
                if y % 4 == 0:
                    p.leaf(x, y, -math.pi * (0.95 if y % 8 else 0.05), 5, "#4f9a4a", 0.4)
            if stage == 2:
                for y in (12, 19):
                    p.flower(21, y, "#fff4b0", 5, 2.0, eye="#e8c040")
            if stage == 3:
                pod = ramp("#5a7a2a", 4, 0.3)
                for (x, y) in ((20, 9), (27, 14), (21, 18)):
                    for j in range(8):
                        p.set(x, y + j, pod[2]); p.set(x + 1, y + j, pod[1] if j % 3 else pod[3])
            out.append(p)
            continue
        stems = {"lavender": (4, 7, 9), "rose": (2, 3, 4), "hibiscus": (1, 2, 2), "starbloom": (2, 3, 3), "candyfloss": (2, 3, 3)}[kind][stage - 1]
        top = (14, 23, 27)[stage - 1]
        for i in range(stems):
            spread = (i - (stems - 1) / 2)
            x = 16 + spread * (3 if kind == "lavender" else 5)
            h = top - abs(spread) * 2 - p.rnd.randint(0, 2)
            tx, ty = p.stem(x, 31, int(h), "#7a9a68" if kind == "lavender" else "#5a9a3e", curve=spread * 1.5)
            if kind == "lavender":
                if stage >= 2:
                    pr = ramp(color, 4, 0.32) if stage == 3 else ramp("#9ab88a", 4, 0.3)
                    for j in range(8 if stage == 3 else 4):
                        p.set(tx - 1, ty + j, pr[3 if j % 2 else 2])
                        p.set(tx, ty + j, pr[2 if j % 2 else 3])
                        p.set(tx + 1, ty + j, pr[1])
                    p.set(tx, ty - 1, pr[3])
            elif kind == "rose":
                p.leaf(tx, ty + 4, -math.pi * (0.15 if i % 2 else 0.85), 4, leaf)
                if stage == 2:
                    p.berry(tx, ty, 1.6, "#d8505a")
                    p.set(tx - 1, ty + 1, ramp(leaf)[2]); p.set(tx + 1, ty + 1, ramp(leaf)[2])
                if stage == 3:
                    rr = ramp(color, 5, 0.32)
                    p.disc(tx, ty, 3.0, rr)
                    for (dx, dy) in ((-1, -1), (1, 0), (0, 1), (-2, 1), (2, -1)):
                        p.set(tx + dx, ty + dy, rr[1])
                    p.set(tx, ty - 1, rr[4])
            elif kind == "hibiscus":
                if stage == 2:
                    p.berry(tx, ty, 2.0, "#e86a9a")
                if stage == 3:
                    pr = ramp(color, 5, 0.3)
                    for k in range(5):
                        a = k / 5 * math.tau - math.pi / 2
                        for d in range(1, 5):
                            for w in (-1, 0, 1):
                                if d + abs(w) > 5:
                                    continue
                                px = tx + math.cos(a) * d - math.sin(a) * w * 0.8
                                py = ty + math.sin(a) * d + math.cos(a) * w * 0.8
                                p.set(px, py, pr[4 if d == 4 else 3 if w == 0 else 2])
                    p.set(tx, ty, rgb("#b0204a"))
                    for j in range(1, 4):
                        p.set(tx + j * 0.5, ty - j, rgb("#ffe46a"))
            elif kind == "starbloom":
                if stage == 2:
                    p.berry(tx, ty, 1.5, "#d8e8ff")
                if stage == 3:
                    glow = ramp(color, 4, 0.25)
                    for d in range(1, 5):
                        for (dx, dy) in ((0, -1), (1, 0), (0, 1), (-1, 0)):
                            p.set(tx + dx * d, ty + dy * d, glow[3 if d < 3 else 2] if d < 4 else glow[1])
                    for (dx, dy) in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
                        p.set(tx + dx, ty + dy, glow[3])
                        p.set(tx + dx * 2, ty + dy * 2, glow[1])
                    p.set(tx, ty, (255, 255, 255))
            elif kind == "candyfloss":
                if stage >= 2:
                    rad = 2.5 if stage == 2 else 4.2
                    fl = ramp(color if i % 2 == 0 else "#b8d8ff", 4, 0.18)
                    for yy in range(int(ty - rad * 2), int(ty + 2)):
                        for xx in range(int(tx - rad - 1), int(tx + rad + 2)):
                            dx, dy = xx + 0.5 - tx, (yy + 0.5 - (ty - rad * 0.8)) / 0.9
                            if dx * dx + dy * dy <= rad * rad:
                                k = 3 if dx + dy < -1 else 2 if (xx * 3 + yy) % 4 else 1
                                p.set(xx, yy, fl[k])
        out.append(p)
    return out


def crop_leafy(name, seed, kind):
    """Leaf crops: mint, tea, rhubarb, sugar beet, sweet potato, peanut, pineapple."""
    out = []
    for stage in range(4):
        p = Plant(seed + stage)
        if stage == 0:
            p.sprout(16, "#5aa64e" if kind != "rhubarb" else "#6aa64e", h=4)
            out.append(p)
            continue
        g = stage
        if kind == "mint":
            h = (8, 15, 19)[g - 1]
            p.bush(16, 31, (12, 20, 24)[g - 1], h, "#4fb870", leaves=(10, 24, 32)[g - 1], leaf_len=(3, 4, 5)[g - 1])
            if g == 3:
                for x in (10, 16, 22):
                    for j in range(4):
                        p.set(x, 31 - h - 1 + j, ramp("#c8a8f0", 4)[3 - j % 2])
        elif kind == "tea":
            p.bush(16, 31, (12, 20, 26)[g - 1], (9, 16, 21)[g - 1], "#3f8a45", leaves=(12, 26, 36)[g - 1], leaf_len=4)
            if g == 3:
                for _ in range(6):
                    p.flower(p.rnd.randint(8, 24), p.rnd.randint(12, 26), "#ffffff", 5, 1.5)
            if g >= 2:
                for _ in range(8):
                    x, y = p.rnd.randint(8, 24), p.rnd.randint(10, 24)
                    p.set(x, y, ramp("#9ad86a", 4)[3])  # fresh light-green tips
        elif kind == "rhubarb":
            n = (2, 3, 4)[g - 1]
            for i in range(n):
                x = 16 + (i - (n - 1) / 2) * 4
                h = (8, 14, 18)[g - 1] - (i % 2) * 2
                tx, ty = p.stem(x, 31, h, "#d8405a" if g == 3 else "#9ab84a", curve=(i - n / 2) * 1.2, width=2)
                p.leaf(tx, ty, -math.pi / 2 + (i - n / 2) * 0.5, (5, 8, 10)[g - 1], "#4f9a3e", 0.55)
        elif kind == "sugar_beet":
            for i in range(g + 2):
                a = -math.pi * (0.2 + 0.6 * i / (g + 1))
                p.leaf(16, 29, a, (6, 10, 12)[g - 1], "#4f9a3e", 0.36)
                for t in range(4):
                    p.set(16 + math.cos(a) * t, 29 + math.sin(a) * t, ramp("#b8304a", 4)[2])
            if g == 3:
                p.disc(16, 30, 3, ramp("#b8304a", 5, 0.3))
        elif kind == "sweet_potato":
            # A low sprawling vine: wide, flat, with broad heart-ish leaves.
            p.bush(16, 31, (14, 26, 30)[g - 1], (5, 8, 10)[g - 1], "#4f9a4a", leaves=(8, 20, 28)[g - 1], leaf_len=4)
            for _ in range((3, 6, 8)[g - 1]):
                x = p.rnd.randint(4, 27)
                p.leaf(x, 31 - p.rnd.randint(1, 5), -math.pi * p.rnd.uniform(0.2, 0.8), 4, "#5aa654", 0.55)
            if g == 3:
                for x in (9, 22):
                    for i2 in range(-3, 4):
                        p.set(x + i2, 31, ramp("#b0506a", 4)[2 if i2 < 1 else 1])
                        if abs(i2) < 3:
                            p.set(x + i2, 30, ramp("#b0506a", 4)[3])
        elif kind == "peanut":
            p.bush(16, 31, (12, 20, 24)[g - 1], (7, 11, 13)[g - 1], "#5aa64e", leaves=(10, 22, 30)[g - 1], leaf_len=3)
            if g >= 2:
                for _ in range(5):
                    p.flower(p.rnd.randint(8, 24), p.rnd.randint(20, 27), "#ffd84a", 4, 1.3, eye="#e8a020")
            if g == 3:
                shell = ramp("#d8a868", 4, 0.3)
                for x in (8, 15, 22):
                    for i2 in range(4):
                        p.set(x + i2, 31, shell[2 if i2 in (0, 3) else 3]); p.set(x + i2, 30, shell[1] if i2 in (1, 2) else shell[2])
        elif kind == "pineapple":
            n = (5, 8, 11)[g - 1]
            for i in range(n):
                a = -math.pi * (0.08 + 0.84 * i / (n - 1))
                ln = (7, 11, 13)[g - 1] - abs(i - n / 2) * 0.6
                p.leaf(16, 30, a, int(ln), "#5a9a6a", 0.24)
            if g == 3:
                fr = ramp("#e8b03a", 5, 0.3)
                for y in range(17, 28):
                    w = 3 if 19 < y < 26 else 2
                    for x in range(16 - w, 17 + w):
                        k = 3 if (x + y) % 3 else 1
                        if x == 16 - w:
                            k = 1
                        p.set(x, y, fr[k])
                for a in (-2.0, -1.57, -1.1):
                    p.leaf(16.5, 17, a, 5, "#4f8a5a", 0.2)
        out.append(p)
    return out


def corn():
    out = {}
    gr = "#5aa64e"
    # young corn (single block)
    for stage, h in ((0, 9), (1, 18)):
        p = Plant(300 + stage)
        tx, ty = p.stem(16, 31, h, gr, width=2)
        for k in range(3, h, 4):
            side = 1 if (k // 4) % 2 else -1
            p.leaf(16, 31 - k, -math.pi / 2 + side * 1.0, 7 if stage else 5, gr, 0.22)
        out[f"corn_stage{stage}"] = p
    for part in ("bottom", "middle", "middle_ripe", "top"):
        p = Plant(310 + len(out))
        p.stem(15, 31, 32, gr, width=2)
        for k in range(2, 32, 6):
            side = 1 if (k // 6) % 2 else -1
            p.leaf(16, 31 - k, -math.pi / 2 + side * 1.05, 9, gr, 0.2)
        if part == "middle_ripe":
            husk = ramp("#9ac85a", 4, 0.3)
            kern = ramp("#f2c84a", 4, 0.3)
            for y in range(10, 24):
                for x in (18, 19, 20):
                    p.set(x, y, kern[3 if (x + y) % 2 else 2])
                p.set(21, y, husk[2]); p.set(17, y, husk[1])
            for j in range(4):
                p.set(19, 9 - j, ramp("#c8a050", 4)[2])
        if part == "top":
            # tassel
            ts = ramp("#d8c070", 4, 0.3)
            for i in range(-4, 5):
                for t in range(6):
                    p.set(16 + i * 0.8 + (t * i) * 0.25, 4 + t + abs(i) // 2, ts[2 if t % 2 else 3])
            for y in range(0, 12):
                p.set(15, y, (0, 0, 0), 0); p.set(16, y, (0, 0, 0), 0)
            p.stem(15, 31, 22, gr, width=2)
        if part == "bottom":
            for x in (12, 13, 18, 19):
                p.set(x, 31, ramp("#8a6a3e", 4)[1])
        out[f"corn_{part}"] = p
    return out


def peas():
    out = {}
    for stage, h in ((0, 6), (1, 14)):
        p = Plant(400 + stage)
        p.sprout(16, h=h)
        if stage:
            for k in (5, 9, 12):
                p.leaf(16, 31 - k, -math.pi * (0.2 if k % 2 else 0.8), 4, "#6ab85a", 0.45)
        out[f"pea_stage{stage}"] = p
    trellis = ramp("#c8a070", 4, 0.3)
    for part in ("bottom", "bottom_ripe", "top", "top_ripe"):
        p = Plant(410 + len(out))
        for x in (6, 25):
            for y in range(32):
                p.set(x, y, trellis[2]); p.set(x + 1, y, trellis[1])
        for y in (4, 20):
            for x in range(6, 27):
                p.set(x, y, trellis[2])
        # the vine winding up
        for y in range(32):
            x = 16 + math.sin(y * 0.4) * 6
            p.set(x, y, ramp("#5aa64e", 4)[2])
            if y % 5 == 0:
                p.leaf(x, y, -math.pi * (0.1 if y % 10 else 0.9), 4, "#6ab85a", 0.45)
            if y % 7 == 3:
                for t in range(3):
                    p.set(x + (1 if y % 2 else -1) * (t + 1), y - t, ramp("#8ad06a", 4)[3])  # tendrils
        if "ripe" in part:
            pod = ramp("#7ac85a", 5, 0.3)
            for (x, y) in ((10, 8), (21, 14), (12, 24)):
                for j in range(6):
                    p.set(x, y + j, pod[3]); p.set(x + 1, y + j, pod[2])
                    if j % 2:
                        p.set(x, y + j, pod[4])
        else:
            for (x, y) in ((11, 10), (20, 22)):
                p.flower(x, y, "#ffffff", 4, 1.3, eye="#f0b0d0")
        out[f"pea_{part}"] = p
    return out


# ---------------------------------------------------------------- fruit skins (the 3D berries are cut from these)

def skin(color, seed, kind="smooth"):
    r = ramp(color, 5, 0.26)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=r[2])
    for y in range(S):
        for x in range(S):
            k = 2 if (x * 7 + y * 13) % 5 else 3
            if kind == "fuzz" and (x + y) % 3 == 0:
                k = 3
            cv.set(x, y, r[k])
    if kind == "seeds":
        for y in range(1, S, 4):
            for x in range((y // 4) % 2 * 2 + 1, S, 4):
                cv.set(x, y, rgb("#ffe08a")); cv.set(x, y + 1, r[1])
    elif kind == "dimples":
        for _ in range(70):
            cv.set(rnd.randrange(S), rnd.randrange(S), r[1])
    elif kind == "drupelets":
        for y in range(0, S, 3):
            for x in range((y // 3) % 2, S, 3):
                cv.set(x, y, r[4]); cv.set(x + 1, y + 1, r[1])
    elif kind == "bloom":
        for _ in range(60):
            cv.set(rnd.randrange(S), rnd.randrange(S), r[4])
    elif kind == "scales":
        for y in range(0, S, 4):
            for x in range((y // 4) % 2 * 2, S, 4):
                cv.set(x, y, r[0]); cv.set(x + 1, y + 1, r[4])
    elif kind == "petals":
        for y in range(S):
            for x in range(S):
                if (x + y * 2) % 6 == 0:
                    cv.set(x, y, r[1])
    elif kind == "sparkle":
        for _ in range(30):
            cv.set(rnd.randrange(S), rnd.randrange(S), (255, 255, 255))
    elif kind == "shell":
        for y in range(0, S, 3):
            for x in range(S):
                if (x + y) % 4 == 0:
                    cv.set(x, y, r[1])
    return cv


FRUIT = {
    "strawberry": ("#e8394d", "seeds"), "blueberry": ("#5a6ad8", "bloom"), "raspberry": ("#e0406a", "drupelets"),
    "cranberry": ("#c0203a", "smooth"), "cherry": ("#c8203a", "smooth"), "orange": ("#f29a2a", "dimples"),
    "peach": ("#f6a07a", "fuzz"), "mango": ("#f2b03a", "smooth"), "plum": ("#7a3a8a", "bloom"), "pineapple": ("#e8b03a", "scales"),
    "rose": ("#e0405a", "petals"), "hibiscus": ("#f0508a", "petals"), "star": ("#fff0a0", "sparkle"), "fluff": ("#ffc0e0", "fuzz"),
    "vanilla": ("#5a8a32", "smooth"), "peanut": ("#d8a868", "shell"), "rhubarb": ("#d8405a", "smooth"), "sugar_beet": ("#b8304a", "smooth"),
    "sweet_potato": ("#b0506a", "smooth"), "stem": ("#6a9a3e", "smooth"), "young": ("#8ac85a", "smooth"),
}


def build():
    n = 0
    seed = 1000
    def save_stages(name, stages):
        nonlocal n
        for i, p in enumerate(stages):
            p.save(f"block/{name}_crop_stage{i}")
            n += 1
    save_stages("strawberry", crop_strawberry(seed))
    save_stages("blueberry", crop_berry_bush("blueberry", "#5a6ad8", "#ffffff", seed + 10))
    save_stages("raspberry", crop_berry_bush("raspberry", "#e0406a", "#ffffff", seed + 20))
    save_stages("cranberry", crop_berry_bush("cranberry", "#c0203a", "#ffd0e0", seed + 30, low=True, leaf="#3f7a3a"))
    save_stages("lavender", crop_flower("lavender", "#a070d8", seed + 40, "lavender"))
    save_stages("rose", crop_flower("rose", "#e0405a", seed + 50, "rose"))
    save_stages("hibiscus", crop_flower("hibiscus", "#f0508a", seed + 60, "hibiscus"))
    save_stages("starbloom", crop_flower("starbloom", "#fff0a0", seed + 70, "starbloom"))
    save_stages("candyfloss", crop_flower("candyfloss", "#ffb0d8", seed + 80, "candyfloss"))
    save_stages("vanilla", crop_flower("vanilla", "#5a8a32", seed + 90, "vanilla"))
    for kind in ("mint", "tea", "rhubarb", "sugar_beet", "sweet_potato", "peanut", "pineapple"):
        save_stages(kind, crop_leafy(kind, seed + 100 + n, kind))
    for name, p in {**corn(), **peas()}.items():
        p.save(f"block/{name}")
        n += 1
    for i, (name, (color, kind)) in enumerate(FRUIT.items()):
        skin(color, 2000 + i, kind).save(f"block/fruit_{name}")
        n += 1
    return n


if __name__ == "__main__":
    print(build(), "crop textures written")
