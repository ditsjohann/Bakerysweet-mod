"""
32x32 food surfaces: crusts, crumb, dough, icing, sponge, wrappers, pie, macarons, toppings.

Most of these dress small 3D pieces (a loaf, a berry, a slice), so each is an even surface with the detail of the
real thing. Textures the game tints with the flavour colour (frosting, glaze, chips, macaron shells, pie filling,
ice cream, drinks) are drawn light so the flavour shows. Run: python3 blockbench/gen_food.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb

S = 32


def r(c, n=5, spread=0.24):
    return shades(c, n, spread, 0.02)


def noise_fill(cv, p, seed, levels=(1, 2, 2, 3), scale=0.35):
    rnd = random.Random(seed)
    ph = rnd.uniform(0, 6)
    for y in range(S):
        for x in range(S):
            v = math.sin(x * scale + ph) * math.sin(y * scale * 1.3 + ph * 2) + rnd.uniform(-0.6, 0.6)
            k = levels[min(len(levels) - 1, max(0, int((v + 1.6) / 3.2 * len(levels))))]
            cv.set(x, y, p[k])


def crust(color, seed, scores=True, flour=True, diagonal=False):
    """Baked crust: mottled gold-brown, a few scored slashes (paler where the bread opened), a dusting of flour."""
    p = r(color, 5, 0.26)
    rnd = random.Random(seed)
    cv = Canvas(S)
    noise_fill(cv, p, seed, (1, 2, 2, 3))
    if scores:
        for i in range(3):
            y0 = 6 + i * 10
            for t in range(14):
                x = 9 + t + (t * 0.6 if diagonal else 0)
                y = y0 + (t * 0.5 if diagonal else math.sin(t * 0.25) * 1.2)
                cv.set(x, y, p[4])
                cv.set(x, y + 1, p[0])
    if flour:
        for _ in range(28):
            cv.set(rnd.randrange(S), rnd.randrange(S), (250, 244, 232))
    return cv


def crumb(seed):
    """The cut face of bread: soft pale crumb with airy holes, each shadowed at the top."""
    p = r("#f2e2c2", 5, 0.2)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[3])
    for _ in range(46):
        x, y, s = rnd.randrange(S), rnd.randrange(S), rnd.choice((1, 1, 2))
        for dx in range(s):
            cv.set(x + dx, y, p[0])
            cv.set(x + dx, y + 1, p[1])
            cv.set(x + dx, y - 1, p[4])
    return cv


def croissant(seed):
    """Laminated layers: curved golden bands, darker in the folds."""
    p = r("#e0a24e", 5, 0.3)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            band = (y + math.sin(x * 0.2) * 3) % 6
            k = [1, 2, 3, 3, 4, 2][int(band)]
            cv.set(x, y, p[k])
    return cv


def cookie(seed):
    """A baked cookie: golden with darker edges of little cracks and a sparkle of sugar."""
    p = r("#d9a25a", 5, 0.26)
    rnd = random.Random(seed)
    cv = Canvas(S)
    noise_fill(cv, p, seed, (2, 2, 3, 3))
    for _ in range(9):
        x, y = rnd.randrange(S), rnd.randrange(S)
        for t in range(rnd.randint(3, 6)):
            cv.set(x + t, y + rnd.choice((-1, 0, 0, 1)), p[1])
    for _ in range(14):
        cv.set(rnd.randrange(S), rnd.randrange(S), p[4])
    return cv


def smooth(color, seed, bubbles=False, swirl=False, spread=0.18):
    p = r(color, 5, spread)
    rnd = random.Random(seed)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            k = 2
            if swirl:
                d = math.hypot(x - 16, y - 16)
                k = 3 if int(d * 0.7 + math.atan2(y - 16, x - 16) * 1.5) % 3 == 0 else 2
            elif (x * 5 + y * 3) % 13 == 0:
                k = 3
            cv.set(x, y, p[k])
    if bubbles:
        for _ in range(12):
            x, y = rnd.randrange(S), rnd.randrange(S)
            cv.set(x, y, p[0]); cv.set(x + 1, y, p[1]); cv.set(x, y - 1, p[4])
    return cv


def icing(seed, ridged=False):
    """Light icing for tinting: piped ridges (frosting) or a smooth glossy coat (glaze)."""
    p = r("#ffffff", 5, 0.32)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            if ridged:
                v = (x + math.sin(y * 0.4) * 2) % 5
                k = [1, 2, 3, 4, 3][int(v)]
            else:
                k = 3 if (x * 3 + y * 7) % 19 else 4
                if (x + y * 2) % 29 == 0:
                    k = 2
            cv.set(x, y, p[k])
    return cv


def sponge(seed, layers=False):
    """Cake/cupcake sponge: soft crumb; with cream layers for cake sides."""
    p = r("#f0d6a6", 5, 0.22)
    rnd = random.Random(seed)
    cv = Canvas(S)
    noise_fill(cv, p, seed, (2, 3, 3, 3))
    for _ in range(30):
        x, y = rnd.randrange(S), rnd.randrange(S)
        cv.set(x, y, p[1]); cv.set(x, y - 1, p[4])
    if layers:
        cream = r("#fff8f0", 4, 0.12)
        for fy in (9, 20):
            for y in range(fy, fy + 3):
                for x in range(S):
                    cv.set(x, y, cream[3 if y == fy else 2])
    return cv


def wrapper(color, seed):
    """A pleated cupcake paper: soft vertical pleats."""
    p = r(color, 5, 0.22)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            k = [3, 3, 2, 1][x % 4]
            cv.set(x, y, p[k])
    return cv


def pie_crust(seed):
    p = r("#e6b066", 5, 0.26)
    cv = Canvas(S)
    noise_fill(cv, p, seed, (2, 2, 3, 3))
    # fork-pressed rim marks and a lattice of strips
    for x in range(0, S, 4):
        for y in (1, 2):
            cv.set(x, y, p[1])
    for i in range(-S, S, 8):
        for t in range(S):
            cv.set(i + t, t, p[4]); cv.set(i + t + 1, t, p[3])
    return cv


def berry_skin(color, seed, kind):
    p = r(color, 5, 0.28)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[2])
    for y in range(S):
        for x in range(S):
            cv.set(x, y, p[3] if (x * 5 + y * 3) % 7 == 0 else p[2])
    if kind == "seeds":
        for y in range(1, S, 4):
            for x in ((y // 4) % 2 * 2 + 1, ):
                for xx in range(x, S, 4):
                    cv.set(xx, y, rgb("#ffe08a")); cv.set(xx, y + 1, p[1])
    elif kind == "drupelets":
        for y in range(0, S, 3):
            for x in range((y // 3) % 2, S, 3):
                cv.set(x, y, p[4]); cv.set(x + 1, y + 1, p[1])
    elif kind == "bloom":
        for _ in range(70):
            cv.set(rnd.randrange(S), rnd.randrange(S), p[4])
    elif kind == "gloss":
        for _ in range(16):
            x, y = rnd.randrange(S), rnd.randrange(S)
            cv.set(x, y, p[4])
    elif kind == "veins":
        for y in range(S):
            cv.set(16 + math.sin(y * 0.2) * 2, y, p[4])
        for i in range(0, S, 5):
            for t in range(6):
                cv.set(16 - t, i + t * 0.6, p[3]); cv.set(16 + t, i + t * 0.6, p[3])
    elif kind == "segments":
        for y in range(S):
            for x in range(S):
                if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                    cv.set(x, y, rgb("#fff6c8"))
    elif kind == "buds":
        for y in range(0, S, 2):
            for x in range((y // 2) % 2, S, 2):
                cv.set(x, y, p[3] if (x + y) % 4 else p[1])
    elif kind == "sparkle":
        for _ in range(20):
            x, y = rnd.randrange(S), rnd.randrange(S)
            cv.set(x, y, (255, 255, 255)); cv.set(x + 1, y, p[4])
    return cv


def overlay(color, seed, kind):
    """Transparent topping overlays: chocolate shavings, coconut flakes, rose petals, sprinkles."""
    cv = Canvas(S)
    rnd = random.Random(seed)
    if kind == "sprinkles":
        cols = [rgb(c) for c in ("#ff6fa0", "#ffd84a", "#7ad0ff", "#9af07a", "#ffffff", "#c890f0")]
        for _ in range(40):
            x, y = rnd.randrange(S), rnd.randrange(S)
            c = rnd.choice(cols)
            dx, dy = rnd.choice(((1, 0), (0, 1), (1, 1)))
            cv.set(x, y, c); cv.set(x + dx, y + dy, tuple(int(v * 0.8) for v in c))
        return cv
    p = r(color, 5, 0.28)
    for _ in range(14 if kind == "petals" else 22):
        x, y = rnd.randrange(S), rnd.randrange(S)
        length = rnd.randint(3, 5)
        for t in range(length):
            if kind == "petals":
                for w in range(-1, 2):
                    cv.set(x + t, y + w + (t // 2), p[3 if w < 0 else 2])
            else:
                cv.set(x + t, y + (t % 2), p[3 if t % 2 else 2])
                cv.set(x + t, y + 1 + (t % 2), p[1])
    return cv


def ice_cream(seed):
    """A scoop's surface: soft bumps and little curls, light for tinting."""
    p = r("#ffffff", 5, 0.3)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            v = math.sin(x * 0.7) + math.sin(y * 0.6 + x * 0.2)
            cv.set(x, y, p[4 if v > 1.2 else 3 if v > -0.3 else 2])
    return cv


def coins(seed):
    """A pile of Sweet Coins: overlapping gold discs with a stamped heart."""
    g = r("#e9b949", 5, 0.32)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=g[1])
    for _ in range(26):
        cx, cy = rnd.randrange(S), rnd.randrange(S)
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                d = math.hypot(dx, dy)
                if d <= 3.2:
                    k = 0 if d > 2.6 else 4 if dx + dy < -2 else 3 if dx + dy < 1 else 2
                    cv.set((cx + dx) % S, (cy + dy) % S, g[k])
        cv.set(cx % S, cy % S, rgb("#f06a98"))
    return cv


def tea(seed):
    p = r("#b8702e", 5, 0.26)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 16, y - 16)
            cv.set(x, y, p[3] if int(d) % 5 == 0 else p[2])
    return cv


def build():
    jobs = {
        "bread_crust": crust("#c8853e", 1), "bread_bottom": crust("#d8a868", 2, scores=False), "bread_crumb": crumb(3),
        "baguette_crust": crust("#c98a42", 4, diagonal=True), "croissant": croissant(5), "cookie": cookie(6),
        "chip": smooth("#f0f0f0", 7, spread=0.3), "dough": smooth("#f4e6cc", 8), "proof_dough": smooth("#f2e2c4", 9, bubbles=True),
        "batter": smooth("#f6e2b4", 10, swirl=True), "donut_dough": crust("#d6984e", 11, scores=False, flour=False),
        "glaze": icing(12), "frosting": icing(13, ridged=True), "cake_frosting": icing(14), "cake_piping": icing(15, ridged=True),
        "sponge": sponge(16), "cake_sponge": sponge(17), "cake_side": sponge(18, layers=True), "cake_inside": sponge(19, layers=True),
        "cake_bottom": sponge(20), "cupcake_wrapper": wrapper("#f6a6c0", 21), "pie_crust": pie_crust(22),
        "pie_filling": smooth("#ffffff", 23, bubbles=True, spread=0.3), "pie_inside": sponge(24),
        "macaron_shell": smooth("#ffffff", 25, spread=0.22), "macaron_filling": smooth("#fff4ee", 26, swirl=True),
        "macaron_cream": icing(27, ridged=True), "chocolate": berry_skin("#5a3222", 28, "gloss"), "cherry": berry_skin("#d02840", 29, "gloss"),
        "topping_strawberry": berry_skin("#e8394d", 30, "seeds"), "topping_blueberry": berry_skin("#5a6ad8", 31, "bloom"),
        "topping_raspberry": berry_skin("#e0406a", 32, "drupelets"), "topping_mint": berry_skin("#5ac078", 33, "veins"),
        "topping_lavender": berry_skin("#a87ad8", 34, "buds"), "topping_star": berry_skin("#f6d65a", 35, "sparkle"),
        "topping_cream": icing(36, ridged=True), "topping_lemon": berry_skin("#f6dc4a", 37, "segments"),
        "topping_chocolate": overlay("#5a3222", 38, "shavings"), "topping_coconut": overlay("#fffaf2", 39, "shavings"),
        "topping_rose": overlay("#f06a8a", 40, "petals"), "sprinkle_overlay": overlay("#ffffff", 41, "sprinkles"),
        "ice_cream_block": ice_cream(42), "liquid": icing(43), "coin_pile": coins(44), "tea": tea(45),
        "cupcake_lamp_frosting": icing(46, ridged=True), "cupcake_lamp_cherry": berry_skin("#e0405a", 47, "gloss"),
    }
    for name, cv in jobs.items():
        cv.save("block/" + name)
    return len(jobs)


if __name__ == "__main__":
    print(build(), "food textures written")
