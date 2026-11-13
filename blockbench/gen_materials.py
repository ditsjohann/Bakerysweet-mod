"""
32x32 surface materials for furniture and kitchenware: painted wood, glazed ceramic, metals, fabrics.

These go on model faces of every size and angle, so they carry no baked-in light (no shine stripes, no corner
glints) - the game shades the faces. Each is a calm base tone with fine, even detail in the same hue.
Run: python3 blockbench/gen_materials.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb
import gen_woods

S = 32


def r(c, n=5, spread=0.2):
    return shades(c, n, spread, 0.01)


def painted_wood(color, seed, boards=4):
    w = {"planks": color}
    cv = gen_woods.planks(w, seed)
    if boards != 4:
        pass
    return cv


def ceramic(color, seed, speckles=True, band=None):
    """Glazed pottery: soft horizontal throwing rings, a few kiln speckles, an optional painted band."""
    p = r(color, 5, 0.16)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[2])
    for y in range(S):
        ring = 3 if (y % 7 == 0) else 2
        for x in range(S):
            k = ring
            if (x * 3 + y * 5) % 23 == 0:
                k = 3
            cv.set(x, y, p[k])
    if speckles:
        for _ in range(9):
            cv.set(rnd.randrange(S), rnd.randrange(S), p[0])
    if band:
        y0, y1, bc = band
        b = r(bc, 4, 0.2)
        for y in range(y0, y1):
            for x in range(S):
                cv.set(x, y, b[2] if (x + y) % 6 else b[3])
    return cv


def metal(color, seed):
    """Brushed metal: fine horizontal streaks, evenly lit."""
    p = r(color, 5, 0.18)
    rnd = random.Random(seed)
    cv = Canvas(S)
    for y in range(S):
        base = rnd.choice((2, 2, 2, 3, 1))
        for x in range(S):
            k = base
            if rnd.random() < 0.08:
                k = min(4, base + 1)
            cv.set(x, y, p[k])
    return cv


def hammered(color, seed):
    """Hammered copper: little round dimples, each lit on its upper edge."""
    p = r(color, 5, 0.22)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[2])
    for _ in range(22):
        cx, cy = rnd.uniform(0, S), rnd.uniform(0, S)
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                d = math.hypot(dx, dy)
                if d <= 2.6:
                    x, y = int(cx + dx) % S, int(cy + dy) % S
                    cv.set(x, y, p[3] if dy < -1 else p[1] if dy > 1 else p[2])
    return cv


def velvet(color, seed):
    p = r(color, 5, 0.14)
    rnd = random.Random(seed)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            k = 2
            v = (math.sin(x * 0.5 + y * 0.2) + math.sin(y * 0.37 - x * 0.11))
            if v > 1.2:
                k = 3
            elif v < -1.3:
                k = 1
            if rnd.random() < 0.05:
                k = 3
            cv.set(x, y, p[k])
    return cv


def tufted(color, seed):
    """A tufted cushion: soft square puffs with a button dimple at each corner."""
    p = r(color, 5, 0.2)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            u, v = x % 16, y % 16
            puff = math.sin((u + 0.5) / 16 * math.pi) * math.sin((v + 0.5) / 16 * math.pi)
            k = 1 + int(round(puff * 2.6))
            if u == 0 or v == 0:
                k = 1
            cv.set(x, y, p[min(4, k)])
    for (x, y) in ((0, 0), (16, 0), (0, 16), (16, 16)):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            cv.set((x + dx - 1) % S, (y + dy - 1) % S, p[0])
    return cv


def woven(color, seed, check=None):
    """Woven cloth: tiny over-under threads; optionally a gingham check."""
    p = r(color, 5, 0.16)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            k = 3 if (x + y) % 2 == 0 else 2
            if check:
                bx, by = (x // 4) % 2 == 0, (y // 4) % 2 == 0
                c = r(check, 5, 0.16)
                if bx and by:
                    cv.set(x, y, c[1 if k == 2 else 2])
                    continue
                if bx or by:
                    cv.set(x, y, c[3 if k == 2 else 4])
                    continue
            cv.set(x, y, p[k])
    return cv


def sack(seed):
    cv = woven("#d8c09a", seed)
    p = r("#d8c09a", 5, 0.2)
    for x in range(S):
        if x % 8 == 3:
            for y in range(S):
                cv.set(x, y, p[1])
    return cv


def wicker(color, seed):
    p = r(color, 5, 0.26)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            cell = ((x // 6) + (y // 6)) % 2
            u, v = x % 6, y % 6
            t = v if cell == 0 else u
            k = [1, 3, 4, 4, 3, 1][t]
            cv.set(x, y, p[k])
    return cv


def build():
    jobs = {
        # painted and natural woods (same soft planks as the trees)
        "wood_cream": painted_wood("#f2e2c6", 11), "wood_pink": painted_wood("#f2bccc", 12), "wood_mint": painted_wood("#b2dcc8", 13),
        "wood_lilac": painted_wood("#cdb9e4", 14), "table_wood": painted_wood("#c9976a", 15), "counter_wood": painted_wood("#e6c7a0", 16),
        "case_wood": painted_wood("#f0c4d2", 17), "churn_wood": painted_wood("#b98a62", 18), "crate": painted_wood("#c99a62", 19),
        # glazes
        "ceramic_pink": ceramic("#f2a9c2", 21), "ceramic_white": ceramic("#f6f0ec", 22), "mug": ceramic("#fbe2ea", 23),
        "mug_inner": ceramic("#fff6f0", 24, speckles=False), "mixing_bowl": ceramic("#f8cfdc", 25, band=(22, 25, "#f28aac")),
        "mixing_bowl_inner": ceramic("#fff4ee", 26, speckles=False), "flower_pot_peach": ceramic("#f2b08e", 27, band=(3, 6, "#e8906e")),
        "juicer_body": ceramic("#c9ecdc", 28), "fountain/ceramic": ceramic("#fbf2ee", 29), "plate": ceramic("#fffaf6", 30, speckles=False),
        "cupcake_lamp_wrapper": ceramic("#f6a4c0", 31),
        # metals
        "metal": metal("#c8ccd6", 41), "metal_white": metal("#ece8ee", 42), "metal_pink": metal("#f0aac2", 43), "metal_gold": metal("#e6b84e", 44),
        "brass": metal("#d6a84c", 45), "fountain/gold": metal("#eec258", 46), "pie_tin": metal("#c2c6ce", 47), "copper_pot": hammered("#c8774c", 48),
        # fabrics
        "velvet_pink": velvet("#f2a0bc", 51), "velvet_mint": velvet("#a0d8c4", 52), "cushion": tufted("#ffd0de", 53),
        "plush_pink": velvet("#f8bcd0", 54), "plush_white": velvet("#f6f2ee", 55), "plush_brown": velvet("#bc8c66", 56),
        "linen": woven("#f4ece0", 57), "tablecloth": woven("#ffffff", 58, check="#f6aac4"), "sack": sack(59),
        "wicker": wicker("#d4a874", 60), "banneton": wicker("#c69660", 61), "welcome_mat": wicker("#c8a070", 62),
    }
    for name, cv in jobs.items():
        cv.save("block/" + name)
    return len(jobs)


if __name__ == "__main__":
    print(build(), "materials written")
