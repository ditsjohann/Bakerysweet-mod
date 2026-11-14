"""
Makes furniture dyeable: for each block below, the part named here (cloth, cushion, panel ...) gets a light grey
"paint" texture and a tint index in a "_painted" copy of each model, and the blockstate picks the painted model
when the block is dyed (the game tints it with the dye colour).

    python3 blockbench/gen_paintable.py      (run after the generators that write these models)
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
# block -> texture variables in its models that take the dye
PAINT = {
    "cafe_table": ["cloth"],
    "cafe_chair": ["cushion"],
    "pink_awning": ["cloth"],
    "mint_awning": ["cloth"],
    "bakery_counter": ["panel"],
    "display_case": ["wood"],
    "mixing_bowl": ["bowl"],
    "bakery_catalog": ["cover"],
    "parasol_table": ["cloth"],
    "pastel_bench": ["wood"],
}


def paint_texture(tex_id):
    """A grey copy of a texture, light enough that a tint shows true colours but keeping its shading and pattern."""
    path = tex_id.split(":")[1]
    src = os.path.join(ROOT, "textures", path + ".png")
    out_rel = "block/paint/" + os.path.basename(path)
    out = os.path.join(ROOT, "textures", out_rel + ".png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img = Image.open(src).convert("RGBA")
    px = img.load()
    lums = [0.299 * r + 0.587 * g + 0.114 * b for (r, g, b, a) in img.getdata() if a > 0]
    lo, hi = min(lums), max(lums)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            l = 0.299 * r + 0.587 * g + 0.114 * b
            t = 0.5 if hi - lo < 1 else (l - lo) / (hi - lo)
            v = int(150 + t * 105)
            px[x, y] = (v, v, v, a)
    img.save(out)
    return "bakery:" + out_rel


def painted_model(model_id, keys):
    path = os.path.join(ROOT, "models", model_id.split(":")[1] + ".json")
    model = json.load(open(path))
    textures = dict(model.get("textures", {}))
    for key in keys:
        if key in textures:
            textures["paint_" + key] = paint_texture(textures[key])
    for el in model.get("elements", []):
        for face in el.get("faces", {}).values():
            tex = face.get("texture", "").lstrip("#")
            if tex in keys:
                face["texture"] = "#paint_" + tex
                face["tintindex"] = 0
    model["textures"] = textures
    out_id = model_id + "_painted"
    json.dump(model, open(os.path.join(ROOT, "models", out_id.split(":")[1] + ".json"), "w"), indent="\t")
    return out_id


def build():
    n = 0
    for block, keys in PAINT.items():
        bs_path = os.path.join(ROOT, "blockstates", block + ".json")
        bs = json.load(open(bs_path))
        variants = bs["variants"]
        # Start from the plain variants (drop any dyed=... keys from an earlier run).
        plain = {}
        for k, v in variants.items():
            parts = [p for p in k.split(",") if p and not p.startswith("dyed=")]
            plain[",".join(parts)] = v
        painted = {}
        out = {}
        for k, v in plain.items():
            entries = v if isinstance(v, list) else [v]
            new = []
            for e in entries:
                if e["model"] not in painted:
                    painted[e["model"]] = painted_model(e["model"], keys)
                e2 = dict(e)
                e2["model"] = painted[e["model"]]
                new.append(e2)
            out[("dyed=false," + k).rstrip(",")] = v
            out[("dyed=true," + k).rstrip(",")] = new if isinstance(v, list) else new[0]
        bs["variants"] = out
        json.dump(bs, open(bs_path, "w"), indent="\t")
        n += 1
    return n


if __name__ == "__main__":
    print(build(), "blocks made dyeable")
