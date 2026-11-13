"""
16x16 wood textures in a soft, painterly pixel style (studied from the Journey's End wood sheet):

- no hard outlines or 1px groove lines: bark is close tones blended in irregular patches stretched along the grain
- small, close palettes (bark ~6 tones in a narrow brightness band, stripped ~5, planks ~6)
- log ends: rounded-square rings that spiral in, lit toward the upper left and darker toward the edges (a soft
  vignette), inside a dark bark rim
- stripped wood: flame-like vertical strands; planks: soft darker seams and horizontal smears inside each board

Everything is seamless (the noise wraps) and each wood is two hand-picked colour ranges (bark, wood).
Run: python3 blockbench/gen_woods16.py
"""
import math
import os
import random

from PIL import Image

TEX = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures/block")
S = 16

# (bark dark, bark light, wood dark, wood light, bark grain direction)
WOODS = {
    "blossom": ("#2e1a26", "#6c4459", "#98527e", "#ffb6d3", "h"),
    "citrus": ("#3a3226", "#7c6e54", "#a8823a", "#fbe6a0", "v"),
    "orangewood": ("#3a2618", "#7c5636", "#8e4a1e", "#f6b06a", "v"),
    "peachwood": ("#3e221c", "#80503e", "#a85a48", "#fcd0b8", "v"),
    "mangowood": ("#2a2818", "#625c36", "#9a6a1c", "#f4d26a", "v"),
    "plumwood": ("#2a2234", "#6a5c7a", "#5c4c70", "#c8b2de", "h"),
    "palm": ("#4a3624", "#9c7c56", "#9a7a4a", "#f0dcb0", "rings"),
}


def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(dark, light, n):
    """n tones from dark to light; the middle tones lean a touch warmer, like hand-picked ramps."""
    a, b = hx(dark), hx(light)
    out = []
    for i in range(n):
        t = i / (n - 1)
        c = [a[k] + (b[k] - a[k]) * t for k in range(3)]
        warm = math.sin(t * math.pi) * 6
        out.append((min(255, int(c[0] + warm)), int(c[1] + warm * 0.3), max(0, int(c[2] - warm * 0.5))))
    return out


def noise(seed, cw, ch, octaves=((1.0, 1.0), (0.5, 0.45))):
    """Seamless value noise on the 16x16 tile, cells cw x ch (stretched along the grain), a few octaves."""
    rnd = random.Random(seed)
    field = [[0.0] * S for _ in range(S)]
    for scale, weight in octaves:
        w, h = max(1, int(round(cw * scale))), max(1, int(round(ch * scale)))
        gx, gy = max(1, S // w), max(1, S // h)
        grid = [[rnd.random() for _ in range(gx)] for _ in range(gy)]
        for y in range(S):
            for x in range(S):
                fx, fy = x / S * gx, y / S * gy
                x0, y0 = int(fx), int(fy)
                tx, ty = fx - x0, fy - y0
                tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
                a = grid[y0 % gy][x0 % gx]
                b = grid[y0 % gy][(x0 + 1) % gx]
                c = grid[(y0 + 1) % gy][x0 % gx]
                d = grid[(y0 + 1) % gy][(x0 + 1) % gx]
                field[y][x] += weight * ((a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty)
    return field


def quantize(field, n):
    """Spreads a field evenly over n tones (by rank), so every tone shows up in small soft clusters."""
    flat = sorted(v for row in field for v in row)
    cuts = [flat[int(len(flat) * k / n)] for k in range(1, n)]
    return [[sum(v > c for c in cuts) for v in row] for row in field]


def paint(idx, pal, path):
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), pal[max(0, min(len(pal) - 1, idx[y][x]))] + (255,))
    img.save(path)


def bark(dark, light, grain, seed):
    pal = ramp(dark, light, 6)
    if grain == "rings":
        # Palm: soft horizontal rings, each lighter on top, with painterly variation.
        f = noise(seed, 6, 2)
        for y in range(S):
            band = (y % 4) / 3
            for x in range(S):
                f[y][x] = f[y][x] * 0.6 + (1 - band) * 0.9
        return quantize(f, 6), pal
    cw, ch = (2, 5) if grain == "v" else (5, 2)
    f = noise(seed, cw, ch, ((1.0, 1.0), (0.5, 0.65), (0.25, 0.45)))
    return quantize(f, 6), pal


def stripped(dark, light, seed):
    pal = ramp(dark, light, 5)
    f = noise(seed + 5, 2, 4, ((1.0, 1.0), (0.5, 0.6), (0.25, 0.35)))
    return quantize(f, 5), pal


def log_top(bark_dark, bark_light, wood_dark, wood_light, seed, stripped_top=False):
    wood = ramp(wood_dark, wood_light, 6)
    barkp = ramp(bark_dark, bark_light, 6)
    img = Image.new("RGBA", (S, S))
    rnd = random.Random(seed)
    for y in range(S):
        for x in range(S):
            dx, dy = x - 7.5, y - 7.5
            d = (abs(dx) ** 3.2 + abs(dy) ** 3.2) ** (1 / 3.2)
            edge = min(x, y, S - 1 - x, S - 1 - y)
            if not stripped_top and edge <= 1:
                k = 1 if edge == 0 else (3 if dx + dy < 0 else 2)
                img.putpixel((x, y), barkp[k] + (255,))
                continue
            ang = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
            t = d + ang * 2.5
            line = (t % 2.5) < 0.95
            v = 4.2 - d * 0.32 - (dx + dy) * 0.12 + rnd.uniform(-0.25, 0.25)  # light top-left, darker edges
            if line:
                v -= 1.4
            if stripped_top and edge == 0:
                v -= 1.2
            img.putpixel((x, y), wood[max(0, min(5, int(round(v))))] + (255,))
    return img


def planks(dark, light, seed):
    pal = ramp(dark, light, 6)
    f = noise(seed + 9, 6, 1, ((1.0, 1.0), (0.5, 0.5)))
    q = quantize(f, 3)
    joints = [4, 11, 7, 14]
    idx = [[0] * S for _ in range(S)]
    for y in range(S):
        b, r = y // 4, y % 4
        for x in range(S):
            v = 2 + q[y][x]  # 2..4 inside the board
            if r == 0:
                v = min(5, v + 1)  # each board's top catches the light
            if r == 3:
                v = 1 if (x + b) % 5 else 0  # soft seam
            if x == joints[b] and r != 3:
                v = 1
            elif x == (joints[b] + 1) % S and r != 3:
                v = min(5, v + 1)
            idx[y][x] = v
    return idx, pal


LEAVES = {
    # leaf dark, leaf light, flower colour (or None)
    "blossom": ("#a8507e", "#ffc6e2", None), "citrus": ("#2c5a26", "#7cbc4c", "#fffaf0"),
    "orangewood": ("#2a5424", "#74b048", "#ffffff"), "peachwood": ("#36622c", "#88c45a", "#ffb6c8"),
    "mangowood": ("#26502a", "#6aac4a", None), "plumwood": ("#4a3a62", "#b49ad2", None), "palm": ("#2e6a2a", "#86c858", None),
}


def leaves(dark, light, flower, seed):
    """Painterly leaves: soft clusters in 5 close tones, a few see-through gaps in the darkest spots, tiny flowers."""
    pal = ramp(dark, light, 5)
    f = noise(seed + 21, 3, 3, ((1.0, 1.0), (0.5, 0.7), (0.25, 0.35)))
    q = quantize(f, 5)
    rnd = random.Random(seed)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            img.putpixel((x, y), pal[q[y][x]] + (255,))
    darkest = [(x, y) for y in range(S) for x in range(S) if q[y][x] == 0]
    rnd.shuffle(darkest)
    for (x, y) in darkest[:6]:
        img.putpixel((x, y), (0, 0, 0, 0))
    if flower:
        fl = hx(flower)
        spots = [(x, y) for y in range(S) for x in range(S) if q[y][x] >= 3]
        rnd.shuffle(spots)
        for (x, y) in spots[:4]:
            img.putpixel((x, y), fl + (255,))
    return img


def build():
    n = 0
    for i, (name, (bd, bl, wd, wl, grain)) in enumerate(WOODS.items()):
        seed = 40 + i * 13
        idx, pal = bark(bd, bl, grain, seed)
        paint(idx, pal, os.path.join(TEX, f"{name}_log.png"))
        log_top(bd, bl, wd, wl, seed).save(os.path.join(TEX, f"{name}_log_top.png"))
        idx, pal = stripped(wd, wl, seed)
        paint(idx, pal, os.path.join(TEX, f"stripped_{name}_log.png"))
        log_top(bd, bl, wd, wl, seed, stripped_top=True).save(os.path.join(TEX, f"stripped_{name}_log_top.png"))
        idx, pal = planks(wd, wl, seed)
        paint(idx, pal, os.path.join(TEX, f"{name}_planks.png"))
        ld, ll, fl = LEAVES[name]
        leaves(ld, ll, fl, seed).save(os.path.join(TEX, f"{name}_leaves.png"))
        n += 6
    return n


if __name__ == "__main__":
    print(build(), "16x16 wood textures written")
