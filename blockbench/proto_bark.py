"""
Bark style prototypes, built on what the research said works (Faithful 32x guidelines, Slynyrd tile tutorials):
- few colours (5-6), and the darkest never touches the lightest: there's always a step between
- bark is drawn ALONG the grain: long vertical streaks, transitions as linear dithering / anti-aliasing
- 1px crack lines, not flat 2px lines; repeated key clusters, never touching each other; negative space
- clusters that loop over the edges so the block tiles seamlessly; balanced visual weight

A: soft vanilla-plus  - irregular 1px vertical cracks with a lit edge, along-the-grain streaks
B: birch-style cute   - pale bark with dark horizontal "eye" marks (very readable, very cute)
C: cherry-style bands - smooth bark with soft horizontal bands (like vanilla cherry)
D: round trunk        - shaded like a cylinder (dark sides, light middle) with a few soft cracks
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixel import Canvas, shades, rgb
from PIL import Image, ImageDraw

S = 32


def pal(c):
    # 6 steps, hue-shifted (cool shadows, warm lights); index 0 darkest.
    return shades(c, 6, 0.42, 0.045)


def wrap(cv, x, y, c):
    cv.set(int(x) % S, int(y) % S, c)


def style_a(c, seed):
    p = pal(c)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[3])
    # calm field: a few long soft bands along the grain (clusters, not speckles)
    for i in range(4):
        x0 = i * 8 + 5
        for y in range(S):
            wrap(cv, x0 + math.sin(y * 0.18 + i) * 1.0, y, p[4])
    # cracks: 1px dark lines that wander a little, a mid shadow beside them and a lit edge on the other side
    for i in range(5):
        x = i * 6.4 + 2 + rnd.uniform(-0.8, 0.8)
        y0, l = rnd.randrange(S), rnd.randint(11, 19)
        for t in range(l):
            xx = x + math.sin((y0 + t) * 0.25 + i) * 0.8
            wrap(cv, xx, y0 + t, p[1] if 0 < t < l - 1 else p[2])
            wrap(cv, xx + 1, y0 + t, p[2])
            wrap(cv, xx - 1, y0 + t, p[4])
            if 2 < t < l - 3:
                wrap(cv, xx, y0 + t, p[0] if t % 5 else p[1])
    return cv


def style_b(c, seed):
    p = pal(c)
    dark = shades("#4a3a44", 4, 0.3, 0.02)
    rnd = random.Random(seed)
    cv = Canvas(S, fill=p[4])
    for i in range(3):
        x0 = i * 11 + 4
        for y in range(S):
            wrap(cv, x0 + math.sin(y * 0.15 + i) * 1.2, y, p[3])
    # cute horizontal "eye" marks: a dark dash with a soft shadow under and light above
    marks = [(3, 4, 7), (19, 9, 5), (9, 17, 9), (24, 22, 6), (2, 27, 5), (14, 29, 4)]
    for (x, y, l) in marks:
        for t in range(l):
            wrap(cv, x + t, y, dark[1] if 0 < t < l - 1 else dark[2])
            wrap(cv, x + t, y + 1, p[2])
            wrap(cv, x + t, y - 1, p[5])
    return cv


def style_c(c, seed):
    p = pal(c)
    rnd = random.Random(seed)
    cv = Canvas(S)
    for y in range(S):
        band = (y % 11)
        k = 3
        if band in (0,):
            k = 2
        elif band in (1,):
            k = 4
        for x in range(S):
            cv.set(x, y, p[k])

    # small lenticels: light dash over a dark pixel
    for (x, y) in ((5, 5), (20, 8), (12, 18), (27, 21), (4, 27)):
        wrap(cv, x, y, p[5]); wrap(cv, x + 1, y, p[5]); wrap(cv, x, y + 1, p[1])
    return cv


def style_d(c, seed):
    p = pal(c)
    rnd = random.Random(seed)
    cv = Canvas(S)
    # cylinder shading across the block: darker sides, light just left of centre
    for x in range(S):
        t = ((x + 0.5) % 8) / 8
        v = math.sin(t * math.pi) * 0.75 + (0.12 if t < 0.5 else 0)
        k = 1 + int(round(v * 3.4))
        for y in range(S):
            cv.set(x, y, p[min(5, k)])

    for i in range(4):
        x = 8 * i + (i % 2) * 0 + 0.0
        y0, l = rnd.randrange(S), rnd.randint(8, 14)
        for t in range(l):
            base = cv.get(int(x) % S, (y0 + t) % S)
            idx = next((j for j, q in enumerate(p) if q == base[:3]), 3)
            wrap(cv, x, y0 + t, p[max(0, idx - 2)])
            wrap(cv, x - 1, y0 + t, p[min(5, idx + 1)])
    return cv


def sheet(out):
    styles = [("A soft vanilla-plus", style_a), ("B birch-style cute", style_b), ("C cherry-style bands", style_c), ("D rounded ridges", style_d)]
    woods = [("blossom", "#8c5f6a"), ("citrus", "#a8957a"), ("plumwood", "#7d6584"), ("palm", "#a0805e")]
    tile, gap = 3 * 32 * 2, 12
    W = len(styles) * (tile + gap) + 140
    H = len(woods) * (tile + gap) + 40
    im = Image.new("RGB", (W, H), (58, 52, 60))
    d = ImageDraw.Draw(im)
    for si, (name, fn) in enumerate(styles):
        d.text((140 + si * (tile + gap), 10), name, fill=(250, 235, 240))
    for wi, (wname, col) in enumerate(woods):
        y = 30 + wi * (tile + gap)
        d.text((8, y + tile // 2), wname, fill=(250, 235, 240))
        for si, (name, fn) in enumerate(styles):
            t = fn(col, 10 + wi).img
            grid = Image.new("RGBA", (96, 96))
            for gx in range(3):
                for gy in range(3):
                    grid.paste(t, (gx * 32, gy * 32))
            x = 140 + si * (tile + gap)
            im.paste(grid.resize((tile, tile), Image.NEAREST), (x, y))
            # how it reads from a distance (shrunk, like a few blocks away)
            far = grid.resize((48, 48), Image.BOX).resize((48, 48), Image.NEAREST)
            im.paste(far, (x + tile - 52, y + tile - 52))
    im.save(out)


if __name__ == "__main__":
    sheet(sys.argv[1])
