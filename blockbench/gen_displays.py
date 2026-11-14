"""
Display furniture that holds real bakes (customers buy straight from it, like the display case):

    tiered_stand   three round pink plates on a white pole, five bakes
    pastry_shelf   a cream wooden shelf with two rows of three
    glass_cloche   a white plate under a glass dome, for one show-piece

Writes block + item models, blockstates and loot tables. Run: python3 blockbench/gen_displays.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from round_model import Model

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = os.path.join(ROOT, "assets/bakery")


def tiered_stand():
    m = Model(textures={"plate": "bakery:block/ceramic_pink", "pole": "bakery:block/metal_white", "gold": "bakery:block/metal_gold",
                        "particle": "bakery:block/ceramic_pink"})
    m.disc("foot", r=3.5, y1=0, y2=1, tex="pole")
    m.disc("pole", r=0.75, y1=1, y2=12.5, tex="pole")
    m.disc("plate_low", r=7, y1=1, y2=1.75, tex="plate")
    m.ring("lip_low", r_out=7, r_in=6.3, y1=1.75, y2=2.25, tex="plate")
    m.disc("plate_mid", r=5.25, y1=6, y2=6.75, tex="plate")
    m.ring("lip_mid", r_out=5.25, r_in=4.6, y1=6.75, y2=7.25, tex="plate")
    m.disc("plate_top", r=3.5, y1=11, y2=11.75, tex="plate")
    m.disc("knob", r=1, y1=12.5, y2=14, tex="gold")
    return m


def pastry_shelf():
    m = Model(textures={"wood": "bakery:block/wood_cream", "trim": "bakery:block/wood_pink", "particle": "bakery:block/wood_cream"})
    m.box("left", (0, 0, 4), (1.5, 15, 16), tex="wood")
    m.box("right", (14.5, 0, 4), (16, 15, 16), tex="wood")
    m.box("back", (1.5, 0, 14.5), (14.5, 15, 16), tex="trim")
    m.box("bottom", (1.5, 0, 4), (14.5, 1, 14.5), tex="wood")
    m.box("middle", (1.5, 7, 4), (14.5, 8, 14.5), tex="wood")
    m.box("top", (0, 15, 3.5), (16, 16, 16), tex="trim")
    m.box("lip_low", (1.5, 1, 4), (14.5, 1.75, 4.75), tex="trim")
    m.box("lip_mid", (1.5, 8, 4), (14.5, 8.75, 4.75), tex="trim")
    return m


def glass_cloche():
    m = Model(textures={"plate": "bakery:block/ceramic_white", "glass": "bakery:block/case_glass", "gold": "bakery:block/metal_gold",
                        "particle": "bakery:block/ceramic_white"})
    m.disc("plate", r=7, y1=0, y2=1, tex="plate")
    m.ring("rim", r_out=7, r_in=6.4, y1=1, y2=1.5, tex="plate")
    m.disc("dome_low", r=6, y1=1, y2=7, tex="glass", top=None)
    m.disc("dome_mid", r=5, y1=7, y2=9.5, tex="glass")
    m.disc("dome_top", r=3.2, y1=9.5, y2=11, tex="glass")
    m.disc("knob", r=1, y1=11, y2=12.5, tex="gold")
    return m


def blockstate(name):
    return {"variants": {f"facing={f}": {"model": f"bakery:block/{name}", "y": y}
                         for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}


def build():
    for name, make in (("tiered_stand", tiered_stand), ("pastry_shelf", pastry_shelf), ("glass_cloche", glass_cloche)):
        make().save(os.path.join(A, "models/block", name + ".json"))
        json.dump(blockstate(name), open(os.path.join(A, "blockstates", name + ".json"), "w"), indent="\t")
        json.dump({"parent": f"bakery:block/{name}"}, open(os.path.join(A, "models/item", name + ".json"), "w"), indent="\t")
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"bakery:{name}"}],
                                                        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
        json.dump(loot, open(os.path.join(ROOT, "data/bakery/loot_table/blocks", name + ".json"), "w"), indent="\t")
    return 3


if __name__ == "__main__":
    print(build(), "displays written")
