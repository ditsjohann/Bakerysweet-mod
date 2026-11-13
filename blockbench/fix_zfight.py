"""
Removes the flicker ("z-fighting") from block models.

When two faces of a model lie in exactly the same plane and overlap, the graphics card can't tell which is in front,
so they flicker and stripe as you move: it looks like a lighting glitch. This pass, over every block model:

1. drops faces that are completely hidden inside another piece of the model (a solid, non see-through one), and
2. where two visible faces facing the same way still share a plane, pushes the smaller piece's face forward by a
   hair (a few hundredths of a pixel) so one is clearly in front.

Only pieces with the same rotation are compared; a piece turned differently can't share a plane with them anyway.
Run after any model generator:  python3 blockbench/fix_zfight.py
"""
import glob
import json
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery")
EPS = 1e-4
NUDGE = 0.02
AXIS = {"east": 0, "west": 0, "up": 1, "down": 1, "south": 2, "north": 2}
POSITIVE = {"east", "up", "south"}
_alpha_cache = {}


def has_transparency(tex_id):
    if tex_id in _alpha_cache:
        return _alpha_cache[tex_id]
    ns, path = tex_id.split(":", 1) if ":" in tex_id else ("minecraft", tex_id)
    result = True  # unknown (vanilla) textures: be careful, treat as see-through
    if ns == "bakery":
        f = os.path.join(ROOT, "textures", path + ".png")
        if os.path.exists(f):
            a = np.array(Image.open(f).convert("RGBA"))[..., 3]
            result = bool((a < 250).any())
    _alpha_cache[tex_id] = result
    return result


def resolve(model, ref, depth=0):
    """'#name' -> 'bakery:block/x' following the model's textures and its parents."""
    textures = model.get("textures", {})
    while ref.startswith("#") and depth < 8:
        key = ref[1:]
        if key in textures:
            ref = textures[key]
        else:
            parent = model.get("parent", "")
            if parent.startswith("bakery:"):
                pf = os.path.join(ROOT, "models", parent.split(":", 1)[1] + ".json")
                if os.path.exists(pf):
                    return resolve(json.load(open(pf)), ref, depth + 1)
            return None
        depth += 1
    return ref


def rot_key(e):
    r = e.get("rotation")
    if not r or not r.get("angle"):
        return None
    return (r["axis"], r["angle"], tuple(round(v, 4) for v in r["origin"]))


def solid(model, e):
    """Whether a piece blocks the view (no see-through textures on any face)."""
    for f in e["faces"].values():
        t = resolve(model, f.get("texture", ""))
        if t is None or has_transparency(t):
            return False
    return bool(e["faces"])


def rect(e, ax):
    o = [i for i in range(3) if i != ax]
    return (e["from"][o[0]], e["to"][o[0]], e["from"][o[1]], e["to"][o[1]])


def plane(e, face):
    ax = AXIS[face]
    return e["to"][ax] if face in POSITIVE else e["from"][ax]


def covers(big, small):
    return big[0] <= small[0] + EPS and big[1] >= small[1] - EPS and big[2] <= small[2] + EPS and big[3] >= small[3] - EPS


def overlap(a, b):
    return min(a[1], b[1]) - max(a[0], b[0]) > EPS and min(a[3], b[3]) - max(a[2], b[2]) > EPS


def footprint(e):
    """The x/z bounding box of a piece seen from above, taking a y rotation into account."""
    import math
    x1, z1, x2, z2 = e["from"][0], e["from"][2], e["to"][0], e["to"][2]
    r = e.get("rotation")
    if not r or not r.get("angle") or r["axis"] != "y":
        return (x1, x2, z1, z2)
    a = math.radians(r["angle"])
    ox, oz = r["origin"][0], r["origin"][2]
    xs, zs = [], []
    for x, z in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        dx, dz = x - ox, z - oz
        xs.append(ox + dx * math.cos(a) + dz * math.sin(a))
        zs.append(oz - dx * math.sin(a) + dz * math.cos(a))
    return (min(xs), max(xs), min(zs), max(zs))


def separate_turned_caps(model):
    """Tops (and bottoms) of pieces turned differently around y can still share a height: lift one of each pair."""
    els = [e for e in model.get("elements", []) if not e.get("rotation") or not e["rotation"].get("angle") or e["rotation"]["axis"] == "y"]
    changed = 0
    for _ in range(6):
        moved = False
        for face, k, sign in (("up", "to", 1), ("down", "from", -1)):
            seen = []
            for e in els:
                if face not in e["faces"]:
                    continue
                y = round(e[k][1], 4)
                fp = footprint(e)
                for (y2, fp2, e2) in seen:
                    if y2 == y and rot_key(e2) != rot_key(e) and overlap(fp, fp2):
                        e[k][1] = round(e[k][1] + sign * 0.013, 4)
                        changed += 1
                        moved = True
                        break
                seen.append((round(e[k][1], 4), fp, e))
        if not moved:
            break
    return changed


def fix_model(model):
    els = model.get("elements")
    if not els:
        return 0
    changed = 0
    solids = [solid(model, e) for e in els]
    keys = [rot_key(e) for e in els]
    # 1. hidden faces
    for i, e in enumerate(els):
        for face in list(e["faces"]):
            ax = AXIS[face]
            p = plane(e, face)
            r = rect(e, ax)
            for j, f in enumerate(els):
                if i == j or not solids[j] or keys[j] != keys[i]:
                    continue
                inside = (f["from"][ax] <= p + EPS and f["to"][ax] > p + 0.01) if face in POSITIVE else \
                         (f["to"][ax] >= p - EPS and f["from"][ax] < p - 0.01)
                if inside and covers(rect(f, ax), r):
                    del e["faces"][face]
                    changed += 1
                    break
    # 2. visible faces that still share a plane: push the smaller one forward
    for _ in range(4):
        moved = False
        for i, e in enumerate(els):
            for face in list(e["faces"]):
                ax = AXIS[face]
                p = plane(e, face)
                r = rect(e, ax)
                for j, f in enumerate(els):
                    if j <= i or keys[j] != keys[i] or face not in f["faces"]:
                        continue
                    if abs(plane(f, face) - p) > EPS or not overlap(r, rect(f, ax)):
                        continue
                    area_e = (r[1] - r[0]) * (r[3] - r[2])
                    rf = rect(f, ax)
                    area_f = (rf[1] - rf[0]) * (rf[3] - rf[2])
                    target = e if area_e <= area_f else f
                    k = "to" if face in POSITIVE else "from"
                    step = NUDGE if face in POSITIVE else -NUDGE
                    # Models must stay within -16..32: at the limit, pull the face back instead of pushing it out.
                    if not -16 <= target[k][ax] + step <= 32:
                        step = -step
                    target[k][ax] = round(target[k][ax] + step, 4)
                    changed += 1
                    moved = True
        if not moved:
            break
    changed += separate_turned_caps(model)
    # A piece whose faces were all hidden is gone entirely (Minecraft refuses pieces with no faces).
    before = len(els)
    els[:] = [e for e in els if e["faces"]]
    for e in els:
        for k in ("from", "to"):
            e[k] = [min(32, max(-16, v)) for v in e[k]]
    return changed + (before - len(els))


def run(paths=None):
    total = 0
    files = paths or glob.glob(os.path.join(ROOT, "models/block/**/*.json"), recursive=True)
    for path in files:
        model = json.load(open(path))
        n = fix_model(model)
        if n:
            with open(path, "w") as fh:
                json.dump(model, fh, indent="\t")
                fh.write("\n")
            total += 1
    return total


if __name__ == "__main__":
    print(run(), "models fixed")
