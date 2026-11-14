"""
Round shapes for Minecraft block models.

Block models only have boxes, rotatable in 22.5 degree steps around one axis. A regular 16-sided disc is the union
of eight thin boxes turned 0, 22.5, ... 157.5 degrees around the disc's centre: this module writes those boxes, so
pots, bowls, tiers and lamp shades come out round instead of blocky.

    m = Model(textures={"clay": "bakery:block/ceramic_pink"})
    m.disc("body", r=6, y1=0, y2=8, tex="clay")
    m.ring("rim", r_out=6.5, r_in=5, y1=8, y2=9, tex="clay")
    m.box("handle", (7, 2, 1), (9, 6, 2), tex="clay")
    m.save("src/main/resources/assets/bakery/models/block/thing.json")

All sizes are in pixels; (cx, cz) defaults to the block centre (8, 8).
"""
import json
import math

TAN_11 = math.tan(math.radians(11.25))
# How far apart overlapping caps are pushed (pixels): enough for the depth buffer, too small to see.
STAGGER = 0.025
# (base axis, rotation) pairs that give the eight 22.5-degree orientations with only -45..45 rotations.
ORIENTATIONS = [("x", 0), ("x", 22.5), ("x", 45), ("x", -22.5), ("z", 22.5), ("z", 0), ("z", -22.5), ("x", -45)]


def _uv(a, b):
    """A uv span centred on the texture, at most 16 wide."""
    a, b = min(a, b), max(a, b)
    w = min(16.0, b - a)
    c = 8.0
    return [round(c - w / 2, 3), round(c + w / 2, 3)]


def _faces(size, tex, faces=None, tint=None, top_tex=None, bottom_tex=None, uv_y=None):
    w, h, d = size
    ux = _uv(0, w)
    uz = _uv(0, d)
    vy = _uv(0, h) if uv_y is None else uv_y
    out = {}
    for f in (faces or ["north", "south", "east", "west", "up", "down"]):
        if f in ("north", "south"):
            uv = [ux[0], vy[0], ux[1], vy[1]]
        elif f in ("east", "west"):
            uv = [uz[0], vy[0], uz[1], vy[1]]
        else:
            uv = [ux[0], uz[0], ux[1], uz[1]]
        t = tex
        if f == "up" and top_tex:
            t = top_tex
        if f == "down" and bottom_tex:
            t = bottom_tex
        face = {"uv": uv, "texture": "#" + t}
        if tint is not None:
            face["tintindex"] = tint
        out[f] = face
    return out


class Model:
    def __init__(self, textures, parent="block/block", ambient=False, display=None):
        self.data = {"parent": parent, "textures": dict(textures), "elements": []}
        if not ambient:
            self.data["ambientocclusion"] = False
        if "particle" not in self.data["textures"]:
            self.data["textures"]["particle"] = next(iter(textures.values()))
        if display:
            self.data["display"] = display

    def _add(self, name, frm, to, faces, rotation=None, shade=True):
        e = {"name": name, "from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
        if rotation:
            e["rotation"] = rotation
        if not shade:
            e["shade"] = False
        self.data["elements"].append(e)
        return e

    def box(self, name, frm, to, tex, faces=None, top=None, bottom=None, rotation=None, uv_y=None):
        size = tuple(to[i] - frm[i] for i in range(3))
        return self._add(name, frm, to, _faces(size, tex, faces, top_tex=top, bottom_tex=bottom, uv_y=uv_y), rotation)

    def disc(self, name, r, y1, y2, tex, cx=8, cz=8, top=None, bottom=None, sides=True, uv_y=None):
        """A 16-sided upright cylinder of radius r (to the flat sides)."""
        half = r * TAN_11 + 0.001
        for i, (axis, angle) in enumerate(ORIENTATIONS):
            # Stagger each piece's cap a hair so overlapping tops never z-fight (flicker/stripes).
            dy = i * STAGGER
            if axis == "x":
                frm, to = (cx - r, y1 - dy, cz - half), (cx + r, y2 + dy, cz + half)
                faces = ["east", "west"] if sides else []
            else:
                frm, to = (cx - half, y1 - dy, cz - r), (cx + half, y2 + dy, cz + r)
                faces = ["north", "south"] if sides else []
            faces = faces + ["up", "down"]
            rot = {"angle": angle, "axis": "y", "origin": [cx, (y1 + y2) / 2, cz]} if angle else None
            self.box(f"{name}_{i}", frm, to, tex, faces, top=top, bottom=bottom, rotation=rot, uv_y=uv_y)

    def ring(self, name, r_out, r_in, y1, y2, tex, cx=8, cz=8, top=None, segments=16):
        """A round rim: short boxes standing around a circle, each turned to face the centre."""
        assert (360 / segments) % 22.5 == 0, "ring segments must be 8 or 16"
        mid = (r_out + r_in) / 2
        thick = r_out - r_in
        length = 2 * r_out * math.tan(math.pi / segments) + 0.05
        for k in range(segments):
            theta = k * 360 / segments
            # Pick the base direction (E, S, W, N) closest, and the remaining turn (-45..45).
            turns = round(theta / 90)
            rest = theta - turns * 90
            base = turns % 4
            # Box centred on the base axis at distance mid, then turned by -rest about the centre
            # (Minecraft y rotation is clockwise-positive seen from above, angles measured from +x toward +z).
            if base in (0, 2):
                sx = 1 if base == 0 else -1
                frm = (cx + sx * mid - thick / 2, y1, cz - length / 2)
                to = (cx + sx * mid + thick / 2, y2, cz + length / 2)
                faces = ["east", "west", "north", "south", "up"]
            else:
                sz = 1 if base == 1 else -1
                frm = (cx - length / 2, y1, cz + sz * mid - thick / 2)
                to = (cx + length / 2, y2, cz + sz * mid + thick / 2)
                faces = ["north", "south", "east", "west", "up"]
            # Neighbouring segments overlap at their ends: alternate their tops so they never z-fight.
            lift = (k % 2) * STAGGER * 2
            frm = (frm[0], frm[1] - lift, frm[2])
            to = (to[0], to[1] + lift, to[2])
            rot = {"angle": -rest, "axis": "y", "origin": [cx, (y1 + y2) / 2, cz]} if abs(rest) > 1e-6 else None
            self.box(f"{name}_{k}", frm, to, tex, faces, top=top, rotation=rot)

    def cone(self, name, r_bottom, r_top, y1, y2, tex, steps=3, cx=8, cz=8, top=None):
        """A tapered round shape stacked from thin discs."""
        h = (y2 - y1) / steps
        for s in range(steps):
            t = (s + 0.5) / steps
            r = r_bottom + (r_top - r_bottom) * t
            self.disc(f"{name}_{s}", r, y1 + s * h, y1 + (s + 1) * h, tex, cx, cz, top=top)

    def dome(self, name, r, y1, height, tex, steps=4, cx=8, cz=8):
        """A rounded cap (quarter-ellipse profile)."""
        h = height / steps
        for s in range(steps):
            t = (s + 0.5) / steps
            rr = r * math.sqrt(max(0.0, 1 - t * t))
            self.disc(f"{name}_{s}", max(0.4, rr), y1 + s * h, y1 + (s + 1) * h, tex, cx, cz)

    def wrap(self, tex, y_bottom, y_top, plain=(4, 13, 8, 15)):
        """Wraps texture `tex` round the round shapes that use it: each side face takes the column for the way it
        faces (16 columns = 16 sides) and the rows for its height (row 0 at y_top). Tops and bottoms take the plain
        patch. Painted bands and spots then go all the way round instead of repeating one middle strip."""
        normals = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
        span = max(0.01, y_top - y_bottom)
        for e in self.data["elements"]:
            rot = e.get("rotation") or {}
            ang = math.radians(rot.get("angle", 0)) if rot.get("axis") == "y" else 0
            for fname, f in e["faces"].items():
                if f.get("texture") != "#" + tex:
                    continue
                if fname in ("up", "down"):
                    f["uv"] = list(plain)
                    continue
                nx, nz = normals[fname]
                rx, rz = nx * math.cos(ang) + nz * math.sin(ang), -nx * math.sin(ang) + nz * math.cos(ang)
                col = round((math.degrees(math.atan2(rz, rx)) % 360) / 22.5) % 16
                v1 = max(0.0, min(16.0, (y_top - e["to"][1]) / span * 16))
                v2 = max(0.0, min(16.0, (y_top - e["from"][1]) / span * 16))
                f["uv"] = [col, round(v1, 3), col + 1, round(max(v2, v1 + 0.5), 3)]
        return self

    def save(self, path):
        with open(path, "w") as f:
            json.dump(self.data, f, indent="\t")
            f.write("\n")


BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}
