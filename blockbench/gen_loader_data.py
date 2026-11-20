"""
Data files that NeoForge and Forge need but Fabric does not (Fabric does the same things in code):
  - NeoForge: the compostables data map (NeoForge's composter only reads data maps), from BakeryItems.init()
  - NeoForge and Forge: biome modifiers that add the wild fruit trees, from BakeryWorldgen.init()
Reads the Java sources, so it stays in step when items or trees change.
Run: python3 blockbench/gen_loader_data.py
"""
import json
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC = os.path.join(ROOT, "src/main/java/com/bakery")
WOODS = ["palm", "citrus", "blossom", "orangewood", "peachwood", "mangowood", "plumwood"]


def read(path):
    with open(os.path.join(SRC, path)) as f:
        return f.read()


def ids():
    """Java constant name -> registry path, for BakeryItems (directly, or through the block it places)."""
    blocks = {m.group(1): m.group(2) for m in re.finditer(r'public static final Block (\w+) = \w+\("(\w+)"', read("registry/BakeryBlocks.java"))}
    items = {}
    for m in re.finditer(r'public static final Item (\w+) = (\w+)\(([^;]*)', read("registry/BakeryItems.java")):
        name, call, args = m.groups()
        literal = re.match(r'\s*"(\w+)"', args)
        if literal:
            items[name] = literal.group(1)
        else:
            block = re.match(r'\s*BakeryBlocks\.(\w+)', args)
            if block and block.group(1) in blocks:
                items[name] = blocks[block.group(1)]
    return items


def compostables():
    items = ids()
    body = read("registry/BakeryItems.java")
    body = body[body.index("public static void init()"):]
    values = {}
    for arr, chance in re.findall(r'new Item\[\]\{([^}]*)\}\) \{\s*com\.bakery\.platform\.Hooks\.compostable\(compostable, ([0-9.]+)f\)', body):
        for name in re.findall(r'\w+', arr):
            values[f"bakery:{items[name]}"] = {"chance": float(chance)}
    if "set.leaves" in body:
        for w in WOODS:
            values[f"bakery:{w}_leaves"] = {"chance": 0.3}
            values[f"bakery:{w}_sapling"] = {"chance": 0.3}
    return values


def trees():
    out = []
    for m in re.finditer(r'tree\("(\w+)",[^;]*?((?:BiomeKeys\.\w+(?:,\s*)?)+)\);', read("registry/BakeryWorldgen.java"), re.S):
        biomes = ["minecraft:" + b.lower() for b in re.findall(r'BiomeKeys\.(\w+)', m.group(2))]
        out.append((m.group(1), biomes))
    return out


def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")


def build():
    values = compostables()
    dump(os.path.join(ROOT, "neoforge/src/main/resources/data/neoforge/data_maps/item/compostables.json"), {"values": values})
    n = 0
    for name, biomes in trees():
        for loader in ("neoforge", "forge"):
            dump(os.path.join(ROOT, f"{loader}/src/main/resources/data/bakery/{loader}/biome_modifier/{name}.json"),
                 {"type": f"{loader}:add_features", "biomes": biomes, "features": f"bakery:{name}", "step": "vegetal_decoration"})
        n += 1
    return len(values), n


if __name__ == "__main__":
    c, t = build()
    print(c, "compostables and", t, "tree biome modifiers written")
