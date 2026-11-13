"""Small pixel-art drawing kit shared by the hand-drawn texture scripts: palettes, shapes and a tiny 3x5 font."""
import colorsys
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/textures")


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shades(base, n=5, spread=0.3, hue=0.03):
    """n colours from dark to light, shadows a bit cooler, highlights a bit warmer."""
    r, g, b = [c / 255 for c in rgb(base)]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(n):
        t = i / (n - 1) * 2 - 1
        ll = min(0.97, max(0.05, l + t * spread * (1 - abs(l - 0.5))))
        hh = (h - t * hue) % 1
        ss = min(1, max(0, s * (1 - t * 0.1)))
        out.append(tuple(int(v * 255) for v in colorsys.hls_to_rgb(hh, ll, ss)))
    return out


class Canvas:
    def __init__(self, w, h=None, fill=None):
        self.w, self.h = w, h or w
        self.img = Image.new("RGBA", (self.w, self.h), (fill + (255,)) if fill else (0, 0, 0, 0))
        self.px = self.img.load()

    def set(self, x, y, c, a=255):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = tuple(c[:3]) + (a,)

    def get(self, x, y):
        return self.px[x, y]

    def rect(self, x1, y1, x2, y2, c):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.set(x, y, c)

    def round_rect(self, x1, y1, x2, y2, fill, border=None):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                corner = (x in (x1, x2)) and (y in (y1, y2))
                if corner:
                    continue
                edge = x in (x1, x2) or y in (y1, y2)
                self.set(x, y, border if (edge and border) else fill)

    def hline(self, x1, x2, y, c):
        for x in range(x1, x2 + 1):
            self.set(x, y, c)

    def vline(self, x, y1, y2, c):
        for y in range(y1, y2 + 1):
            self.set(x, y, c)

    def disc(self, cx, cy, r, ramp, light=(-0.6, -0.7)):
        """A shaded ball: ramp dark->light, lit from the upper left, with a tiny specular dot."""
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = (dx * dx + dy * dy) ** 0.5
                if d > r:
                    continue
                lit = -(dx * light[0] + dy * light[1]) / max(r, 0.01)
                edge = d / r
                v = 0.55 + lit * 0.45 - edge ** 3 * 0.35
                i = max(0, min(len(ramp) - 1, int(round(v * (len(ramp) - 1)))))
                self.set(x, y, ramp[i])
        self.set(cx - r * 0.4, cy - r * 0.45, ramp[-1])

    def paste(self, src, x, y):
        im = src if isinstance(src, Image.Image) else Image.open(src).convert("RGBA")
        self.img.alpha_composite(im.convert("RGBA"), (x, y))
        self.px = self.img.load()

    def outline(self, c):
        """A 1px outline around everything opaque (on the transparent side)."""
        src = self.img.copy().load()
        for y in range(self.h):
            for x in range(self.w):
                if src[x, y][3] == 0:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        X, Y = x + dx, y + dy
                        if 0 <= X < self.w and 0 <= Y < self.h and src[X, Y][3] > 0:
                            self.set(x, y, c)
                            break

    def save(self, rel):
        path = os.path.join(TEX, rel + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


FONT = {
    "0": "111101101101111", "1": "010110010010111", "2": "111001111100111", "3": "111001111001111", "4": "101101111001001",
    "5": "111100111001111", "6": "111100111101111", "7": "111001001010010", "8": "111101111101111", "9": "111101111001111",
    "C": "111100100100111", "A": "010101111101101", "T": "111010010010010", "L": "100100100100111", "O": "111101101101111",
    "G": "111100101101111", "S": "111100111001111", "W": "101101101111101", "E": "111100111100111", "N": "101111111111101",
    "!": "010010010000010", "♥": "101111111010000",
}


def text(cv, s, x, y, c):
    for ch in s:
        g = FONT.get(ch)
        if g:
            for i, bit in enumerate(g):
                if bit == "1":
                    cv.set(x + i % 3, y + i // 3, c)
        x += 4
    return x
