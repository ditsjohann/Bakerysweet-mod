"""
The bakery counter and the bread, redone in the calm 16x16 style of the woods:

- counter: a soft pink-and-cream marble top with a rounded lip, cream board cabinet, a framed front panel with a
  little heart, and a rounded pastel-pink vintage register (sloped keys, a little display, a brass crank) instead
  of the boxy mint block; the open/closed sign stands on a brass easel
- bread: golden crusts with soft painterly shading, a few score marks and a dusting of flour, a pale crumb

Run after gen_food/gen_deco/gen_kitchen (it replaces their counter and bread textures): python3 blockbench/gen_counter_bread.py
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods16 import ramp, noise, quantize, hx, S
from round_model import Model
from PIL import Image

A = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
TEX = os.path.join(A, "textures/block")


def canvas(c=None):
    return Image.new("RGBA", (S, S), (hx(c) + (255,)) if c else (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def painted(pal, seed, cw, ch):
    """A calm fill: the noise spread over the palette's tones in soft clusters."""
    img = canvas()
    q = quantize(noise(seed, cw, ch, ((1.0, 1.0), (0.5, 0.45))), len(pal))
    for y in range(S):
        for x in range(S):
            put(img, x, y, pal[q[y][x]])
    return img


# ------------------------------------------------------------------ counter

def counter_top():
    """Pale pink-cream marble: soft clouds and two thin broken veins."""
    pal = [hx(c) for c in ("#eedbe1", "#f5e6eb", "#faf0f2", "#fff9fa")]
    img = painted(pal, 71, 5, 4)
    vein = hx("#e3c3cf")
    rnd = random.Random(5)
    for start in (2, 10):
        x = start
        for y in range(S):
            if rnd.random() < 0.8:
                put(img, x % S, y, vein)
            x += rnd.choice((0, 1, 1))
    return img


def counter_wood():
    """Cream boards standing upright, one shade apart, soft seams."""
    pal = ramp("#c9a273", "#f3dfbd", 6)
    img = canvas()
    f = noise(83, 1, 6, ((1.0, 1.0),))
    for b in range(4):
        base = (2, 3, 2, 3)[b]
        for y in range(S):
            for xx in range(4):
                x = b * 4 + xx
                k = 1 if xx == 3 else min(4, base + (1 if f[y][x] > 0.66 else 0))
                if xx == 0 and y % 8 == 3:
                    k = 1  # a soft board end
                put(img, x, y, pal[k])
    return img


def counter_panel():
    """The cabinet's front: a framed cream panel with a small pink heart."""
    img = canvas("#f9eee2")
    light, dark, inner = hx("#fffaf3"), hx("#e8d2b8"), hx("#f4e4d2")
    for i in range(S):
        put(img, i, 0, light); put(img, 0, i, light)
        put(img, i, S - 1, dark); put(img, S - 1, i, dark)
    for i in range(3, 13):
        put(img, i, 3, dark); put(img, 3, i, dark)
        put(img, i, 12, light); put(img, 12, i, light)
    for y in range(4, 12):
        for x in range(4, 12):
            put(img, x, y, inner)
    heart = [".##.##.", "#######", ".#####.", "..###..", "...#..."]
    pink, hi = hx("#ee8aaa"), hx("#f9bfd2")
    for dy, row in enumerate(heart):
        for dx, ch in enumerate(row):
            if ch == "#":
                put(img, 5 + dx - 1, 5 + dy, hi if (dy == 1 and dx in (1, 4)) else pink)
    return img


def register_body():
    """Pastel pink enamel, lighter at the top, with a cream band."""
    pal = [hx(c) for c in ("#e48eaa", "#ee9fb8", "#f6b3c7", "#fcc8d6")]
    img = canvas()
    for y in range(S):
        for x in range(S):
            k = 3 if y < 3 else 2 if y < 8 else 1 if y < 13 else 0
            put(img, x, y, pal[k])
    for x in range(S):
        put(img, x, 9, hx("#fff1e4"))
        put(img, x, 10, hx("#f3dcc8"))
    return img


def register_keys():
    """Rows of round pink keys on a cream plate."""
    img = canvas("#fbf0e4")
    for row in range(3):
        for col in range(4):
            x, y = 1 + col * 4, 2 + row * 5
            for (dx, dy, c) in ((0, 0, "#f6a9c1"), (1, 0, "#f6a9c1"), (0, 1, "#e48eaa"), (1, 1, "#d97d9a"), (0, 0, "#fcd3df")):
                put(img, x + dx, y + dy, hx(c))
    return img


def register_display():
    """A little dark window with a glowing heart."""
    img = canvas("#4a3946")
    for i in range(S):
        put(img, i, 0, hx("#e8c46a")); put(img, i, S - 1, hx("#c9a24a"))
        put(img, 0, i, hx("#e8c46a")); put(img, S - 1, i, hx("#c9a24a"))
    heart = [".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."]
    for dy, row in enumerate(heart):
        for dx, ch in enumerate(row):
            if ch == "#":
                put(img, 4 + dx, 5 + dy, hx("#ff8db0") if dy > 1 else hx("#ffc2d4"))
    return img


def counter_model(sign):
    m = Model(textures={"wood": "bakery:block/counter_wood", "marble": "bakery:block/counter_top", "panel": "bakery:block/counter_panel",
                        "register": "bakery:block/register", "keys": "bakery:block/register_keys", "display": "bakery:block/register_display",
                        "brass": "bakery:block/brass", "trim": "bakery:block/wood_pink", "sign": f"bakery:block/{sign}",
                        "particle": "bakery:block/counter_wood"}, ambient=True)
    m.box("cabinet", (0.5, 0, 1), (15.5, 12.5, 16), "wood")
    m.box("panel", (1.5, 1.5, 0.5), (14.5, 11.5, 1), "panel", faces=["north", "east", "west", "up", "down"])
    m.box("kick", (0.5, 0, 0.3), (15.5, 1.2, 1), "trim")
    m.box("trim", (0.5, 12, 0.4), (15.5, 12.6, 1), "trim")
    m.box("top", (0, 12.6, 0), (16, 14, 16), "marble")
    m.box("lip", (0.4, 12.3, -0.4), (15.6, 13.7, 0.2), "marble")
    # The register: a rounded body, sloped keys in front, a little display hood, a crank on the side.
    m.box("reg_base", (8.6, 14, 7.6), (15.2, 14.6, 13.4), "brass")
    m.box("reg_body", (9, 14.6, 8.4), (14.8, 17.4, 13), "register")
    m.box("reg_round", (9.4, 17.4, 8.8), (14.4, 18, 12.6), "register")
    m.box("reg_keys", (9.2, 14.8, 6.2), (14.6, 16.2, 9), "keys", rotation={"angle": -22.5, "axis": "x", "origin": [12, 15.5, 8.4]})
    m.box("reg_hood", (10.4, 18, 9.6), (13.6, 20, 11.4), "display")
    m.box("reg_hood_top", (10.2, 20, 9.4), (13.8, 20.4, 11.6), "brass")
    m.box("crank", (14.8, 15.6, 10.4), (15.6, 16.2, 11.0), "brass")
    m.box("crank_arm", (15.6, 15.6, 10.4), (16.0, 17.8, 11.0), "brass")
    m.box("crank_knob", (15.5, 17.8, 10.2), (16.2, 18.4, 11.2), "trim")
    # The open/closed sign on a little brass easel.
    m.box("easel_l", (2.0, 14, 3.4), (2.5, 18.5, 3.9), "brass", rotation={"angle": -22.5, "axis": "x", "origin": [2.25, 14, 3.6]})
    m.box("easel_r", (6.0, 14, 3.4), (6.5, 18.5, 3.9), "brass", rotation={"angle": -22.5, "axis": "x", "origin": [6.25, 14, 3.6]})
    m.box("sign", (1.4, 15.5, 2.6), (7.1, 19.5, 3.2), "sign", faces=["north", "south", "up", "down", "east", "west"],
          rotation={"angle": -22.5, "axis": "x", "origin": [4.25, 15.5, 2.9]})
    return m


# ------------------------------------------------------------------ bread

def crust(dark, light, seed, scores=3, diagonal=False, flour=4):
    """Golden crust: lighter on top, a calm three-tone shading, opened score marks and a little flour."""
    pal = ramp(dark, light, 5)
    img = canvas()
    f = noise(seed, 6, 3, ((1.0, 1.0),))
    for y in range(S):
        for x in range(S):
            v = 1.4 + (1 - y / S) * 1.6 + (f[y][x] - 0.5) * 0.9
            put(img, x, y, pal[max(1, min(3, int(round(v))))])
    rnd = random.Random(seed)
    for s in range(scores):
        y0 = 2 + s * 5
        x0 = rnd.randrange(3, 6)
        for t in range(6):
            x, y = x0 + t, y0 + (t // 2 if diagonal else 0)
            put(img, x, y, pal[4])          # the opened score shows the paler bread
            put(img, x, y + 1, pal[0])      # with a soft shadow under it
    for _ in range(flour):
        put(img, rnd.randrange(S), rnd.randrange(S // 2), hx("#fff3e0"))
    return img


def crumb():
    pal = [hx(c) for c in ("#e9cf9f", "#f3e0b8", "#f9ecd0", "#fff6e2")]
    img = painted(pal, 211, 3, 3)
    rnd = random.Random(2)
    for _ in range(5):
        x, y = rnd.randrange(S), rnd.randrange(S)
        put(img, x, y, hx("#dcbd88"))
    return img


def loaf_model():
    m = Model(textures={"wood": "bakery:block/pastry_board", "crust": "bakery:block/bread_crust", "bottom": "bakery:block/bread_bottom",
                        "crumb": "bakery:block/bread_crumb", "particle": "bakery:block/pastry_board"}, ambient=True)
    m.box("board", (2, 0, 3), (14, 1, 13), "wood")
    m.box("loaf", (4, 1, 5), (12, 4.5, 11), "crust", bottom="bottom")
    m.box("side_x", (3.4, 1.4, 5.6), (12.6, 4, 10.4), "crust", bottom="bottom")
    m.box("side_z", (4.6, 1.4, 4.4), (11.4, 4, 11.6), "crust", bottom="bottom")
    m.box("dome", (4.6, 4.5, 5.6), (11.4, 5.6, 10.4), "crust")
    m.box("crown", (5.6, 5.6, 6.6), (10.4, 6.3, 9.4), "crust")
    m.box("slice", (10.2, 1, 3.2), (13.4, 4.2, 4.2), "crust", faces=["north", "south", "east", "west", "up"])
    return m


def baguette_model():
    m = Model(textures={"cloth": "bakery:block/tablecloth", "crust": "bakery:block/baguette_crust", "bottom": "bakery:block/bread_bottom",
                        "particle": "bakery:block/tablecloth"}, ambient=True)
    m.box("cloth", (1, 0, 3), (15, 0.3, 13), "cloth", faces=["up", "down", "north", "south", "east", "west"])
    for name, z, angle in (("b1", 5.2, 22.5), ("b2", 8.8, -22.5)):
        rot = {"angle": angle, "axis": "y", "origin": [8, 1.5, z + 1]}
        m.box(name, (2, 0.3, z), (14, 2.4, z + 2.2), "crust", bottom="bottom", rotation=rot)
        m.box(name + "_top", (2.6, 2.4, z + 0.4), (13.4, 3.0, z + 1.8), "crust", rotation=rot)
        m.box(name + "_tip_a", (1.2, 0.6, z + 0.4), (2, 2, z + 1.8), "crust", rotation=rot)
        m.box(name + "_tip_b", (14, 0.6, z + 0.4), (14.8, 2, z + 1.8), "crust", rotation=rot)
    return m


def basket_model():
    m = Model(textures={"wicker": "bakery:block/wicker", "cloth": "bakery:block/tablecloth", "crust": "bakery:block/bread_crust",
                        "baguette": "bakery:block/baguette_crust", "particle": "bakery:block/wicker"}, ambient=True)
    m.box("basket", (2, 0, 3), (14, 5.5, 13), "wicker")
    m.box("rim", (1.6, 5.5, 2.6), (14.4, 6.2, 13.4), "wicker")
    m.box("cloth", (2.5, 6, 3.5), (13.5, 6.4, 12.5), "cloth")
    m.box("drape", (13.5, 3, 5), (14.6, 6.4, 11), "cloth")
    m.box("loaf1", (3.5, 6.4, 4.5), (8.5, 8.8, 8.5), "crust")
    m.box("loaf1_top", (4.1, 8.8, 5.1), (7.9, 9.5, 7.9), "crust")
    m.box("loaf2", (8, 6.4, 7.5), (13, 8.8, 11.5), "crust")
    m.box("loaf2_top", (8.6, 8.8, 8.1), (12.4, 9.5, 10.9), "crust")
    rot = {"angle": -45, "axis": "z", "origin": [5.5, 6, 10]}
    m.box("baguette", (4.6, 5, 9.1), (6.4, 13.5, 10.9), "baguette", rotation=rot)
    m.box("baguette_tip", (4.9, 13.5, 9.4), (6.1, 14.3, 10.6), "baguette", rotation=rot)
    return m


def build():
    textures = {
        "counter_top": counter_top(), "counter_wood": counter_wood(), "counter_panel": counter_panel(),
        "register": register_body(), "register_keys": register_keys(), "register_display": register_display(),
        "bread_crust": crust("#a35f28", "#e9b46e", 11), "baguette_crust": crust("#a8662c", "#efbf7a", 12, scores=3, diagonal=True),
        "bread_bottom": crust("#c08a54", "#ebc78e", 13, scores=0, flour=0), "bread_crumb": crumb(),
    }
    for name, img in textures.items():
        img.save(os.path.join(TEX, name + ".png"))
    for sign, name in (("sign_open", "bakery_counter_open"), ("sign_closed", "bakery_counter_closed")):
        path = os.path.join(A, "models/block", name + ".json")
        counter_model(sign).save(path)
        # The sign shows its whole picture (not just the middle a small box would get).
        model = json.load(open(path))
        for el in model["elements"]:
            if el.get("name") == "sign":
                el["faces"]["north"]["uv"] = [0, 0, 16, 16]
                el["faces"]["south"]["uv"] = [16, 0, 0, 16]
        json.dump(model, open(path, "w"), indent="\t")
    loaf_model().save(os.path.join(A, "models/block/bread_loaf.json"))
    baguette_model().save(os.path.join(A, "models/block/baguette.json"))
    basket_model().save(os.path.join(A, "models/block/bread_basket.json"))
    return len(textures)


if __name__ == "__main__":
    print(build(), "counter and bread textures written")
