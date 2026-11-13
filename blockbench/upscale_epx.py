"""
Doubles every texture that is still 16 pixels wide to 32 with EPX / Scale2x: each pixel becomes four, and corners
take a neighbour's colour where two edges meet, so stair-stepped diagonals and curves come out smoother instead of
just blockier. Hand-painted 32px materials (see gen_textures.py) are left alone.
"""
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures")


def epx(a):
    h, w = a.shape[:2]
    p = np.pad(a, ((1, 1), (1, 1), (0, 0)), mode="edge")
    P = p[1:-1, 1:-1]
    A = p[:-2, 1:-1]   # up
    B = p[1:-1, 2:]    # right
    C = p[1:-1, :-2]   # left
    D = p[2:, 1:-1]    # down
    eq = lambda x, y: np.all(x == y, axis=2)
    o1 = np.where((eq(C, A) & ~eq(C, D) & ~eq(A, B))[..., None], A, P)
    o2 = np.where((eq(A, B) & ~eq(A, C) & ~eq(B, D))[..., None], B, P)
    o3 = np.where((eq(D, C) & ~eq(D, B) & ~eq(C, A))[..., None], C, P)
    o4 = np.where((eq(B, D) & ~eq(B, A) & ~eq(D, C))[..., None], D, P)
    out = np.zeros((h * 2, w * 2, a.shape[2]), dtype=a.dtype)
    out[0::2, 0::2] = o1
    out[0::2, 1::2] = o2
    out[1::2, 0::2] = o3
    out[1::2, 1::2] = o4
    return out


def run(folders=("block", "item")):
    n = 0
    for folder in folders:
        for d, _, files in os.walk(os.path.join(ROOT, folder)):
            for f in files:
                if not f.endswith(".png"):
                    continue
                path = os.path.join(d, f)
                im = Image.open(path).convert("RGBA")
                if im.size[0] != 16:
                    continue
                Image.fromarray(epx(np.array(im))).save(path)
                n += 1
    return n


if __name__ == "__main__":
    print(run(sys.argv[1:] or ("block", "item")), "textures doubled")
