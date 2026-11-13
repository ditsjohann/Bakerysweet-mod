"""Regenerates every generated texture and model, in order (later scripts own what they draw). Then removes z-fighting."""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
STEPS = ["gen_textures.py", "gen_materials.py", "gen_food.py", "gen_deco.py", "gen_woods.py", "gen_crops.py",
         "gen_catalog.py", "gen_fountain.py", "gen_couch.py", "gen_kitchen.py", "fix_zfight.py"]
for step in STEPS:
    print("==", step)
    subprocess.run([sys.executable, os.path.join(HERE, step)], check=True, cwd=os.path.dirname(HERE))
