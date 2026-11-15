"""
Café cats: the round cushioned cat bed and the wicker kitten basket (with a kitten peeking out, and empty).
Run: python3 blockbench/gen_pets.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import put, canvas, hx, TEX
from round_model import Model, BLOCK_DISPLAY

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")
D = os.path.join(ROOT, "data/bakery")
ITEM = os.path.join(A, "textures/item")


def dump(data, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


def blob(img, x, y, rows, cols):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in cols:
                put(img, x + dx, y + dy, cols[ch])


# ------------------------------------------------------------------ bed art

def bed_rim():
    """The padded rim, unrolled: soft pink with a lighter rolled top and a calm stitched line."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = "#ffd2de" if y < 4 else "#f7b4c8" if y < 13 else "#e898b0"
            if y == 8 and x % 3 == 0:
                c = "#ffe4ec"
            put(img, x, y, hx(c))
    return img


def bed_cushion():
    """The cushion seen from above: cream, a soft pink heart in the middle."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#fff4e8") if (x + y) % 9 else hx("#f6e6d4"))
    blob(img, 5, 5, [".pp.pp.", "pPpppdp", "pppppdp", ".ppppd.", "..ppd..", "...d..."],
         {"p": hx("#ffc0d4"), "P": hx("#ffe0ea"), "d": hx("#f0a0bc")})
    return img


def cat_bed_model():
    m = Model({"rim": "bakery:block/pets/bed_rim", "cushion": "bakery:block/pets/bed_cushion"})
    m.disc("base", 6.6, 0, 1.4, "rim")
    m.ring("rim", 7.0, 4.8, 1.2, 4.6, "rim", top="rim")
    m.disc("cushion", 5.0, 1.2, 2.4, "rim", top="cushion")
    m.wrap("rim", 0, 4.6, plain=(4, 1, 8, 3))
    # the cushion top shows the whole picture
    for e in m.data["elements"]:
        up = e["faces"].get("up")
        if up and up.get("texture") == "#cushion":
            hw = (e["to"][0] - e["from"][0]) / 2 * 8 / 5.0
            hd = (e["to"][2] - e["from"][2]) / 2 * 8 / 5.0
            up["uv"] = [round(8 - hw, 3), round(8 - hd, 3), round(8 + hw, 3), round(8 + hd, 3)]
    return m


# ------------------------------------------------------------------ basket art

WICKER = {"w": hx("#d8a868"), "W": hx("#ecc488"), "k": hx("#b07e48"), "K": hx("#8a5e34"), "b": hx("#f48aaa"), "B": hx("#ffc0d4")}


def basket(kitten):
    img = canvas()
    # handle with a pink bow
    blob(img, 3, 1, ["..kkkkkkk..", ".k.......k.", "k.........k", "k.........k"], WICKER)
    blob(img, 6, 0, ["BbB.", ".b..", "B.B."], WICKER)
    if kitten:
        cat = {"g": hx("#b8b4c0"), "G": hx("#d8d4de"), "d": hx("#8e8a98"), "p": hx("#f6a8c0"), "e": hx("#3a3442"), "n": hx("#f08aa8"),
               "w": hx("#ffffff")}
        blob(img, 4, 3, [
            "g.....g.",
            "gp...pg.",
            "gGGGGGgd",
            "GGeGGeGd",
            "GwGnGwGd",
            ".GGGGGd.",
        ], cat)
    else:
        blob(img, 4, 6, ["..BBBB..", ".BBBBBB."], WICKER)
    # the weave: rows of alternating light and dark
    for y in range(8, 15):
        for x in range(2, 14):
            if (y == 14 and x in (2, 13)):
                continue
            put(img, x, y, WICKER["W"] if (x + (y // 2)) % 2 else WICKER["w"])
        put(img, 2, y, WICKER["k"])
        put(img, 13, y, WICKER["K"])
    for x in range(2, 14):
        put(img, x, 8, WICKER["k"])
        put(img, x, 15, WICKER["K"])
    return img


def build():
    pets = os.path.join(TEX, "pets")
    os.makedirs(pets, exist_ok=True)
    bed_rim().save(os.path.join(pets, "bed_rim.png"))
    bed_cushion().save(os.path.join(pets, "bed_cushion.png"))
    cat_bed_model().save(os.path.join(A, "models/block/cat_bed.json"))
    variants = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for area in range(5):
            variants[f"area={area},facing={f}"] = {"model": "bakery:block/cat_bed", "y": y}
    dump({"variants": variants}, os.path.join(A, "blockstates/cat_bed.json"))
    dump({"parent": "bakery:block/cat_bed", "display": BLOCK_DISPLAY}, os.path.join(A, "models/item/cat_bed.json"))
    dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "bakery:cat_bed"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
         os.path.join(D, "loot_table/blocks/cat_bed.json"))
    basket(True).save(os.path.join(ITEM, "kitten_basket.png"))
    basket(False).save(os.path.join(ITEM, "kitten_basket_empty.png"))
    dump({"parent": "minecraft:item/generated", "textures": {"layer0": "bakery:item/kitten_basket"},
          "overrides": [{"predicate": {"bakery:empty": 1}, "model": "bakery:item/kitten_basket_empty"}]},
         os.path.join(A, "models/item/kitten_basket.json"))
    dump({"parent": "minecraft:item/generated", "textures": {"layer0": "bakery:item/kitten_basket_empty"}},
         os.path.join(A, "models/item/kitten_basket_empty.json"))
    return 2


if __name__ == "__main__":
    print(build(), "pet pieces written")
