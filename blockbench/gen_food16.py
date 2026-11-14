"""
Food textures drawn as structured 16x16 pixel art: every one is built from what the food is made of - laminated
croissant layers, a sponge's air cells, piped ridges, a strawberry's seed lattice, a chocolate bar's squares,
a crust's scores - lit from the upper left in a few hand-picked tones. No noise and no random colour shifting.
White/pale textures stay light because the game tints them with the flavour colour.

Run after gen_food.py and gen_counter_bread.py (replaces their versions): python3 blockbench/gen_food16.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_materials16 import Px, tones, hx, planks, S


def ring_ramp(*cols):
    return [hx(c) for c in cols]


# ------------------------------------------------------------------ bread and pastry

def crust(base, scores=3, diagonal=False, flour=True):
    """A baked crust: a soft top-lit gradient (dithered where tones meet), oval scores opened to pale bread."""
    p = tones(base, 5, 0.18)
    px = Px()
    for y in range(S):
        for x in range(S):
            if y < 5:
                k = 3
            elif y == 5:
                k = 3 if (x + y) % 2 else 2
            elif y < 11:
                k = 2
            elif y == 11:
                k = 2 if (x + y) % 2 else 1
            else:
                k = 1
            px.set(x, y, p[k])
    # Each score: a lens of pale bread with a lit lip above and a shadow under.
    spots = ((2, 2), (8, 7), (3, 12)) if not diagonal else ((1, 1), (6, 6), (11, 11))
    for (x0, y0) in spots[:scores]:
        for t in range(6):
            x = x0 + t
            y = y0 + (t // 3 if diagonal else 0)
            px.set(x, y, p[4])
            if 0 < t < 5:
                px.set(x, y + 1, hx("#f6dcaa"))
            px.set(x, y + (2 if 0 < t < 5 else 1), p[0])
    if flour:
        for (x, y) in ((12, 1), (6, 4), (14, 9), (10, 13)):
            px.set(x, y, hx("#fff4e2"))
    return px


def crumb():
    """The inside of a loaf: pale crumb with a few irregular air holes, each shaded on its upper rim."""
    p = ring_ramp("#e2c690", "#efd9aa", "#f8e8c4", "#fff5de")
    px = Px(p[2])
    holes = (((2, 2), (3, 2), (2, 3)), ((9, 1), (10, 1), (10, 2), (11, 2)), ((5, 7), (6, 7), (7, 7), (6, 8), (5, 8)),
             ((12, 7), (12, 8), (13, 8)), ((1, 11), (2, 11)), ((8, 12), (9, 12), (9, 13), (10, 13)), ((14, 13), (14, 14)))
    for hole in holes:
        top = min(y for x, y in hole)
        for (x, y) in hole:
            px.set(x, y, p[0] if y == top else p[1])
            if (x, y + 1) not in hole:
                px.set(x, y + 1, p[3])
    return px


def croissant():
    """Laminated layers: arching bands, each lit on top with a dark crease under it."""
    p = tones("#d8954a", 5, 0.22)
    px = Px()
    for y in range(S):
        for x in range(S):
            arch = int(round(1.5 * math.sin(x / S * math.pi * 2)))
            band = (y + arch) % 4
            px.set(x, y, p[(4, 3, 2, 0)[band]])
    return px


def cookie():
    """A golden cookie: a few deliberate chocolate chips (each with a glint) and crackle lines."""
    p = tones("#dfa75c", 5, 0.16)
    px = Px(p[2])
    for (x, y, n, dx, dy) in ((1, 4, 4, 1, 0), (9, 1, 3, 1, 1), (6, 11, 5, 1, 0), (12, 8, 3, 0, 1)):
        for i in range(n):
            px.set(x + i * dx, y + i * dy, p[1])
            px.set(x + i * dx, y + i * dy + 1, p[3])
    chip = ring_ramp("#4a2a1a", "#6a3e26", "#8e5a38")
    for (x, y) in ((3, 1), (11, 5), (5, 7), (13, 12), (2, 13), (8, 14)):
        px.set(x, y, chip[1]); px.set(x + 1, y, chip[2]); px.set(x, y + 1, chip[0]); px.set(x + 1, y + 1, chip[1])
    return px


def dough(bubbles=False):
    """Soft dough: an even pale surface with two gentle folds; proofed dough shows little gas bubbles."""
    p = ring_ramp("#e6d2ac", "#f0e0c0", "#f6eacf", "#fdf5e4")
    px = Px(p[2])
    for (y0, amp, phase) in ((4, 1, 0), (11, 1, 2)):
        for x in range(S):
            y = y0 + int(round(amp * math.sin((x + phase) / S * math.pi * 2)))
            px.set(x, y, p[3])
            px.set(x, y + 1, p[1])
    if bubbles:
        for (x, y) in ((3, 7), (9, 2), (12, 14), (6, 13), (14, 7)):
            px.set(x, y, p[3]); px.set(x, y + 1, p[0])
    return px


def batter():
    """Batter in the bowl: smooth, with one lighter swirl left by the whisk."""
    p = ring_ramp("#e8cc92", "#f2dca8", "#f8e8bf", "#fff4d8")
    px = Px(p[2])
    for t in range(0, 260):
        a = t / 18
        r = 1 + a * 0.9
        x, y = int(round(8 + math.cos(a) * r)), int(round(8 + math.sin(a) * r))
        if 0 <= x < S and 0 <= y < S:
            px.set(x, y, p[3])
            if 0 <= y + 1 < S:
                px.set(x, y + 1, p[1])
    return px


def donut_dough():
    """A fried donut: golden top and bottom with the pale ring round its middle."""
    p = tones("#d9984c", 5, 0.2)
    px = Px()
    for y in range(S):
        k = 3 if y < 3 else 2 if y < 6 else 4 if y < 9 else 2 if y < 13 else 1
        for x in range(S):
            px.set(x, y, p[k])
    for x in range(S):
        px.set(x, 6, p[3]); px.set(x, 9, p[1])
    return px


def pie_crust():
    """A lattice top: strips over and under, each strip rounded (lit edge, shaded edge), crimped where they cross."""
    p = tones("#e2a65c", 5, 0.2)
    px = Px()
    for y in range(S):
        for x in range(S):
            on_h, on_v = y % 8 < 4, x % 8 < 4
            if on_h and on_v:
                over_h = ((x // 8) + (y // 8)) % 2 == 0
                t = (y % 4) if over_h else (x % 4)
            elif on_h:
                t = y % 4
            elif on_v:
                t = x % 4
            else:
                px.set(x, y, hx("#9a4a3a"))  # the filling showing through
                continue
            px.set(x, y, p[(3, 4, 2, 1)[t]])
    return px


def sponge(layers=False, base="#f6dda0"):
    """Sponge cake: an even crumb with neat little air cells; layered sponge has cream filling between."""
    p = tones(base, 5, 0.12)
    px = Px(p[2])
    cells = ((2, 1), (7, 3), (12, 1), (4, 6), (10, 7), (14, 5), (1, 10), (6, 12), (11, 11), (14, 14), (3, 14), (8, 9))
    for (x, y) in cells:
        px.set(x, y, p[0]); px.set(x + 1, y, p[1]); px.set(x, y + 1, p[3])
    if layers:
        cream = ring_ramp("#f2e8dc", "#fff8ee", "#ffffff")
        for y0 in (4, 11):
            for x in range(S):
                px.set(x, y0, cream[1]); px.set(x, y0 + 1, cream[2]); px.set(x, y0 + 2, cream[0])
    return px


def wrapper(base):
    """A cupcake case: soft pleats, each lit on its left and folded into shadow on its right."""
    p = tones(base, 5, 0.16)
    px = Px()
    for x in range(S):
        k = (4, 3, 2, 1)[x % 4]
        for y in range(S):
            px.set(x, y, p[k])
    for x in range(S):
        px.set(x, 0, p[4])
    return px


def macaron_shell_pink():
    """The pouf's pink macaron shell: smooth on top, frilly feet along the bottom."""
    p = tones("#f6a8c4", 5, 0.12)
    px = Px(p[2])
    for x in range(S):
        px.set(x, 2, p[3]); px.set(x, 3, p[3])
        for y in range(12, S):
            px.set(x, y, p[1] if (x + y) % 2 else p[0])
        px.set(x, 11, p[3] if x % 3 else p[2])
    return px


def macaron_shell():
    """A macaron shell: a smooth dome on top and the frilly 'feet' along the bottom."""
    p = ring_ramp("#e6e0e2", "#f2eef0", "#faf8f8", "#ffffff")
    px = Px(p[2])
    for x in range(S):
        px.set(x, 2, p[3]); px.set(x, 3, p[3])
        for y in range(12, S):
            px.set(x, y, p[1] if (x + y) % 2 else p[0])
        px.set(x, 11, p[3] if x % 3 else p[2])
    return px


# ------------------------------------------------------------------ icing and cream (tinted in game: kept light)

def smooth_icing(ripples=True):
    """Smooth icing / glaze / frosting: nearly flat, a soft highlight and a couple of gentle ripples."""
    p = ring_ramp("#e8e4e6", "#f2eff0", "#fafafa", "#ffffff")
    px = Px(p[2])
    if ripples:
        for (y0, phase) in ((5, 0), (12, 3)):
            for x in range(S):
                y = y0 + int(round(math.sin((x + phase) / S * math.pi * 2)))
                px.set(x, y, p[1])
                px.set(x, y - 1, p[3])
    for (x, y, n) in ((2, 2, 3), (10, 9, 2)):
        for i in range(n):
            px.set(x + i, y, p[3])
    return px


def piped():
    """Piped cream: ridges from a star tip, each with a lit side, a crest and a shaded side."""
    p = ring_ramp("#d8d2d6", "#ebe6e9", "#f6f3f5", "#ffffff")
    px = Px()
    for x in range(S):
        k = (3, 2, 1, 0)[x % 4]
        for y in range(S):
            px.set(x, y, p[k])
    return px


def ice_cream():
    """A scoop: creamy, with curling ripples where the scoop dragged."""
    p = ring_ramp("#e6dcd8", "#f2eae6", "#faf5f2", "#ffffff")
    px = Px(p[2])
    for (y0, phase) in ((3, 0), (8, 5), (13, 2)):
        for x in range(S):
            y = y0 + int(round(1.2 * math.sin((x + phase) / 8 * math.pi)))
            px.set(x, y, p[0]); px.set(x, y - 1, p[3])
    return px


def liquid():
    """A drink's surface: even, with two pale ripple rings."""
    p = ring_ramp("#e4e0dc", "#f0ece8", "#f8f5f2", "#ffffff")
    px = Px(p[2])
    for r in (3, 6):
        for a in range(0, 360, 6):
            x, y = int(round(8 + math.cos(math.radians(a)) * r)), int(round(8 + math.sin(math.radians(a)) * r * 0.8))
            px.set(x, y, p[3] if a > 180 else p[1])
    return px


# ------------------------------------------------------------------ fruit, chocolate, toppings

def chocolate():
    """A chocolate bar: square pieces, each bevelled - lit top and left, dark bottom and right."""
    p = ring_ramp("#3a1e14", "#4e2a1c", "#643a26", "#7e4c32", "#9a6444")
    px = Px(p[2])
    for y in range(S):
        for x in range(S):
            u, v = x % 4, y % 4
            if u == 0 or v == 0:
                px.set(x, y, p[3] if (u == 0 and v != 0) or (v == 0 and u != 0) else p[1])
            elif u == 3 or v == 3:
                px.set(x, y, p[1])
    for (x, y) in ((1, 1), (9, 5), (5, 13)):
        px.set(x, y, p[4])
    return px


def glossy(base):
    """A shiny round fruit (cherry): deep colour with a curved white highlight."""
    p = tones(base, 5, 0.25)
    px = Px(p[2])
    for y in range(S):
        for x in range(S):
            if y > 11:
                px.set(x, y, p[1])
    for (x, y) in ((3, 2), (4, 2), (5, 2), (2, 3), (2, 4), (3, 3)):
        px.set(x, y, hx("#fff0f2"))
    px.set(6, 3, p[4]); px.set(2, 5, p[4])
    return px


def strawberry():
    """Strawberry skin: red, a little lighter on top, seeds set in a neat diamond lattice in tiny dimples."""
    p = tones("#e8394d", 5, 0.2)
    px = Px()
    for y in range(S):
        for x in range(S):
            px.set(x, y, p[3] if y < 5 else p[2])
    for y in range(1, S, 4):
        for x in range((y // 4 % 2) * 2 + 1, S, 4):
            px.set(x, y, hx("#ffe48a"))
            px.set(x, y + 1, p[0])
    return px


def blueberry():
    """Blueberries: round berries with the dusty pale bloom on their tops and a dark star crown."""
    p = tones("#5a6ad8", 5, 0.24)
    px = Px(p[1])
    for (cx, cy) in ((4, 4), (12, 4), (8, 11), (0, 11), (16, 11), (4, 18), (12, -3)):
        for y in range(cy - 3, cy + 4):
            for x in range(cx - 3, cx + 4):
                d = (x - cx) ** 2 + (y - cy) ** 2
                if d <= 10:
                    px.set(x, y, p[4] if d <= 3 and y < cy else p[3] if y < cy else p[2])
        px.set(cx, cy + 1, p[0])
    return px


def raspberry():
    """Raspberry: a grid of round drupelets, each lit on its upper left."""
    p = tones("#e0406a", 5, 0.22)
    px = Px(p[1])
    for y in range(0, S, 3):
        for x in range((y // 3 % 2) * 1, S, 3):
            px.set(x, y, p[4]); px.set(x + 1, y, p[3]); px.set(x, y + 1, p[3]); px.set(x + 1, y + 1, p[2])
    return px


def mint_leaf():
    """A mint leaf: a pale midrib with paired veins, the soft blade crinkled between them."""
    p = tones("#5ac078", 5, 0.2)
    px = Px(p[2])
    for x in range(S):
        px.set(x, 8, p[4])
    for x0 in range(1, S, 4):
        for i in range(4):
            px.set(x0 + i, 7 - i, p[3]); px.set(x0 + i, 9 + i, p[1])
    return px


def lavender():
    """Lavender buds: little violet ovals in staggered rows."""
    p = tones("#a87ad8", 5, 0.2)
    px = Px(p[1])
    for y in range(0, S, 3):
        for x in range((y // 3 % 2) * 2, S, 4):
            px.set(x, y, p[4]); px.set(x + 1, y, p[3]); px.set(x, y + 1, p[2]); px.set(x + 1, y + 1, p[2])
    return px


def star_sugar():
    """Starbloom sugar: golden with tiny four-point sparkles."""
    p = tones("#f6d65a", 5, 0.16)
    px = Px(p[2])
    for (x, y) in ((3, 3), (11, 6), (6, 12), (14, 13)):
        px.set(x, y, hx("#ffffff"))
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            px.set(x + dx, y + dy, p[4])
    return px


def lemon():
    """Lemon slices: pale segments radiating from the middle, a yellow rind round the edge."""
    p = tones("#f6dc4a", 5, 0.18)
    px = Px()
    for y in range(S):
        for x in range(S):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            a = (math.degrees(math.atan2(dy, dx)) + 360) % 45
            if d > 7:
                px.set(x, y, p[1])
            elif d > 6:
                px.set(x, y, p[2])
            elif a < 5 or d < 1:
                px.set(x, y, hx("#fff8d8"))
            else:
                px.set(x, y, p[4] if d < 4 else p[3])
    return px


def overlay(kind, base):
    """Sprinkles, shavings and petals on a clear background, drawn piece by piece."""
    px = Px()
    if kind == "sprinkles":
        cols = ("#ff7aa8", "#7ac8ff", "#ffe066", "#8ce0a8", "#c8a0ff", "#ffffff")
        pieces = ((1, 2, 1, 0), (5, 1, 0, 1), (9, 3, 1, 0), (13, 1, 0, 1), (3, 7, 0, 1), (7, 8, 1, 0), (12, 7, 1, 0),
                  (1, 12, 1, 0), (6, 13, 0, 1), (10, 12, 1, 0), (14, 11, 0, 1))
        for i, (x, y, dx, dy) in enumerate(pieces):
            c = hx(cols[i % len(cols)])
            px.set(x, y, c); px.set(x + dx, y + dy, c)
    elif kind == "shavings":
        p = tones(base, 4, 0.2)
        for (x, y) in ((2, 2), (9, 1), (5, 7), (12, 8), (2, 12), (9, 13)):
            for i in range(3):
                px.set(x + i, y + (1 if i == 1 else 0), p[3 if i == 0 else 2])
            px.set(x + 1, y + 2, p[0])
    elif kind == "petals":
        p = tones(base, 4, 0.18)
        for (x, y) in ((2, 2), (10, 3), (6, 9), (13, 11), (2, 13)):
            px.set(x, y, p[3]); px.set(x + 1, y, p[2]); px.set(x, y + 1, p[2]); px.set(x + 1, y + 1, p[1])
    return px


def coins():
    """A pile of Sweet Coins: round coins with a bright rim on top and a dark one under, a heart stamped in each."""
    p = ring_ramp("#a8741a", "#d29a2a", "#ecbc44", "#ffe07a", "#fff4c0")
    px = Px(p[1])
    for (cx, cy) in ((4, 4), (12, 3), (8, 10), (0, 11), (16, 11), (4, 17), (12, 19)):
        for y in range(cy - 3, cy + 3):
            for x in range(cx - 3, cx + 4):
                d = ((x - cx) / 3.5) ** 2 + ((y - cy + 0.5) / 2.6) ** 2
                if d <= 1:
                    px.set(x, y, p[3] if d > 0.6 and y < cy else p[0] if d > 0.6 else p[2])
        px.set(cx, cy, hx("#f06a98")); px.set(cx - 1, cy - 1, hx("#f06a98")); px.set(cx + 1, cy - 1, hx("#f06a98"))
    return px


def tea():
    """Tea in the cup: warm amber, darker at the rim, a pale reflection ring."""
    p = ring_ramp("#7a3e14", "#9a5420", "#b86e30", "#d6904a", "#f2c890")
    px = Px(p[2])
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.5:
                px.set(x, y, p[1])
            elif 3.5 < d < 4.5 and x < y:
                px.set(x, y, p[4])
    return px


# ------------------------------------------------------------------ boards and stone

def marble(base_tones, vein):
    """Marble: an even pale stone with two thin veins that wander and branch (drawn, not noise)."""
    p = [hx(c) for c in base_tones]
    px = Px(p[1])
    v = hx(vein)
    paths = (((0, 3), (1, 1), (1, 1), (1, 0), (1, 1), (1, 0), (1, -1), (1, 0), (1, 1), (1, 1), (1, 1), (1, 0), (1, 1), (1, 0), (1, 1), (1, 0)),
             ((5, 15), (1, -1), (0, -1), (1, -1), (1, -1), (1, 0), (1, -1), (0, -1), (1, -1), (1, -1)))
    for path in paths:
        x, y = path[0]
        for (dx, dy) in path[1:]:
            px.set(x, y, v)
            px.set(x, y - 1, p[2])
            x, y = x + dx, y + dy
    # a branch
    for i in range(4):
        px.set(9 + i, 9 + i // 2, v)
    for (x, y) in ((3, 10), (12, 2), (7, 6)):
        px.set(x, y, p[2])
    return px


# ------------------------------------------------------------------ skins (fruit piles, leaves, stone)

def dimpled(base):
    """Citrus peel: tiny pores in a staggered grid, each a dark dot with a lit pixel above it."""
    p = tones(base, 5, 0.16)
    px = Px(p[2])
    for y in range(1, S, 3):
        for x in range((y // 3 % 2) * 2, S, 4):
            px.set(x, y, p[1]); px.set(x, y - 1, p[3])
    return px


def leaf(base):
    """A leaf: a pale midrib running across, paired side veins, the blade a little darker toward its edge."""
    p = tones(base, 5, 0.2)
    px = Px(p[2])
    for x in range(S):
        px.set(x, 7, p[4]); px.set(x, 8, p[3])
    for x0 in range(0, S, 5):
        for i in range(4):
            px.set(x0 + i, 6 - i, p[3]); px.set(x0 + i, 9 + i, p[1])
    for x in range(S):
        px.set(x, 0, p[1]); px.set(x, 15, p[0])
    return px


def petal(base):
    """A petal: soft colour paling toward the tip, with fine veins fanning from the base."""
    p = tones(base, 5, 0.14)
    px = Px()
    for y in range(S):
        for x in range(S):
            px.set(x, y, p[3] if y < 5 else p[2] if y < 12 else p[1])
    for vx in (3, 8, 13):
        for y in range(6, S):
            px.set(vx + (0 if y > 10 else (1 if vx < 8 else -1 if vx > 8 else 0)), y, p[1])
    return px


def fibres(base):
    """A coconut shell: rows of short hairy fibres slanting down, over the dark husk."""
    p = tones(base, 5, 0.24)
    px = Px(p[1])
    for y in range(0, S, 3):
        for x in range((y // 3 % 2) * 2, S, 4):
            px.set(x, y, p[3]); px.set(x + 1, y + 1, p[2]); px.set(x + 2, y + 2, p[0])
    return px


def ridged(base):
    """A young green coconut: smooth skin with soft lengthwise ridges."""
    p = tones(base, 5, 0.14)
    px = Px()
    for x in range(S):
        k = (3, 3, 2, 2, 2, 1)[x % 6]
        for y in range(S):
            px.set(x, y, p[k])
    return px


def pebbled(base):
    """Cream stone: smooth, with a few rounded pebbles set in it (lit top, shaded bottom) and a hairline crack."""
    p = tones(base, 5, 0.1)
    px = Px(p[2])
    for (x, y) in ((2, 3), (10, 2), (6, 9), (13, 11), (2, 13)):
        px.set(x, y, p[3]); px.set(x + 1, y, p[3]); px.set(x, y + 1, p[2]); px.set(x + 1, y + 1, p[1])
    for i in range(5):
        px.set(8 + i, 5 + i // 2, p[1])
    return px


# ------------------------------------------------------------------ big brick sheets (64px, mapped across the oven and mill)

def brick_sheet(bw, bh, mortar, brick_cols, light, dark, size=64, chips=True):
    """Clean bricks on a 64px sheet: each brick one flat tone (three variants), a lit top edge, a shaded bottom and
    right edge, an occasional chip, set in mortar with a shadow line under each course."""
    img = Px.__new__(Px)
    from PIL import Image
    img.img = Image.new("RGBA", (size, size), hx(mortar[0]) + (255,))
    m_light, m_dark = hx(mortar[0]), hx(mortar[1])
    cols = [hx(c) for c in brick_cols]
    hi, lo = hx(light), hx(dark)

    def put(x, y, c):
        img.img.putpixel((x % size, y % size), c + (255,))

    for row in range(size // bh):
        off = (bw // 2) * (row % 2)
        for b in range(size // bw + 1):
            x0, y0 = b * bw + off, row * bh
            c = cols[(b * 7 + row * 3) % len(cols)]
            for y in range(y0 + 1, y0 + bh):
                for x in range(x0 + 1, x0 + bw):
                    put(x, y, c)
            for x in range(x0 + 1, x0 + bw):
                put(x, y0 + 1, hi)
                put(x, y0 + bh - 1, lo)
            for y in range(y0 + 1, y0 + bh):
                put(x0 + bw - 1, y, lo)
            if chips and (b + row * 2) % 5 == 0:
                put(x0 + 3, y0 + bh - 2, lo); put(x0 + 4, y0 + bh - 2, lo); put(x0 + 3, y0 + bh - 3, c)
        for x in range(size):
            put(x, row * bh, m_light)
            if row > 0 and x % 3 == 0:
                put(x, row * bh, m_dark)  # a little texture in the mortar line
    return img


def catalog_wood():
    """The catalog lectern's pink wood: calm planks."""
    return planks("#f2b4c8", True)


def soil():
    """Potting soil: dark crumbs drawn as little clods, each lit on top."""
    p = ring_ramp("#2e1c14", "#3e2618", "#52321e", "#6a4226")
    px = Px(p[1])
    for (x, y) in ((1, 1), (6, 2), (11, 1), (3, 6), (9, 6), (14, 5), (1, 11), (6, 10), (12, 11), (8, 14), (3, 14)):
        px.set(x, y, p[3]); px.set(x + 1, y, p[2]); px.set(x, y + 1, p[2]); px.set(x + 1, y + 1, p[0])
    return px


def jam_frames():
    """Simmering jam (four frames, tinted in game): a glossy surface with a few bubbles rising and popping."""
    from PIL import Image
    p = ring_ramp("#d6d0d2", "#e6e1e3", "#f2eff0", "#ffffff")
    out = Image.new("RGBA", (S, S * 4))
    bubbles = ((3, 4), (10, 9), (6, 12), (13, 3))
    for f in range(4):
        px = Px(p[2])
        for (x, y, n) in ((2, 2, 3), (9, 6, 2)):
            for i in range(n):
                px.set(x + i, y, p[3])
        for i, (x, y) in enumerate(bubbles):
            stage = (f + i) % 4
            if stage == 1:
                px.set(x, y, p[3]); px.set(x, y + 1, p[1])
            elif stage == 2:
                px.set(x, y, p[3]); px.set(x + 1, y, p[3]); px.set(x, y + 1, p[1]); px.set(x + 1, y + 1, p[1])
            elif stage == 3:
                px.set(x - 1, y, p[1]); px.set(x + 2, y, p[1]); px.set(x, y - 1, p[1]); px.set(x + 1, y + 2, p[1])
        out.paste(px.img, (0, f * S))
    return out


def cloud_rug():
    """A sky-blue rug with a pale border and two puffy clouds, each shaded underneath."""
    sky, edge = hx("#a8d4f2"), hx("#d8eefc")
    px = Px(sky)
    for i in range(S):
        px.set(i, 0, edge); px.set(i, 15, edge); px.set(0, i, edge); px.set(15, i, edge)
    cloud = ["..###...", ".#####.#", "########", ".######."]
    for (ox, oy) in ((2, 3), (6, 9)):
        for dy, row in enumerate(cloud):
            for dx, ch in enumerate(row):
                if ch == "#":
                    px.set(ox + dx, oy + dy, hx("#ffffff") if dy < 3 else hx("#d6e8f6"))
    return px


def porcelain_floral():
    """White porcelain with little painted sprigs: a pink five-dot flower and two green leaves, set in a lattice."""
    px = Px(hx("#fdf8f6"))
    for (x, y) in ((3, 3), (11, 11), (11, 3), (3, 11)):
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            px.set(x + dx, y + dy, hx("#f2a2bc"))
        px.set(x, y, hx("#f6d06a"))
        px.set(x + 2, y + 1, hx("#8ac27a")); px.set(x - 2, y + 1, hx("#a6d48e"))
    for i in range(S):
        px.set(i, 7, hx("#f6eae8"))
    return px


def build():
    big = {
        "oven/bricks": brick_sheet(8, 4, ("#ead8c2", "#c9b49a"), ("#c4573f", "#b84e38", "#cc6248"), "#dc7a5e", "#9a3e2c"),
        "mill/stone_bricks": brick_sheet(16, 8, ("#6e6a66", "#5a5652"), ("#a8a4a0", "#9e9a96", "#b0aca8"), "#c4c0bc", "#86827e"),
        "oven/stone": brick_sheet(16, 8, ("#d6b48c", "#c09a72"), ("#f4e6cc", "#efdfc2", "#f8ecd6"), "#fffaf0", "#dcc6a4"),
    }
    for name, px in big.items():
        px.img.save(os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures/block", name + ".png"))
    jam_frames().save(os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures/block/jam_surface.png"))
    jobs = {
        "catalog_wood": catalog_wood(), "soil": soil(), "juice_liquid": liquid(), "cloud_rug": cloud_rug(), "porcelain_floral": porcelain_floral(),
        "leaf": leaf("#5aa64e"), "petal": petal("#f6a8c4"), "lemon_skin": dimpled("#f6dc4a"), "lemon_peel": dimpled("#f8e46a"),
        "lemon_young": dimpled("#a8d05a"), "coconut_shell": fibres("#7a5232"), "coconut_young": ridged("#7ab05a"),
        "stone_cream": pebbled("#ece2d4"), "macaron_pouf_shell": macaron_shell_pink(),
        "bread_crust": crust("#c8853e"), "bread_bottom": crust("#d8a868", scores=0, flour=False), "bread_crumb": crumb(),
        "baguette_crust": crust("#c98a42", diagonal=True), "croissant": croissant(), "cookie": cookie(),
        "dough": dough(), "proof_dough": dough(bubbles=True), "batter": batter(), "donut_dough": donut_dough(),
        "glaze": smooth_icing(False), "frosting": piped(), "cake_frosting": smooth_icing(), "cake_piping": piped(),
        "sponge": sponge(), "cake_sponge": sponge(), "cake_side": sponge(layers=True), "cake_inside": sponge(layers=True),
        "cake_bottom": sponge(base="#e8c07a"), "cupcake_wrapper": wrapper("#f6a6c0"), "pie_crust": pie_crust(),
        "pie_filling": smooth_icing(), "pie_inside": sponge(base="#f2c47a"),
        "macaron_shell": macaron_shell(), "macaron_filling": smooth_icing(), "macaron_cream": piped(),
        "chocolate": chocolate(), "cherry": glossy("#d02840"), "topping_strawberry": strawberry(), "topping_blueberry": blueberry(),
        "topping_raspberry": raspberry(), "topping_mint": mint_leaf(), "topping_lavender": lavender(), "topping_star": star_sugar(),
        "topping_cream": piped(), "topping_lemon": lemon(), "topping_chocolate": overlay("shavings", "#5a3222"),
        "topping_coconut": overlay("shavings", "#fffaf2"), "topping_rose": overlay("petals", "#f06a8a"), "sprinkle_overlay": overlay("sprinkles", ""),
        "ice_cream_block": ice_cream(), "liquid": liquid(), "coin_pile": coins(), "tea": tea(),
        "cupcake_lamp_frosting": piped(), "cupcake_lamp_cherry": glossy("#e0405a"),
        "marble": marble(("#efe9ea", "#f8f3f3", "#fdfafa"), "#ddd2d6"),
        "counter_top": marble(("#f1e2e6", "#f8ecef", "#fdf6f8"), "#e6c6d2"),
        "pastry_board": planks("#dcb184"),
    }
    for name, px in jobs.items():
        px.save(name)
    return len(jobs)


if __name__ == "__main__":
    print(build(), "16x16 food textures written")
