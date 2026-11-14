"""
Wild nature: flowers, ferns, cattails, ground cover, flowering bushes, a blossom lily pad, a mossy log and a mossy boulder.

Every plant is drawn like the saplings: clean 1px stems, a few hand-placed leaves in two layers (darker back leaves,
lit front leaves with a 1px shadow), flowers lit from the upper left. Blocks use vanilla-style models (cross for
flowers); round bits use round_model. Writes textures, models, blockstates, item models and loot tables.
Run: python3 blockbench/gen_wild.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import sap_leaf, blade, stem, put, canvas, hx, TEX
from round_model import Model, BLOCK_DISPLAY
from gen_nature import fern, top_full

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")
OUT = os.path.join(TEX, "wild")
LEAF = [hx(c) for c in ("#1e4a22", "#2e6a2c", "#4a8c3a", "#6aaa4c", "#90c86a")]
SOFT = [hx(c) for c in ("#2a5a34", "#3c7a40", "#58984c", "#78b45e", "#9ccc7c")]
REED = [hx(c) for c in ("#3a5a2a", "#507a34", "#6a9640", "#88b052", "#a8c86c")]
STEM = hx("#5a9a44"), hx("#3e7a32")


def tone(c, f):
    """Lighter (f > 1) or darker (f < 1) version of a colour."""
    c = hx(c) if isinstance(c, str) else c
    if f >= 1:
        return tuple(min(255, int(v + (255 - v) * (f - 1))) for v in c[:3])
    return tuple(int(v * f) for v in c[:3])


def blob(img, x, y, pattern, cols):
    """Draws a little hand-made sprite: pattern rows of characters, cols maps a character to a colour."""
    for dy, row in enumerate(pattern):
        for dx, ch in enumerate(row):
            if ch in cols:
                put(img, x + dx, y + dy, cols[ch])


# ------------------------------------------------------------------ flowers (cross models)

DAISY = ["..w..", ".wWw.", "wWyWw", ".wWg.", "..g.."]
DAISY_S = [".w.", "wyw", ".g."]


def daisies():
    img = canvas()
    sap_leaf(img, 4.5, 13.5, 4.0, 2.0, -0.5, LEAF, back=True)
    sap_leaf(img, 11.5, 13.0, 4.0, 2.0, 0.6, LEAF, back=True)
    for pts in (((5, 15), (5, 10), (4, 7)), ((10, 15), (11, 9), (11, 5)), ((8, 15), (8, 11))):
        stem(img, pts, *STEM)
    sap_leaf(img, 7.0, 13.5, 3.6, 1.8, 0.4, LEAF)
    cols = {"w": hx("#ffffff"), "W": hx("#f6f4fa"), "g": hx("#dcd8e6"), "y": hx("#f6c23a")}
    blob(img, 9, 2, DAISY, cols)
    blob(img, 2, 5, DAISY, cols)
    blob(img, 7, 9, DAISY_S, cols)
    return img


COSMOS = [".p.p.", "ppPpp", "pPyPd", "ppPdd", ".d.d."]


def pink_cosmos():
    img = canvas()
    for pts in (((6, 15), (6, 9), (4, 4)), ((9, 15), (10, 8), (12, 2)), ((8, 15), (8, 10))):
        stem(img, pts, *STEM)
    # feathery leaves: thin 1px leaflets off the stems
    for (x, y, d) in ((6, 12, -1), (6, 10, 1), (9, 12, 1), (10, 10, -1), (11, 6, 1), (5, 7, -1)):
        put(img, x + d, y, LEAF[3]); put(img, x + 2 * d, y - 1, LEAF[2])
    cols = {"p": hx("#f7a8c6"), "P": hx("#ffc8dc"), "d": hx("#de86aa"), "y": hx("#f6c23a")}
    blob(img, 2, 1, COSMOS, cols)
    blob(img, 10, 0, COSMOS, cols)
    blob(img, 6, 7, [".p.", "pyd", ".d."], cols)
    return img


def forget_me_nots():
    img = canvas()
    for (x, y, l, w, a, b) in ((3.5, 12.5, 5, 3, -0.4, True), (12.5, 12.5, 5, 3, 0.4, True), (8, 11.5, 5, 3.2, -1.2, True),
                               (5.5, 13, 4.6, 2.8, 0.3, False), (10.5, 13, 4.6, 2.8, -0.3, False), (8, 10, 3.6, 2.6, 0.1, False)):
        sap_leaf(img, x, y, l, w, a, LEAF, back=b)
    blue, lit, heart = hx("#8ec6f0"), hx("#b8dcf8"), hx("#ffe08a")
    for (x, y) in ((3, 9), (7, 7), (11, 8), (13, 11), (5, 12), (9, 11), (2, 13), (12, 6)):
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            put(img, x + dx, y + dy, lit if dx + dy < 0 else blue)
        put(img, x, y, heart)
    return img


def lily_of_the_valley():
    img = canvas()
    sap_leaf(img, 5.0, 10.0, 11.0, 4.0, -1.85, SOFT, back=True)
    sap_leaf(img, 11.0, 10.5, 10.0, 3.6, -1.3, SOFT)
    stem(img, [(8, 15), (8, 6), (9, 3), (11, 2), (13, 3)], *STEM)
    white, shade, edge = hx("#ffffff"), hx("#eceaf0"), hx("#d6d2de")
    for (x, y) in ((8, 6), (9, 4), (11, 3), (13, 4)):
        put(img, x - 1, y + 1, white); put(img, x, y + 1, white); put(img, x + 1, y + 1, shade)
        put(img, x - 1, y + 2, edge); put(img, x + 1, y + 2, edge); put(img, x, y + 2, shade)
    return img


def bluebells():
    img = canvas()
    blade(img, 4, 15, 2, 6, SOFT, back=True)
    blade(img, 12, 15, 14, 7, SOFT)
    lit, mid, dark = hx("#aab0f4"), hx("#8a8ee6"), hx("#6a6cc8")
    for pts, bells in ((((6, 15), (6, 5), (8, 2), (10, 3)), ((10, 4), (8, 3), (6, 6), (6, 9))),
                       (((9, 15), (10, 9), (12, 7)), ((12, 8), (10, 10)))):
        stem(img, list(pts), *STEM)
        for (x, y) in bells:
            put(img, x + 1, y, lit); put(img, x + 1, y + 1, mid); put(img, x + 2, y + 1, dark)
            put(img, x + 1, y + 2, mid); put(img, x + 2, y + 2, dark); put(img, x + 3, y + 2, dark)
    return img


def buttercups():
    img = canvas()
    for (x, y, a) in ((4, 14, -0.5), (12, 14, 0.5)):
        sap_leaf(img, x, y, 3.6, 2.4, a, LEAF, back=True)
    for pts in (((4, 15), (4, 9), (3, 6)), ((8, 15), (8, 4)), ((11, 15), (12, 8)), ((6, 15), (6, 12))):
        stem(img, pts, *STEM)
    sap_leaf(img, 9.5, 13.0, 3.4, 2.2, 0.3, LEAF)
    cols = {"y": hx("#ffd84a"), "Y": hx("#fff2a0"), "o": hx("#e8b02a")}
    for (x, y) in ((2, 4), (7, 2), (11, 6), (5, 10)):
        blob(img, x, y, ["y.y", "Yyo", ".o."], cols)
    return img


def foxglove():
    img = canvas()
    for (x, y, l, w, a, b) in ((4.5, 13, 6, 3, -0.4, True), (11.5, 13, 6, 3, 0.4, True), (6, 14, 5, 2.6, 0.2, False), (10, 14, 5, 2.6, -0.2, False)):
        sap_leaf(img, x, y, l, w, a, SOFT, back=b)
    stem(img, [(8, 15), (8, 0)], *STEM)
    lit, mid, dark, dot = hx("#ffc0dc"), hx("#f29ac6"), hx("#d47aa8"), hx("#a04a7a")
    for i, y in enumerate(range(10, 1, -2)):
        side = 1 if i % 2 == 0 else -1
        size = 3 if y > 5 else 2
        x0 = 8 + side
        for k in range(size):
            put(img, x0 + side * k, y, lit if k == 0 else mid)
            put(img, x0 + side * k, y + 1, mid if k < size - 1 else dark)
        put(img, x0 + side * (size - 1), y + 1, dot if size == 3 else dark)
    put(img, 8, 0, hx("#ffd0e4"))
    return img


def cattails():
    img = canvas()
    blade(img, 3, 15, 1, 4, REED, back=True)
    blade(img, 13, 15, 15, 5, REED, back=True)
    blade(img, 6, 15, 4, 2, REED)
    blade(img, 11, 15, 12, 6, REED)
    stem(img, [(8, 15), (8, 1)], REED[3], REED[2])
    stem(img, [(10, 15), (10, 4)], REED[3], REED[2])
    lit, mid, dark = hx("#a8744c"), hx("#8a5a3a"), hx("#6a422a")
    for (x, top) in ((8, 3), (10, 6)):
        for y in range(top, top + 5):
            put(img, x, y, lit if y == top else mid)
            put(img, x + 1, y, dark if y > top else mid)
    return img


# ------------------------------------------------------------------ ground cover

def clover_top():
    """Clover seen from above: three-leaf clovers, a few soft white-pink clover flowers."""
    img = canvas()
    for (x, y) in ((3, 3), (11, 2), (7, 7), (13, 9), (3, 11), (9, 13), (14, 14), (1, 7)):
        for (dx, dy) in ((-1, -1), (1, -1), (0, 1)):
            put(img, x + dx, y + dy, LEAF[3]); put(img, x + dx + 1, y + dy, LEAF[2])
            put(img, x + dx, y + dy + 1, LEAF[2]); put(img, x + dx + 1, y + dy + 1, LEAF[1])
        put(img, x, y, LEAF[1])
    for (x, y) in ((6, 3), (12, 12), (4, 14)):
        put(img, x, y, hx("#ffffff")); put(img, x + 1, y, hx("#fbe8f0"))
        put(img, x, y + 1, hx("#fbe8f0")); put(img, x + 1, y + 1, hx("#f2c8da"))
    return img


def clover_side():
    """A few clover flowers standing up out of the patch."""
    img = canvas()
    for (x, top) in ((4, 9), (9, 7), (12, 10)):
        stem(img, [(x, 15), (x, top + 2)], *STEM)
        put(img, x, top, hx("#ffffff")); put(img, x + 1, top, hx("#fbe8f0"))
        put(img, x, top + 1, hx("#fbe8f0")); put(img, x + 1, top + 1, hx("#f2c8da"))
        put(img, x - 1, 14, LEAF[3]); put(img, x + 1, 14, LEAF[2])
    return img


def moss_top():
    """Soft moss, calm: two greens in rounded tufts, transparent between."""
    img = canvas()
    rnd = random.Random(7)
    for (x, y, r) in ((4, 4, 3.2), (11, 5, 2.6), (6, 11, 3.0), (12, 12, 2.4), (2, 13, 1.6)):
        for py in range(16):
            for px in range(16):
                d = math.hypot(px - x, py - y)
                if d <= r:
                    put(img, px, py, SOFT[3] if px + py < x + y - 1 else SOFT[2])
                elif d <= r + 0.7 and rnd.random() < 0.5:
                    put(img, px, py, SOFT[1])
    return img


def moss_full():
    """Moss covering a surface completely (log and boulder tops)."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, SOFT[2])
    for (x, y, r) in ((3, 3, 2.2), (10, 4, 2.6), (6, 10, 2.4), (13, 12, 2.0), (1, 13, 1.6), (14, 1, 1.4)):
        for py in range(16):
            for px in range(16):
                d = math.hypot(px - x, py - y)
                if d <= r:
                    put(img, px, py, SOFT[3] if px + py < x + y else SOFT[2])
                elif d <= r + 0.8:
                    put(img, px, py, SOFT[1])
    return img


def pebble():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#ddd8d2") if y < 6 else hx("#c8c2bc") if y < 11 else hx("#b2aca6"))
    return img


def flat(c):
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx(c))
    return img


def ramp(*cs):
    img = canvas()
    for y in range(16):
        c = cs[min(len(cs) - 1, y * len(cs) // 16)]
        for x in range(16):
            put(img, x, y, hx(c))
    return img


def cap_top(base, spots):
    img = canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            put(img, x, y, tone(base, 1.18) if d < 4 and x + y < 14 else hx(base) if d < 6 else tone(base, 0.88))
    if spots:
        for (x, y) in ((5, 5), (10, 4), (4, 10), (10, 10)):
            put(img, x, y, hx("#fff6f8")); put(img, x + 1, y, hx("#fff6f8")); put(img, x, y + 1, hx("#f6e6ea"))
    return img


# ------------------------------------------------------------------ bushes

def bush(flower):
    """A dense, calm leafy face: overlapping leaves in two layers, then a few flowers sitting on top."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, LEAF[1])
    rnd = random.Random(11)
    spots = [(x + rnd.uniform(-0.8, 0.8), y + rnd.uniform(-0.8, 0.8)) for y in range(1, 17, 4) for x in range(1 + (y // 4) % 2 * 2, 17, 4)]
    for (x, y) in spots:
        sap_leaf(img, x, y, 4.2, 2.6, rnd.uniform(-1, 1), LEAF, back=True, cast=False)
    for (x, y) in spots[::2]:
        sap_leaf(img, x + 1.5, y + 1.0, 3.8, 2.4, rnd.uniform(-1, 1), LEAF)
    if flower == "rose":
        cols = {"l": hx("#ffc0d4"), "m": hx("#f48aaa"), "d": hx("#d0607e"), "s": LEAF[0]}
        for (x, y) in ((2, 2), (10, 4), (5, 10), (12, 11)):
            put(img, x + 3, y + 3, LEAF[0])
            blob(img, x, y, [".lm.", "lmdm", "mdmd", ".dd."], cols)
    else:
        for (x, y, pal) in ((1, 2, ("#c8dafc", "#a8c4f4", "#8aa6e0")), (9, 3, ("#e4d0fa", "#d2b8f0", "#b498dc")),
                            (4, 9, ("#e4d0fa", "#d2b8f0", "#b498dc")), (11, 10, ("#c8dafc", "#a8c4f4", "#8aa6e0"))):
            cols = {"l": hx(pal[0]), "m": hx(pal[1]), "d": hx(pal[2])}
            blob(img, x, y, [".lmm.", "lmlmm", "mlmmd", "mmmdd", ".mdd."], cols)
    return img


# ------------------------------------------------------------------ water and forest

def lily_pad():
    """A round pad from above with a notch, lit toward the upper left, faint veins."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 360
            if d > 7.4 or (d > 1 and 30 < a < 52):
                continue
            c = "#72b25a" if dx + dy < -2 else "#5a9a4a"
            if d > 6.4:
                c = "#46803c"
            elif round(a) % 60 < 5 and d > 2:
                c = "#4e8a40"
            put(img, x, y, hx(c))
    return img


def lily_bloom():
    """A water lily seen from the side: pointed pink petals opening like a crown, a gold heart."""
    img = canvas()
    cols = {"l": hx("#ffd0e2"), "p": hx("#f7a8c6"), "d": hx("#de86aa"), "y": hx("#f6c84a")}
    blob(img, 4, 9, ["p..l..p", "pl.l.pd", "plpypdd", "lppppdd", ".lppdd."], cols)
    return img


def log_side():
    """The side of a fallen log (rows 4-12 are used): moss along the top, then soft bark lines."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            if y <= 5 or (y == 6 and x % 5 in (1, 2)):
                put(img, x, y, SOFT[3] if y < 5 else SOFT[2])
            else:
                k = ("#8a5e3c", "#9a6a44", "#7a5032")[(y + (x // 6)) % 3]
                put(img, x, y, hx(k))
    return img


def log_end():
    img = canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = "#6a4428" if d > 7 else ("#e8c890" if int(d) % 2 else "#d4ae74")
            put(img, x, y, hx(c))
    return img


def boulder_side():
    """Soft grey-blue stone, moss creeping over the top rows (rows map to height: row 16 - y)."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            if y <= 6 or (y == 7 and x % 4 in (0, 1)):
                put(img, x, y, SOFT[3] if y < 6 else SOFT[2])
            else:
                put(img, x, y, hx("#b6b8c2") if y < 11 else hx("#a2a4b0"))
    for (x, y) in ((3, 10), (11, 12), (7, 14)):
        put(img, x, y, hx("#c8cad2"))
    return img


# ------------------------------------------------------------------ models

CROSS = ["daisies", "pink_cosmos", "forget_me_nots", "lily_of_the_valley", "bluebells", "buttercups", "foxglove", "cattails", "wild_fern"]


def el(name, frm, to, faces, rotation=None):
    e = {"name": name, "from": list(frm), "to": list(to), "faces": faces}
    if rotation:
        e["rotation"] = rotation
    return e


def faces(tex, which="nsewud", **uv):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    out = {}
    for ch in which:
        f = {"texture": "#" + (tex if isinstance(tex, str) else tex.get(ch, tex["*"]))}
        if ch in uv:
            f["uv"] = uv[ch]
        out[names[ch]] = f
    return out


def model(textures, elements, parent="block/block"):
    return {"parent": parent, "ambientocclusion": False, "textures": textures, "elements": elements}


def ground_patch(top, side):
    rot = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    return model({"particle": top, "top": top, "side": side}, [
        el("patch", (0, 0.1, 0), (16, 0.1, 16), faces("top", "ud", u=[0, 0, 16, 16], d=[0, 0, 16, 16])),
        el("a", (2, 0, 8), (14, 6, 8), faces("side", "ns", n=[2, 10, 14, 16], s=[2, 10, 14, 16]), rot),
        el("b", (8, 0, 2), (8, 6, 14), faces("side", "ew", e=[2, 10, 14, 16], w=[2, 10, 14, 16]), rot)])


def mossy_pebbles():
    p = "bakery:block/wild/pebble"
    els = [el("moss", (0, 0.1, 0), (16, 0.1, 16), faces("moss", "ud", u=[0, 0, 16, 16], d=[0, 0, 16, 16]))]
    for i, (a, b) in enumerate((((3, 0, 4), (6, 1.2, 7)), ((9, 0, 3), (11.5, 1, 5)), ((10, 0, 9), (14, 1.6, 12.5)), ((4, 0, 11), (6.5, 0.8, 13)))):
        els.append(el(f"pebble{i}", a, b, faces("pebble", "nsewu")))
    return model({"particle": p, "moss": "bakery:block/wild/moss_top", "pebble": p}, els)


def wild_mushrooms():
    m = Model({"stem": "bakery:block/wild/mushroom_stem", "brown": "bakery:block/wild/cap_brown", "brown_top": "bakery:block/wild/cap_brown_top",
               "pink": "bakery:block/wild/cap_pink", "pink_top": "bakery:block/wild/cap_pink_top", "moss": "bakery:block/wild/moss_top"})
    m.box("moss", (0, 0.1, 0), (16, 0.1, 16), "moss", faces=["up"])
    m.data["elements"][-1]["faces"]["up"]["uv"] = [0, 0, 16, 16]
    for name, cx, cz, h, r, tex in (("a", 6, 7, 4.2, 2.4, "brown"), ("b", 10.5, 9.5, 3.0, 1.8, "pink"), ("c", 7.5, 11.5, 1.8, 1.2, "brown")):
        m.disc(name + "_stem", 0.6 if r < 2 else 0.8, 0, h, "stem", cx=cx, cz=cz)
        m.disc(name + "_cap", r, h, h + r * 0.55, tex, cx=cx, cz=cz, top=tex + "_top")
        m.disc(name + "_cap2", r * 0.6, h + r * 0.55, h + r * 0.85, tex, cx=cx, cz=cz, top=tex + "_top")
    for e in m.data["elements"]:
        up = e["faces"].get("up")
        if up and up["texture"].endswith("_top"):
            name = e["name"].split("_")[0]
            r = {"a": 2.4, "b": 1.8, "c": 1.2}[name]
            cx, cz = {"a": (6, 7), "b": (10.5, 9.5), "c": (7.5, 11.5)}[name]
            hw, hd = (e["to"][0] - e["from"][0]) / 2 * 8 / r, (e["to"][2] - e["from"][2]) / 2 * 8 / r
            up["uv"] = [round(8 - hw, 3), round(8 - hd, 3), round(8 + hw, 3), round(8 + hd, 3)]
    return m.data


def bush_model(tex):
    """One box (uv follows the block, as in vanilla); the rounded, leafy outline is cut into the textures."""
    side, top = f"bakery:block/wild/{tex}", f"bakery:block/wild/{tex}_top"
    return model({"particle": side, "side": side, "top": top}, [
        el("bush", (1, 0, 1), (15, 14, 15), {"north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                                            "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#side"}})])


def round_off(img, corners, r=3.2):
    """Makes the given corners of the 14px face (cols 1-14) round: corners are (x0, y0, cx, cy) squares and centres."""
    for (x0, y0, cx, cy) in corners:
        for y in range(y0, y0 + 3):
            for x in range(x0, x0 + 3):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) > r:
                    img.putpixel((x, y), (0, 0, 0, 0))
    return img


def bush_side(flower):
    img = bush(flower)
    round_off(img, ((1, 2, 4.2, 5.2), (12, 2, 11.8, 5.2)))
    for x in (6, 10):
        img.putpixel((x, 2), (0, 0, 0, 0))
    return img


def bush_top(flower):
    img = bush(flower)
    return round_off(img, ((1, 1, 4.2, 4.2), (12, 1, 11.8, 4.2), (1, 12, 4.2, 11.8), (12, 12, 11.8, 11.8)))


def lily_pad_model():
    rot = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    pad, bloom = "bakery:block/wild/lily_pad", "bakery:block/wild/lily_bloom"
    return model({"particle": pad, "pad": pad, "bloom": bloom}, [
        el("pad", (0, 0.25, 0), (16, 0.25, 16), faces("pad", "ud", u=[0, 0, 16, 16], d=[0, 16, 16, 0])),
        el("bloom_a", (4, 0.25, 8), (12, 6.25, 8), faces("bloom", "ns", n=[4, 9, 12, 15], s=[4, 9, 12, 15]), rot),
        el("bloom_b", (8, 0.25, 4), (8, 6.25, 12), faces("bloom", "ew", e=[4, 9, 12, 15], w=[4, 9, 12, 15]), rot)])


def mossy_log():
    t = {"bark": "bakery:block/wild/log_side", "end": "bakery:block/wild/log_end", "moss": "bakery:block/wild/moss_full",
         "stem": "bakery:block/wild/mushroom_stem", "cap": "bakery:block/wild/cap_pink", "particle": "bakery:block/wild/log_side"}
    # octagonal log along x, ends at x=1 and x=15; uv on the ends maps the 8x8 end (z 4..12, y 0..8) to the 16x16 rings
    core = el("core", (1, 0, 5), (15, 8, 11), {"north": {"texture": "#bark", "uv": [1, 4, 15, 12]}, "south": {"texture": "#bark", "uv": [1, 4, 15, 12]},
                                              "up": {"texture": "#moss"}, "down": {"texture": "#bark"},
                                              "east": {"texture": "#end", "uv": [2, 0, 14, 16]}, "west": {"texture": "#end", "uv": [2, 0, 14, 16]}})
    wide = el("wide", (1, 1, 4), (15, 7, 12), {"north": {"texture": "#bark", "uv": [1, 5, 15, 11]}, "south": {"texture": "#bark", "uv": [1, 5, 15, 11]},
                                              "up": {"texture": "#moss"}, "down": {"texture": "#bark"},
                                              "east": {"texture": "#end", "uv": [0, 2, 16, 14]}, "west": {"texture": "#end", "uv": [0, 2, 16, 14]}})
    els = [core, wide]
    for i, (x, z, h, r) in enumerate(((4, 7, 1.6, 1.3), (5.6, 8.6, 1.0, 0.9), (11.5, 7.5, 1.3, 1.1))):
        els.append(el(f"mush{i}_stem", (x - 0.3, 8, z - 0.3), (x + 0.3, 8 + h, z + 0.3), faces("stem", "nsew")))
        els.append(el(f"mush{i}_cap", (x - r, 8 + h, z - r), (x + r, 8 + h + 0.8, z + r), faces("cap", "nsewud")))
    return model(t, els)


def mossy_boulder():
    t = {"stone": "bakery:block/wild/boulder_side", "moss": "bakery:block/wild/moss_full", "particle": "bakery:block/wild/boulder_side"}
    side = {"texture": "#stone"}
    def box(name, a, b):
        return el(name, a, b, {"north": dict(side), "south": dict(side), "east": dict(side), "west": dict(side), "up": {"texture": "#moss"}})
    return model(t, [box("core", (2, 0, 3), (14, 7, 13)), box("front", (3, 1, 2), (13, 6, 14)), box("wide", (1, 1, 4), (15, 6, 12)),
                     box("top", (4, 7, 4), (12, 9.5, 12)), box("crown", (5.5, 9.5, 5.5), (10.5, 10.2, 10.5)),
                     box("pebble", (12.5, 0, 12.5), (15, 2.5, 15))])


def build():
    os.makedirs(OUT, exist_ok=True)
    textures = {n: globals()[n]() for n in CROSS if n != "wild_fern"}
    textures.update({"wild_fern": fern(), "clover_top": clover_top(), "clover_side": clover_side(), "moss_top": moss_top(),
                     "moss_full": moss_full(), "pebble": pebble(), "mushroom_stem": ramp("#fff8ec", "#f2e6d2", "#e6d6be"),
                     "cap_brown": ramp("#c08a5c", "#b07a50", "#9a6844"), "cap_brown_top": cap_top("#b07a50", False),
                     "cap_pink": ramp("#ffa8c4", "#f68aac", "#e8749a"), "cap_pink_top": cap_top("#f68aac", True),
                     "rose_bush": bush_side("rose"), "hydrangea_bush": bush_side("hydrangea"),
                     "rose_bush_top": bush_top("rose"), "hydrangea_bush_top": bush_top("hydrangea"), "lily_pad": lily_pad(), "lily_bloom": lily_bloom(),
                     "log_side": log_side(), "log_end": log_end(), "boulder_side": boulder_side()})
    for name, img in textures.items():
        img.save(os.path.join(OUT, name + ".png"))

    models = {n: {"parent": "minecraft:block/cross", "textures": {"cross": f"bakery:block/wild/{n}"}} for n in CROSS}
    models.update({"clover_patch": ground_patch("bakery:block/wild/clover_top", "bakery:block/wild/clover_side"),
                   "mossy_pebbles": mossy_pebbles(), "wild_mushrooms": wild_mushrooms(), "rose_bush": bush_model("rose_bush"),
                   "hydrangea_bush": bush_model("hydrangea_bush"), "blossom_lily_pad": lily_pad_model(),
                   "mossy_log": mossy_log(), "mossy_boulder": mossy_boulder()})
    flat_items = {n: f"bakery:block/wild/{n}" for n in CROSS}
    flat_items.update({"blossom_lily_pad": "bakery:block/wild/lily_pad", "clover_patch": "bakery:block/wild/clover_top"})
    for name, data in models.items():
        with open(os.path.join(A, "models/block", name + ".json"), "w") as f:
            json.dump(data, f, indent="\t"); f.write("\n")
        if name in ("mossy_log", "mossy_boulder"):
            variants = {f"facing={d}": {"model": f"bakery:block/{name}", "y": y} for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
        elif name in ("blossom_lily_pad", "mossy_pebbles", "clover_patch"):
            variants = {"": [{"model": f"bakery:block/{name}", "y": y} for y in (0, 90, 180, 270)]}
        else:
            variants = {"": {"model": f"bakery:block/{name}"}}
        with open(os.path.join(A, "blockstates", name + ".json"), "w") as f:
            json.dump({"variants": variants}, f, indent="\t"); f.write("\n")
        item = {"parent": "minecraft:item/generated", "textures": {"layer0": flat_items[name]}} if name in flat_items \
            else {"parent": f"bakery:block/{name}", "display": BLOCK_DISPLAY}
        with open(os.path.join(A, "models/item", name + ".json"), "w") as f:
            json.dump(item, f, indent="\t"); f.write("\n")
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"bakery:{name}"}],
                                                        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
        with open(os.path.join(ROOT, "data/bakery/loot_table/blocks", name + ".json"), "w") as f:
            json.dump(loot, f, indent="\t"); f.write("\n")
    return len(models)


if __name__ == "__main__":
    print(build(), "wild nature blocks written")
