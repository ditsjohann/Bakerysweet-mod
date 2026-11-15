"""
The Café Jukebox: a pastel arched jukebox with a record window, a speaker grille and light tubes that glow
brighter while it plays. Run: python3 blockbench/gen_jukebox.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods_unique import put, canvas, hx, TEX
from round_model import BLOCK_DISPLAY

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")
D = os.path.join(ROOT, "data/bakery")
MINT = "#a8e2cc"


def dump(data, path):
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


def tone(c, f):
    c = hx(c)
    return tuple(min(255, int(v + (255 - v) * (f - 1))) for v in c) if f >= 1 else tuple(int(v * f) for v in c)


def body():
    img = canvas()
    for y in range(16):
        for x in range(16):
            c = tone(MINT, 1.15) if y < 2 else hx(MINT) if y < 13 else tone(MINT, 0.9)
            put(img, x, y, c)
    return img


def front():
    """Record window at the top (a pink record on cream), chrome buttons, a calm speaker grille below."""
    img = body()
    for y in range(1, 8):
        for x in range(3, 13):
            put(img, x, y, hx("#fff6ea"))
    for (x, y) in ((6, 3), (7, 3), (8, 3), (9, 3), (5, 4), (10, 4), (5, 5), (10, 5), (6, 6), (7, 6), (8, 6), (9, 6)):
        put(img, x, y, hx("#f48aaa"))
    for (x, y) in ((6, 4), (9, 4), (6, 5), (9, 5)):
        put(img, x, y, hx("#ffc0d4"))
    for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5)):
        put(img, x, y, hx("#ffffff"))
    for x in (4, 6, 8, 10):
        put(img, x + 1, 9, hx("#ffffff"))
    for y in range(11, 15):
        for x in range(3, 13):
            put(img, x, y, hx("#7cc0a8") if (x + y) % 2 == 0 else hx("#8ccfb6"))
    return img


def lights(on):
    """A light tube: pastel colours along it, brighter (with a white core) when playing."""
    img = canvas()
    cols = ["#ffb0c8", "#ffe08a", "#a8e6cf", "#cdb4f0"]
    for y in range(16):
        c = cols[(y // 4) % 4]
        for x in range(16):
            put(img, x, y, tone(c, 1.25) if on else tone(c, 0.85))
        if on:
            for x in (7, 8):
                put(img, x, y, hx("#ffffff"))
    return img


def wood():
    img = canvas()
    for y in range(16):
        for x in range(16):
            put(img, x, y, hx("#c88a68") if y % 4 else hx("#b07656"))
    return img


def el(name, a, b, tex, which="nsewud", front=None):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    return {"name": name, "from": list(a), "to": list(b),
            "faces": {names[c]: {"texture": "#" + (front if c == "n" and front else tex)} for c in which}}


def model(on):
    t = {"body": "bakery:block/jukebox/body", "front": "bakery:block/jukebox/front", "wood": "bakery:block/jukebox/wood",
         "lights": f"bakery:block/jukebox/lights_{'on' if on else 'off'}", "particle": "bakery:block/jukebox/body"}
    els = [
        el("plinth", (2, 0, 3), (14, 2, 13), "wood"),
        el("body", (2.5, 2, 3.5), (13.5, 12, 12.5), "body", front="front"),
        el("arch0", (3, 12, 3.5), (13, 13.5, 12.5), "body"),
        el("arch1", (4, 13.5, 3.5), (12, 14.6, 12.5), "body"),
        el("arch2", (5.5, 14.6, 3.5), (10.5, 15.4, 12.5), "body"),
        el("tube_l", (1.5, 2, 3), (2.6, 12.4, 4.2), "lights"),
        el("tube_r", (13.4, 2, 3), (14.5, 12.4, 4.2), "lights"),
        el("tube_t0", (2.4, 12, 3), (4.2, 13.6, 4.2), "lights"),
        el("tube_t1", (11.8, 12, 3), (13.6, 13.6, 4.2), "lights"),
        el("tube_t2", (3.8, 13.4, 3), (5.8, 14.7, 4.2), "lights"),
        el("tube_t3", (10.2, 13.4, 3), (12.2, 14.7, 4.2), "lights"),
        el("tube_t4", (5.4, 14.5, 3), (10.6, 15.6, 4.2), "lights"),
    ]
    # the front panel's uv: map the 11 x 10 face onto the whole picture
    els[1]["faces"]["north"]["uv"] = [0, 0, 16, 16]
    return {"parent": "block/block", "ambientocclusion": False, "textures": t, "elements": els}


def build():
    out = os.path.join(TEX, "jukebox")
    os.makedirs(out, exist_ok=True)
    for name, img in {"body": body(), "front": front(), "wood": wood(), "lights_on": lights(True), "lights_off": lights(False)}.items():
        img.save(os.path.join(out, name + ".png"))
    dump(model(False), os.path.join(A, "models/block/cafe_jukebox.json"))
    dump(model(True), os.path.join(A, "models/block/cafe_jukebox_on.json"))
    variants = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for on in (False, True):
            variants[f"facing={f},playing={str(on).lower()}"] = {"model": "bakery:block/cafe_jukebox" + ("_on" if on else ""), "y": y}
    dump({"variants": variants}, os.path.join(A, "blockstates/cafe_jukebox.json"))
    dump({"parent": "bakery:block/cafe_jukebox_on", "display": BLOCK_DISPLAY}, os.path.join(A, "models/item/cafe_jukebox.json"))
    dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "bakery:cafe_jukebox"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
         os.path.join(D, "loot_table/blocks/cafe_jukebox.json"))
    return 1


if __name__ == "__main__":
    print(build(), "jukebox written")
