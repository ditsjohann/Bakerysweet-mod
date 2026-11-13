"""
Photographs every placeable bakery block in-game, one close shot each, for a numbered overview sheet.

    python3 blockbench/catalog_shots.py            # writes run/showcase.txt and run/all_blocks.json
    xvfb-run -a ./gradlew runShowcase              # shots land in run/screenshots/all_<n>.png
    python3 blockbench/catalog_sheet.py out_dir    # numbered contact sheets
"""
import json
import os

BS = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/blockstates")
WOODS = ["blossom", "citrus", "orangewood", "peachwood", "mangowood", "plumwood", "palm"]
SKIP = {"multiblock_part", "oven_rack", "attached_zucchini_stem"}
FRUIT = {"cherries", "coconut", "lemon", "mango", "orange", "peach", "plum"}
TALL = {"corn_crop", "pea_crop", "heart_lamp_post", "parasol_table", "chocolate_fountain", "cake_shelf", "jar_shelf",
        "menu_board", "display_case", "pastry_cart", "hanging_flowers"}
BIG = {"grand_oven", "grand_millstone"}
FLAT = {"cloud_rug", "heart_rug", "welcome_mat", "pastry_board", "cookies", "macarons"}


def has_facing(name):
    return '"facing' in open(os.path.join(BS, name + ".json")).read() or "facing=" in open(os.path.join(BS, name + ".json")).read()


def entries():
    """(label, profile, [(dx, dy, dz, block state)])"""
    out = []
    names = sorted(f[:-5] for f in os.listdir(BS))
    wood_names = set()
    for w in WOODS:
        parts = [
            (f"{w} log", f"{w}_log[axis=y]"), (f"{w} stripped log", f"stripped_{w}_log[axis=y]"),
            (f"{w} wood", f"{w}_wood[axis=y]"), (f"{w} stripped wood", f"stripped_{w}_wood[axis=y]"),
            (f"{w} planks", f"{w}_planks"), (f"{w} stairs", f"{w}_stairs[facing=east]"),
            (f"{w} slab", f"{w}_slab[type=bottom]"), (f"{w} fence", f"{w}_fence"),
            (f"{w} fence gate", f"{w}_fence_gate[facing=north]"),
            (f"{w} trapdoor", f"{w}_trapdoor[facing=south,open=true]"),
            (f"{w} leaves", f"{w}_leaves[persistent=true]"), (f"{w} sapling", f"{w}_sapling"),
        ]
        for label, st in parts:
            wood_names.add(st.split("[")[0])
            out.append((label, "std", [(0, 0, 0, "bakery:" + st)]))
        wood_names.add(f"{w}_door")
        out.append((f"{w} door", "tall", [(0, 0, 0, f"bakery:{w}_door[facing=north,half=lower]"),
                                           (0, 1, 0, f"bakery:{w}_door[facing=north,half=upper]")]))
    for n in names:
        if n in SKIP or n in wood_names:
            continue
        label = n.replace("_", " ")
        fac = "facing=north" if has_facing(n) else ""
        def st(extra=""):
            props = ",".join(x for x in (fac, extra) if x)
            return f"bakery:{n}[{props}]" if props else f"bakery:{n}"
        if n.endswith("_crop") and n not in ("corn_crop", "pea_crop"):
            out.append((label, "std", [(0, -1, 0, "minecraft:farmland"), (0, 0, 0, st("age=3"))]))
        elif n in ("corn_crop", "pea_crop"):
            out.append((label, "big", [(0, -1, 0, "minecraft:farmland")] + [(0, i, 0, st(f"age=3,segment={i}")) for i in range(3)]))
        elif n == "zucchini_stem":
            out.append((label, "std", [(0, -1, 0, "minecraft:farmland"), (0, 0, 0, st("age=7"))]))
        elif n in FRUIT:
            out.append((label, "high", [(0, 2, 0, "minecraft:oak_leaves[persistent=true]"), (0, 1, 0, st("age=2"))]))
        elif n == "pink_couch":
            out.append((label, "wide", [(-1, 0, 0, st("part=left")), (0, 0, 0, st("part=middle")), (1, 0, 0, st("part=right"))]))
        else:
            prof = "big" if n in BIG else "tall" if n in TALL else "flat" if n in FLAT else "std"
            out.append((label, prof, [(0, 0, 0, st())]))
    return out


CAM = {  # profile: (distance in front, feet height above the floor, pitch)
    "std": (2.3, -0.55, 16), "flat": (1.9, -0.45, 38), "tall": (3.6, -0.3, 10), "high": (3.0, 0.1, 6),
    "wide": (3.8, -0.2, 18), "big": (6.2, 0.4, 12),
}


def build():
    items = entries()
    X0, Y, Z0 = 1000, 150, 0
    per_row, dx, dz = 16, 6, 12
    rows = (len(items) + per_row - 1) // per_row
    x1, z1 = X0 + per_row * dx + 6, Z0 + rows * dz + 6
    lines = ["cmd gamerule doDaylightCycle false", "cmd gamerule doMobSpawning false", "cmd gamerule randomTickSpeed 0",
             "cmd time set 6000", "cmd weather clear", "cmd gamemode spectator @a",
             f"cmd forceload add {X0 - 10} {Z0 - 10} {x1} {z1}", "wait 60"]
    for r in range(rows):
        z = Z0 + r * dz
        lines += [f"cmd fill {X0 - 6} {Y - 2} {z - 8} {x1} {Y - 1} {z + 3} minecraft:smooth_quartz",
                  f"cmd fill {X0 - 6} {Y} {z - 8} {x1} {Y + 6} {z + 3} minecraft:air",
                  f"cmd fill {X0 - 6} {Y} {z + 4} {x1} {Y + 6} {z + 4} minecraft:white_concrete"]
    manifest = []
    for i, (label, prof, blocks) in enumerate(items):
        x, z = X0 + (i % per_row) * dx, Z0 + (i // per_row) * dz
        for (bx, by, bz, st) in blocks:
            lines.append(f"cmd setblock {x + bx} {Y + by} {z + bz} {st}")
        manifest.append({"n": i + 1, "label": label, "profile": prof})
    lines.append("wait 100")
    for i, (label, prof, blocks) in enumerate(items):
        x, z = X0 + (i % per_row) * dx, Z0 + (i // per_row) * dz
        dist, h, pitch = CAM[prof]
        lines += [f"cam {x + 0.5} {Y + h} {z + 0.5 - dist} 0 {pitch}", f"shot all_{i + 1:03d}"]
    lines.append("quit")
    open(os.path.join(os.path.dirname(__file__), "..", "run/showcase.txt"), "w").write("\n".join(lines) + "\n")
    json.dump(manifest, open(os.path.join(os.path.dirname(__file__), "..", "run/all_blocks.json"), "w"), indent=1)
    print(len(items), "blocks")


if __name__ == "__main__":
    build()
