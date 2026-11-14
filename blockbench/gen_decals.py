"""
Cake toppers as flat sugar decals: every topper is a little pixel sprite (its inventory icon and the decal laid on
the cake), plus the cake moulds for the newer shapes, drawn from the same shape maths the game uses.

Sprites are drawn on a 12x12 grid of letters (one palette per sprite, '.' is see-through); the script adds a soft
outline, a lit upper-left edge and a shaded lower-right edge, so they all share one look.
Run: python3 blockbench/gen_decals.py
"""
import colorsys
import json
import math
import os

from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
TEX = os.path.join(ASSETS, "textures/item")


def hx(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, dl):
    h, l, s = colorsys.rgb_to_hls(*[v / 255 for v in c])
    return tuple(int(v * 255) for v in colorsys.hls_to_rgb(h, max(0, min(1, l + dl)), s))


DECALS = {
    "fondant_heart": ({"a": "#f27aa4"}, [
        "..aa....aa..",
        ".aaaa..aaaa.",
        "aawaaaaaaaaa",
        "awaaaaaaaaaa",
        "aaaaaaaaaaaa",
        ".aaaaaaaaaa.",
        "..aaaaaaaa..",
        "...aaaaaa...",
        "....aaaa....",
        ".....aa.....",
        "............",
        "............"]),
    "fondant_star": ({"a": "#ffd25a"}, [
        ".....aa.....",
        ".....aa.....",
        "....aaaa....",
        "aaaaawaaaaaa",
        ".aaaawaaaaa.",
        "..aaaaaaaa..",
        "...aaaaaa...",
        "...aaaaaa...",
        "..aaa..aaa..",
        "..aa....aa..",
        ".aa......aa.",
        "............"]),
    "fondant_bunny": ({"a": "#fbf6f2", "b": "#f7a8c4", "k": "#5a3a46", "p": "#ffc0d4", "o": "#c89aaa"}, [
        "..aa....aa..",
        "..ab....ba..",
        "..ab....ba..",
        "..aa....aa..",
        ".aaaaaaaaaa.",
        "aaaaaaaaaaaa",
        "aakaaaaaakaa",
        "apaaaaaaaapa",
        "aaaaabbaaaaa",
        ".aaaaaaaaaa.",
        "..aaaaaaaa..",
        "............"]),
    "sugar_flower": ({"a": "#f7a0c0", "b": "#ffd860"}, [
        "....aaaa....",
        "...aaaaaa...",
        ".aa.aaaa.aa.",
        "aaaa.aa.aaaa",
        "aaaaabbaaaaa",
        ".aaabbbbaaa.",
        ".aaabbbbaaa.",
        "aaaaabbaaaaa",
        "aaaa.aa.aaaa",
        ".aa.aaaa.aa.",
        "...aaaaaa...",
        "....aaaa...."]),
    "mini_macaron": ({"a": "#a8dcc4", "b": "#86c4a8", "w": "#fff6ec"}, [
        "............",
        "..aaaaaaaa..",
        ".aaaaaaaaaa.",
        "aaaaaaaaaaaa",
        "bbbbbbbbbbbb",
        "wwwwwwwwwwww",
        "bbbbbbbbbbbb",
        "aaaaaaaaaaaa",
        ".aaaaaaaaaa.",
        "..aaaaaaaa..",
        "............",
        "............"]),
    "sugar_pearls": ({"a": "#fff8fa", "b": "#f9c6d8", "o": "#d08aa4"}, [
        "............",
        "....aaa.....",
        "...aaaaa.bb.",
        "...aaaaabbbb",
        "....aaa.bbbb",
        ".bbb.....bb.",
        "bbbbb.aaa...",
        "bbbbbaaaaa..",
        ".bbb.aaaaa..",
        "......aaa...",
        "............",
        "............"]),
    "chocolate_curl": ({"a": "#8a5634", "b": "#6a3e24"}, [
        "............",
        "..aa....aa..",
        ".aaba..aaba.",
        "aa.ba.aa.ba.",
        "a..baaa..ba.",
        "...bbaa..ba.",
        "....bb...ba.",
        ".........ba.",
        "..aa....aba.",
        ".aaba..aab..",
        "..bbbaabb...",
        "....bbb....."]),
    "sugar_butterfly": ({"a": "#c8a8f0", "b": "#f7a8c4", "k": "#5a3a46"}, [
        "............",
        "aaa..k..aaa.",
        "aaaa.k.aaaa.",
        "aaaaakaaaaa.",
        "aabaakaabaa.",
        ".aaaakaaaa..",
        "..bbbkbbb...",
        ".bbb.k.bbb..",
        ".bb..k..bb..",
        "............",
        "............",
        "............"]),
    "golden_crown": ({"a": "#f2c94c", "b": "#e05a86", "c": "#7ad0f0"}, [
        "............",
        "a....a....a.",
        "aa..aaa..aa.",
        "aaa.aaa.aaa.",
        "aaaaaaaaaaa.",
        "aaaaaaaaaaa.",
        "aabaacaabaa.",
        "aaaaaaaaaaa.",
        "aaaaaaaaaaa.",
        "............",
        "............",
        "............"]),
    "cake_flag": ({"a": "#f27aa4", "b": "#fbf6f2", "s": "#c89870"}, [
        "s...........",
        "saaaaaaaa...",
        "saabaabaaa..",
        "saaaaaaaaaa.",
        "saabaabaaa..",
        "saaaaaaaa...",
        "s...........",
        "s...........",
        "s...........",
        "s...........",
        "s...........",
        "s..........."]),
    "sugar_rainbow": ({"a": "#f27a8a", "b": "#ffc860", "c": "#9ed88a", "d": "#86b4f0", "w": "#fbf6f2"}, [
        "............",
        "...aaaaaa...",
        "..abbbbbba..",
        ".abccccccba.",
        "abcddddddcba",
        "abcd....dcba",
        "abcd....dcba",
        "wwwd....dwww",
        "wwww....wwww",
        ".ww......ww.",
        "............",
        "............"]),
    "sugar_moon": ({"a": "#ffe48a", "k": "#c8a040"}, [
        "....aaaa....",
        "..aaaa......",
        ".aaaa.......",
        ".aaa........",
        "aaaa........",
        "aaak........",
        "aaaa........",
        "aaaaa......a",
        ".aaaaa...aa.",
        "..aaaaaaaa..",
        "....aaaa....",
        "............"]),
    "sugar_sun": ({"a": "#ffc84a", "b": "#ff9a3c", "k": "#a05a2a"}, [
        ".....b......",
        ".b...b...b..",
        "..b.aaa.b...",
        "...aaaaa....",
        "..aaaaaaa...",
        "bbaakakaabb.",
        "..aaaaaaa...",
        "...akkka....",
        "..b.aaa.b...",
        ".b...b...b..",
        ".....b......",
        "............"]),
    "sugar_cloud": ({"a": "#fbfcff", "b": "#d8e4f4", "o": "#8aaad0"}, [
        "............",
        "............",
        "....aaa.....",
        "...aaaaa....",
        ".aaaaaaaaa..",
        "aaaaaaaaaaa.",
        "aaaaaaaaaaaa",
        "abbaaaaaabba",
        ".bbbbbbbbbb.",
        "............",
        "............",
        "............"]),
    "sugar_cherry": ({"a": "#e8405a", "s": "#6a9a3c", "w": "#ffd0d8"}, [
        ".......ss...",
        "......s.s...",
        ".....s...s..",
        "....s.....s.",
        "...s......s.",
        "..aaa....aaa",
        ".awaaa..awaa",
        ".aaaaa..aaaa",
        ".aaaaa..aaaa",
        "..aaa....aa.",
        "............",
        "............"]),
    "sugar_strawberry": ({"a": "#ee4a5e", "s": "#6ab04a", "w": "#ffe0a0"}, [
        "....ssss....",
        "...ssssss...",
        "..aaaaaaaa..",
        ".aawaaawaaa.",
        ".aaaaaaaaaa.",
        ".aaawaaawaa.",
        "..aaaaaaaa..",
        "..awaaawaa..",
        "...aaaaaa...",
        "....awaa....",
        ".....aa.....",
        "............"]),
    "sugar_bow": ({"a": "#f27aa4", "b": "#d85a8a"}, [
        "............",
        "............",
        "aa........aa",
        "aaaa....aaaa",
        "aaaaa..aaaaa",
        "aaaaabbaaaaa",
        "aaaaabbaaaaa",
        "aaaa.aa.aaaa",
        "aa..aa..aa..",
        "....a....a..",
        "............",
        "............"]),
    "sugar_daisy": ({"a": "#fffaf4", "b": "#ffc84a", "o": "#d8b070"}, [
        "....aaaa....",
        ".aa.aaaa.aa.",
        "aaaa.aa.aaaa",
        "aaaaa..aaaaa",
        ".aaa.bb.aaa.",
        "....bbbb....",
        "....bbbb....",
        ".aaa.bb.aaa.",
        "aaaaa..aaaaa",
        "aaaa.aa.aaaa",
        ".aa.aaaa.aa.",
        "....aaaa...."]),
    "sugar_bear": ({"a": "#b07a52", "b": "#e8c8a8", "k": "#3a2418", "p": "#f0a0a8"}, [
        ".aa......aa.",
        "abba....abba",
        "aaaaaaaaaaaa",
        ".aaaaaaaaaa.",
        "aakaaaaaakaa",
        "apaabbbbaapa",
        "aaabbkkbbaaa",
        "aaaabbbbaaaa",
        ".aaaaaaaaaa.",
        "..aaaaaaaa..",
        "............",
        "............"]),
    "sugar_kitty": ({"a": "#fbf6f2", "b": "#f7a8c4", "k": "#5a3a46", "y": "#ffd25a", "o": "#c89aaa"}, [
        "a..........a",
        "aa........aa",
        "aba......aba",
        "aaaaaaaaaaaa",
        "aaaaaaaaaayy",
        "aakaaaaakayy",
        "aaaaabaaaaaa",
        "kaaaaaaaaaak",
        ".aaaaaaaaaa.",
        "..aaaaaaaa..",
        "............",
        "............"]),
    "sugar_lollipop": ({"a": "#f7a8c4", "b": "#fff4f8", "s": "#e8d8c8", "o": "#c86890"}, [
        "...aaaaaa...",
        "..aabbbbaa..",
        ".abbaaaabba.",
        ".abaabbaaba.",
        ".abababbaba.",
        ".abaaabaaba.",
        "..abbbbaba..",
        "...aaaaaa...",
        ".....ss.....",
        ".....ss.....",
        ".....ss.....",
        ".....ss....."]),
    "sugar_cupcake": ({"a": "#f7a8c4", "b": "#c8925c", "c": "#e8405a", "w": "#fff2f6"}, [
        ".....c......",
        "....aaa.....",
        "..aawaaaa...",
        ".aaaaaaaaa..",
        "aaaaaaaaaaa.",
        ".bbbbbbbbb..",
        ".bwbwbwbwb..",
        "..bbbbbbb...",
        "..bwbwbwb...",
        "...bbbbb....",
        "............",
        "............"]),
    "sugar_leaf": ({"a": "#8acb64", "b": "#5e9a44"}, [
        "............",
        ".......aaa..",
        ".....aaaaaa.",
        "...aaaaaaba.",
        "..aaaaaabaa.",
        ".aaaaabaaa..",
        ".aaaabaaa...",
        ".aabaaaa....",
        ".baaaa......",
        "b...........",
        "............",
        "............"]),
    "sugar_mushroom": ({"a": "#e8405a", "w": "#fff6ec", "s": "#f4e4cc"}, [
        "............",
        "...aaaaaa...",
        "..awaaaawa..",
        ".aaaaaaaaaa.",
        "aawaaaaawaaa",
        "aaaaaawaaaaa",
        "....ssss....",
        "....ssss....",
        "....ssss....",
        "...ssssss...",
        "............",
        "............"]),
}


def decal(palette, rows):
    """A 16x16 sprite: the drawing in the middle, lit up-left, shaded down-right, with a soft darker outline."""
    pal = {k: hx(v) for k, v in palette.items()}
    pal.setdefault("w", (255, 255, 255))
    pal.setdefault("k", (90, 58, 70))
    if "o" in palette:
        pal["o"] = hx(palette["o"])
    main = pal["a"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    filled = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(12, ".")[:12]):
            if ch != ".":
                filled[(x + 2, y + 2)] = ch
    for (x, y), ch in filled.items():
        c = pal[ch]
        if ch not in "wk":
            up_left = (x - 1, y) not in filled or (x, y - 1) not in filled
            down_right = (x + 1, y) not in filled or (x, y + 1) not in filled
            if up_left and not down_right:
                c = shade(c, 0.08)
            elif down_right and not up_left:
                c = shade(c, -0.1)
        img.putpixel((x, y), c + (255,))
    outline = pal["o"] if "o" in pal else shade(main, -0.32)
    for y in range(16):
        for x in range(16):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), outline + (255,))
    return img


# ---- moulds: the same shape maths as CakeGeometry.inside()

def inside(shape, x, z, r):
    if shape == "star":
        a = math.atan2(x, z)
        wave = math.cos(5 * a)
        edge = r * (0.74 + 0.26 * math.copysign(abs(wave) ** 0.6, wave))
        return x * x + z * z <= edge * edge
    if shape == "flower":
        a = math.atan2(x, z)
        edge = r * (0.86 + 0.14 * abs(math.cos(3 * a)))
        return x * x + z * z <= edge * edge
    if shape == "hexagon":
        ax, az, h = abs(x), abs(z), r * 0.95
        return az <= h * 0.866 and ax * 0.866 + az * 0.5 <= h * 0.866
    if shape == "bunny":
        face, fz = r * 0.78, z - r * 0.18
        if x * x + fz * fz <= face * face:
            return True
        for ex in (-r * 0.36, r * 0.36):
            dx, dz = (x - ex) / (r * 0.24), (z + r * 0.55) / (r * 0.5)
            if dx * dx + dz * dz <= 1:
                return True
        return False
    return x * x + z * z <= r * r


MOULDS = {"star": ("#c8ccd6", "#8e92a0", "#eef0f6"), "flower": ("#f7a8c4", "#d0688e", "#ffd6e6"),
          "hexagon": ("#c8ccd6", "#8e92a0", "#eef0f6"), "bunny": ("#f7a8c4", "#d0688e", "#ffd6e6")}


def mould(shape, colors):
    """A baking tin of that shape seen from above: a rim, a lighter inside, a shine on the upper left."""
    rim, dark, inner = (hx(c) for c in colors)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            px, pz = x - 7.5, y - 7.5
            if not inside(shape, px, pz, 7.4):
                continue
            if inside(shape, px, pz, 5.4):
                c = inner if px + pz < -3 else shade(inner, -0.06)
            elif inside(shape, px, pz, 6.3):
                c = shade(rim, -0.05)
            else:
                c = rim if px + pz < 0 else dark
            img.putpixel((x, y), c + (255,))
    # outline
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3]
                                                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), shade(dark, -0.2) + (255,))
    return out


def item_model(texture):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": texture}}


def build():
    n = 0
    for name, (palette, rows) in DECALS.items():
        decal(palette, rows).save(os.path.join(TEX, f"decal_{name}.png"))
        with open(os.path.join(ASSETS, "models/item", f"{name}.json"), "w") as fh:
            json.dump(item_model(f"bakery:item/decal_{name}"), fh, indent="\t")
        n += 1
    for shape, colors in MOULDS.items():
        mould(shape, colors).save(os.path.join(TEX, f"{shape}_cake_mold.png"))
        with open(os.path.join(ASSETS, "models/item", f"{shape}_cake_mold.json"), "w") as fh:
            json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"bakery:item/{shape}_cake_mold"}}, fh, indent="\t")
        n += 1
    return n


if __name__ == "__main__":
    print(build(), "decal and mould sprites written")
