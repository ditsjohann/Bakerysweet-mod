"""
32x32 "picture" textures for decor and shop furniture: signs, chalkboard, mailbox, cash register, counter panel,
tip label, donut wall, pastry display, mini cakes, fruit crates and heaps, cookie jar, bunting, calm floor tiles,
and small skins (leaf, petal, lemon, coconut, stone). Run: python3 blockbench/gen_deco.py
"""
import math
import os
import random
import subprocess
import sys
import io

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb, text, FONT, TEX
from PIL import Image

S = 32
FONT.update({"P": "111101111100100", "D": "110101101101110", "I": "111010010010111", "M": "101111111101101", "U": "101101101101111",
             "R": "110101110101101", "K": "101101110101101", "H": "101101111101101", "F": "111100110100100", "Y": "101101010010010",
             "B": "110101110101110", "J": "001001001101111", ".": "000000000000010", "c": "000111100100111"})


def r(c, n=5, spread=0.24):
    return shades(c, n, spread, 0.02)


def big_text(cv, s, x, y, c, scale=2):
    for ch in s:
        g = FONT.get(ch)
        if g:
            for i, bit in enumerate(g):
                if bit == "1":
                    for dy in range(scale):
                        for dx in range(scale):
                            cv.set(x + (i % 3) * scale + dx, y + (i // 3) * scale + dy, c)
        x += 4 * scale - (scale - 1)
    return x


def width(s, scale=1):
    return len(s) * (4 * scale - (scale - 1)) - 1


def frame(cv, c_dark, c_light, inset=0):
    for i in range(inset, S - inset):
        cv.set(i, inset, c_light); cv.set(inset, i, c_light)
        cv.set(i, S - 1 - inset, c_dark); cv.set(S - 1 - inset, i, c_dark)


HEART = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]


def heart(cv, x, y, ramp):
    for j, row in enumerate(HEART):
        for i, ch in enumerate(row):
            if ch == "1":
                cv.set(x + i, y + j, ramp[3] if (j < 2 and i in (1, 4)) else ramp[2] if j < 4 else ramp[1])


# ---------------------------------------------------------------- signs and boards

def sign(word, board="#fff4ea", ink="#e0507e", scale=None):
    b = r(board, 5, 0.12)
    cv = Canvas(S, fill=b[3])
    pk = r(ink, 5, 0.3)
    frame(cv, pk[1], pk[2])
    frame(cv, b[2], b[4], 1)
    sc = scale or (2 if width(word, 2) <= 28 else 1)
    w = width(word, sc)
    big_text(cv, word, (S - w) // 2, (S - 5 * sc) // 2, pk[1], sc)
    heart(cv, 3, 3, pk) if sc == 1 else None
    return cv


def chalkboard():
    g = r("#33503f", 5, 0.12)
    cv = Canvas(S, fill=g[2])
    rnd = random.Random(7)
    for _ in range(60):
        cv.set(rnd.randrange(S), rnd.randrange(S), g[3])  # chalk dust
    chalk = (236, 240, 230)
    pink = rgb("#f6a8c4")
    text(cv, "MENU", 9, 3, chalk)
    cv.hline(8, 23, 9, (200, 210, 200))
    for i, (y, price) in enumerate(((12, "2"), (17, "3"), (22, "5"))):
        cv.hline(4, 18, y + 1, (190, 200, 190))
        cv.set(3, y + 1, pink)
        text(cv, price, 24, y - 1, chalk)
    # a little cupcake doodle
    for (x, y) in ((22, 26), (23, 27), (24, 27), (25, 27), (26, 26), (23, 28), (25, 28), (24, 29)):
        cv.set(x, y, pink)
    return cv


def open_sign():
    cv = sign("OPEN", "#ffffff", "#ea5a8a")
    return cv


# ---------------------------------------------------------------- shop furniture

def mailbox(front):
    p = r("#f2a0bc", 5, 0.2)
    cv = Canvas(S, fill=p[2])
    for y in range(S):
        for x in range(S):
            cv.set(x, y, p[3] if (x // 8) % 2 == 0 and front is False else p[2])
    if front:
        frame(cv, p[0], p[4])
        # mail slot and a heart emblem
        for x in range(8, 24):
            cv.set(x, 9, p[0]); cv.set(x, 10, (60, 30, 40)); cv.set(x, 11, p[4])
        heart(cv, 13, 17, r("#fff0f6", 5, 0.2))
        text(cv, "MAIL", 9, 25, p[0])
    return cv


def register_body():
    p = r("#a8dcc8", 5, 0.18)
    cv = Canvas(S, fill=p[2])
    frame(cv, p[0], p[4])
    for y in range(4, S, 6):
        cv.hline(2, S - 3, y, p[1])
    g = r("#e9b949", 4, 0.3)
    for x in range(10, 22):
        cv.set(x, 24, g[2]); cv.set(x, 25, g[1])
    return cv


def register_display():
    cv = Canvas(S, fill=rgb("#2c3a34"))
    frame(cv, rgb("#1a2420"), rgb("#4a5a52"))
    green = rgb("#8af0a0")
    big_text(cv, "12", 6, 9, green, 2)
    cv.set(19, 18, green)
    big_text(cv, "50", 21, 9, green, 1) if False else text(cv, "50", 21, 13, green)
    return cv


def register_keys():
    cv = Canvas(S, fill=rgb("#f6f0ea"))
    cols = [r("#ffffff", 4, 0.2), r("#f6a8c4", 4, 0.25), r("#a8dcc8", 4, 0.25)]
    for j in range(4):
        for i in range(4):
            c = cols[2] if (i == 3) else cols[1] if j == 0 else cols[0]
            x, y = 2 + i * 7, 2 + j * 7
            for yy in range(y, y + 6):
                for xx in range(x, x + 6):
                    k = 3 if yy == y or xx == x else 1 if yy == y + 5 or xx == x + 5 else 2
                    cv.set(xx, yy, c[k])
    return cv


def counter_panel():
    b = r("#f6e8d6", 5, 0.14)
    cv = Canvas(S, fill=b[3])
    pk = r("#f2a6c0", 5, 0.26)
    frame(cv, b[1], b[4])
    # raised moulding with a painted cupcake
    for i in range(4, S - 4):
        cv.set(i, 4, b[4]); cv.set(4, i, b[4]); cv.set(i, S - 5, b[1]); cv.set(S - 5, i, b[1])
    w = r("#f6a6c0", 4, 0.25)
    for y in range(17, 24):
        half = 4 - (y - 17) // 3
        for x in range(16 - half, 16 + half):
            cv.set(x, y, w[2] if x % 2 else w[1])
    for (cx, cy, rr) in ((16, 14, 4),):
        for y in range(cy - rr, cy + 2):
            for x in range(cx - rr - 1, cx + rr + 2):
                if (x - cx) ** 2 + (y - cy) ** 2 * 1.6 <= rr * rr + 2:
                    cv.set(x, y, r("#fff4f8", 4, 0.15)[2 if y > cy - 2 else 3])
    cv.set(16, 9, rgb("#e0405a")); cv.set(16, 8, rgb("#5a9a3e"))
    return cv


def tip_label():
    p = r("#fff8ee", 4, 0.1)
    cv = Canvas(S, fill=p[2])
    pk = r("#ea5a8a", 5, 0.3)
    frame(cv, pk[1], pk[2])
    # The jar shows the middle of the label (x 4..28, y 8..24), so everything sits inside that.
    for y in range(8, 24):
        for x in range(4, 28):
            if x in (4, 27) or y in (8, 23):
                cv.set(x, y, pk[2])
    text(cv, "TIPS", 9, 11, pk[0])
    heart(cv, 13, 17, pk)
    return cv


# ---------------------------------------------------------------- food pictures

def donut(cv, cx, cy, icing, sprinkles=True, rad=5.5):
    d = r("#d8984e", 5, 0.26)
    ic = r(icing, 5, 0.24)
    rnd = random.Random(int(cx * 31 + cy))
    for y in range(int(cy - rad - 1), int(cy + rad + 2)):
        for x in range(int(cx - rad - 1), int(cx + rad + 2)):
            dist = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if 1.8 < dist <= rad:
                if dist < rad - 1.2 and not (y > cy + rad * 0.45):
                    cv.set(x, y, ic[3] if (x - cx) + (y - cy) < 0 else ic[2])
                else:
                    cv.set(x, y, d[2] if y < cy else d[1])
    if sprinkles:
        cols = [rgb(c) for c in ("#ffd84a", "#7ad0ff", "#ffffff", "#9af07a")]
        for _ in range(7):
            a, dd = rnd.uniform(0, 6.28), rnd.uniform(2.4, rad - 1.6)
            cv.set(cx + math.cos(a) * dd, cy + math.sin(a) * dd, rnd.choice(cols))


def donut_wall():
    p = r("#b2dcc8", 5, 0.12)
    cv = Canvas(S, fill=p[3])
    for y in range(3, S, 5):
        for x in range(3, S, 5):
            cv.set(x, y, p[1])  # pegboard holes
    for (cx, cy, ic) in ((9, 9, "#f6a8c4"), (23, 9, "#7a4a32"), (9, 23, "#fff6ee"), (23, 23, "#c8a8f0")):
        cv.set(cx, cy - 6, r("#c8a070", 4)[1])
        donut(cv, cx, cy, ic)
    return cv


def pastry_display():
    """The front glass of the pastry cart: three trays of treats."""
    cv = Canvas(S, fill=rgb("#fff6f0"))
    tray = r("#e9e2dc", 4, 0.2)
    for row, y in enumerate((9, 19, 29)):
        cv.hline(0, S - 1, y, tray[1]); cv.hline(0, S - 1, y + 1, tray[2])
        for i, x in enumerate(range(3, S, 7)):
            kind = (row + i) % 3
            if kind == 0:  # cupcake
                w = r("#f6a6c0", 4, 0.25)
                for yy in range(y - 4, y):
                    for xx in range(x - 2, x + 3):
                        cv.set(xx, yy, w[2] if xx % 2 else w[1])
                for xx in range(x - 3, x + 4):
                    cv.set(xx, y - 5, r("#fff4f8", 4)[2]); cv.set(xx, y - 6, r("#fff4f8", 4)[3])
                cv.set(x, y - 8, rgb("#e0405a"))
                cv.hline(x - 2, x + 2, y - 7, r("#fff4f8", 4)[3])
            elif kind == 1:  # croissant
                c = r("#e0a24e", 4, 0.3)
                for t in range(-3, 4):
                    hh = 3 - abs(t) // 2
                    for yy in range(y - hh, y):
                        cv.set(x + t, yy, c[2 if (t + yy) % 2 else 3])
            else:  # macaron stack
                for j, col in enumerate(("#f6a8c4", "#b8e4d0", "#d8c0f0")):
                    m = r(col, 4, 0.2)
                    cv.hline(x - 2, x + 2, y - 2 - j * 3, m[2]); cv.hline(x - 2, x + 2, y - 1 - j * 3, m[1])
    return cv


def mini_cakes():
    cv = Canvas(S)
    for i, (x, col) in enumerate(((5, "#f6a8c4"), (16, "#fff6ee"), (27, "#b8e4d0"))):
        f = r(col, 5, 0.22)
        sp = r("#f0d6a6", 4, 0.2)
        for y in range(18, 28):
            for xx in range(x - 4, x + 5):
                cv.set(xx, y, f[2] if y < 21 else sp[2] if y % 4 else f[3])
        for xx in range(x - 4, x + 5, 2):
            cv.set(xx, 17, f[4])
        cv.disc(x, 15, 1.4, r("#e0405a", 5, 0.3))
        cv.hline(x - 5, x + 5, 28, r("#ffffff", 4)[2])
    return cv


def crate_front(fruit_color, leaf=True):
    w = r("#c99a62", 5, 0.24)
    cv = Canvas(S, fill=w[2])
    for y in range(S):
        for x in range(S):
            k = 2 if (y // 8) % 2 == 0 else 3
            if y % 8 == 0:
                k = 0
            cv.set(x, y, w[k])
    frame(cv, w[0], w[4])
    # a painted fruit label in the middle
    lab = r("#fff6e8", 4, 0.1)
    cv.round_rect(7, 8, 24, 23, lab[2], w[1])
    cv.disc(15.5, 16.5, 4.6, r(fruit_color, 5, 0.32))
    if leaf:
        lf = r("#5aa64e", 4, 0.3)
        cv.set(16, 11, lf[2]); cv.set(17, 10, lf[3]); cv.set(18, 10, lf[2])
    return cv


def pile(fruit_color, seed, size=3.2, seeds=False):
    """A crate of fruit seen from above: a heap of round fruits, each shaded."""
    cv = Canvas(S, fill=r("#7a5232", 4, 0.2)[1])
    rnd = random.Random(seed)
    spots = [(x + rnd.uniform(-1, 1), y + rnd.uniform(-1, 1)) for y in range(3, S, 6) for x in range(3 + (y // 6) % 2 * 3, S, 6)]
    for (x, y) in spots:
        cv.disc(x, y, size, r(fruit_color, 5, 0.32))
        if seeds:
            cv.set(x + 1, y + 1, rgb("#ffe08a")); cv.set(x - 1, y, rgb("#ffe08a"))
            cv.set(x, y - size + 0.5, rgb("#5aa64e"))
    return cv


def cookie_jar_cookies():
    cv = Canvas(S, fill=r("#d9a25a", 4, 0.2)[1])
    rnd = random.Random(3)
    for y in range(2, S, 6):
        for x in range(2 + (y // 6) % 2 * 4, S, 8):
            c = r("#d9a25a", 5, 0.26)
            for yy in range(y, y + 5):
                for xx in range(x, x + 7):
                    if (xx - x - 3) ** 2 / 12 + (yy - y - 2) ** 2 / 5 <= 1:
                        cv.set(xx, yy, c[3] if yy < y + 2 else c[2])
            cv.set(x + 2, y + 2, rgb("#5a3222")); cv.set(x + 4, y + 1, rgb("#5a3222"))
    return cv


def cookie_jar_lid():
    p = r("#f6b0c8", 5, 0.2)
    cv = Canvas(S, fill=p[2])
    for y in range(S):
        for x in range(S):
            if (x + y) % 7 == 0:
                cv.set(x, y, p[3])
    heart(cv, 12, 12, r("#ffffff", 5, 0.2))
    return cv


def bunting():
    cv = Canvas(S)
    cols = ["#f6a8c4", "#fff0a8", "#b8e4d0", "#d8c0f0"]
    string = rgb("#f6f0ea")
    for x in range(S):
        y = 2 + int(math.sin(x / S * math.pi) * 2)
        cv.set(x, y, string)
    for i in range(4):
        x0 = i * 8
        f = r(cols[i], 4, 0.2)
        top = 3 + int(math.sin((x0 + 4) / S * math.pi) * 2)
        for j in range(12):
            half = int(3.5 * (1 - j / 12))
            for x in range(x0 + 4 - half, x0 + 4 + half):
                cv.set(x, top + j, f[2] if x < x0 + 4 else f[1])
        cv.set(x0 + 3, top + 2, f[3])
    return cv


def tiles(c1, c2=None, size=8, grout="#f2ebe4"):
    """Calm glazed tiles: flat colour, a thin grout line, one soft highlight pixel per tile."""
    a, b = r(c1, 5, 0.12), r(c2 or c1, 5, 0.12)
    g = rgb(grout)
    cv = Canvas(S)
    for y in range(S):
        for x in range(S):
            p = a if ((x // size + y // size) % 2 == 0) else b
            u, v = x % size, y % size
            c = p[2]
            if u == 0 or v == 0:
                c = g
            elif u == 1 and v == 1:
                c = p[4]
            elif u == size - 1 or v == size - 1:
                c = p[1]
            cv.set(x, y, c)
    return cv


def skin(color, seed, kind="smooth"):
    p = r(color, 5, 0.22)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[2])
    for y in range(S):
        for x in range(S):
            k = 3 if (x * 7 + y * 3) % 11 == 0 else 2
            cv.set(x, y, p[k])
    if kind == "veins":
        for y in range(S):
            cv.set(15, y, p[4])
        for i in range(0, S, 6):
            for t in range(8):
                cv.set(15 - t, i + t // 2, p[3]); cv.set(15 + t, i + t // 2, p[3])
    elif kind == "dimples":
        for _ in range(60):
            cv.set(rnd.randrange(S), rnd.randrange(S), p[1])
    elif kind == "fibres":
        for x in range(S):
            for y in range(S):
                if (x * 3 + y) % 5 == 0:
                    cv.set(x, y, p[1])
    elif kind == "speckle":
        for _ in range(40):
            cv.set(rnd.randrange(S), rnd.randrange(S), p[1] if rnd.random() < 0.5 else p[3])
    elif kind == "petal":
        for y in range(S):
            for x in range(S):
                if (x - 16) ** 2 + (y - 26) ** 2 < 30:
                    cv.set(x, y, p[3])
    return cv


def flour_label():
    """A burlap sack with a printed label: a wheat sprig and FLOUR."""
    import gen_materials
    cv = gen_materials.sack(70)
    lab = r("#fff6e8", 4, 0.1)
    pk = r("#e0507e", 5, 0.3)
    cv.round_rect(5, 9, 26, 24, lab[2], pk[2])
    text(cv, "FLOUR", 7, 18, pk[0])
    wheat = r("#e2b450", 4, 0.3)
    for t in range(6):
        cv.set(15, 11 + t, wheat[1])
        if t < 4:
            cv.set(14, 11 + t, wheat[3]); cv.set(16, 12 + t, wheat[2])
    return cv


def original(name):
    """The original 16px art of a texture, doubled with EPX (for ones the original drew best)."""
    sys.path.insert(0, os.path.dirname(__file__))
    from upscale_epx import epx
    import numpy as np
    data = subprocess.run(["git", "show", "a552c8a:src/main/resources/assets/bakery/textures/block/" + name + ".png"],
                          capture_output=True, check=True, cwd=os.path.dirname(__file__)).stdout
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    return Image.fromarray(epx(np.array(im)))


def build():
    jobs = {
        "chalkboard": chalkboard(), "open_sign": open_sign(), "sign_open": sign("OPEN"), "sign_closed": sign("CLOSED", "#f2ece6", "#8a7a86"),
        "mailbox": mailbox(False), "mailbox_front": mailbox(True), "register_body": register_body(), "register": register_body(),
        "register_display": register_display(), "register_keys": register_keys(), "counter_panel": counter_panel(), "tip_label": tip_label(),
        "donut_wall": donut_wall(), "pastry_display": pastry_display(), "mini_cakes": mini_cakes(),
        "crate_strawberry": crate_front("#e8394d"), "crate_lemon": crate_front("#f6dc4a"), "crate_coconut": crate_front("#8a5a3a", leaf=False),
        "pile_strawberry": pile("#e8394d", 1, 2.8, seeds=True), "pile_lemon": pile("#f6dc4a", 2, 3.2), "pile_coconut": pile("#8a5a3a", 3, 3.4),
        "macaron_pouf_shell": skin("#f6a8c4", 9, "speckle"), "flour_sack_label": flour_label(),
        "cookie_jar_cookies": cookie_jar_cookies(), "cookie_jar_lid": cookie_jar_lid(), "bunting": bunting(),
        "pink_checker_tiles": tiles("#f8c6d4", "#fff4f6"), "mint_checker_tiles": tiles("#b6e2d0", "#f4fffa"),
        "lilac_checker_tiles": tiles("#d6c2ee", "#faf6ff"), "butter_checker_tiles": tiles("#f8e2a2", "#fffcf0"),
        "pink_tiles": tiles("#f6bccf"), "mint_tiles": tiles("#b0e2cc"), "lavender_tiles": tiles("#cdb8ea"), "cream_tiles": tiles("#f6ead6", size=16),
        "leaf": skin("#5aa64e", 1, "veins"), "petal": skin("#f6a8c4", 2, "petal"), "lemon_skin": skin("#f6dc4a", 3, "dimples"),
        "lemon_peel": skin("#f8e46a", 4, "dimples"), "lemon_young": skin("#a8d05a", 5, "dimples"), "coconut_shell": skin("#7a5232", 6, "fibres"),
        "coconut_young": skin("#7ab05a", 7), "stone_cream": skin("#ece2d4", 8, "speckle"),
    }
    for name, cv in jobs.items():
        cv.save("block/" + name)
    # The original tulips were fuller: keep their art, smoothed to 32px.
    original("tulips").save(os.path.join(TEX, "block/tulips.png"))
    return len(jobs) + 1


if __name__ == "__main__":
    print(build(), "deco textures written")
