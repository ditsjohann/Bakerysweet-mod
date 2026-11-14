"""Round, softer models for the kitchen machines and pots (jam pot, juicer, millstone, catalog stand, tulip pots)."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from round_model import Model

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/models/block")


def path(name):
    return os.path.join(OUT, name + ".json")


# ---- Jam pot: a hammered copper pot with a rolled lip and two wooden-gripped handles
JAM_TEX = {"pot": "bakery:block/copper_pot", "jam": "bakery:block/jam_surface", "wood": "bakery:block/churn_wood",
           "particle": "bakery:block/copper_pot"}


def jam_pot(spoon=False):
    m = Model(JAM_TEX)
    m.disc("base", 4.6, 0, 0.8, "pot")
    m.ring("wall_low", 5.6, 4.7, 0.6, 2.4, "pot")
    m.ring("wall_belly", 6.1, 5.1, 2.2, 6.4, "pot")
    m.ring("wall_neck", 5.8, 5.0, 6.2, 7.6, "pot")
    m.ring("lip", 6.4, 5.0, 7.5, 8.4, "pot")
    m.disc("floor", 5.0, 0.6, 1.0, "pot")
    for side, x1, x2 in (("w", 0.8, 2.4), ("e", 13.6, 15.2)):
        m.box(f"handle_{side}_arm", (min(x1, x2) - (0 if side == "w" else 0), 6.6, 7.4), (max(x1, x2), 7.2, 8.6), "pot")
        m.box(f"handle_{side}_grip", (x1 + (0 if side == "e" else -0.3), 6.2, 6.6), (x2 + (0.3 if side == "e" else 0), 7.6, 9.4), "wood")
    if spoon:
        m.box("spoon", (9, 3, 7.5), (10, 13, 8.5), "wood", rotation={"angle": -22.5, "axis": "z", "origin": [9.5, 3, 8]})
        m.box("spoon_head", (8.5, 2, 7), (10.5, 3.5, 9), "wood", rotation={"angle": -22.5, "axis": "z", "origin": [9.5, 3, 8]})
    return m


for name in ("jam_pot_static", "jam_pot_0", "jam_pot_1", "jam_pot_2", "jam_pot_3"):
    jam_pot().save(path(name))
jam_pot(spoon=True).save(path("jam_pot"))

# ---- Juicer: a round mint base, a glass jug with a handle, a ribbed metal reamer
JUICE_TEX = {"body": "bakery:block/juicer_body", "glass": "bakery:block/juicer_glass", "metal": "bakery:block/metal",
             "juice": "bakery:block/juice_liquid", "brass": "bakery:block/brass", "particle": "bakery:block/juicer_body"}


def juicer():
    m = Model(JUICE_TEX)
    m.disc("foot", 5.6, 0, 0.8, "body")
    m.disc("base", 5.2, 0.8, 2.6, "body")
    m.ring("base_trim", 5.5, 4.8, 2.4, 3.0, "brass")
    m.disc("jug_floor", 3.7, 2.9, 3.1, "glass")
    m.ring("jug", 4.1, 3.6, 3.0, 10.0, "glass")
    m.box("jug_handle_top", (11.9, 8.2, 7.4), (13.6, 8.9, 8.6), "glass")
    m.box("jug_handle", (12.9, 4.6, 7.4), (13.6, 8.9, 8.6), "glass")
    m.box("jug_handle_bot", (11.9, 4.6, 7.4), (13.6, 5.3, 8.6), "glass")
    m.box("spout", (7.3, 8.6, 2.9), (8.7, 9.4, 4.4), "glass")
    m.disc("plate", 4.4, 10.0, 10.8, "metal")
    m.ring("plate_rim", 4.6, 3.9, 10.6, 11.4, "metal")
    m.cone("reamer", 2.1, 0.5, 10.8, 13.8, "metal", steps=4)
    m.disc("reamer_tip", 0.4, 13.8, 14.2, "metal")
    m.box("lever_post", (11.0, 9.6, 7.5), (12.2, 11.4, 8.5), "brass")
    return m


for name in ("juicer", "juicer_0", "juicer_1", "juicer_2", "juicer_3"):
    juicer().save(path(name))

# ---- Grand Millstone: a round bed stone on a round plinth, with stone posts (the runner stone is its own part)
MILL_TEX = {"stone": "bakery:block/mill/stone_bricks", "mill": "bakery:block/mill/millstone", "wood": "bakery:block/mill/wood",
            "particle": "bakery:block/mill/stone_bricks"}


def mill_static():
    m = Model(MILL_TEX)
    m.box("plinth", (-16, -16, -16), (32, 0, 32), "stone")
    m.disc("bed", 17, 0, 3.5, "mill", top="mill")
    m.ring("bed_rim", 18, 15.5, 3.0, 5.0, "mill")
    m.box("spout", (5.5, 1.5, -16), (10.5, 3.5, -11), "wood")
    m.box("spout_lip", (5.5, 1.5, -16), (6.2, 4.5, -11), "wood")
    m.box("spout_lip2", (9.8, 1.5, -16), (10.5, 4.5, -11), "wood")
    for name, x, z in (("post_w", -12, 8), ("post_e", 28, 8), ("post_s", 8, 28)):
        m.disc(name, 2.2, 0, 13, "stone", cx=x, cz=z)
        m.disc(name + "_cap", 2.6, 13, 14, "stone", cx=x, cz=z)
    return m


def mill_runner():
    m = Model(MILL_TEX)
    m.disc("runner", 13, 5, 10, "mill", top="mill")
    m.ring("runner_band", 13.3, 12.5, 6.8, 8.2, "wood")
    m.disc("eye", 2.6, 10, 10.6, "wood")
    m.disc("axle", 2.0, 10, 22, "wood")
    m.box("crossbar", (-12, 19, 6.5), (28, 21, 9.5), "wood")
    m.disc("handle_w", 1.2, 12, 19, "wood", cx=-11, cz=8)
    m.disc("handle_e", 1.2, 12, 19, "wood", cx=27, cz=8)
    return m


mill_static().save(path("grand_millstone_static"))
mill_static().save(path("grand_millstone"))
mill_runner().save(path("part/millstone_runner"))

# (The Bakery Catalog has its own script: gen_catalog.py)

# ---- Tulip pot: a round terracotta-peach pot with a rolled rim, soil, and the tulip bunch
POT_TEX = {"pot": "bakery:block/tulip_pot_wrap", "soil": "bakery:block/soil", "flowers": "bakery:block/tulips",
           "particle": "bakery:block/flower_pot_peach"}


def tulip_pot():
    m = Model(POT_TEX)
    m.cone("body", 3.2, 4.4, 0, 6, "pot", steps=3)
    m.ring("rim", 5.0, 3.9, 5.8, 7.2, "pot")
    m.disc("soil", 4.0, 5.6, 6.5, "soil", top="soil")
    m.box("flowers_a", (1.5, 6, 8), (14.5, 16, 8), "flowers", faces=["north", "south"], rotation={"angle": 45, "axis": "y", "origin": [8, 8, 8]})
    m.box("flowers_b", (8, 6, 1.5), (8, 16, 14.5), "flowers", faces=["east", "west"], rotation={"angle": 45, "axis": "y", "origin": [8, 8, 8]})
    return m


def wrap_uvs(m, tex, y_top):
    """Wraps a texture round a turned (round) shape: each side face takes the texture column for the direction it
    faces (16 columns = 16 sides) and the rows for its height (row 0 at the top). Painted patterns then go all the
    way round instead of every side showing the same middle strip."""
    import math as _m
    normals = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    for e in m.data["elements"]:
        rot = e.get("rotation") or {}
        ang = _m.radians(rot.get("angle", 0)) if rot.get("axis") == "y" else 0
        for fname, f in e["faces"].items():
            if f.get("texture") != "#" + tex or fname not in normals:
                continue
            nx, nz = normals[fname]
            # Minecraft turns elements clockwise (seen from above) for positive angles.
            rx, rz = nx * _m.cos(ang) + nz * _m.sin(ang), -nx * _m.sin(ang) + nz * _m.cos(ang)
            col = round((_m.degrees(_m.atan2(rz, rx)) % 360) / 22.5) % 16
            y1, y2 = e["from"][1], e["to"][1]
            v1 = max(0.0, min(16.0, (y_top - y2) / y_top * 16))
            v2 = max(0.0, min(16.0, (y_top - y1) / y_top * 16))
            f["uv"] = [col, round(v1, 3), col + 1, round(max(v2, v1 + 0.5), 3)]
    return m


def fix_flowers(m):
    # Flat cross planes need full-texture uvs.
    for e in m.data["elements"]:
        if e["name"].startswith("flowers"):
            for f in e["faces"].values():
                f["uv"] = [0, 0, 16, 16]
    return m


fix_flowers(wrap_uvs(tulip_pot(), "pot", 7.2)).save(path("tulip_pot"))

VASE_TEX = {"vase": "bakery:block/tulip_vase_wrap", "flowers": "bakery:block/tulips", "particle": "bakery:block/ceramic_pink"}


def tulip_vase():
    m = Model(VASE_TEX)
    m.disc("foot", 2.6, 0, 0.8, "vase")
    m.disc("belly_low", 3.2, 0.8, 2.4, "vase")
    m.disc("belly", 3.6, 2.4, 5.0, "vase")
    m.disc("shoulder", 3.0, 5.0, 6.4, "vase")
    m.disc("neck", 1.9, 6.4, 8.6, "vase")
    m.ring("lip", 2.5, 1.6, 8.6, 9.4, "vase", segments=8)
    m.box("flowers_a", (2.5, 8, 8), (13.5, 16, 8), "flowers", faces=["north", "south"], rotation={"angle": 45, "axis": "y", "origin": [8, 8, 8]})
    m.box("flowers_b", (8, 8, 2.5), (8, 16, 13.5), "flowers", faces=["east", "west"], rotation={"angle": 45, "axis": "y", "origin": [8, 8, 8]})
    return m


fix_flowers(wrap_uvs(tulip_vase(), "vase", 9.4)).save(path("tulip_vase"))
print("kitchen models written")
