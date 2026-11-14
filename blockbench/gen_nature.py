"""
Nature decorations: a glowing mushroom lamp, a potted succulent, a potted fern, a bird bath with a little bluebird,
a patch of wildflowers, hanging ivy, mossy stepping stones and a tree-stump stool with a cushion.

Models are built from round shapes (textures wrapped round them), plants are drawn like the saplings (two layers of
leaves with shadows). Writes models, blockstates, item models, loot tables and 16x16 textures.
Run: python3 blockbench/gen_nature.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import sap_leaf, blade, stem, put, canvas, hx, TEX
from round_model import Model, BLOCK_DISPLAY

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")
LEAF = [hx(c) for c in ("#1e4a22", "#2e6a2c", "#4a8c3a", "#6aaa4c", "#90c86a")]
FERN = [hx(c) for c in ("#1a4426", "#2a6234", "#3e8244", "#5aa25a", "#80c276")]
IVY = [hx(c) for c in ("#173c22", "#245a2e", "#38783e", "#549852", "#78b66c")]


def ramp_fill(img, rows, cols16=True):
    """Fills each row with a colour: rows is a list of 16 colours (top to bottom)."""
    for y, c in enumerate(rows):
        for x in range(16):
            put(img, x, y, hx(c))


# ------------------------------------------------------------------ textures

def mushroom_cap():
    """Unrolled toadstool cap side: soft pink, a shade darker toward the rim."""
    img = canvas()
    ramp_fill(img, ["#ff9ab8"] * 5 + ["#f683aa"] * 6 + ["#e86c96"] * 5)
    return img


def mushroom_cap_top():
    """The cap seen from above: lit toward the upper left, with a few big soft white spots."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            lit = (x + y) < 13
            put(img, x, y, hx("#ffb0c8" if d < 3 and lit else "#ff9ab8" if d < 5.5 else "#f683aa"))
    for (x, y) in ((6, 6), (10, 4), (3, 9), (11, 10), (6, 12), (2, 4), (13, 7)):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            put(img, x + dx, y + dy, hx("#fff6f8"))
        put(img, x + 1, y + 1, hx("#f6dee6"))
    return img


def mushroom_gills():
    """The underside: warm glowing gills radiating out."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            a = math.atan2(y - 7.5, x - 7.5)
            put(img, x, y, hx("#ffe6a0") if int((a + math.pi) / (2 * math.pi) * 24) % 2 else hx("#f6c870"))
    return img


def mushroom_stem():
    img = canvas()
    ramp_fill(img, ["#fff8ec"] * 4 + ["#f6ead6"] * 8 + ["#e6d4bc"] * 4)
    for x in (3, 9, 13):
        for y in range(2, 14):
            put(img, x, y, hx("#ecdcc6"))
    return img


SUCC = [hx(c) for c in ("#3e7a64", "#5a9a80", "#7ab8a0", "#9ad2b8", "#c0ead4")]


def succulent_top():
    """A succulent rosette from above: plump leaves in three rings (outer ones darker, blush-pink tips)."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, SUCC[1])
    for ring, (n, dist, length, width, off) in enumerate(((8, 5.0, 5.4, 3.6, 0.0), (6, 2.9, 4.2, 3.0, 0.3), (4, 1.0, 3.0, 2.4, 0.8))):
        for i in range(n):
            a = 2 * math.pi * i / n + off
            cx, cy = 7.5 + math.cos(a) * dist, 7.5 + math.sin(a) * dist
            sap_leaf(img, cx + 0.5, cy + 0.5, length, width, a, SUCC, back=ring == 0)
            if ring == 0:
                put(img, round(7.5 + math.cos(a) * 7.2), round(7.5 + math.sin(a) * 7.2), hx("#f2a2c0"))
    return img


def succulent_side():
    img = canvas()
    ramp_fill(img, ["#f2a2c0"] * 2 + ["#9ad2b8"] * 6 + ["#7ab8a0"] * 8)
    return img


def pot_mint():
    """A small mint-glazed pot: a lighter rim, one white band."""
    img = canvas()
    ramp_fill(img, ["#c8eedc"] * 3 + ["#8ccab0"] + ["#a8dcc6"] * 3 + ["#ffffff", "#e4f6ec"] + ["#a8dcc6"] * 5 + ["#8ccab0"] * 2)
    return img


def fern():
    """Arching fern fronds (crossed planes): a rib with leaflets, back fronds darker."""
    img = canvas()
    for (x0, y0, x1, y1) in ((7, 15, 2, 4), (8, 15, 13, 4)):
        blade(img, x0, y0, x1, y1, FERN, back=True, leaflets=True)
    for pts in (((7, 15), (4, 8), (1, 6)), ((8, 15), (11, 8), (14, 6)), ((7, 15), (6, 6), (5, 2)), ((8, 15), (9, 6), (10, 2))):
        for (a, b) in zip(pts, pts[1:]):
            blade(img, a[0], a[1], b[0], b[1], FERN, leaflets=True)
    return img


def wildflowers_top():
    """Seen from above: clover and little flowers scattered over the ground (clear in between)."""
    img = canvas()
    for (x, y) in ((2, 3), (11, 2), (6, 7), (13, 10), (3, 12), (9, 13)):
        for (dx, dy, k) in ((0, 0, 3), (1, 0, 2), (0, 1, 2), (1, 1, 1), (-1, 1, 3), (1, -1, 3)):
            put(img, x + dx, y + dy, LEAF[k])
    for (x, y, c) in ((4, 5, "#ffffff"), (9, 4, "#f7a8c6"), (12, 7, "#ffe08a"), (5, 10, "#cdb4f0"), (11, 12, "#ffffff"), (1, 8, "#f7a8c6"),
                      (14, 14, "#cdb4f0"), (7, 1, "#ffe08a")):
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            put(img, x + dx, y + dy, hx(c))
        put(img, x, y, hx("#f6c84a"))
    return img


def wildflowers_side():
    """A few flowers standing up out of the patch, on thin stems."""
    img = canvas()
    for (x, top, c) in ((3, 8, "#f7a8c6"), (8, 6, "#ffffff"), (12, 9, "#cdb4f0")):
        stem(img, [(x, 15), (x, top + 1)], LEAF[2], LEAF[1])
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            put(img, x + dx, top + dy, hx(c))
        put(img, x, top, hx("#f6c84a"))
        sap_leaf(img, x + 1.2, 13.0, 2.6, 1.6, 0.4, LEAF)
    return img


def ivy():
    """Hanging ivy for a wall: vines trailing down from the top, heart-shaped leaves in two layers."""
    img = canvas()
    vine = hx("#5e6a2e"), hx("#46502a")
    for x0, length in ((3, 15), (8, 10), (12, 14)):
        stem(img, [(x0, 0), (x0 + 1, length // 2), (x0, length)], *vine)
    for x0, length in ((3, 15), (8, 10), (12, 14)):
        for i, y in enumerate(range(2, length, 3)):
            side = -1 if i % 2 else 1
            sap_leaf(img, x0 + side * 1.6 + 0.5, y + 0.5, 3.2, 2.6, 0.5 * side + 1.57, IVY, back=(i + x0) % 3 == 0)
    return img


def stone(moss=True):
    """Pastel cream stone, smooth with a few pebbles and a little moss at the edges."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#f2e8d8") if (x + y) % 7 else hx("#e6dac6"))
    for (x, y) in ((3, 4), (11, 3), (7, 10), (13, 12)):
        put(img, x, y, hx("#d8c8b0")); put(img, x + 1, y, hx("#fff8ec"))
    if moss:
        for (x, y) in ((0, 14), (1, 15), (14, 0), (15, 1), (15, 15), (0, 0)):
            put(img, x, y, hx("#8ab86a"))
    return img


def water():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#9ad6f0"))
            if (x - 4.5) ** 2 + (y - 4.5) ** 2 < 5:
                put(img, x, y, hx("#b8e6f8"))
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        put(img, x, y, hx("#e4f6fd"))
    return img


def bird():
    """A bluebird's colours: blue back, peachy breast, a dark eye."""
    img = canvas()
    ramp_fill(img, ["#6aa8e6"] * 8 + ["#ffc8a0"] * 8)
    put(img, 4, 4, hx("#2a2a3a")); put(img, 11, 4, hx("#2a2a3a"))
    return img


def bark_wrap():
    """Unrolled stump bark: soft vertical furrows, mossy at the foot."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            k = ("#8a5e3c", "#a8764c", "#7a5032", "#9a6a44")[x % 4]
            if y > 12 and x % 3 == 0:
                k = "#7aa85a"
            put(img, x, y, hx(k))
    return img


def stump_top():
    """Tree rings on the cut top, a dark rim of bark."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = "#7a5032" if d > 7 else ("#e8c890" if int(d) % 2 else "#d4ae74")
            put(img, x, y, hx(c))
    return img


# ------------------------------------------------------------------ models

def top_full(m, name, r, same_scale=True):
    """Maps a round 16x16 picture over the tops of discs: every slab's up face shows its own part of it, at one
    scale (radius r fills the picture), so stacked tiers line up into one coherent top view."""
    k = 8 / r
    for e in m.data["elements"]:
        f = e["faces"].get("up")
        if not f or f.get("texture") != "#" + name:
            continue
        hw = (e["to"][0] - e["from"][0]) / 2 * k
        hd = (e["to"][2] - e["from"][2]) / 2 * k
        f["uv"] = [round(8 - hw, 3), round(8 - hd, 3), round(8 + hw, 3), round(8 + hd, 3)]


def tex(*names):
    return {n: f"bakery:block/{f}" for n, f in names}


def mushroom_lamp():
    m = Model(tex(("stem", "nature/mushroom_stem"), ("cap", "nature/mushroom_cap"), ("top", "nature/mushroom_cap_top"),
                  ("gills", "nature/mushroom_gills")))
    m.disc("stem", 1.5, 0, 7.2, "stem")
    for i, (r, y1, y2) in enumerate(((5.4, 7.2, 8.4), (5.0, 8.4, 9.4), (4.3, 9.4, 10.3), (3.3, 10.3, 11.0), (2.0, 11.0, 11.5))):
        m.disc(f"cap{i}", r, y1, y2, "cap", top="top", bottom="gills" if i == 0 else None)
    m.wrap("cap", 7.2, 11.5)
    m.wrap("stem", 0, 7.2)
    top_full(m, "top", 5.4, same_scale=True)
    return m


def potted_succulent():
    m = Model(tex(("pot", "nature/pot_mint"), ("leaves", "nature/succulent_side"), ("top", "nature/succulent_top"), ("soil", "soil")))
    m.disc("pot", 3.4, 0, 4.6, "pot")
    m.ring("rim", 3.9, 3.0, 4.6, 5.6, "pot")
    m.disc("soil", 3.1, 4.4, 5.2, "soil", top="soil")
    m.disc("rosette0", 3.6, 5.0, 6.4, "leaves", top="top")
    m.disc("rosette1", 2.6, 6.4, 7.6, "leaves", top="top")
    m.disc("rosette2", 1.4, 7.6, 8.6, "leaves", top="top")
    m.wrap("pot", 0, 5.6)
    m.wrap("leaves", 5.0, 8.6)
    for e in m.data["elements"]:
        f = e["faces"].get("up")
        if f and f.get("texture") == "#top":
            r = max(e["to"][0] - e["from"][0], e["to"][2] - e["from"][2]) / 2
            hw, hd = (e["to"][0] - e["from"][0]) / 2 * 8 / r, (e["to"][2] - e["from"][2]) / 2 * 8 / r
            # smaller tiers show the inner part of the rosette
            k = r / 3.6
            f["uv"] = [round(8 - hw * k, 3), round(8 - hd * k, 3), round(8 + hw * k, 3), round(8 + hd * k, 3)]
    return m


def potted_fern():
    m = Model(tex(("pot", "tulip_pot_wrap"), ("soil", "soil"), ("fern", "nature/fern")))
    m.cone("pot", 3.4, 4.6, 0, 6, "pot", steps=3)
    m.ring("rim", 5.0, 3.9, 5.8, 7.2, "pot")
    m.disc("soil", 4.0, 5.6, 6.5, "soil", top="soil")
    m.wrap("pot", 0, 7.2)
    rot = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    m.box("fern_a", (0.5, 5, 8), (15.5, 16, 8), "fern", faces=["north", "south"], rotation=rot)
    m.box("fern_b", (8, 5, 0.5), (8, 16, 15.5), "fern", faces=["east", "west"], rotation=rot)
    return m


def bird_bath():
    m = Model(tex(("stone", "nature/stone"), ("water", "nature/water"), ("bird", "nature/bird"), ("beak", "metal_gold")))
    m.disc("base", 4.2, 0, 1.2, "stone")
    m.disc("pillar", 1.6, 1.2, 8.8, "stone")
    m.disc("bowl", 6.2, 8.8, 10.4, "stone")
    m.ring("lip", 6.6, 5.2, 10.4, 11.4, "stone")
    m.disc("water", 5.4, 10.2, 10.9, "stone", top="water")
    top_full(m, "water", 5.4)
    # a little bluebird perched on the rim
    m.box("bird_body", (10.6, 11.4, 6.6), (13.0, 13.4, 9.4), "bird")
    m.box("bird_head", (10.2, 13.0, 7.0), (11.8, 14.6, 8.6), "bird")
    m.box("bird_beak", (9.6, 13.6, 7.6), (10.2, 14.0, 8.0), "beak")
    m.box("bird_tail", (12.8, 12.6, 7.4), (14.2, 13.2, 8.6), "bird")
    return m


def wildflower_patch():
    m = Model(tex(("top", "nature/wildflowers_top"), ("side", "nature/wildflowers_side")))
    m.box("patch", (0, 0.1, 0), (16, 0.1, 16), "top", faces=["up"])
    rot = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    m.box("flowers_a", (2, 0, 8), (14, 6, 8), "side", faces=["north", "south"], rotation=rot)
    m.box("flowers_b", (8, 0, 2), (8, 6, 14), "side", faces=["east", "west"], rotation=rot)
    for e in m.data["elements"]:
        for f in e["faces"].values():
            f["uv"] = [0, 0, 16, 16] if e["name"] == "patch" else [0, 6.4, 16, 16]
    return m


def hanging_ivy():
    m = Model(tex(("ivy", "nature/ivy")))
    m.box("ivy", (0, 0, 15.6), (16, 16, 15.6), "ivy", faces=["north", "south"])
    for e in m.data["elements"]:
        for f in e["faces"].values():
            f["uv"] = [0, 0, 16, 16]
    return m


def stepping_stones():
    m = Model(tex(("stone", "nature/stone")))
    for i, (cx, cz, r) in enumerate(((4.5, 4.5, 3.2), (11.0, 7.5, 3.6), (5.5, 12.0, 2.8))):
        m.disc(f"stone{i}", r, 0, 1.0 + i * 0.15, "stone", cx=cx, cz=cz)
    return m


def stump_stool():
    m = Model(tex(("bark", "nature/bark_wrap"), ("top", "nature/stump_top"), ("cushion", "cushion")))
    m.disc("stump", 5.0, 0, 7.4, "bark", top="top")
    m.disc("root", 5.6, 0, 1.2, "bark")
    m.disc("cushion", 4.2, 7.4, 8.8, "cushion")
    m.wrap("bark", 0, 7.4, plain=(0, 13, 4, 15))
    top_full(m, "top", 5.0)
    return m


MODELS = {"mushroom_lamp": mushroom_lamp, "potted_succulent": potted_succulent, "potted_fern": potted_fern, "bird_bath": bird_bath,
          "wildflower_patch": wildflower_patch, "hanging_ivy": hanging_ivy, "stepping_stones": stepping_stones, "stump_stool": stump_stool}


def build():
    textures = {"mushroom_cap": mushroom_cap(), "mushroom_cap_top": mushroom_cap_top(), "mushroom_gills": mushroom_gills(), "mushroom_stem": mushroom_stem(),
                "succulent_top": succulent_top(), "succulent_side": succulent_side(), "pot_mint": pot_mint(), "fern": fern(),
                "wildflowers_top": wildflowers_top(), "wildflowers_side": wildflowers_side(), "ivy": ivy(), "stone": stone(),
                "water": water(), "bird": bird(), "bark_wrap": bark_wrap(), "stump_top": stump_top()}
    os.makedirs(os.path.join(TEX, "nature"), exist_ok=True)
    for name, img in textures.items():
        img.save(os.path.join(TEX, "nature", name + ".png"))
    for name, make in MODELS.items():
        make().save(os.path.join(A, "models/block", name + ".json"))
        variants = {f"facing={f}": {"model": f"bakery:block/{name}", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
        json.dump({"variants": variants}, open(os.path.join(A, "blockstates", name + ".json"), "w"), indent="\t")
        item = {"parent": f"bakery:block/{name}", "display": BLOCK_DISPLAY}
        if name == "hanging_ivy":  # a flat wall plant reads best as a flat icon
            item = {"parent": "minecraft:item/generated", "textures": {"layer0": "bakery:block/nature/ivy"}}
        json.dump(item, open(os.path.join(A, "models/item", name + ".json"), "w"), indent="\t")
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"bakery:{name}"}],
                                                        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
        json.dump(loot, open(os.path.join(ROOT, "data/bakery/loot_table/blocks", name + ".json"), "w"), indent="\t")
    return len(MODELS)


if __name__ == "__main__":
    print(build(), "nature decorations written")
