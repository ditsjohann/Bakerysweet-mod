"""
32x32 material textures for Sweetheart Bakery.

Each material is painted as a light/height field and then mapped onto a short colour ramp (dark -> light) built from
one base colour with hue-shifted shadows (cooler) and highlights (warmer). Quantising to the ramp is what gives the
clean, hand-painted pixel-art look instead of random noise.

Run: python3 blockbench/gen_textures.py   (writes into src/main/resources/assets/bakery/textures/block)
"""
import colorsys
import math
import os
import random

import numpy as np
from PIL import Image

S = 32
ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures/block")


# ---------------------------------------------------------------- colour

def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))


def ramp(base, n=6, spread=0.34, hue_shift=0.035, sat_boost=0.12):
    """n colours from shadow to highlight around a base colour."""
    r, g, b = hexrgb(base) if isinstance(base, str) else base
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(n):
        t = i / (n - 1) * 2 - 1  # -1 .. 1
        ll = min(0.97, max(0.04, l + t * spread * (1 - abs(l - 0.5))))
        # Shadows lean cool (toward blue/purple), highlights warm (toward yellow).
        hh = (h - t * hue_shift) % 1.0 if not (0.45 < h < 0.75) else (h + t * hue_shift * 0.5) % 1.0
        ss = min(1, max(0, s * (1 + (-t) * sat_boost)))
        rr, gg, bb = colorsys.hls_to_rgb(hh, ll, ss)
        out.append((int(rr * 255), int(gg * 255), int(bb * 255)))
    return out


def apply(field, pal, dither=True, seed=0):
    """Maps a 0..1 field onto a palette, with a tiny ordered dither so bands melt into each other."""
    bayer = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16 - 0.5
    f = np.clip(field, 0, 1) * (len(pal) - 1)
    if dither:
        f = f + np.tile(bayer, (S // 4, S // 4))[: field.shape[0], : field.shape[1]] * 0.55
    idx = np.clip(np.round(f), 0, len(pal) - 1).astype(int)
    pal = np.array(pal, dtype=np.uint8)
    rgb = pal[idx]
    alpha = np.full(field.shape + (1,), 255, dtype=np.uint8)
    return np.concatenate([rgb, alpha], axis=2)


# ---------------------------------------------------------------- noise

def value_noise(w, h, cell, seed, tile=True):
    rnd = np.random.RandomState(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    grid = rnd.rand(gh + 1, gw + 1)
    if tile:
        grid[-1, :] = grid[0, :]
        grid[:, -1] = grid[:, 0]
    ys = np.linspace(0, gh, h, endpoint=False)
    xs = np.linspace(0, gw, w, endpoint=False)
    y0 = ys.astype(int)
    x0 = xs.astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    a = grid[y0][:, x0]
    b = grid[y0][:, x0 + 1]
    c = grid[y0 + 1][:, x0]
    d = grid[y0 + 1][:, x0 + 1]
    top = a + (b - a) * fx[None, :]
    bot = c + (d - c) * fx[None, :]
    return top + (bot - top) * fy[:, None]


def fbm(w, h, seed, octaves=(16, 8, 4), weights=(0.55, 0.3, 0.15)):
    out = np.zeros((h, w))
    for i, (c, wt) in enumerate(zip(octaves, weights)):
        out += value_noise(w, h, c, seed + i * 31) * wt
    return out


def grid_xy(w=S, h=S):
    return np.meshgrid(np.arange(w), np.arange(h))


def save(name, arr):
    path = os.path.join(ROOT, name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(path)


def outline_rgba(arr, color=(0, 0, 0)):
    return arr


# ---------------------------------------------------------------- materials

def wood(base, seed=1, planks=4, vertical=False, nails=True):
    """Painted planks: seams, soft grain lines, a highlight on each plank's top edge, little nail heads."""
    X, Y = grid_xy()
    if vertical:
        X, Y = Y, X
    ph = S // planks
    rnd = random.Random(seed)
    field = np.zeros((S, S))
    for p in range(planks):
        y0 = p * ph
        off = rnd.uniform(0, 6.28)
        shade = rnd.uniform(-0.06, 0.06)
        rows = (Y >= y0) & (Y < y0 + ph)
        grain = np.sin((X * 0.18 + off) + np.sin(X * 0.09 + off * 2) * 1.6 + (Y - y0) * 1.3)
        g = 0.55 + shade + grain * 0.07 + (fbm(S, S, seed + p) - 0.5) * 0.18
        # top-edge highlight, bottom-edge shadow
        g = np.where(Y == y0 + 1, g + 0.18, g)
        g = np.where(Y == y0 + ph - 2, g - 0.1, g)
        field = np.where(rows, g, field)
        # a few streaky grain lines
        for _ in range(2):
            ly = y0 + rnd.randint(2, ph - 3)
            amp = rnd.uniform(0.6, 1.4)
            for x in range(S):
                yy = int(round(ly + math.sin(x * 0.35 + off) * amp))
                if y0 + 1 < yy < y0 + ph - 1:
                    if vertical:
                        field[x, yy] -= 0.13
                    else:
                        field[yy, x] -= 0.13
        # a knot now and then
        if rnd.random() < 0.35:
            kx, ky = rnd.randint(4, S - 5), y0 + ph // 2
            d = np.sqrt((X - kx) ** 2 * 0.6 + (Y - ky) ** 2 * 2.2)
            field = np.where(rows & (d < 2.4), field - 0.2 + d * 0.05, field)
    seam = (Y % ph == 0)
    field = np.where(seam, 0.05, field)
    # staggered end joints
    for p in range(planks):
        jx = (p * 13 + 7) % S
        m = (Y >= p * ph) & (Y < (p + 1) * ph) & (X == jx)
        field = np.where(m, 0.12, field)
        if nails:
            for nx in ((jx + 2) % S, (jx - 3) % S):
                ny = p * ph + ph // 2
                if vertical:
                    field[nx, ny] = 0.0
                else:
                    field[ny, nx] = 0.0
    if vertical:
        field = field.T
    return apply(field, ramp(base, 7, 0.36))


def ceramic(base, seed=2, speckle=True, band=None):
    """Glossy glaze: a soft vertical sheen, a bright specular stripe, a darker foot and a few speckles."""
    X, Y = grid_xy()
    field = 0.52 + 0.12 * np.cos((X - 9) / S * math.pi * 1.6) - (Y / S) * 0.14
    field += (fbm(S, S, seed, (16, 8), (0.6, 0.4)) - 0.5) * 0.08
    spec = np.exp(-((X - 8) ** 2) / 3.0) * 0.38 * (1 - Y / S * 0.6)
    field += spec
    field += np.exp(-((X - 13) ** 2) / 1.2) * 0.1
    if band:
        y0, y1 = band
        field = np.where((Y >= y0) & (Y < y1), field - 0.22, field)
        field = np.where(Y == y0, field + 0.12, field)
    arr = apply(field, ramp(base, 7, 0.3))
    if speckle:
        rnd = random.Random(seed)
        for _ in range(10):
            x, y = rnd.randrange(S), rnd.randrange(S)
            c = arr[y, x, :3].astype(int)
            arr[y, x, :3] = np.clip(c - 30, 0, 255)
    return arr


def metal(base, seed=3, brushed=True):
    """Brushed metal: fine horizontal streaks, a diagonal glint, darker rims."""
    X, Y = grid_xy()
    rnd = np.random.RandomState(seed)
    streak = rnd.rand(S, 1) * np.ones((1, S))
    streak = (streak + np.roll(streak, 1, 0)) / 2
    field = 0.45 + (streak - 0.5) * (0.16 if brushed else 0.05)
    diag = (X + Y * 0.6) / S
    field += np.exp(-((diag - 0.45) ** 2) / 0.02) * 0.35 + np.exp(-((diag - 0.85) ** 2) / 0.004) * 0.2
    field -= np.exp(-((diag - 1.15) ** 2) / 0.05) * 0.12
    edge = np.minimum(np.minimum(X, S - 1 - X), np.minimum(Y, S - 1 - Y))
    field = np.where(edge == 0, field - 0.18, field)
    field = np.where(edge == 1, field + 0.06, field)
    return apply(field, ramp(base, 7, 0.4, 0.05))


def copper(seed=4):
    arr = metal("#c8754a", seed)
    X, Y = grid_xy()
    # hammered dimples
    rnd = random.Random(seed)
    field = np.zeros((S, S))
    for _ in range(26):
        cx, cy = rnd.uniform(0, S), rnd.uniform(0, S)
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        field += np.where(d < 3, (3 - d) / 3 * 0.12, 0) * np.where(X - cx + Y - cy < 0, 1, -1)
    base = np.array(ramp("#c8754a", 7, 0.4, 0.05))
    lum = arr[..., :3].mean(axis=2) / 255
    lum = np.clip(lum + field, 0, 1)
    out = apply(lum * 0.95 + 0.03, list(map(tuple, base)))
    return out


def velvet(base, seed=5):
    """Plush velvet: soft folds and a diagonal sheen."""
    X, Y = grid_xy()
    folds = np.sin((X * 0.35 + np.sin(Y * 0.2) * 2)) * 0.06
    field = 0.5 + folds + (fbm(S, S, seed, (8, 4), (0.6, 0.4)) - 0.5) * 0.14
    field += np.exp(-(((X - Y) / S) ** 2) / 0.03) * 0.16
    return apply(field, ramp(base, 6, 0.3))


def cushion(base, seed=6):
    """A quilted cushion: puffy diamonds with button dimples at the corners."""
    X, Y = grid_xy()
    u = ((X + Y) % 16) / 16.0
    v = ((X - Y) % 16) / 16.0
    puff = np.sin(u * math.pi) * np.sin(v * math.pi)
    field = 0.32 + puff * 0.45 + (fbm(S, S, seed, (8, 4), (0.6, 0.4)) - 0.5) * 0.06
    seam = (np.abs(u) < 0.07) | (np.abs(v) < 0.07)
    field = np.where(seam, field - 0.08, field)
    buttons = ((X + Y) % 16 == 0) & ((X - Y) % 16 == 0)
    field = np.where(buttons, 0.05, field)
    return apply(field, ramp(base, 7, 0.32))


def linen(base, seed=7, check=None):
    """Woven cloth: tiny over-under threads; optionally a gingham check."""
    X, Y = grid_xy()
    weave = ((X + Y) % 2) * 0.06 + np.sin(X * 1.4) * 0.03 + np.sin(Y * 1.4) * 0.03
    field = 0.55 + weave + (fbm(S, S, seed, (8, 4), (0.5, 0.5)) - 0.5) * 0.1
    if check:
        band = 4
        cx = (X // band) % 2 == 0
        cy = (Y // band) % 2 == 0
        field = field - np.where(cx & cy, 0.36, np.where(cx | cy, 0.18, 0))
    return apply(field, ramp(base, 7, 0.36))


def tiles(c1, c2=None, seed=8, size=8, grout="#efe6e0"):
    """Glazed tiles with grout lines and a bevel; a checkerboard when two colours are given."""
    X, Y = grid_xy()
    tx, ty = X % size, Y % size
    bevel = np.where((tx == 1) | (ty == 1), 0.18, 0) - np.where((tx == size - 1) | (ty == size - 1), 0.12, 0)
    gloss = np.exp(-(((tx - 3) ** 2 + (ty - 2.5) ** 2) / 3.0)) * 0.14
    noise = (fbm(S, S, seed, (8, 4), (0.5, 0.5)) - 0.5) * 0.06
    field = 0.5 + bevel + gloss + noise
    out = np.zeros((S, S, 4), dtype=np.uint8)
    a = apply(field, ramp(c1, 6, 0.26))
    b = apply(field, ramp(c2 or c1, 6, 0.26))
    checker = ((X // size + Y // size) % 2 == 0)
    out = np.where(checker[..., None], a, b)
    g = np.array(ramp(grout, 3, 0.2)[0] + (255,), dtype=np.uint8)
    gmask = (tx == 0) | (ty == 0)
    out[gmask] = g
    return out


def marble(base, vein="#c8b8c0", seed=9):
    X, Y = grid_xy()
    t = fbm(S, S, seed, (16, 8, 4), (0.5, 0.3, 0.2))
    v = np.abs(np.sin((X * 0.12 + Y * 0.2 + t * 6)))
    field = 0.62 + (t - 0.5) * 0.12
    arr = apply(field, ramp(base, 6, 0.18))
    vm = v < 0.08
    arr[vm, :3] = np.array(ramp(vein, 3, 0.2)[1])
    vm2 = (v >= 0.08) & (v < 0.16)
    arr[vm2, :3] = (arr[vm2, :3].astype(int) * 0.6 + np.array(ramp(vein, 3, 0.2)[2]) * 0.4).astype(np.uint8)
    return arr


def bricks(base, mortar="#e8dccc", seed=10, bw=16, bh=8):
    X, Y = grid_xy()
    rnd = random.Random(seed)
    row = Y // bh
    offset = (row % 2) * (bw // 2)
    bx = (X + offset) // bw
    lx, ly = (X + offset) % bw, Y % bh
    field = np.zeros((S, S))
    tint = {}
    for r in range(S // bh + 1):
        for c in range(S // bw + 2):
            tint[(r, c)] = rnd.uniform(-0.12, 0.12)
    t = np.vectorize(lambda r, c: tint.get((r, c), 0))(row, bx)
    field = 0.5 + t + (fbm(S, S, seed, (8, 4), (0.5, 0.5)) - 0.5) * 0.18
    field = np.where(ly == 1, field + 0.14, field)
    field = np.where(ly == bh - 1, field - 0.12, field)
    field = np.where(lx == 1, field + 0.06, field)
    arr = apply(field, ramp(base, 6, 0.3))
    m = (ly == 0) | (lx == 0)
    mort = apply(0.5 + (fbm(S, S, seed + 5) - 0.5) * 0.3, ramp(mortar, 4, 0.2), dither=False)
    arr[m] = mort[m]
    return arr


def stone(base, seed=11):
    X, Y = grid_xy()
    t = fbm(S, S, seed, (16, 8, 4, 2), (0.4, 0.3, 0.2, 0.1)) if False else fbm(S, S, seed)
    cracks = np.abs(np.sin(X * 0.3 + t * 5)) < 0.05
    field = 0.5 + (t - 0.5) * 0.5
    field = np.where(cracks, field - 0.2, field)
    return apply(field, ramp(base, 6, 0.3))


def soil(seed=12):
    X, Y = grid_xy()
    t = fbm(S, S, seed, (4, 2), (0.6, 0.4))
    field = 0.4 + (t - 0.5) * 0.7
    arr = apply(field, ramp("#5b3a2a", 6, 0.3))
    rnd = random.Random(seed)
    for _ in range(14):
        x, y = rnd.randrange(S), rnd.randrange(S)
        arr[y, x, :3] = (122, 92, 70)
    return arr


def sponge(seed=13):
    """Cake sponge seen from the side (tinted by the flavour): crumb holes, plus a creamy filling layer."""
    X, Y = grid_xy()
    t = fbm(S, S, seed, (4, 2), (0.6, 0.4))
    field = 0.66 + (t - 0.5) * 0.28
    rnd = random.Random(seed)
    for _ in range(40):
        x, y = rnd.randrange(S), rnd.randrange(S)
        r = rnd.choice((0.8, 1.2, 1.6))
        d = np.sqrt((X - x) ** 2 + (Y - y) ** 2)
        field = np.where(d < r, field - 0.22, field)
        field = np.where((d >= r) & (d < r + 0.8) & (Y < y), field + 0.08, field)
    # filling layers (lighter, smooth) at 1/3 and 2/3
    for fy in (10, 21):
        layer = (Y >= fy) & (Y < fy + 3)
        field = np.where(layer, 0.92 - (Y - fy) * 0.03, field)
        field = np.where(Y == fy + 3, field - 0.15, field)
    return apply(field, ramp("#f2ead8", 7, 0.32, 0.02, 0.0))


def frosting(seed=14):
    """Smooth buttercream (white, tinted in game): gentle spatula swirls and a soft sheen."""
    X, Y = grid_xy()
    cx, cy = 16, 16
    ang = np.arctan2(Y - cy, X - cx)
    rad = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
    swirl = np.sin(rad * 0.55 + ang * 2) * 0.06
    field = 0.74 + swirl + (fbm(S, S, seed, (8, 4), (0.6, 0.4)) - 0.5) * 0.05
    field += np.exp(-(((X - 10) ** 2 + (Y - 9) ** 2) / 40)) * 0.12
    return apply(field, ramp("#e0dada", 6, 0.42, 0.0, 0.0))


def piping(seed=15):
    """Piped icing (tinted in game): glossy with soft ridges."""
    X, Y = grid_xy()
    ridges = np.sin(X * 0.8 + Y * 0.25) * 0.08
    field = 0.72 + ridges + np.exp(-(((X - 10) ** 2 + (Y - 8) ** 2) / 30)) * 0.2
    return apply(field, ramp("#e2dcdc", 6, 0.44, 0.0, 0.0))


def chocolate(seed=16, frames=1, flowing=False):
    """Melted chocolate: deep brown with glossy streaks (animated: the streaks run downward)."""
    out = []
    for f in range(frames):
        X, Y = grid_xy()
        yy = (Y - f * (S / frames)) % S if flowing else Y
        t = fbm(S, S, seed + (0 if flowing else f))
        if flowing:
            # vertical drips that slide down a frame at a time
            streak = np.sin(X * 0.9 + np.sin(yy * 0.2) * 1.5) * 0.5 + 0.5
            field = 0.32 + streak * 0.18 + (t - 0.5) * 0.08
            field += np.where(((X * 7 + 3) % 11 < 2), np.clip(np.sin(yy * 0.4) * 0.25, 0, 1), 0)
        else:
            ang = np.arctan2(Y - 16, X - 16)
            field = 0.34 + (t - 0.5) * 0.14 + np.sin(np.sqrt((X - 16) ** 2 + (Y - 16) ** 2) * 0.7 - f * 0.8 + ang) * 0.06
            field += np.exp(-(((X - 10) ** 2 + (Y - 10) ** 2) / 18)) * 0.3
        out.append(apply(field, ramp("#5a3220", 7, 0.36, 0.03)))
    return np.concatenate(out, axis=0)


def wicker(base, seed=17):
    X, Y = grid_xy()
    cell = 6
    u, v = X % cell, Y % cell
    horiz = ((X // cell + Y // cell) % 2 == 0)
    strand = np.where(horiz, np.sin(v / cell * math.pi), np.sin(u / cell * math.pi))
    field = 0.25 + strand * 0.6 + (fbm(S, S, seed, (8, 4)) - 0.5) * 0.1
    return apply(field, ramp(base, 6, 0.36))


def glass(tint="#dff2f6", alpha=110):
    X, Y = grid_xy()
    field = 0.6 + np.exp(-(((X - Y) / 3.0) ** 2) / 1.6) * 0.4 + np.exp(-(((X - Y - 7) / 2.0) ** 2) / 0.8) * 0.25
    arr = apply(field, ramp(tint, 4, 0.2), dither=False)
    a = np.clip(alpha + (field - 0.6) * 300, 40, 230).astype(np.uint8)
    edge = np.minimum(np.minimum(X, S - 1 - X), np.minimum(Y, S - 1 - Y)) == 0
    a = np.where(edge, 200, a)
    arr[..., 3] = a
    return arr


def liquid(base, seed=18):
    X, Y = grid_xy()
    t = fbm(S, S, seed, (8, 4), (0.6, 0.4))
    ripple = np.sin(np.sqrt((X - 16) ** 2 + (Y - 16) ** 2) * 0.9) * 0.08
    field = 0.55 + (t - 0.5) * 0.3 + ripple + np.exp(-(((X - 9) ** 2 + (Y - 8) ** 2) / 16)) * 0.35
    return apply(field, ramp(base, 6, 0.44, 0, 0))


def jam(seed=19, frames=4):
    out = []
    for f in range(frames):
        X, Y = grid_xy()
        t = fbm(S, S, seed + f * 3, (8, 4), (0.6, 0.4))
        field = 0.42 + (t - 0.5) * 0.3
        # bubbles
        rnd = random.Random(seed + f)
        for _ in range(4):
            bx, by, r = rnd.randrange(4, 28), rnd.randrange(4, 28), rnd.choice((1.2, 1.8, 2.4))
            d = np.sqrt((X - bx) ** 2 + (Y - by) ** 2)
            field = np.where(np.abs(d - r) < 0.6, field + 0.3, field)
        out.append(apply(field, ramp("#dcd4d4", 6, 0.46, 0, 0)))
    return np.concatenate(out, axis=0)


# ---------------------------------------------------------------- hand-made bits

def catalog_cover():
    """A pink cloth-bound book: gold border, a gold heart emblem, a title plate."""
    X, Y = grid_xy()
    field = 0.5 + (fbm(S, S, 20, (8, 4), (0.6, 0.4)) - 0.5) * 0.16 + ((X + Y) % 2) * 0.03
    field += np.exp(-(((X - 9) ** 2 + (Y - 8) ** 2) / 60)) * 0.14
    arr = apply(field, ramp("#e8789e", 7, 0.3))
    gold = ramp("#e8b84a", 5, 0.35)
    for i in range(S):
        for (x, y) in ((i, 2), (i, S - 3), (2, i), (S - 3, i)):
            if 2 <= x <= S - 3 and 2 <= y <= S - 3:
                arr[y, x, :3] = gold[3]
        for (x, y) in ((i, 3), (3, i)):
            if 3 <= x <= S - 4 and 3 <= y <= S - 4:
                arr[y, x, :3] = gold[1]
    # corner flourishes
    for (cx, cy) in ((5, 5), (S - 6, 5), (5, S - 6), (S - 6, S - 6)):
        arr[cy, cx, :3] = gold[4]
    # heart emblem
    heart = ["01100110", "11111111", "11111111", "11111111", "01111110", "00111100", "00011000"]
    for j, row in enumerate(heart):
        for i, ch in enumerate(row):
            if ch == "1":
                x, y = 12 + i, 10 + j
                c = gold[4] if (j < 2 and i in (1, 5)) else gold[3] if j < 4 else gold[2]
                arr[y, x, :3] = c
    # title plate
    for x in range(9, 23):
        for y in (21, 22, 23, 24):
            arr[y, x, :3] = (255, 244, 236) if y in (22, 23) else gold[2]
    for x in range(11, 21, 2):
        arr[22, x, :3] = (200, 120, 150)
    arr[23, 11:20, :3] = (220, 160, 180)
    return arr


def catalog_pages():
    X, Y = grid_xy()
    field = 0.7 + np.where(Y % 2 == 0, 0.08, -0.04) + (fbm(S, S, 21, (8,), (1,)) - 0.5) * 0.06
    arr = apply(field, ramp("#f6ecdc", 5, 0.2), dither=False)
    # a pink ribbon bookmark
    arr[:, 22:25, :3] = np.array(ramp("#e85a8a", 5, 0.3))[[1, 3, 2]][None, :, :]
    return arr


def millstone_top():
    """The runner stone seen from above: radial furrows and a wooden-pegged eye."""
    X, Y = grid_xy()
    cx = cy = 15.5
    ang = np.arctan2(Y - cy, X - cx)
    rad = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
    furrow = (np.abs(np.sin(ang * 8 + rad * 0.12)) < 0.18) & (rad > 5)
    field = 0.55 + (fbm(S, S, 22) - 0.5) * 0.4 - rad / S * 0.12
    field = np.where(furrow, field - 0.25, field)
    field = np.where(rad < 4, 0.12, field)
    field = np.where((rad >= 4) & (rad < 5.2), 0.78, field)
    return apply(field, ramp("#a8a29c", 6, 0.34))


def tulips():
    """Cross-plane tulip bunch: three blooms on curved stems with leaves (transparent background)."""
    arr = np.zeros((S, S, 4), dtype=np.uint8)
    leaf = ramp("#5aa65a", 5, 0.3)
    stems = [(9, 9, "#f06a90"), (16, 5, "#ffd05a"), (23, 10, "#c890e8")]
    for sx, top, col in stems:
        petals = ramp(col, 5, 0.32)
        # stem
        for y in range(top + 6, S):
            x = int(round(sx + math.sin((y - top) * 0.12) * 1.2 + (16 - sx) * (y - top) / (S * 2.2)))
            arr[y, x] = leaf[2] + (255,)
            arr[y, x + 1] = leaf[1] + (255,)
        # bloom: cup shape with three petals
        cup = ["0110110", "1121211", "1222221", "1232321", "1233321", "0122210", "0011100"]
        for j, row in enumerate(cup):
            for i, ch in enumerate(row):
                if ch != "0":
                    x, y = sx - 3 + i, top + j
                    arr[y, x] = petals[{"1": 1, "2": 2, "3": 4}[ch]] + (255,)
        # leaves
        for k in range(7):
            y = S - 2 - k
            for x in (sx - 2 - k // 2, sx + 3 + k // 2):
                if 0 <= x < S:
                    arr[y, x] = leaf[3 if k % 2 else 2] + (255,)
    return arr


# ---------------------------------------------------------------- the list

def build():
    # Only the materials no later script redraws (see build_all.py for the full order).
    jobs = {
        "pastry_board": wood("#dcb184", 11, planks=2, nails=False),
        "porcelain_floral": ceramic("#fdf6f4", 10),
        "marble": marble("#f8f2f2"),
        "oven_bricks": bricks("#c45e46", "#ead9c2", 2),
        "mill/millstone": millstone_top(),
        "soil": soil(),
        "juicer_glass": glass("#e6f6fa", 90),
        "case_glass": glass("#eef8ff", 70),
        "jar_glass": glass("#e8f4f0", 90),
        "juice_liquid": liquid("#dcd6cc"),
    }
    for name, arr in jobs.items():
        save(name, arr)
    save("fountain/chocolate_pool", chocolate(16, frames=4))
    save("fountain/chocolate_flow", chocolate(17, frames=8, flowing=True))
    save("jam_surface", jam())
    return list(jobs) + ["fountain/chocolate_pool", "fountain/chocolate_flow", "jam_surface"]


if __name__ == "__main__":
    done = build()
    print(len(done), "textures written")


# ---------------------------------------------------------------- patterns (wallpapers, rugs, awnings)

HEART = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
STRAWBERRY = ["0022200", "0222220", "0111110", "1131311", "1111111", "0131310", "0111110", "0011100", "0001000"]
BOW = ["110000011", "111000111", "111121111", "111000111", "110010011", "000101000"]
CLOUD = ["000111000000", "001111100110", "011111111111", "111111111111", "011111111110"]


def stamp(arr, sprite, x0, y0, colors):
    for j, row in enumerate(sprite):
        for i, ch in enumerate(row):
            if ch != "0":
                x, y = (x0 + i) % S, (y0 + j) % S
                c = colors[ch]
                arr[y, x, :3] = c[:3]
                arr[y, x, 3] = 255


def paper(base, seed):
    X, Y = grid_xy()
    field = 0.6 + (fbm(S, S, seed, (8, 4), (0.5, 0.5)) - 0.5) * 0.1 + ((X + Y) % 2) * 0.02
    return apply(field, ramp(base, 5, 0.18))


def heart_colors(c):
    r = ramp(c, 5, 0.32)
    return {"1": r[2], "2": r[4], "3": r[0]}


def wallpaper_hearts():
    arr = paper("#f8c8d8", 40)
    for (x, y) in ((4, 4), (20, 4), (12, 20), (28, 20)):
        stamp(arr, HEART, x, y, heart_colors("#f07aa0"))
        arr[(y + 1) % S, (x + 1) % S, :3] = (255, 255, 255)
    return arr


def wallpaper_strawberries():
    arr = paper("#fff4ea", 41)
    red, green = ramp("#e84a5a", 5, 0.32), ramp("#5cb85c", 4, 0.3)
    cols = {"1": red[2], "2": green[2], "3": (255, 230, 150)}
    for (x, y) in ((3, 3), (19, 11), (9, 20), (25, 26)):
        stamp(arr, STRAWBERRY, x, y, cols)
        arr[(y + 3) % S, (x + 1) % S, :3] = red[4]
    # tiny dots between
    for (x, y) in ((14, 4), (2, 15), (28, 18), (16, 29)):
        arr[y, x, :3] = (248, 180, 196)
    return arr


def wallpaper_polka():
    arr = paper("#bfe2f4", 42)
    X, Y = grid_xy()
    for (cx, cy) in ((6, 6), (22, 6), (14, 22), (30, 22), (14, -10)):
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        arr[d < 3.2, :3] = (255, 255, 255)
        arr[(d >= 2.2) & (d < 3.2) & (Y > cy), :3] = (232, 244, 252)
    return arr


def wallpaper_gingham():
    return linen("#fbe4ec", 43, check=True)


def wallpaper_bows():
    X, Y = grid_xy()
    stripe = (X // 4) % 2 == 0
    field = 0.6 + np.where(stripe, 0.12, -0.06) + (fbm(S, S, 44) - 0.5) * 0.06
    arr = apply(field, ramp("#f4b8cc", 5, 0.22))
    bow = ramp("#e05a86", 5, 0.32)
    for (x, y) in ((3, 4), (19, 20)):
        stamp(arr, BOW, x, y, {"1": bow[2], "2": bow[0]})
        arr[y + 1, x + 1, :3] = bow[4]
    return arr


def rug_heart():
    X, Y = grid_xy()
    arr = velvet("#f6a8c4", 45)
    big = [r.replace("1", "1") for r in HEART]
    # a big heart scaled x4 in the middle
    for j, row in enumerate(HEART):
        for i, ch in enumerate(row):
            if ch == "1":
                for dy in range(4):
                    for dx in range(4):
                        x, y = 2 + i * 4 + dx, 4 + j * 4 + dy
                        if 0 <= x < S and 0 <= y < S:
                            arr[y, x, :3] = (236, 84, 132) if (dx + dy) % 3 else (220, 70, 118)
    # fringe and a soft border
    arr[0, :, :3] = (255, 236, 244)
    arr[S - 1, :, :3] = (255, 236, 244)
    return arr


def rug_cloud():
    arr = velvet("#a8d4f2", 46)
    white = ramp("#ffffff", 3, 0.1)
    stamp(arr, CLOUD, 2, 4, {"1": white[2]})
    stamp(arr, CLOUD, 16, 18, {"1": white[2]})
    stamp(arr, ["0111", "1111"], 22, 6, {"1": white[1]})
    return arr


def awning(base):
    X, Y = grid_xy()
    stripe = (X // 8) % 2 == 0
    fold = np.sin(X / 8 * math.pi) ** 2 * 0.12
    field = 0.55 + fold + (fbm(S, S, 47, (8, 4)) - 0.5) * 0.06 - (Y / S) * 0.08
    a = apply(field, ramp(base, 6, 0.3))
    b = apply(field + 0.1, ramp("#fffaf6", 6, 0.16))
    return np.where(stripe[..., None], a, b)


def lace():
    arr = np.zeros((S, S, 4), dtype=np.uint8)
    X, Y = grid_xy()
    white = np.array([255, 252, 250, 230], dtype=np.uint8)
    # a fine net with flower medallions; holes stay transparent
    net = ((X + Y) % 4 == 0) | ((X - Y) % 4 == 0)
    arr[net] = white
    for (cx, cy) in ((8, 8), (24, 24), (24, 8), (8, 24)):
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        ring = (d > 2.5) & (d < 4.2)
        arr[ring] = white
        arr[d < 1.2] = white
        petal = (d > 4.2) & (d < 5.5) & (np.abs(np.sin(np.arctan2(Y - cy, X - cx) * 3)) > 0.7)
        arr[petal] = white
    # scalloped bottom edge
    for x in range(S):
        h = int(2 + 1.5 * abs(math.sin(x / 4 * math.pi)))
        for y in range(S - h, S):
            arr[y, x] = white
    return arr


def build_patterns():
    jobs = {"heart_wallpaper": wallpaper_hearts(), "strawberry_wallpaper": wallpaper_strawberries(), "polka_dot_wallpaper": wallpaper_polka(),
            "gingham_wallpaper": wallpaper_gingham(), "bow_wallpaper": wallpaper_bows(), "heart_rug": rug_heart(), "cloud_rug": rug_cloud(),
            "pink_awning": awning("#f6a0bc"), "mint_awning": awning("#9adcc2"), "lace_curtains": lace(),
            "plush_pink": velvet("#f8b8cc", 48), "plush_white": velvet("#f6f2ee", 49), "plush_brown": velvet("#b88864", 50),
            "welcome_mat": wicker("#c8a070", 51)}
    for name, arr in jobs.items():
        save(name, arr)
    return list(jobs)


if __name__ == "__main__":
    print(len(build_patterns()), "pattern textures written")
