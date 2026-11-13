"""Pink couch: four pieces (single, left end, middle, right end) that join into one long sofa. Faces north."""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from round_model import Model
import json

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
TEX = {"velvet": "bakery:block/velvet_pink", "cushion": "bakery:block/cushion", "wood": "bakery:block/wood_cream",
       "gold": "bakery:block/metal_gold", "particle": "bakery:block/velvet_pink"}


def roll_x(m, name, x1, x2, cy, cz, r, tex):
    """A round bolster lying along x (octagon: a box plus the same box turned 45 degrees). The pieces' ends are
    staggered a hair so they never sit in the same plane (which flickers)."""
    s = 0.03
    m.box(name + "_a", (x1, cy - r, cz - r * 0.42), (x2, cy + r, cz + r * 0.42), tex)
    m.box(name + "_b", (x1 + s, cy - r * 0.42, cz - r), (x2 - s, cy + r * 0.42, cz + r), tex)
    m.box(name + "_c", (x1 + 2 * s, cy - r * 0.42, cz - r), (x2 - 2 * s, cy + r * 0.42, cz + r), tex,
          rotation={"angle": 45, "axis": "x", "origin": [x1, cy, cz]})
    m.box(name + "_d", (x1 + 3 * s, cy - r * 0.42, cz - r), (x2 - 3 * s, cy + r * 0.42, cz + r), tex,
          rotation={"angle": -45, "axis": "x", "origin": [x1, cy, cz]})


def roll_z(m, name, z1, z2, cx, cy, r, tex):
    """A round arm roll lying along z (ends staggered like roll_x)."""
    s = 0.03
    m.box(name + "_a", (cx - r * 0.42, cy - r, z1), (cx + r * 0.42, cy + r, z2), tex)
    m.box(name + "_b", (cx - r, cy - r * 0.42, z1 + s), (cx + r, cy + r * 0.42, z2 - s), tex)
    m.box(name + "_c", (cx - r, cy - r * 0.42, z1 + 2 * s), (cx + r, cy + r * 0.42, z2 - 2 * s), tex,
          rotation={"angle": 45, "axis": "z", "origin": [cx, cy, z1]})
    m.box(name + "_d", (cx - r, cy - r * 0.42, z1 + 3 * s), (cx + r, cy + r * 0.42, z2 - 3 * s), tex,
          rotation={"angle": -45, "axis": "z", "origin": [cx, cy, z1]})


def couch(part):
    left = part in ("single", "left")    # arm on the left (west) end
    right = part in ("single", "right")  # arm on the right (east) end
    m = Model(TEX)
    x1 = 2.6 if left else 0
    x2 = 13.4 if right else 16
    # Frame and seat
    m.box("base", (0 if not left else 0.6, 2, 2), (16 if not right else 15.4, 6, 15), "velvet")
    m.box("seat", (x1, 6, 2.4), (x2, 8.2, 12.2), "cushion", top="cushion")
    roll_x(m, "seat_front", x1, x2, 7.1, 2.6, 1.15, "cushion")
    # Back, its rounded top, and a plump back cushion
    m.box("back", (0 if not left else 0.6, 6, 12), (16 if not right else 15.4, 14.6, 15.2), "velvet")
    roll_x(m, "back_top", 0 if not left else 0.6, 16 if not right else 15.4, 14.6, 13.6, 1.6, "velvet")
    m.box("back_cushion", (x1 + 0.4, 8.2, 10.2), (x2 - 0.4, 14, 12.1), "cushion")
    roll_x(m, "back_cushion_top", x1 + 0.4, x2 - 0.4, 13.8, 11.15, 0.95, "cushion")
    # Tufting buttons
    for bx in [b for b in (4, 8, 12) if x1 + 1 < b < x2 - 1] or [8]:
        m.box(f"button_{bx}", (bx - 0.35, 11.1, 10.0), (bx + 0.35, 11.8, 10.25), "gold")
    # Arms with round rolls
    if left:
        m.box("arm_l", (0.4, 2, 1.8), (2.8, 10, 15), "velvet")
        roll_z(m, "arm_l_roll", 1.2, 15, 1.6, 10.2, 1.5, "velvet")
    if right:
        m.box("arm_r", (13.2, 2, 1.8), (15.6, 10, 15), "velvet")
        roll_z(m, "arm_r_roll", 1.2, 15, 14.4, 10.2, 1.5, "velvet")
    # Tapered cream legs at the ends; middles get a hidden centre foot
    feet = []
    if left:
        feet += [(1.4, 2.8), (1.4, 13.6)]
    if right:
        feet += [(14.6, 2.8), (14.6, 13.6)]
    if not feet:
        feet = [(8, 8)]
    for i, (fx, fz) in enumerate(feet):
        m.box(f"leg_{i}", (fx - 0.8, 0.8, fz - 0.8), (fx + 0.8, 2, fz + 0.8), "wood")
        m.box(f"leg_tip_{i}", (fx - 0.5, 0, fz - 0.5), (fx + 0.5, 0.8, fz + 0.5), "gold")
    m.save(os.path.join(ROOT, f"models/block/pink_couch_{part}.json"))


for p in ("single", "left", "middle", "right"):
    couch(p)

rot = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for d, y in rot.items():
    for p in ("single", "left", "middle", "right"):
        v = {"model": f"bakery:block/pink_couch_{p}"}
        if y:
            v["y"] = y
        variants[f"facing={d},part={p}"] = v
with open(os.path.join(ROOT, "blockstates/pink_couch.json"), "w") as f:
    json.dump({"variants": variants}, f, indent="\t")
    f.write("\n")
