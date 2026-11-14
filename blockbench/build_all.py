"""Regenerates every generated texture and model, in order (later scripts own what they draw). Then removes z-fighting."""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
STEPS = ["gen_textures.py", "gen_materials.py", "gen_food.py", "gen_deco.py", "gen_woods.py", "gen_woods16.py", "gen_woods_unique.py", "gen_leaves16.py", "gen_crops.py", "gen_crops16.py",
         "gen_catalog.py", "gen_fountain.py", "gen_couch.py", "gen_kitchen.py", "gen_notebook.py", "gen_decals.py", "gen_deco16.py", "gen_materials16.py", "gen_counter_bread.py",
         "gen_food16.py", "gen_displays.py", "gen_paintable.py", "gen_nature.py", "fix_zfight.py"]
for step in STEPS:
    print("==", step)
    subprocess.run([sys.executable, os.path.join(HERE, step)], check=True, cwd=os.path.dirname(HERE))
