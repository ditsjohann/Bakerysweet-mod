"""
Builds numbered contact sheets from the shots of catalog_shots.py.
    python3 blockbench/catalog_sheet.py <out_dir>
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
CROP = {"std": 330, "flat": 330, "tall": 420, "high": 420, "wide": 520, "big": 640}
CELL, COLS, PER_SHEET = 220, 6, 36


def font(size):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def main(out):
    os.makedirs(out, exist_ok=True)
    items = json.load(open(os.path.join(ROOT, "run/all_blocks.json")))
    big, small = font(22), font(15)
    sheets = []
    for s in range(0, len(items), PER_SHEET):
        chunk = items[s:s + PER_SHEET]
        rows = (len(chunk) + COLS - 1) // COLS
        sheet = Image.new("RGB", (COLS * CELL, rows * (CELL + 26)), (250, 244, 246))
        d = ImageDraw.Draw(sheet)
        for i, it in enumerate(chunk):
            path = os.path.join(ROOT, f"run/screenshots/all_{it['n']:03d}.png")
            cx, cy = (i % COLS) * CELL, (i // COLS) * (CELL + 26)
            if os.path.exists(path):
                shot = Image.open(path).convert("RGB")
                c = CROP[it["profile"]]
                w, h = shot.size
                box = (w // 2 - c // 2, h // 2 - c // 2, w // 2 + c // 2, h // 2 + c // 2)
                sheet.paste(shot.crop(box).resize((CELL - 6, CELL - 6), Image.LANCZOS), (cx + 3, cy + 3))
            d.rounded_rectangle((cx + 6, cy + 6, cx + 54, cy + 34), 7, fill=(214, 74, 130))
            d.text((cx + 30, cy + 20), str(it["n"]), font=big, fill="white", anchor="mm")
            d.text((cx + CELL // 2, cy + CELL + 12), it["label"], font=small, fill=(90, 50, 70), anchor="mm")
        p = os.path.join(out, f"blocks_{s // PER_SHEET + 1}.png")
        sheet.save(p)
        sheets.append(p)
    print("\n".join(sheets))


if __name__ == "__main__":
    main(sys.argv[1])
