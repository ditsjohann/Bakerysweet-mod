"""
Crops at 16x16, built the same way as the saplings: a few hand-placed leaves in two layers (darker leaves at the back,
lit leaves in front, each casting a 1px shadow onto what is behind it), clean stems, and the produce drawn with a lit
side and a shaded side. No random scatter of tiny leaves - every plant is composed.

Four stages per crop: 0 sprout, 1 young plant, 2 grown (buds / flowers), 3 ripe.
Run after gen_crops.py (it replaces the stage textures): python3 blockbench/gen_crops16.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import sap_leaf, blade, stem, fruit, tiny_flower, put, canvas, hx, TEX

GREEN = ("#1e4a22", "#2e6a2c", "#4a8c3a", "#6aaa4c", "#90c86a")
DEEP = ("#173c20", "#245a2a", "#3a7a36", "#58984a", "#7cb664")
MINT = ("#1e5a3a", "#2e7a4e", "#4a9c66", "#6abc84", "#94d8a6")
SAGE = ("#3a5040", "#52705a", "#6e8e74", "#8eac90", "#b2c8b0")
STALK = ("#2a4a1c", "#3e6a28", "#5a8a36")


def pal(p):
    return [hx(c) for c in p]


def ground_stem(img, x, top, col=STALK):
    """A short upright stem from the soil to `top`, lit on its left."""
    c = pal(col)
    for y in range(top, 16):
        put(img, x, y, c[2] if y < 14 else c[1])


def shaded_blob(img, cx, cy, r, base, highlight=True):
    """A round fruit or bloom: lit upper left, shaded lower right, a bright glint."""
    b = hx(base)
    lite = tuple(min(255, v + 45) for v in b)
    mid = b
    dark = tuple(int(v * 0.7) for v in b)
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            side = -(dx + dy)
            put(img, x, y, lite if side > r * 0.6 else dark if side < -r * 0.5 else mid)
    if highlight and r >= 1.4:
        put(img, int(cx - r * 0.4), int(cy - r * 0.4), tuple(min(255, v + 90) for v in b))


# ------------------------------------------------------------------ plant shapes

def bush(stage, leaves, produce=None, flower=None, low=False):
    """A rounded leafy bush. Stage 0 a sprout, 1 a small mound, 2 full with flowers, 3 full with produce."""
    img = canvas()
    lp = pal(leaves)
    if stage == 0:
        ground_stem(img, 7, 12)
        sap_leaf(img, 5.4, 11.4, 3.4, 2.0, 2.6, lp)
        sap_leaf(img, 9.6, 11.0, 3.4, 2.0, 0.5, lp)
        return img
    scale = (0.62, 0.85, 1.0)[stage - 1] * (0.82 if low else 1.0)
    base_y = 15

    def at(dx, dy):
        return 7.5 + dx * scale, base_y - dy * scale

    stem(img, [(7, 15), (7, int(base_y - 6 * scale))], pal(STALK)[2], pal(STALK)[1])
    stem(img, [(7, 14), (5, int(base_y - 4 * scale))], pal(STALK)[2], pal(STALK)[1])
    stem(img, [(8, 14), (10, int(base_y - 4 * scale))], pal(STALK)[2], pal(STALK)[1])
    back = ((-4.5, 7.5, 2.5), (4.5, 7.5, 0.6), (0, 10, 1.57), (-6.5, 4, 2.9), (6.5, 4, 0.2), (-2.2, 11, 2.0), (2.4, 11, 1.1))
    front = ((-3.6, 5.2, 2.6), (3.6, 5.0, 0.5), (-1.6, 8.4, 2.1), (1.8, 8.2, 1.0), (0, 3.6, 1.57), (-5.4, 2.4, 3.0), (5.4, 2.2, 0.15))
    n_back, n_front = ((4, 4), (6, 6), (7, 7))[stage - 1]
    ln, wd = 4.2 * max(0.8, scale), 2.6 * max(0.8, scale)
    for (dx, dy, a) in back[:n_back]:
        sap_leaf(img, *at(dx, dy), ln, wd, a, lp, back=True)
    for (dx, dy, a) in front[:n_front]:
        sap_leaf(img, *at(dx, dy), ln, wd, a, lp)
    if stage == 2 and flower:
        for (dx, dy) in ((-3.5, 8), (3.6, 7.2), (0.5, 10.4), (-5.6, 4.6), (5.4, 4.2)):
            x, y = at(dx, dy)
            tiny_flower(img, int(x), int(y), hx(flower[0]), hx(flower[1]))
    if stage == 3 and produce:
        color, size, spots = produce
        for (dx, dy) in spots:
            x, y = at(dx, dy)
            fruit(img, int(x), int(y), color, size)
    return img


def rosette(stage, leaves, kind):
    """A low rosette of leaves spreading from the ground: strawberries, beets, sweet potatoes, peanuts."""
    img = canvas()
    lp = pal(leaves)
    if stage == 0:
        ground_stem(img, 7, 13)
        sap_leaf(img, 5.6, 12.6, 3.0, 1.9, 2.7, lp)
        sap_leaf(img, 9.4, 12.4, 3.0, 1.9, 0.45, lp)
        return img
    spread = (0.7, 0.9, 1.0)[stage - 1]
    stalk = pal(STALK)
    tips = ((-5.5, 5, 2.7), (5.5, 5, 0.45), (-2.5, 8, 2.1), (2.5, 8, 1.05), (0, 9, 1.57), (-6.5, 2, 3.0), (6.5, 2, 0.15))
    count = (4, 6, 7)[stage - 1]
    for i, (dx, dy, a) in enumerate(tips[:count]):
        x, y = 7.5 + dx * spread, 15 - dy * spread
        stem(img, [(7 + (1 if dx > 0 else 0), 15), (int(x), int(y + 1))], stalk[2], stalk[1])
    for i, (dx, dy, a) in enumerate(tips[:count]):
        x, y = 7.5 + dx * spread, 15 - dy * spread
        sap_leaf(img, x, y - 0.6, 4.0, 2.8, a, lp, back=True)
    # A heart of fresh leaves in front fills the middle, so it reads as a full clump, not a ring.
    centre = ((-2.4, 3.6, 2.4), (2.4, 3.4, 0.7), (0, 5.6, 1.57), (-4.2, 1.6, 2.9), (4.2, 1.6, 0.25))
    for (dx, dy, a) in centre[:(3, 4, 5)[stage - 1]]:
        sap_leaf(img, 7.5 + dx * spread, 15 - dy * spread, 4.2, 3.0, a, lp)
    if kind == "strawberry":
        if stage == 2:
            for (x, y) in ((4, 10), (11, 9), (8, 6)):
                tiny_flower(img, x, y, hx("#ffffff"), hx("#f6d65a"))
        if stage == 3:
            red = ("#a01e30", "#d22c40", "#ee4a5c", "#ff8090")
            for (x, y) in ((2, 12), (11, 12), (6, 13)):
                for (dx, dy, k) in ((0, 0, 2), (1, 0, 1), (2, 0, 1), (0, 1, 2), (1, 1, 1), (2, 1, 0), (1, 2, 0)):
                    put(img, x + dx, y + dy, hx(red[k]))
                put(img, x, y, hx(red[3])); put(img, x + 1, y + 1, hx("#ffe48a"))
                put(img, x + 1, y - 1, hx("#3a7a2e"))
    elif kind == "sugar_beet":
        if stage >= 2:
            for (x, y, c) in ((6, 13, "#a8285a"), (7, 13, "#c43a6c"), (8, 13, "#8a1e48"), (6, 14, "#8a1e48"), (7, 14, "#a8285a"), (8, 14, "#6e1438")):
                put(img, x, y, hx(c))
            if stage == 3:
                put(img, 5, 14, hx("#a8285a")); put(img, 9, 14, hx("#6e1438")); put(img, 7, 12, hx("#d65a86"))
    elif kind == "sweet_potato":
        if stage >= 2:
            for (x, y, c) in ((4, 14, "#c86a3a"), (5, 14, "#e08a4a"), (6, 14, "#b05a30"), (10, 14, "#e08a4a"), (11, 14, "#b05a30")):
                put(img, x, y, hx(c))
        if stage == 2:
            tiny_flower(img, 11, 8, hx("#e8b6f0"), hx("#a04cb4"))
    elif kind == "peanut":
        if stage >= 2:
            for (x, y) in ((4, 9), (11, 8), (7, 7)):
                put(img, x, y, hx("#f6d24a")); put(img, x + 1, y, hx("#e8b030"))
        if stage == 3:
            for (x, y) in ((5, 14), (9, 14)):
                put(img, x, y, hx("#d8a868")); put(img, x + 1, y, hx("#b8864a"))
    return img


def flower_plant(stage, leaves, bloom, kind):
    """A leafy base with tall stems carrying buds, then open blooms."""
    img = canvas()
    lp = pal(leaves)
    stalk = pal(STALK)
    if stage == 0:
        ground_stem(img, 7, 12)
        sap_leaf(img, 5.6, 11.6, 3.2, 1.9, 2.6, lp)
        sap_leaf(img, 9.4, 11.2, 3.2, 1.9, 0.5, lp)
        return img
    heights = {1: (8, 7), 2: (4, 5, 6), 3: (3, 4, 5)}[stage]
    xs = (4, 11, 7)[:len(heights)]
    tops = []
    for x, h in zip(xs, heights):
        stem(img, [(7 if x < 7 else 8, 15), (x, h + 3), (x, h)], stalk[2], stalk[1])
        tops.append((x, h))
    for (x, y, a, b) in ((4.5, 12.6, 2.7, True), (10.6, 12.4, 0.45, True), (5.8, 11.0, 2.3, False), (9.4, 10.6, 0.8, False),
                         (3.8, 9.0, 2.9, False), (11.4, 9.4, 0.2, False)):
        sap_leaf(img, x, y, 3.8, 2.4, a, lp, back=b)
    for (x, y) in tops:
        if stage == 1:
            put(img, x, y, hx(bloom[1])); put(img, x, y - 1, lp[3])
        elif stage == 2:
            put(img, x, y, hx(bloom[1])); put(img, x, y - 1, hx(bloom[0])); put(img, x - 1, y, lp[3]); put(img, x + 1, y, lp[2])
        else:
            if kind == "rose":
                b = [hx(c) for c in ("#7a1428", "#b81e3a", "#e0405a", "#ff7a8c")]
                for (dx, dy, k) in ((-1, -1, 3), (0, -1, 2), (1, -1, 2), (-1, 0, 2), (0, 0, 1), (1, 0, 1), (-1, 1, 1), (0, 1, 0), (1, 1, 0),
                                    (0, -2, 3), (2, 0, 1)):
                    put(img, x + dx, y + dy, b[k])
            elif kind == "hibiscus":
                b = [hx(c) for c in ("#a82060", "#e04a88", "#f878aa", "#ffb0cc")]
                for (dx, dy, k) in ((0, -2, 3), (-2, 0, 3), (2, 0, 2), (-1, 2, 1), (1, 2, 1), (-1, -1, 2), (1, -1, 2), (-1, 0, 2),
                                    (1, 0, 1), (0, 1, 1), (-1, 1, 1), (1, 1, 0)):
                    put(img, x + dx, y + dy, b[k])
                put(img, x, y, hx("#ffe060")); put(img, x, y - 1, hx("#7a1440"))
            elif kind == "starbloom":
                b = [hx(c) for c in ("#c8a020", "#f0c840", "#fff080", "#ffffff")]
                for (dx, dy, k) in ((0, -2, 3), (-2, 0, 2), (2, 0, 2), (-1, 2, 1), (1, 2, 1), (0, -1, 2), (-1, 0, 2), (1, 0, 1), (0, 1, 1)):
                    put(img, x + dx, y + dy, b[k])
                put(img, x, y, hx("#ffffff"))
            elif kind == "candyfloss":
                shaded_blob(img, x + 0.5, y + 0.2, 2.2, "#ffb0d8")
                put(img, x - 1, y - 1, hx("#fff0f8"))
    return img


def lavender(stage):
    """Lavender: a clump of narrow grey-green blades and violet spikes of buds rising out of it."""
    img = canvas()
    lp = pal(SAGE)
    blades = ((7, 15, 3, 8), (8, 15, 12, 8), (7, 15, 5, 6), (8, 15, 10, 6), (7, 15, 1, 10), (8, 15, 14, 10), (7, 15, 7, 7))
    count = (3, 5, 7, 7)[stage]
    for i, (x0, y0, x1, y1) in enumerate(blades[:count]):
        sy = y1 + (6 - stage * 2 if stage < 2 else 0)
        blade(img, x0, y0, x1, sy, lp, back=i % 2 == 0)
    if stage >= 2:
        violet = [hx(c) for c in ("#5a3a8a", "#7a52b0", "#9a70d0", "#c4a2f0")]
        for (x, top) in ((5, 2), (9, 1), (12, 4), (2, 5)):
            stem(img, [(x, 9), (x, top + 4)], pal(SAGE)[2], pal(SAGE)[1])
            for y in range(top, top + 4):
                k = 3 if (y - top) % 2 == 0 else 2
                put(img, x, y, violet[k if stage == 3 else 1]); put(img, x + 1, y, violet[k - 1 if stage == 3 else 0])
    return img


def vanilla(stage):
    """Vanilla: a climbing orchid vine on a little stake, glossy leaves left and right, long green pods when ripe."""
    img = canvas()
    lp = pal(DEEP)
    wood = [hx(c) for c in ("#6a4a2a", "#8e6a40", "#b08a5a")]
    for y in range(2, 16):
        put(img, 7, y, wood[2]); put(img, 8, y, wood[1])
    height = (12, 8, 4, 3)[stage]
    for y in range(height, 16):
        put(img, 6 if y % 4 < 2 else 9, y, pal(STALK)[2])
    sides = ((5.0, 13.0, 2.8), (10.4, 11.6, 0.3), (4.8, 9.6, 2.9), (10.6, 8.0, 0.25), (5.0, 6.2, 2.8), (10.4, 4.6, 0.3))
    for i, (x, y, a) in enumerate(sides):
        if y < height:
            continue
        sap_leaf(img, x, y, 4.2, 2.4, a, lp, back=i % 3 == 2)
    if stage == 2:
        tiny_flower(img, 10, 6, hx("#fff4c8"), hx("#e8c040"))
        tiny_flower(img, 4, 9, hx("#fff4c8"), hx("#e8c040"))
    if stage == 3:
        pod = [hx(c) for c in ("#3a5a1e", "#5a7e2a", "#7aa040")]
        for (x, y) in ((4, 9), (11, 7), (5, 12)):
            for i in range(4):
                put(img, x, y + i, pod[2 if i == 0 else 1]); put(img, x + 1, y + i, pod[0])
    return img


def rhubarb(stage):
    """Rhubarb: big crinkled leaves held up on thick red stalks."""
    img = canvas()
    lp = pal(GREEN)
    red = [hx(c) for c in ("#7a1830", "#b02848", "#d84868")]
    if stage == 0:
        ground_stem(img, 7, 12, ("#7a1830", "#b02848", "#d84868"))
        sap_leaf(img, 7.5, 10.5, 4, 3, 1.57, lp)
        return img
    stalks = (((4, 8), (11, 7)), ((3, 6), (12, 5), (7, 4)), ((2, 5), (13, 4), (7, 3), (5, 7)))[min(stage, 3) - 1]
    for (x, y) in stalks:
        pts = [(7 if x < 7 else 8, 15), (x, y + 2)]
        stem(img, pts, red[2], red[1])
        if stage >= 2:
            stem(img, [(pts[0][0] + (1 if x < 7 else -1), 15), (x + (1 if x < 7 else -1), y + 3)], red[1], red[0])
    for i, (x, y) in enumerate(stalks):
        sap_leaf(img, x, y, 5.6 if stage >= 2 else 4.4, 4.0 if stage >= 2 else 3.0, 1.57 + (x - 7.5) * 0.12, lp, back=i % 2 == 0)
    return img


def pineapple(stage):
    """Pineapple: a fountain of spiky blades; the fruit with its own leafy crown sits in the middle when ripe."""
    img = canvas()
    lp = pal(("#24462a", "#36633a", "#4e824a", "#6e9e5c", "#94bc7a"))
    blades = ((7, 14, 1, 7), (8, 14, 14, 7), (7, 14, 3, 4), (8, 14, 12, 4), (7, 14, 0, 11), (8, 14, 15, 11), (7, 14, 5, 3), (8, 14, 10, 3))
    count = (3, 5, 8, 8)[stage]
    for i, (x0, y0, x1, y1) in enumerate(blades[:count]):
        blade(img, x0, y0, x1, y1 + (3 if stage < 2 else 0), lp, back=i >= 4)
    if stage == 3:
        gold = [hx(c) for c in ("#a8641a", "#d8902a", "#f2b83e", "#ffd86a")]
        for y in range(6, 12):
            for x in range(6, 10):
                if (x == 6 or x == 9) and y in (6, 11):
                    continue
                k = 3 if (x + y) % 3 == 0 else 2 if x < 8 else 1
                put(img, x, y, gold[k])
        for (x, y) in ((7, 3), (8, 4), (6, 4), (9, 3), (7, 5), (8, 5)):
            put(img, x, y, lp[3 if x < 8 else 2])
    elif stage == 2:
        for (x, y) in ((7, 9), (8, 9), (7, 10), (8, 10)):
            put(img, x, y, hx("#e86a8a") if (x + y) % 2 else hx("#c84a70"))
    return img


def build():
    out = {
        "blueberry": [bush(s, GREEN, ("#5a6ad8", 2, ((-3.5, 7.5), (3.2, 6.8), (0, 9.6), (-5.4, 3.6), (5.2, 3.4), (-1.4, 4.4), (2.0, 4.0))),
                           ("#ffffff", "#f0d0e0")) for s in range(4)],
        "raspberry": [bush(s, GREEN, ("#e0406a", 2, ((-3.5, 7.5), (3.2, 6.8), (0, 9.6), (-5.4, 3.6), (5.2, 3.4), (-1.4, 4.4), (2.0, 4.0))),
                           ("#ffffff", "#f6d65a")) for s in range(4)],
        "cranberry": [bush(s, DEEP, ("#c0203a", 2, ((-3.5, 6.5), (3.2, 6.0), (0, 8.0), (-5.0, 3.0), (5.0, 3.0), (0.5, 4.0))),
                           ("#ffd0e0", "#c0406a"), low=True) for s in range(4)],
        "tea": [bush(s, DEEP, ("#a6d870", 1, ((-3.5, 8.5), (3.2, 8.0), (0, 11), (-6, 5), (6, 5))), ("#ffffff", "#f6d65a")) for s in range(4)],
        "mint": [bush(s, MINT, None, ("#e8d8f8", "#b090d8")) for s in range(4)],
        "strawberry": [rosette(s, GREEN, "strawberry") for s in range(4)],
        "sugar_beet": [rosette(s, GREEN, "sugar_beet") for s in range(4)],
        "sweet_potato": [rosette(s, DEEP, "sweet_potato") for s in range(4)],
        "peanut": [rosette(s, GREEN, "peanut") for s in range(4)],
        "rose": [flower_plant(s, DEEP, ("#e0405a", "#7a1428"), "rose") for s in range(4)],
        "hibiscus": [flower_plant(s, GREEN, ("#f878aa", "#a82060"), "hibiscus") for s in range(4)],
        "starbloom": [flower_plant(s, GREEN, ("#fff080", "#c8a020"), "starbloom") for s in range(4)],
        "candyfloss": [flower_plant(s, MINT, ("#ffd0e8", "#e080b0"), "candyfloss") for s in range(4)],
        "lavender": [lavender(s) for s in range(4)],
        "vanilla": [vanilla(s) for s in range(4)],
        "rhubarb": [rhubarb(s) for s in range(4)],
        "pineapple": [pineapple(s) for s in range(4)],
    }
    n = 0
    for name, stages in out.items():
        for i, img in enumerate(stages):
            img.save(os.path.join(TEX, f"{name}_crop_stage{i}.png"))
            n += 1
    return n


if __name__ == "__main__":
    print(build(), "16x16 crop stages written")
