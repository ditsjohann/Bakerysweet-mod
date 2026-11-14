"""
Furniture and kitchenware materials drawn as real 16x16 pixel art: every texture is built from the material's own
structure (boards with grain strokes and joints, glaze rings and highlights, woven threads, wicker strands, tufted
diamonds), lit from the upper left, in 4-5 hand-picked tones. No noise, no random colour shifting.

Run after gen_materials.py (replaces its 32px noise-based versions): python3 blockbench/gen_materials16.py
"""
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures/block")
S = 16


def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def tones(base, n=5, spread=0.16, warm=True):
    """n tones around a base colour, darker ones a little warmer/more saturated like hand-picked ramps."""
    r, g, b = hx(base)
    out = []
    for i in range(n):
        t = (i - (n - 1) / 2) / ((n - 1) / 2)  # -1 .. 1
        k = 1 + t * spread
        shift = -t * 6 if warm else 0
        out.append((max(0, min(255, int(r * k + shift))), max(0, min(255, int(g * k))), max(0, min(255, int(b * k - shift)))))
    return out


class Px:
    def __init__(self, fill=None):
        self.img = Image.new("RGBA", (S, S), (fill + (255,)) if fill else (0, 0, 0, 0))

    def set(self, x, y, c):
        self.img.putpixel((x % S, y % S), tuple(c[:3]) + (255,))

    def rect(self, x1, y1, x2, y2, c):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.set(x, y, c)

    def save(self, name):
        path = os.path.join(TEX, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# ------------------------------------------------------------------ wood

# Grain strokes per board: (board, x, length) - drawn one tone darker, like vanilla planks' grain lines.
GRAIN = [(0, 2, 4), (0, 10, 3), (1, 6, 5), (1, 13, 2), (2, 1, 3), (2, 9, 5), (3, 4, 4), (3, 12, 3)]
JOINTS = [5, 12, 2, 9]
KNOT = (2, 6)


def planks(base, painted=False):
    """Four 4px boards: a lit top row, a dark seam, a staggered butt joint, a few grain strokes and one knot."""
    p = tones(base, 5, 0.12 if painted else 0.18)
    px = Px(p[2])
    for b in range(4):
        y0 = b * 4
        for x in range(S):
            px.set(x, y0, p[3])          # the board's upper edge catches the light
            px.set(x, y0 + 3, p[0])      # the seam under it
        j = JOINTS[b]
        for y in range(y0, y0 + 3):
            px.set(j, y, p[0])
            px.set(j + 1, y, p[3])
    for (b, x, length) in GRAIN:
        y = b * 4 + (1 if (x + b) % 2 else 2)
        for i in range(length):
            if (x + i) % S not in (JOINTS[b], JOINTS[b] + 1):
                px.set(x + i, y, p[1])
    if not painted:
        bx, by = KNOT
        px.set(bx, by, p[0]); px.set(bx + 1, by, p[1]); px.set(bx - 1, by, p[1])
    return px


# ------------------------------------------------------------------ glaze

def ceramic(base, band=None, motif=None):
    """Smooth glaze: an even colour, faint throwing rings, a couple of soft highlight dashes; optional painted band."""
    p = tones(base, 5, 0.1)
    px = Px(p[2])
    for y in (3, 9, 14):
        for x in range(S):
            if (x + y) % 7 not in (0, 1):
                px.set(x, y, p[3] if y != 14 else p[1])
    for (x, y, n) in ((2, 5, 3), (10, 11, 2)):
        for i in range(n):
            px.set(x + i, y, p[4])
    if band:
        y0, y1, bc = band
        b = tones(bc, 4, 0.12)
        for y in range(y0, y1 + 1):
            for x in range(S):
                px.set(x, y, b[2] if y not in (y0, y1) else b[1])
        if motif == "dots":
            for x in range(1, S, 4):
                px.set(x, (y0 + y1) // 2, hx("#fff8f4"))
    return px


# ------------------------------------------------------------------ metal

def metal(base, glints=False):
    """Brushed metal: an even base with long, straight brush lines (a darker one and a lighter one under it)."""
    p = tones(base, 5, 0.14, warm=False)
    px = Px(p[2])
    for row, y in enumerate(range(1, S, 5)):
        gap = (row * 6 + 3) % S
        for x in range(S):
            if not (gap <= x < gap + 3):
                px.set(x, y, p[1])
        for x in range((gap + 5) % S, (gap + 5) % S + 7):
            px.set(x, y + 2, p[3])
    if glints:
        for (x, y) in ((3, 4), (12, 14)):
            px.set(x, y, p[4]); px.set(x + 1, y, p[4]); px.set(x, y + 1, p[3])
    return px


def hammered(base):
    """Hammered copper: a grid of round dimples, each bright on its upper-left rim and dark on the lower right."""
    p = tones(base, 5, 0.2)
    px = Px(p[2])
    for row in range(4):
        for col in range(4):
            cx, cy = col * 4 + (2 if row % 2 else 0), row * 4 + 1
            px.set(cx, cy, p[4]); px.set(cx + 1, cy, p[3])
            px.set(cx, cy + 1, p[3]); px.set(cx + 1, cy + 1, p[1])
            px.set(cx + 2, cy + 1, p[1]); px.set(cx + 1, cy + 2, p[0])
    return px


# ------------------------------------------------------------------ fabric

def velvet(base):
    """Velvet / plush: soft vertical ribs, each lit on its left and shaded on its right."""
    p = tones(base, 5, 0.12)
    px = Px(p[2])
    for x in range(S):
        k = (3, 2, 2, 1)[x % 4]
        for y in range(S):
            px.set(x, y, p[k])
    for (x, y) in ((1, 4), (9, 11), (5, 14)):
        px.set(x, y, p[4])
    return px


def tufted(base):
    """A tufted cushion: diamond puffs, lit on the upper left, with a button sunk where the seams meet."""
    p = tones(base, 5, 0.14)
    px = Px()
    for y in range(S):
        for x in range(S):
            u, v = (x + y) % 8, (x - y) % 8
            if u == 0 or v == 0:
                px.set(x, y, p[0])            # the seams
            elif u + v <= 6:
                px.set(x, y, p[3])            # the lit upper half of each puff
            else:
                px.set(x, y, p[2])
    for (x, y) in ((0, 0), (8, 0), (4, 4), (12, 4), (0, 8), (8, 8), (4, 12), (12, 12)):
        px.set(x, y, p[0]); px.set(x + 1, y, p[1])
    return px


def linen(base):
    """Linen: an even cloth with a few slubby threads running across it."""
    p = tones(base, 4, 0.06)
    px = Px(p[2])
    for y in (2, 7, 12):
        for x in range(S):
            if (x + y) % 6 not in (0, 1):
                px.set(x, y, p[3])
    for x in (4, 11):
        for y in range(S):
            if (x + y) % 5 not in (0,):
                px.set(x, y, p[1] if y % 2 else p[2])
    return px


def gingham(base, check):
    """Gingham: 4px bands of colour each way; where they cross the colour is doubled; a fine weave on top."""
    w, c = hx(base), tones(check, 4, 0.1)
    px = Px(w)
    for y in range(S):
        for x in range(S):
            a, b = (x // 4) % 2 == 0, (y // 4) % 2 == 0
            col = c[0] if a and b else c[2] if a or b else w
            if (x + y) % 2 == 0 and col != w:
                col = c[1] if col == c[0] else c[3]
            px.set(x, y, col)
    return px


def sack(base):
    """Burlap: chunky over-under threads with little gaps between them."""
    p = tones(base, 4, 0.14)
    px = Px(p[2])
    for y in range(S):
        for x in range(S):
            over = (x // 2 + y // 2) % 2 == 0
            if x % 2 == 1 and y % 2 == 1:
                px.set(x, y, p[0])
            elif over:
                px.set(x, y, p[3] if y % 2 == 0 else p[2])
            else:
                px.set(x, y, p[1] if x % 2 == 0 else p[2])
    return px


def wicker(base):
    """Wicker: 4px strands woven over and under; each strand is round (lit middle, dark edges) with dark gaps."""
    p = tones(base, 5, 0.18)
    px = Px()
    for y in range(S):
        for x in range(S):
            cell = ((x // 4) + (y // 4)) % 2
            t = (y % 4) if cell == 0 else (x % 4)   # across the strand
            along = (x % 4) if cell == 0 else (y % 4)
            k = (1, 3, 2, 0)[t]
            if along == 0 and t in (1, 2):
                k = 1  # where it dives under the crossing strand
            px.set(x, y, p[k])
    return px


def build():
    jobs = {
        "wood_cream": planks("#f2e2c6", True), "wood_pink": planks("#f2bccc", True), "wood_mint": planks("#b2dcc8", True),
        "wood_lilac": planks("#cdb9e4", True), "table_wood": planks("#c9976a"), "case_wood": planks("#f0c4d2", True),
        "churn_wood": planks("#b98a62"), "crate": planks("#c99a62"),
        "ceramic_pink": ceramic("#f2a9c2"), "ceramic_white": ceramic("#f7f1ed"), "mug": ceramic("#fbe2ea"),
        "mug_inner": ceramic("#fff6f0"), "mixing_bowl": ceramic("#f8cfdc", band=(11, 13, "#f28aac"), motif="dots"),
        "mixing_bowl_inner": ceramic("#fff4ee"), "flower_pot_peach": ceramic("#f2b08e", band=(2, 4, "#e8906e")),
        "juicer_body": ceramic("#c9ecdc"), "fountain/ceramic": ceramic("#fbf2ee"), "plate": ceramic("#fffaf6"),
        "cupcake_lamp_wrapper": ceramic("#f6a4c0"),
        "metal": metal("#c8ccd6"), "metal_white": metal("#ece8ee"), "metal_pink": metal("#f0aac2"), "metal_gold": metal("#e6b84e", True),
        "brass": metal("#d6a84c", True), "fountain/gold": metal("#eec258", True), "pie_tin": metal("#c2c6ce"), "copper_pot": hammered("#c8774c"),
        "velvet_pink": velvet("#f2a0bc"), "velvet_mint": velvet("#a0d8c4"), "cushion": tufted("#ffd0de"),
        "plush_pink": velvet("#f8bcd0"), "plush_white": velvet("#f6f2ee"), "plush_brown": velvet("#bc8c66"),
        "linen": linen("#f4ece0"), "tablecloth": gingham("#ffffff", "#f6aac4"), "sack": sack("#d8bf94"),
        "wicker": wicker("#d4a874"), "banneton": wicker("#c69660"), "welcome_mat": wicker("#c8a070"),
    }
    for name, px in jobs.items():
        px.save(name)
    return len(jobs)


if __name__ == "__main__":
    print(build(), "16x16 materials written")
