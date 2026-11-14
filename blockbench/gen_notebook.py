"""
The Order Notebook: a little spiral notebook hung on the wall. Pink quilted cover, a cream lined page (the orders
are written on it at runtime by its block entity renderer), a ribbon bookmark and silver spiral rings on top.
Writes its two textures, the block model, blockstate, item model and loot table.
Run: python3 blockbench/gen_notebook.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
ASSETS = os.path.join(ROOT, "assets/bakery")
TEX = os.path.join(ASSETS, "textures/block")


def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def cover():
    """Soft pink with a stitched border and a tiny quilted diamond pattern."""
    img = Image.new("RGBA", (16, 16), hx("#f4a6c4"))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[x, y] = hx("#ee98b8")
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                px[x, y] = hx("#d0688e")
            elif edge == 1 and (x + y) % 2 == 0:
                px[x, y] = hx("#fff0f6")    # the stitching
    return img


def page():
    """Cream paper with faint blue lines and a pink margin line on the left."""
    img = Image.new("RGBA", (16, 16), hx("#fffaf0"))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 3 == 2:
                px[x, y] = hx("#e4eef6")
            if x == 2:
                px[x, y] = hx("#f8c8d8")
    return img


def el(frm, to, faces, name=None):
    e = {"from": frm, "to": to, "faces": faces}
    if name:
        e["name"] = name
    return e


def face(tex, uv, cull=None):
    f = {"texture": tex, "uv": uv}
    if cull:
        f["cullface"] = cull
    return f


def model():
    c, p, r = "#cover", "#page", "#ring"
    all6 = lambda t, uv: {d: face(t, uv) for d in ("north", "east", "south", "west", "up", "down")}
    elements = [
        # back cover against the wall
        el([2, 1, 15], [14, 15, 16], all6(c, [2, 1, 14, 15]), "cover"),
        # the page, a little inset
        el([3, 2, 14.5], [13, 13, 15], {"north": face(p, [3, 2, 13, 13]), "up": face(p, [3, 2, 13, 3]), "east": face(p, [0, 0, 1, 11]),
                                        "west": face(p, [0, 0, 1, 11])}, "page"),
        # ribbon bookmark hanging over the bottom
        el([10, 0, 14.4], [11, 3, 14.5], {"north": face(c, [0, 0, 1, 3]), "south": face(c, [0, 0, 1, 3])}, "ribbon"),
    ]
    # spiral rings along the top edge
    for x in (4, 6.5, 9, 11.5):
        elements.append(el([x, 13.5, 14.3], [x + 0.6, 15.5, 15.2], all6(r, [0, 0, 1, 2])))
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "bakery:block/order_notebook_cover", "cover": "bakery:block/order_notebook_cover",
                     "page": "bakery:block/order_notebook_page", "ring": "bakery:block/metal_white"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [0, 180, 0], "translation": [0, 0, -4], "scale": [0.9, 0.9, 0.9]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 2.5, -2], "scale": [0.4, 0.4, 0.4]},
            "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
        },
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent="\t")


def build():
    cover().save(os.path.join(TEX, "order_notebook_cover.png"))
    page().save(os.path.join(TEX, "order_notebook_page.png"))
    write(os.path.join(ASSETS, "models/block/order_notebook.json"), model())
    write(os.path.join(ASSETS, "models/item/order_notebook.json"), {"parent": "bakery:block/order_notebook"})
    write(os.path.join(ASSETS, "blockstates/order_notebook.json"), {"variants": {
        f"facing={d}": {"model": "bakery:block/order_notebook", "y": y} for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(os.path.join(ROOT, "data/bakery/loot_table/blocks/order_notebook.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "bakery:order_notebook"}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


if __name__ == "__main__":
    build()
    print("order notebook written")
