"""The Bakery Catalog block: a pink lectern holding a big open mail-order catalog with illustrated pages."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb, text, TEX
from round_model import Model
from PIL import Image

ITEMS = os.path.join(TEX, "item")
PAPER, PAPER_D, INK = rgb("#fff7ec"), rgb("#f1e2cc"), rgb("#c98aa0")
PINK = shades("#ef7fa6", 5, 0.32)
GOLD = shades("#e9b949", 5, 0.36)


def icon(name):
    return Image.open(os.path.join(ITEMS, name + ".png")).convert("RGBA")


def coin(cv, x, y):
    """A 6x6 gold Sweet Coin with a dark rim, a shine and a little heart stamped in."""
    art = [".oooo.", "ohllgo", "olggdo", "ogggdo", "oggddo", ".oooo."]
    cols = {"o": GOLD[0], "h": GOLD[4], "l": GOLD[3], "g": GOLD[2], "d": GOLD[1]}
    for j, row in enumerate(art):
        for i, ch in enumerate(row):
            if ch in cols:
                cv.set(x + i, y + j, cols[ch])
    cv.set(x + 2, y + 2, PINK[2]); cv.set(x + 3, y + 2, PINK[2]); cv.set(x + 2, y + 3, PINK[1]); cv.set(x + 3, y + 3, PINK[1])


def page(cv, x0, items, header):
    """One page (26 x 46 px at 4 px per model pixel): a pink banner, two product cards, and lines of text."""
    w = 26
    cv.rect(x0, 0, x0 + w - 1, 45, PAPER)
    for y in range(0, 46, 3):
        cv.set(x0 + (y * 7) % w, y, PAPER_D)
    cv.rect(x0 + 1, 1, x0 + w - 2, 6, PINK[3])
    cv.hline(x0 + 1, x0 + w - 2, 7, PINK[1])
    for x in range(x0 + 1, x0 + w - 1, 3):
        cv.set(x, 7, PINK[3])
    text(cv, header, x0 + 3, 1, (255, 255, 255))
    y = 10
    for name, price in items:
        cv.round_rect(x0 + 1, y, x0 + w - 2, y + 17, (255, 255, 255), rgb("#f4c6d6"))
        cv.paste(icon(name), x0 + 2, y + 1)
        # price tag: a gold coin and the price
        coin(cv, x0 + 18, y + 2)
        text(cv, str(price), x0 + 18, y + 10, PINK[0])
        y += 19
    for ly in (y, y + 3):
        if ly < 45:
            cv.hline(x0 + 3, x0 + w - 5, ly, INK)


def spread():
    cv = Canvas(64)
    page(cv, 0, [("cupcake", 6), ("cake", 9)], "SWEET")
    page(cv, 32, [("rolling_pin", 4), ("strawberry", 3)], "NEW!")
    # page curl shading toward the spine
    for y in range(46):
        cv.set(25, y, PAPER_D)
        cv.set(32, y, PAPER_D)
    cv.save("block/catalog_spread")


def cover():
    cv = Canvas(32, fill=PINK[2])
    for y in range(32):
        for x in range(32):
            if (x + y) % 2 == 0:
                cv.set(x, y, PINK[2] if (x * 3 + y) % 7 else PINK[1])
    for i in range(32):
        cv.set(i, 0, PINK[0]); cv.set(i, 31, PINK[0]); cv.set(0, i, PINK[0]); cv.set(31, i, PINK[0])
    for i in range(2, 30):
        cv.set(i, 2, GOLD[3]); cv.set(i, 29, GOLD[2]); cv.set(2, i, GOLD[3]); cv.set(29, i, GOLD[2])
    cv.save("block/catalog_cover")


def edges():
    cv = Canvas(32, fill=PAPER)
    for y in range(0, 32, 2):
        cv.hline(0, 31, y, PAPER_D)
    cv.save("block/catalog_edges")


def wood():
    """Smooth painted wood: soft vertical grain, no harsh lines."""
    base = shades("#f3b6c9", 6, 0.22, 0.02)
    cv = Canvas(32, fill=base[3])
    for x in range(32):
        for y in range(32):
            g = ((x * 5 + (y // 7) * 3) % 11 == 0)
            k = 2 if g else 3
            if (x + y * 3) % 17 == 0:
                k = 4
            cv.set(x, y, base[k])
    cv.save("block/catalog_wood")


def plaque():
    cv = Canvas(32, fill=GOLD[2])
    for i in range(32):
        cv.set(i, 0, GOLD[0]); cv.set(i, 31, GOLD[0]); cv.set(0, i, GOLD[0]); cv.set(31, i, GOLD[0])
        cv.set(i, 1, GOLD[4]); cv.set(1, i, GOLD[4])
    cv.round_rect(9, 8, 22, 23, rgb("#fff6ee"), GOLD[1])
    heart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
    for j, row in enumerate(heart):
        for i, ch in enumerate(row):
            if ch == "1":
                cv.set(12 + i, 12 + j, PINK[3] if j < 2 and i in (1, 4) else PINK[2] if j < 4 else PINK[1])
    cv.save("block/catalog_plaque")


def model():
    m = Model({"wood": "bakery:block/catalog_wood", "gold": "bakery:block/metal_gold", "cover": "bakery:block/catalog_cover",
               "pages": "bakery:block/catalog_spread", "edges": "bakery:block/catalog_edges", "plaque": "bakery:block/catalog_plaque",
               "particle": "bakery:block/catalog_cover"})
    # Round foot, gold trims, a turned column
    m.disc("foot", 4.8, 0, 1.0, "wood")
    m.disc("foot_trim", 4.2, 1.0, 1.5, "gold")
    m.disc("base", 3.2, 1.5, 2.6, "wood")
    m.disc("column", 1.7, 2.6, 9.0, "wood")
    m.disc("band_low", 2.1, 3.4, 4.0, "gold")
    m.disc("band_high", 2.1, 7.6, 8.2, "gold")
    m.disc("neck", 2.6, 8.8, 10.0, "wood")
    plaque = m.box("plaque", (5.8, 5.0, 5.9), (10.2, 6.8, 6.2), "plaque", faces=["north"])
    plaque["faces"]["north"]["uv"] = [0, 4, 16, 12]
    # The reading desk, tilted toward the reader (front is north / -z)
    rot = {"angle": -22.5, "axis": "x", "origin": [8, 10.5, 8]}
    m.box("desk", (1.0, 10.0, 2.0), (15.0, 11.0, 14.5), "wood", rotation=rot)
    m.box("desk_lip", (1.0, 11.0, 1.4), (15.0, 12.2, 2.4), "wood", rotation=rot)
    # The open catalog: cover underneath, two page blocks, the spine, a ribbon
    m.box("cover", (1.4, 11.0, 2.6), (14.6, 11.4, 14.2), "cover", rotation=rot)
    left = m.box("pages_left", (1.8, 11.4, 2.8), (8.0, 12.6, 14.0), "edges", rotation=rot)
    right = m.box("pages_right", (8.0, 11.4, 2.8), (14.2, 12.6, 14.0), "edges", rotation=rot)
    # The page art: a 64px texture, 4 texels per pixel; left page at u 0..6.5, right page at u 8..14.5.
    # The reader stands north looking south: turn the art round so its top is the far edge, and the first page
    # (u 0..6.5) lands on the reader's left (east, +x).
    left["faces"]["up"] = {"uv": [8.3, 0, 14.5, 11.2], "texture": "#pages", "rotation": 180}
    right["faces"]["up"] = {"uv": [0, 0, 6.2, 11.2], "texture": "#pages", "rotation": 180}
    m.box("spine_dip", (7.7, 12.55, 2.8), (8.3, 12.62, 14.0), "edges", faces=["up"], rotation=rot)
    ribbon = m.box("ribbon", (11.0, 9.6, 2.2), (11.7, 12.65, 2.8), "cover", faces=["north", "south", "up"], rotation=rot)
    m.save(os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/models/block/bakery_catalog.json"))


spread(); cover(); edges(); wood(); plaque(); model()
print("catalog done")
