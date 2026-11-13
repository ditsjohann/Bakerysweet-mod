"""
Writes run/showcase.txt for an in-game photo gallery: the given blocks stand in a row on a cream floor in front of a
plain wall, and the camera takes a close shot of every pair.

    python3 blockbench/gallery.py name "bakery:pink_couch[facing=north]" "bakery:juicer[facing=north]" ...
Then: xvfb-run -a ./gradlew runShowcase  (shots land in run/screenshots/<name>_<n>.png)
"""
import sys

name, blocks = sys.argv[1], sys.argv[2:]
X0, Y, Z = 100, 150, 0
lines = ["cmd gamerule doDaylightCycle false", "cmd gamerule doMobSpawning false", "cmd time set 6000", "cmd weather clear",
         "cmd gamemode spectator @a", f"cmd forceload add {X0 - 8} {Z - 16} {X0 + len(blocks) * 2 + 8} {Z + 16}", "wait 40",
         f"cmd fill {X0 - 4} {Y - 1} {Z - 6} {X0 + len(blocks) * 2 + 4} {Y - 1} {Z + 3} minecraft:smooth_quartz",
         f"cmd fill {X0 - 4} {Y} {Z - 6} {X0 + len(blocks) * 2 + 4} {Y + 6} {Z + 2} minecraft:air",
         f"cmd fill {X0 - 4} {Y} {Z + 3} {X0 + len(blocks) * 2 + 4} {Y + 5} {Z + 3} minecraft:white_concrete"]
for i, b in enumerate(blocks):
    lines.append(f"cmd setblock {X0 + i * 2} {Y} {Z} {b}")
lines.append("wait 60")
for g in range(0, len(blocks), 2):
    cx = X0 + g * 2 + 1.5
    lines += [f"cam {cx} {Y - 0.55} {Z - 2.2} 0 24", "wait 25", f"shot {name}_{g // 2:02d}"]
lines.append("quit")
open("run/showcase.txt", "w").write("\n".join(lines) + "\n")
print(len(blocks), "blocks,", (len(blocks) + 1) // 2, "shots")
