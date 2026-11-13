"""
16x16 leaves, studied from the Journey's End leaf sheet: soft clumps of leaves in a few close tones, each lit from
the upper left and shaded to the lower right, sitting on a darker leafy shade (no black underlayer, no outlines).

Each tree has its own leaf shape, its own way of letting light through and its own accent:

    cherry blossom  five-petal flowers packed in clusters, a few green leaf tips   petals thin out to small holes
    citrus          broad glossy ovals with a shine pixel                          very few, tight holes
    orangewood      round, dense, dark clumps                                      almost solid
    peachwood       long slender leaves in fans, yellow-green                      airy slits between the fans
    mangowood       long leaves hanging straight down, a few bronze young leaves   vertical gaps
    plumwood        little round coin leaves, dusky violet                         scattered pin holes
    palm            feathery fronds                                                mostly air

Every leaf also gets an "accent" texture (blossoms, buds or young leaves) and the blockstate picks between the
plain and the accent texture and turns the block at random, so the tiling never shows as a grid.
Fruit is not painted on: it hangs under the leaves as its own block.
Run: python3 blockbench/gen_leaves16.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_woods16 import ramp, hx, noise, TEX, S
from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")

# four calm tones: shade, rim, body, light (close together, so it reads soft), then the see-through gaps per block
LEAF = {
    "blossom": (("#c26a96", "#d888b0", "#f0aecb", "#ffcfe3"), 3),
    "citrus": (("#2f5e2c", "#3f7636", "#5a9644", "#7cb456"), 2),
    "orangewood": (("#2a5228", "#386a32", "#4f8a40", "#6ea650"), 1),
    "peachwood": (("#4a7432", "#5c8a3c", "#78a64a", "#9cc260"), 4),
    "mangowood": (("#2a5a30", "#38703a", "#4f8e48", "#70ac5a"), 3),
    "plumwood": (("#4e3e6c", "#605084", "#7c6aa2", "#a08cc4"), 2),
    "palm": (("#2f6a2c", "#3e8036", "#58a046", "#7cbc5a"), 0),
}


def canvas():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def put(img, x, y, c, a=255):
    img.putpixel((int(x) % S, int(y) % S), tuple(c[:3]) + (a,))


def clump(img, cx, cy, rx, ry, ang, pal):
    """One soft clump or leaf: body tone, a lit patch toward the upper left, one shaded rim at the lower right."""
    ca, sa = math.cos(ang), math.sin(ang)
    r = int(max(rx, ry)) + 2
    for oy in range(-r, r + 1):
        for ox in range(-r, r + 1):
            px, py = ox + 0.5 - (cx % 1), oy + 0.5 - (cy % 1)
            u = (px * ca + py * sa) / rx
            v = (-px * sa + py * ca) / ry
            d = u * u + v * v
            if d > 1.0:
                continue
            lit = -(px + py) / (rx + ry)
            k = 3 if lit > 0.28 else 2
            if d > 0.55 and lit < -0.2:
                k = 1
            put(img, math.floor(cx) + ox, math.floor(cy) + oy, pal[k])


def leaf(img, cx, cy, length, width, ang, pal, cast=True):
    """One small leaf (a few pixels): a lit upper edge, a body, a darker lower rim, and a 1px shadow it casts on
    whatever lies below-right of it. Drawn back to front, so every leaf keeps its own outline instead of melting
    into a blob."""
    ca, sa = math.cos(ang), math.sin(ang)
    rx, ry = length / 2, width / 2
    r = int(rx) + 3
    body, shadow = [], []
    for oy in range(-r, r + 1):
        for ox in range(-r, r + 1):
            px, py = ox + 0.5 - (cx % 1), oy + 0.5 - (cy % 1)
            u = (px * ca + py * sa) / rx
            v = (-px * sa + py * ca) / ry
            d = u * u + v * v
            X, Y = math.floor(cx) + ox, math.floor(cy) + oy
            if d <= 1.0:
                side = -(px + py) / (rx + ry)          # >0 toward the light (upper left)
                k = 3 if side > 0.12 else 2
                if d > 0.45 and side < -0.1:
                    k = 1
                body.append((X, Y, k))
            elif cast and d <= 1.8 and px + py > 0.4:
                shadow.append((X, Y))
    for (X, Y) in shadow:
        put(img, X, Y, pal[0])
    for (X, Y, k) in body:
        put(img, X, Y, pal[k])


# hand-drawn sprites: digits index a ramp (0 = shade/shadow), "c" the deep pink heart, "." leaves what is below
FLOWER_BIG = [".33..",
              "34432",
              "33c21",
              ".3210",
              "..10."]
FLOWER_SMALL = [".3.",
                "3c2",
                ".21"]
GREEN_LEAF = ["..32",
              ".321",
              "3210"]


def stamp(img, sprite, x0, y0, ramp, heart=None):
    for j, row in enumerate(sprite):
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            put(img, x0 + i, y0 + j, heart if ch == "c" else ramp[int(ch)])


# where the blossoms sit on the (wrapping) tile: (x, y, big?) - spaced evenly, never in rows
BLOSSOMS = [(0, 0, 1), (5, 1, 1), (11, 0, 1), (3, 6, 1), (8, 5, 1), (13, 6, 1), (0, 11, 1), (6, 11, 1),
            (11, 11, 1), (10, 3, 0), (15, 3, 0), (4, 15, 0)]
BLOSSOMS_ACCENT = [(1, 0, 1), (6, 0, 1), (12, 1, 1), (3, 5, 1), (9, 6, 1), (14, 6, 0), (0, 10, 1), (5, 11, 1),
                   (11, 11, 1), (10, 3, 0), (15, 11, 0), (8, 14, 0)]
BLOSSOM_LEAVES = [(4, 3), (13, 9), (8, 9), (1, 7), (14, 14)]


# small leaves per tree: (number of leaves, length range, width range, angle function)
FINE = {
    "citrus": (15, (4.8, 5.4), (2.6, 3.0), lambda r: r.uniform(-0.9, 0.9) + r.choice((0.0, math.pi / 2))),
    "orangewood": (19, (3.8, 4.4), (2.6, 3.0), lambda r: r.uniform(0, math.pi)),
    "peachwood": (17, (6.0, 7.0), (1.9, 2.1), lambda r: r.choice((0.7, 2.45)) + r.uniform(-0.25, 0.25)),
    "mangowood": (15, (6.4, 7.4), (2.0, 2.3), lambda r: math.pi / 2 + r.uniform(-0.3, 0.3)),
}


def fine_leaves(name, pal, rnd, accent):
    img = canvas()
    for y in range(S):
        for x in range(S):
            put(img, x, y, pal[0])
    if name == "blossom":
        pal = [hx("#a6537f")] + list(pal[1:])
        img = canvas()
        for y in range(S):
            for x in range(S):
                put(img, x, y, pal[0])
        petal = [pal[0]] + [hx(c) for c in ("#d985ab", "#f0a6c8", "#fcc4dc", "#ffe2ee")]
        green = [pal[0]] + [hx(c) for c in ("#5c8a44", "#7aa652", "#9cc46c")]
        for (x, y) in BLOSSOM_LEAVES:
            stamp(img, GREEN_LEAF, x, y, green)
        for (x, y, big) in (BLOSSOMS_ACCENT if accent else BLOSSOMS):
            stamp(img, FLOWER_BIG if big else FLOWER_SMALL, x, y, petal, hx("#b04a78"))
        return img, pal
    n, (l0, l1), (w0, w1), angf = FINE[name]
    pal = [tuple(int(c * 0.8) for c in pal[0])] + list(pal[1:])
    for y in range(S):
        for x in range(S):
            put(img, x, y, pal[0])
    pts = poisson(rnd, S / math.sqrt(n) * 0.78, n)
    for (x, y) in pts:
        leaf(img, x, y, rnd.uniform(l0, l1), rnd.uniform(w0, w1), angf(rnd), pal)
    if name == "citrus":                       # a glossy shine pixel on a few leaves
        lit = [(x, y) for y in range(S) for x in range(S) if img.getpixel((x, y))[:3] == tuple(pal[3])]
        rnd.shuffle(lit)
        for (x, y) in lit[:4]:
            put(img, x, y, hx("#a6d27a"))
    if name == "mangowood" and accent:        # bronze young leaves at the tips
        bronze = [pal[0], hx("#7a4a2a"), hx("#a46a3a"), hx("#c88e52")]
        for _ in range(2):
            leaf(img, rnd.uniform(0, S), rnd.uniform(0, S), 4.6, 1.3, math.pi / 2 + rnd.uniform(-0.3, 0.3), bronze)
    return img, pal


def poisson(rnd, min_d, want):
    """Points scattered evenly but organically over the wrapping tile, sorted top to bottom."""
    pts = []
    tries = 0
    while len(pts) < want and tries < 6000:
        tries += 1
        x, y = rnd.uniform(0, S), rnd.uniform(0, S)
        if all(min(abs(x - px), S - abs(x - px)) ** 2 + min(abs(y - py), S - abs(y - py)) ** 2 >= min_d * min_d for px, py in pts):
            pts.append((x, y))
    return sorted(pts, key=lambda p: p[1] + p[0] * 0.3)


def spots(n_side, rnd, wobble=0.3):
    """About n_side^2 points scattered organically (wrapping Poisson-disc), never in rows or bricks."""
    want = n_side * n_side
    min_d = S / n_side * 0.82
    pts = []
    tries = 0
    while len(pts) < want and tries < 4000:
        tries += 1
        x, y = rnd.uniform(0, S), rnd.uniform(0, S)
        ok = True
        for (px, py) in pts:
            dx = min(abs(x - px), S - abs(x - px))
            dy = min(abs(y - py), S - abs(y - py))
            if dx * dx + dy * dy < min_d * min_d:
                ok = False
                break
        if ok:
            pts.append((x, y))
    return sorted(pts, key=lambda p: p[1])        # lower clumps overlap the ones above them


def holes(img, pal, n, rnd):
    """A few small gaps, only where the shade shows (never cutting into a clump)."""
    shade = tuple(pal[0])
    cand = [(x, y) for y in range(S) for x in range(S) if img.getpixel((x, y))[:3] == shade]
    rnd.shuffle(cand)
    made = []
    for (x, y) in cand:
        if len(made) >= n:
            break
        if any(abs(x - mx) < 5 and abs(y - my) < 5 for mx, my in made):
            continue
        made.append((x, y))
        img.putpixel((x, y), (0, 0, 0, 0))
        dx, dy = rnd.choice(((1, 0), (0, 1), (0, 0)))
        nx, ny = (x + dx) % S, (y + dy) % S
        if img.getpixel((nx, ny))[:3] == shade:
            img.putpixel((nx, ny), (0, 0, 0, 0))


def leaves(name, accent=False):
    tones, gaps = LEAF[name]
    pal = [hx(c) for c in tones]
    rnd = random.Random(sum(map(ord, name)) * 31 + (7 if accent else 0))
    img = canvas()
    if name == "blossom" or name in FINE:      # many small, separate leaves (or flowers)
        img, pal = fine_leaves(name, pal, rnd, accent)
        holes(img, pal, gaps + 3, rnd)
        if accent and name != "mangowood":
            dot = {"blossom": "#fff4f8", "citrus": "#fffbe8", "orangewood": "#fffbe8", "peachwood": "#ffd2dc"}[name]
            lit = [(x, y) for y in range(S) for x in range(S) if img.getpixel((x, y))[:3] in (tuple(pal[3]), hx("#ffdcea"))]
            rnd.shuffle(lit)
            placed = []
            for (x, y) in lit:
                if len(placed) == 2:
                    break
                if all(abs(x - a) + abs(y - b) > 7 for a, b in placed):
                    placed.append((x, y))
                    put(img, x, y, hx(dot))
        return img
    if name != "palm":
        for y in range(S):
            for x in range(S):
                put(img, x, y, pal[0])
    if name in ("blossom", "orangewood"):      # big round puffs
        for (x, y) in spots(3, rnd):
            k = rnd.uniform(0.85, 1.15)
            clump(img, x, y, 3.0 * k, 2.6 * k, 0, pal)
        for (x, y) in spots(2, rnd):
            clump(img, x, y, 2.0, 1.8, 0, pal)
    elif name == "citrus":                      # broad ovals, tilted
        for (x, y) in spots(4, rnd):
            clump(img, x, y, 2.6, 1.6, rnd.uniform(-0.6, 0.6), pal)
    elif name == "peachwood":                   # long slender leaves in pairs
        for (x, y) in spots(3, rnd):
            a = rnd.uniform(0.4, 1.1)
            clump(img, x - 1, y, 3.2, 1.0, a, pal)
            clump(img, x + 1, y + rnd.choice((-1, 0, 1)), 3.0, 1.0, math.pi - a - rnd.uniform(-0.3, 0.3), pal)
    elif name == "mangowood":                   # long leaves hanging down
        for (x, y) in spots(4, rnd, 0.25):
            clump(img, x, y, 3.4, 1.2, math.pi / 2 + rnd.uniform(-0.2, 0.2), pal)
    elif name == "plumwood":                    # small round coin leaves
        for (x, y) in spots(5, rnd, 0.25):
            clump(img, x, y, rnd.uniform(1.7, 2.3), rnd.uniform(1.4, 1.9), rnd.uniform(0, 3.14), pal)
    elif name == "palm":                        # two soft fronds over open air
        for (ox, oy) in ((0, 3), (8, 11)):
            for t in range(0, 16, 3):
                for side in (-1, 1):
                    a = math.atan2(side, 0.8)
                    clump(img, ox + t + math.cos(a) * 1.6, oy + t * 0.25 + math.sin(a) * 1.6, 2.2, 0.9, a, pal)
            for t in range(16):
                put(img, ox + t, oy + t * 0.25, pal[1])
    holes(img, pal, gaps, rnd)
    if accent:                                  # one or two quiet details, nothing more
        dot = {"blossom": "#fff0f6", "citrus": "#fffbe8", "orangewood": "#fffbe8", "peachwood": "#ffd2dc",
               "mangowood": "#b88a4e", "plumwood": "#ece0f6", "palm": "#a8d470"}[name]
        lit = [(x, y) for y in range(S) for x in range(S) if img.getpixel((x, y))[:3] == tuple(pal[3])]
        rnd.shuffle(lit)
        placed = []
        for (x, y) in lit:
            if len(placed) == 2:
                break
            if all(abs(x - a) + abs(y - b) > 7 for a, b in placed):
                placed.append((x, y))
                put(img, x, y, hx(dot))
    return img


def blockstate(name):
    """Plain leaves in four turns (weight 3 each) and the accent texture in two (weight 1 each)."""
    vs = []
    for (x, y) in ((0, 0), (0, 90), (0, 180), (0, 270)):
        v = {"model": f"bakery:block/{name}_leaves", "weight": 3}
        if y:
            v["y"] = y
        vs.append(v)
    for (x, y) in ((0, 0), (0, 180)):
        v = {"model": f"bakery:block/{name}_leaves_accent", "weight": 1}
        if y:
            v["y"] = y
        vs.append(v)
    return {"variants": {"": vs}}


def build():
    n = 0
    for name in LEAF:
        leaves(name).save(os.path.join(TEX, f"{name}_leaves.png"))
        leaves(name, accent=True).save(os.path.join(TEX, f"{name}_leaves_accent.png"))
        with open(os.path.join(ASSETS, "models/block", f"{name}_leaves_accent.json"), "w") as fh:
            json.dump({"parent": "minecraft:block/leaves", "textures": {"all": f"bakery:block/{name}_leaves_accent"}}, fh, indent="\t")
        with open(os.path.join(ASSETS, "blockstates", f"{name}_leaves.json"), "w") as fh:
            json.dump(blockstate(name), fh, indent="\t")
        n += 2
    return n


if __name__ == "__main__":
    print(build(), "leaf textures written")
