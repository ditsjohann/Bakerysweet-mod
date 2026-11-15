"""
The coffee corner: coffee and cinnamon crops, beans, syrups, coffee and latte mugs, the espresso machine and the
syrup rack. Calm 16x16 art in the same hand-drawn style as the rest (crops like the saplings).

Writes textures, block/item models, blockstates, loot tables and recipes.
Run: python3 blockbench/gen_coffee.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from gen_woods_unique import put, canvas, hx, TEX
from gen_crops16 import bush, DEEP, GREEN
from round_model import BLOCK_DISPLAY

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")
D = os.path.join(ROOT, "data/bakery")
ITEM = os.path.join(A, "textures/item")
SYRUPS = [("caramel", "#d9924a"), ("mocha", "#7a4a32"), ("speculoos", "#c07a3e"), ("pumpkin_spice", "#e8843a"), ("vanilla", "#f4e2b0")]


def tone(c, f):
    c = hx(c) if isinstance(c, str) else c
    if f >= 1:
        return tuple(min(255, int(v + (255 - v) * (f - 1))) for v in c[:3])
    return tuple(int(v * f) for v in c[:3])


def blob(img, x, y, rows, cols):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in cols:
                put(img, x + dx, y + dy, cols[ch])


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def dump(data, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


# ------------------------------------------------------------------ item art

BEAN = [".lm.", "lmmd", "mcmd", "mcdd", ".dd."]


def beans(light, mid, dark, crease):
    img = canvas()
    cols = {"l": hx(light), "m": hx(mid), "d": hx(dark), "c": hx(crease)}
    for (x, y) in ((2, 8), (8, 9), (5, 3)):
        blob(img, x, y, BEAN, cols)
    return img


def coffee_cherries():
    img = canvas()
    stalk, leaf = hx("#3e6a28"), (hx("#2e6a2c"), hx("#4a8c3a"), hx("#6aaa4c"))
    for (x, y) in ((8, 2), (8, 3), (7, 4), (9, 4)):
        put(img, x, y, stalk)
    blob(img, 9, 1, ["ll", ".lm", "..mm"], {"l": leaf[2], "m": leaf[1]})
    cols = {"l": hx("#ff7a80"), "m": hx("#d8304a"), "d": hx("#a01e34"), "w": hx("#ffd0d4")}
    for (x, y) in ((3, 5), (8, 6), (5, 10)):
        blob(img, x, y, [".lmm.", "lwmmd", "mmmmd", "mmmdd", ".ddd."], cols)
    return img


def cinnamon():
    """Two rolled cinnamon sticks, crossed; the cut ends show the curl."""
    img = canvas()
    bark, lit, dark, curl = hx("#b06a3a"), hx("#cc8650"), hx("#86482a"), hx("#e8b07a")
    for (x0, y0) in ((2, 11), (4, 13)):
        for i in range(10):
            x, y = x0 + i, y0 - i
            put(img, x, y, lit); put(img, x + 1, y, bark); put(img, x + 1, y + 1, dark)
        put(img, x0 + 10, y0 - 10, curl); put(img, x0 + 11, y0 - 10, dark)
        put(img, x0, y0 + 1, curl)
    return img


def seeds(c1, c2):
    img = canvas()
    for (x, y) in ((4, 6), (9, 5), (6, 10), (11, 10)):
        put(img, x, y, hx(c1)); put(img, x + 1, y, hx(c1)); put(img, x, y + 1, hx(c2)); put(img, x + 1, y + 1, hx(c2))
    return img


def syrup_bottle(liquid):
    """A syrup bottle with a white pump on top, a cream label and a soft highlight."""
    img = canvas()
    l, m, d = tone(liquid, 1.25), hx(liquid), tone(liquid, 0.72)
    cols = {"p": hx("#ffffff"), "q": hx("#dcdce6"), "n": hx("#e8eef4"), "g": hx("#c8d4dc"),
            "l": l, "m": m, "d": d, "c": hx("#fff6e6"), "k": hx("#f0dcc4"), "h": hx("#ffffff")}
    blob(img, 4, 0, [
        "..pppq..",
        "pppppq..",
        "p..pq...",
        "...nq...",
        "..nngg..",
        ".gnmmdg.",
        "glmmmmdg",
        "ghmmmmdg",
        "gcccccck",
        "gcckcckk",
        "gcccccck",
        "glmmmmdg",
        "glmmmddg",
        "gmmmmddg",
        ".gddddg.",
    ], cols)
    return img


def mug_drink(liquid_rows, drizzle=()):
    """Base mug with its top filled: liquid_rows maps (x, y) to colours; steam is copied from the matcha latte."""
    mug = Image.open(os.path.join(ITEM, "mug.png")).convert("RGBA")
    ref = Image.open(os.path.join(ITEM, "matcha_latte.png")).convert("RGBA")
    img = mug.copy()
    for y in range(0, 4):
        for x in range(16):
            if ref.getpixel((x, y)) != mug.getpixel((x, y)):
                img.putpixel((x, y), ref.getpixel((x, y)))
    for (x, y), c in liquid_rows.items():
        img.putpixel((x, y), hx(c) + (255,))
    layer = canvas()
    for (x, y) in drizzle:
        put(layer, x, y, (255, 255, 255))
    return img, layer


def coffee_mug():
    rows = {(x, 5): ("#8a5a3a" if x in (4, 5) else "#5a3826") for x in range(3, 10)}
    rows.update({(x, 6): "#3e2418" for x in range(3, 10)})
    return mug_drink(rows, drizzle=((5, 5), (6, 5), (7, 5)))


def latte_mug():
    rows = {(x, 5): "#f4e6d2" for x in range(3, 10)}
    rows.update({(x, 6): "#e8d2b4" for x in range(3, 10)})
    rows[(5, 5)] = rows[(7, 5)] = "#d6b48c"   # a tiny heart of latte art
    rows[(6, 6)] = "#d6b48c"
    return mug_drink(rows, drizzle=((4, 5), (8, 5), (6, 5)))


# ------------------------------------------------------------------ block art

def flat(c):
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx(c))
    return img


def enamel(base):
    """Soft enamel: a lit band near the top, gently darker toward the bottom and edges."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = tone(base, 1.12) if y in (1, 2) else hx(base) if y < 12 else tone(base, 0.93)
            if x in (0, 15):
                c = tone(base, 0.9)
            put(img, x, y, c)
    return img


def machine_front(base):
    """The front: a round pressure gauge and two little buttons on the pink enamel."""
    img = enamel(base)
    blob(img, 5, 2, [".www.", "wcccw", "wcnkw", "wcccw", ".www."],
         {"w": hx("#d8dce4"), "c": hx("#fffaf2"), "n": hx("#e05a7a"), "k": hx("#4a3a3a")})
    put(img, 4, 9, hx("#ffffff")); put(img, 5, 9, hx("#e6e6ee"))
    put(img, 10, 9, hx("#ffffff")); put(img, 11, 9, hx("#e6e6ee"))
    return img


def chrome():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#f2f4f8") if x % 8 in (2, 3) else hx("#d8dce4") if x % 8 < 6 else hx("#c2c8d2"))
    return img


def tray():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#5a5a66") if (x + y) % 2 else hx("#70707c"))
    return img


def hopper_glass():
    """Glass: a pale frame with a clear middle (beans show through)."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, hx("#e8f4fa"))
            elif x == 2 and 2 <= y <= 8:
                put(img, x, y, hx("#ffffff"), 160)
    return img


def beans_fill():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#5a3826") if (x // 2 + y // 2) % 2 else hx("#4a2c1c"))
            if (x % 4, y % 4) == (1, 1):
                put(img, x, y, hx("#7a5038"))
    return img


def milk():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#fffdf8") if (x + y) % 5 else hx("#f2eee6"))
    return img


def wood_handle():
    return flat("#6a4430")


def syrup_liquid(liquid):
    """Bottle side for the rack: liquid with a lit stripe, a cream label band in the middle."""
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = tone(liquid, 1.2) if x in (4, 5) else hx(liquid) if x < 11 else tone(liquid, 0.75)
            if 6 <= y <= 9:
                c = hx("#fff6e6") if not (y == 8 and x % 3 == 0) else tone(liquid, 0.9)
            put(img, x, y, c)
    return img


# ------------------------------------------------------------------ models

def el(name, a, b, faces):
    return {"name": name, "from": list(a), "to": list(b), "faces": faces}


def all_faces(tex, which="nsewud", top=None, front=None):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    out = {}
    for ch in which:
        t = tex
        if ch == "u" and top:
            t = top
        if ch == "n" and front:
            t = front
        out[names[ch]] = {"texture": "#" + t}
    return out


def machine_model():
    t = {"body": "bakery:block/coffee/machine_body", "front": "bakery:block/coffee/machine_front", "chrome": "bakery:block/coffee/chrome",
         "tray": "bakery:block/coffee/tray", "glass": "bakery:block/coffee/hopper_glass", "handle": "bakery:block/coffee/handle",
         "particle": "bakery:block/coffee/machine_body"}
    els = [
        el("body", (3, 0, 6), (13, 12, 14), all_faces("body", front="front")),
        el("lid", (2.5, 12, 5.5), (13.5, 13, 14.5), all_faces("chrome")),
        el("tray", (3, 0, 1.5), (13, 1, 6), all_faces("tray", "nsewu")),
        el("group_head", (6.5, 8, 3.5), (9.5, 10, 6), all_faces("chrome")),
        el("portafilter", (7, 7.2, 3.8), (9, 8, 5.7), all_faces("chrome")),
        el("handle", (9, 7.3, 4.3), (12.5, 7.9, 5.2), all_faces("handle")),
        el("wand", (11.6, 3.5, 4.4), (12.3, 9, 5.1), all_faces("chrome")),
        el("hopper", (6, 13, 8), (10, 16, 12), all_faces("glass", "nsewu")),
        el("hopper_cap", (6.5, 16, 8.5), (9.5, 16.6, 11.5), all_faces("chrome")),
    ]
    return {"parent": "block/block", "ambientocclusion": False, "textures": t, "elements": els}


def part_model(textures, elements):
    return {"parent": "block/block", "ambientocclusion": False, "textures": textures, "elements": elements}


def rack_model():
    t = {"wood": "bakery:block/citrus_planks", "particle": "bakery:block/citrus_planks"}
    return part_model(t, [el("tray", (0, 0, 4), (16, 1.5, 12), all_faces("wood")),
                          el("back", (0.5, 1.5, 4), (15.5, 4, 5), all_faces("wood")),
                          el("rail", (0.5, 1.5, 11), (15.5, 2.5, 12), all_faces("wood"))])


def bottle_model(flavor, slot):
    cx = 14 - slot * 4
    t = {"liquid": f"bakery:block/coffee/syrup_{flavor}", "white": "bakery:block/coffee/milk", "glass": "bakery:block/coffee/chrome",
         "particle": f"bakery:block/coffee/syrup_{flavor}"}
    return part_model(t, [
        el("bottle", (cx - 1.5, 1.5, 6.5), (cx + 1.5, 7.5, 9.5), all_faces("liquid", "nsewu")),
        el("neck", (cx - 0.7, 7.5, 7.3), (cx + 0.7, 8.4, 8.7), all_faces("glass", "nsewu")),
        el("pump", (cx - 0.6, 8.4, 7.4), (cx + 0.6, 10, 8.6), all_faces("white", "nsewu")),
        el("nozzle", (cx - 0.35, 9.3, 5.8), (cx + 0.35, 9.9, 7.4), all_faces("white", "nsewud")),
    ])


def crop_models(name, loot_item, seeds_item, drop=(2, 4)):
    tea = os.path.join(A, "models/block/tea_crop_stage0.json")
    base = json.load(open(tea))
    for s in range(4):
        m = json.loads(json.dumps(base))
        m["textures"] = {"plant": f"bakery:block/{name}_crop_stage{s}", "particle": f"bakery:block/{name}_crop_stage{s}"}
        dump(m, os.path.join(A, f"models/block/{name}_crop_stage{s}.json"))
    dump({"variants": {f"age={s}": {"model": f"bakery:block/{name}_crop_stage{s}"} for s in range(4)}},
         os.path.join(A, f"blockstates/{name}_crop.json"))
    loot = json.load(open(os.path.join(D, "loot_table/blocks/tea_crop.json")))
    text = json.dumps(loot).replace("bakery:tea_crop", f"bakery:{name}_crop").replace("bakery:tea_leaves", loot_item) \
        .replace("bakery:tea_seeds", seeds_item)
    loot = json.loads(text)
    dump(loot, os.path.join(D, f"loot_table/blocks/{name}_crop.json"))


def recipes():
    r = os.path.join(D, "recipe")
    for kind, time in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
        dump({"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": "bakery:coffee_cherries"},
              "result": {"id": "bakery:roasted_coffee_beans", "count": 1}, "experience": 0.3, "cookingtime": time},
             os.path.join(r, f"roasted_coffee_beans_{kind}.json"))
    syrup = {"caramel": ["minecraft:sugar", "minecraft:sugar", "minecraft:milk_bucket"],
             "mocha": ["minecraft:sugar", "minecraft:cocoa_beans"],
             "speculoos": ["minecraft:sugar", "bakery:cinnamon", "bakery:cookies"],
             "pumpkin_spice": ["minecraft:sugar", "bakery:cinnamon", "minecraft:pumpkin"],
             "vanilla": ["minecraft:sugar", "bakery:vanilla_pod"]}
    for name, parts in syrup.items():
        dump({"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": [{"item": "minecraft:glass_bottle"}] + [{"item": p} for p in parts],
              "result": {"id": f"bakery:{name}_syrup", "count": 1}}, os.path.join(r, f"{name}_syrup.json"))


def self_loot(name):
    dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"bakery:{name}"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
         os.path.join(D, f"loot_table/blocks/{name}.json"))


def build():
    out = os.path.join(TEX, "coffee")
    # crops
    for s in range(4):
        save(bush(s, DEEP, ("#d8304a", 2, ((-3.5, 7.5), (3.2, 6.8), (0, 9.6), (-5.4, 3.6), (5.2, 3.4), (-1.4, 4.4))), ("#ffffff", "#f6f0d0")),
             os.path.join(TEX, f"coffee_crop_stage{s}.png"))
        save(bush(s, GREEN, ("#b06a3a", 1, ((-3.5, 8.5), (3.2, 8.0), (0, 11), (-6, 5), (6, 5), (-1, 6))), ("#ffd8e0", "#e08aa8")),
             os.path.join(TEX, f"cinnamon_crop_stage{s}.png"))
    crop_models("coffee", "bakery:coffee_cherries", "bakery:coffee_seeds")
    crop_models("cinnamon", "bakery:cinnamon", "bakery:cinnamon_seeds")
    # items
    items = {"coffee_seeds": seeds("#c8d8a0", "#a0b47a"), "cinnamon_seeds": seeds("#a87a52", "#86603e"),
             "coffee_cherries": coffee_cherries(), "cinnamon": cinnamon(),
             "roasted_coffee_beans": beans("#8a5a3a", "#6a4028", "#4a2a1a", "#2e1810")}
    for name, liquid in SYRUPS:
        items[f"{name}_syrup"] = syrup_bottle(liquid)
    for name, img in items.items():
        save(img, os.path.join(ITEM, name + ".png"))
        dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"bakery:item/{name}"}}, os.path.join(A, f"models/item/{name}.json"))
    for name, (img, drizzle) in (("coffee", coffee_mug()), ("latte", latte_mug())):
        save(img, os.path.join(ITEM, name + ".png"))
        save(drizzle, os.path.join(ITEM, name + "_syrup.png"))
        dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"bakery:item/{name}", "layer1": f"bakery:item/{name}_syrup"}},
             os.path.join(A, f"models/item/{name}.json"))
    # machine
    pink = "#f6b8c8"
    for name, img in {"machine_body": enamel(pink), "machine_front": machine_front(pink), "chrome": chrome(), "tray": tray(),
                      "hopper_glass": hopper_glass(), "beans": beans_fill(), "milk": milk(), "handle": wood_handle()}.items():
        save(img, os.path.join(out, name + ".png"))
    for name, liquid in SYRUPS:
        save(syrup_liquid(liquid), os.path.join(out, f"syrup_{name}.png"))
    dump(machine_model(), os.path.join(A, "models/block/coffee_machine.json"))
    dump(part_model({"beans": "bakery:block/coffee/beans", "particle": "bakery:block/coffee/beans"},
                    [el("beans", (6.3, 13, 8.3), (9.7, 15.2, 11.7), all_faces("beans", "nsewu"))]),
         os.path.join(A, "models/block/coffee_machine_beans.json"))
    dump(part_model({"chrome": "bakery:block/coffee/chrome", "milk": "bakery:block/coffee/milk", "particle": "bakery:block/coffee/milk"},
                    [el("pitcher", (10.2, 1, 2), (12.8, 4.2, 4.6), all_faces("chrome", "nsewu", top="milk")),
                     el("spout", (10.8, 3.6, 1.6), (11.6, 4.2, 2), all_faces("chrome", "nsewu"))]),
         os.path.join(A, "models/block/coffee_machine_milk.json"))
    facings = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    parts = []
    for f, y in facings:
        parts.append({"when": {"facing": f}, "apply": {"model": "bakery:block/coffee_machine", "y": y}})
        parts.append({"when": {"facing": f, "level": "1|3"}, "apply": {"model": "bakery:block/coffee_machine_beans", "y": y}})
        parts.append({"when": {"facing": f, "level": "2|3"}, "apply": {"model": "bakery:block/coffee_machine_milk", "y": y}})
    dump({"multipart": parts}, os.path.join(A, "blockstates/coffee_machine.json"))
    dump({"parent": "bakery:block/coffee_machine", "display": BLOCK_DISPLAY}, os.path.join(A, "models/item/coffee_machine.json"))
    # rack
    dump(rack_model(), os.path.join(A, "models/block/syrup_rack.json"))
    parts = []
    for f, y in facings:
        parts.append({"when": {"facing": f}, "apply": {"model": "bakery:block/syrup_rack", "y": y}})
        for slot in range(4):
            for i, (name, _) in enumerate(SYRUPS):
                parts.append({"when": {"facing": f, f"slot{slot}": str(i + 1)}, "apply": {"model": f"bakery:block/syrup_rack_{name}_{slot}", "y": y}})
    for slot in range(4):
        for name, _ in SYRUPS:
            dump(bottle_model(name, slot), os.path.join(A, f"models/block/syrup_rack_{name}_{slot}.json"))
    dump({"multipart": parts}, os.path.join(A, "blockstates/syrup_rack.json"))
    # item: the rack with a few bottles on it
    item = rack_model()
    for slot, (name, _) in zip(range(4), SYRUPS):
        b = bottle_model(name, slot)
        item["textures"][f"liquid{slot}"] = b["textures"]["liquid"]
        item["textures"]["white"] = b["textures"]["white"]
        item["textures"]["glass"] = b["textures"]["glass"]
        for e in b["elements"]:
            for face in e["faces"].values():
                if face["texture"] == "#liquid":
                    face["texture"] = f"#liquid{slot}"
            item["elements"].append(e)
    item["display"] = BLOCK_DISPLAY
    dump(item, os.path.join(A, "models/item/syrup_rack.json"))
    self_loot("coffee_machine")
    self_loot("syrup_rack")
    recipes()
    # the placed mug shows coffee and lattes too
    mug = json.load(open(os.path.join(A, "blockstates/mug.json")))
    for part in mug["multipart"]:
        drinks = part["when"].get("drink")
        if drinks:
            names = drinks.split("|")
            for d in ("coffee", "latte"):
                if d not in names:
                    names.append(d)
            part["when"]["drink"] = "|".join(names)
    dump(mug, os.path.join(A, "blockstates/mug.json"))
    return len(items) + 2


if __name__ == "__main__":
    print(build(), "coffee items written")
